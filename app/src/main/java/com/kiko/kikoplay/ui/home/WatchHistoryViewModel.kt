package com.kiko.kikoplay.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiko.kikoplay.data.local.entity.WatchHistoryEntity
import com.kiko.kikoplay.data.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchHistoryViewModel @Inject constructor(
    private val watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {

    val allHistory: StateFlow<List<WatchHistoryEntity>> =
        watchHistoryRepository.getAll().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    fun delete(entity: WatchHistoryEntity) {
        viewModelScope.launch { watchHistoryRepository.delete(entity) }
    }

    fun clearAll() {
        viewModelScope.launch { watchHistoryRepository.clearAll() }
    }
}
