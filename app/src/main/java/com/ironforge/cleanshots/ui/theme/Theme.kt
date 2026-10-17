package com.ironforge.cleanshots.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.ironforge.cleanshots.R
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
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

@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

/** Manrope (SIL OFL 1.1, see assets/licenses); scripts it lacks fall back to the system font. */
val Manrope = FontFamily(listOf(400, 500, 600, 700, 800).map(::manrope))

private val AppTypography = Typography().withFamily(Manrope).let { t ->
    t.copy(
        displayMedium = t.displayMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp),
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        labelSmall = t.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    )
}

private fun Typography.withFamily(family: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family)
)

/** Letter-spacing breaks joined scripts such as Arabic, so RTL layouts get untracked text. */
private val RtlTypography = AppTypography.let { t ->
    fun TextStyle.untracked() = copy(letterSpacing = TextUnit.Unspecified)
    t.copy(
        displayMedium = t.displayMedium.untracked(),
        displaySmall = t.displaySmall.untracked(),
        headlineMedium = t.headlineMedium.untracked(),
        headlineSmall = t.headlineSmall.untracked(),
        labelLarge = t.labelLarge.untracked(),
        labelSmall = t.labelSmall.untracked()
    )
}

@Composable
fun CleanShotsTheme(content: @Composable () -> Unit) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    MaterialTheme(colorScheme = Scheme, typography = if (rtl) RtlTypography else AppTypography, content = content)
}
