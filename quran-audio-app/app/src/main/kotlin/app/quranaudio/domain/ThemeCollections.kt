package app.quranaudio.domain

/**
 * Built-in themed collections shown on the Playlists screen. Each is a short, editorial list of
 * Surahs, played with a reciter the listener chooses (their last-used reciter by default).
 *
 * Selections are editorial suggestions only. Where a selection follows a well-known practice
 * (e.g. reciting Al-Mulk at night, Al-Kahf on Fridays), that is noted; no claim of reward or
 * ruling is made in the app.
 */
enum class ThemeCollection(val key: String, val surahs: List<Int>) {
    MOST_BEAUTIFUL("most_beautiful", listOf(55, 19, 12, 36, 18, 56, 67, 1)),
    SLEEP("sleep", listOf(67, 32, 2, 112, 113, 114)),      // Al-Mulk, As-Sajdah, Al-Baqarah, the three Quls
    FOCUS("focus", listOf(2, 3, 4, 18, 20, 10)),            // longer Surahs for sustained listening
    EMOTIONAL("emotional", listOf(12, 19, 55, 93, 94, 39)),
    MORNING("morning", listOf(1, 36, 56, 112, 113, 114)),
    EVENING("evening", listOf(67, 56, 32, 112, 113, 114)),
    RAMADAN("ramadan", listOf(2, 97, 44, 1, 3)),            // Al-Baqarah (2:183–187 on fasting), Al-Qadr
    FRIDAY("friday", listOf(18, 62, 32, 76)),               // Al-Kahf, Al-Jumu'ah
    ;

    companion object {
        fun fromKey(key: String): ThemeCollection? = entries.firstOrNull { it.key == key }
    }
}
