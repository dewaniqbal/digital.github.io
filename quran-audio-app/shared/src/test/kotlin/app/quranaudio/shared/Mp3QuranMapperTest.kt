package app.quranaudio.shared

import app.quranaudio.shared.catalog.RecitationStyle
import app.quranaudio.shared.source.Mp3QuranMapper
import app.quranaudio.shared.source.Mp3QuranRecitersResponse
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class Mp3QuranMapperTest {

    private val json = Json { ignoreUnknownKeys = true }
    private fun load(name: String): Mp3QuranRecitersResponse =
        json.decodeFromString(javaClass.classLoader.getResource(name)!!.readText())

    private val reciters = Mp3QuranMapper.map(
        load("mp3quran_reciters_eng.json").reciters,
        load("mp3quran_reciters_ar.json").reciters,
    )
    private fun reciter(id: Int) = reciters.single { it.id == "mp3quran:$id" }

    @Test fun `maps ids names and arabic names`() {
        val r = reciter(1)
        assertThat(r.name).isEqualTo("Ibrahim Al-Akdar")
        assertThat(r.nameArabic).isEqualTo("إبراهيم الأخضر")
        assertThat(r.sourceId).isEqualTo("mp3quran")
        assertThat(r.hasCompleteQuran).isTrue()
        assertThat(reciter(112).nameArabic).isEqualTo("محمد صديق المنشاوي")
        assertThat(reciter(21200).nameArabic).isNull()
    }

    @Test fun `collapses whitespace in names`() {
        assertThat(reciter(7).name).isEqualTo("Ahmad Saud")
    }

    @Test fun `builds zero padded audio urls and adds missing trailing slash`() {
        val rec = reciter(7).recitations.single()
        assertThat(rec.audioUrlFor(112)).isEqualTo("https://cdn.mp3quran.net/audio/a_saud/112.mp3")
        assertThat(rec.audioUrlFor(1)).isNull()
        assertThat(reciter(1).recitations.single().audioUrlFor(1)).isEqualTo("https://cdn.mp3quran.net/audio/ibrahim-akhdar/r1/001.mp3")
    }

    @Test fun `partial reciters keep exactly the surahs the source lists`() {
        val r = reciter(7)
        assertThat(r.hasCompleteQuran).isFalse()
        assertThat(r.availableSurahs).containsExactly(112, 113, 114).inOrder()
    }

    @Test fun `complete recitation is ordered first`() {
        val recs = reciter(123).recitations
        assertThat(recs.first().isComplete).isTrue()
        assertThat(recs.first().id).isEqualTo("mp3quran:123")
    }

    @Test fun `parses styles and keeps descriptive labels`() {
        val titles = reciter(112).recitations.associate { it.id to (it.style to it.title) }
        assertThat(titles["mp3quran:113"]).isEqualTo(RecitationStyle.MUJAWWAD to "Mujawwad")
        assertThat(titles["mp3quran:114"]).isEqualTo(RecitationStyle.MUALLIM to "Mu'allim (teaching)")
        assertThat(titles["mp3quran:10924"]).isEqualTo(RecitationStyle.OTHER to "Hafs an Assem · Recorded in 1387 AH (1967 CE)")
        assertThat(reciter(1).recitations.single().title).isEqualTo("Hafs an Assem · Murattal")
    }

    @Test fun `drops reciters without any valid surah and filters invalid numbers`() {
        assertThat(reciters.none { it.id == "mp3quran:999" }).isTrue()
        assertThat(reciter(21200).availableSurahs).containsExactly(36, 67).inOrder()
    }

    @Test fun `parses dates and tolerates bad ones`() {
        assertThat(reciter(1).addedAtEpochMs).isEqualTo(java.time.Instant.parse("2025-09-06T00:39:03Z").toEpochMilli())
    }

    @Test fun `marks editorial featured reciters only`() {
        assertThat(reciter(123).featured).isTrue()
        assertThat(reciter(112).featured).isTrue()
        assertThat(reciter(1).featured).isFalse()
    }
}
