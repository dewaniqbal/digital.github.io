package app.sakina.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {
    private fun TestScope.timer(onExpire: () -> Unit) =
        SleepTimer(backgroundScope, onExpire, { currentTime }, tickMs = 1_000)

    @Test fun countdownFiresOnceAtDeadline() = runTest {
        var fired = 0
        val t = timer { fired++ }
        t.startCountdown(5_000)
        advanceTimeBy(4_999); runCurrent()
        assertEquals(0, fired)
        assertTrue(t.state.value is SleepTimerState.Countdown)
        advanceTimeBy(2); runCurrent()
        assertEquals(1, fired)
        assertEquals(SleepTimerState.Off, t.state.value)
        advanceTimeBy(60_000); runCurrent()
        assertEquals(1, fired)
    }

    @Test fun cancelPreventsExpiry() = runTest {
        var fired = 0
        val t = timer { fired++ }
        t.startCountdown(3_000)
        advanceTimeBy(1_000); t.cancel(); advanceTimeBy(10_000); runCurrent()
        assertEquals(0, fired)
        assertEquals(SleepTimerState.Off, t.state.value)
    }

    @Test fun restartReplacesPreviousCountdown() = runTest {
        var fired = 0
        val t = timer { fired++ }
        t.startCountdown(2_000)
        advanceTimeBy(1_000)
        t.startCountdown(5_000)
        advanceTimeBy(4_000); runCurrent()
        assertEquals(0, fired)
        advanceTimeBy(1_500); runCurrent()
        assertEquals(1, fired)
    }

    @Test fun endOfSurahModeStopsOnlyWhenSurahEnds() = runTest {
        var fired = 0
        val t = timer { fired++ }
        t.stopAtEndOfSurah()
        advanceTimeBy(3_600_000); runCurrent()
        assertEquals(0, fired)
        assertTrue(t.onSurahEnded())
        assertEquals(1, fired)
        assertFalse(t.onSurahEnded(), "only once")
    }

    @Test fun surahEndedIgnoredWhenCountdownRunning() = runTest {
        val t = timer { }
        t.startCountdown(10_000)
        assertFalse(t.onSurahEnded())
        t.cancel()
    }
}
