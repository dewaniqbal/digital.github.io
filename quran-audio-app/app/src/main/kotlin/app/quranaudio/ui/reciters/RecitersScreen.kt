package app.quranaudio.ui.reciters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quranaudio.R
import app.quranaudio.ads.AdBanner
import app.quranaudio.domain.SyncStatus
import app.quranaudio.shared.catalog.RecitationStyle
import app.quranaudio.shared.quran.SurahCatalog
import app.quranaudio.ui.components.AppTopBar
import app.quranaudio.ui.components.EmptyState
import app.quranaudio.ui.components.ErrorState
import app.quranaudio.ui.components.GradientBackground
import app.quranaudio.ui.components.LoadingSkeleton
import app.quranaudio.ui.components.ReciterRow
import app.quranaudio.ui.components.SearchBar
import app.quranaudio.ui.components.SurahRow
import app.quranaudio.ui.theme.Spacing

@Composable
fun RecitersScreen(
    contentPadding: PaddingValues,
    onOpenReciter: (String) -> Unit,
    onOpenSurah: (Int) -> Unit,
    viewModel: RecitersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        GradientBackground()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item { AppTopBar(stringResource(R.string.nav_reciters)) }
            item {
                PrimaryTabRow(selectedTabIndex = tab, containerColor = androidx.compose.ui.graphics.Color.Transparent) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_reciters)) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_surahs)) })
                }
            }
            if (tab == 1) {
                items(SurahCatalog.all, key = { "s${it.number}" }) { s -> SurahRow(s, onPlay = { onOpenSurah(s.number) }) }
                return@LazyColumn
            }
            item {
                Column(Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm)) {
                    SearchBar(state.query, viewModel::setQuery, placeholder = stringResource(R.string.reciters_search_placeholder))
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = Spacing.screen), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(state.filters, key = { it.toString() }) { f ->
                        FilterChip(selected = f == state.filter, onClick = { viewModel.setFilter(f) }, label = { Text(filterLabel(f)) })
                    }
                }
            }
            when {
                state.loading -> item { LoadingSkeleton() }
                state.total == 0 && state.status is SyncStatus.Failed -> item {
                    ErrorState(stringResource(R.string.error_load_reciters), onRetry = viewModel::retry, offline = (state.status as SyncStatus.Failed).offline)
                }
                state.reciters.isEmpty() -> item {
                    EmptyState(title = stringResource(R.string.reciters_empty), icon = Icons.Filled.PersonSearch)
                }
                else -> {
                    item {
                        Text(
                            pluralStringResource(R.plurals.reciter_count, state.reciters.size, state.reciters.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs),
                        )
                    }
                    items(state.reciters, key = { it.id }) { r ->
                        ReciterRow(r, onClick = { onOpenReciter(r.id) }, onToggleFavourite = { viewModel.toggleFavourite(r.id) })
                    }
                    item { AdBanner(Modifier.padding(vertical = Spacing.md)) }
                }
            }
        }
    }
}

@Composable
fun filterLabel(f: ReciterFilter): String = when (f) {
    ReciterFilter.All -> stringResource(R.string.filter_all)
    ReciterFilter.Favourites -> stringResource(R.string.filter_favourites)
    ReciterFilter.Featured -> stringResource(R.string.filter_featured)
    ReciterFilter.MostListened -> stringResource(R.string.filter_most_listened)
    ReciterFilter.New -> stringResource(R.string.filter_new)
    ReciterFilter.CompleteQuran -> stringResource(R.string.filter_complete)
    is ReciterFilter.Style -> styleLabel(f.style)
    is ReciterFilter.Country -> f.country
}

@Composable
fun styleLabel(style: RecitationStyle): String = stringResource(
    when (style) {
        RecitationStyle.MURATTAL -> R.string.style_murattal
        RecitationStyle.MUJAWWAD -> R.string.style_mujawwad
        RecitationStyle.MUALLIM -> R.string.style_muallim
        RecitationStyle.OTHER -> R.string.style_other
    },
)
