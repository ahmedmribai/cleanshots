package com.ironforge.cleanshots.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.ironforge.cleanshots.ui.scan.ScanViewModel
import com.ironforge.cleanshots.ui.scan.formatBytes
import com.ironforge.cleanshots.ui.theme.Accent
import com.ironforge.cleanshots.ui.theme.Muted
import com.ironforge.cleanshots.ui.theme.SurfaceCard

@Composable
fun BlurryScreen(scanVm: ScanViewModel) {
    val ui by scanVm.ui.collectAsState()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("${ui.blurry.size} blurry photos • ${formatBytes(ui.blurry.sumOf { it.size })}", color = Muted)
        LazyColumn(Modifier.fillMaxSize()) {
            items(ui.blurry, key = { it.mediaId }) { img ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = img.contentUri.toUri(), contentDescription = null, modifier = Modifier.size(64.dp), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(img.displayName, maxLines = 1)
                            Text("blur ${String.format(java.util.Locale.getDefault(), "%.1f", img.blurScore ?: 0f)} • ${formatBytes(img.size)}", color = Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrashScreenInner(
    items: List<com.ironforge.cleanshots.data.db.TrashEntry>,
    onRestore: () -> Unit,
    onDelete: (List<Long>) -> Unit
) {
    var sel by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(setOf<Long>()) }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("${items.size} items in trash (system keeps 30 days)", color = Muted)
        Row {
            androidx.compose.material3.TextButton(onClick = onRestore) { Text("Restore last", color = Accent) }
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.TextButton(onClick = { onDelete(sel.toList()); sel = emptySet() }) { Text("Delete permanently", color = Color(0xFFFF6B6B)) }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(items, key = { it.mediaId }) { e ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = e.mediaId in sel,
                            onCheckedChange = { sel = if (it) sel + e.mediaId else sel - e.mediaId },
                            colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Color.Black)
                        )
                        Column {
                            Text(e.displayName.ifEmpty { "photo ${e.mediaId}" }, maxLines = 1)
                            Text(formatBytes(e.size), color = Muted)
                        }
                    }
                }
            }
        }
    }
}
