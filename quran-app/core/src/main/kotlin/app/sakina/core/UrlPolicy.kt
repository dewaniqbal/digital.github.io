package app.sakina.core

import java.net.URI

/** Strict validation for every external URL the app will hand to a network stack. */
object UrlPolicy {
    private val IPV4 = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")

    /** Returns the normalized URL if it is safe to use for [allowedHosts], otherwise null. */
    fun validateAudioUrl(raw: String, allowedHosts: List<String>): String? {
        val uri = parse(raw) ?: return null
        val host = uri.host?.lowercase() ?: return null
        return if (allowedHosts.any { hostMatches(host, it) }) uri.toString() else null
    }

    /** HTTPS-only, no credentials, standard port, hostname (never an IP literal or localhost). */
    fun parse(raw: String): URI? {
        if (raw.length > 2048 || raw.any { it.isISOControl() || it == ' ' }) return null
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        if (!"https".equals(uri.scheme, ignoreCase = true)) return null
        if (uri.userInfo != null) return null
        if (uri.port != -1 && uri.port != 443) return null
        val host = uri.host?.lowercase() ?: return null
        if (host == "localhost" || host.endsWith(".local") || host.endsWith(".internal")) return null
        if (IPV4.matches(host) || host.contains(':') || '.' !in host) return null
        return uri
    }

    /** Pattern is an exact host or "*.suffix" (matches subdomains only, not the bare suffix). */
    fun hostMatches(host: String, pattern: String): Boolean {
        val p = pattern.trim().lowercase()
        if (p.isEmpty()) return false
        return if (p.startsWith("*.")) host.endsWith(p.substring(1)) && host.length > p.length - 1 else host == p
    }

    fun isValidTemplate(template: String): Boolean {
        if (!template.startsWith("https://")) return false
        val probe = template.replace("{surah3}", "001").replace("{surah}", "1")
        return '{' !in probe && '}' !in probe && parse(probe) != null
    }
}
