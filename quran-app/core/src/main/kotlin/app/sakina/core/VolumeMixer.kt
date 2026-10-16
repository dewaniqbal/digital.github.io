package app.sakina.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What happens to the background sound when Quran playback stops (user setting). */
enum class AmbientOnQuranStop { STOP, CONTINUE }

/**
 * User-facing volume state. Quran and background sound are two fully separate channels:
 * each has its own level and its own mute. Levels are slider positions in 0f..1f.
 */
data class VolumeSettings(
    val quranVolume: Float = 0.8f,
    val ambientVolume: Float = 0.3f,
    val quranMuted: Boolean = false,
    val ambientMuted: Boolean = false,
    val ambientEnabled: Boolean = false, // never on by default
    val ambientOnQuranStop: AmbientOnQuranStop = AmbientOnQuranStop.STOP,
)

/** Persistence seam (DataStore in the app, in-memory in tests). */
interface VolumeStore {
    suspend fun load(): VolumeSettings
    suspend fun save(settings: VolumeSettings)
}

/**
 * Single owner of both channels' gains. Every setter copies only its own channel's fields, so a
 * change to one channel cannot affect the other (enforced by tests). Gains are computed per channel
 * only from that channel's own settings: no ducking or cross-coupling, hence no surprise jumps.
 *
 * Clipping headroom: the two players are summed by the platform mixer, so the background channel is
 * capped at [AMBIENT_CEILING] of full scale (recitation always has priority) and both use a
 * squared (perceptual) slider curve, which makes low settings gentle and avoids abrupt steps.
 */
class VolumeMixer(initial: VolumeSettings = VolumeSettings()) {
    private val _settings = MutableStateFlow(sanitize(initial))
    val settings: StateFlow<VolumeSettings> = _settings.asStateFlow()

    fun setQuranVolume(v: Float) = _settings.update { it.copy(quranVolume = clamp(v)) }
    fun setAmbientVolume(v: Float) = _settings.update { it.copy(ambientVolume = clamp(v)) }
    fun setQuranMuted(m: Boolean) = _settings.update { it.copy(quranMuted = m) }
    fun setAmbientMuted(m: Boolean) = _settings.update { it.copy(ambientMuted = m) }
    fun setAmbientEnabled(e: Boolean) = _settings.update { it.copy(ambientEnabled = e) }
    fun setAmbientOnQuranStop(p: AmbientOnQuranStop) = _settings.update { it.copy(ambientOnQuranStop = p) }

    /** Gain (0..1) to apply to the recitation player. Depends only on Quran settings. */
    val quranGain: Float get() = gainOf(_settings.value.quranVolume, _settings.value.quranMuted, 1f)

    /** Gain (0..AMBIENT_CEILING) for the background player. Depends only on background settings. */
    val ambientGain: Float
        get() = _settings.value.let { if (!it.ambientEnabled) 0f else gainOf(it.ambientVolume, it.ambientMuted, AMBIENT_CEILING) }

    /** Should the background player keep running after the Quran player stopped? */
    fun ambientShouldPlayAfterQuranStops(): Boolean =
        _settings.value.let { it.ambientEnabled && it.ambientOnQuranStop == AmbientOnQuranStop.CONTINUE }

    companion object {
        const val AMBIENT_CEILING = 0.5f

        fun clamp(v: Float): Float = if (v.isNaN()) 0f else v.coerceIn(0f, 1f)

        fun gainOf(volume: Float, muted: Boolean, ceiling: Float): Float =
            if (muted) 0f else clamp(volume).let { it * it } * ceiling

        fun sanitize(s: VolumeSettings) = s.copy(quranVolume = clamp(s.quranVolume), ambientVolume = clamp(s.ambientVolume))

        /** Linear ramp of gains for fades (e.g. 250 ms at ~25 ms steps), ending exactly at [to]. */
        fun ramp(from: Float, to: Float, steps: Int): List<Float> {
            require(steps >= 1)
            return (1..steps).map { i -> if (i == steps) to else from + (to - from) * i / steps }
        }
    }
}
