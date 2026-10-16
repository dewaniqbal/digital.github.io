package app.quranaudio.shared.catalog

import app.quranaudio.shared.quran.SurahCatalog
import kotlinx.serialization.Serializable

/**
 * Normalised reciter catalogue: the single contract between the backend proxy and the app.
 *
 * Every upstream provider (mp3quran.net, Quran Foundation, …) is mapped into this shape, so new
 * reciters or whole new sources can be added server-side without shipping a new app version.
 */
@Serializable
data class Catalog(
    val schemaVersion: Int = SCHEMA_VERSION,
    val generatedAtEpochMs: Long,
    val sources: List<AudioSource>,
    val reciters: List<CatalogReciter>,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/** Where audio comes from, and what its terms allow. */
@Serializable
data class AudioSource(
    val id: String,
    val name: String,
    val websiteUrl: String,
    val termsUrl: String? = null,
    /** Human-readable attribution shown on the About screen. */
    val attribution: String,
    /**
     * Whether the source's terms permit storing audio on the device for offline listening.
     * The app disables the download feature for any source where this is false.
     */
    val allowsOfflineDownload: Boolean = false,
    /** Upper bound (days) the source permits audio to be cached; null when the terms state none. */
    val maxCacheDays: Int? = null,
)

@Serializable
data class CatalogReciter(
    /** Globally unique, stable id: "<sourceId>:<id at source>". */
    val id: String,
    val sourceId: String,
    val name: String,
    val nameArabic: String? = null,
    /** When the source added/updated this reciter; drives the "New reciters" section. */
    val addedAtEpochMs: Long? = null,
    /** Only set when the source provides it; never guessed. */
    val country: String? = null,
    val imageUrl: String? = null,
    /** Editorially featured (see `FeaturedReciters`). */
    val featured: Boolean = false,
    val recitations: List<CatalogRecitation>,
) {
    val availableSurahs: Set<Int> get() = recitations.flatMapTo(sortedSetOf()) { it.surahs }
    val hasCompleteQuran: Boolean get() = recitations.any { it.isComplete }
    val styles: Set<RecitationStyle> get() = recitations.mapTo(linkedSetOf()) { it.style }
}

@Serializable
data class CatalogRecitation(
    /** Unique within the catalogue: "<sourceId>:<recitation id at source>". */
    val id: String,
    /** Display title, e.g. "Hafs an Asim · Murattal". */
    val title: String,
    /** Narration (riwayah), e.g. "Hafs an Asim", when known. */
    val rewaya: String? = null,
    val style: RecitationStyle,
    /**
     * HTTPS URL template. `{surah}` is replaced with the plain number (1…114),
     * `{surah3}` with the zero-padded number (001…114).
     */
    val audioUrlTemplate: String? = null,
    /** Explicit per-Surah URLs; take precedence over [audioUrlTemplate]. */
    val audioUrls: Map<Int, String> = emptyMap(),
    /** Surah numbers this recitation has audio for, ascending. */
    val surahs: List<Int>,
) {
    val isComplete: Boolean get() = surahs.size == SurahCatalog.SURAH_COUNT

    fun hasSurah(number: Int): Boolean = number in surahs

    /** Resolved audio URL, or null if this recitation does not contain the Surah. */
    fun audioUrlFor(surah: Int): String? {
        if (!hasSurah(surah)) return null
        audioUrls[surah]?.let { return it }
        val template = audioUrlTemplate ?: return null
        return template
            .replace("{surah3}", surah.toString().padStart(3, '0'))
            .replace("{surah}", surah.toString())
    }
}

@Serializable
enum class RecitationStyle {
    /** Measured, moderate-pace recitation. */
    MURATTAL,
    /** Melodic, slower recitation with extended tajweed. */
    MUJAWWAD,
    /** Teaching recitation (al-Mushaf al-Mu'allim), with repetition for learners. */
    MUALLIM,
    OTHER,
}
