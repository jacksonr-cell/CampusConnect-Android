package com.campusconnect.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import com.campusconnect.app.ui.theme.*

/**
 * Slowly animating radial gradient background — used on hero screens
 * and the top section of the feed for a premium, living feel.
 */
@Composable
fun AnimatedPurpleBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bgGradient")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue  = 0.35f,
        animationSpec = infiniteRepeatable(
            animation  = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 300f,
        targetValue  = 500f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowRadius",
    )

    Box(
        modifier = modifier.background(
            brush = Brush.radialGradient(
                colors = listOf(
                    DeepRoyalPurple.copy(alpha = glowAlpha),
                    ElectricPurple.copy(alpha = glowAlpha * 0.5f),
                    RichBlack.copy(alpha = 1f),
                ),
                radius    = glowRadius,
                tileMode  = TileMode.Clamp,
            )
        ),
        content = content,
    )
}

/** Static radial glow overlay for card backgrounds. */
@Composable
fun PurpleGlowOverlay(
    modifier: Modifier = Modifier,
    alpha: Float = 0.12f,
) {
    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    ElectricPurple.copy(alpha = alpha),
                    Color.Transparent,
                )
            )
        )
    )
}
