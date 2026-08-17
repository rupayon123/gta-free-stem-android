# Android v1.0.1 verification

Evidence recorded August 16, 2026 with Java 17, Android SDK/Build Tools 36, and
an API 36 Play Store emulator. The source identifies itself as:

```text
package: com.rupayonhaldar.gtafreestem
versionName: 1.0.1
versionCode: 2
compileSdk: 36
targetSdk: 36
minSdk: 26
permission: android.permission.INTERNET
```

## Recorded automated results

- JVM suite (`testDebugUnitTest`): 105 tests passed, with 0 failures and 0 errors.
- Full instrumentation suite (`connectedDebugAndroidTest`): 11 of 11 tests
  passed on an API 36 emulator using three-button system navigation.
- Focused system-navigation regression: 1 of 1 targeted test passed on the same
  API 36 emulator using gesture navigation.
- Debug lint (`lintDebug`): zero issues.
- Release lint (`lintRelease`): zero issues.
- Public-root GitHub Actions run
  [`31980604818`](https://github.com/rupayon123/gta-free-stem-android/actions/runs/31980604818)
  completed successfully, including the static configuration verifier, JVM
  tests, debug/release lint, debug assembly, and unsigned release bundle.
- Final workflow/evidence commit run
  [`31981089197`](https://github.com/rupayon123/gta-free-stem-android/actions/runs/31981089197)
  also completed successfully using the current pinned action releases.

These are the recorded results for the current v1.0.1 source. They were not
rerun merely to rewrite this document.

## Signed candidate identity

The signed candidate was built from the sanitized zero-parent public source
commit `292c90256cda68393b603411f3f92297e0ad85ce`.

```text
AAB size: 5.2 MiB
AAB SHA-256: 63cd724942ceed05e94321eaabeb485bfbec880c1ec4b423dfb87b9b9b4e6fbb
Public upload-certificate SHA-256: 0D:8E:B2:CD:06:A1:89:B5:FF:7C:15:2A:B7:AE:72:C5:60:89:26:4C:FD:D5:64:95:E2:71:5B:BC:A7:DB:F7:B5
```

`jarsigner -verify` returned success and `jar verified`. The certificate read
from the AAB and the certificate read from the protected local upload keystore
had the same owner, issuer, validity window, and SHA-256 fingerprint. The
verification also emitted self-signed-certificate, missing-timestamp, and
JarInputStream warnings; Google Play acceptance remains a separate required
check. No private key, password, keystore, tester link, or Play identifier is
stored in this repository.

## Google Play internal-test result

Google Play accepted the signed AAB as version `1.0.1` (`versionCode` 2) for
the existing internal-testing track and reported it **Available to internal
testers**. The tester opt-in page exposes a **Download test app** link and notes
that already-installed test builds receive updates as they become available.
Play reported API 26+, target SDK 36, a 2.9 MB new-install download, and a
1.34 MB update download.

Play emitted one non-blocking warning because the bundle contains native code
from an AndroidX dependency without a native debug-symbol archive. No matching
symbol archive was generated locally. This warning remains recorded for later
crash-symbolication review; it did not block internal-test publication.

## Behavior covered by the current source

- A five-destination adaptive shell provides Home, Opportunities, High School,
  Support, and Account on compact screens, with an adaptive rail on wider screens.
- Safe drawing insets keep app navigation above Android gesture and three-button
  system-navigation areas.
- Browse supports keyword, category, age, region/city, language, pathway,
  equity, and sort controls. Nearby/distance and New Finds remain explicitly
  unavailable in this Android version.
- Saved opportunities persist full offline records and separate Current and
  Archive entries without claiming cloud sync.
- Account provides an optional on-device display name, System/Light/Dark theme,
  System plus 18 explicit languages, and an alert-intent preference that does
  not schedule notifications.
- Feed loading rejects malformed, oversized, stale, incomplete, unhealthy, or
  non-free network data and retains bounded bundled/cache fallback behavior.

## Evidence boundaries

This record does **not** establish that v1.0.1 has:

- been installed from a Play-generated artifact
- passed on a physical Galaxy Z Flip 6 or any other physical Android device
- reached closed testing, production, or public availability outside the
  configured internal-test group

The PNG files in this directory document the earlier version-1 emulator
baseline and its older three-tab interface. They are not current v1.0.1 visual
evidence and must not be used to claim current navigation, physical-device, or
release-artifact validation.

## Historical version-1 Play status

An earlier `1.0` (`versionCode` 1) build was accepted into restricted internal
testing on August 16, 2026. That historical acceptance is not a v1.0.1 upload,
physical-device signoff, production release, or public-availability claim.
Tester identities, invitation URLs, numeric Play identifiers, account details,
private signing material, and recovery records are excluded from this public
evidence.

## Remaining release and parity gates

- install a Play-generated build on representative physical Android hardware
- complete physical-device, TalkBack, large-text, foldable, offline, upgrade,
  link, save, and system-navigation checks against that artifact
- make and verify an independent encrypted backup of the upload key
- review the Play pre-launch report and current production-access requirements
- finish store, privacy, Data safety, content-rating, target-audience, and policy
  declarations for the exact candidate
- decide and implement remaining iOS-parity work: map/nearby browsing, local
  match notifications, deep links, and remaining untranslated copy

This evidence does not prove a source push, Play upload, broader test rollout,
production rollout, or public availability.
