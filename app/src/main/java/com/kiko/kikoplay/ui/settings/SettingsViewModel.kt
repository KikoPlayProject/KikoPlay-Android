package com.kiko.kikoplay.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiko.kikoplay.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val maxCacheSize: StateFlow<Long> = settingsRepository.maxCacheSize.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )

    val autoClearCache: StateFlow<Boolean> = settingsRepository.autoClearCache.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val syncPlayProgress: StateFlow<Boolean> = settingsRepository.syncPlayProgress.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    fun setMaxCacheSize(bytes: Long) {
        viewModelScope.launch { settingsRepository.setMaxCacheSize(bytes) }
    }

    fun setAutoClearCache(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoClearCache(enabled) }
    }

    fun setSyncPlayProgress(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSyncPlayProgress(enabled) }
    }
}
