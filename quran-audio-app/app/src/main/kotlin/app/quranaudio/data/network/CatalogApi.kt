package app.quranaudio.data.network

import app.quranaudio.shared.catalog.Catalog
import app.quranaudio.shared.source.Mp3QuranRecitersResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/** Our own backend proxy (see /backend). */
interface BackendCatalogApi {
    @GET("v1/catalog")
    suspend fun catalog(@Header("If-None-Match") etag: String?): Response<Catalog>
}

/** Public mp3quran.net API v3, used directly when no backend is configured. */
interface Mp3QuranDirectApi {
    @GET("reciters")
    suspend fun reciters(@Query("language") language: String): Mp3QuranRecitersResponse
}
