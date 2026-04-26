package com.kiko.kikoplay.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiko.kikoplay.data.local.entity.CacheTaskEntity
import com.kiko.kikoplay.data.remote.KikoPlayApi
import com.kiko.kikoplay.data.remote.model.PlaylistNode
import com.kiko.kikoplay.data.remote.model.UpdateTimeRequest
import com.kiko.kikoplay.data.repository.CacheRepository
import com.kiko.kikoplay.data.repository.ConnectionRepository
import com.kiko.kikoplay.data.repository.PlaylistRepository
import com.kiko.kikoplay.data.repository.SettingsRepository
import com.kiko.kikoplay.ui.navigation.VideoPlayerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class PlayStateFilter(val label: String) {
    ALL("\u5168\u90e8"),
    UNWATCHED("\u672a\u64ad\u653e"),
    WATCHING("\u672a\u770b\u5b8c"),
    WATCHED("\u5df2\u770b\u5b8c")
}

data class BreadcrumbItem(val name: String, val index: Int)

data class ListScrollPosition(
    val index: Int = 0,
    val offset: Int = 0
)

data class PlaylistUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentItems: List<PlaylistNode> = emptyList(),
    val cachedMediaIds: Set<String> = emptySet(),
    val pathStack: List<BreadcrumbItem> = emptyList(),
    val scrollPosition: ListScrollPosition = ListScrollPosition(),
    val filter: PlayStateFilter = PlayStateFilter.ALL,
    val isSelectionMode: Boolean = false,
    val selectedIndices: Set<Int> = emptySet()
)

