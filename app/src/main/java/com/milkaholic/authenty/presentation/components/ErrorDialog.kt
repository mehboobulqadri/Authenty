package com.milkaholic.authenty.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.milkaholic.authenty.domain.AuthentyError

@Composable
fun ErrorDialog(
    error: AuthentyError,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = getErrorIcon(error),
                contentDescription = "Error",
                tint = getErrorColor(error)
            )
        },
        title = {
            Text(
                text = getErrorTitle(error),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Add specific help text based on error type
                getErrorHelpText(error)?.let { helpText ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = helpText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (onRetry != null) "Cancel" else "OK")
            }
        }
    )
}

@Composable
fun ErrorSnackbar(
    error: AuthentyError,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Snackbar(
        action = {
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text("RETRY")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("DISMISS")
                }
            }
        },
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getErrorIcon(error),
                contentDescription = "Error",
                tint = getErrorColor(error),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun getErrorIcon(error: AuthentyError): ImageVector {
    return when (error) {
        is AuthentyError.InvalidBase32Format,
        is AuthentyError.Base32TooShort,
        is AuthentyError.Base32TooLong,
        is AuthentyError.QrInvalidFormat,
        is AuthentyError.QrMissingData,
        is AuthentyError.InvalidAccountName,
        is AuthentyError.InvalidIssuer,
        is AuthentyError.ValidationError -> Icons.Default.Warning
        
        is AuthentyError.StorageError,
        is AuthentyError.StorageCorrupted,
        is AuthentyError.BiometricAuthFailed,
        is AuthentyError.CameraUnavailable,
        is AuthentyError.UnknownError -> Icons.Default.Error
        
        else -> Icons.Default.Info
    }
}

@Composable
private fun getErrorColor(error: AuthentyError): androidx.compose.ui.graphics.Color {
    return when (error) {
        is AuthentyError.InvalidBase32Format,
        is AuthentyError.Base32TooShort,
        is AuthentyError.Base32TooLong,
        is AuthentyError.QrInvalidFormat,
        is AuthentyError.QrMissingData,
        is AuthentyError.InvalidAccountName,
        is AuthentyError.InvalidIssuer,
        is AuthentyError.DuplicateAccount,
        is AuthentyError.ValidationError -> MaterialTheme.colorScheme.tertiary
        
        is AuthentyError.StorageError,
        is AuthentyError.StorageCorrupted,
        is AuthentyError.BiometricAuthFailed,
        is AuthentyError.CameraUnavailable,
        is AuthentyError.UnknownError -> MaterialTheme.colorScheme.error
        
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getErrorTitle(error: AuthentyError): String {
    return when (error) {
        is AuthentyError.InvalidBase32Format,
        is AuthentyError.Base32TooShort,
        is AuthentyError.Base32TooLong -> "Invalid Secret"
        
        is AuthentyError.QrNotFound,
        is AuthentyError.QrInvalidFormat,
        is AuthentyError.QrMissingData -> "QR Code Error"
        
        is AuthentyError.DuplicateAccount -> "Duplicate Account"
        
        is AuthentyError.InvalidAccountName,
        is AuthentyError.InvalidIssuer,
        is AuthentyError.ValidationError -> "Invalid Input"
        
        is AuthentyError.StorageError,
        is AuthentyError.StorageCorrupted -> "Storage Error"
        
        is AuthentyError.BiometricNotAvailable,
        is AuthentyError.BiometricNotEnrolled,
        is AuthentyError.BiometricAuthFailed -> "Security Error"
        
        is AuthentyError.CameraPermissionDenied,
        is AuthentyError.CameraUnavailable -> "Camera Error"
        
        is AuthentyError.UnknownError -> "Unexpected Error"
        
        is AuthentyError.NetworkRequired -> "Network Error"
    }
}

private fun getErrorHelpText(error: AuthentyError): String? {
    return when (error) {
        is AuthentyError.InvalidBase32Format -> 
            "Please enter a valid Base32 secret containing only letters A-Z and numbers 2-7."
        
        is AuthentyError.Base32TooShort -> 
            "The secret must be at least 16 characters long for security."
        
        is AuthentyError.QrInvalidFormat -> 
            "Make sure you're scanning a TOTP authenticator QR code, not a different type."
        
        is AuthentyError.CameraPermissionDenied -> 
            "Go to Settings > Apps > Authenty > Permissions to enable camera access."
        
        is AuthentyError.BiometricNotEnrolled -> 
            "Set up fingerprint or face unlock in your device settings first."
        
        is AuthentyError.DuplicateAccount -> 
            "This account is already added. Each account can only be added once."
        
        is AuthentyError.ValidationError -> 
            "Please check your input and try again."
        
        else -> null
    }
}