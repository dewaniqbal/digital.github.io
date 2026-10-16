package app.quranaudio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quranaudio.data.repository.CatalogRepository
import app.quranaudio.data.repository.LibraryRepository
import app.quranaudio.domain.ListeningEntry
import app.quranaudio.domain.Reciter
import app.quranaudio.domain.SyncStatus
import app.quranaudio.domain.usecase.PlayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val status: SyncStatus = SyncStatus.Idle,
    val catalogEmpty: Boolean = true,
    val continueListening: ListeningEntry? = null,
    val recent: List<ListeningEntry> = emptyList(),
    val favourites: List<Reciter> = emptyList(),
    val featured: List<Reciter> = emptyList(),
    val newReciters: List<Reciter> = emptyList(),
    val mostListened: List<Reciter> = emptyList(),
    val recommended: List<Reciter> = emptyList(),
    val totalReciters: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    library: LibraryRepository,
    private val play: PlayUseCase,
) : ViewModel() {

    private val recent = library.recentlyPlayed(limit = 12)

    val state: StateFlow<HomeUiState> = combine(
        catalog.reciters,
        recent,
        library.mostListenedReciterIds(limit = 10),
        catalog.status,
    ) { reciters, recentEntries, mostIds, status ->
        val byId = reciters.associateBy { it.id }
        val favourites = reciters.filter { it.isFavourite }
        val mostListened = mostIds.mapNotNull { byId[it] }
        val featured = reciters.filter { it.featured }.sortedBy { it.featuredRank ?: Int.MAX_VALUE }
        val newest = reciters.filter { it.addedAtEpochMs != null }.sortedByDescending { it.addedAtEpochMs }.take(12)
        // Simple, explainable recommendation: featured complete reciters the listener has not tried yet.
        val heard = (mostIds + favourites.map { it.id }).toSet()
        val recommended = featured.filter { it.hasCompleteQuran && it.id !in heard }.take(8)
        HomeUiState(
            loading = reciters.isEmpty() && status !is SyncStatus.Failed,
            status = status,
            catalogEmpty = reciters.isEmpty(),
            continueListening = recentEntries.firstOrNull { it.durationMs == null || it.positionMs < (it.durationMs - 10_000) } ?: recentEntries.firstOrNull(),
            recent = recentEntries,
            favourites = favourites,
            featured = featured,
            newReciters = newest,
            mostListened = mostListened,
            recommended = recommended,
            totalReciters = reciters.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { catalog.refreshIfStale() }
    }

    fun retry() {
        viewModelScope.launch { catalog.refresh() }
    }

    fun continueListening(entry: ListeningEntry) {
        viewModelScope.launch { play.continueListening(entry) }
    }
}
