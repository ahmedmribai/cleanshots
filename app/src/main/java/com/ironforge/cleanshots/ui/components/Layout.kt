package com.ironforge.cleanshots.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ironforge.cleanshots.ui.scan.formatBytes
import com.ironforge.cleanshots.ui.theme.Accent
import com.ironforge.cleanshots.ui.theme.Background
import com.ironforge.cleanshots.ui.theme.BrandGradient
import com.ironforge.cleanshots.ui.theme.Muted
import com.ironforge.cleanshots.ui.theme.OnAccent
import com.ironforge.cleanshots.ui.theme.Stroke
import com.ironforge.cleanshots.ui.theme.SurfaceCard
import com.ironforge.cleanshots.ui.theme.SurfaceHigh

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction, pressed = 0.97f)
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(SurfaceHigh, SurfaceCard)))
            .border(1.dp, Stroke, shape)
            .then(if (onClick != null) Modifier.clickable(interaction, ripple(), onClick = onClick) else Modifier)
            .padding(18.dp),
        content = content
    )
}

@Composable
fun ScreenHeader(title: String, subtitle: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            AnimatedContent(
                targetState = subtitle,
                transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith (fadeOut() + slideOutVertically { -it / 2 }) },
                label = "subtitle"
            ) { Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium) }
        }
        trailing()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(40.dp).enterAnimation(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(SurfaceHigh).border(1.dp, Stroke, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

/** Floating bar that slides up while something is selected. */
@Composable
fun CleanActionBar(count: Int, bytes: Long, onClean: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = count > 0,
        modifier = modifier,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Background)))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            GradientButton(
                text = "Move $count to Trash · ${formatBytes(bytes)}",
                onClick = onClean,
                leading = Icons.Rounded.DeleteSweep,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Pulsing placeholder tiles shown while the first scan is still running. */
@Composable
fun ShimmerGrid(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "shimmer")
        .animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "shimmerAlpha")
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        userScrollEnabled = false,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(12) {
            Box(
                Modifier
                    .aspectRatio(1f)
                    .graphicsLayer { alpha = pulse }
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceHigh)
            )
        }
    }
}

@Composable
fun GradientIconBadge(icon: ImageVector, modifier: Modifier = Modifier, brush: Brush = BrandGradient) {
    Box(modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(brush), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = OnAccent, modifier = Modifier.size(24.dp))
    }
}
