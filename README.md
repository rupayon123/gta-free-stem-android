# GTA FREE STEM for Android

GTA FREE STEM is a native Android app for browsing free STEM opportunities. This project uses Kotlin, Jetpack Compose, the Gradle wrapper, and Java 17.

> Release status: version 1.1.0 (`versionCode` 3) is the current local parity
> candidate. It has not been uploaded to Google Play, installed from a
> Play-generated artifact, approved for production, or shown to be publicly
> available. Version 1.0.1 remains available only to configured internal
> testers. Complete every remaining gate in the release runbook before a
> broader rollout.

## Ownership And Engineering Evidence

Personal project by **Rupayon Haldar**, separate from SARIT employment. My work covers the Kotlin/Compose app, local persistence, feed integration, search and filters, and Android-specific navigation and alert behavior.

- [App and platform integration](app/src/main/java/com/rupayonhaldar/gtafreestem/), [unit tests](app/src/test/), and [device tests](app/src/androidTest/) expose the implementation and its checks.
- [CI runs](https://github.com/rupayon123/gta-free-stem-android/actions) are the source for committed-build status. A locally passing build is not evidence that GitHub CI or Play distribution passed.
- Suggested demo: browse → search/filter → save a listing → reopen saved items offline → test alert permission denial. Validate the exact candidate on a device before claiming that journey works.

## Historical Candidate Scope

The scope below was recorded in August 2026. It is not current end-to-end signoff: a later build repair does not automatically verify every screen, map, animation, or Play-distribution claim.

As of August 18, 2026, the version 1.1.0 source brings the Android experience
up to the current iOS product scope while using Android-native interaction and
accessibility patterns. It includes:

- the storybook-inspired visual system, branded launch sequence, adaptive Home
  composition, compact opportunity cards, and responsive phone/tablet shell
- list and zero-cost offline schematic-map browsing, opportunity map previews,
  New Finds, distance sorting, and an explicit Nearby action that requests one
  foreground approximate-location fix and keeps that fix only in memory
- opt-in, on-device new-match alerts backed by WorkManager; the app asks for
  notification permission when required, checks the public feed approximately
  every three hours when connected, compares matches locally, and uses no push
  notification service
- app-owned deep links and dynamic Opportunities/High School shortcuts
- local Current/Archive saves, full opportunity details and external actions,
  a local Profile, theme choices, and a shared 18-language catalog with RTL
  support
- strict validation of the current 104-record feed, clean word-boundary handling
  for provider excerpts, and bounded bundled/cache fallback when a healthy
  network snapshot is unavailable

The app deliberately has no online account or sign-in flow, paid map provider,
ads, analytics, attribution, or push service.

## Current release-evidence status

> **The August 19 v1.1.0 evidence is a superseded historical snapshot.** The
> app source has materially changed since that snapshot, so its signed
> artifacts, test/lint results, runtime checks, and screenshots do not verify
> the current working tree and must not be uploaded or presented as current
> release evidence.

For history, that earlier modified working-tree snapshot recorded 151 passing
JVM tests, zero-error debug and release lint, 19 of 19 API 36
gesture-navigation instrumentation tests, adaptive/RTL tablet checks, a
three-button inset check, and signed-release cold/warm deep-link smoke tests.
Its signed AAB/APK, upload certificate, R8 mapping, merged manifest, and 16 KB
native alignment were also inspected. The historical hashes and limitations
are preserved in
[`verification/V1.1.0-LOCAL-VERIFICATION.md`](verification/V1.1.0-LOCAL-VERIFICATION.md).

A current candidate requires a reviewed source freeze, clean rebuild, fresh
full test/lint and device verification, newly signed artifacts, screenshot
recapture, and regenerated provenance. No current upload artifact of record or
upload/publication authorization exists.

## Historical verified baseline

The following evidence belongs specifically to version `1.0.1` (`versionCode`
2) on August 16, 2026. It is preserved for release history and must not be
treated as final evidence for the version 1.1.0 candidate:

- 105 JVM tests passing
- the full 11-test instrumentation suite passing in three-button system-navigation mode
- a focused system-navigation/inset regression test passing in gesture mode
- debug and release lint completing with zero issues
- strict validation of the feed snapshot used by that historical candidate and
  a bounded bundled/cache fallback

That exact public source root was also built as a signed 5.2 MiB AAB. Its JAR
signature verified, and its public signing-certificate SHA-256 matched the
locally protected upload key; the public-safe checksum is recorded in
[`verification/VERIFICATION.md`](verification/VERIFICATION.md). This does not
prove that version 1.0.1 has been installed from a Play-generated artifact or
passed on physical hardware. Google Play accepted version 1.0.1 into restricted
internal testing and reports it available to internal testers; that is not
production or public availability. An independent encrypted backup of the
upload key also remains required. Tester invitations, numeric Play identifiers,
account-contact details, identity evidence, and private signing material are
intentionally not stored here. Current v1.1.0 evidence remains pending under
the release-evidence requirements above; physical-device,
Play-generated-install, live policy, and store-review evidence is still
required.

## Android baseline

| Setting | Intended value |
| --- | --- |
| Application ID | `com.rupayonhaldar.gtafreestem` |
| Compile SDK | 36 |
| Target SDK | 36 |
| Minimum SDK | 26 |
| Current candidate | `1.1.0` (`versionCode` 3) |
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

## Reliable push flow for every change

Before pushing, confirm you are inside the GTA Android repo root:

```bash
git rev-parse --show-toplevel
```

If that command fails with "not a git repository", you are in the wrong directory.

Use this command when you finish an edit so your branch stays aligned before pushing:

```bash
./scripts/push-safe.sh "Describe this change"
```

`push-safe.sh` now:

- pulls and rebases against `origin/main` before committing/pushing,
- stages and commits all local edits automatically,
- retries the push after transient failures, and
- verifies the remote head matches your local `HEAD`.

If a push still fails, it is usually one of:

- branch mismatch (not on `main`), or
- local auth/fetch issue (network or token) preventing sync from `origin`.
In both cases, run the same command from the GTA Free STEM Android root.

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

The app has no online user accounts, ads, analytics, attribution, or push
notification service. The optional display name, searches, filters, saved
details, language/theme settings, alert state/history, and approximate Nearby
fix remain on the device; the Nearby fix is in-memory only. When the user opts
in, Android WorkManager can refresh the public feed over HTTPS and post local
new-match notifications. Opportunity and source links may open in the user's
browser. Recheck the final release artifact, merged manifest, dependencies, and
runtime traffic before submitting the Data safety form.

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
