package app.quranaudio.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.quranaudio.R
import app.quranaudio.ui.theme.Spacing

/** PLAY ALL / SHUFFLE / ADD TO PLAYLIST action row used by reciter, playlist and collection screens. */
@Composable
fun PlayActionsRow(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToPlaylist: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onPlayAll, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(Spacing.xs))
            Text(stringResource(R.string.action_play_all))
        }
        FilledTonalButton(onClick = onShuffle, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
            Icon(Icons.Filled.Shuffle, contentDescription = null)
            Spacer(Modifier.width(Spacing.xs))
            Text(stringResource(R.string.action_shuffle))
        }
        if (onAddToPlaylist != null) {
            FilledTonalIconButton(onClick = onAddToPlaylist, enabled = enabled, modifier = Modifier.heightIn(min = 52.dp).width(52.dp)) {
                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = stringResource(R.string.action_add_to_playlist))
            }
        }
    }
}
