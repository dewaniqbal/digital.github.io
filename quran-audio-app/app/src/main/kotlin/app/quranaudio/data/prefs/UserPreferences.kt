package app.quranaudio.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.quranaudio.shared.playback.AudioMix
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, DARK, LIGHT }
enum class RepeatSetting { OFF, ONE, ALL }

data class Settings(
    val onboardingDone: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val playbackSpeed: Float = 1f,
    val autoPlayNext: Boolean = true,
    val repeat: RepeatSetting = RepeatSetting.OFF,
    val shuffle: Boolean = false,
    val wifiOnly: Boolean = false,
    val quranVolume: Float = AudioMix.DEFAULT_QURAN_VOLUME,
    val backgroundVolume: Float = AudioMix.DEFAULT_BACKGROUND_VOLUME,
    /** Last selected ambient sound; selection persists, playback never auto-starts. */
    val backgroundSound: String? = null,
    val stopBackgroundWithQuran: Boolean = true,
    val lastReciterId: String? = null,
    val catalogEtag: String? = null,
    val catalogFetchedAtMs: Long = 0,
    val lastSessionJson: String? = null,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val store = context.dataStore

    private object K {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val themeMode = stringPreferencesKey("theme_mode")
        val speed = floatPreferencesKey("playback_speed")
        val autoNext = booleanPreferencesKey("auto_play_next")
        val repeat = stringPreferencesKey("repeat")
        val shuffle = booleanPreferencesKey("shuffle")
        val wifiOnly = booleanPreferencesKey("wifi_only")
        val quranVolume = floatPreferencesKey("quran_volume")
        val bgVolume = floatPreferencesKey("background_volume")
        val bgSound = stringPreferencesKey("background_sound")
        val bgStopWithQuran = booleanPreferencesKey("background_stop_with_quran")
        val lastReciter = stringPreferencesKey("last_reciter")
        val catalogEtag = stringPreferencesKey("catalog_etag")
        val catalogFetchedAt = longPreferencesKey("catalog_fetched_at")
        val lastSession = stringPreferencesKey("last_session")
        val adNavigations = intPreferencesKey("ad_navigations")
    }

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            onboardingDone = p[K.onboardingDone] ?: false,
            themeMode = p[K.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            playbackSpeed = (p[K.speed] ?: 1f).coerceIn(0.5f, 2f),
            autoPlayNext = p[K.autoNext] ?: true,
            repeat = p[K.repeat]?.let { runCatching { RepeatSetting.valueOf(it) }.getOrNull() } ?: RepeatSetting.OFF,
            shuffle = p[K.shuffle] ?: false,
            wifiOnly = p[K.wifiOnly] ?: false,
            quranVolume = (p[K.quranVolume] ?: AudioMix.DEFAULT_QURAN_VOLUME).coerceIn(0f, 1f),
            backgroundVolume = (p[K.bgVolume] ?: AudioMix.DEFAULT_BACKGROUND_VOLUME).coerceIn(0f, 1f),
            backgroundSound = p[K.bgSound],
            stopBackgroundWithQuran = p[K.bgStopWithQuran] ?: true,
            lastReciterId = p[K.lastReciter],
            catalogEtag = p[K.catalogEtag],
            catalogFetchedAtMs = p[K.catalogFetchedAt] ?: 0,
            lastSessionJson = p[K.lastSession],
        )
    }.distinctUntilChanged()

    suspend fun current(): Settings = settings.first()

    suspend fun setOnboardingDone() = store.edit { it[K.onboardingDone] = true }
    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[K.themeMode] = mode.name }
    suspend fun setPlaybackSpeed(speed: Float) = store.edit { it[K.speed] = speed.coerceIn(0.5f, 2f) }
    suspend fun setAutoPlayNext(on: Boolean) = store.edit { it[K.autoNext] = on }
    suspend fun setRepeat(r: RepeatSetting) = store.edit { it[K.repeat] = r.name }
    suspend fun setShuffle(on: Boolean) = store.edit { it[K.shuffle] = on }
    suspend fun setWifiOnly(on: Boolean) = store.edit { it[K.wifiOnly] = on }
    suspend fun setQuranVolume(v: Float) = store.edit { it[K.quranVolume] = v.coerceIn(0f, 1f) }
    suspend fun setBackgroundVolume(v: Float) = store.edit { it[K.bgVolume] = v.coerceIn(0f, 1f) }
    suspend fun setBackgroundSound(id: String?) = store.edit { if (id == null) it.remove(K.bgSound) else it[K.bgSound] = id }
    suspend fun setStopBackgroundWithQuran(on: Boolean) = store.edit { it[K.bgStopWithQuran] = on }
    suspend fun setLastReciter(id: String) = store.edit { it[K.lastReciter] = id }
    suspend fun setCatalogMeta(etag: String?, fetchedAtMs: Long) = store.edit {
        if (etag == null) it.remove(K.catalogEtag) else it[K.catalogEtag] = etag
        it[K.catalogFetchedAt] = fetchedAtMs
    }
    suspend fun setLastSession(json: String?) = store.edit { if (json == null) it.remove(K.lastSession) else it[K.lastSession] = json }
}
