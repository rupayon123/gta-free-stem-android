# Google Play listing handoff for v1.1.0

> Current internal-update candidate: version 1.1.0, **version code 4**. Code 3
> was uploaded only to an unpublished draft and is retained below as evidence
> for that tested candidate. Code 4 adds the navigation-label wrapping fix;
> its signed package and final navigation smoke require fresh receipts.


> **Superseded release-evidence notice (August 20, 2026):** The August 19
> screenshots, provenance hashes, signed AAB/APK identities, and recorded test
> counts referenced by this handoff describe an earlier source snapshot, not
> the materially changed current working tree. Keep them only as history; do
> not upload or present them as current-candidate evidence. The current
> candidate needs a source freeze, clean rebuild, fresh tests/device checks,
> screenshot recapture, and regenerated provenance before release review. No
> upload or publication is authorized by this document.

This directory prepares listing material for `com.rupayonhaldar.gtafreestem`
version `1.1.0` (`versionCode` 3). It does not authorize an upload or claim that
Google Play has reviewed or published the candidate.

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

The live map is now MapLibre/OpenFreeMap with an offline schematic fallback.
Recheck current listing descriptions against that behavior. Eighteen interface
languages do not prove fully translated provider descriptions. The live Android
privacy disclosure is at the Cloudflare URL below (October 27 policy update).

## Suggested listing fields

| Field | Candidate value |
| --- | --- |
| App name | GTA FREE STEM |
| Default language | English (Canada) |
| App category | Education |
| Contains ads | No |
| Privacy policy | https://gta-free-stem.pages.dev/privacy |
| Support | https://gta-free-stem.pages.dev/support |
| Terms | https://gta-free-stem.pages.dev/terms |

The English title, short description, full description, and version-3 release
notes are in `en-US/`. Review them in the live Console because character limits,
field names, declarations, and policy prompts can change.

## Deterministic assets

- `assets/icon-512.png` is a 512 × 512 32-bit RGBA PNG exported from the app's
  existing source icon without generative alteration. Its alpha channel is
  fully opaque, so the artwork remains visually identical to the source.
- `assets/feature-graphic-1024x500.png` is a 1024 × 500 opaque PNG composed from
  the existing logo, current brand palette, and exact factual listing copy.
- Historical August 19 exact-to-snapshot screenshots are retained in
  `assets/screenshots/phone/`; their source fingerprint, installed debug APK,
  capture state, dimensions, and hashes are recorded in
  `assets/PLAY_ASSET_PROVENANCE.txt`. They are superseded visual history, not
  evidence for the current working tree, a current signed AAB, or a
  Play-generated install.

## Historical August 19 accessibility text — match only the retained screenshots

Play Console accepts alt text for preview assets. Keep each description with
the matching file and recheck it against the final visible frame before upload.

| Asset | Candidate alt text |
| --- | --- |
| `01-home.png` | Home screen with storybook logo, free-for-everyone message, search, and five-tab navigation. |
| `02-browse-controls.png` | Opportunities screen with search, filters, Hunt, Nearby, alerts, and list-map controls. |
| `03-browse-list.png` | Opportunity list showing free GTA STEM events with provider, date, category, and ages. |
| `04-browse-map.png` | Offline schematic map with readable opportunity-count clusters. |
| `05-opportunity-detail.png` | Google Prompting Essentials detail with a free badge, provider, clean source excerpt, and offline map preview. |
| `06-profile.png` | Profile screen showing the on-device profile, Saved library, language, theme, alerts, and local-data controls. |

Before upload, verify every image and line of copy against the final signed AAB,
the live privacy/support/terms pages, the selected target audience, Data safety,
content rating, Families implications, and all current Play Console prompts.
