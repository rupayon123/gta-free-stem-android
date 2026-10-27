# Google Play Console Checklist

> Current internal-update candidate: version 1.1.0, **version code 4**. Code 3
> was uploaded only to an unpublished draft and is retained below as evidence
> for that tested candidate. Code 4 adds the navigation-label wrapping fix;
> its signed package and final navigation smoke require fresh receipts.


Use this as an evidence checklist, not as proof of readiness. Requirements shown
by the live Play Console take precedence. Last policy review: August 18, 2026.
Keep account identifiers, contact details, identity evidence, tester identities,
invitation URLs, private signing material, and recovery records outside the
repository.

Checked account and track items record historical Console facts. They do not
authorize or prove processing of the v1.1.0 candidate unless v1.1.0 is named
explicitly.

> **Evidence reset (August 20, 2026):** the app source materially changed after
> the August 19 v1.1.0 working-tree snapshot. Its 151-test/19-test counts, lint
> results, signed AAB/APK and hashes, runtime checks, and screenshots are
> superseded historical evidence. Uncheck and repeat every exact-artifact gate
> after a reviewed source freeze. Nothing in this checklist authorizes an
> upload, rollout, or publication.

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

## 1. Developer account

- [x] Correct developer account and current fee terms confirmed privately in the live Console.
- [ ] Identity, payments-profile, contact, and device-verification requirements completed privately; no supporting documents are committed.
- [ ] Public developer name and contact details reviewed for accuracy.
- [x] Android developer verification complete and `com.rupayonhaldar.gtafreestem` registered where requested.
- [ ] September 30, 2026 developer-verification enforcement reviewed for the initially affected countries.
- [ ] Current production-access testing requirements shown for the account are planned and tracked privately.

## 2. App identity and ownership

- [x] Play app entry ownership confirmed privately for the intended release owner.
- [x] App name is GTA FREE STEM.
- [x] Default language is English (Canada); App and Free were selected deliberately.
- [x] Application ID is exactly `com.rupayonhaldar.gtafreestem`.
- [x] Current local candidate source is `versionName` `1.1.0`, `versionCode` 3.
- [x] Historical version `1.0` (`versionCode` 1) and version `1.0.1` (`versionCode` 2) acceptance/evidence are kept separate from current-candidate evidence.
- [ ] Exact v1.1.0 signing identity and increasing version code independently confirmed for a newly signed artifact built after the reviewed source freeze; the August 19 result is historical only.
- [x] Play App Signing was enrolled for the earlier version-1 internal test.
- [ ] Upload keystore is outside the repository with password-manager and encrypted offline backups.

## 3. Store listing

- [ ] Reconcile prepared title, description, and release notes with the current candidate and live Console; earlier copy approval predates the map/feed changes.
- [ ] Final listing copy distinguishes live MapLibre/OpenFreeMap maps and their
  resource requests from the offline schematic fallback; one-shot approximate
  Nearby, local alerts, deep links/shortcuts, and 18-language interface support
  are described accurately. Do not claim full provider-description translations,
  guaranteed alerts, push delivery, or an online account. Historical
  schematic-only listing approval is not current-candidate proof.
- [ ] Current-candidate high-resolution icon, feature graphic, and phone screenshots meet the dimensions shown by the live Console; the retained August 19 assets are historical only and tablet screenshots are not included.
- [ ] Phone screenshots are recaptured from the exact frozen candidate, show real app behavior, contain no private data or painted overlays, and have regenerated source/APK/hash provenance.
- [ ] App category and tags match free STEM-opportunity discovery.
- [ ] Support email is monitored.
- [ ] Support website works while logged out: <https://gta-free-stem.pages.dev/support>.
- [ ] Privacy policy works while logged out: <https://gta-free-stem.pages.dev/privacy>.
- [ ] Terms page works while logged out: <https://gta-free-stem.pages.dev/terms>.
- [ ] External GitHub/jsDelivr feed and opportunity links are described accurately where relevant.

## 4. App content and policy declarations

