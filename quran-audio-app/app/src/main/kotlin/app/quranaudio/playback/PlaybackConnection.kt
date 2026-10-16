package app.quranaudio.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.quranaudio.data.prefs.UserPreferences
import app.quranaudio.data.repository.ConnectivityMonitor
import app.quranaudio.data.repository.LibraryRepository
import app.quranaudio.di.ApplicationScope
import app.quranaudio.domain.Track
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class QueueEntry(val index: Int, val track: Track)

data class PlayerUiState(
    val connected: Boolean = false,
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    /** Play requested (playing, buffering or about to play). Used to suppress interstitial ads. */
    val playWhenReady: Boolean = false,
    val hasError: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffle: Boolean = false,
    val speed: Float = 1f,
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val durationMs: Long? = null,
) {
    val isActive: Boolean get() = playWhenReady || isPlaying || isBuffering
    val upNext: List<QueueEntry> get() = queue.drop(currentIndex + 1)
}

data class PlaybackProgress(val positionMs: Long, val bufferedMs: Long, val durationMs: Long?)

/**
 * The UI's handle on playback. Wraps a [MediaController] connected to [PlaybackService] and turns
 * player callbacks into a [StateFlow]. All player calls happen on the main thread.
 */
@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
    private val library: LibraryRepository,
    private val prefs: UserPreferences,
    private val connectivity: ConnectivityMonitor,
    private val artwork: ArtworkCache,
    private val events: PlaybackEvents,
) {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controller: MediaController? = null

    /** Connects lazily; safe to call repeatedly. */
    suspend fun ensureConnected(): MediaController = withContext(Dispatchers.Main) {
        controller?.takeIf { it.isConnected } ?: connect()
    }

    fun connectInBackground() {
        appScope.launch(Dispatchers.Main) { runCatching { ensureConnected() } }
    }

    private suspend fun connect(): MediaController {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        val c = suspendCancellableCoroutine { cont ->
            future.addListener({
                runCatching { future.get() }.onSuccess { cont.resume(it) }.onFailure { cont.resumeWithException(it) }
            }, MoreExecutors.directExecutor())
            cont.invokeOnCancellation { MediaController.releaseFuture(future) }
        }
        c.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = publish(player)
        })
        controller = c
        publish(c)
        return c
    }

    private fun publish(p: Player) {
        val queue = (0 until p.mediaItemCount).mapNotNull { i -> MediaItems.toTrack(p.getMediaItemAt(i))?.let { QueueEntry(i, it) } }
        _state.value = PlayerUiState(
            connected = true,
            track = MediaItems.toTrack(p.currentMediaItem),
            isPlaying = p.isPlaying,
            isBuffering = p.playbackState == Player.STATE_BUFFERING,
            playWhenReady = p.playWhenReady,
            hasError = p.playerError != null,
            repeatMode = p.repeatMode,
            shuffle = p.shuffleModeEnabled,
            speed = p.playbackParameters.speed,
            queue = queue,
            currentIndex = p.currentMediaItemIndex,
            hasNext = p.hasNextMediaItem(),
            hasPrevious = p.hasPreviousMediaItem() || p.currentPosition > 3_000,
            durationMs = p.duration.takeIf { it != C.TIME_UNSET && it > 0 },
        )
    }

    /**
     * Position updates for the progress bar. Cold: it only ticks while a screen collects it,
     * so there is no polling when the UI is not visible.
     */
    val progress: Flow<PlaybackProgress> = flow {
        val c = ensureConnected()
        while (true) {
            emit(PlaybackProgress(c.currentPosition, c.bufferedPosition, c.duration.takeIf { it != C.TIME_UNSET && it > 0 }))
            delay(if (c.isPlaying) 500 else 1_000)
        }
    }.retryWhen { _, attempt -> delay(1_000L * (attempt + 1).coerceAtMost(5)); true }
        .flowOn(Dispatchers.Main).distinctUntilChanged()

    // ---- Commands --------------------------------------------------------------------------

    /**
     * Plays [tracks] starting at [startIndex]. Resumes the start track from its saved position
     * unless [startPositionMs] is given. Returns false when playback was refused (e.g. Wi-Fi only).
     */
    suspend fun play(tracks: List<Track>, startIndex: Int = 0, startPositionMs: Long? = null, shuffle: Boolean? = null): Boolean {
        if (tracks.isEmpty()) return false
        if (!networkAllowsStreaming()) return false
        val index = startIndex.coerceIn(0, tracks.lastIndex)
        val position = startPositionMs ?: library.savedPositionMs(tracks[index])
        val items = buildItems(tracks)
        val c = runCatching { ensureConnected() }.getOrElse {
            events.emit(PlaybackMessage.TRACK_UNAVAILABLE)
            return false
        }
        withContext(Dispatchers.Main) {
            if (shuffle != null) c.shuffleModeEnabled = shuffle
            c.setMediaItems(items, index, position)
            c.prepare()
            c.play()
        }
        prefs.setLastReciter(tracks[index].reciterId)
        return true
    }

    suspend fun addToQueue(tracks: List<Track>, playNext: Boolean = false) {
        if (tracks.isEmpty()) return
        val c = runCatching { ensureConnected() }.getOrNull() ?: return
        if (c.mediaItemCount == 0) {
            play(tracks)
            return
        }
        val items = buildItems(tracks)
        withContext(Dispatchers.Main) {
            if (playNext) c.addMediaItems(c.currentMediaItemIndex + 1, items) else c.addMediaItems(items)
        }
    }

    private suspend fun buildItems(tracks: List<Track>): List<MediaItem> {
        val art = tracks.map { it.reciterId to it.reciterName }.distinct()
            .associate { (id, name) -> id to artwork.uriFor(id, name) }
        return tracks.map { t ->
            val item = MediaItems.toMediaItem(t)
            val uri = art[t.reciterId] ?: return@map item
            item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setArtworkUri(uri).build()).build()
        }
    }

    private suspend fun networkAllowsStreaming(): Boolean {
        val net = connectivity.current()
        if (!net.online) {
            events.emit(PlaybackMessage.OFFLINE)
            return false
        }
        if (prefs.current().wifiOnly && !net.unmetered) {
            events.emit(PlaybackMessage.WIFI_ONLY_BLOCKED)
            return false
        }
        return true
    }

    private fun command(block: (MediaController) -> Unit) {
        appScope.launch(Dispatchers.Main) { runCatching { block(ensureConnected()) } }
    }

    fun togglePlayPause() = command { c ->
        when {
            c.playerError != null -> { c.prepare(); c.play() }
            c.isPlaying || (c.playWhenReady && c.playbackState == Player.STATE_BUFFERING) -> c.pause()
            c.playbackState == Player.STATE_ENDED -> { c.seekToDefaultPosition(0); c.play() }
            else -> { if (c.playbackState == Player.STATE_IDLE) c.prepare(); c.play() }
        }
    }

    fun pause() = command { it.pause() }
    fun next() = command { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    fun previous() = command { it.seekToPrevious() }
    fun seekTo(positionMs: Long) = command { it.seekTo(positionMs.coerceAtLeast(0)) }
    fun seekBy(deltaMs: Long) = command { it.seekTo((it.currentPosition + deltaMs).coerceAtLeast(0)) }
    fun skipTo(index: Int) = command { if (index in 0 until it.mediaItemCount) { it.seekToDefaultPosition(index); it.play() } }
    fun removeFromQueue(index: Int) = command { if (index in 0 until it.mediaItemCount) it.removeMediaItem(index) }
    fun moveInQueue(from: Int, to: Int) = command { if (from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount) it.moveMediaItem(from, to) }
    fun setSpeed(speed: Float) = command { it.playbackParameters = PlaybackParameters(speed.coerceIn(0.5f, 2f)) }
    fun setShuffle(on: Boolean) = command { it.shuffleModeEnabled = on }
    fun cycleRepeat() = command {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /** Retry after an error. */
    fun retry() = command { it.prepare(); it.play() }

    /** Skip a failing track. */
    fun skipFailed() = command {
        if (it.hasNextMediaItem()) {
            it.seekToNextMediaItem()
            it.prepare()
            it.play()
        }
    }

    fun stopAndClear() = command { it.stop(); it.clearMediaItems() }

    /** Current error details for the error card; null when playing normally. */
    fun currentError(): PlaybackException? = controller?.playerError
}
