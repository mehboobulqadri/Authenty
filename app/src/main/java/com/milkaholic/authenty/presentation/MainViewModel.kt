package com.milkaholic.authenty.presentation

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.milkaholic.authenty.data.AccountModel
import com.milkaholic.authenty.data.AccountWithProfile
import android.net.Uri
import com.milkaholic.authenty.domain.BackupManager
import com.milkaholic.authenty.domain.AuthentyError
import com.milkaholic.authenty.domain.AuthentyResult
import com.milkaholic.authenty.domain.TokenGenerator

import com.milkaholic.authenty.domain.SecurityManager

import com.milkaholic.authenty.domain.ClipboardHelper
import com.milkaholic.authenty.domain.ProfileManager

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val profileManager = ProfileManager.getInstance(application)
    private val securityManager = SecurityManager.getInstance(application)
    private val backupManager = BackupManager(application, profileManager)
    private val clipboardHelper = ClipboardHelper(application)

    // The list of accounts to show on screen (profile-aware)
    var accounts = mutableStateOf<List<AccountWithProfile>>(emptyList())
        private set

    // The current time in seconds (updates every 100ms)
    var currentTime = mutableStateOf(System.currentTimeMillis())
        private set

    // The progress bar value (0.0 to 1.0) - Deprecated, use currentTime
    var progress = mutableFloatStateOf(0f)
        private set

    // Error state management
    var currentError = mutableStateOf<AuthentyError?>(null)
        private set
    
    var showErrorDialog = mutableStateOf(false)
        private set

    // Theme state
    var isDarkMode = mutableStateOf(true) // Default to Dark Mode
        private set

    // Loading states
    var isAddingAccount = mutableStateOf(false)
        private set
    
    var isDeletingAccount = mutableStateOf<Long?>(null)
        private set

    // Search and filter state
    var searchQuery = mutableStateOf("")
        private set
    
    var filteredAccounts = mutableStateOf<List<AccountWithProfile>>(emptyList())
        private set
    
    var selectedCategory = mutableStateOf<String?>(null)
        private set
    
    // Reorder mode state
    var isReorderMode = mutableStateOf(false)
        private set

    init {
        // Observe profile changes to automatically reload accounts when profile switches
        viewModelScope.launch {
            profileManager.currentProfile.collect {
                if (!securityManager.isDuressMode.value) {
                    loadAccounts()
                }
            }
        }
        
        // Observe Duress Mode
        viewModelScope.launch {
            securityManager.isDuressMode.collect { isDuress ->
                if (isDuress) {
                    // Clear accounts from UI immediately to simulate empty state
                    accounts.value = emptyList()
                    filteredAccounts.value = emptyList()
                } else {
                    loadAccounts()
                }
            }
        }
        
        startTimer()
    }

    // --- Error Management ---
    fun showError(error: AuthentyError) {
        currentError.value = error
        showErrorDialog.value = true
    }
    
    fun dismissError() {
        currentError.value = null
        showErrorDialog.value = false
    }
    
    fun clearError() {
        currentError.value = null
        showErrorDialog.value = false
    }

    // --- Theme Management ---
    fun toggleTheme(isDark: Boolean) {
        isDarkMode.value = isDark
    }

    // --- Account Management ---
    fun loadAccounts() {
        if (securityManager.isDuressMode.value) {
            accounts.value = emptyList()
            filteredAccounts.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                val loadedAccounts = withContext(Dispatchers.IO) {
                    profileManager.getAccountsForCurrentProfile()
                }
                accounts.value = loadedAccounts
                performSearch() // Refresh search results
                clearError()
            } catch (e: Exception) {
                showError(AuthentyError.StorageError)
            }
        }
    }

    /**
     * Enhanced add account with proper error handling
     */
    fun addAccount(
        name: String, 
        issuer: String, 
        secret: String, 
        category: String = "Personal",
        algorithm: String = "SHA1",
        digits: Int = 6,
        period: Int = 30
    ) {
        viewModelScope.launch {
            isAddingAccount.value = true
            
            val newAccount = AccountModel(
                name = name.trim(),
                issuer = issuer.trim(),
                secret = secret.trim(),
                algorithm = algorithm,
                digits = digits,
                period = period
            )
            
            val result = withContext(Dispatchers.IO) {
                profileManager.addAccountToCurrentProfile(newAccount, category)
            }
            
            result.onSuccess {
                loadAccounts() // Reload accounts on success
                clearError()
            }.onError { error ->
                showError(error)
            }
            
            isAddingAccount.value = false
        }
    }
    
    /**
     * Legacy add account method for backward compatibility (different parameter order)
     */
    fun addAccountLegacy(issuer: String, name: String, secret: String) {
        addAccount(name, issuer, secret)
    }

    /**
     * Enhanced delete account with proper error handling
     */
    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            isDeletingAccount.value = id
            
            val result = withContext(Dispatchers.IO) {
                profileManager.deleteAccountFromCurrentProfile(id)
            }
            
            result.onSuccess {
                loadAccounts() // Reload accounts on success
                clearError()
            }.onError { error ->
                showError(error)
            }
            
            isDeletingAccount.value = null
        }
    }
    
    /**
     * Check if account already exists in current profile
     */
    fun accountExists(issuer: String, name: String): Boolean {
        return accounts.value.any { 
            it.account.issuer.equals(issuer, ignoreCase = true) && 
            it.account.name.equals(name, ignoreCase = true) 
        }
    }
    
    /**
     * Get account count for current profile
     */
    fun getAccountCount(): Int {
        return accounts.value.size
    }
    
    /**
     * Get accounts by category
     */
    fun getAccountsByCategory(category: String): List<AccountWithProfile> {
        return accounts.value.filter { it.category == category }
    }
    
    /**
     * Update account category
     */
    fun updateAccountCategory(accountId: Long, newCategory: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                profileManager.updateAccountCategory(accountId, newCategory)
            }
            result.onSuccess {
                loadAccounts() // Reload to reflect changes
            }.onError { error ->
                showError(error)
            }
        }
    }

    fun updateBackupCodes(accountId: Long, backupCodes: List<String>) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                profileManager.updateAccountBackupCodes(accountId, backupCodes)
            }
            result.onSuccess {
                loadAccounts()
            }.onError { error ->
                showError(error)
            }
        }
    }
    
    /**
     * Search and filter functionality
     */
    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        performSearch()
    }
    
    fun filterByCategory(category: String?) {
        selectedCategory.value = category
        performSearch()
    }
    
    fun clearFilters() {
        searchQuery.value = ""
        selectedCategory.value = null
        performSearch()
    }
    
    private fun performSearch() {
        viewModelScope.launch {
            val baseAccounts = accounts.value
            var result = baseAccounts
            
            // Apply search query filter
            if (searchQuery.value.isNotBlank()) {
                // Search might be heavy if many accounts, move to IO
                result = withContext(Dispatchers.IO) {
                    profileManager.searchAccounts(searchQuery.value)
                }
            }
            
            // Apply category filter
            selectedCategory.value?.let { category ->
                result = result.filter { it.category == category }
            }
            
            filteredAccounts.value = result
        }
    }
    
    /**
     * Get available categories for filtering
     */
    fun getAvailableCategories(): List<String> {
        return accounts.value.map { it.category }.distinct().sorted()
    }
    
    /**
     * Get accounts to display (filtered or all)
     */
    fun getDisplayAccounts(): List<AccountWithProfile> {
        return if (searchQuery.value.isNotBlank() || selectedCategory.value != null) {
            filteredAccounts.value
        } else {
            accounts.value
        }
    }
    
    /**
     * Reorder accounts functionality
     */
    fun reorderAccounts(accountIds: List<Long>) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                profileManager.reorderAccounts(accountIds)
            }
            result.onSuccess {
                loadAccounts() // Reload to reflect new order
            }.onError { error ->
                showError(error)
            }
        }
    }
    
    /**
     * Toggle reorder mode
     */
    fun toggleReorderMode() {
        isReorderMode.value = !isReorderMode.value
        // Clear search/filters when entering reorder mode
        if (isReorderMode.value) {
            clearFilters()
        }
    }

    // Updates the timer and forces UI to refresh every 100ms
    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                currentTime.value = System.currentTimeMillis()
                progress.floatValue = TokenGenerator.getProgress() // Keep for backward compatibility
                delay(100) // Update 10 times a second for smooth animation
            }
        }
    }
    
    /**
     * Retry last failed operation (if applicable)
     */
    fun retryLastOperation() {
        // For now, just reload accounts
        loadAccounts()
        dismissError()
    }

    // --- Backup & Restore ---
    fun exportBackup(password: String, uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.exportBackup(password, uri)
            if (result is AuthentyResult.Error) {
                showError(result.error)
            }
        }
    }

    fun importBackup(password: String, uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.importBackup(password, uri)
            if (result is AuthentyResult.Success) {
                loadAccounts()
            } else if (result is AuthentyResult.Error) {
                showError(result.error)
            }
        }
    }

    fun copyToClipboard(text: String, label: String) {
        clipboardHelper.copyToClipboard(text, label)
    }
}