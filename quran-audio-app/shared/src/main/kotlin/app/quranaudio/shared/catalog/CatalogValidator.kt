package app.quranaudio.shared.catalog

import app.quranaudio.shared.quran.SurahCatalog
import java.net.URI

/**
 * Defensive validation of catalogue data received from the network.
 *
 * Remote data is never trusted blindly: URLs must be HTTPS and point to an allow-listed host,
 * Surah numbers must be 1…114, names must be sane, and duplicates are dropped. Anything that
 * fails is removed rather than crashing the app.
 */
class CatalogValidator(
    /** Host names (exact) or suffixes starting with "." (e.g. ".mp3quran.net") that audio may come from. */
    private val allowedAudioHosts: Set<String>,
    private val allowedImageHosts: Set<String> = emptySet(),
) {

    data class Result(val catalog: Catalog, val droppedReciters: Int, val droppedRecitations: Int)

    fun validate(catalog: Catalog): Result {
        var droppedRecitations = 0
        var droppedReciters = 0
        val sourceIds = catalog.sources.map { it.id }.toSet()
        val seenReciters = HashSet<String>()
        val seenRecitations = HashSet<String>()

        val reciters = catalog.reciters.mapNotNull { reciter ->
            val name = reciter.name.trim().replace(WHITESPACE, " ")
            if (name.isEmpty() || name.length > MAX_NAME || reciter.sourceId !in sourceIds || !seenReciters.add(reciter.id)) {
                droppedReciters++
                return@mapNotNull null
            }
            val recitations = reciter.recitations.mapNotNull { rec ->
                val cleaned = sanitize(rec)
                if (cleaned == null || !seenRecitations.add(cleaned.id)) {
                    droppedRecitations++
                    null
                } else cleaned
            }
            if (recitations.isEmpty()) {
                droppedReciters++
                return@mapNotNull null
            }
            reciter.copy(
                name = name,
                nameArabic = reciter.nameArabic?.trim()?.replace(WHITESPACE, " ")?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME },
                imageUrl = reciter.imageUrl?.takeIf { isAllowedUrl(it, allowedImageHosts) },
                country = reciter.country?.trim()?.takeIf { it.isNotEmpty() && it.length <= 64 },
                recitations = recitations,
            )
        }
        return Result(catalog.copy(reciters = reciters), droppedReciters, droppedRecitations)
    }

    private fun sanitize(rec: CatalogRecitation): CatalogRecitation? {
        val surahs = rec.surahs.filter(SurahCatalog::isValid).distinct().sorted()
        if (surahs.isEmpty() || rec.title.isBlank()) return null
        val template = rec.audioUrlTemplate?.takeIf { t ->
            ("{surah}" in t || "{surah3}" in t) && isAllowedUrl(t.replace("{surah3}", "001").replace("{surah}", "1"), allowedAudioHosts)
        }
        val urls = rec.audioUrls.filter { (n, u) -> SurahCatalog.isValid(n) && isAllowedUrl(u, allowedAudioHosts) }
        // Every listed Surah must resolve to a playable URL.
        val playable = surahs.filter { it in urls || template != null }
        if (playable.isEmpty()) return null
        return rec.copy(title = rec.title.trim(), audioUrlTemplate = template, audioUrls = urls, surahs = playable)
    }

    fun isAllowedUrl(url: String, hosts: Set<String> = allowedAudioHosts): Boolean {
        if (url.length > MAX_URL || url.any { it.isWhitespace() }) return false
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.userInfo != null) return false
        val host = uri.host?.lowercase() ?: return false
        return hosts.any { allowed ->
            if (allowed.startsWith(".")) host.endsWith(allowed) else host == allowed
        }
    }

    private companion object {
        const val MAX_NAME = 120
        const val MAX_URL = 2048
        val WHITESPACE = Regex("\\s+")
    }
}
