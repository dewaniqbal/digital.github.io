package app.quranaudio.playback

import android.os.SystemClock
import app.quranaudio.di.ApplicationScope
import app.quranaudio.shared.playback.SleepTimerOption
import app.quranaudio.shared.playback.SleepTimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sleep timer. Uses the monotonic clock and a single suspended coroutine (no polling loop):
 * the coroutine sleeps until the deadline, then emits [expired]. The playback service reacts by
 * fading out and stopping both the Quran and the ambient sound.
 *
 * "End of Surah" is implemented by the service via ExoPlayer's pause-at-end-of-item.
 */
@Singleton
class SleepTimerController @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<SleepTimerState?>(null)
    val state: StateFlow<SleepTimerState?> = _state.asStateFlow()

    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()

    private var job: Job? = null

    fun start(option: SleepTimerOption) {
        job?.cancel()
        val state = SleepTimerState.start(option, SystemClock.elapsedRealtime())
        _state.value = state
        val deadline = state.deadlineElapsedMs ?: return
        job = scope.launch(Dispatchers.Main) {
            delay((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0))
            fire()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = null
    }

    /** Called by the service when the end-of-Surah pause happened. */
    internal fun fire() {
        job = null
        _state.value = null
        _expired.tryEmit(Unit)
    }

    val isEndOfSurah: Boolean get() = _state.value?.option == SleepTimerOption.EndOfSurah

    fun remainingMs(): Long? = _state.value?.remainingMs(SystemClock.elapsedRealtime())
}
