package com.kiko.kikoplay.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiko.kikoplay.data.remote.KikoPlayApi
import com.kiko.kikoplay.data.remote.model.PlaylistNode
import com.kiko.kikoplay.data.remote.model.UpdateTimeRequest
import com.kiko.kikoplay.data.repository.CacheRepository
import com.kiko.kikoplay.data.repository.ConnectionRepository
import com.kiko.kikoplay.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlayStateFilter(val label: String) {
    ALL("全部"),
    UNWATCHED("未播放"),
    WATCHING("未看完"),
    WATCHED("已看完")
}

data class BreadcrumbItem(val name: String, val index: Int)

data class PlaylistUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentItems: List<PlaylistNode> = emptyList(),
    val pathStack: List<BreadcrumbItem> = emptyList(),
    val filter: PlayStateFilter = PlayStateFilter.ALL,
    val isSelectionMode: Boolean = false,
    val selectedIndices: Set<Int> = emptySet()
)

@HiltViewModel
class PlaylistBrowserViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val connectionRepository: ConnectionRepository,
    private val cacheRepository: CacheRepository,
    private val api: KikoPlayApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
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
                        pathStack = emptyList()
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "加载失败") }
            }
        }
    }

    fun navigateToFolder(index: Int, node: PlaylistNode) {
        val children = node.nodes ?: return
        _uiState.update { state ->
            val newPath = state.pathStack + BreadcrumbItem(node.text, index)
            state.copy(
                currentItems = applyFilter(children, state.filter),
                pathStack = newPath,
                isSelectionMode = false,
                selectedIndices = emptySet()
            )
        }
    }

    fun navigateUp() {
        val state = _uiState.value
        if (state.pathStack.isEmpty()) return
        val newPath = state.pathStack.dropLast(1)
        val items = if (newPath.isEmpty()) {
            playlistRepository.getCachedPlaylist() ?: emptyList()
        } else {
            val indices = newPath.map { it.index }
            playlistRepository.getNodeAtPath(indices) ?: emptyList()
        }
        _uiState.update {
            it.copy(
                currentItems = applyFilter(items, it.filter),
                pathStack = newPath,
                isSelectionMode = false,
                selectedIndices = emptySet()
            )
        }
    }

    fun navigateToPathIndex(index: Int) {
        val state = _uiState.value
        if (index < 0) {
            // Navigate to root
            val items = playlistRepository.getCachedPlaylist() ?: emptyList()
            _uiState.update {
                it.copy(
                    currentItems = applyFilter(items, it.filter),
                    pathStack = emptyList(),
                    isSelectionMode = false,
                    selectedIndices = emptySet()
                )
            }
            return
        }
        val newPath = state.pathStack.take(index + 1)
        val indices = newPath.map { it.index }
        val items = playlistRepository.getNodeAtPath(indices) ?: emptyList()
        _uiState.update {
            it.copy(
                currentItems = applyFilter(items, it.filter),
                pathStack = newPath,
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
            val indices = state.pathStack.map { it.index }
            playlistRepository.getNodeAtPath(indices) ?: emptyList()
        }
        _uiState.update {
            it.copy(filter = filter, currentItems = applyFilter(rawItems, filter))
        }
    }

    fun toggleSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = !it.isSelectionMode, selectedIndices = emptySet()) }
    }

    fun toggleSelection(index: Int) {
        _uiState.update { state ->
            val newSet = state.selectedIndices.toMutableSet()
            if (newSet.contains(index)) newSet.remove(index) else newSet.add(index)
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
            state.selectedIndices.forEach { index ->
                val node = state.currentItems.getOrNull(index) ?: return@forEach
                if (!node.isFolder && node.mediaId != null) {
                    try {
                        api.updatePlayTime(
                            UpdateTimeRequest(mediaId = node.mediaId, playTime = 0.0, playTimeState = 2)
                        )
                    } catch (_: Exception) {}
                }
            }
            clearSelection()
            loadPlaylist()
        }
    }

    private fun applyFilter(items: List<PlaylistNode>, filter: PlayStateFilter): List<PlaylistNode> {
        if (filter == PlayStateFilter.ALL) return items
        return items.filter { node ->
            if (node.isFolder) true
            else when (filter) {
                PlayStateFilter.UNWATCHED -> node.playTimeState == 0
                PlayStateFilter.WATCHING -> node.playTimeState == 1
                PlayStateFilter.WATCHED -> node.playTimeState == 2
                PlayStateFilter.ALL -> true
            }
        }
    }
}
