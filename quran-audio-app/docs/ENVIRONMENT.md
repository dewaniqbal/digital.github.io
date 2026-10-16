# Environment variables & build configuration

Nothing secret is committed. Values come from Gradle properties (`-PNAME=value` or
`~/.gradle/gradle.properties`) or environment variables of the same name.

## Android app (build time)

| Name | Default | Purpose |
|---|---|---|
| `CATALOG_BASE_URL` | *(empty)* | HTTPS base URL of the backend proxy, e.g. `https://api.example.org/`. Empty = call the public mp3quran.net API directly. |
| `ADMOB_APP_ID` | Google test app id (debug) / none (release) | AdMob **app** id `ca-app-pub-…~…`. |
| `ADMOB_BANNER_ID` | Google test banner (debug) | Banner ad unit id. |
| `ADMOB_INTERSTITIAL_ID` | Google test interstitial (debug) | Interstitial ad unit id. |
| `PRIVACY_POLICY_URL` | `https://example.org/privacy` | Shown in Settings/About. **Must be replaced** before release. |
| `TERMS_URL` | `https://example.org/terms` | Terms of use page. |
| `CONTACT_EMAIL` | `support@example.org` | Support address shown in About. |
| `VERSION_CODE` / `VERSION_NAME` | `1` / `1.0.0` | Release versioning (set by CI). |

Release builds enable ads **only** when all three `ADMOB_*` values are provided; otherwise ads
are disabled entirely (Google's test ids are never shipped in a release build).

Release signing reads `keystore.properties` in the project root (git-ignored):

```properties
storeFile=release.jks
storePassword=…
keyAlias=quranaudio
keyPassword=…
```

## Backend (runtime)

| Name | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port. Terminate TLS at your load balancer / reverse proxy. |
| `MP3QURAN_ENABLED` | `true` | Include mp3quran.net reciters. |
| `QF_CLIENT_ID` | — | Quran Foundation OAuth client id (enables the QF provider). |
| `QF_CLIENT_SECRET` | — | Quran Foundation client secret. **Server-side only.** |
| `QF_AUTH_URL` | `https://oauth2.quran.foundation` | Use the pre-live URL while testing. |
| `QF_API_URL` | `https://apis.quran.foundation/content/api/v4` | Content API base. |
| `CATALOG_REFRESH_MINUTES` | `360` | Upstream refresh interval (5–1440). |
| `RATE_LIMIT_PER_MINUTE` | `60` | Per-client request limit for `/v1/catalog`. |

Store secrets in your platform's secret manager (Cloud Run secrets, Fly secrets, Kubernetes
secrets…), never in the image or repository.
