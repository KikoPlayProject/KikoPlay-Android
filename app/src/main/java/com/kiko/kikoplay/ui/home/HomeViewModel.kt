package com.kiko.kikoplay.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiko.kikoplay.data.local.entity.WatchHistoryEntity
import com.kiko.kikoplay.data.remote.ConnectionInfo
import com.kiko.kikoplay.data.remote.ConnectionManager
import com.kiko.kikoplay.data.remote.model.PlaylistNode
import com.kiko.kikoplay.data.repository.PlaylistRepository
import com.kiko.kikoplay.data.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    connectionManager: ConnectionManager,
    watchHistoryRepository: WatchHistoryRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    val connectionInfo: StateFlow<ConnectionInfo?> = connectionManager.connection

    val recentHistory: StateFlow<List<WatchHistoryEntity>> =
        watchHistoryRepository.getRecent(10).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    private val _recentDirs = MutableStateFlow<List<PlaylistNode>>(emptyList())
    val recentDirs: StateFlow<List<PlaylistNode>> = _recentDirs.asStateFlow()

    fun loadRecentDirs() {
        viewModelScope.launch {
            val cached = playlistRepository.getCachedPlaylist()
            if (cached != null) {
                _recentDirs.value = cached.filter { it.isFolder }.take(4)
            } else {
                val result = playlistRepository.fetchPlaylist()
                result.onSuccess { nodes ->
                    _recentDirs.value = nodes.filter { it.isFolder }.take(4)
                }
            }
        }
    }
}
