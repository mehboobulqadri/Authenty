package com.milkaholic.authenty.domain

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class AutoLockManager private constructor(
    private val context: Context,
    private val securityManager: SecurityManager
) : DefaultLifecycleObserver {

    private val prefs: SharedPreferences = context.getSharedPreferences("auto_lock_prefs", Context.MODE_PRIVATE)
    private var autoLockJob: Job? = null
    private var lastActivityTime: Long = System.currentTimeMillis()
    private var isAppInForeground: Boolean = true
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        @Volatile
        private var INSTANCE: AutoLockManager? = null

        fun getInstance(context: Context, securityManager: SecurityManager): AutoLockManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutoLockManager(context, securityManager).also { INSTANCE = it }
            }
        }

        private const val KEY_AUTO_LOCK_ENABLED = "auto_lock_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val KEY_LAST_LOCK_TIME = "last_lock_time"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_END_TIME = "lockout_end_time"
        private const val KEY_LOCK_ON_APP_SWITCH = "lock_on_app_switch"

        private const val DEFAULT_TIMEOUT_MS = 1 * 60 * 1000L // 1 minute
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val BASE_LOCKOUT_TIME = 1 * 60 * 1000L // 1 minute
    }

    var onAutoLockTriggered: (() -> Unit)? = null

    fun initialize() {
        updateActivity()
        if (isAutoLockEnabled()) {
            startAutoLockTimer()
        }
    }

    fun cleanup() {
        scope.cancel()
        autoLockJob?.cancel()
    }

    fun updateActivity() {
        lastActivityTime = System.currentTimeMillis()
        if (isAutoLockEnabled() && isAppInForeground) {
            restartAutoLockTimer()
        }
    }

    fun isAutoLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_LOCK_ENABLED, true)
    }

    fun setAutoLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LOCK_ENABLED, enabled).apply()
        securityManager.logSettingsModification("auto_lock_enabled", (!enabled).toString(), enabled.toString())
        
        if (enabled && isAppInForeground) {
            startAutoLockTimer()
        } else {
            stopAutoLockTimer()
        }
    }

    fun getAutoLockTimeout(): Long {
        val timeout = prefs.getLong(KEY_AUTO_LOCK_TIMEOUT, DEFAULT_TIMEOUT_MS)
        
        // Safety check: if timeout is immediate (0L) or too small, reset to safe default
        if (timeout <= 0L || timeout == TimeoutPresets.IMMEDIATE) {
            setAutoLockTimeout(TimeoutPresets.ONE_MINUTE)
            return TimeoutPresets.ONE_MINUTE
        }
        
        return timeout
    }

    fun setAutoLockTimeout(timeoutMs: Long) {
        val oldTimeout = getAutoLockTimeout()
        prefs.edit().putLong(KEY_AUTO_LOCK_TIMEOUT, timeoutMs).apply()
        securityManager.logSettingsModification("auto_lock_timeout", oldTimeout.toString(), timeoutMs.toString())
        
        if (isAutoLockEnabled() && isAppInForeground) {
            restartAutoLockTimer()
        }
    }

    fun shouldLockOnAppSwitch(): Boolean {
        return prefs.getBoolean(KEY_LOCK_ON_APP_SWITCH, false)
    }

    fun setLockOnAppSwitch(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ON_APP_SWITCH, enabled).apply()
        securityManager.logSettingsModification("lock_on_app_switch", (!enabled).toString(), enabled.toString())
    }

    fun getFailedAttempts(): Int {
        return prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    fun recordFailedAttempt() {
        val attempts = getFailedAttempts() + 1
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply()
        
        securityManager.logAuthenticationFailure("system", mapOf("failed_attempts" to attempts.toString()))

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            triggerLockout(attempts)
        }
    }

    fun clearFailedAttempts() {
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply()
    }

    fun isInLockout(): Boolean {
        val lockoutEndTime = prefs.getLong(KEY_LOCKOUT_END_TIME, 0)
        return System.currentTimeMillis() < lockoutEndTime
    }

    fun getLockoutTimeRemaining(): Long {
        val lockoutEndTime = prefs.getLong(KEY_LOCKOUT_END_TIME, 0)
        val remaining = lockoutEndTime - System.currentTimeMillis()
        return maxOf(0, remaining)
    }

    fun triggerLockout(attempts: Int) {
        val lockoutDuration = calculateLockoutDuration(attempts)
        val lockoutEndTime = System.currentTimeMillis() + lockoutDuration
        
        prefs.edit().putLong(KEY_LOCKOUT_END_TIME, lockoutEndTime).apply()
        
        securityManager.logAutoLock("failed_attempts_lockout")
        
        scope.launch {
            delay(lockoutDuration)
            clearLockout()
        }
    }

    fun clearLockout() {
        prefs.edit().remove(KEY_LOCKOUT_END_TIME).apply()
    }

    fun recordSuccessfulAuth() {
        clearFailedAttempts()
        clearLockout()
        updateActivity()
    }

    fun getTimeUntilAutoLock(): Long {
        if (!isAutoLockEnabled() || !isAppInForeground) {
            return Long.MAX_VALUE
        }
        
        val elapsed = System.currentTimeMillis() - lastActivityTime
        val timeout = getAutoLockTimeout()
        return maxOf(0, timeout - elapsed)
    }

    fun forceAutoLock() {
        recordLockTime()
        securityManager.logAutoLock("manual_trigger")
        onAutoLockTriggered?.invoke()
    }

    private fun startAutoLockTimer() {
        stopAutoLockTimer()
        
        autoLockJob = scope.launch {
            while (isActive && isAutoLockEnabled() && isAppInForeground) {
                val timeUntilLock = getTimeUntilAutoLock()
                
                if (timeUntilLock <= 0) {
                    recordLockTime()
                    securityManager.logAutoLock("timeout")
                    onAutoLockTriggered?.invoke()
                    break
                } else {
                    delay(minOf(timeUntilLock, 1000L)) // Check every second or until lock time
                }
            }
        }
    }

    private fun stopAutoLockTimer() {
        autoLockJob?.cancel()
        autoLockJob = null
    }

    private fun restartAutoLockTimer() {
        if (isAutoLockEnabled() && isAppInForeground) {
            startAutoLockTimer()
        }
    }

    private fun recordLockTime() {
        prefs.edit().putLong(KEY_LAST_LOCK_TIME, System.currentTimeMillis()).apply()
    }

    private fun calculateLockoutDuration(attempts: Int): Long {
        val multiplier = when {
            attempts <= 3 -> 1
            attempts <= 5 -> 2
            attempts <= 10 -> 5
            attempts <= 15 -> 15
            else -> 30
        }
        return BASE_LOCKOUT_TIME * multiplier
    }

    // Lifecycle methods
    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
        updateActivity()
        
        if (shouldLockOnAppSwitch()) {
            val lastLockTime = prefs.getLong(KEY_LAST_LOCK_TIME, 0)
            val lockThreshold = 2000L // 2 seconds threshold
            
            if (System.currentTimeMillis() - lastLockTime > lockThreshold) {
                securityManager.logAutoLock("app_resume")
                onAutoLockTriggered?.invoke()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground = false
        stopAutoLockTimer()
        recordLockTime()
        
        if (shouldLockOnAppSwitch()) {
            securityManager.logAutoLock("app_background")
            onAutoLockTriggered?.invoke()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cleanup()
    }

    // Timeout presets for easy configuration
    object TimeoutPresets {
        val IMMEDIATE = 0L
        val THIRTY_SECONDS = 30 * 1000L
        val ONE_MINUTE = 60 * 1000L
        val TWO_MINUTES = 2 * 60 * 1000L
        val FIVE_MINUTES = 5 * 60 * 1000L
        val TEN_MINUTES = 10 * 60 * 1000L
        val FIFTEEN_MINUTES = 15 * 60 * 1000L
        val THIRTY_MINUTES = 30 * 60 * 1000L
        val NEVER = Long.MAX_VALUE

        fun getAllPresets(): List<Pair<String, Long>> = listOf(
            "30 seconds" to THIRTY_SECONDS,
            "1 minute" to ONE_MINUTE,
            "2 minutes" to TWO_MINUTES,
            "5 minutes" to FIVE_MINUTES,
            "10 minutes" to TEN_MINUTES,
            "15 minutes" to FIFTEEN_MINUTES,
            "30 minutes" to THIRTY_MINUTES,
            "Never" to NEVER
        )

        fun getDisplayName(timeoutMs: Long): String {
            return when (timeoutMs) {
                IMMEDIATE -> "30 seconds" // Fallback for legacy immediate setting
                THIRTY_SECONDS -> "30 seconds"
                ONE_MINUTE -> "1 minute"
                TWO_MINUTES -> "2 minutes"
                FIVE_MINUTES -> "5 minutes"
                TEN_MINUTES -> "10 minutes"
                FIFTEEN_MINUTES -> "15 minutes"
                THIRTY_MINUTES -> "30 minutes"
                NEVER -> "Never"
                else -> "${TimeUnit.MILLISECONDS.toMinutes(timeoutMs)} minutes"
            }
        }
    }
}