package app.quranaudio.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quranaudio.BuildConfig
import app.quranaudio.R
import app.quranaudio.ui.theme.Spacing
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdsEntryPoint {
    fun adsManager(): AdsManager
}

/**
 * Inline adaptive banner for browsing screens (Home, Reciters, Search, Playlists) only.
 * Clearly labelled as an advertisement; never placed on the player or over Quran content.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current || !BuildConfig.ADS_ENABLED) return
    val context = LocalContext.current
    val manager = remember { EntryPointAccessors.fromApplication(context.applicationContext, AdsEntryPoint::class.java).adsManager() }
    val canRequest by manager.canRequestAds.collectAsStateWithLifecycle()
    if (!canRequest) return
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adView = remember(widthDp) {
        AdView(context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            loadAd(manager.adRequest())
        }
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }
    Column(modifier.fillMaxWidth().padding(horizontal = Spacing.screen), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.ad_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AndroidView(factory = { adView })
        }
    }
}
