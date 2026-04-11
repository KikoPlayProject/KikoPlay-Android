package com.kiko.kikoplay.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kiko.kikoplay.data.remote.ConnectionManager
import com.kiko.kikoplay.data.remote.KikoPlayApi
import com.kiko.kikoplay.data.remote.model.DanmakuSource
import com.kiko.kikoplay.data.remote.model.UpdateDelayRequest
import com.kiko.kikoplay.data.remote.model.UpdateTimeRequest
import com.kiko.kikoplay.data.remote.model.UpdateTimelineRequest
import com.kiko.kikoplay.data.remote.model.ScreenshotRequest
import com.kiko.kikoplay.data.remote.model.LaunchDanmakuRequest
import com.kiko.kikoplay.data.repository.CacheRepository
import com.kiko.kikoplay.data.repository.WatchHistoryRepository
import com.kiko.kikoplay.ui.navigation.VideoPlayerRoute
import com.kiko.kikoplay.ui.player.danmaku.DanmakuItem
import com.kiko.kikoplay.ui.player.danmaku.DanmakuParser
import com.kiko.kikoplay.util.MediaUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val danmakuItems: List<DanmakuItem> = emptyList(),
    val danmakuSources: List<DanmakuSource> = emptyList(),
    val launchScripts: List<String> = emptyList(),
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
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val route = savedStateHandle.toRoute<VideoPlayerRoute>()

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            mediaId = route.mediaId,
            title = route.title,
            animeTitle = route.animeTitle,
            danmuPool = route.danmuPool,
            sourceType = route.sourceType,
            localPath = route.localPath,
            mediaUrl = when (route.sourceType) {
                1 -> route.localPath ?: ""
                2 -> route.localPath ?: ""
                else -> mediaUrlBuilder.buildMediaUrl(route.mediaId)
            }
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        loadSubtitle()
        loadDanmaku()
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

    fun toggleDanmaku() {
        _uiState.update { it.copy(isDanmakuVisible = !it.isDanmakuVisible) }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreen = fullscreen) }
    }

    fun setControlsVisible(visible: Boolean) {
        _uiState.update { it.copy(controlsVisible = visible) }
    }

    fun syncPlayTime(playTime: Double, playTimeState: Int, durationMs: Long = 0) {
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
                    playTime = (playTime * 1000).toLong(),
                    duration = durationMs,
                    playTimeState = playTimeState,
                    sourceType = route.sourceType,
                    isCached = isCached,
                    remoteUri = remoteUri,
                    localPath = localPath,
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
                        playTime = playTime,
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
}
