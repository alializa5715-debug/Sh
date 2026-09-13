package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Google Gemini Authentic Mobile Palette
val GeminiBackground = Color(0xFF131314)
val GeminiSurface = Color(0xFF1E1F20)
val GeminiSurfaceVariant = Color(0xFF282A2C)
val GeminiSurfaceElevated = Color(0xFF333537)
val GeminiOutline = Color(0xFF37393B)
val GeminiOutlineFocused = Color(0xFF5A5D5F)

val GeminiTextPrimary = Color(0xFFE3E3E3)
val GeminiTextSecondary = Color(0xFFC4C7C5)
val GeminiTextMuted = Color(0xFF8E918F)

val GeminiBlue = Color(0xFF8AB4F8)
val GeminiBlueBright = Color(0xFF4285F4)
val GeminiPurple = Color(0xFF9B72CF)
val GeminiPink = Color(0xFFD96570)
val GeminiCoral = Color(0xFFE8710A)
val GeminiGreen = Color(0xFF81C995)
val GeminiRed = Color(0xFFF28B82)

// Gemini Sparkle & Title Gradient
val GeminiSparkleGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4285F4), // Google Blue
        Color(0xFF9B72CF), // Gemini Purple
        Color(0xFFD96570), // Gemini Pink
        Color(0xFFF28B82)  // Soft Coral
    )
)

val GeminiPillGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF1F2836),
        Color(0xFF261F36)
    )
)

val GeminiGlowBrush = Brush.radialGradient(
    colors = listOf(
        Color(0x334285F4),
        Color(0x1A9B72CF),
        Color.Transparent
    )
)

// Legacy alias compatibility
val ElectricBlue = GeminiBlueBright
val SoftViolet = GeminiPurple
val DeepCharcoal = GeminiBackground
val SurfaceDark = GeminiSurface
val SurfaceVariantDark = GeminiSurfaceVariant
val SecondarySurface = GeminiSurfaceElevated
val BorderDark = GeminiOutline
val TextPrimary = GeminiTextPrimary
val TextSecondary = GeminiTextSecondary
val TextMuted = GeminiTextMuted
val EmeraldGreen = GeminiGreen
val CrimsonRed = GeminiRed
val AmberWarning = GeminiCoral
val NovaGradient = GeminiSparkleGradient
val NovaGlowGradient = GeminiGlowBrush
