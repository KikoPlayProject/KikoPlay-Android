package com.kiko.kikoplay.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistNode(
    val animeName: String? = null,
    val color: String? = null,
    val danmuPool: String? = null,
    val mediaId: String? = null,
    val playTime: Double? = null,
    val playTimeState: Int? = null,
    val text: String,
    val marker: Int? = null,
    val nodes: List<PlaylistNode>? = null
) {
    val isFolder: Boolean get() = nodes != null
}
