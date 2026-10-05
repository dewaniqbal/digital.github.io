package app.quranaudio.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.quranaudio.R
import app.quranaudio.playback.PlayerUiState
import app.quranaudio.shared.playback.SleepTimerOption
import app.quranaudio.shared.playback.SleepTimerState
import app.quranaudio.shared.playback.formatDuration
import app.quranaudio.ui.components.SurahBadge
import app.quranaudio.ui.theme.Spacing
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(state: PlayerUiState, viewModel: PlayerViewModel, onDismiss: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { if (state.currentIndex > 0) listState.scrollToItem(state.currentIndex) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.player_queue) + " · " + pluralStringResource(R.plurals.surah_count, state.queue.size, state.queue.size),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm),
        )
        LazyColumn(state = listState, modifier = Modifier.navigationBarsPadding()) {
            items(state.queue, key = { "${it.index}-${it.track.key}" }) { entry ->
                val current = entry.index == state.currentIndex
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .background(if (current) MaterialTheme.colorScheme.surfaceContainerHigh else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { viewModel.skipTo(entry.index) }
                        .padding(horizontal = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (current) Icon(Icons.Filled.GraphicEq, contentDescription = stringResource(R.string.cd_now_playing), tint = MaterialTheme.colorScheme.primary)
                    else SurahBadge(entry.track.surahNumber, size = 36.dp)
                    Spacer(Modifier.width(Spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(entry.track.surah.nameTransliterated, style = MaterialTheme.typography.titleSmall)
                        Text(entry.track.reciterName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!current) {
                        IconButton(onClick = { viewModel.moveInQueue(entry.index, entry.index - 1) }, enabled = entry.index > 0) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = stringResource(R.string.action_move_up))
                        }
                        IconButton(onClick = { viewModel.moveInQueue(entry.index, entry.index + 1) }, enabled = entry.index < state.queue.lastIndex) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = stringResource(R.string.action_move_down))
                        }
                        IconButton(onClick = { viewModel.removeFromQueue(entry.index) }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_remove_from_queue, entry.track.surah.nameTransliterated))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(current: SleepTimerState?, viewModel: PlayerViewModel, onDismiss: () -> Unit) {
    var custom by remember { mutableStateOf(false) }
    if (custom) {
        CustomTimerDialog(
            onDismiss = { custom = false },
            onConfirm = { minutes -> viewModel.startSleepTimer(SleepTimerOption.Duration(minutes)); custom = false; onDismiss() },
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = Spacing.md)) {
            Text(stringResource(R.string.player_sleep_timer), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm))
            if (current != null) {
                val remaining = viewModel.sleepRemainingMs()
                Text(
                    if (remaining != null) stringResource(R.string.sleep_remaining, formatDuration(remaining)) else stringResource(R.string.sleep_end_of_surah_active),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.screen),
                )
            }
            SleepTimerOption.PRESET_MINUTES.forEach { m ->
                OptionRow(pluralStringResource(R.plurals.minutes, m, m), selected = (current?.option as? SleepTimerOption.Duration)?.minutes == m) {
                    viewModel.startSleepTimer(SleepTimerOption.Duration(m)); onDismiss()
                }
            }
            OptionRow(stringResource(R.string.sleep_end_of_surah), selected = current?.option == SleepTimerOption.EndOfSurah) {
                viewModel.startSleepTimer(SleepTimerOption.EndOfSurah); onDismiss()
            }
            OptionRow(stringResource(R.string.sleep_custom), selected = false) { custom = true }
            if (current != null) {
                TextButton(onClick = { viewModel.cancelSleepTimer(); onDismiss() }, modifier = Modifier.padding(horizontal = Spacing.sm)) {
                    Text(stringResource(R.string.sleep_cancel))
                }
            }
        }
    }
}

@Composable
private fun CustomTimerDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var minutes by remember { mutableFloatStateOf(20f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_custom)) },
        text = {
            Column {
                Text(pluralStringResource(R.plurals.minutes, minutes.roundToInt(), minutes.roundToInt()), style = MaterialTheme.typography.titleLarge)
                Slider(value = minutes, onValueChange = { minutes = it }, valueRange = 1f..180f)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(minutes.roundToInt().coerceIn(1, 180)) }) { Text(stringResource(R.string.action_start)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSheet(current: Float, onSelect: (Float) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = Spacing.md)) {
            Text(stringResource(R.string.player_speed), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm))
            SPEEDS.forEach { s ->
                OptionRow(if (s == 1f) stringResource(R.string.speed_normal) else "${s}x", selected = current == s) { onSelect(s) }
            }
        }
    }
}

val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(Spacing.sm))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
