package app.quranaudio.ui.theme

/** Deterministic, calm gradient pairs for generated reciter artwork (ARGB ints). */
object ArtworkPalette {
    private val pairs = listOf(
        0xFF2B2F77.toInt() to 0xFF6A5ACD.toInt(),
        0xFF1B3B6F.toInt() to 0xFF4C8BF5.toInt(),
        0xFF27214F.toInt() to 0xFF8E6BBF.toInt(),
        0xFF0F3D3E.toInt() to 0xFF3E8E7E.toInt(),
        0xFF3B1F4F.toInt() to 0xFFB06AB3.toInt(),
        0xFF1E2A44.toInt() to 0xFF6C8EBF.toInt(),
        0xFF2E1F3D.toInt() to 0xFF7A5C99.toInt(),
        0xFF183A57.toInt() to 0xFF5DA9E9.toInt(),
    )

    fun colorsFor(seed: String): Pair<Int, Int> = pairs[Math.floorMod(seed.hashCode(), pairs.size)]

    /** Up to two initials, skipping Arabic articles such as "Al-". */
    fun initials(name: String): String {
        val words = name.split(' ', '-').map { it.trim() }.filter { it.isNotEmpty() }
            .filterNot { it.equals("al", true) || it.equals("el", true) || it.equals("bin", true) || it.equals("ibn", true) }
        return words.take(2).mapNotNull { w -> w.firstOrNull { it.isLetter() }?.uppercaseChar() }.joinToString("").ifEmpty { "Q" }
    }
}
