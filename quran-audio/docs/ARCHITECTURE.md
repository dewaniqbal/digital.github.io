# Quran Audio (Android) — Architecture & Plan

## Status (Milestone 1)
| Item | State |
|---|---|
| Version catalog, Gradle skeleton | done |
| `core:model` (domain models, playback state machine, retry policy) | done, **18 JVM unit tests passing** |
| `core:common` (AppResult/AppError, dispatchers) | done, tested |
| `network_security_config.xml` (HTTPS only, system CAs only) | done |
| Android modules (everything below marked ⏳) | not started — see "Blocked" |

**Blocked / needs from you:**
1. Backend base URL + sample JSON (reciters, surahs, audio URL scheme). DTOs and validation cannot be written honestly without it.
2. The build sandbox cannot reach `dl.google.com`, so AGP / AndroidX / Media3 cannot be resolved here. Android modules would be unverified until built in Android Studio or CI. I chose to ship only code I could compile and test rather than hand you a large uncompiled tree.

## Module structure
```
app/                      ⏳ Application, MainActivity, NavHost, Hilt entry, flavors (dev/staging/prod), R8
core/
  model/                  ✅ pure Kotlin: domain models + PlaybackStateMachine + RetryPolicy
  common/                 ✅ pure Kotlin: AppResult, AppError, DispatcherProvider
  network/                ⏳ Retrofit/OkHttp/kotlinx-serialization, DTOs + validating mappers (https-only URLs, range checks)
  database/               ⏳ Room (reciters, surahs, playlists, favourites, recently played, playback position) + DataStore prefs
  audio/                  ⏳ PlaybackService (MediaSessionService), ExoPlayer adapter → state machine, ambient-sound player, focus/noisy/call handling, queue persistence
  designsystem/           ⏳ Material 3 theme, shared components, a11y helpers
feature/
  home/ reciters/ surahs/ playlists/ player/ search/ settings/   ⏳ (screen + ViewModel + use cases + repository interfaces' impls where feature-specific)
```
Deviations from the brief, deliberately:
- **`core:security` dropped**: its content is the network security config, no-secrets rule, and R8 setup — all live in `app` and `core:network`. A near-empty module is over-engineering. Keystore-backed storage gets added to `core:database` only if a secret ever exists (V1 has none).
- **`feature:backgroundsound` merged into `core:audio` + `feature:settings`**: it is one player instance plus a toggle/volume setting behind a feature flag.
- `core:model` holds the playback state machine because it is pure domain logic; this lets it be fully unit tested on the JVM.

## Layering (UDF)
Composable → ViewModel (`StateFlow<UiState>` + `onEvent`) → UseCase → Repository (interface in domain, impl in data) → Remote (Retrofit) / Local (Room, DataStore). Room is the single source of truth: repositories expose Room flows and refresh from network in the background, so screens are never blank offline.

## Audio design
`PlaybackService` owns ExoPlayer + MediaSession. Player callbacks → `PlaybackEvent` → `PlaybackStateMachine.reduce` → `StateFlow<PlaybackSnapshot>`. Failures go through `RetryPolicy` (exponential backoff, max 3) then `SuggestNext`. Position/queue persisted periodically and on pause/destroy for process-death restore. `setHandleAudioBecomingNoisy(true)` + `AudioAttributes(handleAudioFocus=true)` cover headphone disconnect, calls, focus loss.

## Security baseline (MASVS)
HTTPS only, system CAs only, no secrets in APK, `allowBackup=false`, R8 + resource shrinking in release, Timber planted in debug only, response validation in mappers, per-flavor base URLs via BuildConfig (non-secret), pinning added once backend host is known. CI: dependency vulnerability scan, Detekt, Lint.

## Build
`cd quran-audio && gradle test` (JVM modules). Android modules will be added to `settings.gradle.kts` as they land.

## Roadmap (one PR-sized step each)
1. network + database + repositories (needs API sample)
2. audio service + player screen
3. home, reciters, surahs
4. playlists, favourites, search
5. settings + ambient sound
6. CI (lint, detekt, dependency scan), UI tests
