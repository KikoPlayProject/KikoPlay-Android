package com.kiko.kikoplay.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kiko.kikoplay.data.remote.StatsApi
import com.kiko.kikoplay.data.remote.model.StatsEventRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class StatsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val statsApi: StatsApi
) {
    companion object {
        private const val EVENT_ANDROID_UV = "android_uv"
        private const val DEVICE_ID_FILE_NAME = "stats_device_id"
        private val KEY_LAST_SUCCESS_LOCAL_DATE = stringPreferencesKey("stats_last_success_local_date")
    }

    private val reportMutex = Mutex()

    suspend fun reportDailyStartup(startupTimeMs: Long) {
        try {
            withContext(Dispatchers.IO) {
                reportMutex.withLock {
                    reportDailyStartupLocked(startupTimeMs)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private suspend fun reportDailyStartupLocked(startupTimeMs: Long) {
        val localDate = currentLocalDate()
        val preferences = dataStore.data.first()
        if (preferences[KEY_LAST_SUCCESS_LOCAL_DATE] == localDate) return

        val deviceId = getOrCreateDeviceId()
        val version = resolveAppVersion()
        val request = StatsEventRequest(
            ts = System.currentTimeMillis() / 1000,
            ev = EVENT_ANDROID_UV,
            os = "Android ${Build.VERSION.RELEASE}",
            kv = version.name,
            st = startupTimeMs,
            did = deviceId,
            app = context.packageName,
            vc = version.code,
            sdk = Build.VERSION.SDK_INT,
            model = Build.MODEL.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            tz = TimeZone.getDefault().id,
            ld = localDate,
            lang = Locale.getDefault().toLanguageTag()
        )

        val response = runCatching { statsApi.reportStartup(request) }.getOrNull()
        val isSuccessful = response?.isSuccessful == true
        response?.body()?.close()
        response?.errorBody()?.close()
        if (isSuccessful) {
            dataStore.edit { it[KEY_LAST_SUCCESS_LOCAL_DATE] = localDate }
        }
    }

    private fun getOrCreateDeviceId(): String {
        val file = File(context.noBackupFilesDir, DEVICE_ID_FILE_NAME)
        val existingDeviceId = runCatching { file.readText().trim() }.getOrNull()
        if (!existingDeviceId.isNullOrBlank()) return existingDeviceId

        val deviceId = UUID.randomUUID().toString()
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(deviceId)
        }
        return deviceId
    }

    private fun resolveAppVersion(): AppVersion {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            AppVersion(
                name = packageInfo.versionName.orEmpty(),
                code = PackageInfoCompat.getLongVersionCode(packageInfo)
            )
        } catch (_: PackageManager.NameNotFoundException) {
            AppVersion(name = "", code = 0)
        }
    }

    private fun currentLocalDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private data class AppVersion(
        val name: String,
        val code: Long
    )
}
