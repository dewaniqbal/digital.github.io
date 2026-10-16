# AdMob setup

## Development
Debug builds use Google's official **test** ids (app `ca-app-pub-3940256099942544~3347511713`,
banner `…/9214589741`, interstitial `…/1033173712`). Never click real ads on your own device.

## Production
1. Create the app in AdMob, then one **Adaptive banner** and one **Interstitial** unit.
2. Build with `-PADMOB_APP_ID=… -PADMOB_BANNER_ID=… -PADMOB_INTERSTITIAL_ID=…`
   (or env vars). Missing values ⇒ ads disabled in release.
3. In AdMob → *Privacy & messaging*: create a **GDPR** message (and US state regulations
   message). The app calls UMP on every launch and shows "Ad privacy choices" in Settings
   when required.
4. In AdMob → *Blocking controls*, block sensitive categories that conflict with a Quran app
   (e.g. gambling, alcohol, dating, sexual/suggestive content, astrology). The app already sets
   **max ad content rating = G**.
5. Publish `app-ads.txt` on the developer website listed in Play Console.

## Placement rules implemented
- Banners: inline at the end of Home, Reciters, Search results and Playlists — labelled
  "Advertisement", never on Now Playing, Background Sound, or over Quran content.
- Interstitials (`shared/ads/InterstitialPolicy.kt`): only on navigation between browsing
  screens; never while recitation plays, buffers or is about to play; never to/from the
  player; ≥ 10 minutes apart; ≤ 2 per session; ≥ 6 navigations between; none in the first 3
  minutes; only after consent.
- No rewarded ads, no ad-gated listening, no fake buttons or notifications.

## Families policy
If the app's target audience includes children, you must comply with Google Play Families
policy (self-certified ad SDK settings, `tagForChildDirectedTreatment`). The current
configuration targets a general audience.
