package app.quranaudio.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quranaudio.R
import app.quranaudio.ads.AdBanner
import app.quranaudio.domain.ThemeCollection
import app.quranaudio.ui.components.AppTopBar
import app.quranaudio.ui.components.GradientBackground
import app.quranaudio.ui.components.PlaylistCard
import app.quranaudio.ui.components.SectionHeader
import app.quranaudio.ui.theme.Spacing

@Composable
fun LibraryScreen(
    contentPadding: PaddingValues,
    onOpenFavourites: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenCollection: (ThemeCollection) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val dir = LocalLayoutDirection.current
    val gridPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(dir) + Spacing.screen,
        end = contentPadding.calculateEndPadding(dir) + Spacing.screen,
        top = contentPadding.calculateTopPadding(),
        bottom = contentPadding.calculateBottomPadding() + Spacing.lg,
    )
    Box(Modifier.fillMaxSize()) {
        GradientBackground()
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = gridPadding,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AppTopBar(
                    title = stringResource(R.string.nav_playlists),
                    horizontalPadding = 0.dp,
                    actions = {
                        IconButton(onClick = { showCreate = true }) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.playlist_create))
                        }
                    },
                )
            }
            item {
                PlaylistCard(
                    title = stringResource(R.string.library_favourites),
                    subtitle = pluralStringResource(R.plurals.surah_count, state.favouriteCount, state.favouriteCount),
                    icon = FavouritesIcon,
                    gradient = FavouritesGradient,
                    onClick = onOpenFavourites,
                )
            }
            item {
                PlaylistCard(
                    title = stringResource(R.string.library_recently_played),
                    subtitle = pluralStringResource(R.plurals.surah_count, state.recentCount, state.recentCount),
                    icon = RecentIcon,
                    gradient = RecentGradient,
                    onClick = onOpenRecent,
                )
            }
            if (state.playlists.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(stringResource(R.string.library_your_playlists), horizontalPadding = 0.dp) }
                items(state.playlists, key = { "p${it.id}" }) { p ->
                    PlaylistCard(
                        title = p.name,
                        subtitle = pluralStringResource(R.plurals.surah_count, p.trackCount, p.trackCount),
                        icon = PlaylistIcon,
                        gradient = UserPlaylistGradient,
                        onClick = { onOpenPlaylist(p.id) },
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(stringResource(R.string.library_collections), horizontalPadding = 0.dp) }
            items(ThemeCollection.entries, key = { it.key }) { c ->
                val style = collectionStyle(c)
                PlaylistCard(
                    title = stringResource(style.title),
                    subtitle = pluralStringResource(R.plurals.surah_count, c.surahs.size, c.surahs.size),
                    icon = style.icon,
                    gradient = style.gradient,
                    onClick = { onOpenCollection(c) },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) { AdBanner() }
        }
    }
    if (showCreate) {
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_create),
            initial = "",
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                viewModel.create(name) { onOpenPlaylist(it) }
            },
        )
    }
}


