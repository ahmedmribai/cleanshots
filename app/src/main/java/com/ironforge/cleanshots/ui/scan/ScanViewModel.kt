package com.ironforge.cleanshots.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ironforge.cleanshots.data.db.ScannedImage
import com.ironforge.cleanshots.data.scan.DuplicateGroup
import com.ironforge.cleanshots.data.scan.ScanProgress
import com.ironforge.cleanshots.data.scan.ScanRepository
import com.ironforge.cleanshots.data.scan.VideoItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ScanUi(
    val scanning: Boolean = false,
    val progress: ScanProgress? = null,
    val shots: List<ScannedImage> = emptyList(),
    val shotsBytes: Long = 0L,
    val groups: List<DuplicateGroup> = emptyList(),
    val blurry: List<ScannedImage> = emptyList(),
    val videos: List<VideoItem> = emptyList()
)

class ScanViewModel(private val repo: ScanRepository) : ViewModel() {
    private val _ui = MutableStateFlow(ScanUi())
    val ui: StateFlow<ScanUi> = _ui
    private var job: Job? = null

    fun start() {
        if (_ui.value.scanning) return
        job?.cancel()
        job = viewModelScope.launch {
            _ui.value = _ui.value.copy(scanning = true)
            repo.fullScan().collect { p ->
                _ui.value = _ui.value.copy(progress = p, scanning = p.phase != "Done")
            }
            refresh()
        }
    }

    suspend fun refresh() {
        val (shots, bytes) = repo.screenshots()
        val groups = repo.duplicateGroups()
        val blurry = repo.blurry()
        val videos = repo.largeVideos()
        _ui.value = _ui.value.copy(shots = shots, shotsBytes = bytes, groups = groups, blurry = blurry, videos = videos, scanning = false)
    }

    /** Hides trashed items right away; the next scan prunes their DB rows. */
    fun removeTrashed(ids: Set<Long>) {
        val cur = _ui.value
        val shots = cur.shots.filter { it.mediaId !in ids }
        val groups = cur.groups.mapNotNull { g ->
            val dupes = g.dupes.filter { it.mediaId !in ids }
            if (g.keeper.mediaId in ids || dupes.isEmpty()) null else DuplicateGroup(g.keeper, dupes)
        }
        _ui.value = cur.copy(
            shots = shots,
            shotsBytes = shots.sumOf { it.size },
            groups = groups,
            blurry = cur.blurry.filter { it.mediaId !in ids },
            videos = cur.videos.filter { it.mediaId !in ids }
        )
    }

    fun swapKeeper(groupIndex: Int, newKeeperId: Long) {
        val groups = _ui.value.groups.toMutableList()
        if (groupIndex !in groups.indices) return
        val g = groups[groupIndex]
        val all = listOf(g.keeper) + g.dupes
        val nk = all.firstOrNull { it.mediaId == newKeeperId } ?: return
        groups[groupIndex] = DuplicateGroup(nk, all.filter { it.mediaId != nk.mediaId })
        _ui.value = _ui.value.copy(groups = groups)
    }

    class Factory(private val repo: ScanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = ScanViewModel(repo) as T
    }
}

fun formatBytes(b: Long): String {
    if (b < 1024) return "$b B"
    val kb = b / 1024.0
    if (kb < 1024) return String.format(java.util.Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
    return String.format(java.util.Locale.getDefault(), "%.2f GB", mb / 1024.0)
}
