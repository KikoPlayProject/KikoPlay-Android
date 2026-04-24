package com.kiko.kikoplay.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kiko.kikoplay.data.model.PlayerPreferences
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
        val KEY_PLAYER_DANMAKU_VISIBLE = booleanPreferencesKey("player_danmaku_visible")
        val KEY_PLAYER_DANMAKU_ALPHA = floatPreferencesKey("player_danmaku_alpha")
        val KEY_PLAYER_DANMAKU_FONT_SIZE = floatPreferencesKey("player_danmaku_font_size")
        val KEY_PLAYER_DANMAKU_SPEED = floatPreferencesKey("player_danmaku_speed")
        val KEY_PLAYER_DANMAKU_DISPLAY_AREA = floatPreferencesKey("player_danmaku_display_area")
        val KEY_PLAYER_SHOW_SCROLL_DANMAKU = booleanPreferencesKey("player_show_scroll_danmaku")
        val KEY_PLAYER_SHOW_TOP_DANMAKU = booleanPreferencesKey("player_show_top_danmaku")
        val KEY_PLAYER_SHOW_BOTTOM_DANMAKU = booleanPreferencesKey("player_show_bottom_danmaku")
        val KEY_PLAYER_PLAYBACK_SPEED = floatPreferencesKey("player_playback_speed")
    }

    val cachePath: Flow<String> = dataStore.data.map { it[KEY_CACHE_PATH] ?: "" }
    val maxCacheSize: Flow<Long> = dataStore.data.map { it[KEY_MAX_CACHE_SIZE] ?: 0L }
    val autoClearCache: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_CLEAR_CACHE] ?: false }
    val syncPlayProgress: Flow<Boolean> = dataStore.data.map { it[KEY_SYNC_PLAY_PROGRESS] ?: true }
    val playerPreferences: Flow<PlayerPreferences> = dataStore.data.map { preferences ->
        PlayerPreferences(
            isDanmakuVisible = preferences[KEY_PLAYER_DANMAKU_VISIBLE] ?: true,
            danmakuAlpha = preferences[KEY_PLAYER_DANMAKU_ALPHA] ?: 1f,
            danmakuFontSize = preferences[KEY_PLAYER_DANMAKU_FONT_SIZE] ?: 1f,
            danmakuSpeed = preferences[KEY_PLAYER_DANMAKU_SPEED] ?: 1f,
            danmakuDisplayArea = preferences[KEY_PLAYER_DANMAKU_DISPLAY_AREA] ?: 1f,
            showScrollDanmaku = preferences[KEY_PLAYER_SHOW_SCROLL_DANMAKU] ?: true,
            showTopDanmaku = preferences[KEY_PLAYER_SHOW_TOP_DANMAKU] ?: true,
            showBottomDanmaku = preferences[KEY_PLAYER_SHOW_BOTTOM_DANMAKU] ?: true,
            playbackSpeed = preferences[KEY_PLAYER_PLAYBACK_SPEED] ?: 1f
        )
    }

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

    suspend fun setPlayerPreferences(preferences: PlayerPreferences) {
        dataStore.edit {
            it[KEY_PLAYER_DANMAKU_VISIBLE] = preferences.isDanmakuVisible
            it[KEY_PLAYER_DANMAKU_ALPHA] = preferences.danmakuAlpha
            it[KEY_PLAYER_DANMAKU_FONT_SIZE] = preferences.danmakuFontSize
            it[KEY_PLAYER_DANMAKU_SPEED] = preferences.danmakuSpeed
            it[KEY_PLAYER_DANMAKU_DISPLAY_AREA] = preferences.danmakuDisplayArea
            it[KEY_PLAYER_SHOW_SCROLL_DANMAKU] = preferences.showScrollDanmaku
            it[KEY_PLAYER_SHOW_TOP_DANMAKU] = preferences.showTopDanmaku
            it[KEY_PLAYER_SHOW_BOTTOM_DANMAKU] = preferences.showBottomDanmaku
            it[KEY_PLAYER_PLAYBACK_SPEED] = preferences.playbackSpeed
        }
    }
}
