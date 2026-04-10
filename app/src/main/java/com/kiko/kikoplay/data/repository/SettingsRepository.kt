package com.kiko.kikoplay.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_CACHE_PATH = stringPreferencesKey("cache_path")
        val KEY_MAX_CACHE_SIZE = longPreferencesKey("max_cache_size") // bytes, 0 = unlimited
        val KEY_AUTO_CLEAR_CACHE = booleanPreferencesKey("auto_clear_cache")
        val KEY_SYNC_PLAY_PROGRESS = booleanPreferencesKey("sync_play_progress")
    }

    val cachePath: Flow<String> = dataStore.data.map { it[KEY_CACHE_PATH] ?: "" }
    val maxCacheSize: Flow<Long> = dataStore.data.map { it[KEY_MAX_CACHE_SIZE] ?: 0L }
    val autoClearCache: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_CLEAR_CACHE] ?: false }
    val syncPlayProgress: Flow<Boolean> = dataStore.data.map { it[KEY_SYNC_PLAY_PROGRESS] ?: true }

    suspend fun setCachePath(path: String) {
        dataStore.edit { it[KEY_CACHE_PATH] = path }
    }

    suspend fun setMaxCacheSize(bytes: Long) {
        dataStore.edit { it[KEY_MAX_CACHE_SIZE] = bytes }
    }

    suspend fun setAutoClearCache(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_CLEAR_CACHE] = enabled }
    }

    suspend fun setSyncPlayProgress(enabled: Boolean) {
        dataStore.edit { it[KEY_SYNC_PLAY_PROGRESS] = enabled }
    }
}
