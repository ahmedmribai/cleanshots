# CleanShots pre-release audit

Source-based review of the current main branch. This is not a claim that the app has passed device, Play Console, or security testing.

## Confirmed findings

### Product IDs and pricing
- BillingManager.kt defines cleanshots_weekly_4_99, cleanshots_yearly_19_99, and cleanshots_lifetime_9_99.
- The source says the suffixes are historical. The paywall displays formatted prices returned by Google Play ProductDetails, not prices inferred from IDs.
- The README's instruction to "rename price" while retaining IDs is misleading. Product IDs are identifiers; configure current prices and offers in Play Console and verify the returned products on a license-tester device. Do not create duplicate products just to make an ID suffix match a price.

Release gate: verify product IDs, product types, base plans/offers, regions, trial eligibility, and displayed prices in Play Console. Test weekly/yearly renewals and the lifetime one-time product.

### Device-cached premium entitlement
- BillingManager.refreshPremium() queries Google Play for subscription and one-time purchases, then writes the result to PrefsManager.
- PrefsManager stores a premium boolean locally in DataStore.
- If Play cannot be reached or returns a non-OK response, refreshPremium() returns null and leaves the cached value unchanged. This avoids locking out a paying user during transient outages.
- There is no server-side purchase-token verification in this repository. The local flag is a cache/convenience state, not a cryptographically protected entitlement record. A determined user with a modified/rooted device may tamper with local state.

Decision before monetization: either accept this as a small offline-first app's risk and document the trust boundary, or add a backend purchase-token verification service before treating premium status as high-assurance. Do not describe the current implementation as server-verified. If adding a backend, keep Play service credentials off-device and verify package name, product ID, purchase state, and subscription expiry/revocation server-side.

### Purchase lifecycle
The code acknowledges eligible purchases and re-queries purchases on startup/restore. The acknowledgement callback result is currently ignored, and the purchase-update handler sets the local premium flag when it sees a purchased product.

Release gate: test successful purchase, pending purchase, cancellation, billing unavailable, acknowledgement failure/retry, subscription expiry/cancellation, refund/revocation, reinstall, restore, and offline startup. Confirm pending purchases never unlock Pro and entitlement eventually follows Play's authoritative state. Add automated tests around entitlement-state decisions where practical.

### Media permission and privacy wording
The manifest requests image/video media access, Android 14 selected-visual-media access, and legacy read access only through API 32. This is consistent with a library-scanning product, but the store listing phrase "works with only the photos you choose to share" can imply a picker-only workflow. Actual scan scope depends on the access the user grants.

Release gate: verify full-library and selected-media flows on Android 13, 14, and 15/16; verify denied, limited, and later-revoked access. Align listing and privacy policy with actual access, on-device processing, and exceptions. Check every dependency, network call, and crash reporter before claiming no data leaves the device.

### Privacy policy URL
The paywall contains a hard-coded URL: https://ahmedmribai.github.io/cleanshots/. The README's instruction to set a PRIVACY_POLICY_URL constant in PaywallScreen.kt does not match the current source.

Release gate: verify that the URL is publicly reachable over HTTPS, names the app/developer, describes media access and local processing, covers Google Play billing and notifications, and matches the Data safety form and actual SDK/network behavior. Test the in-app link on a release build.

### Release signing
The release build only attaches the release signing config when keystore.properties exists. Without it, a release artifact may be unsigned and cannot be uploaded as a Play release.

Release gate: create and securely back up the upload key, keep keystore files/passwords out of Git, build a signed AAB, verify its application ID/version/signature, and enroll in Play App Signing. Test the exact AAB through an internal testing track.

### Benchmark claims
The README reports results from a 4,533-photo emulator library and provides scripts to generate/evaluate a dataset. These are useful engineering signals, but not a substitute for representative physical-device testing. The README itself says cropped/edited near-duplicates are not detected.

Release gate: rerun from a clean checkout; preserve dataset-generation parameters and evaluation output; test low-memory/low-storage devices, large libraries, videos, HEIC/RAW where supported, and interruption/resume. Publish measured limits and known misses rather than implying perfect detection.

## Recommended order
1. Correct stale pricing and privacy-policy setup instructions in the README.
2. Verify the Play Console catalogue and test purchase lifecycle with license testers.
3. Decide whether local cached entitlements are an accepted risk or whether server verification is required.
4. Validate permission behavior and listing/privacy consistency across Android versions.
5. Produce and verify a signed AAB on an internal track.
6. Re-run benchmarks and soak tests on at least one lower-end physical device before public rollout.

## Scope and limitations
This review used the README, Gradle configuration, Android manifest, paywall screen, billing manager, preferences manager, and store listing. It did not execute Gradle, install the app, inspect Play Console, test a purchase, or inspect every source file/dependency. Findings should be treated as review items until those checks are completed.
