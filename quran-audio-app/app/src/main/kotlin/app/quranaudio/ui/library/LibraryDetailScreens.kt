package app.quranaudio.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quranaudio.R
import app.quranaudio.domain.PlaylistItem
import app.quranaudio.domain.Track
import app.quranaudio.shared.playback.ResumePolicy
import app.quranaudio.shared.playback.formatDuration
import app.quranaudio.shared.quran.SurahCatalog
import app.quranaudio.ui.common.AddToPlaylistSheet
import app.quranaudio.ui.common.ExtraTrackActions
import app.quranaudio.ui.common.PlayActionsRow
import app.quranaudio.ui.common.TrackActionsSheet
import app.quranaudio.ui.common.ref
import app.quranaudio.ui.components.AppTopBar
import app.quranaudio.ui.components.EmptyState
import app.quranaudio.ui.components.GradientBackground
import app.quranaudio.ui.components.LoadingSkeleton
import app.quranaudio.ui.components.ReciterCard
import app.quranaudio.ui.components.SectionHeader
import app.quranaudio.ui.components.SurahRow
import app.quranaudio.ui.theme.Spacing

@Composable
fun PlaylistDetailScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenReciter: (String) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuFor by remember { mutableStateOf<PlaylistItem?>(null) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        GradientBackground()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item {
                AppTopBar(
                    title = state.name,
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = { renaming = true }) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.playlist_rename)) }
                        IconButton(onClick = { deleting = true }) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.playlist_delete)) }
                    },
                )
            }
            when {
                state.loading -> item { LoadingSkeleton(rows = 4) }
                state.items.isEmpty() -> item {
                    EmptyState(
                        title = stringResource(R.string.playlist_empty_title),
                        message = stringResource(R.string.playlist_empty_message),
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                    )
                }
                else -> {
                    item { PlayActionsRow(onPlayAll = { viewModel.playAll(false) }, onShuffle = { viewModel.playAll(true) }, enabled = state.playable.isNotEmpty()) }
                    itemsIndexed(state.items, key = { _, it -> it.entryId }) { _, item ->
                        val surah = SurahCatalog.get(item.ref.surahNumber)
                        SurahRow(
                            surah = surah,
                            subtitle = item.track?.reciterName ?: stringResource(R.string.track_unavailable),
                            onPlay = { if (item.track != null) viewModel.playFrom(item) },
                            onMore = { menuFor = item },
                        )
                    }
                }
            }
        }
    }

    menuFor?.let { item ->
        val track = item.track
        val idx = state.items.indexOf(item)
        val extra = ExtraTrackActions(
            onRemove = { viewModel.remove(item) },
            onMoveUp = if (idx > 0) ({ viewModel.move(item, -1) }) else null,
            onMoveDown = if (idx < state.items.lastIndex) ({ viewModel.move(item, +1) }) else null,
            onOpenReciter = track?.let { t -> { onOpenReciter(t.reciterId) } },
        )
        if (track != null) {
            TrackActionsSheet(track = track, onDismiss = { menuFor = null }, extra = extra)
        } else {
            ConfirmDialog(
                title = stringResource(R.string.track_unavailable),
                message = stringResource(R.string.track_unavailable_message),
                confirm = stringResource(R.string.action_remove),
                onDismiss = { menuFor = null },
                onConfirm = { viewModel.remove(item); menuFor = null },
            )
        }
    }
    if (renaming) {
        PlaylistNameDialog(stringResource(R.string.playlist_rename), state.name, onDismiss = { renaming = false }) { renaming = false; viewModel.rename(it) }
    }
    if (deleting) {
        ConfirmDialog(
            title = stringResource(R.string.playlist_delete),
            message = stringResource(R.string.playlist_delete_message, state.name),
            confirm = stringResource(R.string.action_delete),
            onDismiss = { deleting = false },
            onConfirm = { deleting = false; viewModel.delete(); onBack() },
        )
    }
}

