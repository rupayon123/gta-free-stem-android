# GTA FREE STEM for Android

GTA FREE STEM is a native Android app for browsing free STEM opportunities. This project uses Kotlin, Jetpack Compose, the Gradle wrapper, and Java 17.

> Release status: version 1.0.1 is available only to configured Google Play
> internal testers. This is not a production release or public-availability
> claim. Complete every remaining gate in the release runbook before a broader
> rollout.

## Current verified baseline

As of August 16, 2026, version `1.0.1` (`versionCode` 2) has been exercised
locally with Java 17, Android SDK 36, and an API 36 Play Store emulator. The
current recorded baseline includes:

- 105 JVM tests passing
- the full 11-test instrumentation suite passing in three-button system-navigation mode
- a focused system-navigation/inset regression test passing in gesture mode
- debug and release lint completing with zero issues
- strict validation of the 125-record feed and a bounded bundled/cache fallback
  when a healthy network snapshot is unavailable

The exact public source root was also built as a signed 5.2 MiB AAB. Its JAR
signature verified, and its public signing-certificate SHA-256 matched the
locally protected upload key; the public-safe checksum is recorded in
[`verification/VERIFICATION.md`](verification/VERIFICATION.md). This does not
prove that version 1.0.1 has been installed from a Play-generated artifact or
passed on physical hardware. Google Play accepted version 1.0.1 into restricted
internal testing and reports it available to internal testers; that is not
production or public availability. An independent encrypted backup of the
upload key also remains required. Tester invitations, numeric Play identifiers,
account-contact details, identity evidence, and private signing material are
intentionally not stored here.

The current app uses a five-destination adaptive shell: Home, Opportunities,
High School, Support, and Account. It includes system-bar-safe insets for gesture
and three-button navigation, expanded keyword/pathway/equity filters, local
full-record saves with Current and Archive sections, opportunity details and
external actions, strict live/offline feed handling, an optional on-device
display name, local language/theme/alert-intent preferences, and a shared
18-language catalog with RTL support. It deliberately has no online account or
sign-in flow.

This is not full iOS feature parity. Map/nearby browsing, local match
notifications, deep links, and localization of remaining older
browse/detail/accessibility copy are still outstanding.

## Android baseline

| Setting | Intended value |
| --- | --- |
| Application ID | `com.rupayonhaldar.gtafreestem` |
| Compile SDK | 36 |
| Target SDK | 36 |
| Minimum SDK | 26 |
| Current version | `1.0.1` (`versionCode` 2) |
| Java toolchain | 17 |
| Play artifact | Signed Android App Bundle (`.aab`) |

The application ID becomes the permanent Play identity after the first artifact is uploaded. Confirm it before creating or uploading to the Play app entry.

## Prerequisites

- Android Studio with Android SDK Platform 36 and the matching build tools installed.
- A Java 17 JDK. Android Studio's bundled JDK is suitable when it reports Java 17.
- Git for normal source-control use.
- A physical or virtual device running Android 8.0 (API 26) or later for manual testing.

No global Gradle installation is needed; use the checked-in wrapper.

## Local setup

1. Open this directory in Android Studio.
2. Let Android Studio generate `local.properties`, or set its `sdk.dir` to the local Android SDK. This machine-specific file is ignored by Git.
3. Confirm the toolchain:

   ```bash
   ./gradlew --version
   ```

4. Run the same core checks used by CI:

   ```bash
   ./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
   ```

5. Launch the `app` configuration from Android Studio, or install the debug APK from `app/build/outputs/apk/debug/` on a test device.

Debug builds use the Android debug key and are not Play upload artifacts.

## Release preparation

Start with these documents:

- [Android release runbook](docs/ANDROID_RELEASE_RUNBOOK.md)
- [Play Console checklist](docs/PLAY_CONSOLE_CHECKLIST.md)
- [Privacy and Data safety draft](docs/PRIVACY_AND_DATA_SAFETY_DRAFT.md)
- [Public-safe Google Play status](docs/PLAY_ACCOUNT_STATUS.md)

Run the static release configuration check before producing a candidate:

```bash
./scripts/verify-release-config.sh
```

The check does not prove runtime quality, policy compliance, signing identity, Play review acceptance, or publication. To validate a locally configured upload key without revealing it, use the secret-gated mode described in the runbook.

## Data and cost posture

The app has no online user accounts, ads, analytics, or active push/local
notifications. The optional display name, searches, filters, saved details,
language/theme settings, and inactive alerts preference remain on the device.
The opportunities feed is retrieved over HTTPS from GitHub/jsDelivr, and
external opportunity links may open in the user's browser. Recheck the final
release artifact and dependencies before submitting the Data safety form.

Google's current registration cost for full distribution is a one-time USD $25 fee, with no annual Play Console fee. Hosting and third-party services can change their terms independently, so review their current limits before each release.

## Secret handling

- Never commit `keystore.properties`, `.jks`, or `.keystore` files.
- Keep the upload keystore outside the repository and store its passwords only
  in an approved local secret store. Make an independent encrypted backup before
  treating key recovery as complete.
- CI intentionally receives no signing secrets; its release bundle is a build
  check, not a Play-upload artifact. Signed AAB production stays local unless a
  separately reviewed, secret-gated release workflow is approved.
- The example files contain placeholders only. No private upload key or password is included in this repository.

## Open source and community

This project is available under the [MIT License](LICENSE). Contributions are
welcome, especially human-reviewed translations, accessibility improvements,
foldable-device testing, documentation, and focused bug fixes.

The MIT license covers project-owned source and documentation. Opportunity
descriptions, provider materials, and third-party names or marks remain subject
to their original sources and owners; the license does not grant trademark
endorsement rights.

- Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.
- Use [the Android localization guide](docs/LOCALIZATION.md) for language work.
- Follow the [Code of Conduct](CODE_OF_CONDUCT.md).
- Report vulnerabilities privately as described in [SECURITY.md](SECURITY.md).
