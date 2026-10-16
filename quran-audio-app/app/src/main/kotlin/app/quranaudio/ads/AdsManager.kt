package app.quranaudio.ads

import android.app.Activity
import android.content.Context
import app.quranaudio.BuildConfig
import app.quranaudio.shared.ads.InterstitialPolicy
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Mobile Ads with respectful defaults:
 *  - consent is gathered with Google's User Messaging Platform before any ad request;
 *  - maximum ad content rating is G (family-safe) for a religious app;
 *  - interstitials only at natural navigation points and never while Quran audio is active
 *    (see InterstitialPolicy); no ads at all on the Now Playing screen;
 *  - release builds without production ad ids have ads disabled (never Google test ids).
 */
@Singleton
class AdsManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val consentInfo: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val initialized = AtomicBoolean(false)
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val policy = InterstitialPolicy()
    private val appStartedAt = System.currentTimeMillis()
    private var lastInterstitialAt: Long? = null
    private var navigationsSinceAd = 0
    private var shownThisSession = 0
    private var interstitial: InterstitialAd? = null
    private var loading = false

    val enabled: Boolean get() = BuildConfig.ADS_ENABLED

    val privacyOptionsRequired: Boolean
        get() = consentInfo.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Call from the activity at start-up. Shows the consent form only when required. */
    fun gatherConsent(activity: Activity) {
        if (!enabled) return
        val params = ConsentRequestParameters.Builder().build()
        consentInfo.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> onConsentResolved() }
        }, { _ -> onConsentResolved() })
        // Consent from a previous session lets ads start immediately.
        if (consentInfo.canRequestAds()) onConsentResolved()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> onConsentResolved() }
    }

    private fun onConsentResolved() {
        _canRequestAds.value = consentInfo.canRequestAds()
        if (!consentInfo.canRequestAds() || !initialized.compareAndSet(false, true)) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build(),
        )
        MobileAds.initialize(context) {}
    }

    fun adRequest(): AdRequest = AdRequest.Builder().build()

    /** Record a navigation between browsing screens (a "natural point" for an interstitial). */
    fun onNavigation(activity: Activity, playbackActive: Boolean, involvesPlayer: Boolean) {
        if (!enabled) return
        navigationsSinceAd++
        val ctx = InterstitialPolicy.Context(
            nowMs = System.currentTimeMillis(),
            appStartedAtMs = appStartedAt,
            lastShownAtMs = lastInterstitialAt,
            navigationsSinceLastAd = navigationsSinceAd,
            shownThisSession = shownThisSession,
            playbackActive = playbackActive,
            involvesPlayerScreen = involvesPlayer,
            adsConsentObtained = _canRequestAds.value,
        )
        val ad = interstitial
        if (ad != null && policy.mayShow(ctx)) {
            interstitial = null
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() = preload()
                override fun onAdFailedToShowFullScreenContent(error: AdError) = preload()
            }
            lastInterstitialAt = ctx.nowMs
            navigationsSinceAd = 0
            shownThisSession++
            ad.show(activity)
        } else if (ad == null) {
            preload()
        }
    }

    private fun preload() {
        if (!enabled || !_canRequestAds.value || loading || interstitial != null) return
        loading = true
        InterstitialAd.load(context, BuildConfig.ADMOB_INTERSTITIAL_ID, adRequest(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
                loading = false
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                loading = false
            }
        })
    }
}
