package com.ironforge.cleanshots.ui.review

import com.ironforge.cleanshots.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ironforge.cleanshots.data.db.ScannedImage
import com.ironforge.cleanshots.data.scan.MediaStoreScanner
import com.ironforge.cleanshots.ui.components.EmptyState
import com.ironforge.cleanshots.ui.components.GradientButton
import com.ironforge.cleanshots.ui.components.Photo
import com.ironforge.cleanshots.ui.components.Pill
import com.ironforge.cleanshots.ui.components.RoundIconButton
import com.ironforge.cleanshots.ui.components.SoftButton
import com.ironforge.cleanshots.ui.scan.formatBytes
import com.ironforge.cleanshots.ui.theme.Accent
import com.ironforge.cleanshots.ui.theme.BrandGradient
import com.ironforge.cleanshots.ui.theme.Danger
import com.ironforge.cleanshots.ui.theme.DangerGradient
import com.ironforge.cleanshots.ui.theme.Muted
import com.ironforge.cleanshots.ui.theme.OnAccent
import com.ironforge.cleanshots.ui.theme.Stroke
import com.ironforge.cleanshots.ui.theme.SurfaceHigh
import com.ironforge.cleanshots.ui.trash.Cleaner
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

/**
 * One photo at a time: swipe left to mark for cleaning, right to keep. Nothing is moved until the
 * user confirms the batch at the end, and every decision can be undone.
 */
@Composable
fun SwipeReviewScreen(candidates: List<ScannedImage>, cleaner: Cleaner, onClose: () -> Unit) {
    // Snapshot so the deck doesn't reshuffle when a background scan refreshes the lists.
    val deck = remember { candidates.toList() }
    var index by remember { mutableIntStateOf(0) }
    val marked = remember { mutableStateListOf<ScannedImage>() }
    val history = remember { mutableStateListOf<Boolean>() }
    var finished by remember { mutableStateOf(false) }

    if (deck.isEmpty()) {
        EmptyState(Icons.Rounded.TaskAlt, stringResource(R.string.review_empty_title), stringResource(R.string.review_empty_message))
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ReviewTopBar(index, deck.size, marked.size, onClose = onClose, onDone = { finished = true })
        AnimatedContent(
            targetState = finished || index >= deck.size,
            transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(150)) },
            label = "reviewState",
            modifier = Modifier.weight(1f)
        ) { done ->
            if (done) {
                ReviewSummary(
                    marked = marked,
                    onClean = { cleaner.clean(marked.toList()) { onClose() } },
                    onContinue = if (index < deck.size) ({ finished = false }) else null,
                    onClose = onClose
                )
            } else {
                SwipeDeck(
                    deck = deck,
                    index = index,
                    canUndo = history.isNotEmpty(),
                    onDecide = { delete ->
                        if (delete) marked.add(deck[index])
                        history.add(delete)
                        index++
                    },
                    onUndo = {
                        if (history.isNotEmpty()) {
                            if (history.removeAt(history.lastIndex)) marked.removeAt(marked.lastIndex)
                            index--
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ReviewTopBar(index: Int, total: Int, marked: Int, onClose: () -> Unit, onDone: () -> Unit) {
    val progress by animateFloatAsState((index.toFloat() / total).coerceIn(0f, 1f), label = "reviewProgress")
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton(Icons.Rounded.Close, stringResource(R.string.action_close), onClose, size = 44.dp)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(stringResource(R.string.review_position, (index + 1).coerceAtMost(total), total), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(SurfaceHigh)) {
                Box(Modifier.fillMaxWidth(progress).height(6.dp).clip(RoundedCornerShape(3.dp)).background(BrandGradient))
            }
        }
        Pill(if (marked > 0) stringResource(R.string.review_done_count, marked) else stringResource(R.string.review_done), brush = if (marked > 0) BrandGradient else null,
            textColor = if (marked > 0) OnAccent else Muted, onClick = onDone)
    }
}

@Composable
private fun SwipeDeck(
    deck: List<ScannedImage>,
    index: Int,
    canUndo: Boolean,
    onDecide: (delete: Boolean) -> Unit,
    onUndo: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    var armed by remember { mutableStateOf(false) }
    var flinging by remember { mutableStateOf(false) }
    val threshold = widthPx * 0.28f

    fun fling(delete: Boolean) {
        if (flinging) return
        flinging = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            offsetX.animateTo(if (delete) -widthPx * 1.4f else widthPx * 1.4f, tween(230))
            onDecide(delete)
            offsetX.snapTo(0f)
            armed = false
            flinging = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            val measured = with(LocalDensity.current) { maxWidth.toPx() }
            LaunchedEffect(measured) { widthPx = measured }
            val drag = (abs(offsetX.value) / threshold).coerceAtMost(1f)
            deck.getOrNull(index + 1)?.let { next ->
                key(next.mediaId) {
                    ReviewCard(next, Modifier.graphicsLayer {
                        val s = 0.92f + 0.08f * drag
                        scaleX = s
                        scaleY = s
                        alpha = 0.55f + 0.45f * drag
                    })
                }
            }
            deck.getOrNull(index)?.let { current ->
                key(current.mediaId) {
                    ReviewCard(
                        current,
                        Modifier
                            .graphicsLayer {
                                translationX = offsetX.value
                                rotationZ = offsetX.value / widthPx * 14f
                            }
                            .pointerInput(index) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (abs(offsetX.value) > threshold) fling(offsetX.value < 0)
                                        else scope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.6f)) }
                                    },
                                    onDragCancel = { scope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.6f)) } }
                                ) { change, amount ->
                                    if (flinging) return@detectHorizontalDragGestures
                                    change.consume()
                                    val moved = offsetX.value + amount
                                    scope.launch { offsetX.snapTo(moved) }
                                    val past = abs(moved) > threshold
                                    if (past != armed) {
                                        armed = past
                                        if (past) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                        stamp = (offsetX.value / threshold).coerceIn(-1f, 1f)
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(Icons.Rounded.Close, stringResource(R.string.action_clean), { fling(true) }, size = 68.dp, brush = DangerGradient, tint = Color.White)
            RoundIconButton(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.action_undo), onUndo, size = 48.dp, enabled = canUndo && !flinging)
            RoundIconButton(Icons.Rounded.Check, stringResource(R.string.action_keep), { fling(false) }, size = 68.dp, brush = BrandGradient, tint = OnAccent)
        }
    }
}

