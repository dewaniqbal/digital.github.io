package app.sakina.core

enum class Revelation { MECCAN, MEDINAN }

/** Immutable description of one surah. [number] is the canonical 1..114 index. */
data class Surah(
    val number: Int,
    val nameArabic: String,
    val transliteration: String,
    val ayahCount: Int,
    val revelation: Revelation,
) {
    init {
        require(number in 1..SurahData.COUNT) { "Surah number out of range: $number" }
        require(ayahCount > 0) { "ayahCount must be positive for surah $number" }
    }

    /** Zero-padded 3-digit number, e.g. 1 -> "001". Many audio hosts name files this way. */
    val number3: String get() = number.toString().padStart(3, '0')
}
