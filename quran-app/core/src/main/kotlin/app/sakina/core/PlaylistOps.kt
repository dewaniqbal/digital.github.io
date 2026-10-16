package app.sakina.core

/** An entry in a user playlist: a surah, optionally pinned to a reciter (null = use the current reciter). */
data class PlaylistEntry(val id: Long, val surah: Int, val reciterId: String?)

/** Pure list operations shared by the UI and tests; persistence lives in the app module. */
object PlaylistOps {
    const val MAX_NAME = 60

    fun normalizeName(raw: String): String? {
        val n = raw.trim().replace(Regex("\\s+"), " ")
        return n.takeIf { it.isNotEmpty() && it.length <= MAX_NAME && n.none { c -> c.isISOControl() } }
    }

    fun isNameTaken(name: String, existing: Collection<String>, ignoring: String? = null): Boolean =
        existing.any { it.equals(name, ignoreCase = true) && !it.equals(ignoring, ignoreCase = true) }

    fun add(list: List<PlaylistEntry>, e: PlaylistEntry): List<PlaylistEntry> {
        require(e.surah in 1..SurahData.COUNT) { "invalid surah" }
        return list + e
    }

    fun remove(list: List<PlaylistEntry>, id: Long): List<PlaylistEntry> = list.filterNot { it.id == id }

    fun move(list: List<PlaylistEntry>, from: Int, to: Int): List<PlaylistEntry> {
        if (from !in list.indices || to !in list.indices) return list
        return list.toMutableList().apply { add(to, removeAt(from)) }
    }
}
