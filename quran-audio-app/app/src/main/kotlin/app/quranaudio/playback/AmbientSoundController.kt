package app.quranaudio.playback

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.quranaudio.data.prefs.UserPreferences
import app.quranaudio.di.ApplicationScope
import app.quranaudio.shared.playback.AudioMix
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Independent player for the optional ambient sound, mixed under the Quran recitation.
 *
 * Rules:
 *  - OFF by default and never started automatically: only an explicit user action turns it on,
 *    and the on/off state is not restored after the app restarts (only the selection and volume).
 *  - It does not request audio focus (the Quran player owns focus); it follows the Quran
 *    player instead, pausing whenever recitation is interrupted (calls, other apps, unplugged
 *    headphones) and, if the user chooses, whenever the Quran stops.
 *  - Its gain is capped relative to the Quran gain (see AudioMix) and changes are faded.
 */
@OptIn(UnstableApi::class)
@Singleton
class AmbientSoundController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferences,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    data class State(
        val selected: AmbientSound? = null,
        /** The user has switched ambience on for this session. */
        val enabled: Boolean = false,
        /** Audible right now. */
        val playing: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var player: ExoPlayer? = null
    private var fadeJob: Job? = null
    private var quranSlider = AudioMix.DEFAULT_QURAN_VOLUME
    private var backgroundSlider = AudioMix.DEFAULT_BACKGROUND_VOLUME
    private var quranActive = false
    private var stopWithQuran = true

    init {
        appScope.launch {
            prefs.settings.collect { s ->
                withContext(Dispatchers.Main) {
                    quranSlider = s.quranVolume
                    backgroundSlider = s.backgroundVolume
                    stopWithQuran = s.stopBackgroundWithQuran
                    if (_state.value.selected == null) _state.update { it.copy(selected = AmbientSound.fromId(s.backgroundSound)) }
                    player?.volume = targetGain()
                }
            }
        }
    }

    private fun targetGain(): Float = AudioMix.backgroundGain(backgroundSlider, quranSlider)

    /** User picked a sound in the Background Sounds screen: select it and switch ambience on. */
    fun select(sound: AmbientSound) {
        appScope.launch { prefs.setBackgroundSound(sound.id) }
        val wasSame = _state.value.selected == sound && player != null
        _state.update { it.copy(selected = sound, enabled = true) }
        if (!wasSame) {
            ensurePlayer().apply {
                setMediaItem(MediaItem.fromUri(Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).path(sound.rawRes.toString()).build()))
                prepare()
            }
        }
        // An explicit tap is a user action, so it may start ambience even while the Quran is paused.
        startWithFade(force = true)
    }

    /** User switched ambience off. */
    fun turnOff() {
        _state.update { it.copy(enabled = false) }
        fadeOutThen { releasePlayer() }
    }

    fun toggle() {
        val s = _state.value
        if (s.enabled) turnOff() else s.selected?.let(::select) ?: select(AmbientSound.RAIN)
    }

    /** Called by the playback service whenever the Quran player's activity changes. */
    internal fun onQuranActivityChanged(active: Boolean, interrupted: Boolean) {
        quranActive = active
        val s = _state.value
        if (!s.enabled) return
        when {
            active -> startWithFade()
            interrupted || stopWithQuran -> fadeOutThen { player?.pause() }
        }
    }

    /** Sleep timer expiry or the service shutting down: stop everything. */
    internal fun stopAll() {
        _state.update { it.copy(enabled = false, playing = false) }
        fadeJob?.cancel()
        releasePlayer()
    }

    private fun startWithFade(force: Boolean = false) {
        val p = player ?: return
        // Ambience accompanies recitation: it resumes automatically only together with the Quran.
        if (!force && stopWithQuran && !quranActive) {
            _state.update { it.copy(playing = false) }
            return
        }
        fadeJob?.cancel()
        fadeJob = appScope.launch(Dispatchers.Main) {
            val target = targetGain()
            p.volume = 0f
            p.play()
            _state.update { it.copy(playing = true) }
            val steps = 15
            for (i in 1..steps) {
                delay(AudioMix.FADE_DURATION_MS / steps)
                p.volume = target * AudioMix.fadeIn(i / steps.toFloat())
            }
            p.volume = targetGain()
        }
    }

    private fun fadeOutThen(after: () -> Unit) {
        val p = player ?: run { after(); return }
        fadeJob?.cancel()
        fadeJob = appScope.launch(Dispatchers.Main) {
            val start = p.volume
            val steps = 12
            for (i in 1..steps) {
                delay(AudioMix.FADE_DURATION_MS / steps)
                p.volume = start * AudioMix.fadeOut(i / steps.toFloat())
            }
            after()
            _state.update { it.copy(playing = false) }
        }
    }

    private fun ensurePlayer(): ExoPlayer = player ?: ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            /* handleAudioFocus = */ false,
        )
        .build()
        .also {
            it.repeatMode = Player.REPEAT_MODE_ONE
            it.volume = 0f
            player = it
        }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}
