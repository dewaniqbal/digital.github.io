package app.sakina.core

import java.text.Normalizer

/** Search-folding helpers so users can type "al fatiha", "Al-Fatihah" or an Arabic name without tashkeel. */
object TextNormalizer {
    private val ARABIC_MARKS = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")

    fun arabic(s: String): String = ARABIC_MARKS.replace(Normalizer.normalize(s, Normalizer.Form.NFKC), "")
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        .replace('ى', 'ي').replace('ة', 'ه')
        .filter { it.isLetterOrDigit() }

    /** Lowercase, strip accents, drop everything that is not a letter/digit, fold "ph"-style variants minimally. */
    fun latin(s: String): String {
        val d = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        return d.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }
    }
}
