package com.milkaholic.authenty.presentation

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    // Allow Fingerprint, Face, OR Device PIN/Pattern (Like WhatsApp)
    private const val AUTH_TYPES = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(AUTH_TYPES) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // If user cancels (errorCode 13) or hardware error, trigger error callback
                // But ignore "Canceled by system" to prevent loops
                if (errorCode != BiometricPrompt.ERROR_CANCELED) {
                    onError()
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Authenty")
            .setSubtitle("Use Fingerprint or PIN")
            .setAllowedAuthenticators(AUTH_TYPES)
            // Note: When using DEVICE_CREDENTIAL, we cannot set a Negative Button (Cancel)
            // The OS handles the back button automatically.
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }
}