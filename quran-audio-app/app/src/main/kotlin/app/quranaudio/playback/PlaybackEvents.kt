package app.quranaudio.playback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** One-shot user-facing messages raised by the playback layer (shown as snackbars). */
enum class PlaybackMessage { WIFI_ONLY_BLOCKED, OFFLINE, TRACK_UNAVAILABLE, SLEEP_TIMER_ENDED }

@Singleton
class PlaybackEvents @Inject constructor() {
    private val _messages = MutableSharedFlow<PlaybackMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<PlaybackMessage> = _messages.asSharedFlow()
    fun emit(message: PlaybackMessage) { _messages.tryEmit(message) }
}
