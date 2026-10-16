package app.sakina.core

import kotlinx.serialization.json.Json

/** Result of validating untrusted catalog JSON. Invalid parts are dropped and reported, never silently kept. */
data class ParseResult(val catalog: Catalog, val issues: List<String>) {
    val ok: Boolean get() = issues.isEmpty()
}

object CatalogParser {
    val json = Json { ignoreUnknownKeys = true; isLenient = false; explicitNulls = false }

    private val ID = Regex("^[A-Za-z0-9._-]{1,64}$")

    /** Throws [IllegalArgumentException] for structurally unusable input (bad JSON / unsupported schema). */
    fun parse(text: String): ParseResult {
        val raw = try {
            json.decodeFromString(Catalog.serializer(), text)
        } catch (e: Exception) {
            throw IllegalArgumentException("Catalog is not valid JSON for schema v1: ${e.message}", e)
        }
        require(raw.schemaVersion == Catalog.SUPPORTED_SCHEMA) {
            "Unsupported catalog schemaVersion ${raw.schemaVersion}"
        }
        return validate(raw)
    }

    fun validate(raw: Catalog): ParseResult {
        val issues = mutableListOf<String>()

        val sources = LinkedHashMap<String, CatalogSource>()
        for (s in raw.sources) {
            when {
                !ID.matches(s.id) -> issues += "source '${s.id}': invalid id"
                s.id in sources -> issues += "source '${s.id}': duplicate"
                s.name.isBlank() || s.attribution.isBlank() || s.licenseSummary.isBlank() ->
                    issues += "source '${s.id}': name, attribution and licenseSummary are required"
                s.streamingAllowed && s.allowedHosts.isEmpty() ->
                    issues += "source '${s.id}': streaming allowed but no allowedHosts"
                s.offlineAllowed && !s.streamingAllowed ->
                    issues += "source '${s.id}': offline allowed without streaming"
                s.allowedHosts.any { UrlPolicy.parse("https://" + it.removePrefix("*.")) == null } ->
                    issues += "source '${s.id}': invalid allowedHosts entry"
                else -> sources[s.id] = s
            }
        }

        val reciters = LinkedHashMap<String, Reciter>()
        for (r in raw.reciters) {
            if (!ID.matches(r.id)) { issues += "reciter '${r.id}': invalid id"; continue }
            if (r.id in reciters) { issues += "reciter '${r.id}': duplicate"; continue }
            if (r.nameEn.isBlank()) { issues += "reciter '${r.id}': nameEn required"; continue }
            val recs = r.recitations.mapNotNull { validateRecitation(r.id, it, sources, issues) }
            val seen = HashSet<String>()
            val unique = recs.filter { rec ->
                val fresh = seen.add(rec.id)
                if (!fresh) issues += "reciter '${r.id}': duplicate recitation '${rec.id}'"
                fresh
            }
            reciters[r.id] = r.copy(recitations = unique)
        }
        return ParseResult(raw.copy(sources = sources.values.toList(), reciters = reciters.values.toList()), issues)
    }

    private fun validateRecitation(
        reciterId: String,
        rec: Recitation,
        sources: Map<String, CatalogSource>,
        issues: MutableList<String>,
    ): Recitation? {
        val tag = "reciter '$reciterId' recitation '${rec.id}'"
        val src = sources[rec.sourceId]
        if (!ID.matches(rec.id)) { issues += "$tag: invalid id"; return null }
        if (src == null) { issues += "$tag: unknown source '${rec.sourceId}'"; return null }
        if (rec.urlTemplate == null && rec.urls.isEmpty()) { issues += "$tag: no urlTemplate or urls"; return null }
        if (rec.surahs != null && rec.surahs.any { it !in 1..SurahData.COUNT }) {
            issues += "$tag: surah list contains invalid numbers"; return null
        }
        val tpl = rec.urlTemplate
        if (tpl != null) {
            if (!UrlPolicy.isValidTemplate(tpl)) { issues += "$tag: invalid urlTemplate"; return null }
            val host = UrlPolicy.parse(tpl.replace("{surah3}", "001").replace("{surah}", "1"))!!.host.lowercase()
            if (src.allowedHosts.none { UrlPolicy.hostMatches(host, it) }) {
                issues += "$tag: host '$host' not allowed for source '${src.id}'"; return null
            }
        }
        val goodUrls = rec.urls.filter { (n, u) ->
            (n in 1..SurahData.COUNT && UrlPolicy.validateAudioUrl(u, src.allowedHosts) != null)
                .also { ok -> if (!ok) issues += "$tag: dropped invalid url for surah $n" }
        }
        if (tpl == null && goodUrls.isEmpty()) return null
        return rec.copy(urls = goodUrls)
    }
}

/** Looks up playable audio URLs; only returns URLs from sources that permit streaming. */
class CatalogIndex(val catalog: Catalog) {
    private val sources = catalog.sources.associateBy { it.id }
    private val reciters = catalog.reciters.associateBy { it.id }

    fun reciter(id: String): Reciter? = reciters[id]
    fun source(id: String): CatalogSource? = sources[id]

    fun availableSurahs(rec: Recitation): List<Int> {
        if (sources[rec.sourceId]?.streamingAllowed != true || rec.status == SourceStatus.BROKEN) return emptyList()
        val fromTemplate = if (rec.urlTemplate != null) (rec.surahs ?: (1..SurahData.COUNT).toList()) else emptyList()
        return (fromTemplate + rec.urls.keys).distinct().sorted()
    }

    fun audioUrl(rec: Recitation, surah: Int): String? {
        val src = sources[rec.sourceId] ?: return null
        if (!src.streamingAllowed || rec.status == SourceStatus.BROKEN || surah !in 1..SurahData.COUNT) return null
        rec.urls[surah]?.let { return UrlPolicy.validateAudioUrl(it, src.allowedHosts) }
        val tpl = rec.urlTemplate ?: return null
        if (rec.surahs != null && surah !in rec.surahs) return null
        val url = tpl.replace("{surah3}", surah.toString().padStart(3, '0')).replace("{surah}", surah.toString())
        return UrlPolicy.validateAudioUrl(url, src.allowedHosts)
    }

    fun canDownload(rec: Recitation): Boolean = sources[rec.sourceId]?.offlineAllowed == true

    /** Search by Arabic name or English/alternate transliteration; optional style filter. */
    fun search(query: String, style: String? = null): List<Reciter> {
        val nq = TextNormalizer.latin(query)
        val aq = TextNormalizer.arabic(query)
        return catalog.reciters.filter { r ->
            (style == null || r.recitations.any { it.style.equals(style, ignoreCase = true) }) &&
                (query.isBlank() ||
                    (nq.isNotEmpty() && (listOf(r.nameEn) + r.alternateNames).any { TextNormalizer.latin(it).contains(nq) }) ||
                    (aq.isNotEmpty() && r.nameAr != null && TextNormalizer.arabic(r.nameAr).contains(aq)))
        }
    }

    fun styles(): List<String> = catalog.reciters.flatMap { it.recitations }.mapNotNull { it.style }.distinct().sorted()
}
