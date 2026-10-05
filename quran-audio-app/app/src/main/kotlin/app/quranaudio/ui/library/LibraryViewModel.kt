package app.quranaudio.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quranaudio.data.repository.LibraryRepository
import app.quranaudio.domain.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val loading: Boolean = true,
    val playlists: List<Playlist> = emptyList(),
    val favouriteCount: Int = 0,
    val recentCount: Int = 0,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(private val library: LibraryRepository) : ViewModel() {

    val state: StateFlow<LibraryUiState> = combine(
        library.playlistSummaries,
        library.favouriteTrackKeys.map { it.size },
        library.recentlyPlayed(limit = 50).map { it.size },
    ) { playlists, favCount, recentCount ->
        LibraryUiState(loading = false, playlists = playlists, favouriteCount = favCount, recentCount = recentCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun create(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch { onCreated(library.createPlaylist(name)) }
    }
}
