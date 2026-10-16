package app.quranaudio.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.quranaudio.data.repository.CatalogRepository
import app.quranaudio.data.repository.LibraryRepository
import app.quranaudio.domain.ListeningEntry
import app.quranaudio.domain.PlaylistItem
import app.quranaudio.domain.Reciter
import app.quranaudio.domain.ThemeCollection
import app.quranaudio.domain.Track
import app.quranaudio.domain.usecase.PlayUseCase
import app.quranaudio.ui.navigation.CollectionRoute
import app.quranaudio.ui.navigation.PlaylistRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailUiState(
    val loading: Boolean = true,
    val exists: Boolean = true,
    val name: String = "",
    val items: List<PlaylistItem> = emptyList(),
) {
    val playable: List<Track> get() = items.mapNotNull { it.track }
}

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val library: LibraryRepository,
    private val play: PlayUseCase,
) : ViewModel() {
    val playlistId = savedStateHandle.toRoute<PlaylistRoute>().playlistId

    val state: StateFlow<PlaylistDetailUiState> = combine(library.playlist(playlistId), library.playlistItems(playlistId)) { p, items ->
        PlaylistDetailUiState(loading = false, exists = p != null, name = p?.name.orEmpty(), items = items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistDetailUiState())

    fun playFrom(item: PlaylistItem) = viewModelScope.launch {
        val tracks = state.value.playable
        play.playTracks(tracks, tracks.indexOfFirst { it.key == item.track?.key }.coerceAtLeast(0), shuffle = false)
    }

    fun playAll(shuffle: Boolean) = viewModelScope.launch { play.playTracks(state.value.playable, 0, shuffle = shuffle) }
    fun rename(name: String) = viewModelScope.launch { library.renamePlaylist(playlistId, name) }
    fun delete() = viewModelScope.launch { library.deletePlaylist(playlistId) }
    fun remove(item: PlaylistItem) = viewModelScope.launch { library.removeFromPlaylist(playlistId, item.entryId) }

    fun move(item: PlaylistItem, delta: Int) = viewModelScope.launch {
        val ids = state.value.items.map { it.entryId }.toMutableList()
        val from = ids.indexOf(item.entryId)
        val to = from + delta
        if (from < 0 || to !in ids.indices) return@launch
        ids.add(to, ids.removeAt(from))
        library.reorderPlaylist(playlistId, ids)
    }
}

data class TrackListUiState(val loading: Boolean = true, val tracks: List<Track> = emptyList(), val entries: List<ListeningEntry> = emptyList())

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    library: LibraryRepository,
    catalog: CatalogRepository,
    private val play: PlayUseCase,
) : ViewModel() {
    val state: StateFlow<TrackListUiState> = library.favouriteTracks.map { TrackListUiState(loading = false, tracks = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackListUiState())

    val favouriteReciters: StateFlow<List<Reciter>> = catalog.reciters.map { list -> list.filter { it.isFavourite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun play(index: Int, shuffle: Boolean = false) = viewModelScope.launch { play.playTracks(state.value.tracks, index, shuffle) }
}

@HiltViewModel
class RecentViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val play: PlayUseCase,
) : ViewModel() {
    val state: StateFlow<TrackListUiState> = library.recentlyPlayed(limit = 100)
        .map { entries -> TrackListUiState(loading = false, tracks = entries.map { it.track }, entries = entries) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackListUiState())

    fun resume(entry: ListeningEntry) = viewModelScope.launch { play.continueListening(entry) }
    fun clear() = viewModelScope.launch { library.clearHistory() }
}

data class CollectionUiState(
    val collection: ThemeCollection,
    val reciter: Reciter? = null,
    val reciters: List<Reciter> = emptyList(),
    val tracks: Map<Int, Track> = emptyMap(),
    val loading: Boolean = true,
)

@HiltViewModel
class CollectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val play: PlayUseCase,
) : ViewModel() {
    val collection: ThemeCollection =
        ThemeCollection.fromKey(savedStateHandle.toRoute<CollectionRoute>().key) ?: ThemeCollection.MOST_BEAUTIFUL

    private val selectedReciterId = MutableStateFlow<String?>(null)

    val state: StateFlow<CollectionUiState> = combine(catalog.reciters, selectedReciterId) { reciters, selected ->
        val eligible = reciters.filter { r -> r.hasCompleteQuran }.sortedWith(compareBy({ !it.featured }, { it.featuredRank ?: Int.MAX_VALUE }, { it.name }))
        val reciter = reciters.firstOrNull { it.id == selected } ?: eligible.firstOrNull()
        val tracks = reciter?.let { r -> collection.surahs.mapNotNull { s -> catalog.trackForSurah(r.id, s)?.let { s to it } }.toMap() }.orEmpty()
        CollectionUiState(collection, reciter, eligible, tracks, loading = reciters.isEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionUiState(collection))

    init {
        viewModelScope.launch { selectedReciterId.value = play.defaultReciterId() }
    }

    fun selectReciter(id: String) { selectedReciterId.value = id }

    fun play(startIndex: Int = 0, shuffle: Boolean = false) = viewModelScope.launch {
        val r = state.value.reciter ?: return@launch
        play.playSurahsWithReciter(collection.surahs, r.id, startIndex, shuffle = shuffle)
    }
}
