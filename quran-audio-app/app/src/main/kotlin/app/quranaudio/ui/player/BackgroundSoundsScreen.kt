package app.quranaudio.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlutterDash
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PestControl
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quranaudio.R
import app.quranaudio.playback.AmbientSound
import app.quranaudio.ui.components.AppTopBar
import app.quranaudio.ui.components.BackgroundSoundCard
import app.quranaudio.ui.components.GradientBackground
import app.quranaudio.ui.components.VolumeSlider
import app.quranaudio.ui.theme.Spacing

fun AmbientSound.icon(): ImageVector = when (this) {
    AmbientSound.RAIN -> Icons.Filled.WaterDrop
    AmbientSound.BIRDS -> Icons.Filled.FlutterDash
    AmbientSound.FIRE -> Icons.Filled.LocalFireDepartment
    AmbientSound.WAVES -> Icons.Filled.Waves
    AmbientSound.WIND -> Icons.Filled.Air
    AmbientSound.CAT -> Icons.Filled.Pets
    AmbientSound.OWL -> Icons.Filled.NightsStay
    AmbientSound.RIVER -> Icons.Filled.Water
    AmbientSound.CRICKETS -> Icons.Filled.PestControl
    AmbientSound.THUNDERSTORM -> Icons.Filled.Thunderstorm
    AmbientSound.THUNDER -> Icons.Filled.Bolt
    AmbientSound.TRAIN -> Icons.Filled.Train
}

/** Optional ambient sound mixer. Off by default; nothing plays until the user picks a sound. */
@Composable
fun BackgroundSoundsScreen(onBack: () -> Unit, contentPadding: PaddingValues, viewModel: PlayerViewModel = hiltViewModel()) {
    val ambient by viewModel.ambientState.collectAsStateWithLifecycle()
    val mix by viewModel.mix.collectAsStateWithLifecycle()
    // Local slider state for smooth dragging; persisted when the drag ends.
    var quran by remember(mix.quranVolume) { mutableFloatStateOf(mix.quranVolume) }
    var background by remember(mix.backgroundVolume) { mutableFloatStateOf(mix.backgroundVolume) }

    Box(Modifier.fillMaxSize()) {
        GradientBackground()
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { AppTopBar(stringResource(R.string.background_title), onBack = onBack) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                val switchLabel = stringResource(R.string.background_enable)
                Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(switchLabel, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(if (ambient.enabled) R.string.background_on_hint else R.string.background_off_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = ambient.enabled, onCheckedChange = { viewModel.toggleAmbient() }, modifier = Modifier.semantics { contentDescription = switchLabel })
                }
            }
            items(AmbientSound.entries, key = { it.id }) { sound ->
                BackgroundSoundCard(
                    label = stringResource(sound.label),
                    icon = sound.icon(),
                    selected = ambient.enabled && ambient.selected == sound,
                    onClick = { if (ambient.enabled && ambient.selected == sound) viewModel.ambientOff() else viewModel.selectAmbient(sound) },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    VolumeSlider(
                        label = stringResource(R.string.volume_quran),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        value = quran,
                        onValueChange = { quran = it },
                        onValueChangeFinished = { viewModel.setQuranVolume(quran) },
                    )
                    VolumeSlider(
                        label = stringResource(R.string.volume_background),
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        value = background,
                        onValueChange = { background = it },
                        onValueChangeFinished = { viewModel.setBackgroundVolume(background) },
                    )
                    Text(stringResource(R.string.volume_cap_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val label = stringResource(R.string.background_stop_with_quran)
                        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(checked = mix.stopWithQuran, onCheckedChange = viewModel::setStopWithQuran, modifier = Modifier.semantics { contentDescription = label })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.padding(end = Spacing.xs).fillMaxWidth(0.06f))
                        Text(stringResource(R.string.background_license_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
