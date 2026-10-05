package app.quran.core.model

/** Immutable domain models. Contain no Android or serialization types. */

@JvmInline
public value class ReciterId(public val value: String)

@JvmInline
public value class SurahNumber(public val value: Int) {
    init {
        require(value in 1..SURAH_COUNT) { "Surah number out of range: $value" }
    }

    public companion object {
        public const val SURAH_COUNT: Int = 114
    }
}

public data class Reciter(
    val id: ReciterId,
    val name: String,
    val artworkUrl: String?,
)

public data class Surah(
    val number: SurahNumber,
    val nameArabic: String,
    val nameTransliterated: String,
    val nameTranslated: String,
    val ayahCount: Int,
)

/** One playable item: a Surah recited by a Reciter. [audioUrl] is always https. */
public data class Track(
    val reciterId: ReciterId,
    val surah: SurahNumber,
    val audioUrl: String,
) {
    val key: String get() = "${reciterId.value}:${surah.value}"
}

public data class Playlist(
    val id: Long,
    val name: String,
    val tracks: List<TrackRef>,
)

/** Lightweight reference stored in playlists; URL is resolved from cached metadata at play time. */
public data class TrackRef(val reciterId: ReciterId, val surah: SurahNumber)
