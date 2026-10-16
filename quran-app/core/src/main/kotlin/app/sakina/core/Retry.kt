package app.sakina.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Marks a failure that retrying cannot fix (4xx other than 408/429, validation errors, ...). */
class PermanentFailure(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Bounded exponential backoff. Retries only non-[PermanentFailure] exceptions, never swallows
 * cancellation, and gives up after [maxAttempts] by rethrowing the last error.
 */
suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 500,
    maxDelayMs: Long = 8_000,
    factor: Double = 2.0,
    sleep: suspend (Long) -> Unit = { delay(it) },
    block: suspend (attempt: Int) -> T,
): T {
    require(maxAttempts >= 1)
    var wait = initialDelayMs
    var last: Exception? = null
    for (attempt in 1..maxAttempts) {
        try {
            return block(attempt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: PermanentFailure) {
            throw e
        } catch (e: Exception) {
            last = e
            if (attempt == maxAttempts) break
            sleep(wait)
            wait = (wait * factor).toLong().coerceAtMost(maxDelayMs)
        }
    }
    throw last!!
}
