# Google Play Console Checklist

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

- [x] Prepared app title, short description, full description, and release notes are accurate for the local candidate and do not promise unavailable features; recheck in the live Console.
- [x] Prepared listing copy describes the implemented offline schematic map, one-shot
  approximate Nearby flow, opt-in local alerts, deep links/shortcuts, and
  18-language catalog accurately without promising live map tiles, precise or
  background location, guaranteed alert timing, push delivery, or an account.
- [ ] Current-candidate high-resolution icon, feature graphic, and phone screenshots meet the dimensions shown by the live Console; the retained August 19 assets are historical only and tablet screenshots are not included.
- [ ] Phone screenshots are recaptured from the exact frozen candidate, show real app behavior, contain no private data or painted overlays, and have regenerated source/APK/hash provenance.
- [ ] App category and tags match free STEM-opportunity discovery.
- [ ] Support email is monitored.
- [ ] Support website works while logged out: <https://gta-free-stem.vercel.app/support>.
- [ ] Privacy policy works while logged out: <https://gta-free-stem.vercel.app/privacy>.
- [ ] Terms page works while logged out: <https://gta-free-stem.vercel.app/terms>.
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
  `POST_NOTIFICATIONS`, plus WorkManager's merged normal permissions/components.
- [ ] Intellectual-property rights verified for the name, icons, screenshots, feed data, descriptions, and third-party marks.

## 5. Privacy and data review

- [ ] Fresh frozen-source, merged-manifest, and dependency review confirms no accounts, ads, analytics, attribution, crash-reporting upload, paid-map
  SDK, or push notification service in the release; the August 19 artifact
  review is historical only.
- [ ] Fresh merged-release inspection confirms saved items/preferences and alert
  history remain local, the Nearby fix is in-memory only, and Android backup
  and device transfer remain disabled. The current source manifest/rules declare
  that posture, but exact-artifact verification is pending.
- [x] Approximate location is requested only after Use nearby, only for a
  one-shot foreground fix; no precise/background permission, persistence, or
  transmission exists.
- [x] Notifications are explicitly opt-in; denial/revocation is handled, local
  WorkManager checks and known-ID matching are verified, disabling cancels
  scheduled work, and no push registration occurs.
- [x] Production opportunity feed uses HTTPS GitHub/jsDelivr endpoints only.
- [ ] Normal request metadata visible to GitHub/jsDelivr is addressed in the public privacy wording and Data safety analysis.
- [x] No unencrypted HTTP endpoint or cleartext exception exists.
- [ ] External links open safely and are distinguishable from app-owned content.
- [ ] Local data deletion behavior tested, including uninstall; confirm that app data is not restored from Android backup or device transfer.
- [ ] Public privacy wording names a monitored contact and has an effective date.

## 6. Technical release evidence

- [x] `./scripts/verify-release-config.sh` passes for the current source-manifest configuration. The August 19 static pass predates current source changes.
- [ ] `REQUIRE_SIGNING=1 ./scripts/verify-release-config.sh` passes for a newly signed v1.1.0 artifact without exposing secrets, and its upload certificate is independently checked. The August 19 pass is historical only.
- [x] Complete current-source v1.1.0 JVM suite passes from the reviewed tree (186/186).
- [x] Complete v1.1.0 instrumentation suite passes on required configurations from the current source: API 36 emulator (36/36) and API 26 emulator (36/36). The superseded August 19 snapshot recorded 19 of 19 on API 36 gesture navigation, four adaptive/RTL MainActivity tests at a 900 dp tablet override, and a three-button inset pass.
- [x] v1.1.0 debug and release lint are rerun from the current source. The superseded August 19 snapshot recorded zero errors and 23 reviewed non-blocking warnings in each variant.
- [ ] Exact frozen-tree v1.1.0 candidate is built, signed, inspected, and identified without exposing secrets. The August 19 signed working-tree artifact is historical and must not be uploaded.
- [x] CI passes tests, debug/release lint, debug assembly, and unsigned release bundle on the current source.
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

### Current v1.1.0 candidate record

```text
Source freeze / reviewed commit: pending
Application ID: com.rupayonhaldar.gtafreestem
Version name / code: 1.1.0 / 3
Fresh full JVM, lint, and instrumentation evidence: complete (186 JVM tests; 0-error debug/release lint; API 36 36/36 connected; API 26 36/36 connected), same-source tree
Fresh signed AAB SHA-256: pending
Fresh signed APK SHA-256: pending
Fresh upload-certificate cross-check: pending
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
