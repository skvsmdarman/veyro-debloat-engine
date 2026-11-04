package com.veyro.debloatengine.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---- Veyro brand palette (matches the launcher crest) ----
val VoidBlack = Color(0xFF0B0514)
val DeepPurple = Color(0xFF1A0B2E)
val PanelPurple = Color(0xFF1D1233)
val VeyroPurple = Color(0xFF8B5CF6)
val VeyroPurpleBright = Color(0xFFA78BFA)
val VeyroWhite = Color(0xFFF4F1FF)
val VeyroGreen = Color(0xFF4ADE80)
val VeyroRed = Color(0xFFF87171)
val VeyroAmber = Color(0xFFFBBF24)
val VeyroBlue = Color(0xFF60A5FA)
val DimText = Color(0xFF9B93B5)

private val VeyroColorScheme = darkColorScheme(
    primary = VeyroPurple,
    onPrimary = VeyroWhite,
    primaryContainer = DeepPurple,
    onPrimaryContainer = VeyroPurpleBright,
    secondary = VeyroPurpleBright,
    onSecondary = VoidBlack,
    background = VoidBlack,
    onBackground = VeyroWhite,
    surface = PanelPurple,
    onSurface = VeyroWhite,
    surfaceVariant = DeepPurple,
    onSurfaceVariant = DimText,
    error = VeyroRed,
    outline = Color(0xFF3A2A5C)
)

private val VeyroTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp, color = DimText),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun VeyroTheme(content: @Composable () -> Unit) {
    // The engine is intentionally always-dark: it is a performance tool.
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = VeyroColorScheme,
        typography = VeyroTypography,
        content = content
    )
}
