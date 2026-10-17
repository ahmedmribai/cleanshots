package com.ironforge.cleanshots.ui.trash

import androidx.compose.ui.platform.LocalResources
import android.content.res.Resources
import com.ironforge.cleanshots.R
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.net.toUri
import com.ironforge.cleanshots.data.Cleanable
import com.ironforge.cleanshots.data.store.PrefsManager
import com.ironforge.cleanshots.ui.scan.ScanViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The single consent-first path for moving photos to Trash: checks the free limit, asks Android
 * for approval, and only then updates the app's records, celebrates and offers undo.
 */
class Cleaner internal constructor(
    private val requester: MediaRequester,
    private val trashVm: TrashViewModel,
    private val scanVm: ScanViewModel,
    private val snack: SnackbarHostState,
    private val scope: CoroutineScope,
    private val resources: Resources,
    private val isPremium: State<Boolean>,
    private val freeUsed: State<Int>,
    private val onNeedPremium: State<() -> Unit>,
    private val onCleaned: State<(Long) -> Unit>
) {
    val freeLeft: Int? get() = if (isPremium.value) null else (PrefsManager.FREE_LIMIT - freeUsed.value).coerceAtLeast(0)

    fun clean(images: List<Cleanable>, onApproved: () -> Unit = {}) {
        if (images.isEmpty()) return
        if (PrefsManager.exceedsFreeLimit(isPremium.value, freeUsed.value, images.size)) {
            onNeedPremium.value()
            return
        }
        if (!requester.isSupported) {
            scope.launch { snack.showSnackbar(resources.getString(R.string.trash_unsupported)) }
            return
        }
        val uris = images.map { it.contentUri.toUri() }
        val meta = images.associate { it.contentUri to (it.displayName to it.size) }
        requester.trash(uris) { approved ->
            if (!approved) return@trash
            trashVm.recordTrashed(uris, meta)
            scanVm.removeTrashed(images.mapTo(HashSet()) { it.mediaId })
            onApproved()
            onCleaned.value(images.sumOf { it.size })
            scope.launch {
                val label = resources.getQuantityString(R.plurals.snackbar_moved, images.size, images.size)
                val undo = resources.getString(R.string.snackbar_undo)
                val result = snack.showSnackbar(label, actionLabel = undo, duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed) {
                    requester.untrash(uris) { restored ->
                        if (restored) { trashVm.onUntrashed(uris); scanVm.start() }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberCleaner(
    requester: MediaRequester,
    trashVm: TrashViewModel,
    scanVm: ScanViewModel,
    snack: SnackbarHostState,
    isPremium: Boolean,
    freeUsed: Int,
    onNeedPremium: () -> Unit,
    onCleaned: (Long) -> Unit
): Cleaner {
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val premiumState = rememberUpdatedState(isPremium)
    val usedState = rememberUpdatedState(freeUsed)
    val needPremiumState = rememberUpdatedState(onNeedPremium)
    val cleanedState = rememberUpdatedState(onCleaned)
    return remember(requester, trashVm, scanVm, snack, resources) {
        Cleaner(requester, trashVm, scanVm, snack, scope, resources, premiumState, usedState, needPremiumState, cleanedState)
    }
}
