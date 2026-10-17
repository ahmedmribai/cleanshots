package com.ironforge.cleanshots.ui.screens

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
import com.ironforge.cleanshots.data.db.ScannedImage
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
    PhotoGridScreen(
        title = "Screenshots",
        images = ui.shots,
        scanning = ui.scanning,
        cleaner = cleaner,
        emptyIcon = Icons.Rounded.Screenshot,
        emptyTitle = "No screenshots",
        emptyMessage = "Nothing to clear here. New screenshots will show up after the next scan."
    )
}

/** Selectable photo grid with a floating clean bar; long-press previews a photo. */
@Composable
fun PhotoGridScreen(
    title: String,
    images: List<ScannedImage>,
    scanning: Boolean,
    cleaner: Cleaner,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String
) {
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var preview by remember { mutableStateOf<ScannedImage?>(null) }
    val ids = remember(images) { images.mapTo(HashSet()) { it.mediaId } }
    val live = selected intersect ids
    val chosen = images.filter { it.mediaId in live }
    val allSelected = images.isNotEmpty() && live.size == images.size

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = title,
                subtitle = if (images.isEmpty() && scanning) "Scanning your gallery…"
                else "${images.size} photos · ${formatBytes(images.sumOf { it.size })}"
            ) {
                if (images.isNotEmpty()) {
                    Pill(
                        if (allSelected) "CLEAR" else "SELECT ALL",
                        textColor = Accent,
                        onClick = { selected = if (allSelected) emptySet() else ids }
                    )
                }
            }
            when {
                images.isEmpty() && scanning -> ShimmerGrid(Modifier.fillMaxSize())
                images.isEmpty() -> EmptyState(emptyIcon, emptyTitle, emptyMessage)
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(images, key = { it.mediaId }) { img ->
                        val isSelected = img.mediaId in live
                        SelectableThumb(
                            uri = img.contentUri,
                            selected = isSelected,
                            onClick = { selected = if (isSelected) selected - img.mediaId else selected + img.mediaId },
                            onLongClick = { preview = img },
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

    preview?.let { img ->
        PhotoPreviewDialog(img, onDismiss = { preview = null }, actionLabel = "Move to Trash", onAction = { cleaner.clean(listOf(img)) })
    }
}
