package app.quran.core.model.playback

/** Idle → Loading → Buffering → Playing → Paused → Seeking → Completed → Error */
public enum class PlaybackStatus { Idle, Loading, Buffering, Playing, Paused, Seeking, Completed, Error }

public enum class PlaybackErrorKind { Network, Source, Unknown }

public data class PlaybackError(val kind: PlaybackErrorKind, val retryable: Boolean = kind != PlaybackErrorKind.Source)

public data class PlaybackSnapshot(
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val trackKey: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    /** User intent: should audio run once ready. Survives Loading/Buffering/Seeking. */
    val playWhenReady: Boolean = true,
    val error: PlaybackError? = null,
    val retryCount: Int = 0,
)

public sealed interface PlaybackEvent {
    public data class Prepare(val trackKey: String, val startPositionMs: Long = 0, val playWhenReady: Boolean = true) : PlaybackEvent
    public data object BufferingStarted : PlaybackEvent
    public data class Ready(val durationMs: Long) : PlaybackEvent
    public data object Play : PlaybackEvent
    public data object Pause : PlaybackEvent
    public data class SeekTo(val positionMs: Long) : PlaybackEvent
    public data object SeekDone : PlaybackEvent
    public data class PositionChanged(val positionMs: Long) : PlaybackEvent
    public data class SpeedChanged(val speed: Float) : PlaybackEvent
    public data object Ended : PlaybackEvent
    public data class Failure(val error: PlaybackError) : PlaybackEvent
    public data object Retry : PlaybackEvent
    public data object Stop : PlaybackEvent
}
