package app.sakina.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Adapter for the public Quran.com API v4 (https://api.quran.com/api/v4).
 * Shapes below follow the published API reference as recalled during authoring; they could NOT be
 * checked against the live service from the build sandbox (network policy), and the service terms
 * for audio redistribution are NOT verified. The source therefore ships with licenseVerified=false
 * and the app marks it accordingly. Parsing is tolerant of unknown fields and strict about URLs.
 */
object QuranComMapper {
    const val SOURCE_ID = "quran-com"
    const val API_BASE = "https://api.quran.com/api/v4"
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Serializable
    data class RecitationsResponse(val recitations: List<ApiRecitation> = emptyList())

    @Serializable
    data class ApiRecitation(
        val id: Int,
        @SerialName("reciter_name") val reciterName: String,
        val style: String? = null,
    )

    @Serializable
    data class ChapterAudioResponse(
        @SerialName("audio_files") val audioFiles: List<ApiAudioFile> = emptyList(),
        @SerialName("audio_file") val audioFile: ApiAudioFile? = null,
    )

    @Serializable
    data class ApiAudioFile(
        @SerialName("chapter_id") val chapterId: Int? = null,
        @SerialName("audio_url") val audioUrl: String? = null,
    )

    /** One reciter entry per API recitation id; style comes from the API ("Murattal", "Mujawwad", ...). */
    fun parseRecitations(body: String): List<Reciter> =
        json.decodeFromString(RecitationsResponse.serializer(), body).recitations
            .filter { it.reciterName.isNotBlank() }
            .map { r ->
                Reciter(
                    id = "qcom-${r.id}",
                    nameEn = r.reciterName.trim(),
                    recitations = listOf(
                        Recitation(
                            id = "qcom-${r.id}-chapter",
                            sourceId = SOURCE_ID,
                            style = r.style?.trim()?.takeIf { it.isNotEmpty() },
                            status = SourceStatus.UNVERIFIED,
                        )
                    ),
                )
            }

    /** Maps /chapter_recitations/{id} to surah -> validated URL. Invalid URLs are dropped. */
    fun parseChapterUrls(body: String, source: CatalogSource): Map<Int, String> {
        val resp = json.decodeFromString(ChapterAudioResponse.serializer(), body)
        val files = resp.audioFiles + listOfNotNull(resp.audioFile)
        val out = LinkedHashMap<Int, String>()
        for (f in files) {
            val n = f.chapterId ?: continue
            val url = f.audioUrl?.let { UrlPolicy.validateAudioUrl(it, source.allowedHosts) } ?: continue
            if (n in 1..SurahData.COUNT) out[n] = url
        }
        return out
    }
}
