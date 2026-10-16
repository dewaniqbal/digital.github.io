package app.quranaudio.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.quranaudio.R
import app.quranaudio.domain.Track
import app.quranaudio.ui.theme.Radii
import app.quranaudio.ui.theme.Spacing
import kotlin.math.roundToInt

/** Labelled 0–100 % volume slider with an icon; percentage is announced to screen readers. */
@Composable
fun VolumeSlider(
    label: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val percent = (value * 100).roundToInt()
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.xs))
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text("$percent%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = "$percent%"
            },
        )
    }
}

/**
 * Persistent mini player shown above the bottom navigation while something is loaded.
 * Tap opens Now Playing; horizontal swipe left skips to the next Surah.
 */
@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    isBuffering: Boolean,
    progress: Float?,
    hasNext: Boolean,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openLabel = stringResource(R.string.cd_open_now_playing)
    var dragTotal by remember { mutableFloatStateOf(0f) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs)
            .clip(Radii.card)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClickLabel = openLabel, onClick = onOpen)
            .pointerInput(hasNext) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = { if (dragTotal < -120f && hasNext) onNext() },
                ) { _, delta -> dragTotal += delta }
            },
    ) {
        Row(Modifier.padding(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            ReciterArtwork(track.reciterId, track.reciterName, 48.dp, shape = Radii.small)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.surah_title, track.surah.nameTransliterated),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(track.reciterName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            PlayPauseIcon(isPlaying = isPlaying, isBuffering = isBuffering, onClick = onTogglePlay)
            IconButton(onClick = onNext, enabled = hasNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.cd_next))
            }
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                trackColor = Color.Transparent,
            )
        }
    }
}

@Composable
fun PlayPauseIcon(isPlaying: Boolean, isBuffering: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 48.dp) {
    val label = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play)
    IconButton(onClick = onClick, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            if (isBuffering) {
                CircularProgressIndicator(Modifier.size(size * 0.75f), strokeWidth = 2.dp)
            }
            AnimatedContent(targetState = isPlaying, label = "playPause") { playing ->
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = label,
                    modifier = Modifier.size(size * 0.6f),
                )
            }
        }
    }
}
