package app.quran.core.model.playback

import app.quran.core.model.playback.PlaybackStatus.*

/**
 * Pure, deterministic reducer. The ExoPlayer listener translates player callbacks into
 * [PlaybackEvent]s; the service publishes the resulting [PlaybackSnapshot] as a StateFlow.
 * Events that are invalid for the current state are ignored (never throw), so a late or
 * duplicated player callback can never crash playback.
 */
public object PlaybackStateMachine {

    public fun reduce(s: PlaybackSnapshot, e: PlaybackEvent): PlaybackSnapshot = when (e) {
        is PlaybackEvent.Prepare -> PlaybackSnapshot(
            status = Loading,
            trackKey = e.trackKey,
            positionMs = e.startPositionMs.coerceAtLeast(0),
            speed = s.speed,
            playWhenReady = e.playWhenReady,
        )
        PlaybackEvent.Stop -> PlaybackSnapshot(speed = s.speed)
        is PlaybackEvent.SpeedChanged -> s.copy(speed = e.speed.coerceIn(MIN_SPEED, MAX_SPEED))
        is PlaybackEvent.PositionChanged ->
            if (s.status == Playing || s.status == Paused || s.status == Buffering) {
                s.copy(positionMs = e.positionMs.clampToDuration(s.durationMs))
            } else s
        is PlaybackEvent.Failure ->
            if (s.status == Idle) s else s.copy(status = Error, error = e.error)
        PlaybackEvent.Retry ->
            if (s.status == Error && s.error?.retryable == true) {
                s.copy(status = Loading, error = null, retryCount = s.retryCount + 1)
            } else s
        PlaybackEvent.Play -> play(s)
        PlaybackEvent.Pause -> pause(s)
        is PlaybackEvent.SeekTo -> seek(s, e.positionMs)
        PlaybackEvent.SeekDone -> if (s.status == Seeking) settle(s) else s
        PlaybackEvent.BufferingStarted ->
            if (s.status in setOf(Loading, Playing, Paused, Seeking)) s.copy(status = Buffering) else s
        is PlaybackEvent.Ready ->
            if (s.status == Loading || s.status == Buffering) {
                settle(s.copy(durationMs = e.durationMs.coerceAtLeast(0), retryCount = 0))
            } else s.copy(durationMs = e.durationMs.coerceAtLeast(0))
        PlaybackEvent.Ended -> if (s.status == Playing || s.status == Buffering) s.copy(status = Completed, positionMs = s.durationMs) else s
    }

    private fun play(s: PlaybackSnapshot): PlaybackSnapshot = when (s.status) {
        Paused -> s.copy(status = Playing, playWhenReady = true)
        Loading, Buffering, Seeking -> s.copy(playWhenReady = true)
        Completed -> s.copy(status = Seeking, positionMs = 0, playWhenReady = true) // replay
        else -> s
    }

    private fun pause(s: PlaybackSnapshot): PlaybackSnapshot = when (s.status) {
        Playing -> s.copy(status = Paused, playWhenReady = false)
        Loading, Buffering, Seeking -> s.copy(playWhenReady = false)
        else -> s
    }

    private fun seek(s: PlaybackSnapshot, positionMs: Long): PlaybackSnapshot = when (s.status) {
        Playing, Paused, Buffering, Seeking, Completed ->
            s.copy(status = Seeking, positionMs = positionMs.clampToDuration(s.durationMs))
        else -> s
    }

    private fun settle(s: PlaybackSnapshot): PlaybackSnapshot =
        s.copy(status = if (s.playWhenReady) Playing else Paused)

    private fun Long.clampToDuration(durationMs: Long): Long =
        if (durationMs > 0) coerceIn(0, durationMs) else coerceAtLeast(0)

    public const val MIN_SPEED: Float = 0.5f
    public const val MAX_SPEED: Float = 2.0f
}