- [ ] App access: declare that no account is required; provide review instructions for any non-obvious path.
- [ ] Ads: declare "No" only after dependency and runtime review confirms no ads or ad SDKs.
- [ ] Data safety: reconcile every answer with `PRIVACY_AND_DATA_SAFETY_DRAFT.md`, final dependencies, permissions, network traffic, and the expected disabled-backup behavior.
- [ ] Content rating questionnaire completed from the final app content.
- [ ] Target audience selected truthfully; do not select adult-only merely to avoid Families requirements.
- [ ] If any selected audience includes children, complete the current Families-policy checklist and review all linked/external content for age appropriateness.
- [ ] News, health, financial, government, social, dating, and other special-category declarations answered from actual behavior rather than the app name.
- [ ] No account-deletion declaration is needed only if there truly is no account creation anywhere in or linked from the app.
- [ ] Permissions and sensitive API declarations match the merged release
  manifest: source `INTERNET`, `ACCESS_COARSE_LOCATION`, and
  `POST_NOTIFICATIONS`, plus WorkManager/MapLibre merged normal permissions and
  components. Confirm the source `tools:node="remove"` for unused
  `ACCESS_FINE_LOCATION` survives the final signed artifact; no background
  location is intended.
- [ ] Intellectual-property rights verified for the name, icons, screenshots, feed data, descriptions, and third-party marks.

## 5. Privacy and data review

- [ ] Fresh frozen-source, merged-manifest, and dependency review confirms no accounts, ads, analytics, attribution, crash-reporting upload, paid-map
  SDK, or push notification service in the release; the August 19 artifact
  review is historical only.
- [ ] Fresh merged-release inspection confirms saved items/preferences and alert
  history remain local, the Nearby fix is in-memory only, and Android backup
  and device transfer remain disabled. The current source manifest/rules declare
  that posture, but exact-artifact verification is pending.
- [ ] Final-artifact Nearby checks confirm a user-initiated approximate foreground
  fix, kept in memory and stripped before browse preferences are persisted;
  requests do not attach the raw fix. Verify no precise/background permission in
  the signed artifact. This does not imply no location-related network data:
  map requests expose IP and requested viewport/tile areas, and map resources
  may be cached separately.
- [x] Notifications are explicitly opt-in; denial/revocation is handled, local
  WorkManager checks and known-ID matching are verified, disabling cancels
  scheduled work, and no push registration occurs.
- [x] Production opportunity feed uses HTTPS GitHub/jsDelivr endpoints only.
- [ ] Reconcile GitHub/jsDelivr feed metadata and OpenFreeMap/infrastructure
  map-resource metadata (IP and requested map areas) with the public privacy
  wording and any applicable Data safety review; on-device raw-fix handling alone
  does not determine all form answers. Internal-only tracks are exempt from the
  Data safety section under Google's linked guidance; check actual track scope
  and any prompts shown without inventing an internal-upload form gate.
- [x] No unencrypted HTTP endpoint or cleartext exception exists.
- [ ] External links open safely and are distinguishable from app-owned content.
- [ ] Local data deletion behavior tested, including uninstall; confirm that app data is not restored from Android backup or device transfer.
- [ ] Public privacy wording names a monitored contact and has an effective date.

## 6. Technical release evidence

- [ ] Attach the non-signing release-configuration receipt for the reviewed source; the August 19 static pass is historical.
- [ ] `REQUIRE_SIGNING=1 ./scripts/verify-release-config.sh` passes for a newly signed v1.1.0 artifact without exposing secrets, and its upload certificate is independently checked. The August 19 pass is historical only.
- [x] October 27 local JVM XML reports 232 tests with zero failures/errors for the candidate review; the older 186/186 result is historical below.
- [x] Development candidate `13c6085` passed 45/45 runtime cases plus 3/3 reduced-motion cases on each of API 26 and API 36; signed-release smoke remains separate.
  Historical 36/36-on-each-API and August 19 evidence is retained below.
