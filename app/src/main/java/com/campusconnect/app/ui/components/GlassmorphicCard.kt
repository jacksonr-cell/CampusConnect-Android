package com.campusconnect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.campusconnect.app.ui.theme.*

/**
 * Glassmorphism card — dark translucent surface with subtle purple border glow.
 * Matches the CampusConnect premium dark aesthetic.
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    glowColor: Color = GlowPurple,
    borderAlpha: Float = 0.15f,
    elevation: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation     = elevation,
                shape         = PostCardShape,
                ambientColor  = glowColor,
                spotColor     = glowColor,
            )
            .clip(PostCardShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B).copy(alpha = 0.85f),
                        Color(0xFF0F0A1E).copy(alpha = 0.90f),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha),
                        DeepRoyalPurple.copy(alpha = borderAlpha * 0.6f),
                        Color.Transparent,
                    )
                ),
                shape = PostCardShape,
            ),
        content = content,
    )
}

/** Subtle chip-style glass pill. */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(ChipShape)
            .background(GlassWhite10)
            .border(0.5.dp, GlassBorder, ChipShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        content = content,
    )
}
