package com.ironforge.cleanshots.data.scan

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val width: Int,
    val height: Int,
    val bucket: String,
    val relativePath: String
)

object MediaStoreScanner {
    private val PROJECTION = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_MODIFIED,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.RELATIVE_PATH
    )

    suspend fun queryPage(
        resolver: ContentResolver,
        offset: Int,
        limit: Int,
        signal: CancellationSignal
    ): List<MediaItem> {
        val out = mutableListOf<MediaItem>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        run {
            val args = Bundle().apply {
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.Images.Media._ID)
                )
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
            }
            resolver.query(uri, PROJECTION, args, signal)?.use { c ->
                val idC = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameC = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeC = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val addC = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val modC = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val wC = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val hC = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val bC = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val rpC = try { c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH) } catch (_: Exception) { -1 }
                while (c.moveToNext()) {
                    val id = c.getLong(idC)
                    out.add(
                        MediaItem(
                            id = id,
                            uri = ContentUris.withAppendedId(uri, id),
                            displayName = c.getString(nameC) ?: "",
                            size = try { c.getLong(sizeC) } catch (_: Exception) { 0L },
                            dateAdded = try { c.getLong(addC) } catch (_: Exception) { 0L },
                            dateModified = try { c.getLong(modC) } catch (_: Exception) { 0L },
                            width = try { c.getInt(wC) } catch (_: Exception) { 0 },
                            height = try { c.getInt(hC) } catch (_: Exception) { 0 },
                            bucket = try { c.getString(bC) ?: "" } catch (_: Exception) { "" },
                            relativePath = if (rpC >= 0) { try { c.getString(rpC) ?: "" } catch (_: Exception) { "" } } else ""
                        )
                    )
                }
            }
        }
        return out
    }

    private val SCREENSHOT_HINTS = listOf(
        "screenshot", "screen_shot", "截屏", "bildschirmfoto", "captura de pantalla", "capture d'écran"
    )

    fun isScreenshot(relativePath: String, bucket: String, displayName: String): Boolean {
        val rp = relativePath.lowercase()
        val b = bucket.lowercase()
        val name = displayName.lowercase()
        return SCREENSHOT_HINTS.any { rp.contains(it) || b.contains(it) || name.contains(it) }
    }
}
