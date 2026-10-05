package app.quran.core.model.playback

/** What the service should do after a playback failure. Never crash; always give the user a way forward. */
public sealed interface RecoveryAction {
    public data class RetryAfter(val delayMs: Long) : RecoveryAction
    /** Retries exhausted or non-retryable: surface error and suggest the next Surah. */
    public data class SuggestNext(val error: PlaybackError) : RecoveryAction
}

public class RetryPolicy(
    private val maxAttempts: Int = 3,
    private val baseDelayMs: Long = 1_000,
    private val maxDelayMs: Long = 8_000,
) {
    init { require(maxAttempts >= 0 && baseDelayMs > 0 && maxDelayMs >= baseDelayMs) }

    public fun decide(error: PlaybackError, attemptsSoFar: Int): RecoveryAction =
        if (error.retryable && attemptsSoFar < maxAttempts) {
            RecoveryAction.RetryAfter((baseDelayMs shl attemptsSoFar.coerceAtMost(20)).coerceAtMost(maxDelayMs))
        } else {
            RecoveryAction.SuggestNext(error)
        }
}