@Composable
fun FavouritesScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenReciter: (String) -> Unit,
    viewModel: FavouritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reciters by viewModel.favouriteReciters.collectAsStateWithLifecycle()
    var menuFor by remember { mutableStateOf<Track?>(null) }
    var addAll by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        GradientBackground()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item { AppTopBar(stringResource(R.string.library_favourites), onBack = onBack) }
            if (reciters.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.favourites_reciters)) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = Spacing.screen), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        items(reciters, key = { it.id }) { r -> ReciterCard(r, onClick = { onOpenReciter(r.id) }) }
                    }
                }
                item { SectionHeader(stringResource(R.string.favourites_surahs)) }
            }
            when {
                state.loading -> item { LoadingSkeleton(rows = 4) }
                state.tracks.isEmpty() -> item {
                    EmptyState(
                        title = stringResource(R.string.favourites_empty_title),
                        message = stringResource(R.string.favourites_empty_message),
                        icon = Icons.Filled.FavoriteBorder,
                    )
                }
                else -> {
                    item {
                        PlayActionsRow(
                            onPlayAll = { viewModel.play(0) },
                            onShuffle = { viewModel.play(0, shuffle = true) },
                            onAddToPlaylist = { addAll = true },
                        )
                    }
                    itemsIndexed(state.tracks, key = { _, t -> t.key }) { index, t ->
                        SurahRow(t.surah, subtitle = t.reciterName, onPlay = { viewModel.play(index) }, onMore = { menuFor = t })
                    }
                }
            }
        }
    }
    menuFor?.let { t -> TrackActionsSheet(t, onDismiss = { menuFor = null }, extra = ExtraTrackActions(onOpenReciter = { onOpenReciter(t.reciterId) })) }
    if (addAll) AddToPlaylistSheet(refs = state.tracks.map { it.ref() }, onDismiss = { addAll = false })
}

@Composable
fun RecentScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenReciter: (String) -> Unit,
    viewModel: RecentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuFor by remember { mutableStateOf<Track?>(null) }
    var clearing by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        GradientBackground()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item {
                AppTopBar(stringResource(R.string.library_recently_played), onBack = onBack, actions = {
                    if (state.entries.isNotEmpty()) {
                        IconButton(onClick = { clearing = true }) { Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.history_clear)) }
                    }
                })
            }
            when {
                state.loading -> item { LoadingSkeleton(rows = 4) }
                state.entries.isEmpty() -> item {
                    EmptyState(title = stringResource(R.string.recent_empty_title), message = stringResource(R.string.recent_empty_message), icon = Icons.Filled.History)
                }
                else -> items(state.entries, key = { it.track.key }) { e ->
                    val subtitle = buildString {
                        append(e.track.reciterName)
                        if (e.positionMs > 0) append(" · ").append(formatDuration(e.positionMs))
                        e.durationMs?.let { append(" / ").append(formatDuration(it)) }
                    }
                    SurahRow(
                        e.track.surah,
                        subtitle = subtitle,
                        progress = ResumePolicy.progress(e.positionMs, e.durationMs),
                        onPlay = { viewModel.resume(e) },
                        onMore = { menuFor = e.track },
                    )
                }
            }
        }
    }
    menuFor?.let { t -> TrackActionsSheet(t, onDismiss = { menuFor = null }, extra = ExtraTrackActions(onOpenReciter = { onOpenReciter(t.reciterId) })) }
    if (clearing) {
        ConfirmDialog(
            title = stringResource(R.string.history_clear),
            message = stringResource(R.string.history_clear_message),
            confirm = stringResource(R.string.action_clear),
            onDismiss = { clearing = false },
            onConfirm = { clearing = false; viewModel.clear() },
        )
    }
}

@Composable
fun CollectionScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val style = collectionStyle(state.collection)
    var pickerOpen by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<Track?>(null) }
    Box(Modifier.fillMaxSize()) {
        GradientBackground()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item { AppTopBar(stringResource(style.title), onBack = onBack) }
            item {
                Column(Modifier.padding(horizontal = Spacing.screen)) {
                    Text(stringResource(style.description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box {
                        TextButton(onClick = { pickerOpen = true }, enabled = state.reciters.isNotEmpty()) {
                            Text(stringResource(R.string.collection_reciter, state.reciter?.name ?: "—"))
                            Icon(Icons.Filled.ExpandMore, contentDescription = null)
                        }
                        DropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                            state.reciters.forEach { r ->
                                DropdownMenuItem(text = { Text(r.name) }, onClick = { viewModel.selectReciter(r.id); pickerOpen = false })
                            }
                        }
                    }
                }
            }
            item { PlayActionsRow(onPlayAll = { viewModel.play(0) }, onShuffle = { viewModel.play(0, shuffle = true) }, enabled = state.tracks.isNotEmpty()) }
            if (state.loading) item { LoadingSkeleton(rows = 4) }
            itemsIndexed(state.collection.surahs, key = { _, s -> s }) { index, number ->
                val track = state.tracks[number]
                SurahRow(
                    SurahCatalog.get(number),
                    subtitle = if (track == null && !state.loading) stringResource(R.string.track_not_by_reciter) else null,
                    onPlay = { if (track != null) viewModel.play(index) },
                    onMore = track?.let { t -> { menuFor = t } },
                )
            }
            item {
                Text(
                    stringResource(R.string.collection_editorial_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.screen),
                )
            }
        }
    }
    menuFor?.let { t -> TrackActionsSheet(t, onDismiss = { menuFor = null }) }
}
