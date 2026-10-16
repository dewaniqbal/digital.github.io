package app.quranaudio.data

import app.quranaudio.domain.TrackRef
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryRepositoryTest {
    private val g = TestGraph()
    private fun ref(s: Int, id: String = "mp3quran:123") = TrackRef(id, id, s)

    @Before fun setUp() = runTest { g.catalog.refresh() }
    @After fun tearDown() = g.db.close()

    @Test fun `favourite reciters and tracks toggle`() = runTest {
        assertThat(g.library.toggleFavouriteReciter("mp3quran:123")).isTrue()
        assertThat(g.catalog.reciters.first().single { it.id == "mp3quran:123" }.isFavourite).isTrue()
        assertThat(g.library.toggleFavouriteReciter("mp3quran:123")).isFalse()

        g.library.toggleFavouriteTrack(ref(36))
        assertThat(g.library.favouriteTracks.first().map { it.surahNumber }).containsExactly(36)
        g.library.toggleFavouriteTrack(ref(36))
        assertThat(g.library.favouriteTracks.first()).isEmpty()
    }

    @Test fun `playlist lifecycle create add reorder remove rename delete`() = runTest {
        val id = g.library.createPlaylist("   Night   listening  ")
        g.library.addToPlaylist(id, listOf(ref(67), ref(36), ref(112, "mp3quran:7")))
        var items = g.library.playlistItems(id).first()
        assertThat(items.map { it.ref.surahNumber }).containsExactly(67, 36, 112).inOrder()
        assertThat(g.library.playlist(id).first()!!.name).isEqualTo("Night listening")

        g.library.reorderPlaylist(id, listOf(items[2].entryId, items[0].entryId, items[1].entryId))
        items = g.library.playlistItems(id).first()
        assertThat(items.map { it.ref.surahNumber }).containsExactly(112, 67, 36).inOrder()

        g.library.removeFromPlaylist(id, items[1].entryId)
        items = g.library.playlistItems(id).first()
        assertThat(items.map { it.ref.surahNumber }).containsExactly(112, 36).inOrder()
        assertThat(items.map { it.position }).containsExactly(0, 1).inOrder()

        g.library.renamePlaylist(id, "")
        assertThat(g.library.playlist(id).first()!!.name).isEqualTo("Playlist")
        assertThat(g.library.playlistSummaries.first().single().trackCount).isEqualTo(2)

        g.library.deletePlaylist(id)
        assertThat(g.library.playlistSummaries.first()).isEmpty()
    }

    @Test fun `large playlist keeps order`() = runTest {
        val id = g.library.createPlaylist("All")
        g.library.addToPlaylist(id, (1..114).map { ref(it) })
        val items = g.library.playlistItems(id).first()
        assertThat(items).hasSize(114)
        assertThat(items.map { it.ref.surahNumber }).isEqualTo((1..114).toList())
    }

    @Test fun `history records plays and resume position`() = runTest {
        val track = g.catalog.tracksFor("mp3quran:123", "mp3quran:123", listOf(18)).single()
        g.library.recordPlayStarted(track)
        g.now = 2_000
        g.library.recordProgress(track, positionMs = 120_000, durationMs = 600_000)
        assertThat(g.library.savedPositionMs(track)).isEqualTo(118_000)
        val recent = g.library.recentlyPlayed().first().single()
        assertThat(recent.playCount).isEqualTo(1)
        assertThat(recent.durationMs).isEqualTo(600_000)

        g.library.recordProgress(track, positionMs = 599_000, durationMs = 600_000)
        assertThat(g.library.savedPositionMs(track)).isEqualTo(0) // finished -> restart
        assertThat(g.library.mostListenedReciterIds().first()).containsExactly("mp3quran:123")

        g.library.clearHistory()
        assertThat(g.library.recentlyPlayed().first()).isEmpty()
    }

    @Test fun `user data survives a catalogue refresh`() = runTest {
        g.library.toggleFavouriteTrack(ref(1))
        val id = g.library.createPlaylist("Keep")
        g.library.addToPlaylist(id, listOf(ref(2)))
        g.catalog.refresh()
        assertThat(g.library.favouriteTracks.first()).hasSize(1)
        assertThat(g.library.playlistItems(id).first().single().track).isNotNull()
    }
}
