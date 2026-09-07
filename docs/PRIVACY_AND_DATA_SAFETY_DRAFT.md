# Privacy Policy and Play Data Safety Draft

> Working draft only. This document is neither legal advice nor a completed Play Console declaration. It assumes the intended behavior described below. Reconcile it with the exact release artifact, merged manifest, dependencies, runtime network traffic, Android backup behavior, and current Google Play definitions before publishing or submitting any answer.

Last technical-draft review: August 19, 2026.

## Assumed release behavior

- No account creation or sign-in.
- No advertising, behavioral tracking, analytics, attribution, crash upload, or
  push notification service.
- No developer-operated collection of names, email addresses, precise or
  approximate location, contacts, photos, messages, payment data, or device
  identifiers.
- An optional display name, searches and filters, saved opportunity snapshots,
  language/theme choices, alert preference/history, and known matching
  opportunity IDs are processed only in local app storage on the device.
- Nearby is optional and user-initiated. It requests Android approximate
  (`ACCESS_COARSE_LOCATION`) permission, obtains one foreground fix, uses that
  fix locally for distance sorting, keeps it only in memory, and neither saves
  nor sends it. The app does not request precise or background location.
- New-match alerts are optional local notifications. When the user enables
  them, the app requests `POST_NOTIFICATIONS` where Android requires it and uses
  WorkManager to refresh the public feed approximately every three hours while
  connected. Matching, known-ID comparison, and notification throttling happen
  on device; there is no FCM or other push-registration token.
- Public opportunity data is downloaded over HTTPS from GitHub/jsDelivr.
- Opportunity and source links can open third-party websites in the user's browser.
- GitHub/jsDelivr and linked sites may receive ordinary request metadata such as IP address, time, user agent, and requested URL under their own policies.
- Android system backup and device-to-device transfer are disabled for app data in the current manifest/rules. This must be confirmed in the merged release manifest.
- WorkManager adds normal platform permissions and private app components in
  the merged manifest, including network-state, wake-lock, boot-completed, and
  foreground-service support. Their presence supports reliable local work; it
  does not add a developer server, telemetry, continuous location, or a push
  channel. Reconcile the exact merged release manifest before submission.

Any false assumption invalidates the candidate wording and Data safety answers.

## Candidate Play Data safety answers

These are review prompts, not answers to submit automatically.

| Play question | Candidate response | Required evidence before submission |
| --- | --- | --- |
| Does the app collect or share required user-data types? | Candidate: No developer collection or sharing. | Inspect the release dependency graph, merged manifest, SDK behavior, network traffic, and Google's current definitions. Resolve whether feed-host request metadata or Android backup changes any disclosure. |
| Does the app collect or share location? | Candidate: No. The user-initiated approximate fix is used only ephemerally on device and is not transmitted or persisted. | Verify runtime traffic, process-death behavior, permission scope, the exact release implementation, and the definitions presented in the live form. |
| Is all transmitted user data encrypted in transit? | Candidate: Yes for app-managed traffic. | Confirm all app endpoints are HTTPS, no cleartext exception is merged, and redirects remain HTTPS. |
| Can users request deletion? | Candidate: no developer account/server data exists to delete. Local data can be removed through app controls, app storage settings, or uninstall. | Test the actual deletion path and confirm disabled backup/device transfer does not restore app data. |
| Does the app contain ads? | Candidate: No. | Confirm no ad SDK, cross-promotion SDK, or ad rendering at runtime. |
| Is an account required? | Candidate: No. | Verify every feature and linked flow. |
| Is the app directed to children? | Undecided product/legal declaration. | Choose the truthful target ages based on product design, listing, content, and opportunity audience. If any audience includes children, complete Families-policy review. |

On-device-only processing is generally treated differently from data sent off device, but the developer must apply the definitions presented in the live form. Do not use “we do not sell data” as a substitute for the more specific collection and sharing questions.

## Candidate public privacy policy

The following copy can be adapted for <https://gta-free-stem.vercel.app/privacy> after technical and legal review.

### GTA FREE STEM Privacy Policy

**Effective date:** August 18, 2026

GTA FREE STEM helps people discover free science, technology, engineering, and mathematics opportunities. You can use the app without creating an account.

#### Information handled by the app

GTA FREE STEM does not ask you to provide an email address, password, payment
information, contacts, photos, or precise location. You may optionally enter a
display name for an on-device profile. That name is not sent to the developer
or used to create an online account. The app does not include advertising or
analytics SDKs, and the developer does not use the app to track you across apps
or websites.

