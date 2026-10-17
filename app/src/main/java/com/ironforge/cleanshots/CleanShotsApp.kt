package com.ironforge.cleanshots

import android.app.Application
import androidx.work.Configuration
import com.ironforge.cleanshots.work.RescanWorker

class CleanShotsApp : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        RescanWorker.schedule(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.INFO).build()
}
