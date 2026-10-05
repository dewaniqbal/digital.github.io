package app.quranaudio.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import app.quranaudio.R
import app.quranaudio.ads.AdBanner
import app.quranaudio.ui.components.AppTopBar
import app.quranaudio.ui.components.EmptyState
import app.quranaudio.ui.components.GradientBackground
import app.quranaudio.ui.components.ReciterArtwork
import app.quranaudio.ui.components.SearchBar
import app.quranaudio.ui.components.SectionHeader
import app.quranaudio.ui.components.SurahRow
import app.quranaudio.ui.theme.Spacing
import androidx.compose.ui.unit.dp

@Composable
fun SearchScreen(
    contentPadding: PaddingValues,
    onOpenReciter: (String) -> Unit,
    onOpenSurah: (Int) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(Modifier.fillMaxSize()) {
        GradientBackground()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item { AppTopBar(stringResource(R.string.nav_search)) }
            item {
                SearchBar(
                    state.query,
                    viewModel::setQuery,
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm).focusRequester(focus),
                )
            }
            when {
                state.query.isBlank() -> item {
                    EmptyState(title = stringResource(R.string.search_hint_title), message = stringResource(R.string.search_hint_message), icon = Icons.Filled.Search)
                }
                state.isEmptyResult -> item {
                    EmptyState(title = stringResource(R.string.search_no_results), message = stringResource(R.string.search_no_results_message), icon = Icons.Filled.SearchOff)
                }
                else -> {
                    if (state.reciters.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.search_section_reciters)) }
                        items(state.reciters, key = { "r${it.id}" }) { r ->
                            ListItem(
                                headlineContent = { Text(r.name) },
                                supportingContent = { Text(r.nameArabic ?: pluralStringResource(R.plurals.surah_count, r.surahCount, r.surahCount)) },
                                leadingContent = { ReciterArtwork(r.id, r.name, 48.dp) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onOpenReciter(r.id) },
                            )
                        }
                    }
                    if (state.surahs.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.search_section_surahs)) }
                        items(state.surahs, key = { "s${it.number}" }) { s -> SurahRow(s, onPlay = { onOpenSurah(s.number) }) }
                    }
                    if (state.playlists.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.search_section_playlists)) }
                        items(state.playlists, key = { "p${it.id}" }) { p ->
                            ListItem(
                                headlineContent = { Text(p.name) },
                                supportingContent = { Text(pluralStringResource(R.plurals.surah_count, p.trackCount, p.trackCount)) },
                                leadingContent = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onOpenPlaylist(p.id) },
                            )
                        }
                    }
                    item { AdBanner(Modifier.padding(vertical = Spacing.md)) }
                }
            }
        }
    }
}
