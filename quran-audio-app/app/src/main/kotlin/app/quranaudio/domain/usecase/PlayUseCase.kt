package app.quranaudio.domain.usecase

import app.quranaudio.data.prefs.UserPreferences
import app.quranaudio.data.repository.CatalogRepository
import app.quranaudio.domain.ListeningEntry
import app.quranaudio.domain.Reciter
import app.quranaudio.domain.ThemeCollection
import app.quranaudio.domain.Track
import app.quranaudio.playback.LastSessionStore
import app.quranaudio.playback.PlaybackConnection
import app.quranaudio.playback.PlaybackEvents
import app.quranaudio.playback.PlaybackMessage
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Builds play queues from user intents ("play this Surah", "play all", "continue") and hands them
 * to the player. Playing a single Surah queues the rest of that recitation after it, so listening
 * naturally continues to the next Surah when Auto-play next is on.
 */
class PlayUseCase @Inject constructor(
    private val catalog: CatalogRepository,
    private val player: PlaybackConnection,
    private val lastSession: LastSessionStore,
    private val prefs: UserPreferences,
    private val events: PlaybackEvents,
) {
    suspend fun playRecitation(reciterId: String, recitationId: String, startSurah: Int? = null, shuffle: Boolean = false): Boolean {
        val tracks = catalog.tracksFor(reciterId, recitationId)
        if (tracks.isEmpty()) return unavailable()
        val index = startSurah?.let { s -> tracks.indexOfFirst { it.surahNumber == s } }?.takeIf { it >= 0 } ?: 0
        return player.play(tracks, index, shuffle = shuffle)
    }

    /** Plays [surah] by [reciterId] (best recitation that has it), continuing with that recitation's following Surahs. */
    suspend fun playSurah(reciterId: String, surah: Int): Boolean {
        val track = catalog.trackForSurah(reciterId, surah) ?: return unavailable()
        return playRecitation(reciterId, track.recitationId, startSurah = surah)
    }

    suspend fun playTracks(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean? = null): Boolean =
        if (tracks.isEmpty()) unavailable() else player.play(tracks, startIndex, shuffle = shuffle)

    /** Plays [surahs] (e.g. a themed collection) with one reciter, skipping Surahs they lack. */
    suspend fun playSurahsWithReciter(surahs: List<Int>, reciterId: String, startIndex: Int = 0, shuffle: Boolean? = null): Boolean {
        val tracks = surahs.mapNotNull { catalog.trackForSurah(reciterId, it) }
        if (tracks.isEmpty()) return unavailable()
        val startSurah = surahs.getOrNull(startIndex)
        val index = tracks.indexOfFirst { it.surahNumber == startSurah }.coerceAtLeast(0)
        return player.play(tracks, index, shuffle = shuffle)
    }

    suspend fun playCollection(collection: ThemeCollection, reciterId: String, startIndex: Int = 0): Boolean =
        playSurahsWithReciter(collection.surahs, reciterId, startIndex)

    /** Resumes the saved queue when it matches [entry]; otherwise continues that recitation from the entry. */
    suspend fun continueListening(entry: ListeningEntry): Boolean {
        val restored = lastSession.load()
        if (restored != null && restored.tracks.getOrNull(restored.index)?.key == entry.track.key) {
            return player.play(restored.tracks, restored.index)
        }
        return playRecitation(entry.track.reciterId, entry.track.recitationId, entry.track.surahNumber)
    }

    /** Reciter used for collections: last used, else first featured complete reciter, else any complete one. */
    suspend fun defaultReciterId(): String? {
        val last = prefs.current().lastReciterId
        val all: List<Reciter> = catalog.reciters.first()
        if (last != null && all.any { it.id == last }) return last
        return all.filter { it.featured && it.hasCompleteQuran }.minByOrNull { it.featuredRank ?: Int.MAX_VALUE }?.id
            ?: all.firstOrNull { it.hasCompleteQuran }?.id
            ?: all.firstOrNull()?.id
    }

    private fun unavailable(): Boolean {
        events.emit(PlaybackMessage.TRACK_UNAVAILABLE)
        return false
    }
}
