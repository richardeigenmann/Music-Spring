package org.richinet.musicandroid

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class TrackViewModel(private val apiService: ApiService) : ScreenModel {
    private val _tags = MutableStateFlow<UiState<List<Tag>>>(UiState.Loading)
    val tags = _tags.asStateFlow()

    private val _tracks = MutableStateFlow<UiState<List<Track>>>(UiState.Loading)
    val tracks = _tracks.asStateFlow()

    init {
        loadTags()
    }

    fun loadTags() {
        screenModelScope.launch {
            _tags.value = UiState.Loading
            try {
                _tags.value = UiState.Success(apiService.getTags())
            } catch (e: Exception) {
                _tags.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadTracksByTag(tagId: Long) {
        screenModelScope.launch {
            _tracks.value = UiState.Loading
            try {
                _tracks.value = UiState.Success(apiService.getTracksByTag(tagId))
            } catch (e: Exception) {
                _tracks.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private val _searchResults = MutableStateFlow<UiState<List<Track>>>(UiState.Success(emptyList()))
    val searchResults: StateFlow<UiState<List<Track>>> = _searchResults.asStateFlow()

    private val _filteredTracks = MutableStateFlow<UiState<List<Track>>>(UiState.Success(emptyList()))
    val filteredTracks: StateFlow<UiState<List<Track>>> = _filteredTracks.asStateFlow()

    fun searchTracks(query: String) {
        if (query.isBlank()) {
            _searchResults.value = UiState.Success(emptyList())
            return
        }
        screenModelScope.launch {
            _searchResults.value = UiState.Loading
            try {
                _searchResults.value = UiState.Success(apiService.searchTracks(query))
            } catch (e: Exception) {
                _searchResults.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun filterTracks(must: List<Long>, can: List<Long>, not: List<Long>) {
        if (must.isEmpty() && can.isEmpty() && not.isEmpty()) {
            _filteredTracks.value = UiState.Success(emptyList())
            return
        }
        screenModelScope.launch {
            _filteredTracks.value = UiState.Loading
            try {
                _filteredTracks.value = UiState.Success(apiService.filterTracks(must, can, not))
            } catch (e: Exception) {
                _filteredTracks.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
