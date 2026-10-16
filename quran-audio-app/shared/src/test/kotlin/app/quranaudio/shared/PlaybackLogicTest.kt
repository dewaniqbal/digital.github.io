package app.quranaudio.shared

import app.quranaudio.shared.playback.AudioMix
import app.quranaudio.shared.playback.ResumePolicy
import app.quranaudio.shared.playback.SleepTimerOption
import app.quranaudio.shared.playback.SleepTimerState
import app.quranaudio.shared.playback.formatDuration
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackLogicTest {

    @Test fun `duration timer counts down on the monotonic clock`() {
        val state = SleepTimerState.start(SleepTimerOption.Duration(15), nowElapsedMs = 1_000)
        assertThat(state.remainingMs(1_000)).isEqualTo(15 * 60_000L)
        assertThat(state.remainingMs(1_000 + 60_000)).isEqualTo(14 * 60_000L)
        assertThat(state.isExpired(1_000 + 15 * 60_000L - 1)).isFalse()
        assertThat(state.isExpired(1_000 + 15 * 60_000L)).isTrue()
        assertThat(state.remainingMs(Long.MAX_VALUE / 2)).isEqualTo(0)
    }

    @Test fun `end of surah timer has no deadline`() {
        val state = SleepTimerState.start(SleepTimerOption.EndOfSurah, 0)
        assertThat(state.remainingMs(10)).isNull()
        assertThat(state.isExpired(Long.MAX_VALUE)).isFalse()
    }

    @Test fun `custom timer bounds are enforced`() {
        assertThat(runCatching { SleepTimerOption.Duration(0) }.isFailure).isTrue()
        assertThat(runCatching { SleepTimerOption.Duration(721) }.isFailure).isTrue()
        assertThat(SleepTimerOption.PRESET_MINUTES).containsExactly(5, 10, 15, 30, 45, 60).inOrder()
    }

    @Test fun `formats durations`() {
        assertThat(formatDuration(74_000)).isEqualTo("01:14")
        assertThat(formatDuration(20 * 60_000L + 22_000)).isEqualTo("20:22")
        assertThat(formatDuration(3_725_000)).isEqualTo("1:02:05")
        assertThat(formatDuration(-5)).isEqualTo("00:00")
    }

    @Test fun `background never exceeds half of quran gain`() {
        for (q in listOf(0.1f, 0.3f, 0.5f, 0.8f, 1f)) for (b in listOf(0f, 0.2f, 0.5f, 1f)) {
            assertThat(AudioMix.backgroundGain(b, q)).isAtMost(AudioMix.quranGain(q) * AudioMix.MAX_BACKGROUND_RELATIVE + 1e-6f)
        }
        assertThat(AudioMix.backgroundGain(0f, 1f)).isEqualTo(0f)
        assertThat(AudioMix.quranGain(1f)).isEqualTo(1f)
    }

    @Test fun `defaults keep background quiet and gains bounded`() {
        assertThat(AudioMix.DEFAULT_BACKGROUND_VOLUME).isAtLeast(0.2f)
        assertThat(AudioMix.DEFAULT_BACKGROUND_VOLUME).isAtMost(0.3f)
        assertThat(AudioMix.quranGain(5f)).isEqualTo(1f)
        assertThat(AudioMix.quranGain(-1f)).isEqualTo(0f)
        assertThat(AudioMix.backgroundGain(1f, 0f)).isAtMost(0.25f)
    }

    @Test fun `fade curves are monotonic and bounded`() {
        assertThat(AudioMix.fadeIn(0f)).isEqualTo(0f)
        assertThat(AudioMix.fadeIn(1f)).isWithin(1e-6f).of(1f)
        assertThat(AudioMix.fadeIn(0.5f)).isGreaterThan(AudioMix.fadeIn(0.25f))
        assertThat(AudioMix.fadeOut(1f)).isWithin(1e-6f).of(0f)
    }

    @Test fun `resume policy`() {
        assertThat(ResumePolicy.resumePositionMs(3_000, 600_000)).isEqualTo(0)
        assertThat(ResumePolicy.resumePositionMs(120_000, 600_000)).isEqualTo(118_000)
        assertThat(ResumePolicy.resumePositionMs(595_000, 600_000)).isEqualTo(0)
        assertThat(ResumePolicy.resumePositionMs(120_000, null)).isEqualTo(118_000)
        assertThat(ResumePolicy.progress(300_000, 600_000)).isEqualTo(0.5f)
        assertThat(ResumePolicy.progress(300_000, null)).isNull()
        assertThat(ResumePolicy.isCompleted(599_000, 600_000)).isTrue()
    }
}
