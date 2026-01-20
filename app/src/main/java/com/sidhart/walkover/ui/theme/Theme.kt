package com.sidhart.walkover.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

import androidx.compose.ui.graphics.Color

// InDrive-inspired dark theme
private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Black,
    primaryContainer = DarkGray,
    onPrimaryContainer = NeonGreen,

    secondary = MediumGray,
    onSecondary = White,
    secondaryContainer = DarkGray,
    onSecondaryContainer = NeonGreen,

    tertiary = NeonGreen,
    onTertiary = Black,

    background = Black,
    onBackground = White,

    surface = DarkGray,
    onSurface = White,
    surfaceVariant = MediumGray,
    onSurfaceVariant = LightGray,

    error = ErrorRed,
    onError = White,

    outline = LightGray,
    outlineVariant = MediumGray
)

// Light theme color scheme
private val LightColorScheme = lightColorScheme(
    primary = LightNeonGreen,
    onPrimary = White,
    primaryContainer = Color(0xFFE8F5D0),
    onPrimaryContainer = DarkText,

    secondary = Color(0xFF5F6368),
    onSecondary = White,
    secondaryContainer = Color(0xFFE8EAED),
    onSecondaryContainer = DarkText,

    tertiary = LightNeonGreen,
    onTertiary = White,

    background = LightBackground,
    onBackground = DarkText,

    surface = LightSurface,
    onSurface = DarkText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = MediumText,

    error = LightErrorRed,
    onError = White,

    outline = LightBorder,
    outlineVariant = Color(0xFFCCCCCC)
)

@Composable
fun WalkOverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Auto-detect system preference
    dynamicColor: Boolean = false, // Set to true if you want Material You colors on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }



    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}