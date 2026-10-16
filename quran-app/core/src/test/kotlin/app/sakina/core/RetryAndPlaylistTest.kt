package app.sakina.core

import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RetryAndPlaylistTest {
    @Test fun retriesTransientThenSucceeds() = runTest {
        val waits = mutableListOf<Long>()
        var calls = 0
        val r = retryWithBackoff(maxAttempts = 4, initialDelayMs = 100, sleep = { waits += it }) {
            if (++calls < 3) throw IOException("flaky") else "ok"
        }
        assertEquals("ok", r)
        assertEquals(listOf(100L, 200L), waits)
    }

    @Test fun givesUpAfterMaxAttemptsWithLastError() = runTest {
        var calls = 0
        val e = assertFailsWith<IOException> {
            retryWithBackoff(maxAttempts = 3, sleep = {}) { calls++; throw IOException("down $calls") }
        }
        assertEquals(3, calls)
        assertEquals("down 3", e.message)
    }

    @Test fun permanentFailureIsNotRetried() = runTest {
        var calls = 0
        assertFailsWith<PermanentFailure> {
            retryWithBackoff(sleep = {}) { calls++; throw PermanentFailure("404") }
        }
        assertEquals(1, calls)
    }

    @Test fun backoffIsCapped() = runTest {
        val waits = mutableListOf<Long>()
        runCatching {
            retryWithBackoff(maxAttempts = 6, initialDelayMs = 1_000, maxDelayMs = 3_000, sleep = { waits += it }) { throw IOException() }
        }
        assertEquals(listOf(1000L, 2000L, 3000L, 3000L, 3000L), waits)
    }

    @Test fun playlistNameRules() {
        assertEquals("My Night List", PlaylistOps.normalizeName("  My   Night List "))
        assertNull(PlaylistOps.normalizeName("   "))
        assertNull(PlaylistOps.normalizeName("x".repeat(61)))
        assertEquals(true, PlaylistOps.isNameTaken("MY LIST", listOf("my list")))
        assertEquals(false, PlaylistOps.isNameTaken("my list", listOf("my list"), ignoring = "My List"))
    }

    @Test fun playlistAddRemoveMove() {
        var l = emptyList<PlaylistEntry>()
        l = PlaylistOps.add(l, PlaylistEntry(1, 36, null))
        l = PlaylistOps.add(l, PlaylistEntry(2, 67, "r1"))
        l = PlaylistOps.add(l, PlaylistEntry(3, 18, null))
        l = PlaylistOps.move(l, 2, 0)
        assertEquals(listOf(3L, 1L, 2L), l.map { it.id })
        l = PlaylistOps.remove(l, 1)
        assertEquals(listOf(3L, 2L), l.map { it.id })
        assertEquals(l, PlaylistOps.move(l, 5, 0))
        assertFailsWith<IllegalArgumentException> { PlaylistOps.add(l, PlaylistEntry(9, 115, null)) }
    }
}
