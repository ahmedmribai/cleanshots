package com.ironforge.cleanshots.ui.screens

import com.ironforge.cleanshots.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ironforge.cleanshots.data.db.TrashEntry
import com.ironforge.cleanshots.ui.components.EmptyState
import com.ironforge.cleanshots.ui.components.GradientButton
import com.ironforge.cleanshots.ui.components.Pill
import com.ironforge.cleanshots.ui.components.ScreenHeader
import com.ironforge.cleanshots.ui.components.SoftButton
import com.ironforge.cleanshots.ui.scan.ScanViewModel
import com.ironforge.cleanshots.ui.scan.formatBytes
import com.ironforge.cleanshots.ui.theme.Accent
import com.ironforge.cleanshots.ui.theme.BrandGradient
import com.ironforge.cleanshots.ui.theme.DangerGradient
import com.ironforge.cleanshots.ui.theme.Muted
import com.ironforge.cleanshots.ui.theme.OnAccent
import com.ironforge.cleanshots.ui.theme.Stroke
import com.ironforge.cleanshots.ui.theme.SurfaceCard
import com.ironforge.cleanshots.ui.theme.SurfaceHigh
import com.ironforge.cleanshots.ui.trash.Cleaner

@Composable
fun BlurryScreen(scanVm: ScanViewModel, cleaner: Cleaner) {
    val ui by scanVm.ui.collectAsState()
    MediaGridScreen(
        title = stringResource(R.string.category_blurry),
        items = ui.blurry,
        scanning = ui.scanning,
        cleaner = cleaner,
        emptyIcon = Icons.Rounded.BlurOn,
        emptyTitle = stringResource(R.string.blurry_empty_title),
        emptyMessage = stringResource(R.string.blurry_empty_message),
        details = ::imageDetails
    )
}

@Composable
fun TrashScreenInner(
    items: List<TrashEntry>,
    canRestore: Boolean,
    onRestore: () -> Unit,
    onDelete: (List<Long>) -> Unit
) {
    var sel by remember { mutableStateOf(setOf<Long>()) }
    val ids = remember(items) { items.mapTo(HashSet()) { it.mediaId } }
    val live = sel intersect ids
    val allSelected = items.isNotEmpty() && live.size == items.size

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.tab_trash),
            subtitle = pluralStringResource(R.plurals.items_and_size, items.size, items.size, formatBytes(items.sumOf { it.size }))
        ) {
            if (items.isNotEmpty()) {
                Pill(stringResource(if (allSelected) R.string.clear_selection else R.string.select_all), textColor = Accent, onClick = { sel = if (allSelected) emptySet() else ids })
            }
        }
        if (items.isEmpty()) {
            EmptyState(Icons.Rounded.DeleteOutline, stringResource(R.string.trash_empty_title), stringResource(R.string.trash_empty_message))
            return@Column
        }
        Text(
            stringResource(R.string.trash_explainer),
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton(stringResource(R.string.trash_undo_last), onClick = onRestore, leading = Icons.Rounded.Restore, enabled = canRestore, modifier = Modifier.weight(1f))
            GradientButton(
                if (live.isEmpty()) stringResource(R.string.trash_delete_forever) else stringResource(R.string.trash_delete_n, live.size),
                onClick = { onDelete(live.toList()); sel = emptySet() },
                enabled = live.isNotEmpty(),
                leading = Icons.Rounded.DeleteForever,
                brush = DangerGradient,
                contentColor = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.mediaId }) { e ->
                TrashRow(e, e.mediaId in live, Modifier.animateItem()) {
                    sel = if (e.mediaId in live) sel - e.mediaId else sel + e.mediaId
                }
            }
        }
    }
}

@Composable
private fun TrashRow(entry: TrashEntry, selected: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val checkScale by animateFloatAsState(if (selected) 1f else 0f, label = "trashCheck")
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) SurfaceHigh else SurfaceCard)
            .border(1.dp, Stroke, shape)
            .clickable(onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceHigh), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Image, contentDescription = null, tint = Muted)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.displayName.ifEmpty { stringResource(R.string.trash_item_fallback, entry.mediaId) }, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatBytes(entry.size), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Box(
            Modifier.size(26.dp).clip(CircleShape).border(1.5.dp, if (selected) Color.Transparent else Muted, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(26.dp).graphicsLayer { scaleX = checkScale; scaleY = checkScale }.clip(CircleShape).background(BrandGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = OnAccent, modifier = Modifier.size(16.dp))
            }
        }
    }
}
