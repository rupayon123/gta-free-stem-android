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

These are the recorded results for the current v1.0.1 source. They were not
rerun merely to rewrite this document.

## Behavior covered by the current source

- A five-destination adaptive shell provides Home, Opportunities, High School,
  Support, and Account on compact screens, with an adaptive rail on wider screens.
- Safe drawing insets keep app navigation above Android gesture and three-button
  system-navigation areas.
- Browse supports keyword, category, age, region/city, language, distance,
  pathway, equity, accessibility, and sort controls.
- Saved opportunities persist full offline records and separate Current and
  Archive entries without claiming cloud sync.
- Account provides an optional on-device display name, System/Light/Dark theme,
  System plus 18 explicit languages, and an alert-intent preference that does
  not schedule notifications.
- Feed loading rejects malformed, oversized, stale, incomplete, unhealthy, or
  non-free network data and retains bounded bundled/cache fallback behavior.

## Evidence boundaries

This record does **not** establish that v1.0.1 has:

- been signed as a release candidate
- been uploaded to or accepted by Google Play
- produced a reviewed artifact checksum or signing-certificate match
- been installed from a Play-generated artifact
- passed on a physical Galaxy Z Flip 6 or any other physical Android device
- reached closed testing, production, or public availability

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

- build, sign, inspect, and independently identify the exact v1.0.1 candidate
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
