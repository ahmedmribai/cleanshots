package com.ironforge.cleanshots.ui.components

import com.ironforge.cleanshots.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ironforge.cleanshots.ui.scan.formatBytes
import com.ironforge.cleanshots.ui.theme.Accent
import com.ironforge.cleanshots.ui.theme.AccentBlue
import com.ironforge.cleanshots.ui.theme.BrandGradient
import com.ironforge.cleanshots.ui.theme.Muted
import com.ironforge.cleanshots.ui.theme.SurfaceHigh
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Fades and lifts content in on first composition; [index] staggers siblings. */
fun Modifier.enterAnimation(index: Int = 0): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        progress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
    }
    graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * 48.dp.toPx()
    }
}

/**
 * Gradient ring. A null [progress] spins indefinitely (used while the total is unknown);
 * otherwise the sweep animates to the given fraction.
 */
@Composable
fun ProgressRing(
    progress: Float?,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val sweep by animateFloatAsState(progress ?: 0f, tween(700, easing = FastOutSlowInEasing), label = "ringSweep")
    val loop = rememberInfiniteTransition(label = "ring")
    val rotation by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "ringSpin")
    val glow by loop.animateFloat(0.18f, 0.38f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "ringGlow")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawCircle(
                Brush.radialGradient(listOf(Accent.copy(alpha = glow), Color.Transparent), center, size.minDimension * 0.75f),
                radius = size.minDimension * 0.75f
            )
            drawArc(SurfaceHigh, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val brush = Brush.sweepGradient(listOf(Accent, AccentBlue, Accent), center)
            if (progress == null) {
                rotate(rotation) {
                    drawArc(brush, 0f, 100f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            } else if (sweep > 0f) {
                rotate(-90f) {
                    drawArc(brush, 0f, 360f * sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
        }
        content()
    }
}

/** Byte count that rolls up to its new value instead of jumping. */
@Composable
fun AnimatedBytes(bytes: Long, style: TextStyle, modifier: Modifier = Modifier) {
    val value = remember { Animatable(0f) }
    LaunchedEffect(bytes) { value.animateTo(bytes.toFloat(), tween(1100, easing = FastOutSlowInEasing)) }
    Text(formatBytes(value.value.toLong()), style = style, modifier = modifier, textAlign = TextAlign.Center)
}

private class Particle(val angle: Float, val speed: Float, val radius: Float, val color: Color)

/** Full-screen burst shown after photos are moved to Trash; dismisses itself. */
@Composable
fun CelebrationOverlay(bytes: Long?, onDone: () -> Unit) {
    var lastBytes by remember { mutableLongStateOf(0L) }
    if (bytes != null) lastBytes = bytes
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(bytes) {
        if (bytes != null) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(2600)
            onDone()
        }
    }
    AnimatedVisibility(visible = bytes != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(400))) {
        val particles = remember {
            val colors = listOf(Accent, AccentBlue, Color.White, Color(0xFFFFD166))
            List(70) {
                Particle(
                    angle = Random.nextFloat() * 6.283f,
                    speed = 0.25f + Random.nextFloat() * 0.75f,
                    radius = 3f + Random.nextFloat() * 6f,
                    color = colors[it % colors.size]
                )
            }
        }
        val t = remember { Animatable(0f) }
        LaunchedEffect(Unit) { t.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDone),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val reach = size.minDimension * 0.6f
                val start = 110.dp.toPx()
                particles.forEach { p ->
                    val d = start + reach * p.speed * t.value
                    val gravity = 260f * t.value * t.value
                    drawCircle(
                        p.color.copy(alpha = (1f - t.value).coerceIn(0f, 1f)),
                        radius = p.radius,
                        center = Offset(center.x + cos(p.angle) * d, center.y + sin(p.angle) * d + gravity)
                    )
                }
            }
            val pop by animateFloatAsState(1f, spring(dampingRatio = 0.45f), label = "pop")
            Column(
                Modifier.padding(32.dp).graphicsLayer { scaleX = 0.6f + 0.4f * t.value.coerceAtMost(1f); scaleY = scaleX; alpha = pop },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.celebrate_cleared), style = MaterialTheme.typography.titleMedium, color = Muted)
                AnimatedBytes(lastBytes, MaterialTheme.typography.displayMedium.copy(brush = BrandGradient))
                Text(
                    stringResource(R.string.celebrate_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
