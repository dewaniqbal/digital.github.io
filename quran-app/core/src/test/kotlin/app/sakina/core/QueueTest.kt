package app.sakina.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueueTest {
    private fun item(uid: Long, surah: Int = uid.toInt()) = QueueItem(uid, "r", "r-1", surah)
    private fun q(n: Int, repeat: RepeatMode = RepeatMode.OFF, shuffle: Boolean = false) =
        PlaybackQueue((1L..n).map { item(it) }, 0, repeat, shuffle, Random(42))

    @Test fun nextAndPreviousWalkTheQueue() {
        val q = q(3)
        assertEquals(1L, q.current?.uid)
        assertEquals(2L, q.next()?.uid)
        assertEquals(3L, q.next()?.uid)
        assertNull(q.next())
        assertEquals(3L, q.current?.uid, "stays on last item when next unavailable")
        assertEquals(2L, q.previous()?.uid)
    }

    @Test fun endedAdvancesThenStops() {
        val q = q(2)
        assertEquals(2L, q.onItemEnded()?.uid)
        assertNull(q.onItemEnded())
    }

    @Test fun repeatOneReplaysOnEndButUserNextAdvances() {
        val q = q(2, RepeatMode.ONE)
        assertEquals(1L, q.onItemEnded()?.uid)
        assertEquals(2L, q.next()?.uid)
    }

    @Test fun repeatAllWraps() {
        val q = q(2, RepeatMode.ALL)
        q.next(); assertEquals(1L, q.onItemEnded()?.uid)
        assertEquals(2L, q.previous()?.uid, "previous wraps to last under repeat-all")
    }

    @Test fun shuffleVisitsEveryItemOnceStartingAtCurrent() {
        val q = q(8, shuffle = true)
        val seen = mutableListOf(q.current!!.uid)
        while (true) seen += q.onItemEnded()?.uid ?: break
        assertEquals(1L, seen.first())
        assertEquals((1L..8L).toSet(), seen.toSet())
        assertEquals(8, seen.size)
    }

    @Test fun shuffleOffRestoresNaturalOrderFromCurrent() {
        val q = q(5, shuffle = true)
        q.next()
        q.setShuffle(false)
        val cur = q.current!!.uid
        assertEquals(cur + 1, q.next()?.uid ?: (cur + 1))
    }

    @Test fun removeCurrentMovesToFollowingItem() {
        val q = q(3)
        q.next() // 2
        assertTrue(q.remove(2))
        assertEquals(3L, q.current?.uid)
        assertEquals(listOf(1L, 3L), q.items.map { it.uid })
    }

    @Test fun removeLastRemainingEmptiesQueue() {
        val q = q(1)
        q.remove(1)
        assertNull(q.current)
        assertTrue(q.items.isEmpty())
    }

    @Test fun playNextInsertsAfterCurrentWithoutChangingCurrent() {
        val q = q(3)
        q.playNext(item(10))
        assertEquals(listOf(1L, 10L, 2L, 3L), q.items.map { it.uid })
        assertEquals(10L, q.next()?.uid)
    }

    @Test fun moveKeepsCurrentItem() {
        val q = q(4)
        q.next()
        q.move(0, 3)
        assertEquals(2L, q.current?.uid)
        assertEquals(listOf(2L, 3L, 4L, 1L), q.items.map { it.uid })
        assertEquals(3L, q.next()?.uid)
    }

    @Test fun jumpToAndDuplicateGuard() {
        val q = q(3)
        assertEquals(3L, q.jumpTo(3)?.uid)
        assertNull(q.jumpTo(99))
        runCatching { q.add(item(3)) }.let { assertTrue(it.isFailure) }
    }
}
