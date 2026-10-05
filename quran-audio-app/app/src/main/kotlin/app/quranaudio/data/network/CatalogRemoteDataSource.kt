package app.quranaudio.data.network

import app.quranaudio.shared.catalog.Catalog
import app.quranaudio.shared.source.Mp3QuranMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

sealed interface CatalogFetchResult {
    data class Updated(val catalog: Catalog, val etag: String?) : CatalogFetchResult
    data object NotModified : CatalogFetchResult
}

/** Network access for the reciter catalogue. Never called from UI code directly. */
interface CatalogRemoteDataSource {
    suspend fun fetch(previousEtag: String?): CatalogFetchResult
}

class BackendCatalogDataSource(private val api: BackendCatalogApi) : CatalogRemoteDataSource {
    override suspend fun fetch(previousEtag: String?): CatalogFetchResult {
        val response = api.catalog(previousEtag)
        return when {
            response.code() == 304 -> CatalogFetchResult.NotModified
            response.isSuccessful -> CatalogFetchResult.Updated(
                response.body() ?: error("Empty catalogue body"),
                response.headers()["ETag"],
            )
            else -> error("Catalogue request failed: HTTP ${response.code()}")
        }
    }
}

/** Builds the same normalised catalogue on-device from the public mp3quran.net API. */
class Mp3QuranDirectDataSource(
    private val api: Mp3QuranDirectApi,
    private val clock: () -> Long = System::currentTimeMillis,
) : CatalogRemoteDataSource {
    override suspend fun fetch(previousEtag: String?): CatalogFetchResult = coroutineScope {
        val en = async { api.reciters("eng") }
        val ar = async { runCatching { api.reciters("ar") }.getOrNull() }
        val reciters = Mp3QuranMapper.map(en.await().reciters, ar.await()?.reciters.orEmpty())
        CatalogFetchResult.Updated(
            Catalog(generatedAtEpochMs = clock(), sources = listOf(Mp3QuranMapper.source), reciters = reciters),
            etag = null,
        )
    }
}
