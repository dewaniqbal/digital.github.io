package app.quranaudio.ui.reciters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quranaudio.data.repository.CatalogRepository
import app.quranaudio.data.repository.LibraryRepository
import app.quranaudio.domain.Reciter
import app.quranaudio.domain.SyncStatus
import app.quranaudio.shared.catalog.RecitationStyle
import app.quranaudio.shared.search.SearchNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/** Filters derived from data that actually exists in the catalogue (nothing is guessed). */
sealed interface ReciterFilter {
    data object All : ReciterFilter
    data object Favourites : ReciterFilter
    data object Featured : ReciterFilter
    data object MostListened : ReciterFilter
    data object New : ReciterFilter
    data object CompleteQuran : ReciterFilter
    data class Style(val style: RecitationStyle) : ReciterFilter
    data class Country(val country: String) : ReciterFilter
}

data class RecitersUiState(
    val loading: Boolean = true,
    val status: SyncStatus = SyncStatus.Idle,
    val query: String = "",
    val filter: ReciterFilter = ReciterFilter.All,
    val filters: List<ReciterFilter> = listOf(ReciterFilter.All),
    val reciters: List<Reciter> = emptyList(),
    val total: Int = 0,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class RecitersViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<ReciterFilter>(ReciterFilter.All)
    private val searchKeys = HashMap<String, String>()

    val state: StateFlow<RecitersUiState> = combine(
        catalog.reciters,
        query.debounce(200),
        filter,
        library.mostListenedReciterIds(limit = 30),
        catalog.status,
    ) { reciters, q, f, mostIds, status ->
        val normalizedQuery = SearchNormalizer.normalize(q)
        val countries = reciters.mapNotNull { it.country }.distinct().sorted()
        val styles = reciters.flatMap { it.styles }.toSet()
        val available = buildList {
            add(ReciterFilter.All)
            add(ReciterFilter.Favourites)
            add(ReciterFilter.Featured)
            if (mostIds.isNotEmpty()) add(ReciterFilter.MostListened)
            add(ReciterFilter.New)
            add(ReciterFilter.CompleteQuran)
            listOf(RecitationStyle.MURATTAL, RecitationStyle.MUJAWWAD, RecitationStyle.MUALLIM).filter { it in styles }.forEach { add(ReciterFilter.Style(it)) }
            countries.forEach { add(ReciterFilter.Country(it)) }
        }
        val filtered = when (f) {
            ReciterFilter.All -> reciters
            ReciterFilter.Favourites -> reciters.filter { it.isFavourite }
            ReciterFilter.Featured -> reciters.filter { it.featured }.sortedBy { it.featuredRank ?: Int.MAX_VALUE }
            ReciterFilter.MostListened -> mostIds.mapNotNull { id -> reciters.firstOrNull { it.id == id } }
            ReciterFilter.New -> reciters.filter { it.addedAtEpochMs != null }.sortedByDescending { it.addedAtEpochMs }.take(30)
            ReciterFilter.CompleteQuran -> reciters.filter { it.hasCompleteQuran }
            is ReciterFilter.Style -> reciters.filter { f.style in it.styles }
            is ReciterFilter.Country -> reciters.filter { it.country == f.country }
        }
        val searched = if (normalizedQuery.isEmpty()) filtered else filtered
            .map { r -> r to SearchNormalizer.score(normalizedQuery, searchKeys.getOrPut(r.id) { SearchNormalizer.normalize("${r.name} ${r.nameArabic.orEmpty()}") }) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
        RecitersUiState(
            loading = reciters.isEmpty() && status !is SyncStatus.Failed,
            status = status,
            query = q,
            filter = f,
            filters = available,
            reciters = searched,
            total = reciters.size,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecitersUiState())

    init {
        viewModelScope.launch { catalog.refreshIfStale() }
    }

    fun setQuery(q: String) { query.value = q }
    fun setFilter(f: ReciterFilter) { filter.value = f }
    fun retry() { viewModelScope.launch { catalog.refresh() } }
    fun toggleFavourite(id: String) { viewModelScope.launch { library.toggleFavouriteReciter(id) } }
}
