# Google Play listing handoff for v1.1.0

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

## Suggested listing fields

| Field | Candidate value |
| --- | --- |
| App name | GTA FREE STEM |
| Default language | English (Canada) |
| App category | Education |
| Contains ads | No |
| Privacy policy | https://gta-free-stem.vercel.app/privacy |
| Support | https://gta-free-stem.vercel.app/support |
| Terms | https://gta-free-stem.vercel.app/terms |

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

## Candidate accessibility text

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
