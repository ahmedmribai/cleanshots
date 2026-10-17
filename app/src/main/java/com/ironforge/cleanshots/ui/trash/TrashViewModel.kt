package com.ironforge.cleanshots.ui.trash

import android.content.ContentUris
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ironforge.cleanshots.data.db.TrashDao
import com.ironforge.cleanshots.data.db.TrashEntry
import com.ironforge.cleanshots.data.store.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TRASH_RETENTION_MS = 30L * 24 * 60 * 60 * 1000

data class TrashUi(val items: List<TrashEntry> = emptyList(), val lastTrashed: List<Uri> = emptyList())

class TrashViewModel(
    private val trashDao: TrashDao,
    private val prefs: PrefsManager
) : ViewModel() {
    private val _ui = MutableStateFlow(TrashUi())
    val ui: StateFlow<TrashUi> = _ui

    fun load() = viewModelScope.launch {
        trashDao.pruneOlderThan(System.currentTimeMillis() - TRASH_RETENTION_MS)
        _ui.value = _ui.value.copy(items = trashDao.getAll())
    }

    fun recordTrashed(uris: List<Uri>, names: Map<String, Pair<String, Long>>) = viewModelScope.launch {
        val entries = uris.mapNotNull { u ->
            val id = mediaIdOf(u) ?: return@mapNotNull null
            val (n, s) = names[u.toString()] ?: ("" to 0L)
            TrashEntry(id, u.toString(), n, s)
        }
        trashDao.upsertAll(entries)
        prefs.incrementDeletes(uris.size)
        _ui.value = _ui.value.copy(lastTrashed = uris)
        load()
    }

    fun lastTrashed(): List<Uri> = _ui.value.lastTrashed

    fun urisFor(ids: List<Long>): List<Uri> =
        _ui.value.items.filter { it.mediaId in ids }.map { it.contentUri.toUri() }

    /** Call only after the user approved the untrash request. */
    fun onUntrashed(uris: List<Uri>) = viewModelScope.launch {
        trashDao.remove(uris.mapNotNull(::mediaIdOf))
        prefs.incrementDeletes(-uris.size)
        _ui.value = _ui.value.copy(lastTrashed = emptyList())
        load()
    }

    /** Call only after the user approved the permanent delete request. */
    fun onPermanentlyDeleted(ids: List<Long>) = viewModelScope.launch {
        trashDao.remove(ids)
        load()
    }

    private fun mediaIdOf(uri: Uri): Long? = try { ContentUris.parseId(uri) } catch (_: Exception) { null }

    class Factory(private val dao: TrashDao, private val prefs: PrefsManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = TrashViewModel(dao, prefs) as T
    }
}
