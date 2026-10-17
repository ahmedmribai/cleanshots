package com.ironforge.cleanshots.ui.home

import com.ironforge.cleanshots.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ironforge.cleanshots.data.Cleanable
import com.ironforge.cleanshots.data.scan.ScanRepository
import com.ironforge.cleanshots.ui.components.AnimatedBytes
import com.ironforge.cleanshots.ui.components.GlassCard
import com.ironforge.cleanshots.ui.components.GradientButton
import com.ironforge.cleanshots.ui.components.GradientIconBadge
import com.ironforge.cleanshots.ui.components.Photo
import com.ironforge.cleanshots.ui.components.Pill
import com.ironforge.cleanshots.ui.components.ProgressRing
import com.ironforge.cleanshots.ui.components.SoftButton
import com.ironforge.cleanshots.ui.components.enterAnimation
import com.ironforge.cleanshots.ui.scan.ScanViewModel
import com.ironforge.cleanshots.ui.scan.formatBytes
import com.ironforge.cleanshots.ui.theme.Accent
import com.ironforge.cleanshots.ui.theme.Background
import com.ironforge.cleanshots.ui.theme.BrandGradient
import com.ironforge.cleanshots.ui.theme.Muted
import com.ironforge.cleanshots.ui.theme.OnAccent
import com.ironforge.cleanshots.ui.trash.Cleaner

@Composable
fun HomeScreen(
    scanVm: ScanViewModel,
    cleaner: Cleaner,
    onOpen: (route: String) -> Unit,
    onReview: () -> Unit,
    onUpgrade: () -> Unit
) {
    val ui by scanVm.ui.collectAsState()
    val extras = remember(ui.groups) { ui.groups.flatMap { it.dupes } }
    val extrasBytes = extras.sumOf { it.size }
    val blurryBytes = ui.blurry.sumOf { it.size }
    val videoBytes = ui.videos.sumOf { it.size }
    val reviewCount = remember(ui.shots, ui.blurry) { (ui.shots + ui.blurry).distinctBy { it.mediaId }.size }
    val total = ui.shotsBytes + extrasBytes + blurryBytes + videoBytes
    val itemCount = ui.shots.size + extras.size + ui.blurry.size + ui.videos.size

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth().enterAnimation(0), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge, color = Accent)
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
            }
            val left = cleaner.freeLeft
            if (left == null) {
                Pill(stringResource(R.string.badge_pro), brush = BrandGradient, textColor = OnAccent)
            } else {
                Pill(pluralStringResource(R.plurals.free_left, left, left), textColor = Accent, onClick = onUpgrade)
            }
        }

        HeroRing(ui.scanning, ui.progress?.done ?: 0, ui.progress?.total ?: 0, ui.progress?.phase, total, itemCount, Modifier.enterAnimation(1))

        if (extras.isNotEmpty()) {
            Column(Modifier.enterAnimation(2)) {
                GradientButton(
                    stringResource(R.string.smart_clean_button, formatBytes(extrasBytes)),
                    onClick = { scanVm.requestSmartSelect(); onOpen("dupes") },
                    leading = Icons.Rounded.AutoAwesome,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    pluralStringResource(R.plurals.smart_clean_caption, extras.size, extras.size),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        CategoryCard(Icons.Rounded.Screenshot, stringResource(R.string.tab_screenshots), ui.shots.size, ui.shotsBytes, ui.shots, Modifier.enterAnimation(3), loaded = ui.loaded) { onOpen("shots") }
        CategoryCard(Icons.Rounded.ContentCopy, stringResource(R.string.tab_duplicates), extras.size, extrasBytes, extras, Modifier.enterAnimation(4), loaded = ui.loaded) { onOpen("dupes") }
        CategoryCard(Icons.Rounded.VideoLibrary, stringResource(R.string.category_large_videos), ui.videos.size, videoBytes, ui.videos, Modifier.enterAnimation(5), loaded = ui.loaded) { onOpen("videos") }
        CategoryCard(Icons.Rounded.BlurOn, stringResource(R.string.category_blurry), ui.blurry.size, blurryBytes, ui.blurry, Modifier.enterAnimation(5), loaded = ui.loaded) { onOpen("blurry") }

        GlassCard(Modifier.fillMaxWidth().enterAnimation(6), onClick = if (reviewCount > 0) onReview else null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientIconBadge(Icons.Rounded.Swipe)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.swipe_review_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (reviewCount > 0) pluralStringResource(R.plurals.swipe_review_subtitle, reviewCount, reviewCount) else stringResource(R.string.swipe_review_empty),
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Muted)
            }
        }

        if (cleaner.freeLeft != null) {
            SoftButton(
                stringResource(R.string.unlock_unlimited),
                onClick = onUpgrade,
                leading = Icons.Rounded.WorkspacePremium,
                tint = Accent,
                modifier = Modifier.fillMaxWidth().enterAnimation(7)
            )
        }

        if (!ui.scanning) {
            SoftButton(
                stringResource(R.string.scan_again),
                onClick = { scanVm.start() },
                leading = Icons.Rounded.Refresh,
                modifier = Modifier.fillMaxWidth().enterAnimation(8)
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HeroRing(
    scanning: Boolean,
    done: Int,
    total: Int,
    phase: String?,
    bytes: Long,
    items: Int,
    modifier: Modifier
) {
    Box(modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        val progress = when {
            !scanning -> 1f
            total > 0 -> done.toFloat() / total
            else -> null
        }
        ProgressRing(progress, Modifier.size(244.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (scanning) {
                    Text(
                        stringResource(if (phase == ScanRepository.PHASE_LISTING || total == 0) R.string.ring_finding else R.string.ring_analyzing),
                        color = Muted,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        if (total > 0) stringResource(R.string.ring_percent, (done * 100L / total).toInt()) else "…",
                        style = MaterialTheme.typography.displaySmall.copy(brush = BrandGradient)
                    )
                    if (total > 0) Text(stringResource(R.string.ring_progress, done, total), color = Muted, style = MaterialTheme.typography.bodySmall)
                } else if (items == 0) {
                    Text(stringResource(R.string.ring_all_clean), style = MaterialTheme.typography.displaySmall.copy(brush = BrandGradient))
                    Text(stringResource(R.string.ring_nothing), color = Muted, style = MaterialTheme.typography.bodySmall)
                } else {
                    AnimatedBytes(bytes, MaterialTheme.typography.displaySmall.copy(brush = BrandGradient))
                    Text(pluralStringResource(R.plurals.ring_can_be_cleared, items, items), color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    icon: ImageVector,
    title: String,
    count: Int,
    bytes: Long,
    previews: List<Cleanable>,
    modifier: Modifier,
    loaded: Boolean,
    onClick: () -> Unit
) {
    GlassCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientIconBadge(icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(if (!loaded) "…" else if (count == 0) stringResource(R.string.category_none) else stringResource(R.string.count_and_size, count, formatBytes(bytes)), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            ThumbStack(previews.take(3))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Muted)
        }
    }
}

@Composable
private fun ThumbStack(images: List<Cleanable>) {
    if (images.isEmpty()) return
    Box(Modifier.width((40 + (images.size - 1) * 18).dp).height(40.dp)) {
        images.forEachIndexed { i, img ->
            val shape = RoundedCornerShape(10.dp)
            Photo(
                img.contentUri,
                Modifier
                    .offset(x = (i * 18).dp)
                    .size(40.dp)
                    .clip(shape)
                    .border(2.dp, Background, shape)
            )
        }
    }
}
