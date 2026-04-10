package com.kiko.kikoplay.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayStateResponse(
    val cur_item: ItemInfo? = null,
    val player: PlayerState? = null
)

@Serializable
data class ItemInfo(
    val position: String? = null,
    val src_type: Int? = null,
    val state: Int? = null,
    val bgm_collection: Boolean? = null,
    val add_time: Long? = null,
    val title: String? = null,
    val anime_title: String? = null,
    val path: String? = null,
    val pool: String? = null
)

@Serializable
data class PlayerState(
    val cur_file: String? = null,
    val state: Int? = null,
    val duration: Double? = null,
    val playtime: Double? = null
)
