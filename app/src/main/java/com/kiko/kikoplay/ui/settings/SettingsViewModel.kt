package com.kiko.kikoplay.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiko.kikoplay.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    val smallWindowPlayback: StateFlow<Boolean> = settingsRepository.smallWindowPlayback.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val backgroundPlayback: StateFlow<Boolean> = settingsRepository.backgroundPlayback.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    fun setSyncPlayProgress(enabled: Boolean) {
        persistSetting { settingsRepository.setSyncPlayProgress(enabled) }
    }

    fun setThemeMode(mode: String) {
        persistSetting { settingsRepository.setThemeMode(mode) }
    }

    fun setFetchPcRecent(enabled: Boolean) {
        persistSetting { settingsRepository.setFetchPcRecent(enabled) }
    }

    fun setSmallWindowPlayback(enabled: Boolean) {
        persistSetting { settingsRepository.setSmallWindowPlayback(enabled) }
    }

    fun setBackgroundPlayback(enabled: Boolean) {
        persistSetting { settingsRepository.setBackgroundPlayback(enabled) }
    }

    private fun persistSetting(block: suspend () -> Unit) {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                block()
            }
        }
    }
}
