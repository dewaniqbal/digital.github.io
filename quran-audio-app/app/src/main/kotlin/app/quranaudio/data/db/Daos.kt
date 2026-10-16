package app.quranaudio.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM reciters ORDER BY name COLLATE NOCASE")
    fun observeReciters(): Flow<List<ReciterEntity>>

    @Query("SELECT * FROM reciters WHERE id = :id")
    fun observeReciter(id: String): Flow<ReciterEntity?>

    @Query("SELECT * FROM reciters WHERE id IN (:ids)")
    suspend fun recitersByIds(ids: List<String>): List<ReciterEntity>

    @Query("SELECT * FROM recitations WHERE reciterId = :reciterId ORDER BY sortOrder")
    fun observeRecitations(reciterId: String): Flow<List<RecitationEntity>>

    @Query("SELECT * FROM recitations WHERE id IN (:ids)")
    suspend fun recitationsByIds(ids: List<String>): List<RecitationEntity>

    @Query("SELECT * FROM recitations WHERE reciterId = :reciterId ORDER BY sortOrder")
    suspend fun recitationsFor(reciterId: String): List<RecitationEntity>

    @Query("SELECT * FROM recitations ORDER BY sortOrder")
    fun observeAllRecitations(): Flow<List<RecitationEntity>>

    @Query("SELECT COUNT(*) FROM reciters")
    suspend fun reciterCount(): Int

    @Query("SELECT * FROM sources")
    fun observeSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources")
    suspend fun sources(): List<SourceEntity>

    @Upsert suspend fun upsertSources(items: List<SourceEntity>)
    @Upsert suspend fun upsertReciters(items: List<ReciterEntity>)
    @Upsert suspend fun upsertRecitations(items: List<RecitationEntity>)

    @Query("DELETE FROM recitations") suspend fun clearRecitations()
    @Query("DELETE FROM reciters") suspend fun clearReciters()
    @Query("DELETE FROM sources") suspend fun clearSources()

    /**
     * Atomically replaces the cached catalogue (readers never observe a half-written state).
     * User tables are untouched because they reference catalogue rows by id without foreign keys.
     */
    @Transaction
    suspend fun replaceCatalog(sources: List<SourceEntity>, reciters: List<ReciterEntity>, recitations: List<RecitationEntity>) {
        clearRecitations()
        clearReciters()
        clearSources()
        upsertSources(sources)
        upsertReciters(reciters)
        upsertRecitations(recitations)
    }
}

@Dao
interface FavouritesDao {
    @Query("SELECT reciterId FROM favourite_reciters ORDER BY addedAtMs DESC")
    fun observeFavouriteReciterIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addReciter(item: FavouriteReciterEntity)

    @Query("DELETE FROM favourite_reciters WHERE reciterId = :id")
    suspend fun removeReciter(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favourite_reciters WHERE reciterId = :id)")
    suspend fun isReciterFavourite(id: String): Boolean

    @Query("SELECT * FROM favourite_tracks ORDER BY addedAtMs DESC")
    fun observeFavouriteTracks(): Flow<List<FavouriteTrackEntity>>

    @Query("SELECT trackKey FROM favourite_tracks")
    fun observeFavouriteTrackKeys(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrack(item: FavouriteTrackEntity)

    @Query("DELETE FROM favourite_tracks WHERE trackKey = :key")
    suspend fun removeTrack(key: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favourite_tracks WHERE trackKey = :key)")
    suspend fun isTrackFavourite(key: String): Boolean
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM listening_history ORDER BY lastPlayedAtMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ListeningHistoryEntity>>

    @Query("SELECT * FROM listening_history WHERE trackKey = :key")
    suspend fun get(key: String): ListeningHistoryEntity?

    @Query("SELECT * FROM listening_history WHERE recitationId = :recitationId")
    fun observeForRecitation(recitationId: String): Flow<List<ListeningHistoryEntity>>

    @Upsert suspend fun upsert(item: ListeningHistoryEntity)

    @Query("SELECT reciterId, SUM(playCount) AS plays FROM listening_history GROUP BY reciterId ORDER BY plays DESC LIMIT :limit")
    fun observeMostListenedReciters(limit: Int): Flow<List<ReciterPlayCount>>

    @Query("DELETE FROM listening_history")
    suspend fun clear()
}

@Dao
interface PlaylistDao {
    @Query(
        """SELECT p.id, p.name, p.updatedAtMs, COUNT(t.id) AS trackCount
           FROM playlists p LEFT JOIN playlist_tracks t ON t.playlistId = p.id
           GROUP BY p.id ORDER BY p.updatedAtMs DESC""",
    )
    fun observePlaylists(): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observePlaylist(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :id ORDER BY position")
    fun observeTracks(id: Long): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :id ORDER BY position")
    suspend fun tracks(id: Long): List<PlaylistTrackEntity>

    @Insert suspend fun insertPlaylist(p: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updatedAtMs = :now WHERE id = :id")
    suspend fun rename(id: Long, name: String, now: Long)

    @Query("UPDATE playlists SET updatedAtMs = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :id")
    suspend fun maxPosition(id: Long): Int

    @Insert suspend fun insertTracks(items: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("UPDATE playlist_tracks SET position = :position WHERE id = :entryId")
    suspend fun setPosition(entryId: Long, position: Int)

    @Transaction
    suspend fun addTracks(playlistId: Long, items: List<Triple<String, String, Int>>, now: Long) {
        var pos = maxPosition(playlistId)
        insertTracks(items.map { (reciterId, recitationId, surah) ->
            PlaylistTrackEntity(playlistId = playlistId, reciterId = reciterId, recitationId = recitationId, surah = surah, position = ++pos, addedAtMs = now)
        })
        touch(playlistId, now)
    }

    /** Persists a new order given the entry ids in their new sequence. */
    @Transaction
    suspend fun reorder(playlistId: Long, orderedEntryIds: List<Long>, now: Long) {
        orderedEntryIds.forEachIndexed { index, entryId -> setPosition(entryId, index) }
        touch(playlistId, now)
    }

    @Transaction
    suspend fun removeEntry(playlistId: Long, entryId: Long, now: Long) {
        deleteEntry(entryId)
        tracks(playlistId).forEachIndexed { index, t -> if (t.position != index) setPosition(t.id, index) }
        touch(playlistId, now)
    }
}

data class PlaylistSummary(val id: Long, val name: String, val updatedAtMs: Long, val trackCount: Int)
