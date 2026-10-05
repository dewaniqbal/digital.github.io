@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package app.quranaudio.data.repository

import app.quranaudio.data.db.FavouriteReciterEntity
import app.quranaudio.data.db.FavouriteTrackEntity
import app.quranaudio.data.db.FavouritesDao
import app.quranaudio.data.db.HistoryDao
import app.quranaudio.data.db.ListeningHistoryEntity
import app.quranaudio.data.db.PlaylistDao
import app.quranaudio.data.db.PlaylistEntity
import app.quranaudio.domain.ListeningEntry
import app.quranaudio.domain.Playlist
import app.quranaudio.domain.PlaylistItem
import app.quranaudio.domain.Track
import app.quranaudio.domain.TrackRef
import app.quranaudio.shared.playback.ResumePolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

/** Favourites, listening history and playlists — all stored locally, no account needed. */
@Singleton
class LibraryRepository @Inject constructor(
    private val favourites: FavouritesDao,
    private val history: HistoryDao,
    private val playlists: PlaylistDao,
    private val catalog: CatalogRepository,
    private val clock: Clock,
) {
    // ---- Favourites ------------------------------------------------------------------------

    val favouriteReciterIds: Flow<Set<String>> = favourites.observeFavouriteReciterIds().map { it.toSet() }
    val favouriteTrackKeys: Flow<Set<String>> = favourites.observeFavouriteTrackKeys().map { it.toSet() }

    val favouriteTracks: Flow<List<Track>> = favourites.observeFavouriteTracks().mapLatest { rows ->
        val refs = rows.map { TrackRef(it.reciterId, it.recitationId, it.surah) }
        val resolved = catalog.resolve(refs)
        refs.mapNotNull { resolved[it.key] }
    }

    suspend fun toggleFavouriteReciter(reciterId: String): Boolean =
        if (favourites.isReciterFavourite(reciterId)) {
            favourites.removeReciter(reciterId); false
        } else {
            favourites.addReciter(FavouriteReciterEntity(reciterId, clock.now())); true
        }

    suspend fun toggleFavouriteTrack(ref: TrackRef): Boolean =
        if (favourites.isTrackFavourite(ref.key)) {
            favourites.removeTrack(ref.key); false
        } else {
            favourites.addTrack(FavouriteTrackEntity(ref.key, ref.reciterId, ref.recitationId, ref.surahNumber, clock.now())); true
        }

    // ---- History ---------------------------------------------------------------------------

    fun recentlyPlayed(limit: Int = 30): Flow<List<ListeningEntry>> = history.observeRecent(limit).mapLatest { rows ->
        val resolved = catalog.resolve(rows.map { TrackRef(it.reciterId, it.recitationId, it.surah) })
        rows.mapNotNull { row ->
            resolved[row.trackKey]?.let { ListeningEntry(it, row.positionMs, row.durationMs, row.lastPlayedAtMs, row.playCount) }
        }
    }

    fun progressFor(recitationId: String): Flow<Map<Int, ListeningHistoryEntity>> =
        history.observeForRecitation(recitationId).map { list -> list.associateBy { it.surah } }

    fun mostListenedReciterIds(limit: Int = 10): Flow<List<String>> =
        history.observeMostListenedReciters(limit).map { list -> list.map { it.reciterId } }

    suspend fun savedPositionMs(track: Track): Long {
        val row = history.get(track.key) ?: return 0
        return ResumePolicy.resumePositionMs(row.positionMs, row.durationMs)
    }

    /** Records that playback of [track] started (increments play count once per start). */
    suspend fun recordPlayStarted(track: Track) {
        val existing = history.get(track.key)
        history.upsert(
            (existing ?: ListeningHistoryEntity(track.key, track.reciterId, track.recitationId, track.surahNumber, 0, null, 0, 0, false))
                .copy(lastPlayedAtMs = clock.now(), playCount = (existing?.playCount ?: 0) + 1, completed = false),
        )
    }

    suspend fun recordProgress(track: Track, positionMs: Long, durationMs: Long?) {
        val existing = history.get(track.key)
        history.upsert(
            ListeningHistoryEntity(
                trackKey = track.key,
                reciterId = track.reciterId,
                recitationId = track.recitationId,
                surah = track.surahNumber,
                positionMs = positionMs.coerceAtLeast(0),
                durationMs = durationMs?.takeIf { it > 0 } ?: existing?.durationMs,
                lastPlayedAtMs = clock.now(),
                playCount = existing?.playCount ?: 1,
                completed = ResumePolicy.isCompleted(positionMs, durationMs),
            ),
        )
    }

    suspend fun clearHistory() = history.clear()

    // ---- Playlists -------------------------------------------------------------------------

    val playlistSummaries: Flow<List<Playlist>> = playlists.observePlaylists().map { rows ->
        rows.map { Playlist(it.id, it.name, it.trackCount, it.updatedAtMs) }
    }

    fun playlist(id: Long): Flow<PlaylistEntity?> = playlists.observePlaylist(id)

    fun playlistItems(id: Long): Flow<List<PlaylistItem>> = playlists.observeTracks(id).mapLatest { rows ->
        val refs = rows.map { TrackRef(it.reciterId, it.recitationId, it.surah) }
        val resolved = catalog.resolve(refs)
        rows.mapIndexed { i, row -> PlaylistItem(row.id, resolved[refs[i].key], refs[i], row.position) }
    }

    suspend fun createPlaylist(name: String): Long {
        val now = clock.now()
        return playlists.insertPlaylist(PlaylistEntity(name = sanitizeName(name), createdAtMs = now, updatedAtMs = now))
    }

    suspend fun renamePlaylist(id: Long, name: String) = playlists.rename(id, sanitizeName(name), clock.now())

    suspend fun deletePlaylist(id: Long) = playlists.deletePlaylist(id)

    suspend fun addToPlaylist(id: Long, refs: List<TrackRef>) {
        if (refs.isEmpty()) return
        playlists.addTracks(id, refs.map { Triple(it.reciterId, it.recitationId, it.surahNumber) }, clock.now())
    }

    suspend fun removeFromPlaylist(playlistId: Long, entryId: Long) = playlists.removeEntry(playlistId, entryId, clock.now())

    suspend fun reorderPlaylist(playlistId: Long, orderedEntryIds: List<Long>) = playlists.reorder(playlistId, orderedEntryIds, clock.now())

    private fun sanitizeName(name: String): String =
        name.trim().replace(Regex("\\s+"), " ").take(MAX_PLAYLIST_NAME).ifEmpty { "Playlist" }

    companion object {
        const val MAX_PLAYLIST_NAME = 60
    }
}

/** Injectable wall clock, so time-dependent logic is testable. */
fun interface Clock {
    fun now(): Long
}
