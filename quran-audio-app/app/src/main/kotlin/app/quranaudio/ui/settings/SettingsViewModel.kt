package app.quranaudio.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quranaudio.data.prefs.Settings
import app.quranaudio.data.prefs.ThemeMode
import app.quranaudio.data.prefs.UserPreferences
import app.quranaudio.data.repository.CatalogRepository
import app.quranaudio.domain.AudioSourceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferences,
    private val player: app.quranaudio.playback.PlaybackConnection,
    catalog: CatalogRepository,
) : ViewModel() {

    val settings: StateFlow<Settings> = prefs.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())
    val sources: StateFlow<List<AudioSourceInfo>> = catalog.sources.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cacheBytes = MutableStateFlow(0L)
    val cacheBytes: StateFlow<Long> = _cacheBytes.asStateFlow()

    init { refreshCacheSize() }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(mode) }
    fun setAutoNext(on: Boolean) = viewModelScope.launch { prefs.setAutoPlayNext(on) }
    fun setWifiOnly(on: Boolean) = viewModelScope.launch { prefs.setWifiOnly(on) }
    fun setSpeed(speed: Float) = viewModelScope.launch {
        prefs.setPlaybackSpeed(speed)
        player.setSpeed(speed)
    }
    fun setStopBackgroundWithQuran(on: Boolean) = viewModelScope.launch { prefs.setStopBackgroundWithQuran(on) }

    /** Clears regenerable caches (artwork, HTTP). User data and the catalogue are kept. */
    fun clearCache() = viewModelScope.launch {
        withContext(Dispatchers.IO) { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
        refreshCacheSize()
    }

    private fun refreshCacheSize() = viewModelScope.launch {
        _cacheBytes.value = withContext(Dispatchers.IO) { sizeOf(context.cacheDir) }
    }

    private fun sizeOf(f: File): Long = if (f.isDirectory) f.listFiles()?.sumOf(::sizeOf) ?: 0 else f.length()
}
