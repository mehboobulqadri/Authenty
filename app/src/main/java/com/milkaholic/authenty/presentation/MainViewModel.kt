package com.milkaholic.authenty.presentation

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.milkaholic.authenty.data.AccountModel
import com.milkaholic.authenty.data.SecureRepository
import com.milkaholic.authenty.domain.TokenGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SecureRepository(application)

    // The list of accounts to show on screen
    var accounts = mutableStateOf<List<AccountModel>>(emptyList())
        private set

    // The progress bar value (0.0 to 1.0)
    var progress = mutableFloatStateOf(0f)
        private set

    init {
        loadAccounts()
        startTimer()
    }

    fun loadAccounts() {
        accounts.value = repository.getAccounts()
    }

    fun addAccount(name: String, issuer: String, secret: String) {
        try {
            // Validate that the secret is valid Base32
            // If this fails, it throws an exception and goes to 'catch'
            com.milkaholic.authenty.domain.Base32.decode(secret)

            val newAccount = AccountModel(
                name = name,
                issuer = issuer,
                secret = secret
            )
            repository.addAccount(newAccount)
            loadAccounts()
        } catch (e: Exception) {
            // In a real app, we would show an error message here
            e.printStackTrace()
        }
    }

    fun deleteAccount(id: Long) {
        repository.deleteAccount(id)
        loadAccounts()
    }

    // Updates the progress bar and forces UI to refresh every 100ms
    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                progress.floatValue = TokenGenerator.getProgress()
                delay(100) // Update 10 times a second for smooth animation
            }
        }
    }
}