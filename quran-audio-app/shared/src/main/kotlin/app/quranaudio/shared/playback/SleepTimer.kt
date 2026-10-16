package app.quranaudio.shared.playback

/** What the user chose in the sleep-timer sheet. */
sealed interface SleepTimerOption {
    /** Stop after a fixed duration (presets or a custom value). */
    data class Duration(val minutes: Int) : SleepTimerOption {
        init {
            require(minutes in MIN_MINUTES..MAX_MINUTES) { "Sleep timer must be $MIN_MINUTES..$MAX_MINUTES minutes" }
        }
    }

    /** Stop when the currently playing Surah finishes. */
    data object EndOfSurah : SleepTimerOption

    companion object {
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 12 * 60
        val PRESET_MINUTES = listOf(5, 10, 15, 30, 45, 60)
    }
}

/**
 * Immutable sleep-timer state. Times use a monotonic clock (`SystemClock.elapsedRealtime()` on
 * Android) so that changing the wall clock or time zone cannot shorten or extend the timer.
 */
data class SleepTimerState(
    val option: SleepTimerOption,
    /** Monotonic time at which playback must stop; null for [SleepTimerOption.EndOfSurah]. */
    val deadlineElapsedMs: Long?,
) {
    fun remainingMs(nowElapsedMs: Long): Long? = deadlineElapsedMs?.let { (it - nowElapsedMs).coerceAtLeast(0) }

    fun isExpired(nowElapsedMs: Long): Boolean = deadlineElapsedMs != null && nowElapsedMs >= deadlineElapsedMs

    companion object {
        fun start(option: SleepTimerOption, nowElapsedMs: Long): SleepTimerState = when (option) {
            is SleepTimerOption.Duration -> SleepTimerState(option, nowElapsedMs + option.minutes * 60_000L)
            SleepTimerOption.EndOfSurah -> SleepTimerState(option, null)
        }
    }
}

/** "mm:ss" or "h:mm:ss" for countdowns and progress labels. */
fun formatDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0) + 500) / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