/** [stamp] runs from -1 (dragged fully towards clean) to 1 (dragged fully towards keep). */
@Composable
private fun ReviewCard(image: ScannedImage, modifier: Modifier, stamp: Float = 0f) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier
            .fillMaxSize()
            .shadow(24.dp, shape)
            .clip(shape)
            .background(SurfaceHigh)
            .border(1.dp, Stroke, shape)
    ) {
        // Blurred, enlarged copy fills the letterbox so landscape photos don't float in empty space.
        Photo(image.contentUri, Modifier.fillMaxSize().blur(36.dp).graphicsLayer { alpha = 0.5f })
        Photo(image.contentUri, Modifier.fillMaxSize(), contentScale = ContentScale.Fit, placeholder = Color.Transparent)
        Box(Modifier.matchParentSize().background(Danger.copy(alpha = 0.28f * (-stamp).coerceAtLeast(0f))))
        Box(Modifier.matchParentSize().background(Accent.copy(alpha = 0.22f * stamp.coerceAtLeast(0f))))
        Stamp(stringResource(R.string.stamp_clean), Danger, alpha = (-stamp).coerceAtLeast(0f), rotation = 12f, modifier = Modifier.align(Alignment.TopEnd))
        Stamp(stringResource(R.string.stamp_keep), Accent, alpha = stamp.coerceAtLeast(0f), rotation = -12f, modifier = Modifier.align(Alignment.TopStart))
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 18.dp)
        ) {
            val isShot = MediaStoreScanner.isScreenshot(image.relativePath, image.bucket, image.displayName)
            Pill(stringResource(if (isShot) R.string.tag_screenshot else R.string.tag_blurry))
            Spacer(Modifier.height(8.dp))
            Text(image.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
            Text(
                "${formatBytes(image.size)} · ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(image.dateAdded * 1000))}",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun Stamp(text: String, color: Color, alpha: Float, rotation: Float, modifier: Modifier) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier
            .padding(28.dp)
            .graphicsLayer {
                this.alpha = alpha
                rotationZ = rotation
                scaleX = 0.8f + 0.2f * alpha
                scaleY = scaleX
            }
            .border(3.dp, color, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun ReviewSummary(
    marked: List<ScannedImage>,
    onClean: () -> Unit,
    onContinue: (() -> Unit)?,
    onClose: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (marked.isEmpty()) {
            Text(stringResource(R.string.review_all_kept), style = MaterialTheme.typography.displaySmall.copy(brush = BrandGradient))
            Text(stringResource(R.string.review_nothing_marked), color = Muted, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(stringResource(R.string.review_ready), color = Muted, style = MaterialTheme.typography.titleMedium)
            Text(formatBytes(marked.sumOf { it.size }), style = MaterialTheme.typography.displayMedium.copy(brush = BrandGradient))
            Text(
                pluralStringResource(R.plurals.review_will_move, marked.size, marked.size),
                color = Muted,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(28.dp))
            GradientButton(pluralStringResource(R.plurals.move_n_to_trash, marked.size, marked.size), onClick = onClean, leading = Icons.Rounded.DeleteSweep, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        if (onContinue != null) {
            SoftButton(stringResource(R.string.review_keep_reviewing), onClick = onContinue, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }
        SoftButton(stringResource(R.string.action_close), onClick = onClose, modifier = Modifier.fillMaxWidth())
    }
}
