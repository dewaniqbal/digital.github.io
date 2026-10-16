package app.quranaudio.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quranaudio.data.repository.CatalogRepository
import app.quranaudio.data.repository.LibraryRepository
import app.quranaudio.domain.Playlist
import app.quranaudio.domain.Reciter
import app.quranaudio.shared.quran.Surah
import app.quranaudio.shared.quran.SurahCatalog
import app.quranaudio.shared.search.SearchNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val searching: Boolean = false,
    val reciters: List<Reciter> = emptyList(),
    val surahs: List<Surah> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
) {
    val isEmptyResult: Boolean get() = query.isNotBlank() && !searching && reciters.isEmpty() && surahs.isEmpty() && playlists.isEmpty()
}

/**
 * Global search across reciters, Surahs and playlists. Debounced, runs off the main thread on
 * pre-normalised text, so typing never blocks the UI.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    catalog: CatalogRepository,
    library: LibraryRepository,
) : ViewModel() {

    private val rawQuery = MutableStateFlow("")

    private val surahIndex: List<Pair<Surah, String>> = SurahCatalog.all.map { s ->
        s to SearchNormalizer.normalize("${s.number} ${s.nameTransliterated} ${s.nameArabic}")
    }

    private val reciterIndex = catalog.reciters.map { list ->
        list.map { r -> r to SearchNormalizer.normalize("${r.name} ${r.nameArabic.orEmpty()}") }
    }.flowOn(Dispatchers.Default)

    private val results = combine(rawQuery.debounce(250).distinctUntilChanged(), reciterIndex, library.playlistSummaries) { q, reciters, playlists ->
        val nq = SearchNormalizer.normalize(q)
        if (nq.isBlank()) return@combine SearchUiState(query = q)
        val number = q.trim().toIntOrNull()
        val surahs = if (number != null) listOfNotNull(SurahCatalog.getOrNull(number)) else
            surahIndex.map { (s, key) -> s to SearchNormalizer.score(nq, key) }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
        SearchUiState(
            query = q,
            reciters = reciters.map { (r, key) -> r to SearchNormalizer.score(nq, key) }.filter { it.second > 0 }
                .sortedByDescending { it.second }.map { it.first }.take(50),
            surahs = surahs.take(30),
            playlists = playlists.filter { SearchNormalizer.score(nq, SearchNormalizer.normalize(it.name)) > 0 },
        )
    }.flowOn(Dispatchers.Default)

    val state: StateFlow<SearchUiState> = combine(rawQuery, results) { q, r ->
        if (q == r.query) r else r.copy(query = q, searching = q.isNotBlank())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(q: String) { rawQuery.value = q }
}
