package app.quranaudio.shared.search

import java.text.Normalizer

/**
 * Text normalisation for fast, forgiving search across English transliterations and Arabic.
 *
 * - Arabic: removes tashkeel and tatweel, unifies alef/yaa/taa-marbuta forms.
 * - Latin: strips accents, apostrophes, hyphens and the "al"/"el"/"ash"… article variants, and
 *   folds common transliteration variants (ee/i, oo/u, ou/u, double letters) so that
 *   "Alafasy", "Al-Afasi" and "Alafasi" all match.
 */
object SearchNormalizer {

    private val ARABIC_DIACRITICS = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val COMBINING_MARKS = Regex("\\p{Mn}+")
    private val NON_ALNUM = Regex("[^\\p{L}\\p{Nd}]+")
    private val ARTICLE = Regex("\\b(al|el|ad|adh|an|ar|as|ash|at|ath|az)\\b")

    fun normalize(input: String): String {
        var s = input.lowercase()
        s = ARABIC_DIACRITICS.replace(s, "")
        s = s.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
            .replace('ى', 'ي').replace('ة', 'ه').replace('ؤ', 'و').replace('ئ', 'ي')
        s = COMBINING_MARKS.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "")
        s = s.replace("'", "").replace("`", "").replace("’", "").replace("ʿ", "").replace("ʾ", "")
        s = NON_ALNUM.replace(s, " ")
        s = ARTICLE.replace(s, " ")
        s = s.split(' ').filter { it.isNotEmpty() }.joinToString(" ") { foldLatin(it) }
        return s
    }

    private fun foldLatin(word: String): String {
        if (word.none { it in 'a'..'z' }) return word
        var w = word
            .replace("ee", "i").replace("ii", "i")
            .replace("oo", "u").replace("ou", "u").replace("uu", "u")
            .replace("aa", "a").replace("ey", "i").replace("y", "i")
            .replace("dh", "z").replace("th", "t").replace("kh", "k").replace("gh", "g").replace("q", "k")
            .replace("e", "i").replace("o", "u")
        // Collapse doubled consonants ("Abbad" ~ "Abad", "Shuraim" ~ "Shuraym").
        w = w.replace(Regex("(.)\\1+"), "$1")
        return w
    }

    /**
     * Relevance of [haystack] (already normalised) for [query] (already normalised).
     * 0 means no match; higher is better.
     */
    fun score(query: String, haystack: String): Int {
        if (query.isEmpty()) return 0
        val compactHay = haystack.replace(" ", "")
        val tokens = query.split(' ').filter { it.isNotEmpty() }
        if (tokens.isEmpty() || tokens.any { it !in compactHay }) return 0
        var score = 1
        if (haystack.startsWith(query) || compactHay.startsWith(query.replace(" ", ""))) score += 3
        if (haystack.split(' ').any { w -> tokens.any { w.startsWith(it) } }) score += 2
        if (haystack == query) score += 5
        return score
    }
}
