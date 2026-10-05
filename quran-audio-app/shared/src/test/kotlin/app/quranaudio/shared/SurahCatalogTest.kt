package app.quranaudio.shared

import app.quranaudio.shared.quran.RevelationPlace
import app.quranaudio.shared.quran.SurahCatalog
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SurahCatalogTest {

    @Test fun `contains all 114 surahs numbered consecutively`() {
        assertThat(SurahCatalog.all).hasSize(114)
        assertThat(SurahCatalog.all.map { it.number }).isEqualTo((1..114).toList())
    }

    @Test fun `verse counts sum to 6236`() {
        assertThat(SurahCatalog.all.sumOf { it.verseCount }).isEqualTo(SurahCatalog.TOTAL_VERSES)
    }

    @Test fun `spot check well known surahs`() {
        with(SurahCatalog.get(1)) {
            assertThat(nameTransliterated).isEqualTo("Al-Fatihah")
            assertThat(nameArabic).isEqualTo("الفاتحة")
            assertThat(verseCount).isEqualTo(7)
            assertThat(revelationPlace).isEqualTo(RevelationPlace.MAKKAH)
            assertThat(paddedNumber).isEqualTo("001")
        }
        assertThat(SurahCatalog.get(2).verseCount).isEqualTo(286)
        assertThat(SurahCatalog.get(15).nameTransliterated).isEqualTo("Al-Hijr")
        assertThat(SurahCatalog.get(114).nameArabic).isEqualTo("الناس")
        assertThat(SurahCatalog.get(114).paddedNumber).isEqualTo("114")
    }

    @Test fun `every surah has non blank names`() {
        SurahCatalog.all.forEach {
            assertThat(it.nameTransliterated).isNotEmpty()
            assertThat(it.nameArabic).isNotEmpty()
            assertThat(it.verseCount).isGreaterThan(2)
        }
    }

    @Test fun `invalid numbers are rejected`() {
        assertThat(SurahCatalog.getOrNull(0)).isNull()
        assertThat(SurahCatalog.getOrNull(115)).isNull()
        assertThat(SurahCatalog.isValid(114)).isTrue()
        assertThat(runCatching { SurahCatalog.get(0) }.isFailure).isTrue()
    }
}
