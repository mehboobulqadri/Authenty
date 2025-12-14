package com.milkaholic.authenty.data

import java.text.SimpleDateFormat
import java.util.*

enum class SecurityEventType {
    BIOMETRIC_AUTH_SUCCESS,
    BIOMETRIC_AUTH_FAILURE,
    PIN_AUTH_SUCCESS,
    PIN_AUTH_FAILURE,
    ACCOUNT_ADDED,
    ACCOUNT_DELETED,
    ACCOUNT_MODIFIED,
    APP_LOCKED,
    APP_UNLOCKED,
    ROOT_DETECTED,
    TAMPER_DETECTED,
    SECURITY_BREACH_DETECTED,
    AUTO_LOCK_TRIGGERED,
    PROGRESSIVE_LOCKOUT,
    SETTINGS_MODIFIED,
    BACKUP_CREATED,
    BACKUP_RESTORED,
    APP_INSTALL_VERIFIED,
    SUSPICIOUS_ACTIVITY
}

data class SecurityEvent(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: SecurityEventType,
    val description: String,
    val severity: SecurityEventSeverity = SecurityEventSeverity.INFO,
    val metadata: Map<String, String> = emptyMap()
) {
    fun getFormattedTimestamp(): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(date)
    }
    
    fun getSeverityColor(): String {
        return when (severity) {
            SecurityEventSeverity.LOW -> "#4CAF50"      // Green
            SecurityEventSeverity.INFO -> "#2196F3"     // Blue
            SecurityEventSeverity.MEDIUM -> "#FF9800"   // Orange
            SecurityEventSeverity.HIGH -> "#F44336"     // Red
            SecurityEventSeverity.CRITICAL -> "#E91E63" // Pink
        }
    }
    
    fun isSecurityThreat(): Boolean {
        return severity in listOf(SecurityEventSeverity.HIGH, SecurityEventSeverity.CRITICAL) ||
                type in listOf(
                    SecurityEventType.ROOT_DETECTED,
                    SecurityEventType.TAMPER_DETECTED,
                    SecurityEventType.SECURITY_BREACH_DETECTED,
                    SecurityEventType.SUSPICIOUS_ACTIVITY
                )
    }
}

enum class SecurityEventSeverity {
    LOW,        // Normal operations
    INFO,       // Informational events
    MEDIUM,     // Failed attempts
    HIGH,       // Security concerns
    CRITICAL    // Immediate threats
}

data class SecuritySummary(
    val totalEvents: Int,
    val successfulAuthentications: Int,
    val failedAuthentications: Int,
    val securityThreats: Int,
    val lastLoginTimestamp: Long?,
    val recentFailures: Int,
    val isDeviceCompromised: Boolean
)