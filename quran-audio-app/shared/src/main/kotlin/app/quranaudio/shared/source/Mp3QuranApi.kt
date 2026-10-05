package app.quranaudio.shared.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs for the public mp3quran.net API v3 (no credentials required).
 *
 * Shape verified against the live API on 2026-10-05:
 *   GET https://www.mp3quran.net/api/v3/reciters?language=eng   -> 242 reciters
 *   GET https://www.mp3quran.net/api/v3/reciters?language=ar    -> same ids, Arabic names
 * Audio files: `<moshaf.server><NNN>.mp3` on cdn.mp3quran.net (HTTPS, byte ranges supported).
 */
object Mp3QuranApi {
    const val BASE_URL = "https://www.mp3quran.net/api/v3/"
    const val RECITERS_EN = "reciters?language=eng"
    const val RECITERS_AR = "reciters?language=ar"
    const val SOURCE_ID = "mp3quran"
    val AUDIO_HOSTS = setOf(".mp3quran.net")
}

@Serializable
data class Mp3QuranRecitersResponse(val reciters: List<Mp3QuranReciter> = emptyList())

@Serializable
data class Mp3QuranReciter(
    val id: Int,
    val name: String,
    val letter: String? = null,
    /** ISO-8601 timestamp of the last change at the source, e.g. "2025-09-06T00:39:03.000000Z". */
    val date: String? = null,
    val moshaf: List<Mp3QuranMoshaf> = emptyList(),
)

@Serializable
data class Mp3QuranMoshaf(
    val id: Int,
    /** e.g. "Rewayat Hafs A'n Assem - Murattal". */
    val name: String,
    val server: String,
    @SerialName("surah_total") val surahTotal: Int = 0,
    @SerialName("moshaf_type") val moshafType: Int? = null,
    @SerialName("rewaya_id") val rewayaId: Int? = null,
    /** Comma-separated Surah numbers, e.g. "1,2,3". */
    @SerialName("surah_list") val surahList: String = "",
)
