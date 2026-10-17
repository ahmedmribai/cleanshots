package com.ironforge.cleanshots.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.ironforge.cleanshots.data.store.PrefsManager
import com.ironforge.cleanshots.ui.trash.TRASH_UNSUPPORTED_MESSAGE
import com.ironforge.cleanshots.ui.trash.TrashViewModel
import com.ironforge.cleanshots.ui.trash.rememberMediaRequester
import kotlinx.coroutines.launch

@Composable
fun DuplicatesScreen(
    scanVm: ScanViewModel,
    trashVm: TrashViewModel,
    snack: SnackbarHostState,
    onNeedPremium: () -> Unit,
    isPremium: Boolean,
    freeUsed: Int
) {
    val ui by scanVm.ui.collectAsState()
    val requester = rememberMediaRequester()
    val scope = rememberCoroutineScope()
    var checked by remember { mutableStateOf(setOf<Long>()) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("${ui.groups.size} duplicate groups • ${formatBytes(ui.groups.sumOf { g -> g.dupes.sumOf { it.size } })} reclaimable", color = Muted)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(ui.groups, key = { _, g -> g.keeper.mediaId }) { index, group ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Original (tap a dupe to make it original)", color = Muted)
                        AsyncImage(
                            model = group.keeper.contentUri.toUri(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(180.dp).background(Color.DarkGray),
                            contentScale = ContentScale.Crop
                        )
                        Text("${group.keeper.displayName} • ${formatBytes(group.keeper.size)}")
                        Spacer(Modifier.height(8.dp))
                        group.dupes.forEach { dupe ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Box(Modifier.size(72.dp).clickable { scanVm.swapKeeper(index, dupe.mediaId) }) {
                                    AsyncImage(model = dupe.contentUri.toUri(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f).clickable { scanVm.swapKeeper(index, dupe.mediaId) }) {
                                    Text(dupe.displayName, maxLines = 1)
                                    Text(formatBytes(dupe.size), color = Muted)
                                }
                                Checkbox(
                                    checked = dupe.mediaId in checked,
                                    onCheckedChange = {
                                        checked = if (it) checked + dupe.mediaId else checked - dupe.mediaId
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Color.Black)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                val toDelete = group.dupes.filter { it.mediaId in checked }
                                if (toDelete.isEmpty()) return@Button
                                if (!isPremium && freeUsed + toDelete.size > PrefsManager.FREE_LIMIT) { onNeedPremium(); return@Button }
                                if (!requester.isSupported) {
                                    scope.launch { snack.showSnackbar(TRASH_UNSUPPORTED_MESSAGE) }
                                    return@Button
                                }
                                val uris = toDelete.map { it.contentUri.toUri() }
                                val meta = toDelete.associate { it.contentUri to (it.displayName to it.size) }
                                requester.trash(uris) { approved ->
                                    if (!approved) return@trash
                                    trashVm.recordTrashed(uris, meta)
                                    val ids = toDelete.map { it.mediaId }.toSet()
                                    scanVm.removeTrashed(ids)
                                    checked = checked - ids
                                    scope.launch {
                                        val r = snack.showSnackbar("Moved to Trash", actionLabel = "UNDO", duration = SnackbarDuration.Long)
                                        if (r == SnackbarResult.ActionPerformed) {
                                            requester.untrash(uris) { restored ->
                                                if (restored) { trashVm.onUntrashed(uris); scanVm.start() }
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Delete checked dupes") }
                    }
                }
            }
        }
    }
}
