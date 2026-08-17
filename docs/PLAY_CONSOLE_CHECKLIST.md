# Google Play Console Checklist

Use this as an evidence checklist, not as proof of readiness. Requirements shown
by the live Play Console take precedence. Last policy review: August 16, 2026.
Keep account identifiers, contact details, identity evidence, tester identities,
invitation URLs, private signing material, and recovery records outside the
repository.

## 1. Developer account

- [ ] Correct developer account and current fee terms confirmed privately in the live Console.
- [ ] Identity, payments-profile, contact, and device-verification requirements completed privately; no supporting documents are committed.
- [ ] Public developer name and contact details reviewed for accuracy.
- [ ] Android developer verification complete and `com.rupayonhaldar.gtafreestem` registered where requested.
- [ ] September 30, 2026 developer-verification enforcement reviewed for the initially affected countries.
- [ ] Current production-access testing requirements shown for the account are planned and tracked privately.

## 2. App identity and ownership

- [ ] Play app entry ownership confirmed privately for the intended release owner.
- [x] App name is GTA FREE STEM.
- [x] Default language is English (Canada); App and Free were selected deliberately.
- [x] Application ID is exactly `com.rupayonhaldar.gtafreestem`.
- [x] Current source is `versionName` `1.0.1`, `versionCode` 2.
- [x] Historical version `1.0` (`versionCode` 1) acceptance is kept separate from current-candidate evidence.
- [x] Exact v1.0.1 signing identity and increasing version code confirmed before any upload.
- [x] Play App Signing was enrolled for the earlier version-1 internal test.
- [ ] Upload keystore is outside the repository with password-manager and encrypted offline backups.

## 3. Store listing

- [ ] App title, short description, and full description are accurate and do not promise unavailable features.
- [ ] Listing does not promise map/nearby browsing, local match notifications, deep links, or complete localization while those parity gaps remain.
- [ ] High-resolution icon, feature graphic, phone screenshots, and any tablet screenshots meet the dimensions currently shown in Play Console.
- [ ] Screenshots come from the exact release candidate, show real app behavior, and contain no private data or misleading overlays.
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
- [ ] Permissions and sensitive API declarations match the merged release manifest.
- [ ] Intellectual-property rights verified for the name, icons, screenshots, feed data, descriptions, and third-party marks.

## 5. Privacy and data review

- [ ] No accounts, ads, analytics, attribution, crash-reporting upload, or push notification service in the release.
- [ ] Saved items/preferences remain local and the merged release manifest/rules still disable Android backup.
- [ ] Production opportunity feed uses HTTPS GitHub/jsDelivr endpoints only.
- [ ] Normal request metadata visible to GitHub/jsDelivr is addressed in the public privacy wording and Data safety analysis.
- [ ] No unencrypted HTTP endpoint or cleartext exception exists.
- [ ] External links open safely and are distinguishable from app-owned content.
- [ ] Local data deletion behavior tested, including uninstall; confirm that app data is not restored from Android backup or device transfer.
- [ ] Public privacy wording names a monitored contact and has an effective date.

## 6. Technical release evidence

- [x] `./scripts/verify-release-config.sh` passes for the intended version.
- [x] `REQUIRE_SIGNING=1 ./scripts/verify-release-config.sh` passes locally without exposing secrets.
- [x] Current v1.0.1 JVM suite passed: 105 tests, 0 failures, 0 errors.
- [x] Current full instrumentation suite passed: 11 of 11 tests on API 36 with three-button system navigation.
- [x] Focused system-navigation/inset regression passed: 1 of 1 test on API 36 with gesture navigation.
- [x] Current debug and release lint completed with zero issues.
- [x] Exact v1.0.1 release candidate built, signed, inspected, and identified without exposing secrets.
- [ ] CI passes tests, debug lint, and debug assembly on the reviewed source commit.
- [x] Historical version-1 bundle was accepted into restricted internal testing; this is not v1.0.1 evidence.
- [ ] Bundle contents, permissions, supported devices, native code, and download size inspected.
- [ ] Release build tested on supported physical Android hardware, including API 26 behavior.
- [ ] Offline, failure, retry, empty, and restored-process states tested.
- [ ] Accessibility tested with TalkBack, large font/display size, keyboard/switch focus where applicable, contrast, and touch targets.
- [ ] Upgrade from the latest distributed test build preserves intended local data.
- [ ] Play-generated APKs inspected and installed from a Play test track.
- [ ] Pre-launch report reviewed; each warning is resolved or documented with evidence.

## 7. Testing tracks and production access

- [x] Historical version 1 was accepted into restricted internal testing; no tester identity or invitation data is stored here.
- [ ] Version 1.0.1 uploaded to the intended test track and verified there; no such upload is currently claimed.
- [ ] Closed test configured with the correct country and tester eligibility.
- [ ] Current tester-count, duration, and production-access requirements shown by the live Console are completed and evidenced privately.
- [ ] Production-access application answers describe real testing and feedback, without boilerplate or invented results.
- [ ] Policy review outcome and all Play Console warnings resolved.
- [ ] Release notes match the exact candidate.
- [ ] Countries/regions selected deliberately, accounting for opportunity coverage and legal/policy obligations.

## 8. Explicit rollout gate

- [x] Exact source commit, AAB SHA-256, version, and upload certificate fingerprint independently cross-checked.
- [ ] Privacy, support, terms, Data safety, content rating, target audience, and Families implications approved for the exact candidate.
- [ ] Production rollout plan, monitoring owner, and halt criteria documented.
- [ ] Authorized person explicitly approves the exact track and artifact.
- [ ] No publication claim is made until Play Console shows the intended release live and it is independently checked from a public user context.

## Public evidence record

Do not add numeric Play app/account IDs, tester names or emails, invitation URLs,
identity/contact evidence, keystore paths, passwords, or secret-manager details.

```text
Date:
Source commit:
Application ID:
Version name / code:
AAB SHA-256:
Public upload-certificate SHA-256:
CI run:
Restricted-track result (no invite URL or tester identities):
Physical devices / API levels:
Play pre-launch report:
Policy review notes:
Privacy/support/terms check:
Approval and scope:
Public availability check (only after rollout):
```
