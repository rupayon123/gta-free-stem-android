# Android Release Runbook

This runbook prepares a GTA FREE STEM Android release candidate. It does not authorize an upload, staged rollout, production rollout, or publication.

Policy notes were reviewed on August 18, 2026. Google Play requirements can change; confirm the live Play Console tasks and linked Google documentation before acting.

## October 27, 2026 candidate evidence snapshot

- Reviewed runtime source: `13c6085` (descendant of the 232-test `f8a6f94` source) (`1.1.0`, version code 3).
- Local JVM report: 232 tests, zero failures or errors. This does not prove a
  signed release or device behavior.
- Reviewed runtime logs for `13c6085`: 45/45 instrumentation cases and 3/3
  reduced-motion cases passed on each of API 26 and API 36 emulators. These
  development-APK results do not establish signed-release or physical-device acceptance.
- Exact-candidate debug/release lint, CI, signed AAB/APK, merged signed
  permissions, native/R8/16 KB inspection, and upload require their own receipts.
- Fresh Play Console review on October 27 still shows `1.0.1` (version code 2)
  available to internal testers. Version code 3 has not been newly built as a
  signed release or uploaded. No new account or payment was used.
- Older 186-JVM/36-on-each-API checklist entries describe a superseded source
  snapshot; their date/source was not recorded in those entries. They are kept
  as history below, not as evidence for `f8a6f94`.

- Frozen native feed: 1,878 records, including 45 explicitly virtual records;
  source `7ca94cff12c9667f93bd9d4febb774ee6e6f0e37`,
  SHA-256 `c61ed81737815ad1543df966f382c8e2672fe2cb28b7d8f6f68cfc3ef39fd391`.
  This is a dated release cut, not a promise that the live feed always has this count.
- Full descriptions are not fully translated: 18-language interface support is
  distinct from accepted full provider-description translations.

## Release invariants

- Intended application ID: `com.rupayonhaldar.gtafreestem`
- Initial release: `versionName` `1.0`, `versionCode` 1
- Historical internal-test update: `versionName` `1.0.1`, `versionCode` 2
- Current local parity candidate: `versionName` `1.1.0`, `versionCode` 3
- SDK baseline: compile/target 36, minimum 26
- Distribution artifact: Android App Bundle (`.aab`)
- Signing: a private upload key held by the developer, plus mandatory Play App Signing for a new Play app
- Data posture: local app data; no accounts, ads, analytics, attribution, or push
  service; one-shot foreground approximate location for opt-in Nearby; opt-in
  local notifications scheduled by WorkManager; public feed fetched over HTTPS
  from GitHub/jsDelivr; interactive MapLibre/OpenFreeMap maps separately request
  styles, tiles, fonts, and sprites, exposing IP/requested-map-area metadata;
  map resource caches are separate from the ephemeral raw Nearby fix
- Public pages:
  - Privacy: <https://gta-free-stem.pages.dev/privacy>
  - Support: <https://gta-free-stem.pages.dev/support>
  - Terms: <https://gta-free-stem.pages.dev/terms>

Changing the application ID after the first Play upload creates a different app. Stop and reconcile any mismatch before creating a release artifact.

## Gate 1: Account and policy prerequisites

1. Confirm whether the Play Console account is Personal or Organization and complete every identity/contact verification task shown in the console.
2. Budget for the current one-time USD $25 full-distribution registration fee. Google does not list an annual Play Console fee.
3. If the account is a new Personal account, complete the real-device verification task when shown. Google currently requires a non-rooted physical Android device running Android 10 or later for that flow.
4. If the Personal account was created after November 13, 2023, plan for the production-access gate: a closed test with at least 12 testers continuously opted in for at least 14 days, followed by Google's production-access application.
5. Complete Android developer verification and package registration in the Play Console. The September 30, 2026 enforcement starts with certified-device installs in Brazil, Indonesia, Singapore, and Thailand; do not wait for enforcement to complete the account task.
6. Decide the truthful target age groups before completing app-content forms. If any selected audience includes children, apply the Google Play Families requirements to the app, listing, privacy policy, dependencies, and external content.

Official references:

