package com.milkaholic.authenty.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.milkaholic.authenty.domain.AuthentyError
import com.milkaholic.authenty.domain.AuthentyResult
import com.milkaholic.authenty.domain.ValidationUtils

class SecureRepository(context: Context) {

    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_auth_prefs", // File name
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w("SecureRepository", "Failed to create EncryptedSharedPreferences, falling back to regular SharedPreferences", e)
        // Fallback to regular SharedPreferences if encryption is not available
        context.getSharedPreferences("secure_auth_prefs_fallback", Context.MODE_PRIVATE)
    }

    private val gson = Gson()

    // --- LEGACY METHODS FOR BACKWARD COMPATIBILITY ---
    
    /**
     * Legacy add account method
     */
    fun addAccount(account: AccountModel) {
        val result = addAccountWithResult(account)
        if (result is AuthentyResult.Error) {
            throw IllegalArgumentException(result.error.message)
        }
    }

    /**
     * Enhanced add account method with proper error handling
     */
    fun addAccountWithResult(account: AccountModel): AuthentyResult<Unit> {
        try {
            // Validate account data
            ValidationUtils.validateAccountName(account.name)?.let { error ->
                return AuthentyResult.Error(error)
            }
            
            ValidationUtils.validateIssuer(account.issuer)?.let { error ->
                return AuthentyResult.Error(error)
            }
            
            ValidationUtils.validateBase32Secret(account.secret)?.let { error ->
                return AuthentyResult.Error(error)
            }

            // Check for duplicates
            val currentList = getAccountsInternal()
            val isDuplicate = currentList.any { 
                it.issuer.equals(account.issuer, ignoreCase = true) && 
                it.name.equals(account.name, ignoreCase = true) 
            }
            
            if (isDuplicate) {
                return AuthentyResult.Error(AuthentyError.DuplicateAccount(account.issuer, account.name))
            }

            // Add account
            val newList = currentList.toMutableList()
            newList.add(account)
            
            val saveResult = saveListWithResult(newList)
            if (saveResult is AuthentyResult.Error) {
                return saveResult
            }
            
            return AuthentyResult.Success(Unit)
            
        } catch (e: Exception) {
            return AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    // --- GET ALL ACCOUNTS ---
    fun getAccounts(): List<AccountModel> {
        val result = getAccountsWithResult()
        return result.getOrNull() ?: emptyList()
    }
    
    /**
     * Enhanced get accounts method with error handling
     */
    fun getAccountsWithResult(): AuthentyResult<List<AccountModel>> {
        return try {
            val accounts = getAccountsInternal()
            AuthentyResult.Success(accounts)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageCorrupted)
        }
    }
    
    private fun getAccountsInternal(): List<AccountModel> {
        val json = sharedPreferences.getString("accounts_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<AccountModel>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    // --- DELETE ACCOUNT ---
    fun deleteAccount(accountId: Long) {
        val result = deleteAccountWithResult(accountId)
        if (result is AuthentyResult.Error) {
            throw IllegalArgumentException(result.error.message)
        }
    }
    
    /**
     * Enhanced delete account method with error handling
     */
    fun deleteAccountWithResult(accountId: Long): AuthentyResult<Unit> {
        try {
            val currentList = getAccountsInternal().toMutableList()
            val initialSize = currentList.size
            
            currentList.removeAll { it.id == accountId }
            
            // Check if account was actually found and removed
            if (currentList.size == initialSize) {
                return AuthentyResult.Error(AuthentyError.UnknownError("Account not found"))
            }
            
            return saveListWithResult(currentList)
            
        } catch (e: Exception) {
            return AuthentyResult.Error(AuthentyError.StorageError)
        }
    }
    
    /**
     * Check if account exists
     */
    fun accountExists(issuer: String, name: String): Boolean {
        return try {
            val accounts = getAccountsInternal()
            accounts.any { 
                it.issuer.equals(issuer, ignoreCase = true) && 
                it.name.equals(name, ignoreCase = true) 
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get account count
     */
    fun getAccountCount(): Int {
        return try {
            getAccountsInternal().size
        } catch (e: Exception) {
            0
        }
    }

    // Helper methods with error handling
    private fun saveList(list: List<AccountModel>) {
        val result = saveListWithResult(list)
        if (result is AuthentyResult.Error) {
            throw IllegalStateException(result.error.message)
        }
    }
    
    private fun saveListWithResult(list: List<AccountModel>): AuthentyResult<Unit> {
        return try {
            val json = gson.toJson(list)
            val editor = sharedPreferences.edit()
            editor.putString("accounts_list", json)
            val success = editor.commit() // Use commit for immediate write
            
            if (success) {
                AuthentyResult.Success(Unit)
            } else {
                AuthentyResult.Error(AuthentyError.StorageError)
            }
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }
}