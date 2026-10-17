package com.ironforge.cleanshots.data.scan

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import com.ironforge.cleanshots.data.Cleanable

data class VideoItem(
    override val mediaId: Long,
    override val contentUri: String,
    override val displayName: String,
    override val size: Long,
    val durationMs: Long,
    val dateAdded: Long
) : Cleanable

object VideoScanner {
    const val LARGE_VIDEO_BYTES = 25L * 1024 * 1024
    private const val MAX_RESULTS = 300

    private val PROJECTION = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.DATE_ADDED
    )

    /** Largest videos first. Returns empty when video access was not granted. */
    fun largeVideos(resolver: ContentResolver, minBytes: Long = LARGE_VIDEO_BYTES): List<VideoItem> {
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val args = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Video.Media.SIZE} >= ?")
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(minBytes.toString()))
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Video.Media.SIZE))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            putInt(ContentResolver.QUERY_ARG_LIMIT, MAX_RESULTS)
        }
        return try {
            resolver.query(uri, PROJECTION, args, null)?.use { c ->
                val idC = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameC = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeC = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durC = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val addC = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                buildList {
                    while (c.moveToNext()) {
                        val id = c.getLong(idC)
                        add(
                            VideoItem(
                                mediaId = id,
                                contentUri = ContentUris.withAppendedId(uri, id).toString(),
                                displayName = c.getString(nameC) ?: "",
                                size = c.getLong(sizeC),
                                durationMs = c.getLong(durC),
                                dateAdded = c.getLong(addC)
                            )
                        )
                    }
                }
            } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    fun formatDuration(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(java.util.Locale.ROOT, h, m, s) else "%d:%02d".format(java.util.Locale.ROOT, m, s)
    }
}
