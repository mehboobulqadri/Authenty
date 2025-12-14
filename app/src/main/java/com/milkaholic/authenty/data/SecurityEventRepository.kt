package com.milkaholic.authenty.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.milkaholic.authenty.domain.AuthentyError
import com.milkaholic.authenty.domain.AuthentyResult
import java.util.concurrent.TimeUnit

class SecurityEventRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securityPrefs = EncryptedSharedPreferences.create(
        context,
        "security_events_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val gson = Gson()

    companion object {
        private const val EVENTS_KEY = "security_events"
        private const val MAX_EVENTS = 1000 // Keep last 1000 events
        private const val CLEANUP_THRESHOLD = 1200 // Cleanup when reaching this
        private const val RETENTION_DAYS = 30L // Keep events for 30 days
    }

    fun logEvent(event: SecurityEvent): AuthentyResult<Unit> {
        return try {
            val events = getEventsInternal().toMutableList()
            events.add(event)

            // Auto-cleanup if needed
            val cleanedEvents = if (events.size > CLEANUP_THRESHOLD) {
                cleanupOldEvents(events)
            } else {
                events
            }

            saveEventsInternal(cleanedEvents)
            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun logAuthenticationAttempt(
        isSuccessful: Boolean,
        method: String,
        details: Map<String, String> = emptyMap()
    ): AuthentyResult<Unit> {
        val eventType = when {
            isSuccessful && method == "biometric" -> SecurityEventType.BIOMETRIC_AUTH_SUCCESS
            !isSuccessful && method == "biometric" -> SecurityEventType.BIOMETRIC_AUTH_FAILURE
            isSuccessful && method == "pin" -> SecurityEventType.PIN_AUTH_SUCCESS
            !isSuccessful && method == "pin" -> SecurityEventType.PIN_AUTH_FAILURE
            else -> SecurityEventType.SUSPICIOUS_ACTIVITY
        }

        val severity = if (isSuccessful) SecurityEventSeverity.INFO else SecurityEventSeverity.MEDIUM
        val description = "${method.capitalize()} authentication ${if (isSuccessful) "successful" else "failed"}"

        val event = SecurityEvent(
            type = eventType,
            description = description,
            severity = severity,
            metadata = details + mapOf("method" to method, "success" to isSuccessful.toString())
        )

        return logEvent(event)
    }

    fun logAccountModification(
        action: String,
        accountName: String,
        issuer: String
    ): AuthentyResult<Unit> {
        val eventType = when (action.lowercase()) {
            "add", "added" -> SecurityEventType.ACCOUNT_ADDED
            "delete", "deleted", "remove", "removed" -> SecurityEventType.ACCOUNT_DELETED
            else -> SecurityEventType.ACCOUNT_MODIFIED
        }

        val event = SecurityEvent(
            type = eventType,
            description = "Account $action: $issuer ($accountName)",
            severity = SecurityEventSeverity.INFO,
            metadata = mapOf(
                "action" to action,
                "account_name" to accountName,
                "issuer" to issuer
            )
        )

        return logEvent(event)
    }

    fun logSecurityThreat(
        threatType: SecurityEventType,
        description: String,
        metadata: Map<String, String> = emptyMap()
    ): AuthentyResult<Unit> {
        val event = SecurityEvent(
            type = threatType,
            description = description,
            severity = SecurityEventSeverity.HIGH,
            metadata = metadata
        )

        return logEvent(event)
    }

    fun getAllEvents(): AuthentyResult<List<SecurityEvent>> {
        return try {
            val events = getEventsInternal()
            AuthentyResult.Success(events.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageCorrupted)
        }
    }

    fun getRecentEvents(hours: Int = 24): AuthentyResult<List<SecurityEvent>> {
        return try {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours.toLong())
            val events = getEventsInternal()
                .filter { it.timestamp >= cutoffTime }
                .sortedByDescending { it.timestamp }
            
            AuthentyResult.Success(events)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageCorrupted)
        }
    }

    fun getEventsByType(type: SecurityEventType): AuthentyResult<List<SecurityEvent>> {
        return try {
            val events = getEventsInternal()
                .filter { it.type == type }
                .sortedByDescending { it.timestamp }
            
            AuthentyResult.Success(events)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageCorrupted)
        }
    }

    fun getSecuritySummary(): AuthentyResult<SecuritySummary> {
        return try {
            val events = getEventsInternal()
            val recentEvents = events.filter { 
                it.timestamp >= System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
            }

            val successfulAuth = events.count { 
                it.type in listOf(
                    SecurityEventType.BIOMETRIC_AUTH_SUCCESS,
                    SecurityEventType.PIN_AUTH_SUCCESS
                )
            }

            val failedAuth = events.count { 
                it.type in listOf(
                    SecurityEventType.BIOMETRIC_AUTH_FAILURE,
                    SecurityEventType.PIN_AUTH_FAILURE
                )
            }

            val securityThreats = events.count { it.isSecurityThreat() }

            val lastLogin = events
                .filter { 
                    it.type in listOf(
                        SecurityEventType.BIOMETRIC_AUTH_SUCCESS,
                        SecurityEventType.PIN_AUTH_SUCCESS
                    )
                }
                .maxByOrNull { it.timestamp }?.timestamp

            val recentFailures = recentEvents.count { 
                it.type in listOf(
                    SecurityEventType.BIOMETRIC_AUTH_FAILURE,
                    SecurityEventType.PIN_AUTH_FAILURE
                )
            }

            val isCompromised = events.any { 
                it.type in listOf(
                    SecurityEventType.ROOT_DETECTED,
                    SecurityEventType.TAMPER_DETECTED
                ) && it.timestamp >= System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            }

            val summary = SecuritySummary(
                totalEvents = events.size,
                successfulAuthentications = successfulAuth,
                failedAuthentications = failedAuth,
                securityThreats = securityThreats,
                lastLoginTimestamp = lastLogin,
                recentFailures = recentFailures,
                isDeviceCompromised = isCompromised
            )

            AuthentyResult.Success(summary)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageCorrupted)
        }
    }

    fun getFailedAttemptsCount(timeWindowMinutes: Int = 30): Int {
        return try {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(timeWindowMinutes.toLong())
            getEventsInternal().count { event ->
                event.timestamp >= cutoffTime && event.type in listOf(
                    SecurityEventType.BIOMETRIC_AUTH_FAILURE,
                    SecurityEventType.PIN_AUTH_FAILURE
                )
            }
        } catch (e: Exception) {
            0
        }
    }

    fun clearOldEvents(): AuthentyResult<Unit> {
        return try {
            val events = getEventsInternal()
            val cleanedEvents = cleanupOldEvents(events)
            saveEventsInternal(cleanedEvents)
            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun clearAllEvents(): AuthentyResult<Unit> {
        return try {
            saveEventsInternal(emptyList())
            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    private fun getEventsInternal(): List<SecurityEvent> {
        val json = securityPrefs.getString(EVENTS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<SecurityEvent>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun saveEventsInternal(events: List<SecurityEvent>) {
        val json = gson.toJson(events)
        val editor = securityPrefs.edit()
        editor.putString(EVENTS_KEY, json)
        editor.apply()
    }

    private fun cleanupOldEvents(events: List<SecurityEvent>): List<SecurityEvent> {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RETENTION_DAYS)
        
        return events
            .filter { it.timestamp >= cutoffTime }
            .sortedByDescending { it.timestamp }
            .take(MAX_EVENTS)
    }
}