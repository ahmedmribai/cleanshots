package com.ironforge.cleanshots.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Background = Color(0xFF07090D)
val SurfaceCard = Color(0xFF10141B)
val SurfaceHigh = Color(0xFF181D27)
val Stroke = Color(0x14FFFFFF)
val Accent = Color(0xFF2EE6C5)
val AccentBlue = Color(0xFF4F8CFF)
val OnAccent = Color(0xFF041210)
val Danger = Color(0xFFFF5C7A)
val OnDark = Color(0xFFF2F4F8)
val Muted = Color(0xFF8A93A6)

val BrandGradient = Brush.linearGradient(listOf(Accent, AccentBlue))
val DangerGradient = Brush.linearGradient(listOf(Color(0xFFFF7A59), Danger))

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    secondary = AccentBlue,
    background = Background,
    onBackground = OnDark,
    surface = SurfaceCard,
    onSurface = OnDark,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = Muted,
    secondaryContainer = SurfaceHigh,
    error = Danger,
    outline = Stroke
)

private val AppTypography = Typography().let { t ->
    t.copy(
        displayMedium = t.displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp),
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        labelSmall = t.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    )
}

@Composable
fun CleanShotsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
}
