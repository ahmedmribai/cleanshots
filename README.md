# CleanShots — build choices (locked)

Package `com.ironforge.cleanshots`, Kotlin + Compose M3, minSdk 26, targetSdk/compileSdk 36.

## Why these options
- **Native Kotlin** (not Flutter/RN): zero-copy MediaStore, day-0 Billing 9.1, 4-6MB APK, no bridge tax.
- **No accompanist**: deprecated. Uses `RequestMultiplePermissions`.
- **Billing 9.1.0 + billing-ktx**: PBL 6 expired Aug 2025. Uses `ProductDetails`, `acknowledge`, `queryPurchasesAsync`, auto-reconnect.
- **dHash (not pHash) + BK-Tree threshold 5**: 3-5x faster, 20-80ms grouping for 2000. Single 256px `loadThumbnail` reused for hash + Laplacian blur (threshold 100 tunable).
- **Pricing fixed**: your $9.99 lifetime < $19.99 yearly kills LTV. Ships Weekly $4.99 / Yearly $29.99 / Lifetime $59.99 with trials. Keep your IDs + added `cleanshots_weekly_4_99`. Change prices in Play Console to match.
- **Free 20 deletes**: contextual paywall when a delete would exceed the limit. Premium is stored on-device (no server receipt check).
- **Trash needs Android 11+** (`MediaStore.createTrashRequest`); older versions get an explanatory message.
- **Weekly rescan** via WorkManager (`RescanWorker`, scheduled in `CleanShotsApp`).

## Open in Android Studio
Requires JDK 17+ (you have JDK 21 at JAVA_HOME) + Android SDK 36.
```
File > Open > C:\Users\SkyMil\Desktop\cleaner
Let Gradle sync (AGP 8.13, Kotlin 2.3, Room 2.8, Compose BOM 2024.12.01)
Run on device API 30+ for trash flow
```
Gradle wrapper (8.13) is included. CLI: `./gradlew testDebugUnitTest lintDebug assembleDebug`.
Create `local.properties` with `sdk.dir=...` if Android Studio has not.

## Create products in Play Console
- `cleanshots_weekly_4_99` SUBS $4.99/week 3-day trial
- `cleanshots_yearly_19_99` SUBS $29.99/year 7-day trial (rename price, keep ID)
- `cleanshots_lifetime_9_99` INAPP $59.99 (raise price, keep ID)

## Before submission
- Data safety form; set `PRIVACY_POLICY_URL` in `PaywallScreen.kt` (hidden while blank) and the Play listing
- Photo permission declaration video: show scan -> duplicates -> trash -> restore
- Title/desc match real function, no virus/boost claims
- `bundleRelease` signed AAB (add a signing config), R8 already on, logs stripped

## Tools
- `python tools/translations/generate.py` rebuilds `values-*/strings.xml` from `tools/translations/*.py`
  and fails on missing keys or mismatched placeholders. Add new English strings to `values/strings.xml`
  first, then to every language table.
- `python tools/benchmark/make_library.py <dir>` builds a labelled ~4.5k-photo test library (real photos
  from picsum.photos plus generated duplicates, blurred, dark, screenshots and videos). Push it to a
  device, scan with a debug build (it logs `scan_done` with timings), pull `databases/cleanshots.db`
  with `adb exec-out run-as` and run `python tools/benchmark/evaluate.py <db>` for accuracy.
- `python tools/store/make_assets.py <raw_screens_dir>` renders the Play icon, feature graphic and
  captioned screenshots into `store/`. Listing copy and Data safety answers are in `store/listing-en.md`.

### Benchmark (emulator, 2 cores, 4,533 photos)
| | Result |
|---|---|
| First scan | 147 s (~32 ms/photo); phones with more cores are faster |
| Rescan, nothing changed | 1.9 s |
| Blurry: clearly blurred shots caught | ~70 % |
| Blurry: false alarms on natural photos | ~1 % (mostly fog, sky and bokeh) |
| Duplicates: exact/re-encoded/resized sets found | 95 % |
| Duplicates: suggestions that are lookalikes, not copies | ~2 % (Smart Clean pre-selects for review) |
| Cropped or edited near-duplicates | not detected |
