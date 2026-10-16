package app.sakina.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SurahDataTest {
    @Test fun has114SurahsNumbered1To114() {
        assertEquals(114, SurahData.all.size)
        assertEquals((1..114).toList(), SurahData.all.map { it.number })
    }

    @Test fun totalAyahsIs6236() = assertEquals(SurahData.TOTAL_AYAHS, SurahData.all.sumOf { it.ayahCount })

    @Test fun knownAnchors() {
        assertEquals(7, SurahData.get(1).ayahCount)
        assertEquals(286, SurahData.get(2).ayahCount)
        assertEquals(6, SurahData.get(114).ayahCount)
        assertEquals("الفاتحة", SurahData.get(1).nameArabic)
        assertEquals("Al-Ikhlas", SurahData.get(112).transliteration)
        assertEquals(Revelation.MEDINAN, SurahData.get(2).revelation)
        assertEquals(Revelation.MECCAN, SurahData.get(112).revelation)
    }

    @Test fun revelationSplitMatchesTanzil() {
        assertEquals(28, SurahData.all.count { it.revelation == Revelation.MEDINAN })
        assertEquals(86, SurahData.all.count { it.revelation == Revelation.MECCAN })
    }

    @Test fun namesAreUniqueAndNonBlank() {
        assertEquals(114, SurahData.all.map { it.transliteration }.toSet().size)
        assertTrue(SurahData.all.all { it.nameArabic.isNotBlank() && it.transliteration.isNotBlank() })
    }

    @Test fun number3Padding() {
        assertEquals("001", SurahData.get(1).number3)
        assertEquals("114", SurahData.get(114).number3)
    }

    @Test fun searchByTransliterationArabicAndNumber() {
        assertEquals(1, SurahData.search("al fatihah").first().number)
        assertEquals(36, SurahData.search("yasin").first().number)
        assertEquals(2, SurahData.search("البقره").first().number)
        assertEquals(18, SurahData.search("18").single().number)
        assertEquals(114, SurahData.search("").size)
    }

    @Test(expected = IllegalArgumentException::class) fun unknownSurahThrows() { SurahData.get(115) }
}
