package com.kiko.kikoplay

import android.app.Application
import android.os.SystemClock
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KikoPlayApplication : Application(), Configuration.Provider {
    companion object {
        private var processStartElapsedMs: Long = 0L

        fun elapsedSinceProcessStartMs(): Long {
            return SystemClock.elapsedRealtime() - processStartElapsedMs
        }
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        processStartElapsedMs = SystemClock.elapsedRealtime()
        super.onCreate()
        WorkManager.initialize(this, workManagerConfiguration)
    }
}
