package app.sakina.core

/**
 * Static reference data for the 114 surahs.
 *
 * Provenance: ayah counts follow the standard Hafs/Kufan numbering (total 6,236, enforced by tests).
 * Meccan/Medinan flags follow the widely used Tanzil classification (86 Meccan, 28 Medinan);
 * scholars differ on some surahs, so treat it as a convention, not a ruling. Transliterations are
 * conventional English spellings. Cross-check against the Quran.com chapters API
 * (`revelation_place`, `verses_count`) once network access is available.
 */
object SurahData {
    const val COUNT = 114
    const val TOTAL_AYAHS = 6236

    private fun s(n: Int, ar: String, tr: String, ay: Int, r: Char) =
        Surah(n, ar, tr, ay, if (r == 'M') Revelation.MEDINAN else Revelation.MECCAN)

    val all: List<Surah> = listOf(
        s(1, "الفاتحة", "Al-Fatihah", 7, K),
        s(2, "البقرة", "Al-Baqarah", 286, M),
        s(3, "آل عمران", "Ali 'Imran", 200, M),
        s(4, "النساء", "An-Nisa", 176, M),
        s(5, "المائدة", "Al-Ma'idah", 120, M),
        s(6, "الأنعام", "Al-An'am", 165, K),
        s(7, "الأعراف", "Al-A'raf", 206, K),
        s(8, "الأنفال", "Al-Anfal", 75, M),
        s(9, "التوبة", "At-Tawbah", 129, M),
        s(10, "يونس", "Yunus", 109, K),
        s(11, "هود", "Hud", 123, K),
        s(12, "يوسف", "Yusuf", 111, K),
        s(13, "الرعد", "Ar-Ra'd", 43, M),
        s(14, "إبراهيم", "Ibrahim", 52, K),
        s(15, "الحجر", "Al-Hijr", 99, K),
        s(16, "النحل", "An-Nahl", 128, K),
        s(17, "الإسراء", "Al-Isra", 111, K),
        s(18, "الكهف", "Al-Kahf", 110, K),
        s(19, "مريم", "Maryam", 98, K),
        s(20, "طه", "Taha", 135, K),
        s(21, "الأنبياء", "Al-Anbya", 112, K),
        s(22, "الحج", "Al-Hajj", 78, M),
        s(23, "المؤمنون", "Al-Mu'minun", 118, K),
        s(24, "النور", "An-Nur", 64, M),
        s(25, "الفرقان", "Al-Furqan", 77, K),
        s(26, "الشعراء", "Ash-Shu'ara", 227, K),
        s(27, "النمل", "An-Naml", 93, K),
        s(28, "القصص", "Al-Qasas", 88, K),
        s(29, "العنكبوت", "Al-'Ankabut", 69, K),
        s(30, "الروم", "Ar-Rum", 60, K),
        s(31, "لقمان", "Luqman", 34, K),
        s(32, "السجدة", "As-Sajdah", 30, K),
        s(33, "الأحزاب", "Al-Ahzab", 73, M),
        s(34, "سبأ", "Saba", 54, K),
        s(35, "فاطر", "Fatir", 45, K),
        s(36, "يس", "Ya-Sin", 83, K),
        s(37, "الصافات", "As-Saffat", 182, K),
        s(38, "ص", "Sad", 88, K),
        s(39, "الزمر", "Az-Zumar", 75, K),
        s(40, "غافر", "Ghafir", 85, K),
        s(41, "فصلت", "Fussilat", 54, K),
        s(42, "الشورى", "Ash-Shuraa", 53, K),
        s(43, "الزخرف", "Az-Zukhruf", 89, K),
        s(44, "الدخان", "Ad-Dukhan", 59, K),
        s(45, "الجاثية", "Al-Jathiyah", 37, K),
        s(46, "الأحقاف", "Al-Ahqaf", 35, K),
        s(47, "محمد", "Muhammad", 38, M),
        s(48, "الفتح", "Al-Fath", 29, M),
        s(49, "الحجرات", "Al-Hujurat", 18, M),
        s(50, "ق", "Qaf", 45, K),
        s(51, "الذاريات", "Adh-Dhariyat", 60, K),
        s(52, "الطور", "At-Tur", 49, K),
        s(53, "النجم", "An-Najm", 62, K),
        s(54, "القمر", "Al-Qamar", 55, K),
        s(55, "الرحمن", "Ar-Rahman", 78, M),
        s(56, "الواقعة", "Al-Waqi'ah", 96, K),
        s(57, "الحديد", "Al-Hadid", 29, M),
        s(58, "المجادلة", "Al-Mujadila", 22, M),
        s(59, "الحشر", "Al-Hashr", 24, M),
        s(60, "الممتحنة", "Al-Mumtahanah", 13, M),
        s(61, "الصف", "As-Saff", 14, M),
        s(62, "الجمعة", "Al-Jumu'ah", 11, M),
        s(63, "المنافقون", "Al-Munafiqun", 11, M),
        s(64, "التغابن", "At-Taghabun", 18, M),
        s(65, "الطلاق", "At-Talaq", 12, M),
        s(66, "التحريم", "At-Tahrim", 12, M),
        s(67, "الملك", "Al-Mulk", 30, K),
        s(68, "القلم", "Al-Qalam", 52, K),
        s(69, "الحاقة", "Al-Haqqah", 52, K),
        s(70, "المعارج", "Al-Ma'arij", 44, K),
        s(71, "نوح", "Nuh", 28, K),
        s(72, "الجن", "Al-Jinn", 28, K),
        s(73, "المزمل", "Al-Muzzammil", 20, K),
        s(74, "المدثر", "Al-Muddaththir", 56, K),
        s(75, "القيامة", "Al-Qiyamah", 40, K),
        s(76, "الإنسان", "Al-Insan", 31, M),
        s(77, "المرسلات", "Al-Mursalat", 50, K),
        s(78, "النبأ", "An-Naba", 40, K),
        s(79, "النازعات", "An-Nazi'at", 46, K),
        s(80, "عبس", "'Abasa", 42, K),
        s(81, "التكوير", "At-Takwir", 29, K),
        s(82, "الانفطار", "Al-Infitar", 19, K),
        s(83, "المطففين", "Al-Mutaffifin", 36, K),
        s(84, "الانشقاق", "Al-Inshiqaq", 25, K),
        s(85, "البروج", "Al-Buruj", 22, K),
        s(86, "الطارق", "At-Tariq", 17, K),
        s(87, "الأعلى", "Al-A'la", 19, K),
        s(88, "الغاشية", "Al-Ghashiyah", 26, K),
        s(89, "الفجر", "Al-Fajr", 30, K),
        s(90, "البلد", "Al-Balad", 20, K),
        s(91, "الشمس", "Ash-Shams", 15, K),
        s(92, "الليل", "Al-Layl", 21, K),
        s(93, "الضحى", "Ad-Duha", 11, K),
        s(94, "الشرح", "Ash-Sharh", 8, K),
        s(95, "التين", "At-Tin", 8, K),
        s(96, "العلق", "Al-'Alaq", 19, K),
        s(97, "القدر", "Al-Qadr", 5, K),
        s(98, "البينة", "Al-Bayyinah", 8, M),
        s(99, "الزلزلة", "Az-Zalzalah", 8, M),
        s(100, "العاديات", "Al-'Adiyat", 11, K),
        s(101, "القارعة", "Al-Qari'ah", 11, K),
        s(102, "التكاثر", "At-Takathur", 8, K),
        s(103, "العصر", "Al-'Asr", 3, K),
        s(104, "الهمزة", "Al-Humazah", 9, K),
        s(105, "الفيل", "Al-Fil", 5, K),
        s(106, "قريش", "Quraysh", 4, K),
        s(107, "الماعون", "Al-Ma'un", 7, K),
        s(108, "الكوثر", "Al-Kawthar", 3, K),
        s(109, "الكافرون", "Al-Kafirun", 6, K),
        s(110, "النصر", "An-Nasr", 3, M),
        s(111, "المسد", "Al-Masad", 5, K),
        s(112, "الإخلاص", "Al-Ikhlas", 4, K),
        s(113, "الفلق", "Al-Falaq", 5, K),
        s(114, "الناس", "An-Nas", 6, K),
    )

    private val byNumber: Map<Int, Surah> = all.associateBy { it.number }

    fun get(number: Int): Surah = byNumber[number] ?: throw IllegalArgumentException("No surah $number")
    fun find(number: Int): Surah? = byNumber[number]

    /** Matches Arabic (ignoring diacritics/alef variants) or transliteration (ignoring punctuation) or number. */
    fun search(query: String): List<Surah> {
        val q = query.trim()
        if (q.isEmpty()) return all
        q.toIntOrNull()?.let { n -> return listOfNotNull(find(n)) }
        val nq = TextNormalizer.latin(q)
        val aq = TextNormalizer.arabic(q)
        return all.filter {
            (nq.isNotEmpty() && TextNormalizer.latin(it.transliteration).contains(nq)) ||
                (aq.isNotEmpty() && TextNormalizer.arabic(it.nameArabic).contains(aq))
        }
    }
}