- [ ] Attach exact-candidate debug/release lint receipts. Older zero-error results are historical and do not alone verify this source.
- [ ] Exact frozen-tree v1.1.0 candidate is built, signed, inspected, and identified without exposing secrets. The August 19 signed working-tree artifact is historical and must not be uploaded.
- [ ] Attach CI results for the final reviewed source; old checked CI entries are historical rather than proof for this candidate.
- [x] Historical v1.0.1 evidence is preserved below: 105 JVM tests; 11 of 11 API 36 three-button instrumentation tests; 1 of 1 gesture-navigation inset regression; zero-issue debug/release lint; signed and inspected 5.2 MiB AAB. None of this is v1.1.0 evidence.
- [x] Historical version-1 and v1.0.1 bundles were accepted into restricted internal testing; neither acceptance is v1.1.0 evidence.
- [ ] Fresh v1.1.0 bundle contents, merged permissions/components, native code, R8 retention, and 16 KB page-size compatibility are inspected from the frozen source; the August 19 inspection is historical, and Play-supported-device/processed-download details remain external.
- [ ] Release build tested on supported physical Android hardware, including API 26 behavior.
- [ ] Offline, failure, retry, empty, and restored-process states tested.
- [ ] Map/list, Nearby grant/deny/timeout/clear, local alerts, deep links,
  shortcuts, notification opening, all 18 languages/RTL, and reduced motion
  tested from cold and warm states.
- [ ] Accessibility tested with TalkBack, large font/display size, keyboard/switch focus where applicable, contrast, and touch targets.
- [ ] Upgrade from the latest distributed test build preserves intended local data.
- [ ] Play-generated APKs inspected and installed from a Play test track.
- [ ] Pre-launch report reviewed; each warning is resolved or documented with evidence.

## 7. Testing tracks and production access

- [x] Historical version 1 was accepted into restricted internal testing; no tester identity or invitation data is stored here.
- [x] Version 1.0.1 uploaded to the internal-test track and shown as available to internal testers; no tester identity or invitation URL is stored here.
- [ ] Version 1.1.0 uploaded to an explicitly authorized test track and its
  processing result recorded. Local preparation does not authorize this step.
- [ ] Closed test configured with the correct country and tester eligibility.
- [ ] Current tester-count, duration, and production-access requirements shown by the live Console are completed and evidenced privately.
- [ ] Production-access application answers describe real testing and feedback, without boilerplate or invented results.
- [ ] Policy review outcome and all Play Console warnings resolved.
- [ ] Release notes match the exact candidate.
- [ ] Countries/regions selected deliberately, accounting for opportunity coverage and legal/policy obligations.

## 8. Explicit rollout gate

- [ ] Exact v1.1.0 source commit, AAB SHA-256, version, and upload certificate fingerprint independently cross-checked.
- [ ] Privacy, support, terms, Data safety, content rating, target audience, and Families implications approved for the exact candidate.
- [ ] Production rollout plan, monitoring owner, and halt criteria documented.
- [x] Historical authorization covered the v1.0.1 internal-test artifact only; it is not v1.1.0 upload or production approval.
- [ ] Authorized person explicitly approves the exact v1.1.0 artifact and intended track before upload.
- [ ] No publication claim is made until Play Console shows the intended release live and it is independently checked from a public user context.

## Public evidence record

Do not add numeric Play app/account IDs, tester names or emails, invitation URLs,
identity/contact evidence, keystore paths, passwords, or secret-manager details.

### Historical v1.0.1 evidence

```text
Date: August 16, 2026
Source commit: 292c90256cda68393b603411f3f92297e0ad85ce
Application ID: com.rupayonhaldar.gtafreestem
Version name / code: 1.0.1 / 2
AAB SHA-256: 63cd724942ceed05e94321eaabeb485bfbec880c1ec4b423dfb87b9b9b4e6fbb
Public upload-certificate SHA-256: 0D:8E:B2:CD:06:A1:89:B5:FF:7C:15:2A:B7:AE:72:C5:60:89:26:4C:FD:D5:64:95:E2:71:5B:BC:A7:DB:F7:B5
CI run: https://github.com/rupayon123/gta-free-stem-android/actions/runs/31981089197 (passed)
Restricted-track result (no invite URL or tester identities): available to internal testers; one non-blocking native debug-symbol warning
Physical devices / API levels: v1.0.1 pending; API 36 emulator evidence recorded above
Play pre-launch report:
Policy review notes:
Privacy/support/terms check:
Approval and scope:
Public availability check (only after rollout):
```

