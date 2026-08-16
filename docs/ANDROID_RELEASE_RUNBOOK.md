# Android Release Runbook

This runbook prepares a GTA FREE STEM Android release candidate. It does not authorize an upload, staged rollout, production rollout, or publication.

Policy notes were reviewed on August 16, 2026. Google Play requirements can change; confirm the live Play Console tasks and linked Google documentation before acting.

## Release invariants

- Intended application ID: `com.rupayonhaldar.gtafreestem`
- Initial release: `versionName` `1.0`, `versionCode` 1
- Current internal-test update: `versionName` `1.0.1`, `versionCode` 2
- SDK baseline: compile/target 36, minimum 26
- Distribution artifact: Android App Bundle (`.aab`)
- Signing: a private upload key held by the developer, plus mandatory Play App Signing for a new Play app
- Data posture: local app data; no accounts, ads, analytics, or push notifications; public feed fetched over HTTPS from GitHub/jsDelivr
- Public pages:
  - Privacy: <https://gta-free-stem.vercel.app/privacy>
  - Support: <https://gta-free-stem.vercel.app/support>
  - Terms: <https://gta-free-stem.vercel.app/terms>

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
EXPECTED_VERSION_CODE=2 EXPECTED_VERSION_NAME=1.0.1 \
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
2. Inventory every dependency and permission; do not infer Data safety answers only from the visible UI.
3. Confirm there are no account, ad, analytics, crash-reporting, attribution, or push SDKs.
4. Confirm every app-managed endpoint uses HTTPS and that the production feed has an offline/error state.
5. Confirm the merged release manifest still disables Android backup and that the backup/data-extraction rules exclude app data. If a future release enables backup, update the public privacy wording and Data safety analysis before shipping.
6. Open the privacy, support, and terms URLs from a logged-out browser and confirm they are public, current, mobile-readable, and use a monitored support contact.
7. Decide the target audience truthfully and complete any Families-policy review required for users under the applicable age of a child.

## Gate 4: Verify the debug-quality baseline

Use Java 17 and SDK 36, then run:

```bash
./gradlew --no-daemon --stacktrace clean testDebugUnitTest lintDebug assembleDebug
```

On representative API 26 and API 36 devices or emulators, manually verify:

- cold launch and relaunch
- opportunity loading, cached/bundled fallback, retry, empty, and offline states
- search, filters, saved state, and navigation
- external links and back navigation
- light/dark themes, font scaling, screen reader labels, focus order, and touch targets
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

3. Inspect the bundle with Android Studio's APK Analyzer or `bundletool` from an official source.
4. Use Play Console's internal test track and generated APK inspection/pre-launch report before any broader test.
5. Install a Play-generated build on at least one supported physical device; a locally installed debug APK is not equivalent.
6. Repeat the critical behavior, accessibility, offline, upgrade, and data-persistence tests against the release build.

An AAB's existence or signature does not mean Google accepted it or that it is safe to publish.

## Gate 8: Play Console handoff

Complete `docs/PLAY_CONSOLE_CHECKLIST.md`. Enroll the new app in Play App Signing, upload only to the intended test track, and preserve:

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
