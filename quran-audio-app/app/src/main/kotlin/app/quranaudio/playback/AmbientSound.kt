package app.quranaudio.playback

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import app.quranaudio.R

/**
 * Optional ambient background sounds. All loops are original audio synthesised by
 * tools/generate_ambient_sounds.py and released under CC0 (see docs/AUDIO_LICENSING.md),
 * so no third-party recordings are bundled.
 */
enum class AmbientSound(val id: String, @RawRes val rawRes: Int, @StringRes val label: Int) {
    RAIN("rain", R.raw.ambient_rain, R.string.sound_rain),
    BIRDS("birds", R.raw.ambient_birds, R.string.sound_birds),
    FIRE("fire", R.raw.ambient_fire, R.string.sound_fire),
    WAVES("waves", R.raw.ambient_waves, R.string.sound_waves),
    WIND("wind", R.raw.ambient_wind, R.string.sound_wind),
    CAT("cat", R.raw.ambient_cat, R.string.sound_cat),
    OWL("owl", R.raw.ambient_owl, R.string.sound_owl),
    RIVER("river", R.raw.ambient_river, R.string.sound_river),
    CRICKETS("crickets", R.raw.ambient_crickets, R.string.sound_crickets),
    THUNDERSTORM("thunderstorm", R.raw.ambient_thunderstorm, R.string.sound_thunderstorm),
    THUNDER("thunder", R.raw.ambient_thunder, R.string.sound_thunder),
    TRAIN("train", R.raw.ambient_train, R.string.sound_train),
    ;

    companion object {
        fun fromId(id: String?): AmbientSound? = entries.firstOrNull { it.id == id }
    }
}
