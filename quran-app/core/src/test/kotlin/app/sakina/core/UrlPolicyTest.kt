package app.sakina.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UrlPolicyTest {
    private val hosts = listOf("audio.example.org", "*.cdn.example.org")

    @Test fun acceptsAllowedHttps() {
        assertNotNull(UrlPolicy.validateAudioUrl("https://audio.example.org/a.mp3", hosts))
        assertNotNull(UrlPolicy.validateAudioUrl("https://x.cdn.example.org/a.mp3", hosts))
        assertNotNull(UrlPolicy.validateAudioUrl("https://AUDIO.example.org:443/a.mp3", hosts))
    }

    @Test fun rejectsEverythingUnsafe() {
        listOf(
            "http://audio.example.org/a.mp3",
            "https://cdn.example.org/a.mp3",              // wildcard excludes bare suffix
            "https://evilcdn.example.org.evil.test/a.mp3",
            "https://user:pw@audio.example.org/a.mp3",
            "https://audio.example.org:8443/a.mp3",
            "https://127.0.0.1/a.mp3",
            "https://localhost/a.mp3",
            "https://[::1]/a.mp3",
            "ftp://audio.example.org/a.mp3",
            "file:///etc/passwd",
            "javascript:alert(1)",
            "https://audio.example.org/a b.mp3",
            "",
        ).forEach { assertNull(UrlPolicy.validateAudioUrl(it, hosts), it) }
    }

    @Test fun hostMatching() {
        assertTrue(UrlPolicy.hostMatches("a.b.example.org", "*.example.org"))
        assertFalse(UrlPolicy.hostMatches("example.org", "*.example.org"))
        assertFalse(UrlPolicy.hostMatches("x", ""))
    }

    @Test fun templates() {
        assertTrue(UrlPolicy.isValidTemplate("https://audio.example.org/{surah3}.mp3"))
        assertFalse(UrlPolicy.isValidTemplate("http://audio.example.org/{surah3}.mp3"))
        assertFalse(UrlPolicy.isValidTemplate("https://audio.example.org/{other}.mp3"))
    }
}
