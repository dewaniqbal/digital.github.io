# API & backend setup

## 1. Sources at a glance (verified 2026-10-05)

| Source | Auth | What we use | Terms relevant to us |
|---|---|---|---|
| **mp3quran.net API v3** — `https://www.mp3quran.net/api/v3/reciters?language=eng` (+ `language=ar`) | none | 242 reciters; per reciter one or more *moshaf* (recitation) with `server` base URL and `surah_list`; audio at `<server>NNN.mp3` on `cdn.mp3quran.net` (HTTPS, byte-range seeking, CORS `*`) | No published API terms found. See AUDIO_LICENSING.md — **written permission is a release gate.** |
| **Quran Foundation Content API v4** — `https://apis.quran.foundation/content/api/v4` | OAuth2 client-credentials (`scope=content`), headers `x-auth-token` + `x-client-id` | `/resources/chapter_reciters`, `/chapter_recitations/{id}` (per-Surah `audio_url`), `/chapters` (Surah metadata) | Display permitted incl. ad-supported apps; **audio must be streamed or cached ≤ 7 days, never pre-cached**; no redistribution of raw data as a separate product. |

The 114-Surah metadata bundled in `shared/…/SurahCatalog.kt` was captured from QF `/chapters`
(names, Arabic names, verse counts — total 6236) and is checked by unit tests.

## 2. Running without a backend (development)

Leave `CATALOG_BASE_URL` empty. The app calls mp3quran.net directly and maps the response with
the same shared mapper the backend uses.

## 3. Running the backend

```bash
cd quran-audio-app
./gradlew -p backend buildFatJar
PORT=8080 java -jar backend/build/libs/quran-audio-backend.jar
curl -s localhost:8080/health          # ok
curl -s localhost:8080/v1/catalog | head
```

Container example:

```dockerfile
FROM eclipse-temurin:21-jre
COPY backend/build/libs/quran-audio-backend.jar /app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

Endpoints: `GET /health`, `GET /v1/catalog` (gzip, `ETag`, `Cache-Control: public,
max-age=3600`, `304` on `If-None-Match`, `503` + `Retry-After` when no catalogue is available
yet, `429` when rate limited). The service refreshes upstream every `CATALOG_REFRESH_MINUTES`,
keeps the last good catalogue on upstream failure, and refuses to replace a catalogue with one
that shrank by more than half (protects against upstream error pages).

Then build the app with `-PCATALOG_BASE_URL=https://your-host/`.

## 4. Enabling Quran Foundation

1. Create an app in the QF Developer Console, choose **Backend/server app**, request Content
   API access; note the client id and one-time client secret.
2. Set `QF_CLIENT_ID` / `QF_CLIENT_SECRET` on the backend only. While testing, point
   `QF_API_URL` at the pre-production API (`https://apis-prelive.quran.foundation/content/api/v4`)
   and `QF_AUTH_URL` at the matching pre-production OAuth host listed in the QF quick-start.
3. **Verify the response shape:** the provider was written against the documented fields
   (`reciters[].id/name/translated_name/style`, `audio_files[].chapter_id/audio_url`) and parses
   leniently, but it could not be exercised with live credentials during development. Check
   the backend log line `Catalogue refreshed: N reciters from [mp3quran, qf]`.
4. QF audio hosts (`*.quranicaudio.com`, `*.quran.foundation`, `*.qurancdn.com`) are already in
   the app's allow-list. The app never caches audio to disk, satisfying the 7-day rule.

## 5. Security notes

- No secret is present in the APK; `CATALOG_BASE_URL` is public by nature.
- Do not add an "API key" to the app to protect the proxy — it would be extractable. Use the
  per-IP rate limit (add `XForwardedHeaders` when behind a proxy) and your platform's WAF.
- All URLs are HTTPS; cleartext is disabled by `network_security_config.xml`.
- Remote data is validated twice (backend and app): HTTPS only, allow-listed hosts, Surah
  numbers 1–114, sane names, de-duplicated ids.
