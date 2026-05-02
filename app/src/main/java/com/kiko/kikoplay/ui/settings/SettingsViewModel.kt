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

    val syncPlayProgress: StateFlow<Boolean> = settingsRepository.syncPlayProgress.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val themeMode: StateFlow<String> = settingsRepository.themeMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "system"
    )

    val fetchPcRecent: StateFlow<Boolean> = settingsRepository.fetchPcRecent.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    fun setSyncPlayProgress(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSyncPlayProgress(enabled) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setFetchPcRecent(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setFetchPcRecent(enabled) }
    }
}
