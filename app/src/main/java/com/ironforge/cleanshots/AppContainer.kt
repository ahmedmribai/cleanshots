package com.ironforge.cleanshots

import android.content.Context
import androidx.room.Room
import com.ironforge.cleanshots.billing.BillingManager
import com.ironforge.cleanshots.data.db.AppDatabase
import com.ironforge.cleanshots.data.scan.ScanRepository
import com.ironforge.cleanshots.data.store.PrefsManager

class AppContainer(context: Context) {
    private val appCtx = context.applicationContext

    val db: AppDatabase by lazy {
        Room.databaseBuilder(appCtx, AppDatabase::class.java, "cleanshots.db")
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
    }

    val prefs = PrefsManager(appCtx)
    val scanRepo by lazy { ScanRepository(appCtx, db.scanDao()) }
    val billing = BillingManager(appCtx, prefs)
}
