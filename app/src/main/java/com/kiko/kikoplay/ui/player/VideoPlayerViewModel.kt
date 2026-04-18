package com.kiko.kikoplay.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kiko.kikoplay.data.local.model.CachedDanmakuPayload
import com.kiko.kikoplay.data.remote.ConnectionManager
import com.kiko.kikoplay.data.remote.KikoPlayApi
import com.kiko.kikoplay.data.remote.model.DanmakuSource
import com.kiko.kikoplay.data.remote.model.UpdateDelayRequest
import com.kiko.kikoplay.data.remote.model.UpdateTimeRequest
import com.kiko.kikoplay.data.remote.model.UpdateTimelineRequest
import com.kiko.kikoplay.data.remote.model.ScreenshotRequest
import com.kiko.kikoplay.data.remote.model.LaunchDanmakuRequest
import com.kiko.kikoplay.data.repository.CacheRepository
import com.kiko.kikoplay.data.repository.PlaylistRepository
import com.kiko.kikoplay.data.repository.WatchHistoryRepository
import com.kiko.kikoplay.ui.navigation.VideoPlayerRoute
import com.kiko.kikoplay.ui.player.danmaku.DanmakuItem
import com.kiko.kikoplay.ui.player.danmaku.DanmakuParser
import com.kiko.kikoplay.util.CacheFileHelper
import com.kiko.kikoplay.util.MediaUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

data class EpisodeUiItem(
    val mediaId: String,
    val title: String,
    val danmuPool: String?,
    val animeTitle: String?,
    val playTimeState: Int?,
    val playTimeSeconds: Double?,
    val startPositionMs: Long
)

