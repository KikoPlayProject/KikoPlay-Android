package com.kiko.kikoplay.ui.player

import android.content.Context
import android.util.Base64
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import com.kiko.kikoplay.data.model.PlayerPreferences
import com.kiko.kikoplay.data.local.entity.CacheTaskEntity
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
import com.kiko.kikoplay.data.repository.SettingsRepository
import com.kiko.kikoplay.data.repository.WatchHistoryRepository
import com.kiko.kikoplay.ui.navigation.VideoPlayerRoute
import com.kiko.kikoplay.ui.player.danmaku.DanmakuItem
import com.kiko.kikoplay.ui.player.danmaku.DanmakuParser
import com.kiko.kikoplay.ui.player.danmaku.DanmakuSourceSummary
import com.kiko.kikoplay.ui.player.danmaku.FullDanmakuComment
import com.kiko.kikoplay.ui.player.subtitle.REMOTE_SUBTITLE_TRACK_ID
import com.kiko.kikoplay.ui.player.subtitle.RemoteSubtitleNormalizer
import com.kiko.kikoplay.ui.player.subtitle.SubtitleTrackSelector
import com.kiko.kikoplay.util.CacheFileHelper
import com.kiko.kikoplay.util.MediaUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale
import javax.inject.Inject

data class EpisodeUiItem(
    val mediaId: String,
    val title: String,
    val danmuPool: String?,
    val animeTitle: String?,
    val isCached: Boolean,
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
    val subtitleLabel: String? = null,
    val subtitleId: String? = null,
    val parentPath: List<Int> = emptyList(),
    val startPositionMs: Long = 0L,
    val initialPlayTimeState: Int = 0,
    val danmakuItems: List<DanmakuItem> = emptyList(),
    val danmakuSources: List<DanmakuSource> = emptyList(),
    val danmakuSourceSummaries: List<DanmakuSourceSummary> = emptyList(),
    val launchScripts: List<String> = emptyList(),
    val episodes: List<EpisodeUiItem> = emptyList(),
    val isDanmakuLoading: Boolean = false,
    val isDanmakuRefreshing: Boolean = false,
    val isDanmakuVisible: Boolean = true,
    val playerPreferences: PlayerPreferences = PlayerPreferences(),
    val isSyncPlayProgressEnabled: Boolean = true,
    val isFullscreen: Boolean = false,
    val controlsVisible: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val api: KikoPlayApi,
    private val mediaUrlBuilder: MediaUrlBuilder,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val cacheRepository: CacheRepository,
    private val playlistRepository: PlaylistRepository,
    private val connectionManager: ConnectionManager,
    private val settingsRepository: SettingsRepository,
    private val json: Json
) : ViewModel() {
    private companion object {
        const val SOURCE_TYPE_PC = 0
        const val SOURCE_TYPE_LOCAL = 1
        const val SOURCE_TYPE_CACHE = 2
    }

    private val route = savedStateHandle.toRoute<VideoPlayerRoute>()
    val parentPath: List<Int> get() = uiState.value.parentPath
    private var fullDanmakuComments: List<FullDanmakuComment> = emptyList()
    private var episodeNodes: List<com.kiko.kikoplay.data.remote.model.PlaylistNode> = emptyList()
    private var completedCacheTasks: List<CacheTaskEntity> = emptyList()

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
            isFullscreen = route.startFullscreen,
            mediaUrl = when (route.sourceType) {
                1 -> route.localPath ?: ""
                2 -> route.localPath ?: ""
                else -> mediaUrlBuilder.buildMediaUrl(route.mediaId)
            }
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        observeCompletedCacheTasks()
        loadEpisodes()
        loadSubtitle()
        loadDanmaku()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.playerPreferences.collect { preferences ->
                _uiState.update {
                    it.copy(
                        playerPreferences = preferences,
                        isDanmakuVisible = preferences.isDanmakuVisible
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.syncPlayProgress.collect { enabled ->
                _uiState.update { it.copy(isSyncPlayProgressEnabled = enabled) }
            }
        }
    }

    private fun loadEpisodes() {
        if (route.sourceType == SOURCE_TYPE_LOCAL) return
        viewModelScope.launch {
            val initialPath = uiState.value.parentPath
            var resolvedPath = initialPath

            fun resolveEpisodeNodes(path: List<Int>): Pair<List<Int>, List<com.kiko.kikoplay.data.remote.model.PlaylistNode>?> {
                var currentPath = path
                var nodes = playlistRepository.getNodeAtPath(currentPath)
                if (nodes == null || (currentPath.isEmpty() && nodes.none { it.mediaId == route.mediaId || it.nodes != null })) {
                    currentPath = playlistRepository.findParentPathByMediaId(route.mediaId) ?: path
                    nodes = playlistRepository.getNodeAtPath(currentPath)
                } else if (currentPath.isEmpty()) {
                    val inferredPath = playlistRepository.findParentPathByMediaId(route.mediaId)
                    if (inferredPath != null) {
                        currentPath = inferredPath
                        nodes = playlistRepository.getNodeAtPath(currentPath)
                    }
                }
                return currentPath to nodes
            }

            if (route.sourceType == SOURCE_TYPE_PC && playlistRepository.getCachedPlaylist() == null) {
                val playlistResult = playlistRepository.ensurePlaylistLoaded()
                if (playlistResult.isFailure) return@launch
            }

            var resolved = resolveEpisodeNodes(resolvedPath)
            resolvedPath = resolved.first
            var nodes = resolved.second

            if ((nodes == null || nodes.none { it.mediaId == route.mediaId }) && shouldRefreshPlaylistForEpisodeContext()) {
                val playlistResult = playlistRepository.fetchPlaylist()
                if (playlistResult.isSuccess || playlistRepository.getCachedPlaylist() != null) {
                    resolved = resolveEpisodeNodes(initialPath)
                    resolvedPath = resolved.first
                    nodes = resolved.second
                }
            }

            episodeNodes = nodes.orEmpty()
            val episodes = mapEpisodes(episodeNodes)
            _uiState.update {
                it.copy(
                    parentPath = resolvedPath,
                    episodes = episodes
                )
            }
        }
    }

    private fun mapEpisodes(nodes: List<com.kiko.kikoplay.data.remote.model.PlaylistNode>): List<EpisodeUiItem> {
        val cachedMediaIds = resolveCachedEpisodeMediaIds()
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
                    isCached = mediaId in cachedMediaIds,
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

    private fun observeCompletedCacheTasks() {
        viewModelScope.launch {
            cacheRepository.getCompletedTasks().collect { tasks ->
                completedCacheTasks = tasks
                refreshEpisodesFromCacheState()
            }
        }
        viewModelScope.launch {
            connectionManager.connection.collect {
                refreshEpisodesFromCacheState()
            }
        }
    }

    private fun refreshEpisodesFromCacheState() {
        if (episodeNodes.isEmpty()) return
        val updatedEpisodes = mapEpisodes(episodeNodes)
        _uiState.update { state ->
            if (state.episodes == updatedEpisodes) state else state.copy(episodes = updatedEpisodes)
        }
    }

    private fun resolveCachedEpisodeMediaIds(): Set<String> {
        val serverAddress = resolveEpisodeServerAddress() ?: return emptySet()
        return completedCacheTasks.asSequence()
            .filter { task ->
                task.serverAddress == serverAddress &&
                    task.localPath?.let { File(it).exists() } == true
            }
            .map { it.mediaId }
            .toSet()
    }

    private fun loadSubtitle() {
        if (!shouldLoadRemoteSubtitle()) return
        viewModelScope.launch {
            try {
                val result = api.checkSubtitle(route.mediaId)
                val format = result.type.trim().lowercase(Locale.ROOT)
                if (format.isNotBlank()) {
                    val subtitleFile = withContext(Dispatchers.IO) {
                        val rawSubtitle = api.getSubtitle(format, route.mediaId).string()
                        val normalizedSubtitle = RemoteSubtitleNormalizer.normalize(format, rawSubtitle)
                        writeRemoteSubtitleCache(format, normalizedSubtitle)
                    }
                    _uiState.update {
                        it.copy(
                            subtitleFormat = format,
                            subtitleUrl = subtitleFile.toUri().toString(),
                            subtitleLabel = SubtitleTrackSelector.remoteSubtitleLabel(format),
                            subtitleId = REMOTE_SUBTITLE_TRACK_ID
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun writeRemoteSubtitleCache(format: String, content: String): File {
        val subtitleCacheDir = File(appContext.cacheDir, "remote_subtitles").apply {
            mkdirs()
        }
        val safeFormat = format.takeIf { it.isNotBlank() } ?: "srt"
        val subtitleFile = File(subtitleCacheDir, "${route.mediaId.hashCode()}.$safeFormat")
        subtitleFile.writeText(content)
        return subtitleFile
    }

    private fun shouldLoadRemoteSubtitle(): Boolean {
        return when (route.sourceType) {
            SOURCE_TYPE_PC -> true
            SOURCE_TYPE_CACHE -> {
                val activeServerAddress = connectionManager.connection.value?.let { "${it.host}:${it.port}" }
                val targetServerAddress = route.serverAddress?.takeIf { it.isNotBlank() }
                activeServerAddress != null && activeServerAddress == targetServerAddress
            }
            else -> false
        }
    }

    private fun loadDanmaku() {
        loadCachedDanmaku()?.let { cached ->
            fullDanmakuComments = cached.fullComments
            _uiState.update {
                it.copy(
                    danmakuItems = cached.items,
                    danmakuSources = cached.sources,
                    danmakuSourceSummaries = cached.sourceSummaries,
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
                val comments = DanmakuParser.parseFullComments(response.comment)
                fullDanmakuComments = comments
                val sources = response.source ?: emptyList()
                val items = DanmakuParser.toDisplayItems(comments, sources)
                _uiState.update {
                    it.copy(
                        danmakuItems = items,
                        danmakuSources = sources,
                        danmakuSourceSummaries = DanmakuParser.buildSourceSummaries(comments, sources),
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

    private fun shouldRefreshPlaylistForEpisodeContext(): Boolean {
        if (route.sourceType == SOURCE_TYPE_PC) return true
        if (route.sourceType != SOURCE_TYPE_CACHE) return false

        val targetServerAddress = route.serverAddress?.takeIf { it.isNotBlank() } ?: return false
        val activeServerAddress = connectionManager.connection.value?.let { "${it.host}:${it.port}" }
        return activeServerAddress == targetServerAddress
    }

    private fun resolveEpisodeServerAddress(): String? {
        val activeServerAddress = connectionManager.connection.value?.let { "${it.host}:${it.port}" }
        return when (route.sourceType) {
            SOURCE_TYPE_CACHE -> route.serverAddress ?: activeServerAddress
            SOURCE_TYPE_LOCAL -> null
            else -> activeServerAddress ?: route.serverAddress
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
            val fullComments = when (payload.format) {
                CachedDanmakuPayload.FORMAT_FULL -> DanmakuParser.parseFullComments(payload.comment)
                else -> emptyList()
            }
            val items = when (payload.format) {
                CachedDanmakuPayload.FORMAT_FULL -> DanmakuParser.toDisplayItems(fullComments, payload.sources ?: emptyList())
                else -> DanmakuParser.parseV3(payload.comment)
            }
            CachedDanmakuState(
                items = items,
                sources = payload.sources ?: emptyList(),
                sourceSummaries = DanmakuParser.buildSourceSummaries(fullComments, payload.sources ?: emptyList()),
                fullComments = fullComments,
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
        val pool = route.danmuPool
        if (pool.isNullOrBlank()) {
            loadDanmaku()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDanmakuRefreshing = true) }
            try {
                val response = api.getDanmakuFull(pool, update = true)
                val appendedComments = DanmakuParser.parseFullComments(response.comment)
                val mergedComments = mergeFullComments(fullDanmakuComments, appendedComments)
                fullDanmakuComments = mergedComments

                val sources = uiState.value.danmakuSources
                _uiState.update {
                    it.copy(
                        danmakuItems = DanmakuParser.toDisplayItems(mergedComments, sources),
                        danmakuSourceSummaries = DanmakuParser.buildSourceSummaries(
                            comments = mergedComments,
                            sources = sources,
                            newCommentCountBySource = appendedComments.groupingBy { comment -> comment.sourceId }.eachCount()
                        ),
                        isDanmakuRefreshing = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isDanmakuRefreshing = false) }
                loadDanmaku()
            }
        }
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

    suspend fun resolvePlaybackRouteForEpisode(episode: EpisodeUiItem): VideoPlayerRoute {
        val serverAddress = resolveEpisodeServerAddress()
        val cacheTask = cacheRepository.getPlayableCache(episode.mediaId, serverAddress)

        return VideoPlayerRoute(
            mediaId = episode.mediaId,
            title = episode.title,
            sourceType = if (cacheTask != null) SOURCE_TYPE_CACHE else SOURCE_TYPE_PC,
            danmuPool = episode.danmuPool,
            animeTitle = episode.animeTitle,
            localPath = cacheTask?.localPath,
            serverAddress = cacheTask?.serverAddress ?: serverAddress,
            parentPath = uiState.value.parentPath,
            startPositionMs = episode.startPositionMs,
            initialPlayTimeState = episode.playTimeState ?: 0
        )
    }

    fun toggleDanmaku() {
        val updatedPreferences = uiState.value.playerPreferences.copy(
            isDanmakuVisible = !uiState.value.isDanmakuVisible
        )
        _uiState.update {
            it.copy(
                isDanmakuVisible = updatedPreferences.isDanmakuVisible,
                playerPreferences = updatedPreferences
            )
        }
        viewModelScope.launch {
            settingsRepository.setPlayerPreferences(updatedPreferences)
        }
    }

    fun updatePlayerPreferences(preferences: PlayerPreferences) {
        _uiState.update {
            it.copy(
                playerPreferences = preferences,
                isDanmakuVisible = preferences.isDanmakuVisible
            )
        }
        viewModelScope.launch {
            settingsRepository.setPlayerPreferences(preferences)
        }
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
        val isTerminalSync = thumbnailData != null
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                var resolvedThumbnailData = thumbnailData
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
                    resolvedThumbnailData = watchHistoryRepository.record(
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

                if (!isTerminalSync || route.sourceType != SOURCE_TYPE_PC || !uiState.value.isSyncPlayProgressEnabled) return@withContext
                try {
                    val remoteSupportsPreview = connectionManager.connection.value?.kikoVersion?.let { it >= 200100 } == true
                    api.updatePlayTime(
                        UpdateTimeRequest(
                            mediaId = route.mediaId,
                            playTime = playTimeSeconds,
                            playTimeState = playTimeState,
                            preview = resolvedThumbnailData
                                ?.takeIf { remoteSupportsPreview }
                                ?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                        )
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun updateSourceDelay(sourceId: Int, delay: Long) {
        val pool = route.danmuPool ?: return
        viewModelScope.launch {
            try {
                api.updateDelay(UpdateDelayRequest(pool, delay, sourceId))
                updateSourceState(
                    sourceId = sourceId,
                    sourceTransform = { source -> source.copy(delay = delay) },
                    summaryTransform = { source -> source.copy(delayMs = delay) }
                )
            } catch (_: Exception) {}
        }
    }

    fun updateSourceTimeline(sourceId: Int, timeline: String) {
        val pool = route.danmuPool ?: return
        viewModelScope.launch {
            try {
                api.updateTimeline(UpdateTimelineRequest(pool, timeline, sourceId))
                updateSourceState(
                    sourceId = sourceId,
                    sourceTransform = { source -> source.copy(timeline = timeline) },
                    summaryTransform = { source ->
                        source.copy(
                            timeline = timeline,
                            timelineSegmentCount = countTimelineSegments(timeline)
                        )
                    }
                )
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
        val sourceSummaries: List<DanmakuSourceSummary>,
        val fullComments: List<FullDanmakuComment>,
        val launchScripts: List<String>
    )

    private fun updateSourceState(
        sourceId: Int,
        sourceTransform: (DanmakuSource) -> DanmakuSource,
        summaryTransform: (DanmakuSourceSummary) -> DanmakuSourceSummary
    ) {
        _uiState.update { state ->
            state.copy(
                danmakuSources = state.danmakuSources.map { source ->
                    if (source.id == sourceId) sourceTransform(source) else source
                },
                danmakuSourceSummaries = state.danmakuSourceSummaries.map { source ->
                    if (source.id == sourceId) summaryTransform(source) else source
                }
            )
        }
        rebuildDanmakuDisplay()
    }

    private fun rebuildDanmakuDisplay() {
        val sources = uiState.value.danmakuSources
        if (fullDanmakuComments.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                danmakuItems = DanmakuParser.toDisplayItems(fullDanmakuComments, sources),
                danmakuSourceSummaries = DanmakuParser.buildSourceSummaries(
                    comments = fullDanmakuComments,
                    sources = sources,
                    newCommentCountBySource = state.danmakuSourceSummaries.associate { it.id to it.newCommentCount }
                )
            )
        }
    }

    private fun countTimelineSegments(timeline: String): Int {
        if (timeline.isBlank()) return 0
        return timeline
            .split(';')
            .count { it.isNotBlank() && it.trim().split(Regex("\\s+")).size >= 2 }
    }

    private fun mergeFullComments(
        existing: List<FullDanmakuComment>,
        appended: List<FullDanmakuComment>
    ): List<FullDanmakuComment> {
        if (existing.isEmpty()) return appended
        if (appended.isEmpty()) return existing

        return (existing + appended)
            .distinctBy { comment ->
                listOf(
                    comment.sourceId,
                    comment.rawTimeMs,
                    comment.type,
                    comment.color,
                    comment.text,
                    comment.sender
                )
            }
            .sortedBy { it.rawTimeMs }
    }

    private fun normalizeResumePositionMs(
        playTimeSeconds: Double?,
        playTimeState: Int?
    ): Long {
        if (playTimeState == 2) return 0L
        return ((playTimeSeconds ?: 0.0) * 1000).toLong()
    }
}
