package app.quranaudio.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes

private val CatalogRateLimit = RateLimitName("catalog")

val CatalogJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

fun main() {
    val config = Config.fromEnv()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") { module(config) }.start(wait = true)
}

fun createUpstreamClient(engine: HttpClientEngine? = null): HttpClient {
    val configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
        install(ClientContentNegotiation) { json(CatalogJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest { header(HttpHeaders.UserAgent, "QuranAudioBackend/1.0") }
        expectSuccess = false
    }
    return if (engine != null) HttpClient(engine, configure) else HttpClient(CIO, configure)
}

fun Application.module(
    config: Config,
    http: HttpClient = createUpstreamClient(),
    autoRefresh: Boolean = true,
) {
    val log = LoggerFactory.getLogger("QuranAudioBackend")
    val providers = buildList {
        if (config.mp3QuranEnabled) add(Mp3QuranProvider(http))
        config.quranFoundation?.let { add(QuranFoundationProvider(http, it)) }
    }
    require(providers.isNotEmpty()) { "No catalogue provider enabled (set MP3QURAN_ENABLED or QF_CLIENT_ID/QF_CLIENT_SECRET)" }
    log.info("Providers: {}", providers.map { it.source.id })
    val service = CatalogService(providers, CatalogJson)

    install(ContentNegotiation) { json(CatalogJson) }
    install(Compression) { gzip() }
    install(CallLogging)
    install(RateLimit) {
        // Per-client limit. Behind a reverse proxy, install XForwardedHeaders so this sees real client IPs.
        register(CatalogRateLimit) {
            rateLimiter(limit = config.requestsPerMinutePerClient, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "internal_error"))
        }
    }

    if (autoRefresh) {
        launch {
            while (isActive) {
                runCatching { service.refresh() }.onFailure { log.error("Refresh failed", it) }
                delay(config.refreshIntervalMinutes.minutes)
            }
        }
    }

    routing {
        get("/health") { call.respondText("ok") }
        rateLimit(CatalogRateLimit) {
            get("/v1/catalog") {
                val snapshot = service.current() ?: service.refresh()
                if (snapshot == null) {
                    call.response.header(HttpHeaders.RetryAfter, "60")
                    call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "catalog_unavailable"))
                    return@get
                }
                val etag = "\"${snapshot.etag}\""
                call.response.header(HttpHeaders.ETag, etag)
                call.response.header(HttpHeaders.CacheControl, "public, max-age=3600")
                if (call.request.header(HttpHeaders.IfNoneMatch) == etag) {
                    call.respond(HttpStatusCode.NotModified)
                } else {
                    call.respondText(snapshot.body, ContentType.Application.Json)
                }
            }
        }
    }
}
