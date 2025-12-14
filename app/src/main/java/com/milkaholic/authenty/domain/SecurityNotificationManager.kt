package com.milkaholic.authenty.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.milkaholic.authenty.MainActivity
import com.milkaholic.authenty.R
import com.milkaholic.authenty.data.SecurityEvent
import com.milkaholic.authenty.data.SecurityEventSeverity
import com.milkaholic.authenty.data.SecurityEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SecurityNotificationManager private constructor(
    private val context: Context,
    private val securityManager: SecurityManager
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        @Volatile
        private var INSTANCE: SecurityNotificationManager? = null

        fun getInstance(context: Context, securityManager: SecurityManager): SecurityNotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityNotificationManager(context.applicationContext, securityManager).also { INSTANCE = it }
            }
        }

        private const val CHANNEL_SECURITY_ALERTS = "security_alerts"
        private const val CHANNEL_LOCKOUT_NOTICES = "lockout_notices"
        private const val CHANNEL_BREACH_WARNINGS = "breach_warnings"
        
        private const val NOTIFICATION_SECURITY_ALERT = 1001
        private const val NOTIFICATION_LOCKOUT = 1002
        private const val NOTIFICATION_BREACH = 1003
        
        private const val MAX_NOTIFICATIONS_PER_HOUR = 5
        private const val NOTIFICATION_COOLDOWN_MS = 60 * 60 * 1000L // 1 hour
    }

    private var lastNotificationTime: Long = 0
    private var notificationCountThisHour: Int = 0

    fun initialize() {
        createNotificationChannels()
        
        scope.launch {
            monitorSecurityEvents()
        }
    }

    fun showSecurityBreachAlert(
        title: String,
        message: String,
        severity: SecurityEventSeverity = SecurityEventSeverity.HIGH
    ) {
        if (!shouldShowNotification(severity)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BREACH_WARNINGS)
            .setSmallIcon(R.drawable.ic_notification) // You'll need to add this
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(when (severity) {
                SecurityEventSeverity.CRITICAL -> NotificationCompat.PRIORITY_MAX
                SecurityEventSeverity.HIGH -> NotificationCompat.PRIORITY_HIGH
                SecurityEventSeverity.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
                else -> NotificationCompat.PRIORITY_LOW
            })
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_BREACH, notification)
            recordNotificationSent()
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }

    fun showLockoutNotification(
        attemptCount: Int,
        lockoutDuration: Long
    ) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(lockoutDuration)
        
        val title = "Account Temporarily Locked"
        val message = "Too many failed authentication attempts ($attemptCount). " +
                "Try again in $minutes minute${if (minutes != 1L) "s" else ""}."

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_LOCKOUT_NOTICES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_LOCKOUT, notification)
            recordNotificationSent()
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }

    fun showRootDetectionWarning() {
        val title = "Security Warning: Root Access Detected"
        val message = "Your device appears to be rooted. This may compromise the security of your authentication codes. Consider using Authenty on a non-rooted device for maximum security."

        showSecurityBreachAlert(title, message, SecurityEventSeverity.CRITICAL)
    }

    fun showTamperDetectionWarning() {
        val title = "Security Warning: App Tampering Detected"
        val message = "The app's integrity has been compromised. For your security, please reinstall Authenty from a trusted source."

        showSecurityBreachAlert(title, message, SecurityEventSeverity.CRITICAL)
    }

    fun showSuspiciousActivityAlert(details: String) {
        val title = "Suspicious Activity Detected"
        val message = "Unusual activity has been detected: $details. Please review your recent security events."

        showSecurityBreachAlert(title, message, SecurityEventSeverity.HIGH)
    }

    fun showMultipleFailedAttemptsWarning(attemptCount: Int) {
        if (attemptCount < 3) return // Only warn after multiple attempts
        
        val title = "Security Alert: Multiple Failed Attempts"
        val message = "$attemptCount failed authentication attempts detected. If this wasn't you, your account may be under attack."

        showSecurityBreachAlert(title, message, SecurityEventSeverity.MEDIUM)
    }

    fun clearAllNotifications() {
        try {
            notificationManager.cancelAll()
        } catch (e: Exception) {
            // Handle gracefully
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_SECURITY_ALERTS,
                    "Security Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General security notifications"
                },
                
                NotificationChannel(
                    CHANNEL_LOCKOUT_NOTICES,
                    "Account Lockouts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications about account lockouts"
                },
                
                NotificationChannel(
                    CHANNEL_BREACH_WARNINGS,
                    "Security Breaches",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical security breach warnings"
                    enableVibration(true)
                    enableLights(true)
                }
            )

            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { channel ->
                systemNotificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun shouldShowNotification(severity: SecurityEventSeverity): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Reset counter if more than an hour has passed
        if (currentTime - lastNotificationTime > NOTIFICATION_COOLDOWN_MS) {
            notificationCountThisHour = 0
        }

        // Always allow critical notifications
        if (severity == SecurityEventSeverity.CRITICAL) {
            return true
        }

        // Check rate limiting for other notifications
        return notificationCountThisHour < MAX_NOTIFICATIONS_PER_HOUR
    }

    private fun recordNotificationSent() {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastNotificationTime > NOTIFICATION_COOLDOWN_MS) {
            notificationCountThisHour = 0
        }
        
        notificationCountThisHour++
        lastNotificationTime = currentTime
    }

    private suspend fun monitorSecurityEvents() {
        // This would typically use a Flow or LiveData to observe security events
        // For now, we'll implement a basic monitoring system
        
        var lastEventId = 0L
        
        while (true) {
            try {
                val eventsResult = securityManager.getAllSecurityEvents()
                
                if (eventsResult is AuthentyResult.Success) {
                    val newEvents = eventsResult.data.filter { it.id > lastEventId }
                    
                    newEvents.forEach { event ->
                        handleSecurityEvent(event)
                    }
                    
                    if (newEvents.isNotEmpty()) {
                        lastEventId = newEvents.maxOf { it.id }
                    }
                }
                
                // Check every 30 seconds
                kotlinx.coroutines.delay(30000)
                
            } catch (e: Exception) {
                // Handle gracefully and continue monitoring
                kotlinx.coroutines.delay(60000) // Wait longer if there's an error
            }
        }
    }

    private fun handleSecurityEvent(event: SecurityEvent) {
        when (event.type) {
            SecurityEventType.ROOT_DETECTED -> {
                showRootDetectionWarning()
            }
            
            SecurityEventType.TAMPER_DETECTED -> {
                showTamperDetectionWarning()
            }
            
            SecurityEventType.SECURITY_BREACH_DETECTED -> {
                showSuspiciousActivityAlert(event.description)
            }
            
            SecurityEventType.PROGRESSIVE_LOCKOUT -> {
                val attempts = event.metadata["failed_attempts"]?.toIntOrNull() ?: 0
                val duration = event.metadata["lockout_duration_ms"]?.toLongOrNull() ?: 0
                
                if (attempts > 0 && duration > 0) {
                    showLockoutNotification(attempts, duration)
                }
            }
            
            SecurityEventType.BIOMETRIC_AUTH_FAILURE,
            SecurityEventType.PIN_AUTH_FAILURE -> {
                // Check recent failed attempts
                scope.launch {
                    val recentEventsResult = securityManager.getRecentSecurityEvents(1)
                    if (recentEventsResult is AuthentyResult.Success) {
                        val failedAttempts = recentEventsResult.data.count { 
                            it.type in listOf(
                                SecurityEventType.BIOMETRIC_AUTH_FAILURE,
                                SecurityEventType.PIN_AUTH_FAILURE
                            )
                        }
                        
                        if (failedAttempts >= 3) {
                            showMultipleFailedAttemptsWarning(failedAttempts)
                        }
                    }
                }
            }
            
            SecurityEventType.SUSPICIOUS_ACTIVITY -> {
                showSuspiciousActivityAlert(event.description)
            }
            
            else -> {
                // Other events don't trigger notifications
            }
        }
    }
}