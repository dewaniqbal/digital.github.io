package app.quranaudio.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.quranaudio.MainActivity
import app.quranaudio.data.prefs.RepeatSetting
import app.quranaudio.data.prefs.UserPreferences
import app.quranaudio.data.repository.ConnectivityMonitor
import app.quranaudio.data.repository.LibraryRepository
import app.quranaudio.shared.catalog.CatalogValidator
import app.quranaudio.shared.playback.AudioMix
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Foreground media playback service. Owns the Quran [ExoPlayer] and the [MediaSession], which
 * gives lock-screen, notification, Bluetooth, headset, Android Auto-style and Wear controls.
 *
 * Responsibilities kept here (and only here): audio focus, becoming-noisy, persistence of
 * listening progress and the play queue, network-loss recovery, sleep-timer stop, the ambient
 * sound following the recitation, and playback resumption after a restart.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var prefs: UserPreferences
    @Inject lateinit var library: LibraryRepository
    @Inject lateinit var lastSession: LastSessionStore
    @Inject lateinit var sleepTimer: SleepTimerController
    @Inject lateinit var ambient: AmbientSoundController
    @Inject lateinit var connectivity: ConnectivityMonitor
    @Inject lateinit var validator: CatalogValidator
    @Inject lateinit var events: PlaybackEvents

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private var progressJob: Job? = null
    private var fadeJob: Job? = null
    private var awaitingNetwork = false
    private var autoPlayNext = true
    private var quranSlider = AudioMix.DEFAULT_QURAN_VOLUME

    override fun onCreate() {
        super.onCreate()
        player = buildPlayer()
        session = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .setSessionActivity(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .build()
        player.addListener(PlayerListener())
        observeSettings()
        observeSleepTimer()
        observeNetwork()
    }

    private fun buildPlayer(): ExoPlayer {
        val httpFactory = OkHttpDataSource.Factory(okHttpClient)
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            // Retry transient network failures a few times before surfacing an error.
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(/* minimumLoadableRetryCount = */ 6))
        // Long recitations: keep a generous buffer (~3 min of 128 kbps ≈ 3 MB) to ride out
        // brief connectivity drops without stalling.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(60_000, 180_000, 2_500, 5_000)
            .build()
        return ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true) // headphones unplugged / Bluetooth disconnected -> pause
            .setWakeMode(C.WAKE_MODE_NETWORK)   // keep streaming with the screen off
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep playing if the user swiped the app away mid-recitation; otherwise shut down.
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            saveSessionNow()
            stopSelf()
        }
    }

    override fun onDestroy() {
        saveSessionNow()
        ambient.stopAll()
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    // ---- Observers -------------------------------------------------------------------------

    private fun observeSettings() {
        scope.launch {
            val s = prefs.current()
            player.repeatMode = when (s.repeat) {
                RepeatSetting.OFF -> Player.REPEAT_MODE_OFF
                RepeatSetting.ONE -> Player.REPEAT_MODE_ONE
                RepeatSetting.ALL -> Player.REPEAT_MODE_ALL
            }
            player.shuffleModeEnabled = s.shuffle
            player.playbackParameters = PlaybackParameters(s.playbackSpeed)
        }
        scope.launch {
            prefs.settings.map { Triple(it.quranVolume, it.autoPlayNext, it.wifiOnly) }.distinctUntilChanged().collect { (vol, autoNext, _) ->
                quranSlider = vol
                if (fadeJob?.isActive != true) player.volume = AudioMix.quranGain(vol)
                autoPlayNext = autoNext
                updatePauseAtEnd()
            }
        }
    }

    private fun observeSleepTimer() {
        scope.launch { sleepTimer.state.collect { updatePauseAtEnd() } }
        scope.launch {
            sleepTimer.expired.collect {
                fadeOutAndStop()
                events.emit(PlaybackMessage.SLEEP_TIMER_ENDED)
            }
        }
    }

    private fun updatePauseAtEnd() {
        val repeatOne = player.repeatMode == Player.REPEAT_MODE_ONE
        player.pauseAtEndOfMediaItems = sleepTimer.isEndOfSurah || (!autoPlayNext && !repeatOne)
    }

    private fun observeNetwork() {
        scope.launch {
            combine(connectivity.state, prefs.settings.map { it.wifiOnly }.distinctUntilChanged()) { net, wifiOnly -> net to wifiOnly }
                .collect { (net, wifiOnly) ->
                    if (wifiOnly && net.online && !net.unmetered && player.playWhenReady) {
                        player.pause()
                        events.emit(PlaybackMessage.WIFI_ONLY_BLOCKED)
                    }
                    if (net.online && awaitingNetwork) {
                        // Connection is back: resume from where we stopped.
                        awaitingNetwork = false
                        player.prepare()
                    }
                }
        }
    }

    // ---- Sleep timer stop ------------------------------------------------------------------

    private fun fadeOutAndStop() {
        fadeJob?.cancel()
        ambient.stopAll()
        fadeJob = scope.launch {
            val start = player.volume
            val steps = 20
            for (i in 1..steps) {
                delay(150)
                player.volume = start * AudioMix.fadeOut(i / steps.toFloat())
            }
            player.pause()
            player.volume = AudioMix.quranGain(quranSlider)
            saveProgress()
        }
    }

    // ---- Persistence -----------------------------------------------------------------------

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(PROGRESS_SAVE_INTERVAL_MS)
                saveProgress()
            }
        }
    }

    private suspend fun saveProgress() {
        val track = MediaItems.toTrack(player.currentMediaItem) ?: return
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 }
        library.recordProgress(track, player.currentPosition, duration)
        lastSession.save(queueTracks(), player.currentMediaItemIndex, player.currentPosition)
    }

    private fun queueTracks() = (0 until player.mediaItemCount).mapNotNull { MediaItems.toTrack(player.getMediaItemAt(it)) }

    private fun saveSessionNow() {
        if (!::player.isInitialized || player.mediaItemCount == 0) return
        val tracks = queueTracks()
        val index = player.currentMediaItemIndex
        val position = player.currentPosition
        val track = MediaItems.toTrack(player.currentMediaItem)
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 }
        // Runs in the application scope's lifetime; tiny writes that must survive service teardown.
        CoroutineScope(Dispatchers.IO).launch {
            withContext(NonCancellable) {
                track?.let { library.recordProgress(it, position, duration) }
                lastSession.save(tracks, index, position)
            }
        }
    }

    /** Counts a play once per track start (not on every pause/resume). */
    private var lastStartedKey: String? = null

    private fun recordStartIfNew() {
        val track = MediaItems.toTrack(player.currentMediaItem) ?: return
        val key = "${track.key}@${player.currentMediaItemIndex}"
        if (key == lastStartedKey) return
        lastStartedKey = key
        scope.launch { library.recordPlayStarted(track) }
    }

    // ---- Player events ---------------------------------------------------------------------

    private inner class PlayerListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                recordStartIfNew()
                startProgressLoop()
            } else {
                progressJob?.cancel()
                scope.launch { saveProgress() }
            }
            val interrupted = player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE
            ambient.onQuranActivityChanged(active = isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING), interrupted = interrupted)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) {
                val interrupted = reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS ||
                    reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY
                ambient.onQuranActivityChanged(active = false, interrupted = interrupted)
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && sleepTimer.isEndOfSurah) {
                    sleepTimer.fire()
                }
            }
        }

        override fun onPlaybackSuppressionReasonChanged(reason: Int) {
            if (reason != Player.PLAYBACK_SUPPRESSION_REASON_NONE) ambient.onQuranActivityChanged(active = false, interrupted = true)
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            if (oldPosition.mediaItemIndex != newPosition.mediaItemIndex) {
                // Store the final position of the item we are leaving.
                val leaving = MediaItems.toTrack(oldPosition.mediaItem) ?: return
                scope.launch { library.recordProgress(leaving, oldPosition.positionMs, null) }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (player.isPlaying) recordStartIfNew()
            scope.launch { lastSession.save(queueTracks(), player.currentMediaItemIndex, player.currentPosition) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            updatePauseAtEnd()
            scope.launch {
                prefs.setRepeat(
                    when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> RepeatSetting.ONE
                        Player.REPEAT_MODE_ALL -> RepeatSetting.ALL
                        else -> RepeatSetting.OFF
                    },
                )
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            scope.launch { prefs.setShuffle(shuffleModeEnabled) }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            scope.launch { prefs.setPlaybackSpeed(playbackParameters.speed) }
        }

        override fun onPlayerError(error: PlaybackException) {
            val network = error.errorCode in NETWORK_ERRORS
            if (network && !connectivity.isOnline()) {
                awaitingNetwork = true
                events.emit(PlaybackMessage.OFFLINE)
            } else if (!network) {
                events.emit(PlaybackMessage.TRACK_UNAVAILABLE)
            }
            ambient.onQuranActivityChanged(active = false, interrupted = true)
        }
    }

    // ---- Session callback ------------------------------------------------------------------

    private inner class SessionCallback : MediaSession.Callback {
        /** Controllers cannot pass URIs directly; rebuild them, accepting only allow-listed HTTPS hosts. */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = Futures.immediateFuture(
            mediaItems.mapNotNull { item ->
                val uri = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri ?: return@mapNotNull null
                if (!validator.isAllowedUrl(uri.toString())) return@mapNotNull null
                item.buildUpon().setUri(uri).build()
            },
        )

        /** Bluetooth / system "play" after the app or device restarted: resume the last session. */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            scope.launch {
                val restored = runCatching { lastSession.load() }.getOrNull()
                if (restored == null) {
                    future.setException(UnsupportedOperationException("No session to resume"))
                } else {
                    future.set(
                        MediaSession.MediaItemsWithStartPosition(
                            restored.tracks.map { MediaItems.toMediaItem(it) },
                            restored.index,
                            restored.positionMs,
                        ),
                    )
                }
            }
            return future
        }
    }

    companion object {
        private const val PROGRESS_SAVE_INTERVAL_MS = 10_000L
        private val NETWORK_ERRORS = setOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_TIMEOUT,
        )
    }
}
