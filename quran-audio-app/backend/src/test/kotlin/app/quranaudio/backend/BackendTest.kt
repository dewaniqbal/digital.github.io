package app.quranaudio.backend

import app.quranaudio.shared.catalog.Catalog
import com.google.common.truth.Truth.assertThat
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class BackendTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private val mp3QuranEn = """{"reciters":[
        {"id":123,"name":"Mishary Alafasi","date":"2025-08-30T21:47:54.000000Z","moshaf":[{"id":123,"name":"Rewayat Hafs A'n Assem - Murattal","server":"https://cdn.mp3quran.net/audio/afs/","surah_total":2,"surah_list":"1,2"}]},
        {"id":5,"name":"Evil Host","moshaf":[{"id":5,"name":"Rewayat Hafs A'n Assem - Murattal","server":"https://evil.example.com/x/","surah_total":1,"surah_list":"1"}]}
    ]}"""
    private val mp3QuranAr = """{"reciters":[{"id":123,"name":"مشاري العفاسي","moshaf":[]}]}"""

    private fun mp3QuranHandler(fail: () -> Boolean = { false }): suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData = { req ->
        val url = req.url.toString()
        when {
            fail() -> respond("boom", HttpStatusCode.BadGateway)
            "language=eng" in url -> respond(mp3QuranEn, HttpStatusCode.OK, json)
            "language=ar" in url -> respond(mp3QuranAr, HttpStatusCode.OK, json)
            else -> respond("", HttpStatusCode.NotFound)
        }
    }

    @Test fun `serves validated normalised catalogue with etag`() = testApplication {
        val upstream = createUpstreamClient(MockEngine(mp3QuranHandler()))
        application { module(Config(), upstream, autoRefresh = false) }

        val response = client.get("/v1/catalog")
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val catalog = CatalogJson.decodeFromString(Catalog.serializer(), response.bodyAsText())
        assertThat(catalog.reciters.map { it.id }).containsExactly("mp3quran:123") // evil host dropped
        val reciter = catalog.reciters.single()
        assertThat(reciter.nameArabic).isEqualTo("مشاري العفاسي")
        assertThat(reciter.recitations.single().audioUrlFor(2)).isEqualTo("https://cdn.mp3quran.net/audio/afs/002.mp3")
        assertThat(catalog.sources.single().allowsOfflineDownload).isFalse()

        val etag = response.headers[HttpHeaders.ETag]!!
        val cached = client.get("/v1/catalog") { header(HttpHeaders.IfNoneMatch, etag) }
        assertThat(cached.status).isEqualTo(HttpStatusCode.NotModified)
    }

    @Test fun `returns 503 when upstream unavailable and nothing cached`() = testApplication {
        val upstream = createUpstreamClient(MockEngine(mp3QuranHandler(fail = { true })))
        application { module(Config(), upstream, autoRefresh = false) }
        val response = client.get("/v1/catalog")
        assertThat(response.status).isEqualTo(HttpStatusCode.ServiceUnavailable)
        assertThat(response.headers[HttpHeaders.RetryAfter]).isEqualTo("60")
    }

    @Test fun `keeps last good catalogue when upstream later fails`() = runTest {
        var failing = false
        val provider = Mp3QuranProvider(createUpstreamClient(MockEngine(mp3QuranHandler(fail = { failing }))))
        val service = CatalogService(listOf(provider), CatalogJson)
        val first = service.refresh()
        assertThat(first!!.catalog.reciters).hasSize(1)
        failing = true
        val second = service.refresh()
        assertThat(second).isSameInstanceAs(first)
    }

    @Test fun `health endpoint`() = testApplication {
        application { module(Config(), createUpstreamClient(MockEngine(mp3QuranHandler())), autoRefresh = false) }
        assertThat(client.get("/health").bodyAsText()).isEqualTo("ok")
    }

    @Test fun `rate limits per client`() = testApplication {
        application { module(Config(requestsPerMinutePerClient = 2), createUpstreamClient(MockEngine(mp3QuranHandler())), autoRefresh = false) }
        repeat(2) { assertThat(client.get("/v1/catalog").status).isEqualTo(HttpStatusCode.OK) }
        assertThat(client.get("/v1/catalog").status).isEqualTo(HttpStatusCode.TooManyRequests)
    }

    @Test fun `config reads env and never prints the secret`() {
        val c = Config.fromEnv(mapOf("QF_CLIENT_ID" to "id", "QF_CLIENT_SECRET" to "s3cr3t", "MP3QURAN_ENABLED" to "false", "PORT" to "9000"))
        assertThat(c.port).isEqualTo(9000)
        assertThat(c.mp3QuranEnabled).isFalse()
        assertThat(c.quranFoundation).isNotNull()
        assertThat(c.toString()).doesNotContain("s3cr3t")
        assertThat(Config.fromEnv(emptyMap()).quranFoundation).isNull()
    }

    @Test fun `quran foundation provider authenticates caches token and maps audio files`() = runTest {
        val tokenCalls = AtomicInteger()
        val engine = MockEngine { req ->
            val url = req.url.toString()
            when {
                url.endsWith("/oauth2/token") -> {
                    tokenCalls.incrementAndGet()
                    assertThat(req.headers[HttpHeaders.Authorization]).startsWith("Basic ")
                    respond("""{"access_token":"tok","expires_in":3600,"token_type":"bearer"}""", HttpStatusCode.OK, json)
                }
                else -> {
                    assertThat(req.headers["x-auth-token"]).isEqualTo("tok")
                    assertThat(req.headers["x-client-id"]).isEqualTo("cid")
                    when {
                        "chapter_reciters" in url -> respond(
                            """{"reciters":[{"id":7,"name":"Mishari Rashid al-`Afasy","arabic_name":"مشاري راشد العفاسي","style":{"name":"Murattal"}}]}""",
                            HttpStatusCode.OK, json,
                        )
                        "chapter_recitations/7" in url -> respond(
                            """{"audio_files":[{"id":1,"chapter_id":1,"audio_url":"https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/1.mp3"},{"id":2,"chapter_id":2,"audio_url":"https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/2.mp3"}]}""",
                            HttpStatusCode.OK, json,
                        )
                        else -> respond("", HttpStatusCode.NotFound)
                    }
                }
            }
        }
        val provider = QuranFoundationProvider(createUpstreamClient(engine), QuranFoundationConfig("cid", "secret", "https://auth.test", "https://api.test/content/api/v4"))
        val reciters = provider.fetchReciters()
        provider.fetchReciters()
        assertThat(tokenCalls.get()).isEqualTo(1)
        val r = reciters.single()
        assertThat(r.id).isEqualTo("qf:7")
        assertThat(r.recitations.single().audioUrlFor(2)).isEqualTo("https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/2.mp3")
        assertThat(provider.source.maxCacheDays).isEqualTo(7)
        assertThat(provider.source.allowsOfflineDownload).isFalse()
    }
}
