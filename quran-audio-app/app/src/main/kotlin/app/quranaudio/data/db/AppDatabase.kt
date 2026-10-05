package app.quranaudio.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Version history (add a Migration for every bump — destructive migration is never enabled,
 * so user favourites, playlists and history survive app updates):
 *  1 — initial schema.
 */
@Database(
    entities = [
        SourceEntity::class,
        ReciterEntity::class,
        RecitationEntity::class,
        FavouriteReciterEntity::class,
        FavouriteTrackEntity::class,
        ListeningHistoryEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun favouritesDao(): FavouritesDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val NAME = "quran_audio.db"

        /** Register future migrations here, e.g. `Migration(1, 2) { db -> ... }`. */
        val MIGRATIONS = arrayOf<androidx.room.migration.Migration>()
    }
}