The optional display name, searches and filters, saved opportunity details,
language and theme choices, alert preference, and local alert history are kept
in the app's local storage on your device. The current app configuration
excludes app data from Android backup and device-to-device transfer.

#### Nearby location

If you choose Use nearby, GTA FREE STEM asks Android for approximate location
permission. The app obtains a single foreground location fix and uses it on
your device to sort opportunities by distance. The fix is kept only in memory,
is cleared when you turn Nearby off or the app process ends, and is not sent to
the developer or the opportunity-feed providers. GTA FREE STEM does not ask for
precise or background location.

#### New-match notifications

If you turn on new-match alerts, GTA FREE STEM may ask Android for notification
permission. Android can then run an on-device background check approximately
every three hours when a network connection is available. The app downloads the
same public opportunity feed, applies your saved search and filters locally,
remembers matching opportunity IDs on your device, and posts a local
notification when it finds new matches. Notifications are throttled and their
delivery time is not guaranteed by Android. The feature uses no push
notification provider and creates no server-side subscription. Turning alerts
off cancels the scheduled work.

#### Opportunity feed and network information

The app downloads a public opportunity feed over HTTPS from services operated
by GitHub and/or jsDelivr, both while you use the app and, if you enable alerts,
during periodic background checks. Like most internet services, those providers
may automatically receive technical request information, such as an IP address,
request time, user agent, and requested file. The app does not send your Nearby
fix, display name, search text, or filters with that request. Provider handling
of request metadata is governed by their own privacy policies. GTA FREE STEM
does not use the feed request to create advertising or analytics profiles.

#### External links

Opportunity details may link to websites operated by schools, libraries, community organizations, or other third parties. When you open an external link, that site's privacy practices and terms apply. GTA FREE STEM does not control those sites. Review a provider's details and policies before registering or sharing personal information.

#### Data retention and deletion

Because GTA FREE STEM does not provide an account or developer-operated cloud
storage, the developer does not retain an account record to delete. You can
clear the optional profile, saved opportunities, search/filter history, and
local alert history from Profile, remove local app data through Android's
app-storage settings, or uninstall the app. Turning alerts off cancels their
scheduled work. The current app configuration excludes app data from Android
backup and device-to-device transfer.

#### Children's privacy

GTA FREE STEM does not knowingly ask children to submit personal information to the app. Opportunity providers and linked websites may set their own age, parent/guardian consent, registration, and privacy requirements. A parent or guardian should review those requirements before a child uses a linked registration service.

This paragraph must be reviewed against the final Play target-audience declaration and all applicable children's-privacy/Families requirements; it does not by itself establish compliance.

#### Changes

This policy may be updated if the app's features, service providers, or legal obligations change. The effective date above will identify the current version.

#### Contact

For privacy or support questions, use the monitored contact method at <https://gta-free-stem.vercel.app/support>.

## Final-review inventory

Before reusing the draft, record evidence for:

- [ ] Inspect the application ID and version again on a newly signed artifact
  built from the reviewed source freeze. The August 19 signed-local-candidate
  check is superseded historical evidence and authorizes no upload.
- [ ] Inspect the fresh merged manifest permissions and
  `android:allowBackup`/data-extraction rules. The August 19 merged-manifest
  inspection is historical only.
- [ ] Inspect the fresh release dependency tree and embedded SDK inventory. The
  August 19 dependency review is historical only.
- [ ] Runtime traffic during cold launch, feed refresh, search/filter, save, Nearby, alert scheduling/background execution, notification opening, external-link opening, restore, and error handling.
- [ ] Every hostname, redirect, request header, request body, and telemetry endpoint.
- [ ] All local fields and their deletion/backup/restore behavior, including
  alert preference/history and proof that the Nearby fix is in-memory only.
  Source/unit checks pass; physical uninstall/upgrade/restore validation remains.
- [ ] Android permission prompts and denial/revocation behavior for approximate
  location and notifications, plus the complete merged WorkManager manifest.
- [ ] GitHub and jsDelivr roles and current privacy terms.
- [x] Third-party browser/link transition wording reviewed in the app and prepared public-policy patch.
- [ ] Target audience and Families-policy decision.
- [ ] Public privacy/support/terms pages and monitored contact.
- [ ] Play Data safety answers reviewed by the accountable developer immediately before submission.

If the final app changes the documented location or notification behavior, or
adds accounts, analytics, crash uploads, precise/background location, a push
service, personalized recommendations, user submissions, or another network
SDK, stop and rewrite both the policy and Play declarations before release.
