package com.kiko.kikoplay.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiko.kikoplay.data.local.entity.WatchHistoryEntity
import com.kiko.kikoplay.data.remote.ConnectionInfo
import com.kiko.kikoplay.data.remote.ConnectionManager
import com.kiko.kikoplay.data.repository.HistoryPlaybackCoordinator
import com.kiko.kikoplay.data.repository.HistoryPlaybackDecision
import com.kiko.kikoplay.data.repository.HistoryPlaybackTarget
import com.kiko.kikoplay.data.remote.model.PlaylistNode
import com.kiko.kikoplay.data.repository.PlaylistRepository
import com.kiko.kikoplay.data.repository.SwitchConnectionRequest
import com.kiko.kikoplay.data.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    connectionManager: ConnectionManager,
    watchHistoryRepository: WatchHistoryRepository,
    private val playlistRepository: PlaylistRepository,
    private val historyPlaybackCoordinator: HistoryPlaybackCoordinator
) : ViewModel() {

    val connectionInfo: StateFlow<ConnectionInfo?> = connectionManager.connection

    val recentHistory: StateFlow<List<WatchHistoryEntity>> =
        watchHistoryRepository.getRecent(10).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    private val _recentDirs = MutableStateFlow<List<PlaylistNode>>(emptyList())
    val recentDirs: StateFlow<List<PlaylistNode>> = _recentDirs.asStateFlow()

    private val _pendingSwitchRequest = MutableStateFlow<SwitchConnectionRequest?>(null)
    val pendingSwitchRequest: StateFlow<SwitchConnectionRequest?> = _pendingSwitchRequest.asStateFlow()

    private val _historyPlaybackEvents = MutableSharedFlow<HistoryPlaybackTarget>()
    val historyPlaybackEvents: SharedFlow<HistoryPlaybackTarget> = _historyPlaybackEvents.asSharedFlow()

    private val _isResolvingHistory = MutableStateFlow(false)
    val isResolvingHistory: StateFlow<Boolean> = _isResolvingHistory.asStateFlow()

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
}
