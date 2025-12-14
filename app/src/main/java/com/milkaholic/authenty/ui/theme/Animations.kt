package com.milkaholic.authenty.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

object AuthentyAnimations {
    
    // Easing curves
    val smoothEasing = FastOutSlowInEasing
    val snappyEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    
    // Duration constants
    const val FAST_DURATION = 150
    const val MEDIUM_DURATION = 300
    const val SLOW_DURATION = 600
    const val VERY_SLOW_DURATION = 1000
    
    // Animation specs
    val fastSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    
    val mediumSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val smoothTween = tween<Float>(
        durationMillis = MEDIUM_DURATION,
        easing = smoothEasing
    )
    
    val bouncyTween = tween<Float>(
        durationMillis = SLOW_DURATION,
        easing = snappyEasing
    )
}

// Fade in/out animations
@Composable
fun FadeInOut(
    visible: Boolean,
    modifier: Modifier = Modifier,
    duration: Int = AuthentyAnimations.MEDIUM_DURATION,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(duration, easing = AuthentyAnimations.smoothEasing)
        ),
        exit = fadeOut(
            animationSpec = tween(duration, easing = AuthentyAnimations.smoothEasing)
        ),
        content = content
    )
}

// Slide animations
@Composable
fun SlideInFromTop(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(AuthentyAnimations.MEDIUM_DURATION, easing = AuthentyAnimations.smoothEasing)
        ) + fadeIn(animationSpec = AuthentyAnimations.smoothTween),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(AuthentyAnimations.MEDIUM_DURATION, easing = AuthentyAnimations.smoothEasing)
        ) + fadeOut(animationSpec = AuthentyAnimations.smoothTween),
        content = content
    )
}

@Composable
fun SlideInFromBottom(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(AuthentyAnimations.MEDIUM_DURATION, easing = AuthentyAnimations.smoothEasing)
        ) + fadeIn(animationSpec = AuthentyAnimations.smoothTween),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(AuthentyAnimations.MEDIUM_DURATION, easing = AuthentyAnimations.smoothEasing)
        ) + fadeOut(animationSpec = AuthentyAnimations.smoothTween),
        content = content
    )
}

// Scale animations
@Composable
fun ScaleInOut(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(
            initialScale = 0.8f,
            animationSpec = AuthentyAnimations.mediumSpring
        ) + fadeIn(animationSpec = AuthentyAnimations.smoothTween),
        exit = scaleOut(
            targetScale = 0.8f,
            animationSpec = AuthentyAnimations.mediumSpring
        ) + fadeOut(animationSpec = AuthentyAnimations.smoothTween),
        content = content
    )
}

// Bouncy button animation
@Composable
fun BouncyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = AuthentyAnimations.fastSpring,
        label = "button_scale"
    )
    
    Button(
        onClick = {
            onClick()
            isPressed = true
        },
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled
    ) {
        content()
    }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

// Floating card animation
@Composable
fun FloatingCard(
    modifier: Modifier = Modifier,
    isElevated: Boolean = false,
    content: @Composable () -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isElevated) 12.dp else 4.dp,
        animationSpec = tween(
            durationMillis = AuthentyAnimations.MEDIUM_DURATION,
            easing = AuthentyAnimations.smoothEasing
        ),
        label = "card_elevation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isElevated) 1.02f else 1f,
        animationSpec = AuthentyAnimations.mediumSpring,
        label = "card_scale"
    )
    
    Card(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        content()
    }
}

// Shimmer loading animation
@Composable
fun ShimmerLoading(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    
    Box(modifier = modifier) {
        content()
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha }
            ) {
                ShimmerOverlay()
            }
        }
    }
}

// Progressive TOTP refresh animation
@Composable
fun TOTPRefreshAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
    isExpiring: Boolean = false,
    content: @Composable () -> Unit
) {
    val shouldPulse = isExpiring && progress > 0.8f
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val pulseAlpha by if (shouldPulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    
    val rotation by animateFloatAsState(
        targetValue = progress * 360f,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        ),
        label = "progress_rotation"
    )
    
    Box(modifier = modifier) {
        content()
        
        // Pulsing overlay when expiring
        if (shouldPulse && pulseAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = pulseAlpha }
            ) {
                ShimmerOverlay()
            }
        }
    }
}

// Screen transitions
val slideUpTransition = slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(
        durationMillis = AuthentyAnimations.MEDIUM_DURATION,
        easing = AuthentyAnimations.smoothEasing
    )
) + fadeIn(
    animationSpec = tween(
        durationMillis = AuthentyAnimations.MEDIUM_DURATION,
        easing = AuthentyAnimations.smoothEasing
    )
)

val slideDownTransition = slideOutVertically(
    targetOffsetY = { it },
    animationSpec = tween(
        durationMillis = AuthentyAnimations.MEDIUM_DURATION,
        easing = AuthentyAnimations.smoothEasing
    )
) + fadeOut(
    animationSpec = tween(
        durationMillis = AuthentyAnimations.MEDIUM_DURATION,
        easing = AuthentyAnimations.smoothEasing
    )
)