package app.quranaudio.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Database design notes
 * - Catalogue tables (sources, reciters, recitations) are a cache of the remote catalogue and are
 *   replaced on refresh. User tables (favourites, history, playlists) reference catalogue rows by
 *   stable string ids WITHOUT foreign keys, so a reciter temporarily missing upstream never deletes
 *   a user's data.
 * - Surah metadata is static and lives in code (SurahCatalog), not in the database.
 * - Preferences live in DataStore, not Room.
 */

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val websiteUrl: String,
    val termsUrl: String?,
    val attribution: String,
    val allowsOfflineDownload: Boolean,
    val maxCacheDays: Int?,
)

@Entity(
    tableName = "reciters",
    indices = [Index("featuredRank"), Index("addedAtEpochMs"), Index("name")],
)
data class ReciterEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val name: String,
    val nameArabic: String?,
    val country: String?,
    val featured: Boolean,
    /** Order within the editorial Featured row; null when not featured. */
    val featuredRank: Int?,
    val addedAtEpochMs: Long?,
    val surahCount: Int,
    val hasCompleteQuran: Boolean,
    /** Comma-separated RecitationStyle names. */
    val styles: String,
    /** Pre-normalised text for fast search (see SearchNormalizer). */
    val searchKey: String,
)

@Entity(
    tableName = "recitations",
    foreignKeys = [ForeignKey(entity = ReciterEntity::class, parentColumns = ["id"], childColumns = ["reciterId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("reciterId")],
)
data class RecitationEntity(
    @PrimaryKey val id: String,
    val reciterId: String,
    val title: String,
    val rewaya: String?,
    val style: String,
    val urlTemplate: String?,
    /** JSON object {"<surah>": "<url>"} for explicit per-Surah URLs; usually null. */
    val explicitUrlsJson: String?,
    /** Comma-separated ascending Surah numbers. */
    val surahs: String,
    val sortOrder: Int,
)

@Entity(tableName = "favourite_reciters")
data class FavouriteReciterEntity(
    @PrimaryKey val reciterId: String,
    val addedAtMs: Long,
)

@Entity(tableName = "favourite_tracks", indices = [Index("addedAtMs")])
data class FavouriteTrackEntity(
    /** Track key: "<recitationId>#<surah>". */
    @PrimaryKey val trackKey: String,
    val reciterId: String,
    val recitationId: String,
    val surah: Int,
    val addedAtMs: Long,
)

/** Per-track listening state: resume position, duration, play count. Acts as listening history. */
@Entity(tableName = "listening_history", indices = [Index("lastPlayedAtMs"), Index("reciterId")])
data class ListeningHistoryEntity(
    @PrimaryKey val trackKey: String,
    val reciterId: String,
    val recitationId: String,
    val surah: Int,
    val positionMs: Long,
    /** Learned from the player; sources do not publish durations. */
    val durationMs: Long?,
    val lastPlayedAtMs: Long,
    val playCount: Int,
    val completed: Boolean,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [ForeignKey(entity = PlaylistEntity::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["playlistId", "position"])],
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val reciterId: String,
    val recitationId: String,
    val surah: Int,
    val position: Int,
    @ColumnInfo(defaultValue = "0") val addedAtMs: Long,
)

/** Track counts per reciter for "Most listened". */
data class ReciterPlayCount(val reciterId: String, val plays: Int)
