package app.quranaudio.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quranaudio.data.prefs.UserPreferences
import app.quranaudio.data.repository.LibraryRepository
import app.quranaudio.playback.AmbientSound
import app.quranaudio.playback.AmbientSoundController
import app.quranaudio.playback.PlaybackConnection
import app.quranaudio.playback.PlaybackProgress
import app.quranaudio.playback.PlayerUiState
import app.quranaudio.playback.SleepTimerController
import app.quranaudio.shared.playback.SleepTimerOption
import app.quranaudio.shared.playback.SleepTimerState
import app.quranaudio.ui.common.ref
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MixUiState(
    val quranVolume: Float = 1f,
    val backgroundVolume: Float = 0.25f,
    val stopWithQuran: Boolean = true,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val connection: PlaybackConnection,
    private val library: LibraryRepository,
    private val sleepTimer: SleepTimerController,
    private val ambient: AmbientSoundController,
    private val prefs: UserPreferences,
) : ViewModel() {

    val player: StateFlow<PlayerUiState> = connection.state
    val progress: Flow<PlaybackProgress> = connection.progress
    val sleep: StateFlow<SleepTimerState?> = sleepTimer.state
    val ambientState: StateFlow<AmbientSoundController.State> = ambient.state

    val isFavourite: StateFlow<Boolean> = combine(connection.state, library.favouriteTrackKeys) { s, keys -> s.track?.key in keys }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val mix: StateFlow<MixUiState> = prefs.settings.map { MixUiState(it.quranVolume, it.backgroundVolume, it.stopBackgroundWithQuran) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MixUiState())

    init { connection.connectInBackground() }

    fun togglePlay() = connection.togglePlayPause()
    fun next() = connection.next()
    fun previous() = connection.previous()
    fun seekTo(ms: Long) = connection.seekTo(ms)
    fun seekBy(ms: Long) = connection.seekBy(ms)
    fun cycleRepeat() = connection.cycleRepeat()
    fun toggleShuffle() = connection.setShuffle(!player.value.shuffle)
    fun setSpeed(speed: Float) = connection.setSpeed(speed)
    fun skipTo(index: Int) = connection.skipTo(index)
    fun removeFromQueue(index: Int) = connection.removeFromQueue(index)
    fun moveInQueue(from: Int, to: Int) = connection.moveInQueue(from, to)
    fun retry() = connection.retry()
    fun skipFailed() = connection.skipFailed()

    fun toggleFavourite() {
        val track = player.value.track ?: return
        viewModelScope.launch { library.toggleFavouriteTrack(track.ref()) }
    }

    fun startSleepTimer(option: SleepTimerOption) = sleepTimer.start(option)
    fun cancelSleepTimer() = sleepTimer.cancel()
    fun sleepRemainingMs(): Long? = sleepTimer.remainingMs()

    fun selectAmbient(sound: AmbientSound) = ambient.select(sound)
    fun ambientOff() = ambient.turnOff()
    fun toggleAmbient() = ambient.toggle()
    fun setQuranVolume(v: Float) = viewModelScope.launch { prefs.setQuranVolume(v) }
    fun setBackgroundVolume(v: Float) = viewModelScope.launch { prefs.setBackgroundVolume(v) }
    fun setStopWithQuran(on: Boolean) = viewModelScope.launch { prefs.setStopBackgroundWithQuran(on) }
}
