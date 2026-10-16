package app.quranaudio.shared.playback

/** Decides where to resume a previously played Surah. */
object ResumePolicy {
    /** Positions this close to the start are treated as "not started". */
    const val MIN_RESUME_MS = 5_000L
    /** Positions this close to the end are treated as "finished" and restart from the beginning. */
    const val END_THRESHOLD_MS = 10_000L
    /** Rewind a little so the listener regains context after a break. */
    const val REWIND_ON_RESUME_MS = 2_000L

    fun resumePositionMs(savedPositionMs: Long, durationMs: Long?): Long {
        if (savedPositionMs < MIN_RESUME_MS) return 0L
        if (durationMs != null && durationMs > 0 && savedPositionMs >= durationMs - END_THRESHOLD_MS) return 0L
        return (savedPositionMs - REWIND_ON_RESUME_MS).coerceAtLeast(0L)
    }

    /** Progress 0…1 for "Continue listening" cards; null when the duration is unknown. */
    fun progress(positionMs: Long, durationMs: Long?): Float? {
        if (durationMs == null || durationMs <= 0) return null
        return (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    }

    fun isCompleted(positionMs: Long, durationMs: Long?): Boolean =
        durationMs != null && durationMs > 0 && positionMs >= durationMs - END_THRESHOLD_MS
}
