package app.quranaudio.data

import app.quranaudio.data.network.CatalogFetchResult
import app.quranaudio.domain.SyncStatus
import app.quranaudio.domain.TrackRef
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CatalogRepositoryTest {
    private val g = TestGraph()

    @After fun tearDown() = g.db.close()

    @Test fun `refresh stores validated catalogue and drops disallowed hosts`() = runTest {
        assertThat(g.catalog.refresh()).isTrue()
        val reciters = g.catalog.reciters.first()
        assertThat(reciters.map { it.id }).containsExactly("mp3quran:7", "mp3quran:123")
        val alafasi = reciters.single { it.id == "mp3quran:123" }
        assertThat(alafasi.hasCompleteQuran).isTrue()
        assertThat(alafasi.surahCount).isEqualTo(114)
        assertThat(alafasi.featured).isTrue()
        assertThat(g.catalog.status.value).isEqualTo(SyncStatus.Idle)
        assertThat(g.catalog.sources.first().single().allowsOfflineDownload).isFalse()
    }

    @Test fun `failure keeps cached catalogue and reports offline`() = runTest {
        g.catalog.refresh()
        g.failWith()
        assertThat(g.catalog.refresh()).isFalse()
        assertThat(g.catalog.reciters.first()).hasSize(2)
        assertThat(g.catalog.status.value).isInstanceOf(SyncStatus.Failed::class.java)
        assertThat((g.catalog.status.value as SyncStatus.Failed).offline).isTrue()
    }

    @Test fun `not modified keeps data and sends etag`() = runTest {
        g.catalog.refresh()
        g.remote.result = { CatalogFetchResult.NotModified }
        assertThat(g.catalog.refresh()).isTrue()
        assertThat(g.remote.lastEtag).isEqualTo("\"v1\"")
        assertThat(g.catalog.reciters.first()).hasSize(2)
    }

    @Test fun `empty catalogue is rejected`() = runTest {
        g.catalog.refresh()
        g.remote.result = { CatalogFetchResult.Updated(Fixtures.catalog(Fixtures.evil), null) }
        assertThat(g.catalog.refresh()).isFalse()
        assertThat(g.catalog.reciters.first()).hasSize(2)
    }

    @Test fun `refreshIfStale only fetches when cache is old`() = runTest {
        g.catalog.refreshIfStale(now = System.currentTimeMillis())
        g.catalog.refreshIfStale(now = System.currentTimeMillis())
        assertThat(g.remote.calls).isEqualTo(1)
        g.catalog.refreshIfStale(now = System.currentTimeMillis() + 2 * 24 * 3600 * 1000L)
        assertThat(g.remote.calls).isEqualTo(2)
    }

    @Test fun `resolves tracks and skips unknown references`() = runTest {
        g.catalog.refresh()
        val resolved = g.catalog.resolve(
            listOf(TrackRef("mp3quran:123", "mp3quran:123", 1), TrackRef("mp3quran:7", "mp3quran:7", 1), TrackRef("gone", "gone", 2)),
        )
        assertThat(resolved.keys).containsExactly("mp3quran:123#1")
        assertThat(resolved.values.single().audioUrl).isEqualTo("https://cdn.mp3quran.net/audio/mp3quran:123/001.mp3")
        assertThat(g.catalog.tracksFor("mp3quran:7", "mp3quran:7").map { it.surahNumber }).containsExactly(112, 113, 114).inOrder()
        assertThat(g.catalog.trackForSurah("mp3quran:7", 2)).isNull()
    }
}
