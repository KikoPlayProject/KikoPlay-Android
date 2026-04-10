package com.kiko.kikoplay.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object LocalVideosRoute

@Serializable
object CacheManagementRoute

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
    val localPath: String? = null
)

@Serializable
object WatchHistoryRoute
