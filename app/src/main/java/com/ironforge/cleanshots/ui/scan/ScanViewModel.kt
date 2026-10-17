package com.ironforge.cleanshots.ui.scan

import android.os.SystemClock
import android.util.Log
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
    val videos: List<VideoItem> = emptyList(),
    /** False until the first results (cached or fresh) have been loaded. */
    val loaded: Boolean = false
)

private const val TAG = "CleanShots"

class ScanViewModel(private val repo: ScanRepository) : ViewModel() {
    private val _ui = MutableStateFlow(ScanUi())
    val ui: StateFlow<ScanUi> = _ui
    private var job: Job? = null

    /** Set by Smart Clean on Home; Duplicates consumes it to pre-select every extra copy for review. */
    private val _smartSelect = MutableStateFlow(false)
    val smartSelect: StateFlow<Boolean> = _smartSelect

    fun requestSmartSelect() { _smartSelect.value = true }
    fun consumeSmartSelect() { _smartSelect.value = false }

    fun start() {
        if (_ui.value.scanning) return
        job?.cancel()
        job = viewModelScope.launch {
            _ui.value = _ui.value.copy(scanning = true)
            // Show the previous scan's results immediately instead of an empty screen.
            if (!hasLoaded) loadResults()
            val started = SystemClock.elapsedRealtime()
            repo.fullScan().collect { p ->
                _ui.value = _ui.value.copy(progress = p)
            }
            loadResults()
            _ui.value = _ui.value.copy(scanning = false)
            // Stripped from release builds by proguard-rules.pro; used by tools/benchmark.
            val ui = _ui.value
            Log.i(TAG, "scan_done ms=${SystemClock.elapsedRealtime() - started} hashed=${ui.progress?.total} " +
                "shots=${ui.shots.size} groups=${ui.groups.size} blurry=${ui.blurry.size} videos=${ui.videos.size}")
        }
    }

    private var hasLoaded = false

    private suspend fun loadResults() {
        val (shots, bytes) = repo.screenshots()
        val groups = repo.duplicateGroups()
        val blurry = repo.blurry()
        val videos = repo.largeVideos()
        _ui.value = _ui.value.copy(shots = shots, shotsBytes = bytes, groups = groups, blurry = blurry, videos = videos, loaded = true)
        hasLoaded = true
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
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ScanViewModel(repo) as T
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
