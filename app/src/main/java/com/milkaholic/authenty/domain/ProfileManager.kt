package com.milkaholic.authenty.domain

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.milkaholic.authenty.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: ProfileManager? = null
        
        fun getInstance(context: Context): ProfileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProfileManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val PROFILES_KEY = "user_profiles"
        private const val CURRENT_PROFILE_KEY = "current_profile_id"
        private const val PROFILE_ACCOUNTS_KEY = "profile_accounts_"
    }
    
    private val securityManager by lazy { SecurityManager.getInstance(context) }
    
    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "profile_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w("ProfileManager", "Failed to create EncryptedSharedPreferences, falling back to regular SharedPreferences", e)
        context.getSharedPreferences("profile_prefs_fallback", Context.MODE_PRIVATE)
    }

    private val gson = Gson()
    
    // State flows for reactive UI updates
    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    val currentProfile: StateFlow<UserProfile?> = _currentProfile.asStateFlow()
    
    private val _allProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val allProfiles: StateFlow<List<UserProfile>> = _allProfiles.asStateFlow()

    init {
        initializeProfiles()
    }

    private fun initializeProfiles() {
        val profiles = loadProfiles()
        if (profiles.isEmpty()) {
            // Create default profile if none exist
            val defaultProfile = UserProfile.createDefaultProfile()
            saveProfile(defaultProfile)
            setCurrentProfile(defaultProfile.id)
        } else {
            _allProfiles.value = profiles
            val currentProfileId = getCurrentProfileId()
            _currentProfile.value = profiles.find { it.id == currentProfileId } ?: profiles.first()
        }
    }

    // Profile CRUD Operations
    fun createProfile(name: String, settings: ProfileSettings = ProfileSettings()): AuthentyResult<UserProfile> {
        return try {
            val profiles = loadProfiles()
            
            // Validate name uniqueness
            if (profiles.any { it.name.equals(name, ignoreCase = true) }) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Profile name already exists"))
            }
            
            val newProfile = UserProfile(
                name = name,
                settings = settings
            )
            
            saveProfile(newProfile)
            AuthentyResult.Success(newProfile)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun updateProfile(profileId: String, updatedProfile: UserProfile): AuthentyResult<Unit> {
        return try {
            val profiles = loadProfiles().toMutableList()
            val index = profiles.indexOfFirst { it.id == profileId }
            
            if (index == -1) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Profile not found"))
            }
            
            profiles[index] = updatedProfile.copy(id = profileId)
            saveProfiles(profiles)
            _allProfiles.value = profiles
            
            if (profileId == _currentProfile.value?.id) {
                _currentProfile.value = updatedProfile.copy(id = profileId)
            }
            
            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun deleteProfile(profileId: String): AuthentyResult<Unit> {
        return try {
            val profiles = loadProfiles().toMutableList()
            val profileToDelete = profiles.find { it.id == profileId }
            
            if (profileToDelete == null) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Profile not found"))
            }
            
            if (profileToDelete.isDefault || profiles.size <= 1) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Cannot delete default or last remaining profile"))
            }
            
            // Remove profile
            profiles.removeIf { it.id == profileId }
            saveProfiles(profiles)
            
            // Clear profile-specific account data
            clearProfileAccounts(profileId)
            
            // If this was the current profile, switch to default
            if (profileId == _currentProfile.value?.id) {
                val defaultProfile = profiles.find { it.isDefault } ?: profiles.first()
                setCurrentProfile(defaultProfile.id)
            }
            
            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    // Profile Management
    fun setCurrentProfile(profileId: String): AuthentyResult<Unit> {
        return try {
            val profiles = loadProfiles()
            val profile = profiles.find { it.id == profileId }
                ?: return AuthentyResult.Error(AuthentyError.ValidationError("Profile not found"))
            
            sharedPreferences.edit()
                .putString(CURRENT_PROFILE_KEY, profileId)
                .apply()
            
            // Update last used timestamp
            val updatedProfile = profile.copy(lastUsed = System.currentTimeMillis())
            updateProfile(profileId, updatedProfile)
            
            _currentProfile.value = updatedProfile
            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun getCurrentProfile(): UserProfile? = _currentProfile.value

    fun getAllProfiles(): List<UserProfile> = loadProfiles()

    // Account Management within Profiles
    fun addAccountToCurrentProfile(account: AccountModel, category: String = "Personal"): AuthentyResult<Unit> {
        val currentProfile = getCurrentProfile()
            ?: return AuthentyResult.Error(AuthentyError.ValidationError("No active profile"))
        
        return addAccountToProfile(currentProfile.id, account, category)
    }

    fun addAccountToProfile(profileId: String, account: AccountModel, category: String = "Personal"): AuthentyResult<Unit> {
        return try {
            val accountWithProfile = AccountWithProfile(
                account = account,
                profileId = profileId,
                category = category
            )
            
            val profileAccounts = getProfileAccounts(profileId).toMutableList()
            
            // Check for duplicates within this profile
            val isDuplicate = profileAccounts.any {
                it.account.issuer.equals(account.issuer, ignoreCase = true) &&
                it.account.name.equals(account.name, ignoreCase = true)
            }
            
            if (isDuplicate) {
                return AuthentyResult.Error(AuthentyError.DuplicateAccount(account.issuer, account.name))
            }
            
            profileAccounts.add(accountWithProfile)
            saveProfileAccounts(profileId, profileAccounts)
            
            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun getAccountsForCurrentProfile(): List<AccountWithProfile> {
        val currentProfile = getCurrentProfile() ?: return emptyList()
        return getProfileAccounts(currentProfile.id)
    }

    fun getAccountsForProfile(profileId: String): List<AccountWithProfile> {
        if (securityManager.isDuressMode.value) {
            return emptyList()
        }
        return getProfileAccounts(profileId)
    }

    fun getAccountsByCategory(category: String, profileId: String? = null): List<AccountWithProfile> {
        val targetProfileId = profileId ?: getCurrentProfile()?.id ?: return emptyList()
        return getProfileAccounts(targetProfileId).filter { it.category == category }
    }

    fun updateAccountCategory(accountId: Long, newCategory: String): AuthentyResult<Unit> {
        val currentProfile = getCurrentProfile()
            ?: return AuthentyResult.Error(AuthentyError.ValidationError("No active profile"))
        
        return try {
            val accounts = getProfileAccounts(currentProfile.id).toMutableList()
            val accountIndex = accounts.indexOfFirst { it.account.id == accountId }
            
            if (accountIndex == -1) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Account not found"))
            }
            
            accounts[accountIndex] = accounts[accountIndex].copy(category = newCategory)
            saveProfileAccounts(currentProfile.id, accounts)
            
            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun updateAccountBackupCodes(accountId: Long, backupCodes: List<String>): AuthentyResult<Unit> {
        val currentProfile = getCurrentProfile()
            ?: return AuthentyResult.Error(AuthentyError.ValidationError("No active profile"))
        
        return try {
            val accounts = getProfileAccounts(currentProfile.id).toMutableList()
            val accountIndex = accounts.indexOfFirst { it.account.id == accountId }
            
            if (accountIndex == -1) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Account not found"))
            }
            
            val updatedAccount = accounts[accountIndex].account.copy(backupCodes = backupCodes)
            accounts[accountIndex] = accounts[accountIndex].copy(account = updatedAccount)
            saveProfileAccounts(currentProfile.id, accounts)
            
            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun deleteAccountFromCurrentProfile(accountId: Long): AuthentyResult<Unit> {
        val currentProfile = getCurrentProfile()
            ?: return AuthentyResult.Error(AuthentyError.ValidationError("No active profile"))
        
        return try {
            val accounts = getProfileAccounts(currentProfile.id).toMutableList()
            val initialSize = accounts.size
            
            accounts.removeIf { it.account.id == accountId }
            
            if (accounts.size == initialSize) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Account not found"))
            }
            
            saveProfileAccounts(currentProfile.id, accounts)
            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    // Private helper methods
    private fun loadProfiles(): List<UserProfile> {
        val json = sharedPreferences.getString(PROFILES_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<UserProfile>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveProfiles(profiles: List<UserProfile>) {
        val json = gson.toJson(profiles)
        sharedPreferences.edit()
            .putString(PROFILES_KEY, json)
            .apply()
        _allProfiles.value = profiles
    }

    private fun saveProfile(profile: UserProfile) {
        val profiles = loadProfiles().toMutableList()
        val existingIndex = profiles.indexOfFirst { it.id == profile.id }
        
        if (existingIndex >= 0) {
            profiles[existingIndex] = profile
        } else {
            profiles.add(profile)
        }
        
        saveProfiles(profiles)
    }

    private fun getCurrentProfileId(): String? {
        return sharedPreferences.getString(CURRENT_PROFILE_KEY, null)
    }

    private fun getProfileAccounts(profileId: String): List<AccountWithProfile> {
        val key = PROFILE_ACCOUNTS_KEY + profileId
        val json = sharedPreferences.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<AccountWithProfile>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveProfileAccounts(profileId: String, accounts: List<AccountWithProfile>) {
        val key = PROFILE_ACCOUNTS_KEY + profileId
        val json = gson.toJson(accounts)
        sharedPreferences.edit()
            .putString(key, json)
            .apply()
    }

    private fun clearProfileAccounts(profileId: String) {
        val key = PROFILE_ACCOUNTS_KEY + profileId
        sharedPreferences.edit()
            .remove(key)
            .apply()
    }

    // Search and Filter functionality
    fun searchAccounts(query: String, profileId: String? = null): List<AccountWithProfile> {
        val targetProfileId = profileId ?: getCurrentProfile()?.id ?: return emptyList()
        val accounts = getProfileAccounts(targetProfileId)
        
        if (query.isBlank()) return accounts
        
        return accounts.filter { accountWithProfile ->
            accountWithProfile.account.name.contains(query, ignoreCase = true) ||
            accountWithProfile.account.issuer.contains(query, ignoreCase = true) ||
            accountWithProfile.category.contains(query, ignoreCase = true)
        }
    }

    fun getAccountCategories(profileId: String? = null): List<String> {
        val targetProfileId = profileId ?: getCurrentProfile()?.id ?: return emptyList()
        return getProfileAccounts(targetProfileId)
            .map { it.category }
            .distinct()
            .sorted()
    }
    
    // Account ordering functionality
    fun reorderAccounts(accountIds: List<Long>): AuthentyResult<Unit> {
        val currentProfile = getCurrentProfile()
            ?: return AuthentyResult.Error(AuthentyError.ValidationError("No active profile"))
        
        return try {
            val accounts = getProfileAccounts(currentProfile.id).toMutableList()
            
            // Create a map of current accounts by ID for quick lookup
            val accountMap = accounts.associateBy { it.account.id }
            
            // Reorder accounts based on the provided order
            val reorderedAccounts = accountIds.mapNotNull { id ->
                accountMap[id]?.copy(lastUsed = System.currentTimeMillis())
            }
            
            // Add any accounts that weren't in the reorder list (shouldn't happen in normal usage)
            val reorderedIds = accountIds.toSet()
            val remainingAccounts = accounts.filter { it.account.id !in reorderedIds }
            
            val finalAccounts = reorderedAccounts + remainingAccounts
            saveProfileAccounts(currentProfile.id, finalAccounts)
            
            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }
}