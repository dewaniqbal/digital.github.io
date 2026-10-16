package app.quranaudio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.quranaudio.R
import app.quranaudio.domain.Reciter
import app.quranaudio.shared.quran.Surah
import app.quranaudio.ui.theme.ArabicNameStyle
import app.quranaudio.ui.theme.LocalGradients
import app.quranaudio.ui.theme.Radii
import app.quranaudio.ui.theme.Spacing

/** Large circular reciter card for horizontal rows. */
@Composable
fun ReciterCard(reciter: Reciter, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 120.dp) {
    Column(
        modifier = modifier
            .width(size)
            .clip(Radii.card)
            .clickable(onClickLabel = stringResource(R.string.cd_open_reciter), role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ReciterArtwork(reciter.id, reciter.name, size - 8.dp)
        Spacer(Modifier.height(Spacing.xs))
        Text(reciter.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** Reciter list row with favourite toggle. */
@Composable
fun ReciterRow(reciter: Reciter, onClick: () -> Unit, onToggleFavourite: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.screen, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReciterArtwork(reciter.id, reciter.name, 52.dp)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(reciter.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val details = buildList {
                reciter.nameArabic?.let(::add)
                reciter.country?.let(::add)
            }.joinToString(" · ")
            if (details.isNotEmpty()) {
                Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                if (reciter.hasCompleteQuran) stringResource(R.string.reciter_complete_quran)
                else pluralStringResource(R.plurals.surah_count, reciter.surahCount, reciter.surahCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        FavouriteButton(isFavourite = reciter.isFavourite, onToggle = onToggleFavourite)
    }
}

@Composable
fun FavouriteButton(isFavourite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (isFavourite) 1.15f else 1f, spring(dampingRatio = 0.4f), label = "favScale")
    val tint by animateColorAsState(if (isFavourite) Color(0xFFFF8FB1) else MaterialTheme.colorScheme.onSurfaceVariant, label = "favTint")
    val stateText = stringResource(if (isFavourite) R.string.state_favourite else R.string.state_not_favourite)
    IconButton(onClick = onToggle, modifier = modifier.semantics { stateDescription = stateText }) {
        Icon(
            if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = stringResource(if (isFavourite) R.string.cd_remove_favourite else R.string.cd_add_favourite),
            tint = tint,
            modifier = Modifier.scale(scale),
        )
    }
}

/** Surah list row: number, names, verse count / duration, progress and actions. */
@Composable
fun SurahRow(
    surah: Surah,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    progress: Float? = null,
    isCurrent: Boolean = false,
    onMore: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val playLabel = stringResource(R.string.cd_play_surah, surah.nameTransliterated)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clickable(onClickLabel = playLabel, onClick = onPlay)
            .background(if (isCurrent) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent)
            .padding(horizontal = Spacing.screen, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SurahBadge(surah.number)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    surah.nameTransliterated,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(surah.nameArabic, style = ArabicNameStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                subtitle ?: pluralStringResource(R.plurals.verse_count, surah.verseCount, surah.verseCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (progress != null && progress > 0f) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(0.6f).height(3.dp).clip(Radii.pill))
            }
        }
        trailing?.invoke()
        if (onMore != null) {
            IconButton(onClick = onMore) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more_options, surah.nameTransliterated)) }
        }
    }
}

/** Two-column playlist tile with a soft gradient and a simple line illustration. */
@Composable
fun PlaylistCard(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(Radii.tile)
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(Spacing.md),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(56.dp),
        )
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

/** Prominent "Continue listening" hero card. */
@Composable
fun ContinueListeningCard(
    reciterId: String,
    reciterName: String,
    surah: Surah,
    progress: Float?,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.cd_continue_listening, surah.nameTransliterated, reciterName)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radii.tile)
            .background(Brush.linearGradient(LocalGradients.current.hero))
            .clickable(onClick = onPlay)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReciterArtwork(reciterId, reciterName, 88.dp)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.home_continue_listening), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
            Text(stringResource(R.string.surah_title, surah.nameTransliterated), style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(reciterName, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (progress != null) {
                Spacer(Modifier.height(Spacing.xs))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(Radii.pill),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f),
                )
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        FilledIconButton(
            onClick = onPlay,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color(0xFF1B1F52)),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

/** Ambient-sound tile for the background sound grid. */
@Composable
fun BackgroundSoundCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        label = "soundBg",
    )
    val stateText = stringResource(if (selected) R.string.state_selected else R.string.state_not_selected)
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(Radii.card)
            .background(bg)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) { stateDescription = stateText }
            .padding(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (selected) 0.2f else 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}