- [Create a Play Console account](https://support.google.com/googleplay/android-developer/answer/6112435)
- [Testing requirements for new Personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Device verification for new accounts](https://support.google.com/googleplay/android-developer/answer/14316361)
- [Android developer verification](https://support.google.com/android-developer-console/answer/16561738)
- [Target audience and content](https://support.google.com/googleplay/android-developer/answer/9867159)

## Gate 2: Freeze the release identity

From the project root, run:

```bash
EXPECTED_VERSION_CODE=3 EXPECTED_VERSION_NAME=1.1.0 \
  ./scripts/verify-release-config.sh
```

Resolve every failure deliberately. At minimum, inspect the final values in `app/build.gradle.kts` for:

- `applicationId = "com.rupayonhaldar.gtafreestem"`
- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 26`
- a unique, increasing `versionCode`
- the intended user-facing `versionName`

Also confirm that the namespace and Kotlin package structure compile together. They do not technically have to equal the application ID, but an accidental mismatch can break relative manifest class names or make maintenance error-prone.

Record the source commit identifier in the release notes once this project is in a Git repository. Build only from a clean, reviewed source state.

## Gate 3: Review privacy and network behavior

1. Review `docs/PRIVACY_AND_DATA_SAFETY_DRAFT.md` against the exact release source and merged manifest.
2. Inventory every dependency and permission; do not infer Data safety answers only from the visible UI. For this candidate, reconcile the source declarations for `INTERNET`, `ACCESS_COARSE_LOCATION`, and `POST_NOTIFICATIONS` and the normal permissions/components merged by WorkManager, including `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, and `FOREGROUND_SERVICE`.
3. Inventory MapLibre 13.7.1 and its OpenFreeMap resource requests explicitly.
   Confirm no account, ad, analytics, crash-reporting, attribution, paid-map, or
   push service is added. Inspect the signed merged manifest to verify removal
   of the unused SDK precise-location permission; source removal/debug evidence
   alone is not signed-artifact evidence.
4. Confirm Nearby is user-initiated and approximate, uses one foreground fix in
   memory, and strips latitude/longitude/distance before saving browse filters.
   Verify requests do not attach the raw fix. Independently review map requests:
   camera centering on public opportunities and map navigation still exposes
   viewed map areas and IP metadata. Do not describe this as no location-related
   transmission or no map cache.
5. Confirm alerts are opt-in, request notification permission only when needed, schedule unique network-constrained periodic work, compare the downloaded feed with saved filters locally, throttle notifications, and cancel work when disabled. WorkManager timing is inexact; do not promise an exact delivery time.
6. Confirm every app-managed endpoint uses HTTPS and that foreground and background feed refreshes have bounded failure behavior.
7. Confirm the merged release manifest still disables Android backup and that the backup/data-extraction rules exclude app data. If a future release enables backup, update the public privacy wording and Data safety analysis before shipping.
8. Open the Cloudflare privacy/support/terms URLs from a logged-out browser;
   confirm Android/map flows match the current combined policy and pages are
   public and mobile-readable. Verify any displayed support email is monitored;
   if none is displayed, use only a non-sensitive request for a private channel.
   Do not invent a contact address or infer a new internal-track submission gate.
9. Decide the target audience truthfully and complete any Families-policy review required for users under the applicable age of a child.

## Gate 4: Verify the debug-quality baseline

Use Java 17 and SDK 36, then run:

```bash
./gradlew --no-daemon --stacktrace clean testDebugUnitTest lintDebug assembleDebug
```

On representative API 26 and API 36 devices or emulators, manually verify:

- cold launch and relaunch
- opportunity loading, cached/bundled fallback, retry, empty, and offline states
- search, filters, New Finds, list/map switching, saved state, and navigation
- an explicit Nearby flow for granted, denied, disabled, timeout, clear, and relaunch states; confirm the raw Nearby fix does not persist after process death; separately inspect map-resource cache behavior
- alert opt-in/opt-out, Android 13+ notification-permission denial, first baseline, new-match notification, throttle, reboot/reschedule, and notification deep link
- cold- and warm-start deep links plus Opportunities/High School app shortcuts
- external links and back navigation
- branded launch motion and reduced/disabled-animation behavior
- light/dark themes, all 18 languages including RTL, font scaling, screen reader labels, focus order, and touch targets
- rotation, narrow phone layouts, tablets, and process restoration
- uninstall/reinstall and the documented data-deletion/backup behavior

Run instrumented tests on an emulator or device if the project provides them:

```bash
./gradlew --no-daemon connectedDebugAndroidTest
```

A passing debug build does not prove release signing, optimized-release behavior, device coverage, Play policy compliance, or publication.

## Gate 5: Create the upload key locally

Google Play uses two identities: the app-signing key managed by Play App Signing and the developer-held upload key used to authenticate uploaded bundles. Keep them distinct when the Play setup offers that choice.

The repository includes a dry-run-first example helper. Running it without `--execute` creates nothing:

```bash
UPLOAD_KEYSTORE_PATH=/absolute/path/outside/the/repository/gta-free-stem-upload.jks \
UPLOAD_KEY_ALIAS=gta-free-stem-upload \
  ./scripts/generate-upload-keystore.example.sh
```

After reviewing its output, add `--execute` to deliberately create a key. The script invokes `keytool`, which asks for passwords interactively so they do not appear in shell history. Never run the execute mode in CI or during an untrusted screen-sharing/logging session.

Immediately after creation:

1. Store both passwords in a password manager.
2. Make an encrypted offline backup of the keystore and recovery instructions.
3. Keep the keystore outside this repository.
4. Export a shareable public upload certificate when Play requests it:

   ```bash
   keytool -export -rfc \
     -keystore /absolute/path/gta-free-stem-upload.jks \
     -alias gta-free-stem-upload \
     -file gta-free-stem-upload-certificate.pem
   ```

The `.pem` certificate is public; the keystore and passwords are not.

Official reference: [Sign an Android app and use Play App Signing](https://developer.android.com/studio/publish/app-signing).

## Gate 6: Configure local signing

Only after the upload key exists, use one of the two supported local signing paths.

### Preferred on macOS: Keychain-backed build

The source-controlled wrapper uses these exact Keychain identifiers:

- service: `com.rupayonhaldar.gtafreestem.upload-keystore`
- store-password account: `store-password`
- key-password account: `key-password`

Create the two items yourself in a trusted Terminal. Keeping `-w` last makes
`security` prompt instead of placing a password in shell history:

```bash
/usr/bin/security add-generic-password \
  -s com.rupayonhaldar.gtafreestem.upload-keystore \
  -a store-password \
  -w

/usr/bin/security add-generic-password \
  -s com.rupayonhaldar.gtafreestem.upload-keystore \
  -a key-password \
  -w
```

Do not use `-U` unless you are intentionally rotating an existing item. The
Keychain stores only the passwords; the upload keystore still needs an encrypted
offline backup. By default the wrapper expects the keystore at:

```text
~/Library/Application Support/GTAFreeSTEM/signing/gta-free-stem-upload.jks
```

Set `JAVA_HOME` to Java 17, then build without exposing the passwords:

```bash
JAVA_HOME=/absolute/path/to/jdk-17 \
  ./scripts/build-signed-release-from-keychain.sh
```

The wrapper reads both passwords with `/usr/bin/security`, exports the complete
`GTA_UPLOAD_*` environment contract only for the no-daemon verification/build
process, and unsets it on exit. Override only the non-secret defaults when needed:

```bash
GTA_UPLOAD_KEYSTORE_PATH=/absolute/path/outside/the/repository/gta-free-stem-upload.jks \
GTA_UPLOAD_KEY_ALIAS=gta-free-stem-upload \
JAVA_HOME=/absolute/path/to/jdk-17 \
  ./scripts/build-signed-release-from-keychain.sh
```

### Compatible alternative: local properties file

The original ignored properties-file path remains supported:

```bash
cp keystore.properties.example keystore.properties
chmod 600 keystore.properties
```

Replace every placeholder locally. Use an absolute `storeFile` path outside the repository. Do not paste secrets into commands, issue comments, CI logs, screenshots, chat, or source control.

Confirm the ignored state before continuing:

```bash
git check-ignore keystore.properties
```

The command must report `keystore.properties` as ignored. Keep the keystore outside the Git worktree instead of relying on an ignore rule for it. If this directory is not yet in Git, manually verify `.gitignore` now and repeat the command after repository initialization.

Validate the local secret-gated configuration without printing secret values:

```bash
REQUIRE_SIGNING=1 ./scripts/verify-release-config.sh
```

Environment values take precedence when all four are present. A partial
`GTA_UPLOAD_*` environment is rejected rather than silently falling back. Do not
continue if the build script does not apply the selected values only to release
signing.

## Gate 7: Build and inspect the release bundle

With the properties-file path configured:

```bash
./gradlew --no-daemon --stacktrace clean testDebugUnitTest lintRelease bundleRelease
```

For the macOS Keychain path, the wrapper runs the same task sequence by default.

Expected output location:

```text
app/build/outputs/bundle/release/app-release.aab
```

Then:

1. Confirm the build completed with exit code 0 and no lint suppressions were added merely to pass the gate.
2. Record the AAB SHA-256 checksum:

   ```bash
   shasum -a 256 app/build/outputs/bundle/release/app-release.aab
   ```

3. Inspect the bundle with Android Studio's APK Analyzer or `bundletool` from an official source. Record the merged permissions and components, bundled native libraries, supported devices/ABIs, and size.
4. Verify 16 KB page-size compatibility for every packaged native library; do not infer compatibility from `targetSdk` alone.
5. Use Play Console's internal test track and generated APK inspection/pre-launch report before any broader test.
6. Install a Play-generated build on at least one supported physical device; a locally installed debug APK is not equivalent.
7. Repeat the critical behavior, accessibility, offline, location, notification, deep-link, upgrade, and data-persistence tests against the release build.

An AAB's existence or signature does not mean Google accepted it or that it is safe to publish.

## Gate 8: Play Console handoff

Complete `docs/PLAY_CONSOLE_CHECKLIST.md`. Confirm the existing Play App Signing
enrollment and upload only to the explicitly authorized test track, then
preserve:

- source commit identifier
- version name/code and application ID
- AAB SHA-256
- upload certificate SHA-256 fingerprint
- test date, devices/API levels, and results
- Play pre-launch report and policy-form review date
- privacy/support/terms URL check results
- tester opt-in evidence when the Personal-account gate applies
- reviewer and explicit rollout approval

Stop before production rollout until an authorized person reviews the complete evidence and approves the exact artifact.

## Rollback and key incidents

- Play does not support replacing a production artifact with the same `versionCode`. Prepare a fixed build with a higher code and use a staged rollout when appropriate.
- Halt a staged rollout if crash, policy, privacy, content, or data-loss issues appear.
- If the upload key is lost or compromised, use Play Console's upload-key reset process. Do not create a different Play app to work around the issue.
- If the app-signing identity, application ID, or developer account is uncertain, stop and resolve ownership before uploading anything.
