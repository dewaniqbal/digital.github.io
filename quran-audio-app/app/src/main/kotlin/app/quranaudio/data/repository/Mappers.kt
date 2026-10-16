package app.quranaudio.data.repository

import app.quranaudio.data.db.RecitationEntity
import app.quranaudio.data.db.ReciterEntity
import app.quranaudio.data.db.SourceEntity
import app.quranaudio.domain.AudioSourceInfo
import app.quranaudio.domain.Recitation
import app.quranaudio.domain.Reciter
import app.quranaudio.domain.Track
import app.quranaudio.shared.catalog.AudioSource
import app.quranaudio.shared.catalog.CatalogReciter
import app.quranaudio.shared.catalog.RecitationStyle
import app.quranaudio.shared.search.SearchNormalizer
import app.quranaudio.shared.source.FeaturedReciters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun CatalogReciter.toEntity(): ReciterEntity {
    val featuredRank = if (!featured) null else id.substringAfter(':').toIntOrNull()?.let(FeaturedReciters::rankOf) ?: Int.MAX_VALUE
    return ReciterEntity(
        id = id,
        sourceId = sourceId,
        name = name,
        nameArabic = nameArabic,
        country = country,
        featured = featured,
        featuredRank = featuredRank,
        addedAtEpochMs = addedAtEpochMs,
        surahCount = availableSurahs.size,
        hasCompleteQuran = hasCompleteQuran,
        styles = styles.joinToString(",") { it.name },
        searchKey = SearchNormalizer.normalize(listOfNotNull(name, nameArabic).joinToString(" ")),
    )
}

internal fun CatalogReciter.recitationEntities(): List<RecitationEntity> = recitations.mapIndexed { index, r ->
    RecitationEntity(
        id = r.id,
        reciterId = id,
        title = r.title,
        rewaya = r.rewaya,
        style = r.style.name,
        urlTemplate = r.audioUrlTemplate,
        explicitUrlsJson = r.audioUrls.takeIf { it.isNotEmpty() }
            ?.let { urls -> JsonObject(urls.entries.associate { (k, v) -> k.toString() to JsonPrimitive(v) }).toString() },
        surahs = r.surahs.joinToString(","),
        sortOrder = index,
    )
}

internal fun AudioSource.toEntity() = SourceEntity(id, name, websiteUrl, termsUrl, attribution, allowsOfflineDownload, maxCacheDays)

internal fun SourceEntity.toDomain() = AudioSourceInfo(id, name, websiteUrl, termsUrl, attribution, allowsOfflineDownload, maxCacheDays)

internal fun ReciterEntity.toDomain(isFavourite: Boolean = false) = Reciter(
    id = id,
    sourceId = sourceId,
    name = name,
    nameArabic = nameArabic,
    country = country,
    featured = featured,
    featuredRank = featuredRank,
    addedAtEpochMs = addedAtEpochMs,
    surahCount = surahCount,
    hasCompleteQuran = hasCompleteQuran,
    styles = styles.split(',').mapNotNull { s -> runCatching { RecitationStyle.valueOf(s) }.getOrNull() }.toSet(),
    isFavourite = isFavourite,
)

internal fun RecitationEntity.toDomain() = Recitation(
    id = id,
    reciterId = reciterId,
    title = title,
    rewaya = rewaya,
    style = runCatching { RecitationStyle.valueOf(style) }.getOrDefault(RecitationStyle.OTHER),
    surahs = surahs.split(',').mapNotNull { it.toIntOrNull() },
    urlTemplate = urlTemplate,
    explicitUrls = explicitUrlsJson?.let(::parseUrls).orEmpty(),
)

private fun parseUrls(json: String): Map<Int, String> = runCatching {
    Json.parseToJsonElement(json).jsonObject.entries.mapNotNull { (k, v) ->
        val n = k.toIntOrNull() ?: return@mapNotNull null
        val u = v.jsonPrimitive.contentOrNull ?: return@mapNotNull null
        n to u
    }.toMap()
}.getOrDefault(emptyMap())

internal fun buildTrack(reciter: ReciterEntity, recitation: RecitationEntity, surah: Int): Track? {
    val domain = recitation.toDomain()
    val url = domain.audioUrlFor(surah) ?: return null
    return Track(
        reciterId = reciter.id,
        recitationId = recitation.id,
        surahNumber = surah,
        reciterName = reciter.name,
        reciterNameArabic = reciter.nameArabic,
        recitationTitle = recitation.title,
        audioUrl = url,
    )
}
