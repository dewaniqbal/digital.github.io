package app.quranaudio.shared.source

/**
 * Editorial "Featured" selection.
 *
 * This is an explicit, documented editorial choice (not a popularity metric): long-established
 * reciters whose recitations are widely broadcast and published internationally, chosen only
 * among reciters that have a complete 114-Surah recitation at the source. It can be changed
 * server-side without an app update. Ids are mp3quran.net reciter ids verified on 2026-10-05.
 *
 * "Most listened" in the app is computed from the user's own on-device history instead.
 */
object FeaturedReciters {
    val mp3QuranIds: List<Int> = listOf(
        123, // Mishary Alafasi
        51,  // Abdulbasit Abdulsamad
        118, // Mahmoud Khalil Al-Hussary
        112, // Mohammed Siddiq Al-Minshawi
        54,  // Abdulrahman Alsudaes
        31,  // Saud Al-Shuraim
        102, // Maher Al Meaqli
        30,  // Saad Al-Ghamdi
        92,  // Yasser Al-Dosari
        5,   // Ahmad Al-Ajmy
        4,   // Abu Bakr Al Shatri
        81,  // Fares Abbad
        86,  // Nasser Alqatami
        109, // Mohammed Ayyub
        76,  // Ali Jaber
        74,  // Ali Alhuthaifi
        60,  // Abdullah Basfer
        111, // Mohammed Jibreel
        125, // Mustafa Ismail
        12,  // Idrees Abkr
    )

    /** Position in the editorial list, used to order the Featured row; null when not featured. */
    fun rankOf(mp3QuranId: Int): Int? = mp3QuranIds.indexOf(mp3QuranId).takeIf { it >= 0 }
}
