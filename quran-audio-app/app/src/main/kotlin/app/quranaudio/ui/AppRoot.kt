package app.quranaudio.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.quranaudio.R
import app.quranaudio.ads.AdsManager
import app.quranaudio.domain.ThemeCollection
import app.quranaudio.playback.PlaybackMessage
import app.quranaudio.ui.components.MiniPlayer
import app.quranaudio.ui.home.HomeScreen
import app.quranaudio.ui.library.CollectionScreen
import app.quranaudio.ui.library.FavouritesScreen
import app.quranaudio.ui.library.LibraryScreen
import app.quranaudio.ui.library.PlaylistDetailScreen
import app.quranaudio.ui.library.RecentScreen
import app.quranaudio.ui.navigation.AboutRoute
import app.quranaudio.ui.navigation.BackgroundSoundsRoute
import app.quranaudio.ui.navigation.CollectionRoute
import app.quranaudio.ui.navigation.FavouritesRoute
import app.quranaudio.ui.navigation.HomeRoute
import app.quranaudio.ui.navigation.LibraryRoute
import app.quranaudio.ui.navigation.NowPlayingRoute
import app.quranaudio.ui.navigation.PlaylistRoute
import app.quranaudio.ui.navigation.RecentRoute
import app.quranaudio.ui.navigation.ReciterRoute
import app.quranaudio.ui.navigation.RecitersRoute
import app.quranaudio.ui.navigation.SearchRoute
import app.quranaudio.ui.navigation.SettingsRoute
import app.quranaudio.ui.navigation.SurahRoute
import app.quranaudio.ui.onboarding.OnboardingScreen
import app.quranaudio.ui.player.BackgroundSoundsScreen
import app.quranaudio.ui.player.NowPlayingScreen
import app.quranaudio.ui.reciters.ReciterScreen
import app.quranaudio.ui.reciters.RecitersScreen
import app.quranaudio.ui.reciters.SurahDetailScreen
import app.quranaudio.ui.search.SearchScreen
import app.quranaudio.ui.settings.AboutScreen
import app.quranaudio.ui.settings.SettingsScreen
import kotlin.reflect.KClass

private data class TopLevel(val route: Any, val routeClass: KClass<*>, val label: Int, val icon: ImageVector)

private val topLevels = listOf(
    TopLevel(HomeRoute, HomeRoute::class, R.string.nav_home, Icons.Filled.Home),
    TopLevel(RecitersRoute, RecitersRoute::class, R.string.nav_reciters, Icons.Filled.People),
    TopLevel(SearchRoute, SearchRoute::class, R.string.nav_search, Icons.Filled.Search),
    TopLevel(LibraryRoute, LibraryRoute::class, R.string.nav_playlists, Icons.Filled.LibraryMusic),
    TopLevel(SettingsRoute, SettingsRoute::class, R.string.nav_settings, Icons.Filled.Settings),
)

private fun NavDestination?.isPlayer() = this?.hasRoute(NowPlayingRoute::class) == true || this?.hasRoute(BackgroundSoundsRoute::class) == true

