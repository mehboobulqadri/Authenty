package com.milkaholic.authenty.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AuthentyDarkColorScheme = darkColorScheme(
    primary = AuthentyPurple,
    onPrimary = Color.White,
    secondary = AuthentyPink,
    onSecondary = Color.White,
    tertiary = AuthentyFuchsia,
    onTertiary = Color.White,
    
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceGradientDark,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A1F3A),
    onSurfaceVariant = AuthentyLavender,
    
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = AuthentyLavender,
    secondaryContainer = Color(0xFF831843),
    onSecondaryContainer = AuthentyRose,
    
    outline = AuthentyPurple.copy(alpha = 0.5f),
    outlineVariant = AuthentyPurple.copy(alpha = 0.3f),
    
    error = Color(0xFFFF6B6B),
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val AuthentyLightColorScheme = lightColorScheme(
    primary = AuthentyPurple,
    onPrimary = Color.White,
    secondary = AuthentyPink,
    onSecondary = Color.White,
    tertiary = AuthentyDeepPurple,
    onTertiary = Color.White,
    
    background = BackgroundLight,
    onBackground = Color(0xFF1C1B1F),
    surface = SurfaceGradient,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF3E8FF),
    onSurfaceVariant = AuthentyDeepPurple,
    
    primaryContainer = AuthentyLavender.copy(alpha = 0.3f),
    onPrimaryContainer = AuthentyDeepPurple,
    secondaryContainer = AuthentyRose.copy(alpha = 0.3f),
    onSecondaryContainer = Color(0xFF831843),
    
    outline = AuthentyPurple.copy(alpha = 0.5f),
    outlineVariant = AuthentyPurple.copy(alpha = 0.2f),
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun AuthentyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ but we'll prefer our custom theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> AuthentyDarkColorScheme
        else -> AuthentyLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}