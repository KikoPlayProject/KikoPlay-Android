package com.kiko.kikoplay.data.remote.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
data class DanmakuV3Response(
    val code: Int,
    val data: List<JsonArray>,
    val update: Boolean
)

@Serializable
data class DanmakuFullResponse(
    val source: List<DanmakuSource>? = null,
    val comment: List<JsonArray>,
    val launchScripts: List<String>? = null,
    val update: Boolean
)

@Serializable
data class DanmakuSource(
    val delay: Long = 0,
    val duration: Double? = null,
    val id: Int,
    val name: String,
    val scriptData: String? = null,
    val scriptId: String? = null,
    val scriptName: String? = null,
    val timeline: String? = null,
    val clip: String? = null
)

@Serializable
data class LocalDanmakuResponse(
    val comment: List<JsonArray>,
    val local: String? = null
)
