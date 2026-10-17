package com.ironforge.cleanshots.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ironforge.cleanshots.ui.theme.BrandGradient
import com.ironforge.cleanshots.ui.theme.Muted
import com.ironforge.cleanshots.ui.theme.OnAccent
import com.ironforge.cleanshots.ui.theme.SurfaceHigh

@Composable
fun Photo(
    uri: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Color = SurfaceHigh
) {
    val ctx = LocalContext.current
    val request = remember(uri) { ImageRequest.Builder(ctx).data(uri.toUri()).crossfade(220).build() }
    AsyncImage(model = request, contentDescription = null, modifier = modifier.background(placeholder), contentScale = contentScale)
}

/** Grid tile that shrinks, dims and shows an animated check when selected. Long-press opens [onLongClick]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableThumb(
    uri: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    badge: String? = null,
    badgeHighlighted: Boolean = true,
    selectable: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(if (selected) 0.88f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "thumbScale")
    val selectedAlpha by animateFloatAsState(if (selected) 1f else 0f, label = "thumbSel")
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .combinedClickable(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick?.let { action ->
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        action()
                    }
                }
            )
    ) {
        Photo(uri, Modifier.fillMaxSize())
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.35f * selectedAlpha)))
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = selectedAlpha }
                .border(3.dp, BrandGradient, shape)
        )
        if (selectable) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .then(
                        if (selected) Modifier.background(BrandGradient)
                        else Modifier.background(Color.Black.copy(alpha = 0.3f)).border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = selected,
                    enter = scaleIn(spring(dampingRatio = 0.4f)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = OnAccent, modifier = Modifier.size(16.dp))
                }
            }
        }
        badge?.let {
            if (badgeHighlighted) Pill(it, Modifier.align(Alignment.BottomStart).padding(8.dp), brush = BrandGradient, textColor = OnAccent)
            else Pill(it, Modifier.align(Alignment.BottomStart).padding(8.dp))
        }
    }
}

/** Full-screen preview opened by long-press, with an optional primary action. */
@Composable
fun PhotoPreviewDialog(
    uri: String,
    title: String,
    details: String,
    onDismiss: () -> Unit,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(onClick = onDismiss)
                .systemBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Photo(
                uri,
                Modifier.fillMaxWidth().weight(1f, fill = false).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(details, color = Muted, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null) {
                Spacer(Modifier.height(20.dp))
                GradientButton(actionLabel, onClick = { onAction(); onDismiss() }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
