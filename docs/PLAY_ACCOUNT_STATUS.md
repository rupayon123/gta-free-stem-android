# Google Play release status

Last public-safe status review: August 16, 2026. This file records only facts
that are appropriate for a public source repository.

## Current v1.0.1 source and candidate evidence

- The permanent package name is `com.rupayonhaldar.gtafreestem`.
- The current source is version `1.0.1` (`versionCode` 2).
- The live Console reports that all apps are successfully registered for the
  current Android developer-verification requirements.
- 105 JVM tests passed.
- The full 11-test instrumentation suite passed on an API 36 emulator in
  three-button system-navigation mode.
- A focused system-navigation/inset regression passed in gesture mode.
- Debug and release lint completed with zero issues.
- The exact sanitized public source root produced a signed 5.2 MiB AAB.
- `jarsigner` verified the AAB, its public certificate fingerprint matched the
  protected upload key, and the public-safe AAB checksum is recorded in
  `verification/VERIFICATION.md`.
- Current behavior includes the five-tab adaptive shell, expanded filters,
  local full-record Current/Archive saves, local account/language/theme settings,
  and strict live/offline feed handling.

Google Play accepted version 1.0.1 into restricted internal testing and reports
it available to internal testers. The tester opt-in page provides a Download
test app link. This does not establish a Play-generated installation, physical-
device validation, closed testing, production, or public availability.

Play also reported one non-blocking warning for native code without a native
debug-symbol archive. The warning did not block internal testing and remains a
follow-up before broader distribution.

## Historical version-1 Play evidence

- Google Play accepted version `1.0` (`versionCode` 1) into restricted internal
  testing on August 16, 2026.
- Play App Signing was enabled for that earlier build.
- Play reported one non-blocking native debug-symbol warning that still requires
  review before broader distribution.

That historical internal acceptance is not evidence for v1.0.1, a production
release, public availability, or physical-device signoff.

## Not stored in this repository

The following are intentionally kept out of source control:

- developer-account numeric identifiers and contact details
- identity-verification documents or screenshots
- tester names, email addresses, lists, invitations, and opt-in links
- Play Console session exports or account screenshots
- private upload keys, keystores, passwords, recovery instructions, and secret
  manager records

## Remaining before any public release

1. Install the Play-generated build on representative physical Android hardware
   and record launch, offline, browse, save, link, large-text, TalkBack, foldable,
   and system-navigation results without publishing tester identity.
2. Make and verify an independent encrypted backup of the upload key outside the
   repository.
3. Investigate the native debug-symbol warning and review the Play pre-launch
   report.
4. Finish the store listing, privacy, Data safety, content-rating, target-audience,
   and policy declarations for the exact candidate.
5. Complete whatever testing and production-access gates the live Play Console
   requires for the developer account at submission time.
6. Keep the listing within the current feature boundary: map/nearby browsing,
   local match notifications, deep links, and some older translated copy remain
   parity work.
7. Do not claim public availability until the intended production release is
   independently visible to a normal Play Store user.

Official references: [Play Console account setup](https://support.google.com/googleplay/android-developer/answer/6112435),
[device verification](https://support.google.com/googleplay/android-developer/answer/14316361),
and [testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465).
