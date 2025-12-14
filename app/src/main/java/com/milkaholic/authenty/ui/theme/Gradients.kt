package com.milkaholic.authenty.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp

object AuthentyGradients {
    
    @Composable
    fun primaryGradient(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
        return if (darkTheme) {
            Brush.linearGradient(
                colors = listOf(GradientStartDark, GradientEndDark),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(GradientStart, GradientEnd),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
    }
    
    @Composable
    fun subtleGradient(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
        return if (darkTheme) {
            Brush.linearGradient(
                colors = listOf(
                    AuthentyPurple.copy(alpha = 0.1f),
                    AuthentyPink.copy(alpha = 0.1f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    AuthentyLavender.copy(alpha = 0.2f),
                    AuthentyRose.copy(alpha = 0.2f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
    }
    
    @Composable
    fun neonGradient(): Brush {
        return Brush.linearGradient(
            colors = listOf(NeonPurple, NeonPink, NeonBlue),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }
    
    @Composable
    fun shimmerGradient(): Brush {
        return Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.3f),
                Color.White.copy(alpha = 0.1f),
                Color.White.copy(alpha = 0.3f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }
}

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuthentyGradients.subtleGradient())
    ) {
        content()
    }
}

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    useNeonGradient: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (useNeonGradient) {
                        AuthentyGradients.neonGradient()
                    } else {
                        AuthentyGradients.subtleGradient()
                    }
                )
        ) {
            content()
        }
    }
}

@Composable
fun PrimaryGradientCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AuthentyGradients.primaryGradient())
        ) {
            content()
        }
    }
}

@Composable
fun ShimmerOverlay(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    if (isVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AuthentyGradients.shimmerGradient())
        )
    }
}