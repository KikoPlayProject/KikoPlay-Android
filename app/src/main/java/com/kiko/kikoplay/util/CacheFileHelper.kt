package com.kiko.kikoplay.util

import java.io.File

object CacheFileHelper {

    fun getDanmakuCacheFile(
        mediaId: String,
        mediaPath: String
    ): File {
        val mediaFile = File(mediaPath)
        val parent = mediaFile.parentFile ?: mediaFile
        return File(parent, "$mediaId.danmaku.json")
    }
}
