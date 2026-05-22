package com.kiko.kikoplay.data.model

data class PlayerPreferences(
    val isDanmakuVisible: Boolean = true,
    val danmakuAlpha: Float = 1f,
    val danmakuFontSize: Float = 1f,
    val danmakuSpeed: Float = 1f,
    val danmakuDisplayArea: Float = 1f,
    val showScrollDanmaku: Boolean = true,
    val showTopDanmaku: Boolean = true,
    val showBottomDanmaku: Boolean = true,
    val playbackSpeed: Float = 1f,
    val subtitleStylePreset: SubtitleStylePreset = SubtitleStylePreset.SYSTEM,
    val subtitleTextSizePreset: SubtitleTextSizePreset = SubtitleTextSizePreset.SYSTEM
)
