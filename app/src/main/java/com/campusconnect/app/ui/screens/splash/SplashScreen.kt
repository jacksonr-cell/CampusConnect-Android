package com.campusconnect.app.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.campusconnect.app.CampusConnectApp
import com.campusconnect.app.ui.theme.*

@Composable
fun SplashScreen(onSplashComplete: (isLoggedIn: Boolean) -> Unit) {

    // Animation values
    val logoAlpha  by animateFloatAsState(targetValue  = 1f, animationSpec = tween(800), label = "alpha")
    val logoScale  by animateFloatAsState(targetValue  = 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "scale")
    val taglineAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(600, delayMillis = 400), label = "tagline")

    // Pulsing glow
    val infiniteTransition = rememberInfiniteTransition(label = "splashGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue  = 0.45f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(2200)
        val app = context.applicationContext as CampusConnectApp
        val isLoggedIn = app.container.tokenManager.isLoggedInFlow.value
        onSplashComplete(isLoggedIn)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RichBlack),
        contentAlignment = Alignment.Center,
    ) {
        // Background radial glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DeepRoyalPurple.copy(alpha = glowAlpha),
                            ElectricPurple.copy(alpha = glowAlpha * 0.4f),
                            RichBlack.copy(alpha = 0f),
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .scale(logoScale)
                .alpha(logoAlpha),
        ) {
            // Logo mark
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        brush = Brush.linearGradient(PurpleGradient),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "CC",
                    fontSize   = 36.sp,
                    fontWeight = FontWeight.Black,
                    color      = TextPrimary,
                )
            }

            // App name
            Text(
                text       = "CampusConnect",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
            )

            // Tagline — fades in slightly later
            Text(
                text     = "Your Campus. Connected.",
                style    = MaterialTheme.typography.bodyMedium,
                color    = TextSecondary,
                modifier = Modifier.alpha(taglineAlpha),
            )
        }

        // Version tag at bottom
        Text(
            text     = "v1.0.0",
            style    = MaterialTheme.typography.labelSmall,
            color    = TextDisabled,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        )
    }
}
