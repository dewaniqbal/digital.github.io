package app.quranaudio.shared.quran

enum class RevelationPlace { MAKKAH, MADINAH }

data class Surah(
    val number: Int,
    /** Transliterated name, e.g. "Al-Fatihah". */
    val nameTransliterated: String,
    /** Arabic name, e.g. "الفاتحة". */
    val nameArabic: String,
    val verseCount: Int,
    val revelationPlace: RevelationPlace,
) {
    /** Three-digit, zero-padded number used by most Quran audio CDNs ("001" … "114"). */
    val paddedNumber: String get() = number.toString().padStart(3, '0')
}
