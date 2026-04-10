package com.kiko.kikoplay.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateTimeRequest(
    val mediaId: String,
    val playTime: Double,
    val playTimeState: Int
)

@Serializable
data class UpdateDelayRequest(
    val danmuPool: String,
    val delay: Long,
    val source: Int
)

@Serializable
data class UpdateTimelineRequest(
    val danmuPool: String,
    val timeline: String,
    val source: Int
)

@Serializable
data class ScreenshotRequest(
    val animeName: String,
    val mediaId: String,
    val pos: Double,
    val duration: Double? = null,
    val info: String
)

@Serializable
data class LaunchDanmakuRequest(
    val danmuPool: String,
    val text: String,
    val time: Long,
    val color: Int,
    val fontsize: Int,
    val date: String,
    val type: Int,
    val mediaId: String? = null,
    val launchScripts: List<String>? = null
)
