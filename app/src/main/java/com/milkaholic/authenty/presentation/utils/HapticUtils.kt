package com.milkaholic.authenty.presentation.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Utility class for providing haptic feedback throughout the app
 */
class HapticUtils(private val context: Context) {
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Light tap feedback for button presses
     */
    fun lightTap() {
        performHaptic(HapticType.LIGHT_TAP)
    }

    /**
     * Medium click feedback for important actions
     */
    fun click() {
        performHaptic(HapticType.CLICK)
    }

    /**
     * Heavy impact for errors or warnings
     */
    fun heavyImpact() {
        performHaptic(HapticType.HEAVY_IMPACT)
    }

    /**
     * Success feedback for completed actions
     */
    fun success() {
        performHaptic(HapticType.SUCCESS)
    }

    /**
     * Error feedback for failed actions
     */
    fun error() {
        performHaptic(HapticType.ERROR)
    }

    /**
     * Warning feedback for caution actions
     */
    fun warning() {
        performHaptic(HapticType.WARNING)
    }

    /**
     * Long press feedback
     */
    fun longPress() {
        performHaptic(HapticType.LONG_PRESS)
    }

    private fun performHaptic(type: HapticType) {
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val effect = when (type) {
                        HapticType.LIGHT_TAP -> VibrationEffect.createOneShot(20, 50)
                        HapticType.CLICK -> VibrationEffect.createOneShot(30, 100)
                        HapticType.HEAVY_IMPACT -> VibrationEffect.createOneShot(50, 200)
                        HapticType.SUCCESS -> VibrationEffect.createWaveform(
                            longArrayOf(0, 30, 30, 30), 
                            intArrayOf(0, 100, 0, 100), 
                            -1
                        )
                        HapticType.ERROR -> VibrationEffect.createWaveform(
                            longArrayOf(0, 100, 50, 100), 
                            intArrayOf(0, 150, 0, 150), 
                            -1
                        )
                        HapticType.WARNING -> VibrationEffect.createWaveform(
                            longArrayOf(0, 50, 25, 50, 25, 50), 
                            intArrayOf(0, 80, 0, 80, 0, 80), 
                            -1
                        )
                        HapticType.LONG_PRESS -> VibrationEffect.createOneShot(80, 120)
                    }
                    vib.vibrate(effect)
                } catch (e: Exception) {
                    // Fallback for devices with limited vibration support
                    @Suppress("DEPRECATION")
                    vib.vibrate(when (type) {
                        HapticType.LIGHT_TAP -> 20
                        HapticType.CLICK -> 30
                        HapticType.HEAVY_IMPACT -> 50
                        HapticType.SUCCESS -> 100
                        HapticType.ERROR -> 150
                        HapticType.WARNING -> 100
                        HapticType.LONG_PRESS -> 80
                    })
                }
            } else {
                // Fallback for older Android versions
                @Suppress("DEPRECATION")
                vib.vibrate(when (type) {
                    HapticType.LIGHT_TAP -> 20
                    HapticType.CLICK -> 30
                    HapticType.HEAVY_IMPACT -> 50
                    HapticType.SUCCESS -> 100
                    HapticType.ERROR -> 150
                    HapticType.WARNING -> 100
                    HapticType.LONG_PRESS -> 80
                })
            }
        }
    }

    enum class HapticType {
        LIGHT_TAP,
        CLICK,
        HEAVY_IMPACT,
        SUCCESS,
        ERROR,
        WARNING,
        LONG_PRESS
    }
}

/**
 * Composable function to easily get haptic utils in any composable
 */
@Composable
fun rememberHapticUtils(): HapticUtils {
    val context = LocalContext.current
    return remember { HapticUtils(context) }
}