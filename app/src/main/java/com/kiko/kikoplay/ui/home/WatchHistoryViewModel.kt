package com.kiko.kikoplay.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiko.kikoplay.data.local.entity.WatchHistoryEntity
import com.kiko.kikoplay.data.repository.HistoryPlaybackCoordinator
import com.kiko.kikoplay.data.repository.HistoryPlaybackDecision
import com.kiko.kikoplay.data.repository.HistoryPlaybackTarget
import com.kiko.kikoplay.data.repository.SwitchConnectionRequest
import com.kiko.kikoplay.data.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchHistoryViewModel @Inject constructor(
    private val watchHistoryRepository: WatchHistoryRepository,
    private val historyPlaybackCoordinator: HistoryPlaybackCoordinator
) : ViewModel() {

    val allHistory: StateFlow<List<WatchHistoryEntity>> =
        watchHistoryRepository.getAll().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    private val _pendingSwitchRequest = MutableStateFlow<SwitchConnectionRequest?>(null)
    val pendingSwitchRequest: StateFlow<SwitchConnectionRequest?> = _pendingSwitchRequest.asStateFlow()

    private val _historyPlaybackEvents = MutableSharedFlow<HistoryPlaybackTarget>()
    val historyPlaybackEvents: SharedFlow<HistoryPlaybackTarget> = _historyPlaybackEvents.asSharedFlow()

    private val _isResolvingHistory = MutableStateFlow(false)
    val isResolvingHistory: StateFlow<Boolean> = _isResolvingHistory.asStateFlow()

    fun delete(entity: WatchHistoryEntity) {
        viewModelScope.launch { watchHistoryRepository.delete(entity) }
    }

    fun clearAll() {
        viewModelScope.launch { watchHistoryRepository.clearAll() }
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
