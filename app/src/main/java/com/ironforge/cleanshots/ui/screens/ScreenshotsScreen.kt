package com.ironforge.cleanshots.ui.screens

import com.ironforge.cleanshots.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ironforge.cleanshots.data.Cleanable
import com.ironforge.cleanshots.data.db.ScannedImage
import com.ironforge.cleanshots.data.scan.VideoScanner
import com.ironforge.cleanshots.ui.components.CleanActionBar
import com.ironforge.cleanshots.ui.components.EmptyState
import com.ironforge.cleanshots.ui.components.PhotoPreviewDialog
import com.ironforge.cleanshots.ui.components.Pill
import com.ironforge.cleanshots.ui.components.ScreenHeader
import com.ironforge.cleanshots.ui.components.SelectableThumb
import com.ironforge.cleanshots.ui.components.ShimmerGrid
import com.ironforge.cleanshots.ui.scan.ScanViewModel
import com.ironforge.cleanshots.ui.scan.formatBytes
import com.ironforge.cleanshots.ui.theme.Accent
import com.ironforge.cleanshots.ui.trash.Cleaner

@Composable
fun ScreenshotsScreen(scanVm: ScanViewModel, cleaner: Cleaner) {
    val ui by scanVm.ui.collectAsState()
    MediaGridScreen(
        title = stringResource(R.string.tab_screenshots),
        items = ui.shots,
        scanning = ui.scanning,
        cleaner = cleaner,
        emptyIcon = Icons.Rounded.Screenshot,
        emptyTitle = stringResource(R.string.screens_empty_title),
        emptyMessage = stringResource(R.string.screens_empty_message),
        details = ::imageDetails
    )
}

@Composable
fun VideosScreen(scanVm: ScanViewModel, cleaner: Cleaner) {
    val ui by scanVm.ui.collectAsState()
    MediaGridScreen(
        title = stringResource(R.string.category_large_videos),
        items = ui.videos,
        scanning = ui.scanning,
        cleaner = cleaner,
        emptyIcon = Icons.Rounded.VideoLibrary,
        emptyTitle = stringResource(R.string.videos_empty_title),
        emptyMessage = stringResource(R.string.videos_empty_message, formatBytes(VideoScanner.LARGE_VIDEO_BYTES)),
        columns = 2,
        badge = { "${VideoScanner.formatDuration(it.durationMs)} · ${formatBytes(it.size)}" },
        details = { formatBytes(it.size) + " · " + VideoScanner.formatDuration(it.durationMs) }
    )
}

fun imageDetails(img: ScannedImage): String = "${formatBytes(img.size)} · ${img.width} × ${img.height}"

/** Selectable grid with a floating clean bar; long-press previews an item. */
@Composable
fun <T : Cleanable> MediaGridScreen(
    title: String,
    items: List<T>,
    scanning: Boolean,
    cleaner: Cleaner,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String,
    details: (T) -> String,
    columns: Int = 3,
    badge: ((T) -> String)? = null
) {
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var preview by remember { mutableStateOf<T?>(null) }
    val ids = remember(items) { items.mapTo(HashSet()) { it.mediaId } }
    val live = selected intersect ids
    val chosen = items.filter { it.mediaId in live }
    val allSelected = items.isNotEmpty() && live.size == items.size

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = title,
                subtitle = if (items.isEmpty() && scanning) stringResource(R.string.scanning_gallery)
                else pluralStringResource(R.plurals.items_and_size, items.size, items.size, formatBytes(items.sumOf { it.size }))
            ) {
                if (items.isNotEmpty()) {
                    Pill(
                        stringResource(if (allSelected) R.string.clear_selection else R.string.select_all),
                        textColor = Accent,
                        onClick = { selected = if (allSelected) emptySet() else ids }
                    )
                }
            }
            when {
                items.isEmpty() && scanning -> ShimmerGrid(Modifier.fillMaxSize())
                items.isEmpty() -> EmptyState(emptyIcon, emptyTitle, emptyMessage)
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items, key = { it.mediaId }) { item ->
                        val isSelected = item.mediaId in live
                        SelectableThumb(
                            uri = item.contentUri,
                            selected = isSelected,
                            onClick = { selected = if (isSelected) selected - item.mediaId else selected + item.mediaId },
                            onLongClick = { preview = item },
                            badge = badge?.invoke(item),
                            badgeHighlighted = false,
                            modifier = Modifier.animateItem()
                        )
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

    preview?.let { item ->
        PhotoPreviewDialog(
            uri = item.contentUri,
            title = item.displayName,
            details = details(item),
            onDismiss = { preview = null },
            actionLabel = stringResource(R.string.move_to_trash),
            onAction = { cleaner.clean(listOf(item)) }
        )
    }
}