@HiltViewModel
class PlaylistBrowserViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val connectionRepository: ConnectionRepository,
    private val cacheRepository: CacheRepository,
    private val settingsRepository: SettingsRepository,
    private val api: KikoPlayApi
) : ViewModel() {
    private companion object {
        const val SOURCE_TYPE_PC = 0
        const val SOURCE_TYPE_CACHE = 2
    }

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    private val scrollPositions = mutableMapOf<List<Int>, ListScrollPosition>()
    private var completedCacheTasks: List<CacheTaskEntity> = emptyList()

    init {
        observePlaylistProgressUpdates()
        observeCachedMediaIds()
        loadPlaylist()
    }

    fun loadPlaylist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = playlistRepository.fetchPlaylist()
            result.onSuccess { nodes ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentItems = applyFilter(nodes, it.filter),
                        pathStack = emptyList(),
                        scrollPosition = scrollPositionFor(emptyList())
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "\u52a0\u8f7d\u5931\u8d25")
                }
            }
        }
    }

    fun rememberCurrentScrollPosition(index: Int, offset: Int) {
        scrollPositions[currentPathIndices()] = ListScrollPosition(index = index, offset = offset)
    }

    fun navigateToFolder(index: Int, node: PlaylistNode) {
        val children = node.nodes ?: return
        _uiState.update { state ->
            val newPath = state.pathStack + BreadcrumbItem(node.text, index)
            state.copy(
                currentItems = applyFilter(children, state.filter),
                pathStack = newPath,
                scrollPosition = scrollPositionFor(newPath.map { it.index }),
                isSelectionMode = false,
                selectedIndices = emptySet()
            )
        }
    }

    fun navigateUp() {
        val state = _uiState.value
        if (state.pathStack.isEmpty()) return

        val newPath = state.pathStack.dropLast(1)
        val pathIndices = newPath.map { it.index }
        val items = if (newPath.isEmpty()) {
            playlistRepository.getCachedPlaylist() ?: emptyList()
        } else {
            playlistRepository.getNodeAtPath(pathIndices) ?: emptyList()
        }

        _uiState.update {
            it.copy(
                currentItems = applyFilter(items, it.filter),
                pathStack = newPath,
                scrollPosition = scrollPositionFor(pathIndices),
                isSelectionMode = false,
                selectedIndices = emptySet()
            )
        }
    }

    fun navigateToPathIndex(index: Int) {
        val state = _uiState.value
        if (index < 0) {
            val items = playlistRepository.getCachedPlaylist() ?: emptyList()
            _uiState.update {
                it.copy(
                    currentItems = applyFilter(items, it.filter),
                    pathStack = emptyList(),
                    scrollPosition = scrollPositionFor(emptyList()),
                    isSelectionMode = false,
                    selectedIndices = emptySet()
                )
            }
            return
        }

        val newPath = state.pathStack.take(index + 1)
        val pathIndices = newPath.map { it.index }
        val items = playlistRepository.getNodeAtPath(pathIndices) ?: emptyList()
        _uiState.update {
            it.copy(
                currentItems = applyFilter(items, it.filter),
                pathStack = newPath,
                scrollPosition = scrollPositionFor(pathIndices),
                isSelectionMode = false,
                selectedIndices = emptySet()
            )
        }
    }

    fun setFilter(filter: PlayStateFilter) {
        val state = _uiState.value
        val rawItems = if (state.pathStack.isEmpty()) {
            playlistRepository.getCachedPlaylist() ?: emptyList()
        } else {
            playlistRepository.getNodeAtPath(currentPathIndices(state.pathStack)) ?: emptyList()
        }
        _uiState.update {
            it.copy(
                filter = filter,
                currentItems = applyFilter(rawItems, filter),
                scrollPosition = scrollPositionFor(currentPathIndices(it.pathStack))
            )
        }
    }

    fun toggleSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = !it.isSelectionMode, selectedIndices = emptySet()) }
    }

    fun toggleSelection(index: Int) {
        _uiState.update { state ->
            val newSet = state.selectedIndices.toMutableSet()
            if (newSet.contains(index)) {
                newSet.remove(index)
            } else {
                newSet.add(index)
            }
            state.copy(selectedIndices = newSet)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedIndices = state.currentItems.indices.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(isSelectionMode = false, selectedIndices = emptySet()) }
    }

    fun cacheSelected() {
        val state = _uiState.value
        val conn = connectionRepository.activeConnection.value ?: return
        viewModelScope.launch {
            state.selectedIndices.forEach { index ->
                val node = state.currentItems.getOrNull(index) ?: return@forEach
                if (!node.isFolder && node.mediaId != null) {
                    cacheRepository.enqueue(
                        mediaId = node.mediaId,
                        title = node.text,
                        animeTitle = node.animeName,
                        danmuPool = node.danmuPool,
                        serverAddress = "${conn.host}:${conn.port}"
                    )
                }
            }
            clearSelection()
        }
    }

    fun markSelectedWatched() {
        val state = _uiState.value
        viewModelScope.launch {
            val shouldSyncPlayProgress = settingsRepository.syncPlayProgress.first()
            state.selectedIndices.forEach { index ->
                val node = state.currentItems.getOrNull(index) ?: return@forEach
                if (shouldSyncPlayProgress && !node.isFolder && node.mediaId != null) {
                    try {
                        api.updatePlayTime(
                            UpdateTimeRequest(mediaId = node.mediaId, playTime = 0.0, playTimeState = 2)
                        )
                    } catch (_: Exception) {
                    }
                }
            }
            clearSelection()
            loadPlaylist()
        }
    }

    suspend fun resolvePlaybackRouteForNode(
        node: PlaylistNode,
        parentPath: List<Int>,
        startPositionMs: Long,
        initialPlayTimeState: Int
    ): VideoPlayerRoute {
        val mediaId = node.mediaId ?: error("Cannot resolve playback route for folder node")
        val activeServerAddress = resolveActiveServerAddress()
        val cacheTask = cacheRepository.getPlayableCache(mediaId, activeServerAddress)

        return VideoPlayerRoute(
            mediaId = mediaId,
            title = node.text,
            sourceType = if (cacheTask != null) SOURCE_TYPE_CACHE else SOURCE_TYPE_PC,
            danmuPool = node.danmuPool,
            animeTitle = node.animeName,
            localPath = cacheTask?.localPath,
            serverAddress = cacheTask?.serverAddress ?: activeServerAddress,
            parentPath = parentPath,
            startPositionMs = startPositionMs,
            initialPlayTimeState = initialPlayTimeState
        )
    }

    private fun observePlaylistProgressUpdates() {
        viewModelScope.launch {
            playlistRepository.progressUpdates.collect {
                refreshCurrentItemsFromCache()
            }
        }
    }

    private fun observeCachedMediaIds() {
        viewModelScope.launch {
            cacheRepository.getCompletedTasks().collect { tasks ->
                completedCacheTasks = tasks
                refreshCachedMediaIds()
            }
        }
        viewModelScope.launch {
            connectionRepository.activeConnection.collect {
                refreshCachedMediaIds()
            }
        }
    }

    private fun refreshCachedMediaIds() {
        val activeServerAddress = connectionRepository.activeConnection.value?.let { "${it.host}:${it.port}" }
        val cachedMediaIds = if (activeServerAddress == null) {
            emptySet()
        } else {
            completedCacheTasks.asSequence()
                .filter { task ->
                    task.serverAddress == activeServerAddress &&
                        task.localPath?.let { File(it).exists() } == true
                }
                .map { it.mediaId }
                .toSet()
        }
        _uiState.update { state ->
            if (state.cachedMediaIds == cachedMediaIds) state else state.copy(cachedMediaIds = cachedMediaIds)
        }
    }

    private fun refreshCurrentItemsFromCache() {
        val state = _uiState.value
        val rawItems = if (state.pathStack.isEmpty()) {
            playlistRepository.getCachedPlaylist() ?: return
        } else {
            playlistRepository.getNodeAtPath(currentPathIndices(state.pathStack)) ?: return
        }

        _uiState.update {
            val refreshedItems = applyFilter(rawItems, it.filter)
            it.copy(
                currentItems = refreshedItems,
                scrollPosition = scrollPositionFor(currentPathIndices(it.pathStack)),
                selectedIndices = it.selectedIndices.filter { index -> index in refreshedItems.indices }.toSet()
            )
        }
    }

    private fun resolveActiveServerAddress(): String? {
        return connectionRepository.activeConnection.value?.let { "${it.host}:${it.port}" }
    }

    private fun currentPathIndices(
        pathStack: List<BreadcrumbItem> = _uiState.value.pathStack
    ): List<Int> {
        return pathStack.map { it.index }
    }

    private fun scrollPositionFor(path: List<Int>): ListScrollPosition {
        return scrollPositions[path] ?: ListScrollPosition()
    }

    private fun applyFilter(items: List<PlaylistNode>, filter: PlayStateFilter): List<PlaylistNode> {
        if (filter == PlayStateFilter.ALL) return items
        return items.filter { node ->
            if (node.isFolder) {
                true
            } else {
                when (filter) {
                    PlayStateFilter.UNWATCHED -> node.playTimeState == 0
                    PlayStateFilter.WATCHING -> node.playTimeState == 1
                    PlayStateFilter.WATCHED -> node.playTimeState == 2
                    PlayStateFilter.ALL -> true
                }
            }
        }
    }
}
