package app.quranaudio.data.repository

import app.quranaudio.data.db.CatalogDao
import app.quranaudio.data.db.FavouritesDao
import app.quranaudio.data.network.CatalogFetchResult
import app.quranaudio.data.network.CatalogRemoteDataSource
import app.quranaudio.data.prefs.UserPreferences
import app.quranaudio.domain.AudioSourceInfo
import app.quranaudio.domain.Recitation
import app.quranaudio.domain.Reciter
import app.quranaudio.domain.SyncStatus
import app.quranaudio.domain.Track
import app.quranaudio.domain.TrackRef
import app.quranaudio.shared.catalog.CatalogValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reciter catalogue. Room is the single source of truth: the UI observes the database, and
 * [refresh] updates it from the network. The app therefore starts instantly from cache and keeps
 * working offline once a catalogue has been loaded.
 */
@Singleton
class CatalogRepository @Inject constructor(
    private val dao: CatalogDao,
    private val favourites: FavouritesDao,
    private val remote: CatalogRemoteDataSource,
    private val prefs: UserPreferences,
    private val validator: CatalogValidator,
    private val connectivity: ConnectivityMonitor,
) {
    private val refreshMutex = Mutex()
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    val reciters: Flow<List<Reciter>> =
        combine(dao.observeReciters(), favourites.observeFavouriteReciterIds()) { list, favIds ->
            val fav = favIds.toHashSet()
            list.map { it.toDomain(isFavourite = it.id in fav) }
        }

    val sources: Flow<List<AudioSourceInfo>> = dao.observeSources().map { list -> list.map { it.toDomain() } }

    fun reciter(id: String): Flow<Reciter?> =
        combine(dao.observeReciter(id), favourites.observeFavouriteReciterIds()) { r, favIds -> r?.toDomain(id in favIds) }

    /** reciterId -> every Surah available from that reciter (across recitations). */
    val allRecitationSurahs: Flow<Map<String, Set<Int>>> = dao.observeAllRecitations().map { list ->
        list.groupBy { it.reciterId }.mapValues { (_, recs) ->
            recs.flatMapTo(HashSet()) { r -> r.surahs.split(',').mapNotNull { it.toIntOrNull() } }
        }
    }

    fun recitations(reciterId: String): Flow<List<Recitation>> =
        dao.observeRecitations(reciterId).map { list -> list.map { it.toDomain() } }

    /** Resolves references (from playlists, favourites, history) into playable tracks. */
    suspend fun resolve(refs: List<TrackRef>): Map<String, Track> {
        if (refs.isEmpty()) return emptyMap()
        val reciters = refs.map { it.reciterId }.distinct().chunked(CHUNK).flatMap { dao.recitersByIds(it) }.associateBy { it.id }
        val recitations = refs.map { it.recitationId }.distinct().chunked(CHUNK).flatMap { dao.recitationsByIds(it) }.associateBy { it.id }
        return refs.mapNotNull { ref ->
            val reciter = reciters[ref.reciterId] ?: return@mapNotNull null
            val recitation = recitations[ref.recitationId] ?: return@mapNotNull null
            buildTrack(reciter, recitation, ref.surahNumber)?.let { ref.key to it }
        }.toMap()
    }

    /** Tracks for every available Surah of one recitation, in Surah order. */
    suspend fun tracksFor(reciterId: String, recitationId: String, surahs: List<Int>? = null): List<Track> {
        val reciter = dao.recitersByIds(listOf(reciterId)).firstOrNull() ?: return emptyList()
        val recitation = dao.recitationsByIds(listOf(recitationId)).firstOrNull() ?: return emptyList()
        val wanted = surahs ?: recitation.toDomain().surahs
        return wanted.mapNotNull { buildTrack(reciter, recitation, it) }
    }

    /** Best recitation of a reciter containing [surah] (complete ones are listed first). */
    suspend fun trackForSurah(reciterId: String, surah: Int): Track? {
        val reciter = dao.recitersByIds(listOf(reciterId)).firstOrNull() ?: return null
        return dao.recitationsFor(reciterId).firstNotNullOfOrNull { buildTrack(reciter, it, surah) }
    }

    suspend fun isEmpty(): Boolean = dao.reciterCount() == 0

    /** Refreshes when the cache is older than [maxAgeMs] (or empty). Safe to call often. */
    suspend fun refreshIfStale(maxAgeMs: Long = DEFAULT_MAX_AGE_MS, now: Long = System.currentTimeMillis()) {
        val fetchedAt = prefs.current().catalogFetchedAtMs
        if (isEmpty() || now - fetchedAt > maxAgeMs) refresh()
    }

    /** Fetches, validates and stores the catalogue. Returns false on failure (status explains why). */
    suspend fun refresh(): Boolean = refreshMutex.withLock {
        _status.value = SyncStatus.Refreshing
        try {
            val settings = prefs.current()
            val etag = settings.catalogEtag.takeUnless { isEmpty() }
            when (val result = remote.fetch(etag)) {
                CatalogFetchResult.NotModified -> prefs.setCatalogMeta(etag, System.currentTimeMillis())
                is CatalogFetchResult.Updated -> {
                    val validated = validator.validate(result.catalog).catalog
                    check(validated.reciters.isNotEmpty()) { "Catalogue contained no valid reciters" }
                    dao.replaceCatalog(
                        sources = validated.sources.map { it.toEntity() },
                        reciters = validated.reciters.map { it.toEntity() },
                        recitations = validated.reciters.flatMap { it.recitationEntities() },
                    )
                    prefs.setCatalogMeta(result.etag, System.currentTimeMillis())
                }
            }
            _status.value = SyncStatus.Idle
            true
        } catch (e: CancellationException) {
            _status.value = SyncStatus.Idle
            throw e
        } catch (e: Exception) {
            _status.value = SyncStatus.Failed(offline = e is IOException || !connectivity.isOnline())
            false
        }
    }

    private companion object {
        const val DEFAULT_MAX_AGE_MS = 24 * 60 * 60 * 1000L
        const val CHUNK = 500
    }
}
