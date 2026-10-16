package app.quranaudio.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import app.quranaudio.R
import app.quranaudio.playback.PlaybackProgress
import app.quranaudio.shared.playback.formatDuration
import app.quranaudio.ui.components.ReciterArtwork
import app.quranaudio.ui.theme.ArabicNameStyle
import app.quranaudio.ui.theme.ArtworkPalette
import app.quranaudio.ui.theme.Radii
import app.quranaudio.ui.theme.Spacing

/**
 * Full-screen player. No ads are ever shown here.
 */
@Composable
fun NowPlayingScreen(
    onClose: () -> Unit,
    onOpenBackgroundSounds: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.player.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle(PlaybackProgress(0, 0, null))
    val sleep by viewModel.sleep.collectAsStateWithLifecycle()
    val ambient by viewModel.ambientState.collectAsStateWithLifecycle()
    val isFavourite by viewModel.isFavourite.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf(PlayerSheet.NONE) }
    val track = state.track

    val (c1, c2) = remember(track?.reciterId) { ArtworkPalette.colorsFor(track?.reciterId ?: "") }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(c1).copy(alpha = 0.85f), Color(0xFF0A0A12), Color(0xFF0A0A12)))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.cd_close_player), tint = Color.White) }
                Text(
                    stringResource(R.string.player_now_playing),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = { sheet = PlayerSheet.QUEUE }) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = stringResource(R.string.player_queue), tint = Color.White)
                }
            }

            if (track == null) {
                Spacer(Modifier.height(120.dp))
                Text(stringResource(R.string.player_nothing_playing), style = MaterialTheme.typography.titleMedium, color = Color.White)
                return@Column
            }

            BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = Spacing.lg), contentAlignment = Alignment.Center) {
                val art = minOf(maxWidth, 340.dp)
                ReciterArtwork(track.reciterId, track.reciterName, art, shape = Radii.tile)
            }

            Text(
                stringResource(R.string.surah_title, track.surah.nameTransliterated),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(track.surah.nameArabic, style = ArabicNameStyle, color = Color.White.copy(alpha = 0.85f))
            Text(track.reciterName, style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center)
            Text(track.recitationTitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f), textAlign = TextAlign.Center)

            AnimatedVisibility(state.hasError) {
                ErrorCard(onRetry = viewModel::retry, onNext = viewModel::skipFailed, hasNext = state.hasNext)
            }

            SeekBar(progress = progress, durationMs = state.durationMs ?: progress.durationMs, onSeek = viewModel::seekTo)

            Row(
                Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::previous, enabled = state.hasPrevious, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.cd_previous), tint = Color.White, modifier = Modifier.size(40.dp))
                }
                FilledIconButton(
                    onClick = viewModel::togglePlay,
                    modifier = Modifier.size(84.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color(0xFF14163A)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.isBuffering) CircularProgressIndicator(Modifier.size(72.dp), color = Color(0xFF6E64E8), strokeWidth = 3.dp)
                        Icon(
                            if (state.isPlaying || (state.playWhenReady && state.isBuffering)) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play),
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                IconButton(onClick = viewModel::next, enabled = state.hasNext, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.cd_next), tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ToggleIcon(
                    icon = if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    label = stringResource(if (isFavourite) R.string.cd_remove_favourite else R.string.cd_add_favourite),
                    active = isFavourite,
                    onClick = viewModel::toggleFavourite,
                )
                ToggleIcon(
                    icon = Icons.Filled.Bedtime,
                    label = stringResource(R.string.player_sleep_timer),
                    active = sleep != null,
                    badge = sleep?.let { s -> s.remainingMs(android.os.SystemClock.elapsedRealtime())?.let(::formatDuration) ?: stringResource(R.string.sleep_end_of_surah_short) },
                    onClick = { sheet = PlayerSheet.SLEEP },
                )
                ToggleIcon(
                    icon = Icons.Filled.Speed,
                    label = stringResource(R.string.player_speed),
                    active = state.speed != 1f,
                    badge = "${state.speed}x",
                    onClick = { sheet = PlayerSheet.SPEED },
                )
                ToggleIcon(
                    icon = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    label = stringResource(
                        when (state.repeatMode) {
                            Player.REPEAT_MODE_ONE -> R.string.repeat_one
                            Player.REPEAT_MODE_ALL -> R.string.repeat_all
                            else -> R.string.repeat_off
                        },
                    ),
                    active = state.repeatMode != Player.REPEAT_MODE_OFF,
                    onClick = viewModel::cycleRepeat,
                )
                ToggleIcon(
                    icon = Icons.Filled.Shuffle,
                    label = stringResource(if (state.shuffle) R.string.shuffle_on else R.string.shuffle_off),
                    active = state.shuffle,
                    onClick = viewModel::toggleShuffle,
                )
                ToggleIcon(
                    icon = Icons.Filled.GraphicEq,
                    label = stringResource(R.string.player_background_sound),
                    active = ambient.playing,
                    onClick = onOpenBackgroundSounds,
                )
            }

            val upNext = state.upNext
            if (upNext.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.lg))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.player_up_next), style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                    androidx.compose.material3.TextButton(onClick = { sheet = PlayerSheet.QUEUE }) { Text(stringResource(R.string.action_see_all)) }
                }
                upNext.take(3).forEach { entry ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(Radii.small)
                            .clickable { viewModel.skipTo(entry.index) }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${entry.track.surahNumber}", color = Color.White.copy(alpha = 0.6f), modifier = Modifier.width(36.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.track.surah.nameTransliterated, color = Color.White, style = MaterialTheme.typography.titleSmall)
                            Text(entry.track.reciterName, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }

    when (sheet) {
        PlayerSheet.NONE -> Unit
        PlayerSheet.QUEUE -> QueueSheet(state, viewModel, onDismiss = { sheet = PlayerSheet.NONE })
        PlayerSheet.SLEEP -> SleepTimerSheet(sleep, viewModel, onDismiss = { sheet = PlayerSheet.NONE })
        PlayerSheet.SPEED -> SpeedSheet(state.speed, onSelect = { viewModel.setSpeed(it); sheet = PlayerSheet.NONE }, onDismiss = { sheet = PlayerSheet.NONE })
    }
}

