# QA: automated tests and manual checklist

## Automated (run in CI on every push — `.github/workflows/quran-audio-app.yml`)
| Suite | Covers |
|---|---|
| `shared` (JVM) | 114 Surahs & 6236 verses; mp3quran mapping from real API fixtures (padding, partial reciters, styles, Arabic names, bad data); catalogue validation (HTTPS/allow-list/injection); sleep timer; gain cap & fades; resume policy; Arabic/transliteration search; interstitial policy |
| `backend` (JVM) | normalised endpoint, ETag/304, 503 + Retry-After, last-good-copy on upstream failure, per-client rate limit, secret redaction, QF OAuth token caching & mapping |
| `app` (Robolectric) | Room catalogue refresh/validation/failure/304/staleness, track resolution, favourites, playlist create/add/reorder/remove/rename/delete, 114-item playlist order, history & resume, user data surviving catalogue refresh, MediaItem round-trip |

## Manual device checklist (perform on a physical phone, Android 8 and 14+)
- [ ] App launches; onboarding once; Home loads (skeleton → content)
- [ ] Reciters load; count shown equals live catalogue; search & filters work
- [ ] All 114 Surahs listed; Surah → reciter picker plays
- [ ] Play / pause / seek / next / previous; mini player; Now Playing; queue reorder/remove
- [ ] Lock phone → keeps playing; lock-screen + notification controls work
- [ ] Bluetooth headset buttons; disconnecting Bluetooth or unplugging headphones pauses
- [ ] Incoming call pauses, resumes after; other app audio takes focus correctly
- [ ] Airplane mode mid-Surah → buffers then snackbar; reconnect → resumes at the same place
- [ ] Wi-Fi only on mobile data → blocked with message
- [ ] Sleep timer 5 min and End of Surah → fades & stops Quran and background sound
- [ ] Background sound: off by default; select → plays quietly; sliders independent; background never louder than Quran; pauses with Quran (default); not restored after relaunch
- [ ] Favourites, playlists, history persist across app restart and app update
- [ ] Kill app while playing → swipe away keeps playing; reboot → Bluetooth "play" resumes last Surah
- [ ] Rotation / dark-light theme / 200 % font / TalkBack labels on all controls
- [ ] Ads: banners only on browsing screens, labelled; no interstitial while audio active or around the player
- [ ] Rapid next/next/next taps don't crash; very slow network shows buffering, not errors
- [ ] Low storage: app still streams (no disk cache used)
