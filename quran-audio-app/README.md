# Quran Audio — free Android Quran listening app

A calm, fast, ad-supported (respectfully) Android app for listening to complete Quran
recitations: all 114 Surahs, a verified catalogue of reciters, background playback with
lock-screen / Bluetooth controls, playlists, favourites, sleep timer and optional ambient
background sounds. No account, no subscription.

```
OPEN APP → HOME → CHOOSE RECITER → CHOOSE SURAH → PLAY → LOCK PHONE
→ QURAN CONTINUES → CONTROL FROM LOCK SCREEN → RETURN → CONTINUE FROM LAST POSITION
```

## Repository layout

| Path | What | Build |
|---|---|---|
| `app/` | Android app (Kotlin, Jetpack Compose, Material 3, Media3, Room, DataStore, Hilt, AdMob) | `./gradlew :app:assembleDebug` |
| `shared/` | Pure-Kotlin module shared by app and backend: 114-Surah catalogue, catalogue contract + validator, mp3quran.net mapper, sleep-timer / audio-mix / resume / search / ad-policy rules | `./gradlew -p shared test` |
| `backend/` | Optional Ktor catalogue proxy: keeps upstream credentials server-side, validates and caches the catalogue | `./gradlew -p backend test buildFatJar` |
| `tools/generate_ambient_sounds.py` | Generates the 12 original (CC0) ambient loops in `app/src/main/res/raw` | `python3 tools/generate_ambient_sounds.py` |
| `docs/` | Architecture, setup, licensing, privacy and release documentation | — |

## Quick start (development)

Requirements: JDK 17+ (21 recommended), Android SDK with platform 37, Android Studio (latest stable).

```bash
cd quran-audio-app
./gradlew -p shared test            # shared logic tests
./gradlew -p backend test           # backend tests
./gradlew :app:assembleDebug        # debug APK -> app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # app unit tests (Robolectric)
```

With no configuration the debug app talks directly to the public **mp3quran.net API v3**
(no credentials needed) and uses **Google's test AdMob ids**. To use the backend proxy set
`CATALOG_BASE_URL` (see [docs/ENVIRONMENT.md](docs/ENVIRONMENT.md)).

## Documentation

- [Architecture](docs/ARCHITECTURE.md) — layers, playback design, data model, decisions
- [Environment variables](docs/ENVIRONMENT.md) — every build/runtime setting
- [API & backend setup](docs/API_SETUP.md) — mp3quran.net, Quran Foundation OAuth, deploying the proxy
- [AdMob setup](docs/ADMOB_SETUP.md) — ad units, consent, respectful-ads rules
- [Audio licensing & sources](docs/AUDIO_LICENSING.md) — what is used, under which terms, open items
- [Privacy policy requirements](docs/PRIVACY.md) — data inventory and policy checklist
- [Release & Play Store](docs/RELEASE.md) — signing, release build, store checklist
- [QA checklist & test plan](docs/QA.md) — automated coverage and the manual device checklist

## Status and honest limitations

- **Reciters:** the catalogue is built from live source data. On 2026-10-05 mp3quran.net
  listed **242 reciters, 172 of them with all 114 Surahs**. The app displays the real number
  it receives; it never pads the count. Reciters with partial coverage show exactly the Surahs
  the source lists.
- **No reciter photos / countries:** the sources provide neither, so the app uses generated
  artwork and hides country filters until a source supplies reliable country data.
- **Downloads are disabled:** neither source currently grants offline storage (Quran
  Foundation forbids caching audio > 7 days; mp3quran.net publishes no terms). The data model
  and UI read `allowsOfflineDownload` per source so the feature can be enabled once permission
  is documented — see [docs/AUDIO_LICENSING.md](docs/AUDIO_LICENSING.md).
- **Before production** you must: obtain/confirm audio permissions, replace AdMob ids,
  publish a privacy policy, and sign the release — see [docs/RELEASE.md](docs/RELEASE.md).
