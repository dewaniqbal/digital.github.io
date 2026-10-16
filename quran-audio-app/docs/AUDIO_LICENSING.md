# Audio licensing & source documentation

## Quran recitations

### mp3quran.net (default source)
- Access: public API v3, no credentials; audio on `cdn.mp3quran.net`.
- Terms: **no developer terms or licence were found** (`/api` is a directory index; no terms
  page). mp3quran.net publishes its API openly and many apps use it, but that is not a licence.
- What the app does: streams only (no download, no disk cache), shows attribution in About,
  does not modify audio, does not package any recitation in the APK.
- **Release gate:** obtain written permission (email/contact form) covering streaming in a free,
  ad-supported app, and offline downloads if desired. Record it here. If permission is not
  granted, set `MP3QURAN_ENABLED=false` on the backend and ship with Quran Foundation only.

### Quran Foundation (optional, via backend)
- Developer Terms (https://api-docs.quran.foundation/legal/developer-terms/) permit displaying
  QF content, including in apps with advertising; audio must be **streamed or cached ≤ 7
  days**, must **not be pre-cached**, and raw data must not be redistributed as a separate
  product. Source entry: `allowsOfflineDownload=false`, `maxCacheDays=7`.
- Credentials are server-side only (OAuth client-credentials).

### Offline downloads
Disabled for every current source. To enable for a source that grants permission:
1. set `allowsOfflineDownload = true` for that source (backend `AudioSource`);
2. add Media3 `DownloadManager` + `DownloadService` and a `CacheDataSource` in
   `PlaybackService`, gated per track by the source flag;
3. add a Wi-Fi-only constraint, storage checks and a "Remove download" action.
Never enable it for a source that has not granted offline rights.

## Reciter metadata
Names (English and Arabic), recitation style and narration come from the source APIs.
"Featured" is an editorial list of mp3quran ids (`shared/source/FeaturedReciters.kt`), chosen
only among reciters with a complete recitation; it is labelled as editorial in the app. No
reciter photos or countries are shown because no licensed source provides them.

## Ambient background sounds
All 12 loops (`app/src/main/res/raw/ambient_*.ogg`) are **original audio synthesised by
`tools/generate_ambient_sounds.py`** from noise generators and oscillators with a fixed seed.
No recordings, samples or third-party sound libraries are used. They are released by this
project under **CC0 1.0**. Regenerate with `python3 tools/generate_ambient_sounds.py`
(requires numpy, scipy, ffmpeg with libvorbis). They are procedurally generated, so some (e.g.
cat, owl, birds) are stylised rather than realistic; to replace any with licensed recordings,
keep the same file names, master at −6 dBFS peak, and record the licence here.

## Icons and fonts
Material Symbols (Apache 2.0) via `material-icons-extended`. System fonts only. App icon is an
original vector.
