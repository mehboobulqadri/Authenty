package com.milkaholic.authenty.domain

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.milkaholic.authenty.data.SecurityEvent
import com.milkaholic.authenty.data.SecurityEventRepository
import com.milkaholic.authenty.data.SecurityEventSeverity
import com.milkaholic.authenty.data.SecurityEventType
import com.milkaholic.authenty.data.SecuritySummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class SecurityManager private constructor(private val context: Context) {
    
    private val securityEventRepository = SecurityEventRepository(context)
    private val securitySettings = SecuritySettings(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val duressModePrefs = context.getSharedPreferences("duress_mode_prefs", Context.MODE_PRIVATE)

    private val _isDuressMode = MutableStateFlow(duressModePrefs.getBoolean(KEY_DURESS_MODE_ACTIVE, false))
    val isDuressMode: StateFlow<Boolean> = _isDuressMode.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: SecurityManager? = null
        
        private const val KEY_DURESS_MODE_ACTIVE = "duress_mode_active"

        fun getInstance(context: Context): SecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        private val ROOT_INDICATORS = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )

        private val ROOT_APPS = listOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.zachspong.temprootremovejb",
            "com.ramdroid.appquarantine",
            "com.topjohnwu.magisk"
        )
    }

    fun initialize() {
        scope.launch {
            logEvent(
                SecurityEventType.APP_UNLOCKED,
                "Application started",
                SecurityEventSeverity.INFO
            )
            
            performSecurityChecks()
        }
    }

    fun triggerDuressMode() {
        duressModePrefs.edit().putBoolean(KEY_DURESS_MODE_ACTIVE, true).commit()
        _isDuressMode.value = true
        scope.launch {
            logEvent(
                SecurityEventType.SUSPICIOUS_ACTIVITY,
                "Duress PIN entered - Duress Mode activated",
                SecurityEventSeverity.CRITICAL
            )
        }
    }
    
    fun clearDuressMode() {
        duressModePrefs.edit().putBoolean(KEY_DURESS_MODE_ACTIVE, false).commit()
        _isDuressMode.value = false
        scope.launch {
            logEvent(
                SecurityEventType.SETTINGS_MODIFIED,
                "Duress Mode deactivated - Normal operation restored",
                SecurityEventSeverity.INFO
            )
        }
    }

    fun logAuthenticationSuccess(method: String, details: Map<String, String> = emptyMap()) {
        scope.launch {
            securityEventRepository.logAuthenticationAttempt(true, method, details)
            logEvent(
                SecurityEventType.APP_UNLOCKED,
                "User authenticated successfully via $method",
                SecurityEventSeverity.INFO,
                details
            )
        }
    }

    fun logAuthenticationFailure(method: String, details: Map<String, String> = emptyMap()) {
        scope.launch {
            securityEventRepository.logAuthenticationAttempt(false, method, details)
            
            val failedAttempts = securityEventRepository.getFailedAttemptsCount(30)
            
            if (failedAttempts >= securitySettings.getMaxFailedAttempts()) {
                triggerProgressiveLockout(failedAttempts)
            }
        }
    }

    fun logAccountModification(action: String, accountName: String, issuer: String) {
        scope.launch {
            securityEventRepository.logAccountModification(action, accountName, issuer)
        }
    }

    fun logAppLock() {
        scope.launch {
            logEvent(
                SecurityEventType.APP_LOCKED,
                "Application locked",
                SecurityEventSeverity.INFO
            )
        }
    }

    fun logAutoLock(reason: String = "timeout") {
        scope.launch {
            logEvent(
                SecurityEventType.AUTO_LOCK_TRIGGERED,
                "Auto-lock triggered: $reason",
                SecurityEventSeverity.INFO,
                mapOf("reason" to reason)
            )
        }
    }

    fun logSettingsModification(setting: String, oldValue: String?, newValue: String?) {
        scope.launch {
            logEvent(
                SecurityEventType.SETTINGS_MODIFIED,
                "Setting changed: $setting",
                SecurityEventSeverity.INFO,
                mapOf(
                    "setting" to setting,
                    "old_value" to (oldValue ?: "null"),
                    "new_value" to (newValue ?: "null")
                )
            )
        }
    }

    suspend fun performSecurityChecks(): SecurityCheckResult {
        val issues = mutableListOf<SecurityIssue>()

        if (isDeviceRooted()) {
            logEvent(
                SecurityEventType.ROOT_DETECTED,
                "Root access detected on device",
                SecurityEventSeverity.CRITICAL,
                mapOf("check_time" to System.currentTimeMillis().toString())
            )
            issues.add(SecurityIssue.ROOT_DETECTED)
        }

        if (isAppTampered()) {
            logEvent(
                SecurityEventType.TAMPER_DETECTED,
                "Application integrity compromised",
                SecurityEventSeverity.CRITICAL
            )
            issues.add(SecurityIssue.APP_TAMPERED)
        }

        if (isDebugModeEnabled()) {
            logEvent(
                SecurityEventType.SUSPICIOUS_ACTIVITY,
                "Debug mode enabled",
                SecurityEventSeverity.MEDIUM
            )
            issues.add(SecurityIssue.DEBUG_MODE)
        }

        val isSecure = issues.isEmpty()
        
        logEvent(
            SecurityEventType.APP_INSTALL_VERIFIED,
            "Security check completed: ${if (isSecure) "SECURE" else "ISSUES FOUND"}",
            if (isSecure) SecurityEventSeverity.INFO else SecurityEventSeverity.HIGH,
            mapOf(
                "issues_count" to issues.size.toString(),
                "issues" to issues.joinToString(",") { it.name }
            )
        )

        return SecurityCheckResult(isSecure, issues)
    }

    suspend fun getSecuritySummary(): AuthentyResult<SecuritySummary> {
        return securityEventRepository.getSecuritySummary()
    }

    suspend fun getRecentSecurityEvents(hours: Int = 24): AuthentyResult<List<SecurityEvent>> {
        return securityEventRepository.getRecentEvents(hours)
    }

    suspend fun getAllSecurityEvents(): AuthentyResult<List<SecurityEvent>> {
        return securityEventRepository.getAllEvents()
    }

    fun getFailedAttemptsInLastPeriod(minutes: Int = 30): Int {
        return securityEventRepository.getFailedAttemptsCount(minutes)
    }

    fun shouldTriggerLockout(): Boolean {
        val failedAttempts = getFailedAttemptsInLastPeriod()
        return failedAttempts >= securitySettings.getMaxFailedAttempts()
    }

    fun getLockoutDuration(): Long {
        val failedAttempts = getFailedAttemptsInLastPeriod()
        return when {
            failedAttempts < 3 -> 0
            failedAttempts < 5 -> TimeUnit.MINUTES.toMillis(1)
            failedAttempts < 10 -> TimeUnit.MINUTES.toMillis(5)
            failedAttempts < 15 -> TimeUnit.MINUTES.toMillis(15)
            else -> TimeUnit.MINUTES.toMillis(30)
        }
    }

    suspend fun clearSecurityLog(): AuthentyResult<Unit> {
        logEvent(
            SecurityEventType.SETTINGS_MODIFIED,
            "Security log cleared",
            SecurityEventSeverity.MEDIUM
        )
        return securityEventRepository.clearAllEvents()
    }

    private fun isDeviceRooted(): Boolean {
        return checkRootFiles() || checkRootApps() || checkSuCommand()
    }

    private fun checkRootFiles(): Boolean {
        return ROOT_INDICATORS.any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun checkRootApps(): Boolean {
        val packageManager = context.packageManager
        return ROOT_APPS.any { packageName ->
            try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun checkSuCommand(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val exitValue = process.waitFor()
            exitValue == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun isAppTampered(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val appInfo = packageInfo.applicationInfo
            val isDebuggable = appInfo?.let { (it.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 } ?: false
            
            // In production builds, debuggable flag should be false
            isDebuggable
        } catch (e: Exception) {
            true
        }
    }

    private fun isDebugModeEnabled(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val appInfo = packageInfo.applicationInfo
            appInfo?.let { (it.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun triggerProgressiveLockout(failedAttempts: Int) {
        val lockoutDuration = getLockoutDuration()
        
        logEvent(
            SecurityEventType.PROGRESSIVE_LOCKOUT,
            "Progressive lockout triggered after $failedAttempts failed attempts",
            SecurityEventSeverity.HIGH,
            mapOf(
                "failed_attempts" to failedAttempts.toString(),
                "lockout_duration_ms" to lockoutDuration.toString(),
                "lockout_duration_minutes" to TimeUnit.MILLISECONDS.toMinutes(lockoutDuration).toString()
            )
        )

        if (failedAttempts >= 10) {
            logEvent(
                SecurityEventType.SECURITY_BREACH_DETECTED,
                "Multiple failed authentication attempts detected - possible security breach",
                SecurityEventSeverity.CRITICAL,
                mapOf("failed_attempts" to failedAttempts.toString())
            )
        }
    }

    private suspend fun logEvent(
        type: SecurityEventType,
        description: String,
        severity: SecurityEventSeverity,
        metadata: Map<String, String> = emptyMap()
    ) {
        val event = SecurityEvent(
            type = type,
            description = description,
            severity = severity,
            metadata = metadata
        )
        securityEventRepository.logEvent(event)
    }
}

data class SecurityCheckResult(
    val isSecure: Boolean,
    val issues: List<SecurityIssue>
)

enum class SecurityIssue {
    ROOT_DETECTED,
    APP_TAMPERED,
    DEBUG_MODE
}

class SecuritySettings(context: Context) {
    private val prefs = context.getSharedPreferences("security_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MAX_FAILED_ATTEMPTS = "max_failed_attempts"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val KEY_SECURITY_LOGGING_ENABLED = "security_logging_enabled"
        
        private const val DEFAULT_MAX_FAILED_ATTEMPTS = 5
        private const val DEFAULT_AUTO_LOCK_TIMEOUT = 5 * 60 * 1000L // 5 minutes
    }

    fun getMaxFailedAttempts(): Int {
        return prefs.getInt(KEY_MAX_FAILED_ATTEMPTS, DEFAULT_MAX_FAILED_ATTEMPTS)
    }

    fun setMaxFailedAttempts(attempts: Int) {
        prefs.edit().putInt(KEY_MAX_FAILED_ATTEMPTS, attempts).apply()
    }

    fun getAutoLockTimeout(): Long {
        return prefs.getLong(KEY_AUTO_LOCK_TIMEOUT, DEFAULT_AUTO_LOCK_TIMEOUT)
    }

    fun setAutoLockTimeout(timeoutMs: Long) {
        prefs.edit().putLong(KEY_AUTO_LOCK_TIMEOUT, timeoutMs).apply()
    }

    fun isSecurityLoggingEnabled(): Boolean {
        return prefs.getBoolean(KEY_SECURITY_LOGGING_ENABLED, true)
    }

    fun setSecurityLoggingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURITY_LOGGING_ENABLED, enabled).apply()
    }
}