enum class PlayerSheet { NONE, QUEUE, SLEEP, SPEED }

@Composable
private fun SeekBar(progress: PlaybackProgress, durationMs: Long?, onSeek: (Long) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val duration = durationMs ?: 0L
    val fraction = if (duration > 0) (progress.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shown = if (dragging) dragValue else fraction
    val positionMs = (shown * duration).toLong()
    val elapsedLabel = formatDuration(positionMs)
    val remainingLabel = if (duration > 0) "-" + formatDuration(duration - positionMs) else "--:--"
    val a11y = stringResource(R.string.cd_seek_bar, elapsedLabel, if (duration > 0) formatDuration(duration) else "--:--")
    Column(Modifier.fillMaxWidth().padding(top = Spacing.md)) {
        Slider(
            value = shown,
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                if (duration > 0) onSeek((dragValue * duration).toLong())
                dragging = false
            },
            enabled = duration > 0,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.25f)),
            modifier = Modifier.semantics { contentDescription = a11y; stateDescription = elapsedLabel },
        )
        Row(Modifier.fillMaxWidth()) {
            Text(elapsedLabel, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
            Text(remainingLabel, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun ToggleIcon(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit, badge: String? = null) {
    val state = stringResource(if (active) R.string.state_on else R.string.state_off)
    Column(
        Modifier
            .widthIn(min = 52.dp)
            .clip(Radii.small)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label; stateDescription = state }
            .padding(vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = if (active) Color(0xFFB9C3FF) else Color.White.copy(alpha = 0.8f), modifier = Modifier.size(26.dp))
        // Text label (not colour alone) shows state for timers and speed.
        Text(badge ?: "", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
    }
}

@Composable
private fun ErrorCard(onRetry: () -> Unit, onNext: () -> Unit, hasNext: Boolean) {
    Surface(shape = Radii.card, color = Color.White.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth().padding(top = Spacing.md)) {
        Column(Modifier.padding(Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Color.White)
            Text(stringResource(R.string.player_error_unavailable), style = MaterialTheme.typography.titleSmall, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                if (hasNext) OutlinedButton(onClick = onNext) { Text(stringResource(R.string.action_next_track), color = Color.White) }
            }
        }
    }
}
