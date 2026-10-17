package com.ironforge.cleanshots.ui.trash

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

const val TRASH_UNSUPPORTED_MESSAGE = "Moving photos to Trash needs Android 11 or newer"

/**
 * Launches MediaStore consent dialogs and reports whether the user approved.
 * Callers must only change app state inside [onResult] when it receives true.
 */
@SuppressLint("NewApi") // every MediaStore request below is behind the isSupported check in request()
class MediaRequester internal constructor(
    private val resolver: ContentResolver,
    private val launch: (IntentSender, (Boolean) -> Unit) -> Unit
) {
    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun trash(uris: List<Uri>, onResult: (Boolean) -> Unit) = request(onResult) {
        MediaStore.createTrashRequest(resolver, uris, true).intentSender
    }

    fun untrash(uris: List<Uri>, onResult: (Boolean) -> Unit) = request(onResult) {
        MediaStore.createTrashRequest(resolver, uris, false).intentSender
    }

    fun delete(uris: List<Uri>, onResult: (Boolean) -> Unit) = request(onResult) {
        MediaStore.createDeleteRequest(resolver, uris).intentSender
    }

    private fun request(onResult: (Boolean) -> Unit, sender: () -> IntentSender) {
        if (!isSupported) { onResult(false); return }
        try {
            launch(sender(), onResult)
        } catch (e: Exception) {
            onResult(false)
        }
    }
}

@Composable
fun rememberMediaRequester(): MediaRequester {
    val resolver = LocalContext.current.contentResolver
    val callbacks = remember { ArrayDeque<(Boolean) -> Unit>() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        callbacks.removeFirstOrNull()?.invoke(result.resultCode == Activity.RESULT_OK)
    }
    return remember(resolver, launcher) {
        MediaRequester(resolver) { sender, cb ->
            callbacks.addLast(cb)
            launcher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }
}
