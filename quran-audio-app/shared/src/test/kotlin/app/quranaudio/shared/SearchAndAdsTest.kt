package app.quranaudio.shared

import app.quranaudio.shared.ads.InterstitialPolicy
import app.quranaudio.shared.search.SearchNormalizer
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SearchAndAdsTest {

    private fun matches(query: String, text: String) =
        SearchNormalizer.score(SearchNormalizer.normalize(query), SearchNormalizer.normalize(text)) > 0

    @Test fun `transliteration variants match`() {
        assertThat(matches("alafasy", "Mishary Alafasi")).isTrue()
        assertThat(matches("Al-Afasi", "Mishary Alafasi")).isTrue()
        assertThat(matches("mishari", "Mishary Alafasi")).isTrue()
        assertThat(matches("shuraym", "Saud Al-Shuraim")).isTrue()
        assertThat(matches("abdul basit", "Abdulbasit Abdulsamad")).isTrue()
        assertThat(matches("minshawi", "Mohammed Siddiq Al-Minshawi")).isTrue()
        assertThat(matches("baqara", "Al-Baqarah")).isTrue()
        assertThat(matches("yasin", "Ya-Sin")).isTrue()
        assertThat(matches("imran", "Ali 'Imran")).isTrue()
    }

    @Test fun `arabic matches with and without diacritics and alef forms`() {
        assertThat(matches("الفاتحه", "الفاتحة")).isTrue()
        assertThat(matches("ابراهيم", "إبراهيم الأخضر")).isTrue()
        assertThat(matches("المنشاوي", "محمد صديق المنشاوي")).isTrue()
        assertThat(matches("البَقَرَة", "البقرة")).isTrue()
    }

    @Test fun `non matches and ranking`() {
        assertThat(matches("sudais", "Mishary Alafasi")).isFalse()
        assertThat(matches("", "anything")).isFalse()
        val q = SearchNormalizer.normalize("al-kahf")
        val exact = SearchNormalizer.score(q, SearchNormalizer.normalize("Al-Kahf"))
        val partial = SearchNormalizer.score(SearchNormalizer.normalize("kah"), SearchNormalizer.normalize("Al-Kahf"))
        assertThat(exact).isGreaterThan(partial)
    }

    private val policy = InterstitialPolicy()
    private val ok = InterstitialPolicy.Context(
        nowMs = 60 * 60_000L, appStartedAtMs = 0, lastShownAtMs = null, navigationsSinceLastAd = 10,
        shownThisSession = 0, playbackActive = false, involvesPlayerScreen = false, adsConsentObtained = true,
    )

    @Test fun `interstitial allowed only at quiet navigation points`() {
        assertThat(policy.mayShow(ok)).isTrue()
        assertThat(policy.mayShow(ok.copy(playbackActive = true))).isFalse()
        assertThat(policy.mayShow(ok.copy(involvesPlayerScreen = true))).isFalse()
        assertThat(policy.mayShow(ok.copy(adsConsentObtained = false))).isFalse()
        assertThat(policy.mayShow(ok.copy(navigationsSinceLastAd = 2))).isFalse()
        assertThat(policy.mayShow(ok.copy(shownThisSession = 2))).isFalse()
        assertThat(policy.mayShow(ok.copy(lastShownAtMs = ok.nowMs - 60_000))).isFalse()
        assertThat(policy.mayShow(ok.copy(nowMs = 60_000))).isFalse()
    }
}
