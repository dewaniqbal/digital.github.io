package app.sakina.core

import kotlinx.serialization.Serializable

enum class SourceStatus { VERIFIED, UNVERIFIED, BROKEN }

/**
 * An audio provider. Playback and downloads are gated by the flags here; nothing is assumed.
 * A source is only used for streaming when [streamingAllowed] is true, and only for offline storage
 * when [offlineAllowed] is true. [licenseVerified] records that a human actually read the terms.
 */
@Serializable
data class CatalogSource(
    val id: String,
    val name: String,
    val homepage: String? = null,
    val attribution: String,
    val licenseSummary: String,
    val licenseUrl: String? = null,
    val licenseVerified: Boolean = false,
    val streamingAllowed: Boolean = false,
    val offlineAllowed: Boolean = false,
    /** Exact hosts, or "*.example.org" for subdomains. Audio URLs on any other host are rejected. */
    val allowedHosts: List<String> = emptyList(),
)

@Serializable
data class Recitation(
    val id: String,
    val sourceId: String,
    val style: String? = null,
    val riwayah: String? = null,
    /** e.g. "https://host/path/{surah3}.mp3". Placeholders: {surah} and {surah3}. */
    val urlTemplate: String? = null,
    /** Explicit per-surah URLs (surah number -> URL). Takes precedence over [urlTemplate]. */
    val urls: Map<Int, String> = emptyMap(),
    /** Surahs the template covers; null means all 114. Ignored for [urls]-only recitations. */
    val surahs: List<Int>? = null,
    val status: SourceStatus = SourceStatus.UNVERIFIED,
    /** ISO-8601 instant of the last successful validation, if any. */
    val lastValidated: String? = null,
)

@Serializable
data class Reciter(
    val id: String,
    val nameAr: String? = null,
    val nameEn: String,
    val alternateNames: List<String> = emptyList(),
    /** Only set when verified. */
    val country: String? = null,
    val recitations: List<Recitation> = emptyList(),
)

@Serializable
data class Catalog(
    val schemaVersion: Int,
    val generatedAt: String? = null,
    val sources: List<CatalogSource> = emptyList(),
    val reciters: List<Reciter> = emptyList(),
) {
    companion object {
        const val SUPPORTED_SCHEMA = 1
        val EMPTY = Catalog(SUPPORTED_SCHEMA)
    }
}
