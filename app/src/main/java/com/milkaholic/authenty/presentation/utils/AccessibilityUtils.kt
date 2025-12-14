package com.milkaholic.authenty.presentation.utils

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp

/**
 * Accessibility utilities for improving app usability with screen readers and assistive technologies
 */
object AccessibilityUtils {

    /**
     * Minimum touch target size for accessibility (48dp x 48dp)
     */
    val MINIMUM_TOUCH_TARGET = 48.dp

    /**
     * Standard content padding for accessibility
     */
    val CONTENT_PADDING = 16.dp

    /**
     * Extended touch target modifier
     */
    fun Modifier.minimumTouchTarget(): Modifier = this.padding(8.dp)

    /**
     * Create content description for TOTP codes with expiry information
     */
    fun getTotpContentDescription(
        issuer: String,
        accountName: String,
        totpCode: String,
        secondsRemaining: Int
    ): String {
        return "Authentication code for $accountName at $issuer. " +
                "Code is ${totpCode.chunked(3).joinToString(" ")}. " +
                "Expires in $secondsRemaining seconds. Tap to copy."
    }

    /**
     * Create content description for progress indicators
     */
    fun getProgressContentDescription(progress: Float): String {
        val secondsRemaining = (progress * 30).toInt()
        return "Time remaining: $secondsRemaining seconds until code expires"
    }

    /**
     * Create content description for account cards
     */
    fun getAccountCardDescription(
        issuer: String,
        accountName: String,
        position: Int,
        total: Int
    ): String {
        return "Account $position of $total. $issuer account for $accountName"
    }

    /**
     * Create content description for error states
     */
    fun getErrorContentDescription(errorMessage: String): String {
        return "Error: $errorMessage"
    }

    /**
     * Create content description for biometric prompt
     */
    fun getBiometricContentDescription(): String {
        return "Authenticate using fingerprint, face, or device credentials to access your authenticator codes"
    }

    /**
     * Live region announcement for screen readers
     */
    fun Modifier.announceForAccessibility(announcement: String): Modifier {
        return this.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = announcement
        }
    }

    /**
     * Mark content as heading for screen reader navigation
     */
    fun Modifier.accessibilityHeading(): Modifier {
        return this.semantics {
            heading()
        }
    }

    /**
     * Group related content for screen readers
     */
    fun Modifier.accessibilityGroup(description: String): Modifier {
        return this.semantics(mergeDescendants = true) {
            contentDescription = description
        }
    }

    /**
     * Mark interactive elements with proper roles
     */
    fun Modifier.accessibilityButton(label: String): Modifier {
        return this.semantics {
            role = Role.Button
            contentDescription = label
        }
    }

    /**
     * Mark toggle elements (like switches)
     */
    fun Modifier.accessibilityToggle(label: String, isChecked: Boolean): Modifier {
        return this.semantics {
            role = Role.Switch
            contentDescription = label
            toggleableState = if (isChecked) ToggleableState.On else ToggleableState.Off
        }
    }

    /**
     * Mark input fields with proper labels
     */
    fun Modifier.accessibilityTextField(label: String, value: String, error: String? = null): Modifier {
        return this.semantics {
            contentDescription = if (error != null) {
                "$label. Current value: $value. Error: $error"
            } else {
                "$label. Current value: $value"
            }
        }
    }

    /**
     * Create announcement for TOTP code copy action
     */
    fun getCopyCodeAnnouncement(issuer: String): String {
        return "Authentication code for $issuer copied to clipboard"
    }

    /**
     * Create announcement for account addition
     */
    fun getAccountAddedAnnouncement(issuer: String, accountName: String): String {
        return "Account added successfully. $issuer account for $accountName"
    }

    /**
     * Create announcement for account deletion
     */
    fun getAccountDeletedAnnouncement(issuer: String, accountName: String): String {
        return "Account deleted. $issuer account for $accountName removed"
    }

    /**
     * Create announcement for theme change
     */
    fun getThemeChangeAnnouncement(isDark: Boolean): String {
        return if (isDark) "Switched to dark mode" else "Switched to light mode"
    }

    /**
     * Create announcement for QR scan success
     */
    fun getQrScanSuccessAnnouncement(): String {
        return "QR code scanned successfully. Account details detected"
    }

    /**
     * Create announcement for QR scan failure
     */
    fun getQrScanFailureAnnouncement(): String {
        return "QR code scan failed. Please ensure the code is clearly visible and try again"
    }
}

/**
 * Custom modifier for consistent accessibility improvements
 */
@Composable
fun Modifier.accessibilityEnhanced(
    description: String? = null,
    isButton: Boolean = false,
    isHeading: Boolean = false,
    role: Role? = null
): Modifier {
    return this.semantics {
        description?.let { contentDescription = it }
        if (isButton) this.role = Role.Button
        if (isHeading) heading()
        role?.let { this.role = it }
    }
}