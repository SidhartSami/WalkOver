package com.sidhart.walkover.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * WALKOVER THEME
 * Primary:   NeonGreen    — brand CTAs, active states, walk buttons
 * Secondary: ElectricBlue — Compete mode, map overlays, info
 * Tertiary:  CoralOrange  — XP, streaks, badges, warmth accents
 * Error:     ErrorRed     — stop, delete, destructive actions
 */

private val DarkColorScheme = darkColorScheme(
    primary              = NeonGreen,
    onPrimary            = DeepMidnight,
    primaryContainer     = Color(0xFF1A2200),   // Very dark lime tint
    onPrimaryContainer   = NeonGreen,

    secondary            = ElectricBlue,
    onSecondary          = DeepMidnight,
    secondaryContainer   = Color(0xFF001A28),   // Very dark blue tint
    onSecondaryContainer = ElectricBlue,

    tertiary             = CoralOrange,
    onTertiary           = DeepMidnight,
    tertiaryContainer    = Color(0xFF2A1000),   // Very dark orange tint
    onTertiaryContainer  = CoralOrange,

    background           = DeepMidnight,
    onBackground         = White,

    surface              = RichDarkGray,
    onSurface            = White,
    surfaceVariant       = SoftDarkGray,
    onSurfaceVariant     = MutedText,

    error                = ErrorRed,
    onError              = White,
    errorContainer       = Color(0xFF2D0A0A),
    onErrorContainer     = ErrorRed,

    outline              = BorderGray,
    outlineVariant       = BorderGray,

    inverseSurface       = White,
    inverseOnSurface     = DeepMidnight,
    inversePrimary       = NeonGreenDim,

    scrim                = BlackAlpha80
)

private val LightColorScheme = lightColorScheme(
    primary              = NeonGreen,
    onPrimary            = DeepMidnight,
    primaryContainer     = Color(0xFFF0FFB0),
    onPrimaryContainer   = DeepMidnight,

    secondary            = ElectricBlueDim,
    onSecondary          = White,
    secondaryContainer   = Color(0xFFDCF0FF),
    onSecondaryContainer = ElectricBlueDim,

    tertiary             = CoralOrangeDim,
    onTertiary           = White,
    tertiaryContainer    = Color(0xFFFFECE5),
    onTertiaryContainer  = CoralOrangeDim,

    background           = LightBackground,
    onBackground         = DarkText,

    surface              = LightSurface,
    onSurface            = DarkText,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = MediumText,

    error                = ErrorRed,
    onError              = White,
    errorContainer       = Color(0xFFFFE5E5),
    onErrorContainer     = ErrorRed,

    outline              = LightBorder,
    outlineVariant       = LightBorder,

    inverseSurface       = RichDarkGray,
    inverseOnSurface     = White,
    inversePrimary       = NeonGreen
)

@Composable
fun WalkOverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}