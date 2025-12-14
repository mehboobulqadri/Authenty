package com.milkaholic.authenty.domain

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.io.File

class DeviceSecurityChecker(private val context: Context) {

    companion object {
        private val ROOT_INDICATORS = listOf(
            "/system/bin/su",
            "/system/xbin/su", 
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )

        private val ROOT_APPS = listOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.koushikdutta.rommanager",
            "com.koushikdutta.rommanager.license",
            "com.dimonvideo.luckypatcher",
            "com.chelpus.lackypatch",
            "com.ramdroid.appquarantine",
            "com.topjohnwu.magisk",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot"
        )

        private val DANGEROUS_PROPS = listOf(
            "ro.debuggable" to "1",
            "service.adb.root" to "1",
            "ro.secure" to "0"
        )

        private val EMULATOR_INDICATORS = listOf(
            "goldfish" to Build.HARDWARE,
            "ranchu" to Build.HARDWARE,
            "vbox" to Build.PRODUCT,
            "simulator" to Build.MODEL,
            "emulator" to Build.MODEL,
            "Android SDK" to Build.MODEL
        )
    }

    fun performComprehensiveSecurityCheck(): SecurityCheckReport {
        val checks = mutableListOf<SecurityCheck>()

        checks.add(checkRootAccess())
        checks.add(checkHookingFrameworks())
        checks.add(checkDebuggingEnabled())
        checks.add(checkEmulatorEnvironment())
        checks.add(checkApplicationIntegrity())
        checks.add(checkDeveloperOptions())
        checks.add(checkUSBDebugging())

        val highRiskChecks = checks.filter { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL }
        val mediumRiskChecks = checks.filter { it.riskLevel == RiskLevel.MEDIUM }

        return SecurityCheckReport(
            checks = checks,
            overallRisk = when {
                highRiskChecks.any { it.riskLevel == RiskLevel.CRITICAL } -> RiskLevel.CRITICAL
                highRiskChecks.isNotEmpty() -> RiskLevel.HIGH
                mediumRiskChecks.isNotEmpty() -> RiskLevel.MEDIUM
                else -> RiskLevel.LOW
            },
            isDeviceCompromised = highRiskChecks.isNotEmpty(),
            recommendations = generateRecommendations(checks)
        )
    }

    private fun checkRootAccess(): SecurityCheck {
        val indicators = mutableListOf<String>()
        
        if (checkSuBinaries()) {
            indicators.add("Super user binaries found")
        }
        
        if (checkRootApps()) {
            indicators.add("Root management apps detected")
        }
        
        if (checkRootProperties()) {
            indicators.add("Dangerous system properties detected")
        }
        
        if (testSuCommand()) {
            indicators.add("SU command executable")
        }

        return SecurityCheck(
            name = "Root Access Detection",
            passed = indicators.isEmpty(),
            riskLevel = if (indicators.isEmpty()) RiskLevel.LOW else RiskLevel.CRITICAL,
            details = if (indicators.isEmpty()) "No root access detected" else indicators.joinToString(", "),
            recommendation = if (indicators.isEmpty()) null else "Device appears to be rooted. Consider using a non-rooted device for enhanced security."
        )
    }

    private fun checkHookingFrameworks(): SecurityCheck {
        val detectedFrameworks = mutableListOf<String>()
        
        val frameworks = listOf(
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XposedHelpers",
            "com.saurik.substrate.MS",
            "com.cydiasubstrate.CydiaSubstrate"
        )
        
        frameworks.forEach { framework ->
            try {
                Class.forName(framework)
                detectedFrameworks.add(framework.substringAfterLast("."))
            } catch (e: ClassNotFoundException) {
                // Framework not detected
            }
        }

        return SecurityCheck(
            name = "Hooking Framework Detection",
            passed = detectedFrameworks.isEmpty(),
            riskLevel = if (detectedFrameworks.isEmpty()) RiskLevel.LOW else RiskLevel.HIGH,
            details = if (detectedFrameworks.isEmpty()) "No hooking frameworks detected" else "Detected: ${detectedFrameworks.joinToString(", ")}",
            recommendation = if (detectedFrameworks.isEmpty()) null else "Hooking frameworks detected. App behavior may be modified."
        )
    }

    private fun checkDebuggingEnabled(): SecurityCheck {
        val isDebuggable = try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }

        // Check if this is a debug build by examining application info
        val isProductionBuild = try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0
        } catch (e: Exception) {
            true // Assume production if we can't determine
        }

        return SecurityCheck(
            name = "Debug Mode Check",
            passed = !isDebuggable || !isProductionBuild,
            riskLevel = if (isDebuggable && isProductionBuild) RiskLevel.HIGH else RiskLevel.LOW,
            details = when {
                isDebuggable && isProductionBuild -> "App is debuggable in production build"
                isDebuggable && !isProductionBuild -> "Debug build detected (normal for development)"
                else -> "Production build without debug flags"
            },
            recommendation = if (isDebuggable && isProductionBuild) "Disable debugging for production releases" else null
        )
    }

    private fun checkEmulatorEnvironment(): SecurityCheck {
        val emulatorIndicators = mutableListOf<String>()
        
        EMULATOR_INDICATORS.forEach { (indicator, actual) ->
            if (actual.lowercase().contains(indicator.lowercase())) {
                emulatorIndicators.add("$indicator detected in $actual")
            }
        }

        if (Build.FINGERPRINT.contains("generic") || Build.FINGERPRINT.contains("unknown")) {
            emulatorIndicators.add("Generic fingerprint detected")
        }

        return SecurityCheck(
            name = "Emulator Detection",
            passed = emulatorIndicators.isEmpty(),
            riskLevel = if (emulatorIndicators.isEmpty()) RiskLevel.LOW else RiskLevel.MEDIUM,
            details = if (emulatorIndicators.isEmpty()) "Running on physical device" else emulatorIndicators.joinToString(", "),
            recommendation = if (emulatorIndicators.isEmpty()) null else "Running on emulator. Be cautious with sensitive operations."
        )
    }

    private fun checkApplicationIntegrity(): SecurityCheck {
        val issues = mutableListOf<String>()
        
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            
            if (packageInfo.signatures?.isEmpty() != false) {
                issues.add("No package signatures found")
            }
            
        } catch (e: Exception) {
            issues.add("Unable to verify package integrity")
        }

        return SecurityCheck(
            name = "Application Integrity",
            passed = issues.isEmpty(),
            riskLevel = if (issues.isEmpty()) RiskLevel.LOW else RiskLevel.MEDIUM,
            details = if (issues.isEmpty()) "Application integrity verified" else issues.joinToString(", "),
            recommendation = if (issues.isEmpty()) null else "Application integrity could not be fully verified"
        )
    }

    private fun checkDeveloperOptions(): SecurityCheck {
        val isDeveloperEnabled = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }

        return SecurityCheck(
            name = "Developer Options",
            passed = !isDeveloperEnabled,
            riskLevel = if (isDeveloperEnabled) RiskLevel.MEDIUM else RiskLevel.LOW,
            details = if (isDeveloperEnabled) "Developer options are enabled" else "Developer options disabled",
            recommendation = if (isDeveloperEnabled) "Consider disabling developer options for enhanced security" else null
        )
    }

    private fun checkUSBDebugging(): SecurityCheck {
        val isUSBDebuggingEnabled = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }

        return SecurityCheck(
            name = "USB Debugging",
            passed = !isUSBDebuggingEnabled,
            riskLevel = if (isUSBDebuggingEnabled) RiskLevel.MEDIUM else RiskLevel.LOW,
            details = if (isUSBDebuggingEnabled) "USB debugging is enabled" else "USB debugging disabled",
            recommendation = if (isUSBDebuggingEnabled) "Disable USB debugging when not needed" else null
        )
    }

    private fun checkSuBinaries(): Boolean {
        return ROOT_INDICATORS.any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun checkRootApps(): Boolean {
        return ROOT_APPS.any { packageName ->
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun checkRootProperties(): Boolean {
        return try {
            DANGEROUS_PROPS.any { (prop, dangerousValue) ->
                getSystemProperty(prop) == dangerousValue
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun testSuCommand(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val exitValue = process.waitFor()
            exitValue == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            get.invoke(null, key) as? String
        } catch (e: Exception) {
            null
        }
    }

    private fun generateRecommendations(checks: List<SecurityCheck>): List<String> {
        return checks.mapNotNull { it.recommendation }.distinct()
    }
}

data class SecurityCheckReport(
    val checks: List<SecurityCheck>,
    val overallRisk: RiskLevel,
    val isDeviceCompromised: Boolean,
    val recommendations: List<String>
) {
    val passedChecks = checks.count { it.passed }
    val totalChecks = checks.size
    val securityScore = (passedChecks.toFloat() / totalChecks * 100).toInt()
}

data class SecurityCheck(
    val name: String,
    val passed: Boolean,
    val riskLevel: RiskLevel,
    val details: String,
    val recommendation: String?
)

enum class RiskLevel {
    LOW,
    MEDIUM, 
    HIGH,
    CRITICAL
}