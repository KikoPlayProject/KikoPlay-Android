package com.kiko.kikoplay.ui.cache

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiko.kikoplay.data.local.entity.CacheTaskEntity
import com.kiko.kikoplay.data.repository.CacheRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CacheViewModel @Inject constructor(
    private val cacheRepository: CacheRepository
) : ViewModel() {

    val activeTasks: StateFlow<List<CacheTaskEntity>> =
        cacheRepository.getActiveTasks().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    val completedTasks: StateFlow<List<CacheTaskEntity>> =
        cacheRepository.getCompletedTasks().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    fun pauseTask(task: CacheTaskEntity) {
        viewModelScope.launch { cacheRepository.pauseTask(task.id) }
    }

    fun resumeTask(task: CacheTaskEntity) {
        viewModelScope.launch { cacheRepository.resumeTask(task.id) }
    }

    fun cancelTask(task: CacheTaskEntity) {
        viewModelScope.launch { cacheRepository.cancelTask(task) }
    }

    fun deleteCompleted(task: CacheTaskEntity) {
        viewModelScope.launch { cacheRepository.deleteCompleted(task) }
    }
}
