package com.ironforge.cleanshots.data.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.util.Size
import androidx.core.graphics.scale
import com.ironforge.cleanshots.data.db.ScanDao
import com.ironforge.cleanshots.data.db.ScannedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** [total] is 0 while the size of the work is still unknown (the "Listing" phase). */
data class ScanProgress(val done: Int, val total: Int, val phase: String)
data class DuplicateGroup(val keeper: ScannedImage, val dupes: List<ScannedImage>)

class ScanRepository(private val context: Context, private val dao: ScanDao) {

    fun fullScan(pageSize: Int = PAGE_SIZE): Flow<ScanProgress> = flow {
        emit(ScanProgress(0, 0, PHASE_LISTING))
        val all = listAllImages(pageSize) { emit(ScanProgress(it, 0, PHASE_LISTING)) }
        val stamps = withContext(Dispatchers.IO) { dao.getStamps().associate { it.mediaId to it.dateModified } }
        if (all.isNotEmpty()) pruneMissing(all, stamps.keys)

        val toHash = all.filter { stamps[it.id] != it.dateModified }
        var done = 0
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        toHash.chunked(HASH_CHUNK).forEach { chunk ->
            currentCoroutineContext().ensureActive()
            val batch = coroutineScope {
                chunk.chunked((chunk.size / cores).coerceAtLeast(1)).map { sub ->
                    async(Dispatchers.Default) {
                        sub.mapNotNull { item ->
                            ensureActive()
                            hashOne(item)
                        }
                    }
                }.awaitAll().flatten()
            }
            if (batch.isNotEmpty()) withContext(Dispatchers.IO) { dao.upsertAll(batch) }
            done += chunk.size
            emit(ScanProgress(done, toHash.size, PHASE_ANALYZING))
        }
        emit(ScanProgress(toHash.size, toHash.size, PHASE_DONE))
    }

    private suspend fun listAllImages(pageSize: Int, onPage: suspend (Int) -> Unit): List<MediaItem> {
        val resolver = context.contentResolver
        val signal = CancellationSignal()
        val all = mutableListOf<MediaItem>()
        while (true) {
            val page = withContext(Dispatchers.IO) {
                MediaStoreScanner.queryPage(resolver, all.size, pageSize, signal)
            }
            all.addAll(page)
            onPage(all.size)
            if (page.size < pageSize) break
        }
        return all
    }

    private suspend fun pruneMissing(live: List<MediaItem>, known: Set<Long>) {
        val liveIds = live.mapTo(HashSet()) { it.id }
        val stale = known.filter { it !in liveIds }
        // SQLite caps bound variables per statement, so delete in slices.
        withContext(Dispatchers.IO) { stale.chunked(SQL_CHUNK).forEach { dao.deleteByIds(it) } }
    }

    private fun hashOne(item: MediaItem): ScannedImage? {
        val thumb = loadThumb(item.uri) ?: return null
        return try {
            ScannedImage(
                mediaId = item.id,
                contentUri = item.uri.toString(),
                displayName = item.displayName,
                size = item.size,
                dateAdded = item.dateAdded,
                dateModified = item.dateModified,
                width = item.width,
                height = item.height,
                bucket = item.bucket,
                relativePath = item.relativePath,
                dhash = HashUtils.dHash(thumb),
                blurScore = BlurUtils.laplacianVariance(thumb).toFloat()
            )
        } catch (_: Exception) {
            null
        } finally {
            if (!thumb.isRecycled) thumb.recycle()
        }
    }

    /**
     * Decodes straight from the file with JPEG DCT scaling (much faster than MediaStore's thumbnail
     * service on a first scan), then normalises to [THUMB_PX] so blur scores are comparable across
     * camera resolutions. Falls back to the system thumbnail for formats BitmapFactory can't read.
     */
    private fun loadThumb(uri: Uri): Bitmap? {
        val decoded = try { decodeSampled(uri) } catch (_: Exception) { null }
            ?: systemThumbnail(uri)
            ?: return null
        return normalise(decoded)
    }

    private fun systemThumbnail(uri: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, Size(THUMB_PX, THUMB_PX), null)
        } else null
    } catch (_: Exception) {
        null
    }

    private fun decodeSampled(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / (sample * 2) >= THUMB_PX) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun normalise(src: Bitmap): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= THUMB_PX) return src
        val scale = THUMB_PX.toFloat() / longest
        val out = src.scale((src.width * scale).roundToInt().coerceAtLeast(1), (src.height * scale).roundToInt().coerceAtLeast(1))
        if (out !== src) src.recycle()
        return out
    }

    suspend fun screenshots(): Pair<List<ScannedImage>, Long> = withContext(Dispatchers.Default) {
        val shots = dao.getAllHashed().filter { MediaStoreScanner.isScreenshot(it.relativePath, it.bucket, it.displayName) }
        shots to shots.sumOf { it.size }
    }

    /** Screenshots are excluded: app UIs hash alike, and they have their own category anyway. */
    suspend fun duplicateGroups(threshold: Int = HashUtils.DUPLICATE_DISTANCE): List<DuplicateGroup> = withContext(Dispatchers.Default) {
        val all = dao.getAllHashed().filter {
            it.dhash != null && !MediaStoreScanner.isScreenshot(it.relativePath, it.bucket, it.displayName)
        }
        if (all.size < 2) return@withContext emptyList()
        val groups = HashUtils.groupNearDuplicates(all.map { it.dhash!! }, threshold)
        groups.map { idxs ->
            val imgs = idxs.map { all[it] }
            val keeper = imgs.maxWith(compareBy({ it.width.toLong() * it.height }, { it.size }, { it.blurScore ?: 0f }))
            DuplicateGroup(keeper, imgs.filter { it.mediaId != keeper.mediaId })
        }.sortedByDescending { it.dupes.sumOf { d -> d.size } }
    }

    suspend fun largeVideos(): List<VideoItem> =
        withContext(Dispatchers.IO) { VideoScanner.largeVideos(context.contentResolver) }

    /** Screenshots are flat by design, so they would all look "blurry"; they are excluded. */
    suspend fun blurry(threshold: Double = BlurUtils.DEFAULT_THRESHOLD): List<ScannedImage> =
        withContext(Dispatchers.Default) {
            dao.getAllHashed()
                .filter { (it.blurScore?.toDouble() ?: BlurUtils.UNMEASURABLE) < threshold }
                .filterNot { MediaStoreScanner.isScreenshot(it.relativePath, it.bucket, it.displayName) }
                .sortedBy { it.blurScore }
        }

    companion object {
        const val PHASE_LISTING = "Listing"
        const val PHASE_ANALYZING = "Analyzing"
        const val PHASE_DONE = "Done"
        private const val PAGE_SIZE = 800
        private const val HASH_CHUNK = 100
        private const val SQL_CHUNK = 500
        private const val THUMB_PX = 256
    }
}
