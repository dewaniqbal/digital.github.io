package app.quranaudio.backend

import app.quranaudio.shared.catalog.AudioSource
import app.quranaudio.shared.catalog.CatalogRecitation
import app.quranaudio.shared.catalog.CatalogReciter
import app.quranaudio.shared.catalog.RecitationStyle
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.Base64

/**
 * Quran Foundation Content API v4 (OAuth2 client-credentials, scope `content`).
 *
 * Credentials live only in this server's environment. Every request carries the
 * `x-auth-token` and `x-client-id` headers, as required by the API.
 *
 * Terms (https://api-docs.quran.foundation/legal/developer-terms/): audio must be streamed or
 * cached for at most 7 days, and must not be pre-cached — so downloads are disabled for this source.
 *
 * Response fields are parsed leniently because this provider could not be exercised against
 * live credentials while it was written; see docs/API_SETUP.md for the verification steps.
 */
class QuranFoundationProvider(
    private val http: HttpClient,
    private val config: QuranFoundationConfig,
    private val clock: () -> Long = System::currentTimeMillis,
) : CatalogProvider {

    override val source = AudioSource(
        id = SOURCE_ID,
        name = "Quran Foundation",
        websiteUrl = "https://quran.foundation",
        termsUrl = "https://api-docs.quran.foundation/legal/developer-terms/",
        attribution = "Recitation data provided by Quran Foundation (Quran.com)",
        allowsOfflineDownload = false,
        maxCacheDays = 7,
    )
    override val allowedAudioHosts = setOf(".quranicaudio.com", ".quran.foundation", ".qurancdn.com")

    private val tokenMutex = Mutex()
    private var token: String? = null
    private var tokenExpiresAtMs = 0L

    internal suspend fun accessToken(forceRefresh: Boolean = false): String = tokenMutex.withLock {
        val cached = token
        if (!forceRefresh && cached != null && clock() < tokenExpiresAtMs) return cached
        val basic = Base64.getEncoder().encodeToString("${config.clientId}:${config.clientSecret}".toByteArray())
        val response: HttpResponse = http.submitForm(
            url = "${config.authBaseUrl}/oauth2/token",
            formParameters = parameters {
                append("grant_type", "client_credentials")
                append("scope", "content")
            },
        ) { header(HttpHeaders.Authorization, "Basic $basic") }
        check(response.status.isSuccess()) { "Quran Foundation token request failed: ${response.status}" }
        val body = response.body<JsonObject>()
        val newToken = body["access_token"]?.jsonPrimitive?.contentOrNull ?: error("No access_token in token response")
        val expiresIn = body["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L
        token = newToken
        // Refresh a minute early so in-flight requests never use an expired token.
        tokenExpiresAtMs = clock() + (expiresIn - 60).coerceAtLeast(30) * 1000
        newToken
    }

    private suspend fun getJson(path: String): JsonElement {
        suspend fun call(t: String) = http.get("${config.apiBaseUrl}$path") {
            header("x-auth-token", t)
            header("x-client-id", config.clientId)
        }
        var response = call(accessToken())
        if (response.status == HttpStatusCode.Unauthorized) response = call(accessToken(forceRefresh = true))
        check(response.status.isSuccess()) { "Quran Foundation $path failed: ${response.status}" }
        return response.body()
    }

    override suspend fun fetchReciters(): List<CatalogReciter> = coroutineScope {
        val reciters = getJson("/resources/chapter_reciters?language=en").jsonObject["reciters"]?.jsonArray ?: JsonArray(emptyList())
        val limiter = Semaphore(4) // be gentle with the upstream API
        reciters.mapNotNull { it as? JsonObject }.map { r ->
            async { limiter.withPermit { runCatching { mapReciter(r) }.getOrNull() } }
        }.awaitAll().filterNotNull()
    }

    private suspend fun mapReciter(r: JsonObject): CatalogReciter? {
        val id = r["id"]?.jsonPrimitive?.intOrNull ?: return null
        val name = (r["translated_name"] as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull
            ?: r["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val arabic = r["arabic_name"]?.jsonPrimitive?.contentOrNull
        val styleName = (r["style"] as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull
            ?: r["style"]?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
        val files = getJson("/chapter_recitations/$id").jsonObject["audio_files"]?.jsonArray.orEmpty()
        val urls = files.mapNotNull { f ->
            val o = f as? JsonObject ?: return@mapNotNull null
            val chapter = o["chapter_id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val url = o["audio_url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            chapter to url
        }.toMap()
        if (urls.isEmpty()) return null
        val style = when {
            styleName == null -> RecitationStyle.MURATTAL
            styleName.contains("mujawwad", true) -> RecitationStyle.MUJAWWAD
            styleName.contains("muallim", true) || styleName.contains("mu'allim", true) -> RecitationStyle.MUALLIM
            styleName.contains("murattal", true) -> RecitationStyle.MURATTAL
            else -> RecitationStyle.OTHER
        }
        return CatalogReciter(
            id = "$SOURCE_ID:$id",
            sourceId = SOURCE_ID,
            name = name,
            nameArabic = arabic,
            recitations = listOf(
                CatalogRecitation(
                    id = "$SOURCE_ID:$id",
                    title = listOfNotNull("Hafs an Asim", styleName ?: "Murattal").joinToString(" · "),
                    rewaya = "Hafs an Asim",
                    style = style,
                    audioUrls = urls,
                    surahs = urls.keys.sorted(),
                ),
            ),
        )
    }

    companion object {
        const val SOURCE_ID = "qf"
    }
}