### Superseded August 19 v1.1.0 working-tree record — do not reuse

The hashes and results below identify only the materially changed-away-from
August 19 snapshot. Preserve them as history; do not upload its artifacts or use
its counts/screenshots as current-candidate proof.

```text
Date: August 18, 2026
Evidence completed: August 19, 2026
Source commit: pending final clean reviewed state; working tree is based on bb67cbbcaf5dda20c8f6408768faa64d3ab51b2f
Application ID: com.rupayonhaldar.gtafreestem
Version name / code: 1.1.0 / 3
AAB SHA-256: 0e59dc75edafe0c8a432f4462dc3d974d105c9c92e44b138374cf78ef3c67176 (superseded historical signed working-tree candidate; do not upload)
APK SHA-256: 30713d26f70c941d868741d32188e5644cd322c1034596c1c3b6e3aadac546c7 (superseded historical signed working-tree candidate; do not upload)
Public upload-certificate SHA-256: 0D:8E:B2:CD:06:A1:89:B5:FF:7C:15:2A:B7:AE:72:C5:60:89:26:4C:FD:D5:64:95:E2:71:5B:BC:A7:DB:F7:B5
CI run: pending final source state
Historical test/lint result: 151/151 JVM; debug/release lint 0 errors; signed R8 AAB/APK verified; exact August 19 snapshot 19/19 instrumentation
Emulator devices / API levels: medium_phone API 36; 19/19 gesture; 4/4 900 dp tablet adaptive/RTL; 1/1 three-button inset; signed-release cold launch plus warm/cold deep-link smoke
Physical devices / API levels: pending
Play-generated artifact install: pending
Play pre-launch report: pending
Restricted-track result: not uploaded
Policy review notes: pending
Privacy/support/terms check: Android legal/support patch prepared and locally built; monitored support email, deployment, and logged-out public verification pending
Approval and scope: pending
Public availability check (only after rollout): pending
```

### Superseded intermediate v1.1.0 checklist evidence — date/source unspecified

The preceding checklist previously marked 186/186 JVM, API 36 36/36, API 26
36/36, debug/release lint, and CI complete. Preserve those recorded results only
as evidence for that earlier snapshot; no exact date/source was recorded there.
They are not the October 27 232-test/45+3/pending-API-36 candidate receipt.
The superseded August 19 lint entry recorded zero errors and 23 reviewed
non-blocking warnings in each debug/release variant; retain that count only
as evidence for the August 19 snapshot.

### Current v1.1.0 candidate record (October 27, 2026)

```text
Source freeze / reviewed commit: pending
Application ID: com.rupayonhaldar.gtafreestem
Version name / code: 1.1.0 / 3
Current runtime source at review: 13c6085 (signed release still pending)
Local JVM XML: 232 tests, 0 failures, 0 errors
API 26 emulator runtime: 45/45 plus 3/3 reduced-motion passed
API 36 emulator runtime: 45/45 plus 3/3 reduced-motion passed
Exact-candidate lint and CI receipts: attach independently; not inferred from old checklist
Fresh signed AAB SHA-256: pending
Fresh signed APK SHA-256: pending
Existing upload-certificate cross-check: passed October 27; signed artifact cross-check pending
Fresh R8, merged-manifest, native, and 16 KB inspection: pending
Fresh exact-build screenshots and provenance: pending
Physical devices / API levels: pending
Play-generated artifact install: pending
Play pre-launch report: pending
Restricted-track result: not uploaded
Policy and public-page review: pending
Explicit artifact/track upload approval: pending
Public availability check (only after rollout): pending
```
