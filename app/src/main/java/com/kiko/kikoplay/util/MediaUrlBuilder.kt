package com.kiko.kikoplay.util

import com.kiko.kikoplay.data.remote.ConnectionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaUrlBuilder @Inject constructor(
    private val connectionManager: ConnectionManager
) {
    fun buildMediaUrl(mediaId: String): String {
        val conn = connectionManager.connection.value ?: return ""
        return "http://${conn.host}:${conn.port}/media/$mediaId"
    }

    fun buildSubtitleUrl(format: String, mediaId: String): String {
        val conn = connectionManager.connection.value ?: return ""
        return "http://${conn.host}:${conn.port}/sub/$format/$mediaId"
    }
}
