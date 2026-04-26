package com.kiko.kikoplay.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object LocalVideosRoute

@Serializable
data class CacheManagementRoute(
    val initialTab: Int = TAB_ACTIVE
) {
    companion object {
        const val TAB_ACTIVE = 0
        const val TAB_COMPLETED = 1
    }
}

@Serializable
object SettingsRoute

@Serializable
object LanConnectionRoute

@Serializable
data class PlaylistBrowserRoute(val pathIndex: Int = -1)

@Serializable
data class VideoPlayerRoute(
    val mediaId: String,
    val title: String,
    val sourceType: Int = 0, // 0=PC, 1=local, 2=cached
    val danmuPool: String? = null,
    val animeTitle: String? = null,
    val localPath: String? = null,
    val serverAddress: String? = null,
    val parentPath: List<Int> = emptyList(),
    val startPositionMs: Long = 0L,
    val initialPlayTimeState: Int = 0
)

@Serializable
object WatchHistoryRoute
