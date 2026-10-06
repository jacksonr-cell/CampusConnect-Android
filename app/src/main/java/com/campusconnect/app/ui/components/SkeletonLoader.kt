package com.campusconnect.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.campusconnect.app.ui.theme.*

@Composable
private fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color(0xFF2A2A3E),
        Color(0xFF3A2A5E),
        Color(0xFF2A2A3E),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )
    return Brush.linearGradient(
        colors    = shimmerColors,
        start     = Offset(translateAnim - 300f, 0f),
        end       = Offset(translateAnim, 0f),
    )
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(8.dp)) {
    Box(modifier = modifier.clip(shape).background(shimmerBrush()))
}

/** Full skeleton layout matching a PostCard. */
@Composable
fun PostCardSkeleton(modifier: Modifier = Modifier) {
    GlassmorphicCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                ShimmerBox(Modifier.size(44.dp), RoundedCornerShape(50))
                Spacer(Modifier.width(12.dp))
                Column {
                    ShimmerBox(Modifier.width(120.dp).height(12.dp))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(Modifier.width(80.dp).height(10.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            // Content lines
            ShimmerBox(Modifier.fillMaxWidth().height(14.dp))
            Spacer(Modifier.height(6.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.85f).height(14.dp))
            Spacer(Modifier.height(6.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.65f).height(14.dp))
            Spacer(Modifier.height(14.dp))
            // Image placeholder
            ShimmerBox(Modifier.fillMaxWidth().height(200.dp), RoundedCornerShape(12.dp))
            Spacer(Modifier.height(14.dp))
            // Action bar
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(3) {
                    ShimmerBox(Modifier.width(60.dp).height(28.dp), RoundedCornerShape(20.dp))
                }
            }
        }
    }
}

/** Skeleton for the stories row. */
@Composable
fun StoriesRowSkeleton() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(6) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                ShimmerBox(Modifier.size(64.dp), RoundedCornerShape(50))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(Modifier.width(48.dp).height(10.dp))
            }
        }
    }
}
