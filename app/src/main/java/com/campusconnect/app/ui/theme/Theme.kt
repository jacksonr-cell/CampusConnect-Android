package com.campusconnect.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CampusConnectColorScheme = darkColorScheme(
    primary              = DeepRoyalPurple,
    onPrimary            = TextPrimary,
    primaryContainer     = Color(0xFF4C1D95),
    onPrimaryContainer   = NeonViolet,
    secondary            = ElectricPurple,
    onSecondary          = TextPrimary,
    secondaryContainer   = Color(0xFF581C87),
    onSecondaryContainer = LightLavender,
    tertiary             = NeonViolet,
    onTertiary           = RichBlack,
    tertiaryContainer    = Color(0xFF3B0764),
    onTertiaryContainer  = LightLavender,
    background           = RichBlack,
    onBackground         = TextPrimary,
    surface              = DarkCharcoal,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceDark,
    onSurfaceVariant     = TextSecondary,
    surfaceTint          = DeepRoyalPurple,
    outline              = GlassBorder,
    outlineVariant       = Color(0xFF2D2D2D),
    error                = ErrorRed,
    onError              = TextPrimary,
    errorContainer       = Color(0xFF7F1D1D),
    onErrorContainer     = Color(0xFFFECACA),
    inverseSurface       = TextPrimary,
    inverseOnSurface     = RichBlack,
    inversePrimary       = DeepRoyalPurple,
    scrim                = Color(0xCC000000),
)

@Composable
fun CampusConnectTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = RichBlack.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = CampusConnectColorScheme,
        typography  = CampusConnectTypography,
        shapes      = CampusConnectShapes,
        content     = content,
    )
}
