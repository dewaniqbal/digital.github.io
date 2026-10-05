package app.quran.core.model.playback

import app.quran.core.model.playback.PlaybackEvent.*
import app.quran.core.model.playback.PlaybackStatus.*
import app.quran.core.model.SurahNumber
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PlaybackStateMachineTest {

    private fun run(vararg events: PlaybackEvent, from: PlaybackSnapshot = PlaybackSnapshot()) =
        events.fold(from) { s, e -> PlaybackStateMachine.reduce(s, e) }

    private val net = PlaybackError(PlaybackErrorKind.Network)

    @Test fun `happy path reaches Playing then Completed`() {
        val playing = run(Prepare("1:1"), Ready(60_000))
        assertEquals(Playing, playing.status)
        assertEquals(60_000, playing.durationMs)
        assertEquals(Completed, run(Ended, from = playing).status)
    }

    @Test fun `prepare with playWhenReady false settles Paused`() {
        assertEquals(Paused, run(Prepare("1:1", playWhenReady = false), Ready(10)).status)
    }

    @Test fun `pause during loading is honoured once ready`() {
        assertEquals(Paused, run(Prepare("1:1"), Pause, Ready(10)).status)
    }

    @Test fun `buffering returns to Playing`() {
        val s = run(Prepare("1:1"), Ready(100), BufferingStarted)
        assertEquals(Buffering, s.status)
        assertEquals(Playing, run(Ready(100), from = s).status)
    }

    @Test fun `seek flows through Seeking and restores intent`() {
        val paused = run(Prepare("1:1"), Ready(100), Pause)
        val seeking = run(SeekTo(40), from = paused)
        assertEquals(Seeking, seeking.status)
        assertEquals(40, seeking.positionMs)
        assertEquals(Paused, run(SeekDone, from = seeking).status)
    }

    @Test fun `seek is clamped to duration`() {
        assertEquals(100, run(Prepare("1:1"), Ready(100), SeekTo(9_999)).positionMs)
        assertEquals(0, run(Prepare("1:1"), Ready(100), SeekTo(-5)).positionMs)
    }

    @Test fun `completed can replay from start`() {
        val s = run(Prepare("1:1"), Ready(100), Ended, Play)
        assertEquals(Seeking, s.status)
        assertEquals(0, s.positionMs)
    }

    @Test fun `failure moves to Error and retry returns to Loading`() {
        val err = run(Prepare("1:1"), Failure(net))
        assertEquals(Error, err.status)
        val retry = run(Retry, from = err)
        assertEquals(Loading, retry.status)
        assertEquals(1, retry.retryCount)
        assertNull(retry.error)
    }

    @Test fun `non retryable error ignores Retry`() {
        val s = run(Prepare("1:1"), Failure(PlaybackError(PlaybackErrorKind.Source)), Retry)
        assertEquals(Error, s.status)
    }

    @Test fun `successful ready resets retry count`() {
        assertEquals(0, run(Prepare("1:1"), Failure(net), Retry, Ready(10)).retryCount)
    }

    @Test fun `invalid events are ignored and never throw`() {
        val idle = PlaybackSnapshot()
        assertEquals(idle, run(Play, Pause, SeekTo(5), SeekDone, Ended, Retry, Failure(net)))
    }

    @Test fun `stop resets but keeps speed`() {
        val s = run(SpeedChanged(1.5f), Prepare("1:1"), Ready(10), Stop)
        assertEquals(Idle, s.status)
        assertEquals(1.5f, s.speed)
    }

    @Test fun `speed is clamped`() {
        assertEquals(2f, run(SpeedChanged(9f)).speed)
        assertEquals(0.5f, run(SpeedChanged(0.1f)).speed)
    }

    @Test fun `position updates ignored when not playing`() {
        assertEquals(0, run(Prepare("1:1"), PositionChanged(50)).positionMs)
        assertEquals(50, run(Prepare("1:1"), Ready(100), PositionChanged(50)).positionMs)
    }

    @Test fun `retry policy backs off then suggests next`() {
        val p = RetryPolicy()
        assertEquals(RecoveryAction.RetryAfter(1_000), p.decide(net, 0))
        assertEquals(RecoveryAction.RetryAfter(2_000), p.decide(net, 1))
        assertEquals(RecoveryAction.RetryAfter(4_000), p.decide(net, 2))
        assertTrue(p.decide(net, 3) is RecoveryAction.SuggestNext)
        assertTrue(p.decide(PlaybackError(PlaybackErrorKind.Source), 0) is RecoveryAction.SuggestNext)
    }

    @Test fun `surah number validated`() {
        assertThrows<IllegalArgumentException> { SurahNumber(0) }
        assertThrows<IllegalArgumentException> { SurahNumber(115) }
        assertEquals(114, SurahNumber(114).value)
    }
}
