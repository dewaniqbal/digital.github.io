package app.sakina.core

import kotlin.random.Random

enum class RepeatMode { OFF, ONE, ALL }

/** One queue entry: a surah by a specific recitation. [uid] makes duplicates distinguishable. */
data class QueueItem(
    val uid: Long,
    val reciterId: String,
    val recitationId: String,
    val surah: Int,
)

/**
 * Pure queue/transition logic, independent of the player, so it can be unit tested. The Android
 * layer mirrors this state into Media3; Media3 remains the source of truth during playback.
 * Shuffle keeps a stable permutation, so previous/next are consistent and nothing repeats until
 * the whole queue has been played.
 */
class PlaybackQueue(
    items: List<QueueItem> = emptyList(),
    startIndex: Int = 0,
    var repeat: RepeatMode = RepeatMode.OFF,
    shuffle: Boolean = false,
    private val random: Random = Random.Default,
) {
    private val _items = items.toMutableList()
    val items: List<QueueItem> get() = _items.toList()
    var currentUid: Long? = items.getOrNull(startIndex)?.uid
        private set
    var shuffle: Boolean = false
        private set
    private var order: MutableList<Long> = mutableListOf()

    init { setShuffle(shuffle) }

    val current: QueueItem? get() = _items.firstOrNull { it.uid == currentUid }
    val size: Int get() = _items.size

    private fun rebuildOrder() {
        val uids = _items.map { it.uid }
        order = if (shuffle) {
            val rest = uids.filter { it != currentUid }.shuffled(random)
            (listOfNotNull(currentUid) + rest).toMutableList()
        } else uids.toMutableList()
    }

    fun setShuffle(on: Boolean) { shuffle = on; rebuildOrder() }

    fun hasNext(): Boolean = nextUid() != null
    fun hasPrevious(): Boolean = current != null && (repeat == RepeatMode.ALL || order.indexOf(currentUid) > 0)

    private fun nextUid(natural: Boolean = false): Long? {
        val i = order.indexOf(currentUid)
        if (i < 0) return null
        return when {
            !natural || repeat != RepeatMode.ONE -> order.getOrNull(i + 1)
                ?: if (repeat == RepeatMode.ALL && order.isNotEmpty()) order.first() else null
            else -> currentUid
        }
    }

    /** User pressed "next". Wraps only under repeat-all. */
    fun next(): QueueItem? = nextUid()?.also { currentUid = it }?.let { current }

    /** User pressed "previous". */
    fun previous(): QueueItem? {
        val i = order.indexOf(currentUid)
        val target = when {
            i > 0 -> order[i - 1]
            repeat == RepeatMode.ALL && order.isNotEmpty() -> order.last()
            else -> return null
        }
        currentUid = target
        return current
    }

    /** Current item finished by itself. Repeat-one replays; returns null when the queue ends. */
    fun onItemEnded(): QueueItem? = nextUid(natural = true)?.also { currentUid = it }?.let { current }

    fun jumpTo(uid: Long): QueueItem? {
        if (_items.none { it.uid == uid }) return null
        currentUid = uid
        if (shuffle) rebuildOrder()
        return current
    }

    fun add(item: QueueItem) {
        require(_items.none { it.uid == item.uid }) { "duplicate uid" }
        _items += item
        if (currentUid == null) currentUid = item.uid
        order = if (shuffle) (order + item.uid).toMutableList() else _items.map { it.uid }.toMutableList()
    }

    fun playNext(item: QueueItem) {
        require(_items.none { it.uid == item.uid }) { "duplicate uid" }
        val at = _items.indexOfFirst { it.uid == currentUid }.let { if (it < 0) _items.size else it + 1 }
        _items.add(at, item)
        if (currentUid == null) currentUid = item.uid
        val oi = order.indexOf(currentUid).let { if (it < 0) order.size else it + 1 }
        if (shuffle) order.add(oi, item.uid) else order = _items.map { it.uid }.toMutableList()
    }

    /** Removes an item. If it was current, playback moves to the following item (or ends). */
    fun remove(uid: Long): Boolean {
        val idx = _items.indexOfFirst { it.uid == uid }
        if (idx < 0) return false
        val following = if (uid == currentUid) nextUidAfterRemoval(uid) else currentUid
        _items.removeAt(idx)
        order.remove(uid)
        currentUid = following
        return true
    }

    private fun nextUidAfterRemoval(uid: Long): Long? {
        val i = order.indexOf(uid)
        return order.getOrNull(i + 1) ?: order.getOrNull(i - 1)
    }

    /** Reorders the visible list (user drag). Shuffle order is rebuilt around the current item. */
    fun move(from: Int, to: Int) {
        if (from !in _items.indices || to !in _items.indices || from == to) return
        _items.add(to, _items.removeAt(from))
        if (!shuffle) order = _items.map { it.uid }.toMutableList()
    }

    fun clear() { _items.clear(); order.clear(); currentUid = null }
}
