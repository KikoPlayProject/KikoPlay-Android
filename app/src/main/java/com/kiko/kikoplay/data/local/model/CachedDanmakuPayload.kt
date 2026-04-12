package com.kiko.kikoplay.data.local.model

import com.kiko.kikoplay.data.remote.model.DanmakuSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
data class CachedDanmakuPayload(
    val format: String,
    val comment: List<JsonArray>,
    val sources: List<DanmakuSource>? = null,
    val launchScripts: List<String>? = null
) {
    companion object {
        const val FORMAT_FULL = "full"
        const val FORMAT_LOCAL = "local"
    }
}
