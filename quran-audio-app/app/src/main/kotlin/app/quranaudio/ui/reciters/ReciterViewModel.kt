package app.quranaudio.ui.reciters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.quranaudio.data.db.ListeningHistoryEntity
import app.quranaudio.data.repository.CatalogRepository
import app.quranaudio.data.repository.LibraryRepository
import app.quranaudio.domain.Recitation
import app.quranaudio.domain.Reciter
import app.quranaudio.domain.Track
import app.quranaudio.domain.TrackRef
import app.quranaudio.domain.usecase.PlayUseCase
import app.quranaudio.playback.PlaybackConnection
import app.quranaudio.ui.navigation.ReciterRoute
import app.quranaudio.ui.navigation.SurahRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReciterUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val reciter: Reciter? = null,
    val recitations: List<Recitation> = emptyList(),
    val selected: Recitation? = null,
    val progress: Map<Int, ListeningHistoryEntity> = emptyMap(),
    val currentTrackKey: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReciterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
    private val play: PlayUseCase,
    player: PlaybackConnection,
) : ViewModel() {
    private val reciterId = savedStateHandle.toRoute<ReciterRoute>().reciterId
    private val selectedId = MutableStateFlow<String?>(null)

    private val recitationsWithSelection = combine(catalog.recitations(reciterId), selectedId) { list, sel ->
        list to (list.firstOrNull { it.id == sel } ?: list.firstOrNull())
    }

    val state: StateFlow<ReciterUiState> = combine(
        catalog.reciter(reciterId),
        recitationsWithSelection,
        recitationsWithSelection.flatMapLatest { (_, sel) -> sel?.let { library.progressFor(it.id) } ?: flowOf(emptyMap()) },
        player.state.map { it.track?.key },
    ) { reciter, (recitations, selected), progress, currentKey ->
        ReciterUiState(
            loading = false,
            notFound = reciter == null,
            reciter = reciter,
            recitations = recitations,
            selected = selected,
            progress = progress,
            currentTrackKey = currentKey,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReciterUiState())

    fun select(recitationId: String) { selectedId.value = recitationId }

    fun toggleFavourite() = viewModelScope.launch { library.toggleFavouriteReciter(reciterId) }

    fun playAll(shuffle: Boolean) = viewModelScope.launch {
        val rec = state.value.selected ?: return@launch
        play.playRecitation(reciterId, rec.id, shuffle = shuffle)
    }

    fun playSurah(surah: Int) = viewModelScope.launch {
        val rec = state.value.selected ?: return@launch
        play.playRecitation(reciterId, rec.id, startSurah = surah)
    }

    fun refsForAll(): List<TrackRef> {
        val rec = state.value.selected ?: return emptyList()
        return rec.surahs.map { TrackRef(reciterId, rec.id, it) }
    }

    suspend fun track(surah: Int): Track? {
        val rec = state.value.selected ?: return null
        return catalog.tracksFor(reciterId, rec.id, listOf(surah)).firstOrNull()
    }
}

data class SurahDetailUiState(
    val surah: Int,
    val loading: Boolean = true,
    val reciters: List<Reciter> = emptyList(),
)

@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    catalog: CatalogRepository,
    private val library: LibraryRepository,
    private val play: PlayUseCase,
) : ViewModel() {
    val surah = savedStateHandle.toRoute<SurahRoute>().surah

    /** Reciters whose catalogue includes this Surah (complete reciters first). */
    val state: StateFlow<SurahDetailUiState> = combine(catalog.reciters, catalog.allRecitationSurahs) { reciters, surahsByReciter ->
        val having = reciters.filter { r -> surahsByReciter[r.id]?.contains(surah) == true }
            .sortedWith(compareBy({ !it.isFavourite }, { !it.featured }, { it.featuredRank ?: Int.MAX_VALUE }, { it.name }))
        SurahDetailUiState(surah, loading = reciters.isEmpty(), reciters = having)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SurahDetailUiState(surah))

    fun playWith(reciterId: String) = viewModelScope.launch { play.playSurah(reciterId, surah) }
    fun toggleFavourite(reciterId: String) = viewModelScope.launch { library.toggleFavouriteReciter(reciterId) }
}
