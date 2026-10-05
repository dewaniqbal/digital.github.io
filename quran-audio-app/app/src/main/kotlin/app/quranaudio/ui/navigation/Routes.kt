package app.quranaudio.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe navigation destinations.
@Serializable data object OnboardingRoute
@Serializable data object HomeRoute
@Serializable data object RecitersRoute
@Serializable data object SearchRoute
@Serializable data object LibraryRoute
@Serializable data object SettingsRoute
@Serializable data class ReciterRoute(val reciterId: String)
@Serializable data class SurahRoute(val surah: Int)
@Serializable data class PlaylistRoute(val playlistId: Long)
@Serializable data class CollectionRoute(val key: String)
@Serializable data object FavouritesRoute
@Serializable data object RecentRoute
@Serializable data object NowPlayingRoute
@Serializable data object BackgroundSoundsRoute
@Serializable data object AboutRoute
