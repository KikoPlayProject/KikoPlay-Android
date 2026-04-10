package com.kiko.kikoplay.ui.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiko.kikoplay.data.repository.LocalVideo
import com.kiko.kikoplay.data.repository.LocalVideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption(val label: String) {
    NAME("名称"),
    DATE("修改时间"),
    SIZE("大小")
}

data class LocalVideosUiState(
    val isLoading: Boolean = false,
    val videos: List<LocalVideo> = emptyList(),
    val folders: Map<String, List<LocalVideo>> = emptyMap(),
    val isFolderView: Boolean = false,
    val currentFolder: String? = null,
    val sortOption: SortOption = SortOption.DATE
)

@HiltViewModel
class LocalVideosViewModel @Inject constructor(
    private val localVideoRepository: LocalVideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalVideosUiState())
    val uiState: StateFlow<LocalVideosUiState> = _uiState.asStateFlow()

    private var allVideos: List<LocalVideo> = emptyList()

    fun loadVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            allVideos = localVideoRepository.scanVideos()
            val sorted = sortVideos(allVideos, _uiState.value.sortOption)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    videos = sorted,
                    folders = localVideoRepository.groupByFolder(sorted)
                )
            }
        }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isFolderView = !it.isFolderView, currentFolder = null) }
    }

    fun setSortOption(option: SortOption) {
        val sorted = sortVideos(allVideos, option)
        _uiState.update {
            it.copy(
                sortOption = option,
                videos = sorted,
                folders = localVideoRepository.groupByFolder(sorted)
            )
        }
    }

    fun openFolder(folder: String) {
        _uiState.update { it.copy(currentFolder = folder) }
    }

    fun navigateUp() {
        _uiState.update { it.copy(currentFolder = null) }
    }

    private fun sortVideos(videos: List<LocalVideo>, option: SortOption): List<LocalVideo> {
        return when (option) {
            SortOption.NAME -> videos.sortedBy { it.displayName.lowercase() }
            SortOption.DATE -> videos.sortedByDescending { it.dateModified }
            SortOption.SIZE -> videos.sortedByDescending { it.size }
        }
    }
}
