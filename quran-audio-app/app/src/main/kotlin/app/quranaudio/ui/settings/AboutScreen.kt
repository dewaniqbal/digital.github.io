package app.quranaudio.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quranaudio.BuildConfig
import app.quranaudio.R
import app.quranaudio.ui.components.AppTopBar
import app.quranaudio.ui.components.GradientBackground
import app.quranaudio.ui.theme.Spacing

/** About: version, audio/data sources with attribution and terms, licences, privacy, contact. */
@Composable
fun AboutScreen(contentPadding: PaddingValues, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Box(Modifier.fillMaxSize()) {
        GradientBackground()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item { AppTopBar(stringResource(R.string.settings_about_app), onBack = onBack) }
            item { Paragraph(stringResource(R.string.about_intro)) }
            item { Paragraph(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME)) }

            item { Heading(stringResource(R.string.about_audio_sources)) }
            items(sources, key = { it.id }) { src ->
                Column(Modifier.fillMaxWidth()) {
                    LinkRow(src.name, src.attribution) { openUrl(context, src.websiteUrl) }
                    src.termsUrl?.let { url -> LinkRow(stringResource(R.string.about_source_terms, src.name), url) { openUrl(context, url) } }
                    Paragraph(
                        stringResource(if (src.allowsOfflineDownload) R.string.about_source_offline_allowed else R.string.about_source_streaming_only),
                    )
                }
            }
            item { Heading(stringResource(R.string.about_data_sources)) }
            item { Paragraph(stringResource(R.string.about_surah_data)) }
            item { Paragraph(stringResource(R.string.about_featured_note)) }
            item { Paragraph(stringResource(R.string.about_artwork_note)) }

            item { Heading(stringResource(R.string.about_licenses)) }
            item { Paragraph(stringResource(R.string.about_licenses_text)) }
            item { Paragraph(stringResource(R.string.background_license_note)) }

            item { Heading(stringResource(R.string.about_legal)) }
            item { LinkRow(stringResource(R.string.about_privacy_policy), null) { openUrl(context, BuildConfig.PRIVACY_POLICY_URL) } }
            item { LinkRow(stringResource(R.string.about_terms), null) { openUrl(context, BuildConfig.TERMS_URL) } }
            item { LinkRow(stringResource(R.string.about_contact), BuildConfig.CONTACT_EMAIL) { openUrl(context, "mailto:${BuildConfig.CONTACT_EMAIL}") } }
        }
    }
}

@Composable
private fun Heading(text: String) = Text(
    text,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.lg, bottom = Spacing.xs).semantics { heading() },
)

@Composable
private fun Paragraph(text: String) = Text(
    text,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs),
)
