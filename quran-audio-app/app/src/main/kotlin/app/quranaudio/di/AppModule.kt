package app.quranaudio.di

import android.content.Context
import androidx.room.Room
import app.quranaudio.BuildConfig
import app.quranaudio.data.db.AppDatabase
import app.quranaudio.data.db.CatalogDao
import app.quranaudio.data.db.FavouritesDao
import app.quranaudio.data.db.HistoryDao
import app.quranaudio.data.db.PlaylistDao
import app.quranaudio.data.network.BackendCatalogApi
import app.quranaudio.data.network.BackendCatalogDataSource
import app.quranaudio.data.network.CatalogRemoteDataSource
import app.quranaudio.data.network.Mp3QuranDirectApi
import app.quranaudio.data.network.Mp3QuranDirectDataSource
import app.quranaudio.data.repository.Clock
import app.quranaudio.shared.catalog.CatalogValidator
import app.quranaudio.shared.source.Mp3QuranApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Hosts audio may be streamed from. Remote catalogue entries pointing elsewhere are dropped. */
    val ALLOWED_AUDIO_HOSTS = Mp3QuranApi.AUDIO_HOSTS + setOf(".quranicaudio.com", ".quran.foundation", ".qurancdn.com")

    @Provides @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", "QuranAudio-Android/${BuildConfig.VERSION_NAME}").build())
        }
        .build()

    @Provides @Singleton
    fun catalogRemote(client: OkHttpClient, json: Json): CatalogRemoteDataSource {
        val converter = json.asConverterFactory("application/json".toMediaType())
        val backend = BuildConfig.CATALOG_BASE_URL.trim()
        return if (backend.isNotEmpty()) {
            require(backend.startsWith("https://")) { "CATALOG_BASE_URL must use HTTPS" }
            val api = Retrofit.Builder().baseUrl(backend.trimEnd('/') + "/").client(client).addConverterFactory(converter).build()
                .create(BackendCatalogApi::class.java)
            BackendCatalogDataSource(api)
        } else {
            val api = Retrofit.Builder().baseUrl(Mp3QuranApi.BASE_URL).client(client).addConverterFactory(converter).build()
                .create(Mp3QuranDirectApi::class.java)
            Mp3QuranDirectDataSource(api)
        }
    }

    @Provides @Singleton
    fun validator(): CatalogValidator = CatalogValidator(allowedAudioHosts = ALLOWED_AUDIO_HOSTS)

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()

    @Provides fun catalogDao(db: AppDatabase): CatalogDao = db.catalogDao()
    @Provides fun favouritesDao(db: AppDatabase): FavouritesDao = db.favouritesDao()
    @Provides fun historyDao(db: AppDatabase): HistoryDao = db.historyDao()
    @Provides fun playlistDao(db: AppDatabase): PlaylistDao = db.playlistDao()

    @Provides @Singleton
    fun clock(): Clock = Clock(System::currentTimeMillis)

    @Provides @Singleton @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
