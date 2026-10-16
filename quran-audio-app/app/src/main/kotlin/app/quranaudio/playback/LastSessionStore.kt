package app.quranaudio.playback

import app.quranaudio.data.prefs.UserPreferences
import app.quranaudio.data.repository.CatalogRepository
import app.quranaudio.domain.Track
import app.quranaudio.domain.TrackRef
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class LastSession(
    val items: List<Item>,
    val index: Int,
    val positionMs: Long,
) {
    @Serializable
    data class Item(val reciterId: String, val recitationId: String, val surah: Int)
}

/** Persists the play queue so playback can resume after the app or the phone restarts. */
@Singleton
class LastSessionStore @Inject constructor(
    private val prefs: UserPreferences,
    private val catalog: CatalogRepository,
    private val json: Json,
) {
    suspend fun save(tracks: List<Track>, index: Int, positionMs: Long) {
        if (tracks.isEmpty()) return
        // Bound the stored queue so the preferences file stays small.
        val start = (index - MAX_ITEMS / 2).coerceIn(0, (tracks.size - MAX_ITEMS).coerceAtLeast(0))
        val window = tracks.subList(start, minOf(tracks.size, start + MAX_ITEMS))
        val session = LastSession(window.map { LastSession.Item(it.reciterId, it.recitationId, it.surahNumber) }, index - start, positionMs)
        prefs.setLastSession(json.encodeToString(LastSession.serializer(), session))
    }

    /** The saved queue resolved against the current catalogue; null when nothing is restorable. */
    suspend fun load(): Restored? {
        val raw = prefs.current().lastSessionJson ?: return null
        val session = runCatching { json.decodeFromString(LastSession.serializer(), raw) }.getOrNull() ?: return null
        val refs = session.items.map { TrackRef(it.reciterId, it.recitationId, it.surah) }
        val resolved = catalog.resolve(refs)
        val tracks = refs.mapNotNull { resolved[it.key] }
        if (tracks.isEmpty()) return null
        val currentKey = refs.getOrNull(session.index)?.key
        val index = tracks.indexOfFirst { it.key == currentKey }.takeIf { it >= 0 } ?: 0
        return Restored(tracks, index, if (index == session.index || currentKey == tracks[index].key) session.positionMs else 0)
    }

    data class Restored(val tracks: List<Track>, val index: Int, val positionMs: Long)

    private companion object {
        const val MAX_ITEMS = 120
    }
}
