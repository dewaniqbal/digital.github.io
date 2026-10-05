package app.quranaudio.shared

import app.quranaudio.shared.catalog.AudioSource
import app.quranaudio.shared.catalog.Catalog
import app.quranaudio.shared.catalog.CatalogRecitation
import app.quranaudio.shared.catalog.CatalogReciter
import app.quranaudio.shared.catalog.CatalogValidator
import app.quranaudio.shared.catalog.RecitationStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CatalogValidatorTest {

    private val validator = CatalogValidator(allowedAudioHosts = setOf(".mp3quran.net", "download.quranicaudio.com"))
    private val source = AudioSource(id = "s", name = "S", websiteUrl = "https://example.org", attribution = "S")

    private fun rec(id: String, template: String? = "https://cdn.mp3quran.net/a/{surah3}.mp3", surahs: List<Int> = listOf(1, 2), urls: Map<Int, String> = emptyMap()) =
        CatalogRecitation(id = id, title = "T", style = RecitationStyle.MURATTAL, audioUrlTemplate = template, audioUrls = urls, surahs = surahs)

    private fun reciter(id: String, vararg recs: CatalogRecitation, name: String = "Name", sourceId: String = "s") =
        CatalogReciter(id = id, sourceId = sourceId, name = name, recitations = recs.toList())

    private fun validate(vararg reciters: CatalogReciter) =
        validator.validate(Catalog(generatedAtEpochMs = 0, sources = listOf(source), reciters = reciters.toList()))

    @Test fun `keeps valid data untouched`() {
        val r = validate(reciter("s:1", rec("s:a")))
        assertThat(r.catalog.reciters).hasSize(1)
        assertThat(r.droppedReciters).isEqualTo(0)
    }

    @Test fun `rejects non https and foreign hosts`() {
        assertThat(validator.isAllowedUrl("http://cdn.mp3quran.net/a.mp3")).isFalse()
        assertThat(validator.isAllowedUrl("https://evil.example.com/a.mp3")).isFalse()
        assertThat(validator.isAllowedUrl("https://mp3quran.net.evil.com/a.mp3")).isFalse()
        assertThat(validator.isAllowedUrl("https://user:pw@cdn.mp3quran.net/a.mp3")).isFalse()
        assertThat(validator.isAllowedUrl("https://cdn.mp3quran.net/a b.mp3")).isFalse()
        assertThat(validator.isAllowedUrl("javascript:alert(1)")).isFalse()
        assertThat(validator.isAllowedUrl("https://cdn.mp3quran.net/a.mp3")).isTrue()
        assertThat(validator.isAllowedUrl("https://download.quranicaudio.com/q/1.mp3")).isTrue()
    }

    @Test fun `drops recitations with disallowed template and reciters left empty`() {
        val r = validate(reciter("s:1", rec("s:a", template = "https://evil.com/{surah}.mp3")))
        assertThat(r.catalog.reciters).isEmpty()
        assertThat(r.droppedRecitations).isEqualTo(1)
        assertThat(r.droppedReciters).isEqualTo(1)
    }

    @Test fun `template without placeholder is rejected`() {
        val r = validate(reciter("s:1", rec("s:a", template = "https://cdn.mp3quran.net/same.mp3")))
        assertThat(r.catalog.reciters).isEmpty()
    }

    @Test fun `explicit urls only keep surahs that resolve`() {
        val r = validate(reciter("s:1", rec("s:a", template = null, surahs = listOf(1, 2, 3), urls = mapOf(1 to "https://cdn.mp3quran.net/1.mp3", 2 to "http://cdn.mp3quran.net/2.mp3"))))
        assertThat(r.catalog.reciters.single().recitations.single().surahs).containsExactly(1)
    }

    @Test fun `drops invalid surah numbers duplicates unknown sources and blank names`() {
        val r = validate(
            reciter("s:1", rec("s:a", surahs = listOf(0, 2, 2, 1, 115))),
            reciter("s:1", rec("s:b")),
            reciter("s:2", rec("s:c"), name = "   "),
            reciter("s:3", rec("s:d"), sourceId = "unknown"),
        )
        assertThat(r.catalog.reciters.map { it.id }).containsExactly("s:1")
        assertThat(r.catalog.reciters.single().recitations.single().surahs).containsExactly(1, 2).inOrder()
        assertThat(r.droppedReciters).isEqualTo(3)
    }

    @Test fun `duplicate recitation ids are dropped`() {
        val r = validate(reciter("s:1", rec("s:a")), reciter("s:2", rec("s:a")))
        assertThat(r.catalog.reciters.map { it.id }).containsExactly("s:1")
    }
}
