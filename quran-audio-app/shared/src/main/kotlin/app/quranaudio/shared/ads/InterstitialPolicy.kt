package app.quranaudio.shared.ads

/**
 * When an interstitial ad may be shown. Ads must never interfere with listening, so the rules are
 * deliberately conservative:
 *  - never while Quran audio is playing, buffering or about to play,
 *  - never on (or when navigating to/from) the Now Playing screen,
 *  - only at a natural navigation point, after several navigations,
 *  - at most one per [minIntervalMs] and at most [maxPerSession] per app session,
 *  - never in the first [graceAfterLaunchMs] after launch.
 */
class InterstitialPolicy(
    private val minIntervalMs: Long = 10 * 60_000L,
    private val minNavigationsBetween: Int = 6,
    private val maxPerSession: Int = 2,
    private val graceAfterLaunchMs: Long = 3 * 60_000L,
) {
    data class Context(
        val nowMs: Long,
        val appStartedAtMs: Long,
        val lastShownAtMs: Long?,
        val navigationsSinceLastAd: Int,
        val shownThisSession: Int,
        /** True when Quran audio is playing, buffering, or play has been requested. */
        val playbackActive: Boolean,
        val involvesPlayerScreen: Boolean,
        val adsConsentObtained: Boolean,
    )

    fun mayShow(c: Context): Boolean = when {
        !c.adsConsentObtained -> false
        c.playbackActive -> false
        c.involvesPlayerScreen -> false
        c.shownThisSession >= maxPerSession -> false
        c.nowMs - c.appStartedAtMs < graceAfterLaunchMs -> false
        c.navigationsSinceLastAd < minNavigationsBetween -> false
        c.lastShownAtMs != null && c.nowMs - c.lastShownAtMs < minIntervalMs -> false
        else -> true
    }
}