@Composable
fun AppRoot(viewModel: AppViewModel, ads: AdsManager, showOnboarding: Boolean) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val player by viewModel.player.collectAsStateWithLifecycle()
    val progress by viewModel.progressFraction.collectAsStateWithLifecycle(null)
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context as? Activity

    // Playback messages -> snackbars.
    val messages = mapOf(
        PlaybackMessage.WIFI_ONLY_BLOCKED to stringResource(R.string.msg_wifi_only),
        PlaybackMessage.OFFLINE to stringResource(R.string.msg_offline),
        PlaybackMessage.TRACK_UNAVAILABLE to stringResource(R.string.player_error_unavailable),
        PlaybackMessage.SLEEP_TIMER_ENDED to stringResource(R.string.msg_sleep_timer_ended),
    )
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(messages.getValue(it)) } }

    // Ask for the notification permission (Android 13+) the first time the user plays something,
    // so lock-screen / notification controls are visible. Never asked at launch.
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(player.playWhenReady) {
        if (player.playWhenReady && !askedNotifications && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedNotifications = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Interstitials: only at navigation between browsing screens, never around the player, and
    // never while Quran audio is active (enforced again inside AdsManager/InterstitialPolicy).
    var previous by remember { mutableStateOf<NavDestination?>(null) }
    LaunchedEffect(destination) {
        val prev = previous
        previous = destination
        if (activity != null && prev != null && destination != null && prev.id != destination.id) {
            ads.onNavigation(activity, playbackActive = player.isActive, involvesPlayer = prev.isPlayer() || destination.isPlayer())
        }
    }

    val onPlayer = destination.isPlayer()
    val isTopLevel = topLevels.any { t -> destination?.hierarchy?.any { it.hasRoute(t.routeClass) } == true }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                val track = player.track
                AnimatedVisibility(
                    visible = track != null && !onPlayer && destination?.hasRoute(app.quranaudio.ui.navigation.OnboardingRoute::class) != true,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    if (track != null) {
                        MiniPlayer(
                            track = track,
                            isPlaying = player.isPlaying,
                            isBuffering = player.isBuffering,
                            progress = progress,
                            hasNext = player.hasNext,
                            onOpen = { nav.navigate(NowPlayingRoute) { launchSingleTop = true } },
                            onTogglePlay = viewModel::togglePlay,
                            onNext = viewModel::next,
                            modifier = Modifier.padding(bottom = if (isTopLevel) 0.dp else 8.dp),
                        )
                    }
                }
                if (isTopLevel) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        topLevels.forEach { t ->
                            val selected = destination?.hierarchy?.any { it.hasRoute(t.routeClass) } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { nav.navigateTopLevel(t.route) },
                                icon = { Icon(t.icon, contentDescription = null) },
                                label = { Text(stringResource(t.label)) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        AppNavHost(nav, innerPadding, viewModel, showOnboarding)
    }
}

private fun NavHostController.navigateTopLevel(route: Any) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun AppNavHost(nav: NavHostController, padding: PaddingValues, viewModel: AppViewModel, showOnboarding: Boolean) {
    val openReciter: (String) -> Unit = { nav.navigate(ReciterRoute(it)) }
    val openSurah: (Int) -> Unit = { nav.navigate(SurahRoute(it)) }
    val openPlaylist: (Long) -> Unit = { nav.navigate(PlaylistRoute(it)) }
    val openCollection: (ThemeCollection) -> Unit = { nav.navigate(CollectionRoute(it.key)) }
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(
        navController = nav,
        startDestination = if (showOnboarding) app.quranaudio.ui.navigation.OnboardingRoute else HomeRoute,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
    ) {
        composable<app.quranaudio.ui.navigation.OnboardingRoute> {
            OnboardingScreen(onDone = {
                viewModel.finishOnboarding()
                nav.navigate(HomeRoute) { popUpTo(app.quranaudio.ui.navigation.OnboardingRoute) { inclusive = true } }
            })
        }
        composable<HomeRoute> {
            HomeScreen(
                contentPadding = padding,
                onOpenReciter = openReciter,
                onOpenReciters = { nav.navigateTopLevel(RecitersRoute) },
                onOpenCollection = openCollection,
                onOpenPlaylists = { nav.navigateTopLevel(LibraryRoute) },
            )
        }
        composable<RecitersRoute> { RecitersScreen(padding, onOpenReciter = openReciter, onOpenSurah = openSurah) }
        composable<SearchRoute> { SearchScreen(padding, onOpenReciter = openReciter, onOpenSurah = openSurah, onOpenPlaylist = openPlaylist) }
        composable<LibraryRoute> {
            LibraryScreen(
                padding,
                onOpenFavourites = { nav.navigate(FavouritesRoute) },
                onOpenRecent = { nav.navigate(RecentRoute) },
                onOpenCollection = openCollection,
                onOpenPlaylist = openPlaylist,
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(padding, onOpenBackgroundSounds = { nav.navigate(BackgroundSoundsRoute) }, onOpenAbout = { nav.navigate(AboutRoute) })
        }
        composable<ReciterRoute> { ReciterScreen(padding, onBack = back) }
        composable<SurahRoute> { SurahDetailScreen(padding, onBack = back) }
        composable<PlaylistRoute> { PlaylistDetailScreen(padding, onBack = back, onOpenReciter = openReciter) }
        composable<CollectionRoute> { CollectionScreen(padding, onBack = back) }
        composable<FavouritesRoute> { FavouritesScreen(padding, onBack = back, onOpenReciter = openReciter) }
        composable<RecentRoute> { RecentScreen(padding, onBack = back, onOpenReciter = openReciter) }
        composable<AboutRoute> { AboutScreen(padding, onBack = back) }
        composable<NowPlayingRoute>(
            enterTransition = { slideInVertically(tween(320)) { it } + fadeIn() },
            exitTransition = { fadeOut(tween(150)) },
            popExitTransition = { slideOutVertically(tween(280)) { it } + fadeOut() },
        ) {
            NowPlayingScreen(onClose = back, onOpenBackgroundSounds = { nav.navigate(BackgroundSoundsRoute) })
        }
        composable<BackgroundSoundsRoute> { BackgroundSoundsScreen(onBack = back, contentPadding = padding) }
    }
}
