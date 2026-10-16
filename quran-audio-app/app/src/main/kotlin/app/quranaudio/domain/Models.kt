package app.quranaudio.domain

import app.quranaudio.shared.catalog.RecitationStyle
import app.quranaudio.shared.quran.Surah
import app.quranaudio.shared.quran.SurahCatalog

data class Reciter(
    val id: String,
    val sourceId: String,
    val name: String,
    val nameArabic: String?,
    val country: String?,
    val featured: Boolean,
    /** Position in the editorial Featured row; null when not featured. */
    val featuredRank: Int?,
    val addedAtEpochMs: Long?,
    val surahCount: Int,
    val hasCompleteQuran: Boolean,
    val styles: Set<RecitationStyle>,
    val isFavourite: Boolean = false,
)

data class Recitation(
    val id: String,
    val reciterId: String,
    val title: String,
    val rewaya: String?,
    val style: RecitationStyle,
    val surahs: List<Int>,
    private val urlTemplate: String?,
    private val explicitUrls: Map<Int, String>,
) {
    val isComplete: Boolean get() = surahs.size == SurahCatalog.SURAH_COUNT

    fun audioUrlFor(surah: Int): String? {
        if (surah !in surahs) return null
        explicitUrls[surah]?.let { return it }
        return urlTemplate
            ?.replace("{surah3}", surah.toString().padStart(3, '0'))
            ?.replace("{surah}", surah.toString())
    }
}

/** One playable item: a Surah in a specific recitation. */
data class Track(
    val reciterId: String,
    val recitationId: String,
    val surahNumber: Int,
    val reciterName: String,
    val reciterNameArabic: String?,
    val recitationTitle: String,
    val audioUrl: String,
) {
    val key: String get() = keyOf(recitationId, surahNumber)
    val surah: Surah get() = SurahCatalog.get(surahNumber)

    companion object {
        fun keyOf(recitationId: String, surah: Int) = "$recitationId#$surah"

        /** Parses a key produced by [keyOf]; null when malformed. */
        fun parseKey(key: String): Pair<String, Int>? {
            val idx = key.lastIndexOf('#')
            if (idx <= 0) return null
            val surah = key.substring(idx + 1).toIntOrNull()?.takeIf(SurahCatalog::isValid) ?: return null
            return key.substring(0, idx) to surah
        }
    }
}

/** Lightweight reference stored in playlists, favourites and history. */
data class TrackRef(val reciterId: String, val recitationId: String, val surahNumber: Int) {
    val key: String get() = Track.keyOf(recitationId, surahNumber)
}

data class ListeningEntry(
    val track: Track,
    val positionMs: Long,
    val durationMs: Long?,
    val lastPlayedAtMs: Long,
    val playCount: Int,
)

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val updatedAtMs: Long,
)

data class PlaylistItem(val entryId: Long, val track: Track?, val ref: TrackRef, val position: Int)

data class AudioSourceInfo(
    val id: String,
    val name: String,
    val websiteUrl: String,
    val termsUrl: String?,
    val attribution: String,
    val allowsOfflineDownload: Boolean,
    val maxCacheDays: Int?,
)

/** State of a remote-backed collection, for loading / empty / error / offline UI. */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Refreshing : SyncStatus
    data class Failed(val offline: Boolean) : SyncStatus
}
