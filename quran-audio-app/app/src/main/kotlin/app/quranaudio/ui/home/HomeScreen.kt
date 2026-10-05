package app.quranaudio.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quranaudio.R
import app.quranaudio.ads.AdBanner
import app.quranaudio.domain.ListeningEntry
import app.quranaudio.domain.Reciter
import app.quranaudio.domain.SyncStatus
import app.quranaudio.domain.ThemeCollection
import app.quranaudio.shared.playback.ResumePolicy
import app.quranaudio.ui.components.AppTopBar
import app.quranaudio.ui.components.ContinueListeningCard
import app.quranaudio.ui.components.ErrorState
import app.quranaudio.ui.components.GradientBackground
import app.quranaudio.ui.components.LoadingSkeleton
import app.quranaudio.ui.components.PlaylistCard
import app.quranaudio.ui.components.ReciterArtwork
import app.quranaudio.ui.components.ReciterCard
import app.quranaudio.ui.components.SectionHeader
import app.quranaudio.ui.library.collectionStyle
import app.quranaudio.ui.theme.Radii
import app.quranaudio.ui.theme.Spacing
import androidx.compose.foundation.clickable
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onOpenReciter: (String) -> Unit,
    onOpenReciters: () -> Unit,
    onOpenCollection: (ThemeCollection) -> Unit,
    onOpenPlaylists: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        GradientBackground()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item { AppTopBar(title = stringResource(R.string.nav_home)) }
            when {
                state.loading -> item { LoadingSkeleton() }
                state.catalogEmpty && state.status is SyncStatus.Failed -> item {
                    ErrorState(
                        message = stringResource(R.string.error_load_reciters),
                        offline = (state.status as SyncStatus.Failed).offline,
                        onRetry = viewModel::retry,
                    )
                }
                else -> homeContent(state, viewModel::continueListening, onOpenReciter, onOpenReciters, onOpenCollection, onOpenPlaylists)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.homeContent(
    state: HomeUiState,
    onContinue: (ListeningEntry) -> Unit,
    onOpenReciter: (String) -> Unit,
    onOpenReciters: () -> Unit,
    onOpenCollection: (ThemeCollection) -> Unit,
    onOpenPlaylists: () -> Unit,
) {
    state.continueListening?.let { entry ->
        item(key = "continue") {
            ContinueListeningCard(
                reciterId = entry.track.reciterId,
                reciterName = entry.track.reciterName,
                surah = entry.track.surah,
                progress = ResumePolicy.progress(entry.positionMs, entry.durationMs),
                onPlay = { onContinue(entry) },
                modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs),
            )
        }
    }
    if (state.recent.size > 1) {
        item(key = "recentHeader") { SectionHeader(stringResource(R.string.home_recently_played)) }
        item(key = "recent") {
            LazyRow(contentPadding = PaddingValues(horizontal = Spacing.screen), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                items(state.recent, key = { it.track.key }) { entry -> RecentCard(entry, onClick = { onContinue(entry) }) }
            }
        }
    }
    reciterRow("favourites", R.string.home_your_favourites, state.favourites, onOpenReciter)
    reciterRow("featured", R.string.home_featured_reciters, state.featured, onOpenReciter, onSeeAll = onOpenReciters)
    reciterRow("mostListened", R.string.home_most_listened, state.mostListened, onOpenReciter)
    reciterRow("new", R.string.home_new_reciters, state.newReciters, onOpenReciter, onSeeAll = onOpenReciters)
    item(key = "collectionsHeader") {
        SectionHeader(stringResource(R.string.home_playlists), action = stringResource(R.string.action_see_all), onAction = onOpenPlaylists)
    }
    item(key = "collections") {
        LazyRow(contentPadding = PaddingValues(horizontal = Spacing.screen), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            items(ThemeCollection.entries, key = { it.key }) { c ->
                val style = collectionStyle(c)
                PlaylistCard(
                    title = stringResource(style.title),
                    subtitle = pluralStringResource(R.plurals.surah_count, c.surahs.size, c.surahs.size),
                    icon = style.icon,
                    gradient = style.gradient,
                    onClick = { onOpenCollection(c) },
                    modifier = Modifier.width(160.dp),
                )
            }
        }
    }
    reciterRow("recommended", R.string.home_recommended, state.recommended, onOpenReciter)
    item(key = "count") {
        Text(
            pluralStringResource(R.plurals.verified_reciters, state.totalReciters, state.totalReciters),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.md),
        )
    }
    item(key = "ad") { AdBanner(Modifier.padding(vertical = Spacing.sm)) }
    item { Spacer(Modifier.height(Spacing.lg)) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.reciterRow(
    key: String,
    title: Int,
    reciters: List<Reciter>,
    onOpen: (String) -> Unit,
    onSeeAll: (() -> Unit)? = null,
) {
    if (reciters.isEmpty()) return
    item(key = "${key}Header") {
        SectionHeader(stringResource(title), action = onSeeAll?.let { stringResource(R.string.action_see_all) }, onAction = onSeeAll)
    }
    item(key = key) {
        LazyRow(contentPadding = PaddingValues(horizontal = Spacing.screen), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(reciters, key = { it.id }) { r -> ReciterCard(r, onClick = { onOpen(r.id) }) }
        }
    }
}

@Composable
private fun RecentCard(entry: ListeningEntry, onClick: () -> Unit) {
    Column(
        Modifier
            .width(150.dp)
            .clip(Radii.card)
            .clickable(onClick = onClick)
            .padding(Spacing.xxs),
    ) {
        ReciterArtwork(entry.track.reciterId, entry.track.reciterName, 142.dp, shape = Radii.card)
        Spacer(Modifier.height(Spacing.xs))
        Text(stringResource(R.string.surah_title, entry.track.surah.nameTransliterated), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(entry.track.reciterName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        ResumePolicy.progress(entry.positionMs, entry.durationMs)?.let { p ->
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().height(3.dp).clip(Radii.pill))
        }
    }
}
