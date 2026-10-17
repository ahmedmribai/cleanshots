package com.ironforge.cleanshots.ui.screens

import com.ironforge.cleanshots.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ironforge.cleanshots.data.db.ScannedImage
import com.ironforge.cleanshots.ui.components.CleanActionBar
import com.ironforge.cleanshots.ui.components.EmptyState
import com.ironforge.cleanshots.ui.components.GlassCard
import com.ironforge.cleanshots.ui.components.PhotoPreviewDialog
import com.ironforge.cleanshots.ui.components.Pill
import com.ironforge.cleanshots.ui.components.ScreenHeader
import com.ironforge.cleanshots.ui.components.SelectableThumb
import com.ironforge.cleanshots.ui.components.ShimmerGrid
import com.ironforge.cleanshots.ui.components.SoftButton
import com.ironforge.cleanshots.ui.components.enterAnimation
import com.ironforge.cleanshots.ui.scan.ScanViewModel
import com.ironforge.cleanshots.ui.scan.formatBytes
import com.ironforge.cleanshots.ui.theme.Accent
import com.ironforge.cleanshots.ui.theme.Muted
import com.ironforge.cleanshots.ui.trash.Cleaner

private data class Preview(val image: ScannedImage, val groupIndex: Int, val isKeeper: Boolean)

@Composable
fun DuplicatesScreen(scanVm: ScanViewModel, cleaner: Cleaner) {
    val ui by scanVm.ui.collectAsState()
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    val extras = remember(ui.groups) { ui.groups.flatMap { it.dupes } }
    val extraIds = remember(extras) { extras.mapTo(HashSet()) { it.mediaId } }
    val live = selected intersect extraIds
    val chosen = extras.filter { it.mediaId in live }
    val smartSelect by scanVm.smartSelect.collectAsState()
    LaunchedEffect(smartSelect, extraIds) {
        if (smartSelect && extraIds.isNotEmpty()) {
            selected = extraIds
            scanVm.consumeSmartSelect()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.tab_duplicates),
                subtitle = if (ui.groups.isEmpty() && ui.scanning) stringResource(R.string.dupes_comparing)
                else pluralStringResource(R.plurals.dupes_subtitle, ui.groups.size, ui.groups.size, formatBytes(extras.sumOf { it.size }))
            ) {
                if (extras.isNotEmpty()) {
                    Pill(
                        stringResource(if (live.size == extras.size) R.string.clear_selection else R.string.select_extras),
                        textColor = Accent,
                        onClick = { selected = if (live.size == extras.size) emptySet() else extraIds }
                    )
                }
            }
            when {
                ui.groups.isEmpty() && ui.scanning -> ShimmerGrid(Modifier.fillMaxSize())
                ui.groups.isEmpty() -> EmptyState(
                    Icons.Rounded.ContentCopy,
                    stringResource(R.string.dupes_empty_title),
                    stringResource(R.string.dupes_empty_message)
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SoftButton(
                            stringResource(R.string.dupes_smart_select),
                            onClick = { selected = extraIds },
                            leading = Icons.Rounded.AutoAwesome,
                            tint = Accent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    itemsIndexed(ui.groups, key = { _, g -> g.keeper.mediaId }) { index, group ->
                        GlassCard(Modifier.fillMaxWidth().animateItem().enterAnimation(index.coerceAtMost(6))) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(pluralStringResource(R.plurals.dupes_copies, group.dupes.size + 1, group.dupes.size + 1), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text(formatBytes(group.dupes.sumOf { it.size }), color = Muted, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item(key = group.keeper.mediaId) {
                                    SelectableThumb(
                                        uri = group.keeper.contentUri,
                                        selected = false,
                                        selectable = false,
                                        badge = stringResource(R.string.badge_best),
                                        onClick = { preview = Preview(group.keeper, index, isKeeper = true) },
                                        onLongClick = { preview = Preview(group.keeper, index, isKeeper = true) },
                                        modifier = Modifier.width(118.dp)
                                    )
                                }
                                items(group.dupes, key = { it.mediaId }) { dupe ->
                                    val isSelected = dupe.mediaId in live
                                    SelectableThumb(
                                        uri = dupe.contentUri,
                                        selected = isSelected,
                                        onClick = { selected = if (isSelected) selected - dupe.mediaId else selected + dupe.mediaId },
                                        onLongClick = { preview = Preview(dupe, index, isKeeper = false) },
                                        modifier = Modifier.width(118.dp).animateItem()
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.dupes_hint), color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        CleanActionBar(
            count = chosen.size,
            bytes = chosen.sumOf { it.size },
            onClean = { cleaner.clean(chosen) { selected = selected - chosen.mapTo(HashSet()) { it.mediaId } } },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    preview?.let { p ->
        PhotoPreviewDialog(
            uri = p.image.contentUri,
            title = p.image.displayName,
            details = "${formatBytes(p.image.size)} · ${p.image.width} × ${p.image.height}",
            onDismiss = { preview = null },
            actionLabel = if (p.isKeeper) null else stringResource(R.string.dupes_keep_instead),
            onAction = {
                scanVm.swapKeeper(p.groupIndex, p.image.mediaId)
                selected = selected - p.image.mediaId
            }
        )
    }
}
