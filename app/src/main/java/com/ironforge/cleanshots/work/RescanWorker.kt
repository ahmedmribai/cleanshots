package com.ironforge.cleanshots.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ironforge.cleanshots.CleanShotsApp
import java.util.concurrent.TimeUnit

class RescanWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as CleanShotsApp
            app.container.scanRepo.fullScan().collect { }
            val (shots, shotBytes) = app.container.scanRepo.screenshots()
            val dupes = app.container.scanRepo.duplicateGroups().flatMap { it.dupes }
            ScanNotifier.notifyReclaimable(applicationContext, shots.size + dupes.size, shotBytes + dupes.sumOf { it.size })
            Result.success()
        } catch (_: SecurityException) {
            // Media permission was revoked; retrying cannot help until the user grants it again.
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "weekly-rescan"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RescanWorker>(7, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
