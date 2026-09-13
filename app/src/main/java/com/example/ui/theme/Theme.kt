package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GeminiDarkColorScheme = darkColorScheme(
    primary = GeminiBlue,
    onPrimary = Color(0xFF041E49),
    primaryContainer = Color(0xFF004A77),
    onPrimaryContainer = Color(0xFFC2E7FF),
    secondary = GeminiPurple,
    onSecondary = Color(0xFF280B4B),
    secondaryContainer = Color(0xFF432B68),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = GeminiPink,
    background = GeminiBackground,
    onBackground = GeminiTextPrimary,
    surface = GeminiSurface,
    onSurface = GeminiTextPrimary,
    surfaceVariant = GeminiSurfaceVariant,
    onSurfaceVariant = GeminiTextSecondary,
    surfaceContainer = GeminiSurface,
    surfaceContainerHigh = GeminiSurfaceVariant,
    surfaceContainerHighest = GeminiSurfaceElevated,
    outline = GeminiOutline,
    outlineVariant = GeminiOutlineFocused,
    error = GeminiRed,
    onError = Color(0xFF601410)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GeminiDarkColorScheme,
        typography = Typography,
        content = content
    )
}
