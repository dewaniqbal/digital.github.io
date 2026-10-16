# Architecture

## Layers

```
Compose UI (screens, design-system components)
   ↓ observes StateFlow / sends intents
ViewModels (Hilt, one per screen)
   ↓
Use cases (PlayUseCase — queue building; thin on purpose)
   ↓
Repositories (CatalogRepository, LibraryRepository, LastSessionStore)
   ↓                                   ↓
Remote data source                 Local data sources
(Retrofit: backend or mp3quran)    (Room, DataStore)
```

- **Single source of truth:** the UI observes Room. `CatalogRepository.refresh()` fetches,
  validates (`CatalogValidator`), and atomically replaces the cached catalogue. The app starts
  instantly from cache and stays usable offline once a catalogue is loaded.
- **No networking in composables.** Composables only call ViewModel functions.
- **Shared module:** platform-independent rules (Surah catalogue, catalogue contract and
  validation, gain rules, sleep-timer state, resume policy, search normalisation, interstitial
  policy) live in `shared/` and are unit-tested on the JVM; the backend uses the same code.

## Catalogue

`shared/catalog/Catalog.kt` defines the normalised contract (`Catalog → sources, reciters →
recitations`). Each recitation carries an HTTPS URL template (`{surah3}` / `{surah}`) or
explicit per-Surah URLs plus the list of Surahs it really contains. Sources declare
`allowsOfflineDownload` and `maxCacheDays`.

Two interchangeable remote data sources produce this contract:

| `CATALOG_BASE_URL` | Data source | Notes |
|---|---|---|
| empty (default) | `Mp3QuranDirectDataSource` | public API, no secrets, mapping done on-device |
| `https://…` | `BackendCatalogDataSource` | our proxy; adds Quran Foundation, ETag caching, server-side curation |

New reciters or sources are added server-side without an app update. For safety the app only
streams from allow-listed audio hosts (`AppModule.ALLOWED_AUDIO_HOSTS`); a brand-new CDN host
needs one line in the app.

## Playback (Media3)

- `PlaybackService` (`MediaSessionService`, foreground type `mediaPlayback`) owns one
  `ExoPlayer` and the `MediaSession` → notification, lock screen, Bluetooth/headset buttons,
  Wear/Auto surfaces.
- Audio focus handled by ExoPlayer (`handleAudioFocus = true`): phone calls and other apps pause
  or duck the recitation and resume it afterwards. `setHandleAudioBecomingNoisy(true)` pauses
  when headphones are unplugged or Bluetooth disconnects. `WAKE_MODE_NETWORK` keeps streaming
  with the screen off. No polling loops: progress is saved every 10 s **only while playing**.
- Buffer 60–180 s and 6 load retries ride out short network drops; on network loss the
  service waits for connectivity (event-driven `ConnectivityMonitor`) and re-prepares from the
  same position.
- `onAddMediaItems` re-validates every URI against the host allow-list (controllers cannot
  inject arbitrary URLs). `onPlaybackResumption` restores the last queue for Bluetooth/system
  "play" after an app or device restart.
- `PlaybackConnection` (UI side) wraps a `MediaController` and exposes `PlayerUiState`; its
  progress flow is cold and only ticks while a screen collects it.
- Auto-play next OFF and the "End of Surah" sleep timer use ExoPlayer's
  `pauseAtEndOfMediaItems`. Duration timers are a single suspended coroutine on the monotonic
  clock; on expiry the Quran fades out and the ambient sound stops.
- Resume: per-track positions in `listening_history`; the whole queue in DataStore
  (`LastSessionStore`). `ResumePolicy` skips near-start/near-end positions and rewinds 2 s.

## Ambient background sound

`AmbientSoundController` owns a second ExoPlayer (looping raw resource, **no audio focus**), so
it never fights the recitation for focus. It follows the Quran player: it pauses whenever the
recitation is interrupted (call, other app, unplugged headphones) and, by default, whenever the
Quran stops. It is OFF by default and its on-state is never restored on launch — only the chosen
sound and volumes persist. Gain = `AudioMix.backgroundGain()`: perceptual curve, capped at 50 %
of the Quran gain; assets are mastered at −6 dBFS peak, so the mix cannot clip.

## Data model (Room v1)

| Table | Purpose |
|---|---|
| `sources` | audio sources and their terms (`allowsOfflineDownload`, `maxCacheDays`) |
| `reciters`, `recitations` | cached catalogue (replaced on refresh) |
| `favourite_reciters`, `favourite_tracks` | favourites |
| `listening_history` | last position, duration, play count, last played — per track |
| `playlists`, `playlist_tracks` | user playlists with ordered entries |

User tables reference catalogue rows by stable string ids **without foreign keys**, so a
catalogue refresh (or a reciter temporarily missing upstream) never deletes user data. Schemas
are exported to `app/schemas/` on build (commit `app/schemas/**/1.json` from your first local
build and every later version — it is what migration tests compare against); destructive migration is never enabled — add a `Migration` to
`AppDatabase.MIGRATIONS` for every version bump. Surah metadata is static code
(`SurahCatalog`), preferences are DataStore.

## Ads

`AdsManager` gathers UMP consent before any request, sets max content rating **G**, and only
shows interstitials through `InterstitialPolicy` (never while audio plays/buffers, never
around the player, ≥10 min apart, ≤2 per session, ≥6 navigations, 3-min launch grace).
Banners (`AdBanner`) appear inline at the end of browsing lists only, labelled "Advertisement".

## Accessibility & i18n

All strings are resources (`values/strings.xml`), `supportsRtl=true`, start/end paddings,
Arabic names rendered with platform fonts. Touch targets ≥48 dp, content descriptions on
icon buttons, state descriptions on toggles (state never shown by colour alone), headings
marked for TalkBack, sp-based typography for large text. Adding Arabic/Bengali/Urdu/French/
Indonesian/Turkish UI = adding `values-xx/strings.xml`.

## Decisions & trade-offs

- **No reciter photos** rather than unlicensed images. **No country categories** until data exists.
- **Downloads off** until a source grants offline rights (keeps the app compliant).
- **Simple recommendations** (featured complete reciters you haven't tried) — explainable, local.
- **No analytics SDK.** Fewer dependencies, simpler privacy story. If analytics are added later,
  track only the anonymous events listed in docs/PRIVACY.md behind a single interface.
