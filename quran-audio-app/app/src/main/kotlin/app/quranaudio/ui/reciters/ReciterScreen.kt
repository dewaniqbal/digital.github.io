package app.quranaudio.ui.reciters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quranaudio.R
import app.quranaudio.domain.Track
import app.quranaudio.shared.playback.ResumePolicy
import app.quranaudio.shared.playback.formatDuration
import app.quranaudio.shared.quran.SurahCatalog
import app.quranaudio.ui.common.AddToPlaylistSheet
import app.quranaudio.ui.common.PlayActionsRow
import app.quranaudio.ui.common.TrackActionsSheet
import app.quranaudio.ui.components.AppTopBar
import app.quranaudio.ui.components.Chip
import app.quranaudio.ui.components.EmptyState
import app.quranaudio.ui.components.FavouriteButton
import app.quranaudio.ui.components.GradientBackground
import app.quranaudio.ui.components.LoadingSkeleton
import app.quranaudio.ui.components.ReciterArtwork
import app.quranaudio.ui.components.ReciterRow
import app.quranaudio.ui.components.SectionHeader
import app.quranaudio.ui.components.SurahBadge
import app.quranaudio.ui.components.SurahRow
import app.quranaudio.ui.theme.ArabicNameStyle
import app.quranaudio.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ReciterScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: ReciterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuFor by remember { mutableStateOf<Track?>(null) }
    var addAll by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        GradientBackground(height = 420.dp)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item { AppTopBar(title = "", onBack = onBack) }
            val reciter = state.reciter
            when {
                state.loading -> item { LoadingSkeleton(rows = 5) }
                state.notFound || reciter == null -> item {
                    EmptyState(title = stringResource(R.string.reciter_not_found), icon = Icons.Filled.PersonOff)
                }
                else -> {
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.screen), horizontalAlignment = Alignment.CenterHorizontally) {
                            ReciterArtwork(reciter.id, reciter.name, 168.dp)
                            Spacer(Modifier.height(Spacing.md))
                            Text(reciter.name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                            reciter.nameArabic?.let { Text(it, style = ArabicNameStyle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Spacer(Modifier.height(Spacing.xs))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
                                itemVerticalAlignment = Alignment.CenterVertically,
                            ) {
                                reciter.styles.forEach { Chip(styleLabel(it)) }
                                reciter.country?.let { Chip(it) }
                                Chip(
                                    if (reciter.hasCompleteQuran) stringResource(R.string.reciter_complete_quran)
                                    else pluralStringResource(R.plurals.surah_count, reciter.surahCount, reciter.surahCount),
                                )
                                FavouriteButton(reciter.isFavourite, onToggle = viewModel::toggleFavourite)
                            }
                        }
                    }
                    if (state.recitations.size > 1) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.xs),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                items(state.recitations, key = { it.id }) { r ->
                                    FilterChip(
                                        selected = r.id == state.selected?.id,
                                        onClick = { viewModel.select(r.id) },
                                        label = { Text(r.title) },
                                    )
                                }
                            }
                        }
                    }
                    item {
                        PlayActionsRow(
                            onPlayAll = { viewModel.playAll(false) },
                            onShuffle = { viewModel.playAll(true) },
                            onAddToPlaylist = { addAll = true },
                            enabled = state.selected != null,
                        )
                    }
                    val selected = state.selected
                    if (selected != null) {
                        item {
                            SectionHeader(stringResource(R.string.reciter_available_surahs) + " (${selected.surahs.size})")
                        }
                        items(selected.surahs, key = { "${selected.id}#$it" }) { number ->
                            val surah = SurahCatalog.get(number)
                            val progress = state.progress[number]
                            val subtitle = progress?.durationMs?.let { d ->
                                if (progress.positionMs > 0 && !progress.completed) "${formatDuration(progress.positionMs)} / ${formatDuration(d)}" else formatDuration(d)
                            }
                            SurahRow(
                                surah = surah,
                                subtitle = subtitle,
                                progress = progress?.takeIf { !it.completed }?.let { ResumePolicy.progress(it.positionMs, it.durationMs) },
                                isCurrent = state.currentTrackKey == "${selected.id}#$number",
                                onPlay = { viewModel.playSurah(number) },
                                onMore = { scope.launch { menuFor = viewModel.track(number) } },
                            )
                        }
                    }
                }
            }
        }
    }
    menuFor?.let { TrackActionsSheet(it, onDismiss = { menuFor = null }) }
    if (addAll) AddToPlaylistSheet(viewModel.refsForAll(), onDismiss = { addAll = false })
}

@Composable
fun SurahDetailScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: SurahDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val surah = SurahCatalog.get(viewModel.surah)
    Box(Modifier.fillMaxSize()) {
        GradientBackground()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item { AppTopBar(title = "", onBack = onBack) }
            item {
                Column(Modifier.fillMaxWidth().padding(Spacing.screen), horizontalAlignment = Alignment.CenterHorizontally) {
                    SurahBadge(surah.number, size = 72.dp)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(surah.nameArabic, style = ArabicNameStyle.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize, lineHeight = MaterialTheme.typography.headlineLarge.lineHeight * 1.4f))
                    Text(stringResource(R.string.surah_title, surah.nameTransliterated), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    Text(
                        pluralStringResource(R.plurals.verse_count, surah.verseCount, surah.verseCount) + " · " +
                            stringResource(if (surah.revelationPlace == app.quranaudio.shared.quran.RevelationPlace.MAKKAH) R.string.surah_meccan else R.string.surah_medinan),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { SectionHeader(stringResource(R.string.surah_choose_reciter) + " (${state.reciters.size})") }
            if (state.loading) item { LoadingSkeleton(rows = 4) }
            items(state.reciters, key = { it.id }) { r ->
                ReciterRow(r, onClick = { viewModel.playWith(r.id) }, onToggleFavourite = { viewModel.toggleFavourite(r.id) })
            }
        }
    }
}
