package app.quranaudio.ui.settings

import android.app.Activity
import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quranaudio.BuildConfig
import app.quranaudio.R
import app.quranaudio.ads.AdsEntryPoint
import app.quranaudio.data.prefs.ThemeMode
import app.quranaudio.ui.components.AppTopBar
import app.quranaudio.ui.components.GradientBackground
import app.quranaudio.ui.player.SPEEDS
import app.quranaudio.ui.theme.Spacing
import dagger.hilt.android.EntryPointAccessors
import androidx.core.net.toUri

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onOpenBackgroundSounds: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val cache by viewModel.cacheBytes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ads = EntryPointAccessors.fromApplication(context.applicationContext, AdsEntryPoint::class.java).adsManager()

    Box(Modifier.fillMaxSize()) {
        GradientBackground()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item { AppTopBar(stringResource(R.string.nav_settings)) }

            item { Group(stringResource(R.string.settings_playback)) }
            item { SwitchRow(stringResource(R.string.settings_auto_next), stringResource(R.string.settings_auto_next_desc), s.autoPlayNext, viewModel::setAutoNext) }
            item { SwitchRow(stringResource(R.string.settings_wifi_only), stringResource(R.string.settings_wifi_only_desc), s.wifiOnly, viewModel::setWifiOnly) }
            item {
                Text(stringResource(R.string.settings_default_speed), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs))
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.sm)) {
                    SPEEDS.filter { it in listOf(0.75f, 1f, 1.25f, 1.5f) }.forEach { sp ->
                        Row(
                            Modifier
                                .weight(1f)
                                .selectable(selected = s.playbackSpeed == sp, role = Role.RadioButton) { viewModel.setSpeed(sp) }
                                .heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = s.playbackSpeed == sp, onClick = null)
                            Text("${sp}x")
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.settings_repeat_shuffle_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs)) }
            item { LinkRow(stringResource(R.string.player_background_sound), stringResource(R.string.settings_background_desc), onOpenBackgroundSounds) }
            item { SwitchRow(stringResource(R.string.background_stop_with_quran), null, s.stopBackgroundWithQuran, viewModel::setStopBackgroundWithQuran) }
            item { InfoRow(stringResource(R.string.settings_audio_quality), stringResource(R.string.settings_audio_quality_desc)) }

            item { Group(stringResource(R.string.settings_appearance)) }
            ThemeMode.entries.forEach { mode ->
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = s.themeMode == mode, role = Role.RadioButton) { viewModel.setTheme(mode) }
                            .heightIn(min = 52.dp)
                            .padding(horizontal = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = s.themeMode == mode, onClick = null)
                        Text(
                            stringResource(
                                when (mode) {
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                    ThemeMode.DARK -> R.string.theme_dark
                                    ThemeMode.LIGHT -> R.string.theme_light
                                },
                            ),
                            modifier = Modifier.padding(start = Spacing.xs),
                        )
                    }
                }
            }

            item { Group(stringResource(R.string.settings_notifications)) }
            item {
                LinkRow(stringResource(R.string.settings_notification_settings), stringResource(R.string.settings_notification_desc)) {
                    context.startActivity(
                        Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }

            item { Group(stringResource(R.string.settings_storage)) }
            item { InfoRow(stringResource(R.string.settings_downloads), stringResource(R.string.settings_downloads_unavailable)) }
            item { LinkRow(stringResource(R.string.settings_clear_cache), Formatter.formatShortFileSize(context, cache), viewModel::clearCache) }

            item { Group(stringResource(R.string.settings_privacy)) }
            if (ads.enabled && ads.privacyOptionsRequired) {
                item { LinkRow(stringResource(R.string.settings_ad_privacy), null) { (context as? Activity)?.let(ads::showPrivacyOptions) } }
            }
            item { LinkRow(stringResource(R.string.about_privacy_policy), null) { openUrl(context, BuildConfig.PRIVACY_POLICY_URL) } }

            item { Group(stringResource(R.string.settings_about)) }
            item { LinkRow(stringResource(R.string.settings_about_app), stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), onOpenAbout) }
        }
    }
}

fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
private fun Group(title: String) {
    Column {
        HorizontalDivider(Modifier.padding(top = Spacing.md), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm).semantics { heading() },
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.screen, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun LinkRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.screen, vertical = Spacing.sm),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
