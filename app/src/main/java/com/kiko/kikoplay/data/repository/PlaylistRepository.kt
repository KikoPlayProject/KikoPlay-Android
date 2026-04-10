package com.kiko.kikoplay.data.repository

import com.kiko.kikoplay.data.remote.KikoPlayApi
import com.kiko.kikoplay.data.remote.model.PlaylistNode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val api: KikoPlayApi
) {
    private var cachedPlaylist: List<PlaylistNode>? = null

    suspend fun fetchPlaylist(): Result<List<PlaylistNode>> {
        return try {
            val playlist = api.getPlaylist()
            cachedPlaylist = playlist
            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCachedPlaylist(): List<PlaylistNode>? = cachedPlaylist

    fun getNodeAtPath(path: List<Int>): List<PlaylistNode>? {
        var current = cachedPlaylist ?: return null
        for (index in path) {
            val node = current.getOrNull(index) ?: return null
            current = node.nodes ?: return null
        }
        return current
    }

    fun getNodeByPath(path: List<Int>): PlaylistNode? {
        var current = cachedPlaylist ?: return null
        var node: PlaylistNode? = null
        for (index in path) {
            node = current.getOrNull(index) ?: return null
            current = node.nodes ?: emptyList()
        }
        return node
    }

    fun clearCache() {
        cachedPlaylist = null
    }
}
