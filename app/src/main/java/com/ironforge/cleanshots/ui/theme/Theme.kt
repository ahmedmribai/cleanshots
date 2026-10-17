package com.ironforge.cleanshots.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF0A0A0A)
val SurfaceCard = Color(0xFF141414)
val Accent = Color(0xFF00FF88)
val OnDark = Color(0xFFF2F2F2)
val Muted = Color(0xFF9AA0A6)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    background = Background,
    onBackground = OnDark,
    surface = SurfaceCard,
    onSurface = OnDark,
    secondaryContainer = Color(0xFF1E1E1E)
)

@Composable
fun CleanShotsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
}
