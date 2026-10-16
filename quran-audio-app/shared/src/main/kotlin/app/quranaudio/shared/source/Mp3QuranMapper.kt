package app.quranaudio.shared.source

import app.quranaudio.shared.catalog.AudioSource
import app.quranaudio.shared.catalog.CatalogRecitation
import app.quranaudio.shared.catalog.CatalogReciter
import app.quranaudio.shared.catalog.RecitationStyle
import java.time.Instant

/** Maps mp3quran.net API responses into the normalised catalogue. */
object Mp3QuranMapper {

    val source = AudioSource(
        id = Mp3QuranApi.SOURCE_ID,
        name = "mp3quran.net",
        websiteUrl = "https://www.mp3quran.net",
        termsUrl = null, // No published API terms were found; see docs/AUDIO_LICENSING.md.
        attribution = "Recitations streamed from mp3quran.net",
        // Offline storage is not enabled until written permission is obtained from the source.
        allowsOfflineDownload = false,
        maxCacheDays = null,
    )

    fun map(english: List<Mp3QuranReciter>, arabic: List<Mp3QuranReciter>): List<CatalogReciter> {
        val arabicNames = arabic.associate { it.id to it.name }
        return english.mapNotNull { r ->
            val recitations = r.moshaf.mapNotNull(::mapMoshaf)
                // Complete recitations first, then by how much they cover.
                .sortedWith(compareByDescending<CatalogRecitation> { it.isComplete }.thenByDescending { it.surahs.size })
            if (recitations.isEmpty()) return@mapNotNull null
            CatalogReciter(
                id = "${Mp3QuranApi.SOURCE_ID}:${r.id}",
                sourceId = Mp3QuranApi.SOURCE_ID,
                name = r.name.trim().replace(Regex("\\s+"), " "),
                nameArabic = arabicNames[r.id]?.trim()?.takeIf { it.isNotEmpty() },
                addedAtEpochMs = r.date?.let(::parseDate),
                featured = FeaturedReciters.rankOf(r.id) != null,
                recitations = recitations,
            )
        }
    }

    private fun mapMoshaf(m: Mp3QuranMoshaf): CatalogRecitation? {
        val surahs = m.surahList.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..114 }.distinct().sorted()
        if (surahs.isEmpty()) return null
        val server = m.server.trim().let { if (it.endsWith("/")) it else "$it/" }
        val parsed = parseName(m.name)
        return CatalogRecitation(
            id = "${Mp3QuranApi.SOURCE_ID}:${m.id}",
            title = listOfNotNull(parsed.rewaya, parsed.styleLabel).joinToString(" · ").ifEmpty { m.name.trim() },
            rewaya = parsed.rewaya,
            style = parsed.style,
            audioUrlTemplate = "$server{surah3}.mp3",
            surahs = surahs,
        )
    }

    internal data class ParsedName(val rewaya: String?, val style: RecitationStyle, val styleLabel: String?)

    /** "Rewayat Hafs A'n Assem - Murattal" -> ("Hafs an Assem", MURATTAL, "Murattal"). */
    internal fun parseName(raw: String): ParsedName {
        val parts = raw.split(" - ").map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.isNotEmpty() }
        val stylePart = parts.lastOrNull().orEmpty()
        val style = when {
            stylePart.contains("Murattal", ignoreCase = true) -> RecitationStyle.MURATTAL
            stylePart.contains("Mojawwad", ignoreCase = true) || stylePart.contains("Mujawwad", ignoreCase = true) -> RecitationStyle.MUJAWWAD
            stylePart.contains("Mo'lim", ignoreCase = true) || stylePart.contains("Muallim", ignoreCase = true) -> RecitationStyle.MUALLIM
            else -> RecitationStyle.OTHER
        }
        val rewaya = parts.takeIf { it.size > 1 }?.first()
            ?.takeUnless { it.startsWith("Almusshaf", ignoreCase = true) }
            ?.removePrefix("Rewayat ")
            ?.replace(" A'n ", " an ")
            ?.trim()
        val label = when (style) {
            RecitationStyle.MURATTAL -> "Murattal"
            RecitationStyle.MUJAWWAD -> "Mujawwad"
            RecitationStyle.MUALLIM -> "Mu'allim (teaching)"
            // Keep descriptive suffixes such as "Recorded in 1387 AH (1967 CE)" visible.
            RecitationStyle.OTHER -> stylePart.takeIf { it.isNotEmpty() }
        }
        return ParsedName(rewaya, style, label)
    }

    private fun parseDate(raw: String): Long? = runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
}
