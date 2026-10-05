package app.quranaudio.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.quranaudio.data.db.AppDatabase
import app.quranaudio.data.network.CatalogFetchResult
import app.quranaudio.data.network.CatalogRemoteDataSource
import app.quranaudio.data.prefs.UserPreferences
import app.quranaudio.data.repository.CatalogRepository
import app.quranaudio.data.repository.Clock
import app.quranaudio.data.repository.ConnectivityMonitor
import app.quranaudio.data.repository.LibraryRepository
import app.quranaudio.di.AppModule
import app.quranaudio.shared.catalog.AudioSource
import app.quranaudio.shared.catalog.Catalog
import app.quranaudio.shared.catalog.CatalogRecitation
import app.quranaudio.shared.catalog.CatalogReciter
import app.quranaudio.shared.catalog.CatalogValidator
import app.quranaudio.shared.catalog.RecitationStyle
import java.io.IOException

class FakeRemote(var result: () -> CatalogFetchResult) : CatalogRemoteDataSource {
    var calls = 0
    var lastEtag: String? = null
    override suspend fun fetch(previousEtag: String?): CatalogFetchResult {
        calls++
        lastEtag = previousEtag
        return result()
    }
}

object Fixtures {
    val source = AudioSource(id = "mp3quran", name = "mp3quran.net", websiteUrl = "https://www.mp3quran.net", attribution = "test")

    fun recitation(id: String, surahs: List<Int>, host: String = "cdn.mp3quran.net") = CatalogRecitation(
        id = id, title = "Hafs · Murattal", style = RecitationStyle.MURATTAL,
        audioUrlTemplate = "https://$host/audio/$id/{surah3}.mp3", surahs = surahs,
    )

    fun catalog(vararg reciters: CatalogReciter) = Catalog(generatedAtEpochMs = 1, sources = listOf(source), reciters = reciters.toList())

    val alafasi = CatalogReciter(
        id = "mp3quran:123", sourceId = "mp3quran", name = "Mishary Alafasi", nameArabic = "مشاري العفاسي",
        featured = true, addedAtEpochMs = 10, recitations = listOf(recitation("mp3quran:123", (1..114).toList())),
    )
    val partial = CatalogReciter(
        id = "mp3quran:7", sourceId = "mp3quran", name = "Ahmad Saud", addedAtEpochMs = 20,
        recitations = listOf(recitation("mp3quran:7", listOf(112, 113, 114))),
    )
    val evil = CatalogReciter(
        id = "mp3quran:666", sourceId = "mp3quran", name = "Evil", recitations = listOf(recitation("mp3quran:666", listOf(1), host = "evil.example.com")),
    )
}

class TestGraph {
    val context: Context = ApplicationProvider.getApplicationContext()
    val db: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    val prefs = UserPreferences(context)
    var now = 1_000L
    val clock = Clock { now }
    val remote = FakeRemote { CatalogFetchResult.Updated(Fixtures.catalog(Fixtures.alafasi, Fixtures.partial, Fixtures.evil), "\"v1\"") }
    val catalog = CatalogRepository(
        db.catalogDao(), db.favouritesDao(), remote, prefs,
        CatalogValidator(AppModule.ALLOWED_AUDIO_HOSTS), ConnectivityMonitor(context),
    )
    val library = LibraryRepository(db.favouritesDao(), db.historyDao(), db.playlistDao(), catalog, clock)

    fun failWith(e: Exception = IOException("offline")) { remote.result = { throw e } }
}
