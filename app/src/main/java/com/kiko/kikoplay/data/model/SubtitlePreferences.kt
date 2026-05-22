package com.kiko.kikoplay.data.model

enum class SubtitleStylePreset(val storageValue: String) {
    SYSTEM("system"),
    BLACK_BACKGROUND("black_background"),
    TRANSLUCENT_BLACK_BACKGROUND("translucent_black_background"),
    OUTLINE("outline"),
    SHADOW("shadow"),
    YELLOW_OUTLINE("yellow_outline"),
    YELLOW_SHADOW("yellow_shadow");

    companion object {
        fun fromStorage(value: String?): SubtitleStylePreset {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}

enum class SubtitleTextSizePreset(val storageValue: String) {
    SYSTEM("system"),
    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large"),
    EXTRA_LARGE("extra_large");

    companion object {
        fun fromStorage(value: String?): SubtitleTextSizePreset {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}
