package app.sakina.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SleepTimerState {
    data object Off : SleepTimerState
    data class Countdown(val remainingMs: Long) : SleepTimerState
    /** Stop when the current surah finishes. */
    data object EndOfSurah : SleepTimerState
}

/**
 * Sleep timer. [onExpire] is invoked exactly once per armed timer. The countdown is driven by an
 * injectable [monotonicNowMs] so tests (and process resumption) never depend on wall-clock jumps.
 */
class SleepTimer(
    private val scope: CoroutineScope,
    private val onExpire: () -> Unit,
    private val monotonicNowMs: () -> Long,
    private val tickMs: Long = 1_000,
) {
    private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()
    private var job: Job? = null

    fun startCountdown(durationMs: Long) {
        require(durationMs > 0) { "duration must be positive" }
        cancel()
        val deadline = monotonicNowMs() + durationMs
        _state.value = SleepTimerState.Countdown(durationMs)
        job = scope.launch {
            while (true) {
                val left = deadline - monotonicNowMs()
                if (left <= 0) break
                _state.value = SleepTimerState.Countdown(left)
                delay(minOf(tickMs, left))
            }
            _state.value = SleepTimerState.Off
            onExpire()
        }
    }

    fun stopAtEndOfSurah() {
        cancel()
        _state.value = SleepTimerState.EndOfSurah
    }

    /** Call when a surah finishes naturally. Returns true if playback must stop now. */
    fun onSurahEnded(): Boolean {
        if (_state.value != SleepTimerState.EndOfSurah) return false
        _state.value = SleepTimerState.Off
        onExpire()
        return true
    }

    fun cancel() {
        job?.cancel(); job = null
        _state.value = SleepTimerState.Off
    }
}
