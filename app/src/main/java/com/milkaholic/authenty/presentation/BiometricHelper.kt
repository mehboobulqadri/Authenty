package com.milkaholic.authenty.presentation

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.milkaholic.authenty.data.PinManager
import com.milkaholic.authenty.data.PinVerificationResult
import com.milkaholic.authenty.domain.AuthentyResult
import com.milkaholic.authenty.domain.AutoLockManager
import com.milkaholic.authenty.domain.SecurityManager

class BiometricHelper private constructor(
    private val context: Context,
    private val securityManager: SecurityManager,
    private val autoLockManager: AutoLockManager,
    private val pinManager: PinManager
) {

    companion object {
        @Volatile
        private var INSTANCE: BiometricHelper? = null

        fun getInstance(
            context: Context,
            securityManager: SecurityManager,
            autoLockManager: AutoLockManager,
            pinManager: PinManager
        ): BiometricHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BiometricHelper(
                    context.applicationContext,
                    securityManager,
                    autoLockManager,
                    pinManager
                ).also { INSTANCE = it }
            }
        }

        // Allow Fingerprint, Face, OR Device PIN/Pattern
        private const val AUTH_TYPES = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(AUTH_TYPES) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isPinAvailable(): Boolean {
        return pinManager.isPinSet() || pinManager.isBackupPinSet()
    }

    fun shouldUseBiometric(): Boolean {
        return isBiometricAvailable() && !autoLockManager.isInLockout()
    }

    fun shouldUsePinFallback(): Boolean {
        return isPinAvailable() && (!isBiometricAvailable() || autoLockManager.isInLockout())
    }

    fun showAuthenticationPrompt(
        activity: FragmentActivity,
        onSuccess: (method: String) -> Unit,
        onError: (error: String?) -> Unit,
        allowPinFallback: Boolean = true
    ) {
        if (autoLockManager.isInLockout()) {
            val remainingTime = autoLockManager.getLockoutTimeRemaining()
            val minutes = (remainingTime / 60000).toInt()
            val seconds = ((remainingTime % 60000) / 1000).toInt()
            onError("Too many failed attempts. Try again in ${minutes}m ${seconds}s")
            return
        }

        if (shouldUseBiometric()) {
            showBiometricPrompt(activity, onSuccess, onError, allowPinFallback)
        } else if (shouldUsePinFallback() && allowPinFallback) {
            onError("pin_fallback_needed") // Signal to show PIN entry
        } else {
            securityManager.logAuthenticationFailure(
                "none", 
                mapOf("reason" to "no_auth_methods_available")
            )
            onError("No authentication methods available")
        }
    }

    fun verifyPin(pin: String): AuthentyResult<Boolean> {
        val result = pinManager.verifyAnyPin(pin)
        
        return when (result) {
            is AuthentyResult.Success -> {
                when (result.data) {
                    PinVerificationResult.PRIMARY_PIN_VALID -> {
                        securityManager.logAuthenticationSuccess(
                            "pin",
                            mapOf("type" to "primary")
                        )
                        autoLockManager.recordSuccessfulAuth()
                        AuthentyResult.Success(true)
                    }
                    PinVerificationResult.BACKUP_PIN_VALID -> {
                        securityManager.logAuthenticationSuccess(
                            "pin",
                            mapOf("type" to "backup")
                        )
                        autoLockManager.recordSuccessfulAuth()
                        AuthentyResult.Success(true)
                    }
                    PinVerificationResult.DURESS_PIN_VALID -> {
                        securityManager.triggerDuressMode()
                        // Log as success to avoid lockout, but trigger duress mode
                        securityManager.logAuthenticationSuccess(
                            "pin",
                            mapOf("type" to "duress")
                        )
                        autoLockManager.recordSuccessfulAuth()
                        AuthentyResult.Success(true)
                    }
                    PinVerificationResult.INVALID -> {
                        securityManager.logAuthenticationFailure(
                            "pin",
                            mapOf("reason" to "invalid_pin")
                        )
                        autoLockManager.recordFailedAttempt()
                        AuthentyResult.Success(false)
                    }
                }
            }
            is AuthentyResult.Error -> {
                securityManager.logAuthenticationFailure(
                    "pin",
                    mapOf("reason" to "verification_error", "error" to result.error.message)
                )
                autoLockManager.recordFailedAttempt()
                result
            }
        }
    }

    private fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: (method: String) -> Unit,
        onError: (error: String?) -> Unit,
        allowPinFallback: Boolean
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                
                val authType = when {
                    result.authenticationType == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC -> "biometric"
                    result.authenticationType == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL -> "device_credential"
                    else -> "unknown"
                }
                
                securityManager.logAuthenticationSuccess(authType)
                autoLockManager.recordSuccessfulAuth()
                onSuccess(authType)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                
                val isCancellation = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON

                if (!isCancellation) {
                    securityManager.logAuthenticationFailure(
                        "biometric",
                        mapOf(
                            "error_code" to errorCode.toString(),
                            "error_message" to errString.toString()
                        )
                    )
                    
                    if (errorCode != BiometricPrompt.ERROR_CANCELED) {
                        autoLockManager.recordFailedAttempt()
                    }
                }

                when (errorCode) {
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        if (allowPinFallback && isPinAvailable()) {
                            onError("pin_fallback_needed")
                        } else {
                            onError("Biometric authentication locked. ${errString}")
                        }
                    }
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                        if (allowPinFallback && isPinAvailable()) {
                            onError("pin_fallback_needed")
                        } else {
                            onError("No biometric credentials enrolled")
                        }
                    }
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> {
                        onError(null) // User cancellation, no error message
                    }
                    else -> {
                        onError(errString.toString())
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Biometric recognized but not a match.
                // The OS will handle this, we just log it
                securityManager.logAuthenticationFailure(
                    "biometric",
                    mapOf("reason" to "biometric_not_recognized")
                )
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Authenty")
            .setSubtitle("Use your biometric or device credential")
            .setAllowedAuthenticators(AUTH_TYPES)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }
}