package app.quranaudio.backend

import app.quranaudio.shared.catalog.AudioSource
import app.quranaudio.shared.catalog.CatalogReciter
import app.quranaudio.shared.source.Mp3QuranApi
import app.quranaudio.shared.source.Mp3QuranMapper
import app.quranaudio.shared.source.Mp3QuranRecitersResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Public mp3quran.net API v3 (no credentials). */
class Mp3QuranProvider(
    private val http: HttpClient,
    private val baseUrl: String = Mp3QuranApi.BASE_URL,
) : CatalogProvider {
    override val source: AudioSource = Mp3QuranMapper.source
    override val allowedAudioHosts: Set<String> = Mp3QuranApi.AUDIO_HOSTS

    override suspend fun fetchReciters(): List<CatalogReciter> = coroutineScope {
        val en = async { http.get(baseUrl + Mp3QuranApi.RECITERS_EN).body<Mp3QuranRecitersResponse>() }
        // Arabic names are optional: a failure here must not hide the whole catalogue.
        val ar = async { runCatching { http.get(baseUrl + Mp3QuranApi.RECITERS_AR).body<Mp3QuranRecitersResponse>() }.getOrNull() }
        Mp3QuranMapper.map(en.await().reciters, ar.await()?.reciters.orEmpty())
    }
}
