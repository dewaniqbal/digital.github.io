# Privacy policy requirements

The app is designed to collect the minimum: no account, no analytics SDK, no listening data
sent anywhere.

## Data inventory (for the privacy policy and the Play "Data safety" form)

| Data | Where | Shared? | Purpose |
|---|---|---|---|
| Favourites, playlists, listening history, positions, preferences | On device (Room, DataStore); included in Android backup | No | Core features |
| Catalogue requests (IP address, User-Agent `QuranAudio-Android/<version>`) | Your backend / mp3quran.net | Processed by the server | Load reciter list |
| Audio streaming requests (IP address) | Audio CDNs (cdn.mp3quran.net; QF hosts if enabled) | Processed by the CDN | Play recitations |
| Advertising ID, device & app info, coarse location from IP, ad interactions | Google Mobile Ads SDK | **Yes — Google** | Ads, frequency capping, fraud prevention, measurement |
| Consent choices | UMP SDK (on device) | Google | GDPR/US-state consent |

Listening history is **never** transmitted. No analytics events are collected.

## The published policy must state
1. Controller identity and contact (`CONTACT_EMAIL`).
2. The table above in plain language; that no account is required; that local data can be
   removed via Settings → Recently Played → Clear history, or by clearing app data/uninstalling.
3. Advertising by Google AdMob, link to Google's partner-sites policy
   (https://policies.google.com/technologies/partner-sites), how to change consent (Settings →
   Ad privacy choices) and reset/delete the Advertising ID in Android settings.
4. Third-party audio/data providers (mp3quran.net, Quran Foundation if enabled) and that
   their servers receive standard request data.
5. Children: the app is not directed at children under 13 (or adapt ads config per Families policy).
6. Legal bases (GDPR: consent for personalised ads; legitimate interest/contract for streaming),
   retention (local data until deleted; server logs per your hosting), user rights, changes.

Host it at a stable HTTPS URL and set `PRIVACY_POLICY_URL`; enter the same URL in Play Console.

## Play Data safety answers (current build)
- Data collected: Device or other IDs (advertising) — by Google Mobile Ads; App interactions
  (ad interactions) — by Google Mobile Ads; Approximate location (from IP) — by ads SDK.
- Data shared: the above with Google for advertising.
- Encrypted in transit: yes (HTTPS only). Users can request deletion: local data in-app.

## If analytics are added later
Use a single `Analytics` interface, anonymous events only (`app_open`, `play_started`,
`play_completed`, `reciter_selected`, `surah_selected`, `playlist_created`,
`background_sound_used`), no listening history or identifiers, a Settings toggle, and update
this document and the Data safety form.
