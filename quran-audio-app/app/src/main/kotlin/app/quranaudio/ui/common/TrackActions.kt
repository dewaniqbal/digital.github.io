package app.quranaudio.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.quranaudio.R
import app.quranaudio.data.repository.LibraryRepository
import app.quranaudio.domain.Playlist
import app.quranaudio.domain.Track
import app.quranaudio.domain.TrackRef
import app.quranaudio.playback.PlaybackConnection
import app.quranaudio.ui.library.PlaylistNameDialog
import app.quranaudio.ui.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

fun Track.ref() = TrackRef(reciterId, recitationId, surahNumber)

@HiltViewModel
class TrackActionsViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val player: PlaybackConnection,
) : ViewModel() {

    val favouriteKeys: StateFlow<Set<String>> =
        library.favouriteTrackKeys.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playlists: StateFlow<List<Playlist>> =
        library.playlistSummaries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playNext(tracks: List<Track>) = viewModelScope.launch { player.addToQueue(tracks, playNext = true) }
    fun addToQueue(tracks: List<Track>) = viewModelScope.launch { player.addToQueue(tracks) }
    fun toggleFavourite(track: Track) = viewModelScope.launch { library.toggleFavouriteTrack(track.ref()) }
    fun toggleFavourite(ref: TrackRef) = viewModelScope.launch { library.toggleFavouriteTrack(ref) }
    fun addToPlaylist(playlistId: Long, refs: List<TrackRef>) = viewModelScope.launch { library.addToPlaylist(playlistId, refs) }
    fun createPlaylistAndAdd(name: String, refs: List<TrackRef>) = viewModelScope.launch {
        val id = library.createPlaylist(name)
        library.addToPlaylist(id, refs)
    }
}

/** Optional extra actions for a track row, depending on the screen. */
data class ExtraTrackActions(
    val onRemove: (() -> Unit)? = null,
    val onMoveUp: (() -> Unit)? = null,
    val onMoveDown: (() -> Unit)? = null,
    val onOpenReciter: (() -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionsSheet(
    track: Track,
    onDismiss: () -> Unit,
    extra: ExtraTrackActions = ExtraTrackActions(),
    viewModel: TrackActionsViewModel = hiltViewModel(),
) {
    val favourites by viewModel.favouriteKeys.collectAsStateWithLifecycle()
    var showPlaylists by remember { mutableStateOf(false) }
    if (showPlaylists) {
        AddToPlaylistSheet(refs = listOf(track.ref()), onDismiss = { showPlaylists = false; onDismiss() }, viewModel = viewModel)
        return
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = Spacing.md)) {
            Text(
                stringResource(R.string.surah_title, track.surah.nameTransliterated) + " · " + track.reciterName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm),
            )
            val isFav = track.key in favourites
            SheetAction(if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, stringResource(if (isFav) R.string.action_remove_favourite else R.string.action_add_favourite)) {
                viewModel.toggleFavourite(track); onDismiss()
            }
            SheetAction(Icons.AutoMirrored.Filled.PlaylistPlay, stringResource(R.string.action_play_next)) { viewModel.playNext(listOf(track)); onDismiss() }
            SheetAction(Icons.AutoMirrored.Filled.QueueMusic, stringResource(R.string.action_add_to_queue)) { viewModel.addToQueue(listOf(track)); onDismiss() }
            SheetAction(Icons.AutoMirrored.Filled.PlaylistAdd, stringResource(R.string.action_add_to_playlist)) { showPlaylists = true }
            extra.onOpenReciter?.let { SheetAction(Icons.Filled.Person, stringResource(R.string.action_go_to_reciter)) { it(); onDismiss() } }
            extra.onMoveUp?.let { SheetAction(Icons.Filled.ArrowUpward, stringResource(R.string.action_move_up)) { it(); onDismiss() } }
            extra.onMoveDown?.let { SheetAction(Icons.Filled.ArrowDownward, stringResource(R.string.action_move_down)) { it(); onDismiss() } }
            extra.onRemove?.let { SheetAction(Icons.Filled.Delete, stringResource(R.string.action_remove)) { it(); onDismiss() } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    refs: List<TrackRef>,
    onDismiss: () -> Unit,
    viewModel: TrackActionsViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_create),
            initial = "",
            onDismiss = { creating = false },
            onConfirm = { name -> viewModel.createPlaylistAndAdd(name, refs); creating = false; onDismiss() },
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.action_add_to_playlist),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm),
        )
        LazyColumn(Modifier.navigationBarsPadding().padding(bottom = Spacing.md)) {
            item { SheetAction(Icons.Filled.Add, stringResource(R.string.playlist_create)) { creating = true } }
            items(playlists, key = { it.id }) { p ->
                SheetAction(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    p.name,
                    subtitle = pluralStringResource(R.plurals.surah_count, p.trackCount, p.trackCount),
                ) {
                    viewModel.addToPlaylist(p.id, refs); onDismiss()
                }
            }
        }
    }
}

@Composable
fun SheetAction(icon: ImageVector, label: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.screen, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(Spacing.md))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
