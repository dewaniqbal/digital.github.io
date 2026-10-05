package app.quranaudio.backend

import app.quranaudio.shared.catalog.Catalog
import app.quranaudio.shared.catalog.CatalogValidator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.security.MessageDigest

/**
 * Aggregates all enabled providers into one validated catalogue and keeps the last good copy,
 * so a temporary upstream outage never empties the app's catalogue.
 */
class CatalogService(
    private val providers: List<CatalogProvider>,
    private val json: Json,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    data class Snapshot(val catalog: Catalog, val body: String, val etag: String)

    private val log = LoggerFactory.getLogger(CatalogService::class.java)
    private val mutex = Mutex()
    @Volatile private var snapshot: Snapshot? = null

    fun current(): Snapshot? = snapshot

    /** Refreshes from upstream; returns the new snapshot, or the previous one if every provider failed. */
    suspend fun refresh(): Snapshot? = mutex.withLock {
        val results = coroutineScope {
            providers.map { p ->
                async {
                    runCatching { p to p.fetchReciters() }
                        .onFailure { log.warn("Provider {} failed: {}", p.source.id, it.toString()) }
                        .getOrNull()
                }
            }.awaitAll()
        }.filterNotNull()

        if (results.isEmpty()) {
            log.warn("All providers failed; keeping previous catalogue")
            return snapshot
        }
        val validator = CatalogValidator(allowedAudioHosts = results.flatMapTo(HashSet()) { it.first.allowedAudioHosts })
        val raw = Catalog(
            generatedAtEpochMs = clock(),
            sources = results.map { it.first.source },
            reciters = results.flatMap { it.second }.sortedBy { it.name.lowercase() },
        )
        val validated = validator.validate(raw)
        if (validated.droppedReciters + validated.droppedRecitations > 0) {
            log.info("Validation dropped {} reciters and {} recitations", validated.droppedReciters, validated.droppedRecitations)
        }
        // A suspiciously small catalogue (e.g. upstream returned an error page) must not replace a good one.
        val previous = snapshot
        if (previous != null && validated.catalog.reciters.size < previous.catalog.reciters.size / 2) {
            log.warn("New catalogue has {} reciters vs {} before; keeping previous", validated.catalog.reciters.size, previous.catalog.reciters.size)
            return previous
        }
        val body = json.encodeToString(Catalog.serializer(), validated.catalog)
        val etag = sha256(json.encodeToString(Catalog.serializer(), validated.catalog.copy(generatedAtEpochMs = 0)))
        Snapshot(validated.catalog, body, etag).also {
            snapshot = it
            log.info("Catalogue refreshed: {} reciters from {}", it.catalog.reciters.size, it.catalog.sources.map { s -> s.id })
        }
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }.take(32)
}
