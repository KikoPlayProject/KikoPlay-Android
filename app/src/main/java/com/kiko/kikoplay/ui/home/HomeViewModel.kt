package com.kiko.kikoplay.ui.home

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiko.kikoplay.data.local.entity.CacheTaskEntity
import com.kiko.kikoplay.data.local.entity.WatchHistoryEntity
import com.kiko.kikoplay.data.remote.ConnectionInfo
import com.kiko.kikoplay.data.remote.ConnectionManager
import com.kiko.kikoplay.data.repository.HistoryPlaybackCoordinator
import com.kiko.kikoplay.data.repository.HistoryPlaybackDecision
import com.kiko.kikoplay.data.repository.HistoryPlaybackTarget
import com.kiko.kikoplay.data.remote.model.PlaylistNode
import com.kiko.kikoplay.data.repository.CacheRepository
import com.kiko.kikoplay.data.repository.PlaylistRepository
import com.kiko.kikoplay.data.repository.SettingsRepository
import com.kiko.kikoplay.data.repository.SwitchConnectionRequest
import com.kiko.kikoplay.data.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    connectionManager: ConnectionManager,
    watchHistoryRepository: WatchHistoryRepository,
    private val playlistRepository: PlaylistRepository,
    private val historyPlaybackCoordinator: HistoryPlaybackCoordinator,
    private val settingsRepository: SettingsRepository,
    cacheRepository: CacheRepository
) : ViewModel() {
    private val localRecentHistory: Flow<List<WatchHistoryEntity>> = watchHistoryRepository.getRecent(20)

    val connectionInfo: StateFlow<ConnectionInfo?> = connectionManager.connection
    val fetchPcRecent: StateFlow<Boolean> = settingsRepository.fetchPcRecent.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )
    val hasLocalHistory: StateFlow<Boolean> = localRecentHistory
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val pcRecent = MutableStateFlow<List<WatchHistoryEntity>>(emptyList())

    val recentHistory: StateFlow<List<WatchHistoryEntity>> = combine(
        pcRecent,
        localRecentHistory,
        cacheRepository.getCompletedTasks()
    ) { pcItems, localItems, cacheTasks ->
        mergeRecentHistory(
            pcItems = pcItems,
            localItems = localItems,
            cacheTasks = cacheTasks
        ).take(HOME_RECENT_HISTORY_LIMIT)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _recentDirs = MutableStateFlow<List<PlaylistNode>>(emptyList())
    val recentDirs: StateFlow<List<PlaylistNode>> = _recentDirs.asStateFlow()

    private val _pendingSwitchRequest = MutableStateFlow<SwitchConnectionRequest?>(null)
    val pendingSwitchRequest: StateFlow<SwitchConnectionRequest?> = _pendingSwitchRequest.asStateFlow()

    private val _historyPlaybackEvents = MutableSharedFlow<HistoryPlaybackTarget>()
    val historyPlaybackEvents: SharedFlow<HistoryPlaybackTarget> = _historyPlaybackEvents.asSharedFlow()

    private val _isResolvingHistory = MutableStateFlow(false)
    val isResolvingHistory: StateFlow<Boolean> = _isResolvingHistory.asStateFlow()

    fun loadRemoteHomeData() {
        viewModelScope.launch {
            val cached = playlistRepository.getCachedPlaylist()
            val isPlaylistLoaded = if (cached != null) {
                _recentDirs.value = cached.filter { it.isFolder }.take(4)
                true
            } else {
                val result = playlistRepository.fetchPlaylist()
                result.onSuccess { nodes ->
                    _recentDirs.value = nodes.filter { it.isFolder }.take(4)
                }
                result.isSuccess
            }
            if (isPlaylistLoaded) {
                refreshPcRecentInternal()
            } else {
                pcRecent.value = emptyList()
            }
        }
    }

    fun clearRemoteHomeData() {
        _recentDirs.value = emptyList()
        pcRecent.value = emptyList()
    }

    fun onHistoryItemClick(item: WatchHistoryEntity) {
        viewModelScope.launch {
            if (_isResolvingHistory.value) return@launch
            _isResolvingHistory.value = true
            handlePlaybackDecision(historyPlaybackCoordinator.resolve(item))
            _isResolvingHistory.value = false
        }
    }

    fun confirmSwitchConnection() {
        val request = _pendingSwitchRequest.value ?: return
        viewModelScope.launch {
            _pendingSwitchRequest.value = null
            _isResolvingHistory.value = true
            handlePlaybackDecision(historyPlaybackCoordinator.connectAndResolve(request.historyItem))
            _isResolvingHistory.value = false
        }
    }

    fun dismissSwitchConnection() {
        _pendingSwitchRequest.value = null
    }

    private suspend fun handlePlaybackDecision(decision: HistoryPlaybackDecision) {
        when (decision) {
            is HistoryPlaybackDecision.Play -> _historyPlaybackEvents.emit(decision.target)
            is HistoryPlaybackDecision.ConfirmSwitch -> _pendingSwitchRequest.value = decision.request
            HistoryPlaybackDecision.None -> Unit
        }
    }

    private fun mergeRecentHistory(
        pcItems: List<WatchHistoryEntity>,
        localItems: List<WatchHistoryEntity>,
        cacheTasks: List<CacheTaskEntity>
    ): List<WatchHistoryEntity> {
        val cacheTaskByKey = cacheTasks
            .filter { it.status == CacheTaskEntity.STATUS_COMPLETED }
            .associateBy { remoteKey(it.mediaId, it.serverAddress) }
        val localByKey = localItems.associateBy(::historyKey)
        val merged = LinkedHashMap<String, WatchHistoryEntity>()

        pcItems.forEach { pcItem ->
            val key = historyKey(pcItem)
            val localItem = localByKey[key]
            val cacheTask = cacheTaskByKey[key]
            merged[key] = mergePcRecentItem(pcItem, localItem, cacheTask)
        }

        localItems.forEach { item ->
            val key = historyKey(item)
            if (!merged.containsKey(key)) {
                merged[key] = item
            }
        }

        return merged.values.sortedByDescending { it.lastWatched }
    }

    private fun mergePcRecentItem(
        pcItem: WatchHistoryEntity,
        localItem: WatchHistoryEntity?,
        cacheTask: CacheTaskEntity?
    ): WatchHistoryEntity {
        val cachePath = cacheTask?.localPath?.takeIf { File(it).exists() }
        return pcItem.copy(
            id = localItem?.id ?: pcItem.id,
            title = pcItem.title.ifBlank { localItem?.title.orEmpty() },
            animeTitle = pcItem.animeTitle ?: localItem?.animeTitle,
            playTime = if (pcItem.playTime > 0) pcItem.playTime else localItem?.playTime ?: 0L,
            duration = maxOf(pcItem.duration, localItem?.duration ?: 0L),
            playTimeState = pcItem.playTimeState.takeIf { pcItem.playTime > 0 }
                ?: localItem?.playTimeState
                ?: pcItem.playTimeState,
            sourceType = SOURCE_TYPE_PC,
            isCached = localItem?.isCached == true || cachePath != null,
            remoteUri = pcItem.remoteUri ?: localItem?.remoteUri,
            localPath = cachePath ?: localItem?.localPath,
            thumbnailData = pcItem.thumbnailData ?: localItem?.thumbnailData,
            danmuPool = pcItem.danmuPool ?: localItem?.danmuPool,
            serverAddress = pcItem.serverAddress ?: localItem?.serverAddress,
            lastWatched = maxOf(pcItem.lastWatched, localItem?.lastWatched ?: 0L)
        )
    }

    private fun PlaylistNode.toWatchHistory(
        serverAddress: String,
        lastWatched: Long
    ): WatchHistoryEntity? {
        val mediaId = mediaId?.takeIf { it.isNotBlank() } ?: return null
        return WatchHistoryEntity(
            id = pcHistoryId(mediaId, serverAddress),
            mediaId = mediaId,
            title = text,
            animeTitle = animeName,
            playTime = ((playTime ?: 0.0) * 1000).toLong().coerceAtLeast(0L),
            duration = 0L,
            playTimeState = playTimeState ?: 0,
            sourceType = SOURCE_TYPE_PC,
            isCached = false,
            remoteUri = "http://$serverAddress/media/$mediaId",
            thumbnailData = cover?.decodeBase64(),
            lastWatched = lastWatched,
            danmuPool = danmuPool,
            serverAddress = serverAddress
        )
    }

    private fun String.decodeBase64(): ByteArray? {
        return runCatching {
            val payload = substringAfter("base64,", this)
            Base64.decode(payload, Base64.DEFAULT)
        }.getOrNull()
    }

    private fun historyKey(item: WatchHistoryEntity): String {
        return when (item.sourceType) {
            SOURCE_TYPE_LOCAL -> "local:${item.localPath ?: item.mediaId}"
            else -> remoteKey(item.mediaId, item.serverAddress)
        }
    }

    private fun historyKey(mediaId: String, serverAddress: String): String = remoteKey(mediaId, serverAddress)

    private fun remoteKey(mediaId: String, serverAddress: String?): String {
        return "remote:${serverAddress.orEmpty()}:$mediaId"
    }

    private fun pcHistoryId(mediaId: String, serverAddress: String): Long {
        val hash = historyKey(mediaId, serverAddress).hashCode().toLong()
        val positiveHash = if (hash == 0L) 1L else kotlin.math.abs(hash)
        return -positiveHash
    }

    private suspend fun refreshPcRecentInternal() {
        val connection = connectionInfo.value
        if (connection == null || !settingsRepository.fetchPcRecent.first()) {
            pcRecent.value = emptyList()
            return
        }

        val serverAddress = "${connection.host}:${connection.port}"
        val result = playlistRepository.fetchRecent()
        result.onSuccess { nodes ->
            val now = System.currentTimeMillis()
            val previousItems = pcRecent.value.associateBy(::historyKey)
            pcRecent.value = nodes.mapNotNull { node ->
                val mediaId = node.mediaId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val previousLastWatched = previousItems[historyKey(mediaId, serverAddress)]?.lastWatched
                node.toWatchHistory(
                    serverAddress = serverAddress,
                    lastWatched = previousLastWatched ?: now
                )
            }
        }.onFailure {
            pcRecent.value = emptyList()
        }
    }

    companion object {
        const val HOME_RECENT_HISTORY_LIMIT = 4
        private const val SOURCE_TYPE_PC = 0
        private const val SOURCE_TYPE_LOCAL = 1
    }
}
