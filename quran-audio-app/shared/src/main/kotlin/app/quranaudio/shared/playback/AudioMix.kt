package app.quranaudio.shared.playback

import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Gain rules for mixing Quran recitation with an optional ambient background sound.
 *
 * Principles:
 *  - Both sliders are 0…1 user values mapped through a perceptual (squared) curve.
 *  - The background can never be louder than [MAX_BACKGROUND_RELATIVE] × the Quran gain, so it
 *    can never overpower the recitation, whatever the slider positions.
 *  - Background assets are mastered with ≥6 dB headroom (peak −6 dBFS), and gains never exceed
 *    1.0, so the summed output stays well away from clipping.
 */
object AudioMix {
    const val DEFAULT_QURAN_VOLUME = 1.0f
    const val DEFAULT_BACKGROUND_VOLUME = 0.25f
    const val MAX_BACKGROUND_RELATIVE = 0.5f
    const val FADE_DURATION_MS = 1_500L

    /** Perceptual mapping of a 0…1 slider value to linear gain. */
    fun perceptual(slider: Float): Float {
        val v = slider.coerceIn(0f, 1f)
        return v * v
    }

    fun quranGain(quranSlider: Float): Float = perceptual(quranSlider)

    fun backgroundGain(backgroundSlider: Float, quranSlider: Float): Float {
        val requested = perceptual(backgroundSlider)
        val ceiling = quranGain(quranSlider) * MAX_BACKGROUND_RELATIVE
        // When the Quran itself is muted the user is still allowed to hear ambience quietly.
        val effectiveCeiling = if (quranSlider <= 0f) MAX_BACKGROUND_RELATIVE * MAX_BACKGROUND_RELATIVE else ceiling
        return min(requested, effectiveCeiling).coerceIn(0f, 1f)
    }

    /** Equal-power fade curve; progress 0…1 → gain multiplier 0…1. */
    fun fadeIn(progress: Float): Float = sin(progress.coerceIn(0f, 1f) * PI.toFloat() / 2f)

    fun fadeOut(progress: Float): Float = fadeIn(1f - progress.coerceIn(0f, 1f))
}
