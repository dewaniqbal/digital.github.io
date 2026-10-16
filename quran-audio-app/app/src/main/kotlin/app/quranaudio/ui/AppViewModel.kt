package app.quranaudio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quranaudio.data.prefs.Settings
import app.quranaudio.data.prefs.UserPreferences
import app.quranaudio.playback.PlaybackConnection
import app.quranaudio.playback.PlaybackEvents
import app.quranaudio.playback.PlaybackMessage
import app.quranaudio.playback.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** App-wide state for the shell: settings (theme/onboarding), mini player, playback messages. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val connection: PlaybackConnection,
    events: PlaybackEvents,
) : ViewModel() {
    /** Null until DataStore has been read (keeps the splash screen up, avoids a flash). */
    val settings: StateFlow<Settings?> = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val player: StateFlow<PlayerUiState> = connection.state
    val progressFraction: Flow<Float?> = connection.progress.map { p -> p.durationMs?.let { d -> (p.positionMs.toFloat() / d).coerceIn(0f, 1f) } }
    val messages: SharedFlow<PlaybackMessage> = events.messages

    init { connection.connectInBackground() }

    fun finishOnboarding() = viewModelScope.launch { prefs.setOnboardingDone() }
    fun togglePlay() = connection.togglePlayPause()
    fun next() = connection.next()

}
