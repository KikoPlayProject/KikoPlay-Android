package com.kiko.kikoplay.data.repository

import com.kiko.kikoplay.data.remote.KikoPlayApi
import com.kiko.kikoplay.data.remote.ConnectionManager
import com.kiko.kikoplay.data.remote.model.PlaylistNode
import retrofit2.HttpException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PlaylistProgressUpdate(
    val mediaId: String,
    val playTimeSeconds: Double,
    val playTimeState: Int
)

@Singleton
class PlaylistRepository @Inject constructor(
    private val api: KikoPlayApi,
    private val connectionManager: ConnectionManager
) {
    private var cachedPlaylist: List<PlaylistNode>? = null
    private var cachedRecent: List<PlaylistNode>? = null
    private val _progressUpdates = MutableSharedFlow<PlaylistProgressUpdate>(extraBufferCapacity = 16)
    val progressUpdates: SharedFlow<PlaylistProgressUpdate> = _progressUpdates.asSharedFlow()

    suspend fun fetchPlaylist(): Result<List<PlaylistNode>> {
        return try {
            val response = api.getPlaylist()
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
            val playlist = response.body().orEmpty()
            val kikoVersion = response.headers()["X-Kiko"]?.toIntOrNull()
            connectionManager.updateKikoVersion(kikoVersion)
            cachedPlaylist = playlist
            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRecent(): Result<List<PlaylistNode>> {
        return try {
            val recent = api.getRecent()
            cachedRecent = recent
            Result.success(recent)
        } catch (e: Exception) {
            cachedRecent = null
            Result.failure(e)
        }
    }

    suspend fun ensurePlaylistLoaded(): Result<List<PlaylistNode>> {
        return cachedPlaylist?.let { Result.success(it) } ?: fetchPlaylist()
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

    fun findParentPathByMediaId(mediaId: String): List<Int>? {
        val playlist = cachedPlaylist ?: return null
        return findParentPathRecursive(
            nodes = playlist,
            mediaId = mediaId,
            parentPath = emptyList()
        )
    }

    fun updateNodeProgress(
        mediaId: String,
        playTimeSeconds: Double,
        playTimeState: Int
    ) {
        val (updatedPlaylist, playlistChanged) = cachedPlaylist
            ?.let { playlist ->
                updateNodeProgressRecursive(
                    nodes = playlist,
                    mediaId = mediaId,
                    playTimeSeconds = playTimeSeconds,
                    playTimeState = playTimeState
                )
            }
            ?: (null to false)
        val updatedRecent = cachedRecent?.map { node ->
            if (node.mediaId == mediaId && !node.isFolder) {
                node.copy(playTime = playTimeSeconds, playTimeState = playTimeState)
            } else {
                node
            }
        }
        val recentChanged = updatedRecent != cachedRecent
        if (!playlistChanged && !recentChanged) return

        if (playlistChanged) {
            cachedPlaylist = updatedPlaylist
        }
        if (recentChanged) {
            cachedRecent = updatedRecent
        }
        _progressUpdates.tryEmit(
            PlaylistProgressUpdate(
                mediaId = mediaId,
                playTimeSeconds = playTimeSeconds,
                playTimeState = playTimeState
            )
        )
    }

    fun clearCache() {
        cachedPlaylist = null
        cachedRecent = null
    }

    private fun updateNodeProgressRecursive(
        nodes: List<PlaylistNode>,
        mediaId: String,
        playTimeSeconds: Double,
        playTimeState: Int
    ): Pair<List<PlaylistNode>, Boolean> {
        var changed = false
        val updatedNodes = nodes.map { node ->
            when {
                node.mediaId == mediaId && !node.isFolder -> {
                    changed = true
                    node.copy(playTime = playTimeSeconds, playTimeState = playTimeState)
                }
                node.nodes != null -> {
                    val (updatedChildren, childChanged) = updateNodeProgressRecursive(
                        nodes = node.nodes,
                        mediaId = mediaId,
                        playTimeSeconds = playTimeSeconds,
                        playTimeState = playTimeState
                    )
                    if (childChanged) {
                        changed = true
                        node.copy(nodes = updatedChildren)
                    } else {
                        node
                    }
                }
                else -> node
            }
        }
        return updatedNodes to changed
    }

    private fun findParentPathRecursive(
        nodes: List<PlaylistNode>,
        mediaId: String,
        parentPath: List<Int>
    ): List<Int>? {
        nodes.forEachIndexed { index, node ->
            if (node.mediaId == mediaId && !node.isFolder) {
                return parentPath
            }
            val childPath = node.nodes?.let { children ->
                findParentPathRecursive(
                    nodes = children,
                    mediaId = mediaId,
                    parentPath = parentPath + index
                )
            }
            if (childPath != null) {
                return childPath
            }
        }
        return null
    }
}
