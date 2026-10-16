package app.quranaudio.backend

/**
 * Runtime configuration, read from environment variables only (never from source control).
 * See docs/ENVIRONMENT.md for every variable.
 */
data class Config(
    val port: Int = 8080,
    val mp3QuranEnabled: Boolean = true,
    val quranFoundation: QuranFoundationConfig? = null,
    val refreshIntervalMinutes: Long = 360,
    val requestsPerMinutePerClient: Int = 60,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): Config {
            val qfId = env["QF_CLIENT_ID"]?.takeIf { it.isNotBlank() }
            val qfSecret = env["QF_CLIENT_SECRET"]?.takeIf { it.isNotBlank() }
            return Config(
                port = env["PORT"]?.toIntOrNull() ?: 8080,
                mp3QuranEnabled = env["MP3QURAN_ENABLED"]?.toBooleanStrictOrNull() ?: true,
                quranFoundation = if (qfId != null && qfSecret != null) {
                    QuranFoundationConfig(
                        clientId = qfId,
                        clientSecret = qfSecret,
                        authBaseUrl = env["QF_AUTH_URL"] ?: QuranFoundationConfig.PROD_AUTH,
                        apiBaseUrl = env["QF_API_URL"] ?: QuranFoundationConfig.PROD_API,
                    )
                } else null,
                refreshIntervalMinutes = env["CATALOG_REFRESH_MINUTES"]?.toLongOrNull()?.coerceIn(5, 24 * 60) ?: 360,
                requestsPerMinutePerClient = env["RATE_LIMIT_PER_MINUTE"]?.toIntOrNull()?.coerceAtLeast(1) ?: 60,
            )
        }
    }
}

data class QuranFoundationConfig(
    val clientId: String,
    val clientSecret: String,
    val authBaseUrl: String = PROD_AUTH,
    val apiBaseUrl: String = PROD_API,
) {
    override fun toString() = "QuranFoundationConfig(clientId=$clientId, clientSecret=***, authBaseUrl=$authBaseUrl, apiBaseUrl=$apiBaseUrl)"

    companion object {
        const val PROD_AUTH = "https://oauth2.quran.foundation"
        const val PROD_API = "https://apis.quran.foundation/content/api/v4"
    }
}
