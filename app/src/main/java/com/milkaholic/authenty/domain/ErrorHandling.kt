package com.milkaholic.authenty.domain

/**
 * Sealed class representing different types of errors that can occur in the app
 */
sealed class AuthentyError(val message: String, val code: String) {
    
    // Base32 related errors
    data class InvalidBase32Format(val input: String) : AuthentyError(
        "Invalid Base32 secret format. Only letters A-Z and numbers 2-7 are allowed.",
        "BASE32_INVALID"
    )
    
    data class Base32TooShort(val length: Int) : AuthentyError(
        "Secret is too short. Minimum length is 16 characters, got $length.",
        "BASE32_TOO_SHORT"
    )
    
    data class Base32TooLong(val length: Int) : AuthentyError(
        "Secret is too long. Maximum length is 128 characters, got $length.",
        "BASE32_TOO_LONG"
    )
    
    // QR Code related errors
    object QrNotFound : AuthentyError(
        "No QR code detected. Please ensure the code is clearly visible.",
        "QR_NOT_FOUND"
    )
    
    object QrInvalidFormat : AuthentyError(
        "QR code is not a valid authenticator format. Expected 'otpauth://totp/...'",
        "QR_INVALID_FORMAT"
    )
    
    data class QrMissingData(val missingField: String) : AuthentyError(
        "QR code is missing required field: $missingField",
        "QR_MISSING_DATA"
    )
    
    // Account related errors
    data class DuplicateAccount(val issuer: String, val name: String) : AuthentyError(
        "Account '$name' for '$issuer' already exists.",
        "ACCOUNT_DUPLICATE"
    )
    
    data class InvalidAccountName(val name: String) : AuthentyError(
        "Account name is invalid. Name cannot be empty or contain special characters.",
        "ACCOUNT_NAME_INVALID"
    )
    
    data class InvalidIssuer(val issuer: String) : AuthentyError(
        "Issuer name is invalid. Issuer cannot be empty.",
        "ISSUER_INVALID"
    )
    
    // Storage related errors
    object StorageError : AuthentyError(
        "Failed to save account. Please check device storage and try again.",
        "STORAGE_ERROR"
    )
    
    object StorageCorrupted : AuthentyError(
        "Account data appears to be corrupted. You may need to re-add your accounts.",
        "STORAGE_CORRUPTED"
    )
    
    // Security related errors
    object BiometricNotAvailable : AuthentyError(
        "Biometric authentication is not available on this device.",
        "BIOMETRIC_NOT_AVAILABLE"
    )
    
    object BiometricNotEnrolled : AuthentyError(
        "No biometric credentials are enrolled. Please set up fingerprint or face unlock in device settings.",
        "BIOMETRIC_NOT_ENROLLED"
    )
    
    object BiometricAuthFailed : AuthentyError(
        "Biometric authentication failed. Please try again.",
        "BIOMETRIC_AUTH_FAILED"
    )
    
    // Camera related errors
    object CameraPermissionDenied : AuthentyError(
        "Camera permission is required to scan QR codes.",
        "CAMERA_PERMISSION_DENIED"
    )
    
    object CameraUnavailable : AuthentyError(
        "Camera is not available on this device or is being used by another app.",
        "CAMERA_UNAVAILABLE"
    )
    
    // General errors
    data class UnknownError(val exception: String) : AuthentyError(
        "An unexpected error occurred: $exception",
        "UNKNOWN_ERROR"
    )
    
    data class ValidationError(val validationMessage: String) : AuthentyError(
        validationMessage,
        "VALIDATION_ERROR"
    )
    
    object NetworkRequired : AuthentyError(
        "This operation requires network access, but the app is designed to work offline.",
        "NETWORK_REQUIRED"
    )
}

/**
 * Result wrapper for operations that can fail
 */
sealed class AuthentyResult<out T> {
    data class Success<T>(val data: T) : AuthentyResult<T>()
    data class Error(val error: AuthentyError) : AuthentyResult<Nothing>()
    
    inline fun onSuccess(action: (T) -> Unit): AuthentyResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (AuthentyError) -> Unit): AuthentyResult<T> {
        if (this is Error) action(error)
        return this
    }
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun getErrorOrNull(): AuthentyError? = when (this) {
        is Success -> null
        is Error -> error
    }
}

/**
 * Validation utilities
 */
object ValidationUtils {
    
    fun validateAccountName(name: String): AuthentyError? {
        return when {
            name.isBlank() -> AuthentyError.InvalidAccountName("Name cannot be empty")
            name.length > 100 -> AuthentyError.InvalidAccountName("Name too long (max 100 characters)")
            name.contains(Regex("[<>:\"/\\|?*]")) -> AuthentyError.InvalidAccountName("Name contains invalid characters")
            else -> null
        }
    }
    
    fun validateIssuer(issuer: String): AuthentyError? {
        return when {
            issuer.isBlank() -> AuthentyError.InvalidIssuer("Issuer cannot be empty")
            issuer.length > 100 -> AuthentyError.InvalidIssuer("Issuer name too long (max 100 characters)")
            else -> null
        }
    }
    
    fun validateBase32Secret(secret: String): AuthentyError? {
        val cleanSecret = secret.trim().replace(" ", "").replace("-", "").uppercase()
        
        return when {
            cleanSecret.length < 16 -> AuthentyError.Base32TooShort(cleanSecret.length)
            cleanSecret.length > 128 -> AuthentyError.Base32TooLong(cleanSecret.length)
            !cleanSecret.matches(Regex("[A-Z2-7=]*")) -> AuthentyError.InvalidBase32Format(secret)
            else -> null
        }
    }
    
    fun validateOtpUri(uri: String): AuthentyError? {
        return when {
            !uri.startsWith("otpauth://totp/") -> AuthentyError.QrInvalidFormat
            !uri.contains("secret=") -> AuthentyError.QrMissingData("secret")
            else -> null
        }
    }
}