package com.ironforge.cleanshots.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ironforge.cleanshots.data.db.ScannedImage
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
fun ScreenshotsScreen(
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
    var selected by remember { mutableStateOf(setOf<Long>()) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("${ui.shots.size} screenshots • ${formatBytes(ui.shotsBytes)}")
        ui.progress?.takeIf { ui.scanning }?.let { p ->
            if (p.total > 0) {
                LinearProgressIndicator(
                    progress = { (p.done.toFloat() / p.total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                Text("${p.phase} ${p.done}/${p.total}", color = Muted)
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
                Text("${p.phase}...", color = Muted)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                selected = if (selected.size == ui.shots.size) emptySet() else ui.shots.map { it.mediaId }.toSet()
            }) { Text(if (selected.size == ui.shots.size) "Deselect all" else "Select all", color = Accent) }
            Button(
                onClick = {
                    if (!isPremium && freeUsed + selected.size > PrefsManager.FREE_LIMIT) { onNeedPremium(); return@Button }
                    val chosen = ui.shots.filter { it.mediaId in selected }
                    val uris = chosen.map { it.contentUri.toUri() }
                    if (uris.isEmpty()) return@Button
                    if (!requester.isSupported) {
                        scope.launch { snack.showSnackbar(TRASH_UNSUPPORTED_MESSAGE) }
                        return@Button
                    }
                    val meta = chosen.associate { it.contentUri to (it.displayName to it.size) }
                    requester.trash(uris) { approved ->
                        if (!approved) return@trash
                        trashVm.recordTrashed(uris, meta)
                        scanVm.removeTrashed(chosen.map { it.mediaId }.toSet())
                        selected = emptySet()
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
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)
            ) { Text("Delete Selected (${selected.size})") }
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize()) {
            items(ui.shots, key = { it.mediaId }) { img: ScannedImage ->
                val sel = img.mediaId in selected
                Card(
                    modifier = Modifier.padding(4.dp).clickable {
                        selected = if (sel) selected - img.mediaId else selected + img.mediaId
                    },
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Box {
                        AsyncImage(
                            model = img.contentUri.toUri(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color.DarkGray),
                            contentScale = ContentScale.Crop
                        )
                        Checkbox(
                            checked = sel,
                            onCheckedChange = {
                                selected = if (it) selected + img.mediaId else selected - img.mediaId
                            },
                            modifier = Modifier.align(Alignment.TopEnd),
                            colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Color.Black)
                        )
                    }
                }
            }
        }
    }
}