data class PlayerUiState(
    val mediaId: String = "",
    val title: String = "",
    val animeTitle: String? = null,
    val danmuPool: String? = null,
    val sourceType: Int = 0,
    val localPath: String? = null,
    val mediaUrl: String = "",
    val subtitleUrl: String? = null,
    val subtitleFormat: String? = null,
    val parentPath: List<Int> = emptyList(),
    val startPositionMs: Long = 0L,
    val initialPlayTimeState: Int = 0,
    val danmakuItems: List<DanmakuItem> = emptyList(),
    val danmakuSources: List<DanmakuSource> = emptyList(),
    val launchScripts: List<String> = emptyList(),
    val episodes: List<EpisodeUiItem> = emptyList(),
    val isDanmakuLoading: Boolean = false,
    val isDanmakuVisible: Boolean = true,
    val isFullscreen: Boolean = false,
    val controlsVisible: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: KikoPlayApi,
    private val mediaUrlBuilder: MediaUrlBuilder,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val cacheRepository: CacheRepository,
    private val playlistRepository: PlaylistRepository,
    private val connectionManager: ConnectionManager,
    private val json: Json
) : ViewModel() {
    private val route = savedStateHandle.toRoute<VideoPlayerRoute>()
    val parentPath: List<Int> get() = uiState.value.parentPath

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            mediaId = route.mediaId,
            title = route.title,
            animeTitle = route.animeTitle,
            danmuPool = route.danmuPool,
            sourceType = route.sourceType,
            localPath = route.localPath,
            parentPath = route.parentPath,
            startPositionMs = route.startPositionMs,
            initialPlayTimeState = route.initialPlayTimeState,
            mediaUrl = when (route.sourceType) {
                1 -> route.localPath ?: ""
                2 -> route.localPath ?: ""
                else -> mediaUrlBuilder.buildMediaUrl(route.mediaId)
            }
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        loadEpisodes()
        loadSubtitle()
        loadDanmaku()
    }

    private fun loadEpisodes() {
        if (route.sourceType != 0) return
        viewModelScope.launch {
            val initialPath = uiState.value.parentPath
            var resolvedPath = initialPath

            var nodes = playlistRepository.getNodeAtPath(resolvedPath)
            if (nodes == null || (resolvedPath.isEmpty() && nodes.none { it.mediaId == route.mediaId || it.nodes != null })) {
                val playlistResult = playlistRepository.ensurePlaylistLoaded()
                if (playlistResult.isFailure) return@launch
                resolvedPath = playlistRepository.findParentPathByMediaId(route.mediaId) ?: initialPath
                nodes = playlistRepository.getNodeAtPath(resolvedPath)
            } else if (resolvedPath.isEmpty()) {
                val inferredPath = playlistRepository.findParentPathByMediaId(route.mediaId)
                if (inferredPath != null) {
                    resolvedPath = inferredPath
                    nodes = playlistRepository.getNodeAtPath(resolvedPath)
                }
            }

            val episodes = mapEpisodes(nodes.orEmpty())
            _uiState.update {
                it.copy(
                    parentPath = resolvedPath,
                    episodes = episodes
                )
            }
        }
    }

    private fun mapEpisodes(nodes: List<com.kiko.kikoplay.data.remote.model.PlaylistNode>): List<EpisodeUiItem> {
        return nodes
            .asSequence()
            .filterNot { it.isFolder }
            .mapNotNull { node ->
                val mediaId = node.mediaId ?: return@mapNotNull null
                EpisodeUiItem(
                    mediaId = mediaId,
                    title = node.text,
                    danmuPool = node.danmuPool,
                    animeTitle = node.animeName,
                    playTimeState = node.playTimeState,
                    playTimeSeconds = node.playTime,
                    startPositionMs = normalizeResumePositionMs(
                        playTimeSeconds = node.playTime,
                        playTimeState = node.playTimeState
                    )
                )
            }
            .toList()
    }

    private fun loadSubtitle() {
        if (uiState.value.sourceType != 0) return
        viewModelScope.launch {
            try {
                val result = api.checkSubtitle(route.mediaId)
                if (result.type.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            subtitleFormat = result.type,
                            subtitleUrl = mediaUrlBuilder.buildSubtitleUrl(result.type, route.mediaId)
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadDanmaku() {
        loadCachedDanmaku()?.let { cached ->
            _uiState.update {
                it.copy(
                    danmakuItems = cached.items,
                    danmakuSources = cached.sources,
                    launchScripts = cached.launchScripts
                )
            }
            return
        }

        val pool = route.danmuPool
        if (pool.isNullOrBlank()) {
            if (uiState.value.sourceType == 0) loadLocalDanmaku()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isDanmakuLoading = true) }
            try {
                val response = api.getDanmakuFull(pool)
                val items = DanmakuParser.parseFull(response.comment)
                _uiState.update {
                    it.copy(
                        danmakuItems = items,
                        danmakuSources = response.source ?: emptyList(),
                        launchScripts = response.launchScripts ?: emptyList(),
                        isDanmakuLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDanmakuLoading = false) }
                // Fallback to v3
                try {
                    val v3 = api.getDanmaku(pool)
                    val items = DanmakuParser.parseV3(v3.data)
                    _uiState.update { it.copy(danmakuItems = items) }
                } catch (_: Exception) {}
            }
        }
    }

    private fun loadCachedDanmaku(): CachedDanmakuState? {
        if (route.sourceType != 2) return null

        val mediaPath = route.localPath ?: return null
        val cacheFile = CacheFileHelper.getDanmakuCacheFile(route.mediaId, mediaPath)
        if (!cacheFile.exists()) return null

        return runCatching {
            val payload = json.decodeFromString(
                CachedDanmakuPayload.serializer(),
                cacheFile.readText()
            )
            val items = when (payload.format) {
                CachedDanmakuPayload.FORMAT_FULL -> DanmakuParser.parseFull(payload.comment)
                else -> DanmakuParser.parseV3(payload.comment)
            }
            CachedDanmakuState(
                items = items,
                sources = payload.sources ?: emptyList(),
                launchScripts = payload.launchScripts ?: emptyList()
            )
        }.getOrNull()
    }

    private fun loadLocalDanmaku() {
        viewModelScope.launch {
            try {
                val response = api.getLocalDanmaku(route.mediaId)
                val items = DanmakuParser.parseV3(response.comment)
                _uiState.update { it.copy(danmakuItems = items) }
            } catch (_: Exception) {}
        }
    }

    fun refreshDanmaku() {
        loadDanmaku()
    }

    fun cacheEpisodes(episodes: List<EpisodeUiItem>) {
        if (route.sourceType != 0) return

        val serverAddress = connectionManager.connection.value?.let { "${it.host}:${it.port}" } ?: return
        viewModelScope.launch {
            episodes.forEach { episode ->
                cacheRepository.enqueue(
                    mediaId = episode.mediaId,
                    title = episode.title,
                    animeTitle = episode.animeTitle,
                    danmuPool = episode.danmuPool,
                    serverAddress = serverAddress
                )
            }
        }
    }

    fun toggleDanmaku() {
        _uiState.update { it.copy(isDanmakuVisible = !it.isDanmakuVisible) }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreen = fullscreen) }
    }

    fun setControlsVisible(visible: Boolean) {
        _uiState.update { it.copy(controlsVisible = visible) }
    }

    fun syncPlayTime(positionMs: Long, playTimeState: Int, durationMs: Long = 0) {
        syncPlayTime(
            positionMs = positionMs,
            playTimeState = playTimeState,
            durationMs = durationMs,
            thumbnailData = null
        )
    }

    fun syncPlayTime(
        positionMs: Long,
        playTimeState: Int,
        durationMs: Long = 0,
        thumbnailData: ByteArray? = null
    ) {
        val safePositionMs = positionMs.coerceAtLeast(0L)
        val playTimeSeconds = safePositionMs / 1000.0
        if (route.sourceType == 0) {
            playlistRepository.updateNodeProgress(route.mediaId, playTimeSeconds, playTimeState)
        }
        _uiState.update { state ->
            state.copy(
                episodes = state.episodes.map { episode ->
                    if (episode.mediaId == route.mediaId) {
                        episode.copy(
                            playTimeState = playTimeState,
                            playTimeSeconds = playTimeSeconds,
                            startPositionMs = safePositionMs
                        )
                    } else {
                        episode
                    }
                }
            )
        }
        // Record watch history
        viewModelScope.launch {
            try {
                val activeConnection = connectionManager.connection.value
                val activeServerAddress = activeConnection?.let { "${it.host}:${it.port}" }
                val cacheTask = cacheRepository.getByMediaId(route.mediaId)
                val localPath = route.localPath ?: cacheTask?.localPath
                val serverAddress = when (route.sourceType) {
                    0 -> activeServerAddress
                    2 -> cacheTask?.serverAddress ?: activeServerAddress
                    else -> null
                }
                val isCached = when (route.sourceType) {
                    2 -> true
                    0 -> cacheRepository.getPlayableCache(route.mediaId, serverAddress) != null
                    else -> false
                }
                val remoteUri = when (route.sourceType) {
                    0 -> mediaUrlBuilder.buildMediaUrl(route.mediaId)
                    2 -> serverAddress?.let { "http://$it/media/${route.mediaId}" }
                    else -> null
                }
                watchHistoryRepository.record(
                    mediaId = route.mediaId,
                    title = route.title,
                    animeTitle = route.animeTitle,
                    playTime = safePositionMs,
                    duration = durationMs,
                    playTimeState = playTimeState,
                    sourceType = route.sourceType,
                    isCached = isCached,
                    remoteUri = remoteUri,
                    localPath = localPath,
                    thumbnailData = thumbnailData,
                    danmuPool = route.danmuPool,
                    serverAddress = serverAddress
                )
            } catch (_: Exception) {}
        }
        // Sync to PC
        if (uiState.value.sourceType != 0) return
        viewModelScope.launch {
            try {
                api.updatePlayTime(
                    UpdateTimeRequest(
                        mediaId = route.mediaId,
                        playTime = playTimeSeconds,
                        playTimeState = playTimeState
                    )
                )
            } catch (_: Exception) {}
        }
    }

    fun updateSourceDelay(sourceId: Int, delay: Long) {
        val pool = route.danmuPool ?: return
        viewModelScope.launch {
            try {
                api.updateDelay(UpdateDelayRequest(pool, delay, sourceId))
            } catch (_: Exception) {}
        }
    }

    fun updateSourceTimeline(sourceId: Int, timeline: String) {
        val pool = route.danmuPool ?: return
        viewModelScope.launch {
            try {
                api.updateTimeline(UpdateTimelineRequest(pool, timeline, sourceId))
            } catch (_: Exception) {}
        }
    }

    fun screenshot(pos: Double, duration: Double? = null, info: String = "") {
        viewModelScope.launch {
            try {
                api.screenshot(
                    ScreenshotRequest(
                        animeName = route.animeTitle ?: route.title,
                        mediaId = route.mediaId,
                        pos = pos,
                        duration = duration,
                        info = info
                    )
                )
            } catch (_: Exception) {}
        }
    }

    fun sendDanmaku(text: String, time: Long, color: Int, type: Int) {
        val pool = route.danmuPool ?: return
        viewModelScope.launch {
            try {
                api.launchDanmaku(
                    LaunchDanmakuRequest(
                        danmuPool = pool,
                        text = text,
                        time = time,
                        color = color,
                        fontsize = 0,
                        date = (System.currentTimeMillis() / 1000).toString(),
                        type = type,
                        mediaId = route.mediaId,
                        launchScripts = uiState.value.launchScripts.ifEmpty { null }
                    )
                )
            } catch (_: Exception) {}
        }
    }

    private data class CachedDanmakuState(
        val items: List<DanmakuItem>,
        val sources: List<DanmakuSource>,
        val launchScripts: List<String>
    )

    private fun normalizeResumePositionMs(
        playTimeSeconds: Double?,
        playTimeState: Int?
    ): Long {
        if (playTimeState == 2) return 0L
        return ((playTimeSeconds ?: 0.0) * 1000).toLong()
    }
}
