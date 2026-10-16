# Release build & Play Store preparation

## 1. Create an upload key (once)
```bash
keytool -genkeypair -v -keystore release.jks -alias quranaudio -keyalg RSA -keysize 4096 -validity 10000
```
Keep `release.jks` and its passwords out of git (see `.gitignore`). Create `keystore.properties`
as described in ENVIRONMENT.md. Enrol in **Play App Signing**.

## 2. Build
```bash
cd quran-audio-app
./gradlew -p shared test
./gradlew -p backend test
./gradlew :app:testReleaseUnitTest :app:lintRelease
./gradlew :app:bundleRelease \
  -PVERSION_CODE=1 -PVERSION_NAME=1.0.0 \
  -PCATALOG_BASE_URL=https://api.your-domain.org/ \
  -PADMOB_APP_ID=ca-app-pub-XXXX~YYYY -PADMOB_BANNER_ID=ca-app-pub-XXXX/1111 -PADMOB_INTERSTITIAL_ID=ca-app-pub-XXXX/2222 \
  -PPRIVACY_POLICY_URL=https://your-domain.org/privacy -PTERMS_URL=https://your-domain.org/terms -PCONTACT_EMAIL=support@your-domain.org
# AAB: app/build/outputs/bundle/release/app-release.aab
./gradlew :app:assembleRelease ...same -P flags...   # APK for side-loading/testing
```
Release builds are minified and resource-shrunk (R8). Verify on a device before upload:
playback in background, lock-screen controls, Bluetooth, sleep timer, ads labelled correctly.

Verify no secrets are in the artifact:
```bash
unzip -p app/build/outputs/apk/release/app-release.apk classes*.dex | strings | grep -iE "secret|client_secret|BEGIN PRIVATE" || echo "clean"
```

## 3. Play Store checklist
- [ ] Audio permissions confirmed in writing and recorded in AUDIO_LICENSING.md (release gate)
- [ ] Production AdMob ids, GDPR/US messages published, sensitive categories blocked, `app-ads.txt` live
- [ ] Privacy policy URL live and identical in app and Play Console; Data safety form completed (PRIVACY.md)
- [ ] Target API level meets the current Play requirement (`targetSdk = 36`)
- [ ] Foreground service declaration: type **mediaPlayback** — justification "plays Quran audio the user started, with media controls"
- [ ] `POST_NOTIFICATIONS` requested in context (first playback), not at launch
- [ ] Content rating questionnaire (reference/education; no user-generated content)
- [ ] Ads declaration = "Contains ads"; target audience (not primarily children unless Families compliant)
- [ ] Store listing: honest reciter count ("170+ reciters with the complete Quran" only if the live catalogue shows it), screenshots of real UI, no third-party logos
- [ ] Accessibility pass with TalkBack and 200 % font size
- [ ] Internal testing track → closed testing (≥ 14 days with testers if your account requires it) → production
- [ ] Backend deployed with HTTPS, secrets in a secret manager, monitoring on `/health`
