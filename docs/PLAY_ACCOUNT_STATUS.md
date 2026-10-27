# Google Play release status

> Current internal-update candidate: version 1.1.0, **version code 4**. Code 3
> was uploaded only to an unpublished draft and is retained below as evidence
> for that tested candidate. Code 4 adds the navigation-label wrapping fix;
> its signed package and final navigation smoke require fresh receipts.


Last public-safe status review: October 27, 2026. This file records only facts
that are appropriate for a public source repository.

> **Current-tree evidence reset:** the source has materially changed since the
> August 19 v1.1.0 working-tree snapshot. Its signed AAB/APK, test and lint
> counts, runtime checks, and screenshots are superseded historical evidence,
> not proof for today's source. A current candidate requires a source freeze,
> clean rebuild, fresh verification, newly signed artifacts, screenshot
> recapture, and regenerated provenance. This record authorizes no upload,
> rollout, or publication.

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

## Current v1.1.0 local candidate

- The permanent package name is `com.rupayonhaldar.gtafreestem`.
- The current source is version `1.1.0` (`versionCode` 3), an increase from the
  last internal-test version.
- The August 16, 2026 Console review reported that all apps were successfully
  registered for the then-current Android developer-verification requirements;
  confirm the live task state again before any v1.1.0 upload.
- Current behavior includes the storybook visual system and launch experience;
  adaptive Home/Opportunities/High School/Support/Profile navigation; compact
  list browsing, live 2D/3D MapLibre/OpenFreeMap maps with an offline schematic
  fallback; New Finds; local Current/Archive saves; full details and map previews;
  strict live/offline feed validation; and an 18-language interface with RTL support.
  The August 20 description of a 104-record feed and schematic-only map is
  historical, not current behavior.
- Nearby is explicit and optional: it requests approximate location for one
  foreground fix, keeps the fix only in memory, and uses it locally for distance
  sorting. The app does not attach that raw fix to feed or map requests. When an
  interactive map is shown, MapLibre requests OpenFreeMap styles, tiles, fonts,
  and sprites over HTTPS; delivery services receive IP/request metadata and
  requested map areas. The camera focuses public opportunity locations and user
  map navigation, rather than uploading a raw Nearby fix. Viewed areas can still
  reveal an area of interest. Map-resource caching differs from GPS-fix storage.
  The source removes the map SDK's unused precise-location permission; confirm
  the final signed manifest independently. No paid map service or background
  location is used. The old blanket “no live tiles/no location transmission”
  description must not be reused for this candidate.
- New-match alerts are explicit and optional: they request notification
  permission when Android requires it, use local WorkManager checks and on-device
  matching/history, and use no push provider. App-owned deep links and dynamic
  Opportunities/High School shortcuts are also present.

The working tree is not a frozen, reviewed release source. Its current full
test/lint, signed-artifact, screenshot, physical-device, Play-generated-install,
pre-launch, policy, and Play-processing gates remain open. Version 1.1.0 has not
been uploaded and has no production or public-availability approval.

## Superseded August 19 v1.1.0 snapshot — do not reuse

- The non-signing static release-configuration check passed for that snapshot.
- The Keychain-backed signing preflight passed. Its signed working-tree AAB and
  APK independently verified with the expected upload certificate; R8 retention,
  merged manifest, matching DEX/native payloads, and 16 KB compatibility were
  inspected.
- Its executable-source evidence included 151 passing JVM tests, zero-error
  debug/release lint, 19 of 19 API 36 gesture-navigation instrumentation tests,
  four adaptive/RTL MainActivity tests at a 900 dp tablet override, a focused
  three-button inset pass, and signed-release cold/warm deep-link smoke checks.
- Six exact-to-that-snapshot phone screenshots, listing copy, icon, and feature
  graphic were prepared with hashes and provenance.

Those results and assets are preserved only as August 19 history. They do not
verify the materially changed current tree, a current signed AAB/APK, a
Play-generated install, upload approval, production readiness, or publication.

## Historical v1.0.1 evidence

- 105 JVM tests passed.
- The full 11-test instrumentation suite passed on an API 36 emulator in
  three-button system-navigation mode.
- A focused system-navigation/inset regression passed in gesture mode.
- Debug and release lint completed with zero issues.
- The exact sanitized public source root produced a signed 5.2 MiB AAB.
- `jarsigner` verified the AAB, its public certificate fingerprint matched the
  protected upload key, and the public-safe AAB checksum is recorded in
  `verification/VERIFICATION.md`.

Google Play accepted version 1.0.1 into restricted internal testing. A fresh
October 27, 2026 Console check still reports it available to internal testers. The tester opt-in page provides a Download
test app link. This does not establish a Play-generated installation, physical-
device validation, closed testing, production, or public availability.

Play also reported one non-blocking warning for native code without a native
debug-symbol archive. The warning did not block that historical internal test.
Its relevance to v1.1.0 must be determined from the new bundle and Play report.

## Historical version-1 Play evidence

- Google Play accepted version `1.0` (`versionCode` 1) into restricted internal
  testing on August 16, 2026.
- Play App Signing was enabled for that earlier build.
- Play reported one non-blocking native debug-symbol warning that still requires
  review before broader distribution.

That version-1 acceptance is not evidence for v1.0.1 or v1.1.0, a production
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
   and record launch, offline, browse, map, Nearby, alerts, deep-link/shortcut,
   save, external-link, 18-language/RTL, reduced-motion, large-text, TalkBack,
   foldable, and system-navigation results without publishing tester identity.
2. Make and verify an independent encrypted backup of the upload key outside the
   repository.
3. After the final reviewed commit, rebuild and repeat the completed local
   bundle/signature/R8/native/16 KB inspection; then inspect Play-supported
   devices, investigate any native-symbol warning, and review the Play
   pre-launch report.
4. Finish the store listing, privacy, Data safety, content-rating, target-audience,
   and policy declarations for the exact candidate.
5. Complete whatever testing and production-access gates the live Play Console
   requires for the developer account at submission time.
6. Describe map/Nearby, local alerts, deep links/shortcuts, and localization
   precisely: interactive maps request live resources and expose IP/requested
   map areas; raw Nearby fixes remain ephemeral on device. Do not promise
   guaranteed alert timing, a push service, an online account, or fully translated
   provider descriptions. Verify precise/background permission absence in the
   exact signed artifact.
7. Do not claim public availability until the intended production release is
   independently visible to a normal Play Store user.

Official references: [Play Console account setup](https://support.google.com/googleplay/android-developer/answer/6112435),
[device verification](https://support.google.com/googleplay/android-developer/answer/14316361),
and [testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465).
