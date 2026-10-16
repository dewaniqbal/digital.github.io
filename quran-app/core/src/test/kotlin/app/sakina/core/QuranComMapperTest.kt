package app.sakina.core

import kotlin.test.Test
import kotlin.test.assertEquals

/** Synthetic fixtures shaped like the documented API; they are NOT captured live responses. */
class QuranComMapperTest {
    private val src = CatalogSource(
        id = QuranComMapper.SOURCE_ID, name = "Quran.com", attribution = "x", licenseSummary = "unverified",
        streamingAllowed = true, allowedHosts = listOf("audio.example.org"),
    )

    @Test fun parsesRecitationList() {
        val r = QuranComMapper.parseRecitations(
            """{"recitations":[{"id":7,"reciter_name":" Someone ","style":"Murattal","extra":1},{"id":8,"reciter_name":""}]}"""
        )
        assertEquals(1, r.size)
        assertEquals("qcom-7", r[0].id)
        assertEquals("Someone", r[0].nameEn)
        assertEquals("Murattal", r[0].recitations.single().style)
        assertEquals(SourceStatus.UNVERIFIED, r[0].recitations.single().status)
    }

    @Test fun parsesChapterUrlsAndDropsBadOnes() {
        val urls = QuranComMapper.parseChapterUrls(
            """{"audio_files":[
              {"chapter_id":1,"audio_url":"https://audio.example.org/1.mp3"},
              {"chapter_id":2,"audio_url":"http://audio.example.org/2.mp3"},
              {"chapter_id":3,"audio_url":"https://evil.test/3.mp3"},
              {"chapter_id":999,"audio_url":"https://audio.example.org/999.mp3"},
              {"chapter_id":4}]}""", src
        )
        assertEquals(mapOf(1 to "https://audio.example.org/1.mp3"), urls)
    }

    @Test fun parsesSingleChapterShape() {
        val urls = QuranComMapper.parseChapterUrls(
            """{"audio_file":{"chapter_id":112,"audio_url":"https://audio.example.org/112.mp3"}}""", src
        )
        assertEquals(setOf(112), urls.keys)
    }
}
