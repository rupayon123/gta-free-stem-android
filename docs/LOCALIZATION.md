# Android localization guide

GTA FREE STEM welcomes human-reviewed translations that use clear, respectful
language for students, families, educators, and community organizations.

## How localization works

The Android app loads its shared interface catalog from
`app/src/main/res/raw/app_strings.json`. The catalog currently contains 184 keys
for each of 18 languages. The app supports an explicit language choice or the
device language, uses English as a safe fallback, and mirrors Arabic, Farsi, and
Urdu right-to-left.

The supported language identifiers are:

| Tag | Language | Direction |
| --- | --- | --- |
| `en` | English | LTR |
| `fr` | French | LTR |
| `zh` | Simplified Chinese | LTR |
| `yue` | Cantonese, Traditional script | LTR |
| `pa` | Punjabi | LTR |
| `ur` | Urdu | RTL |
| `ta` | Tamil | LTR |
| `tl` | Filipino/Tagalog | LTR |
| `es` | Spanish | LTR |
| `ar` | Arabic | RTL |
| `fa` | Farsi/Persian | RTL |
| `hi` | Hindi | LTR |
| `pt` | Portuguese | LTR |
| `gu` | Gujarati | LTR |
| `bn` | Bengali | LTR |
| `ja` | Japanese | LTR |
| `ko` | Korean | LTR |
| `hu` | Hungarian | LTR |

Some older browse, detail, status, and accessibility copy still needs to be
moved into the catalog. Do not describe any language as complete until a device
review confirms that every visible and spoken string is localized.

## Improve an existing translation

1. Open a translation issue and name the language, fluent reviewer, and keys you
   plan to review.
2. Edit only the relevant values in `app_strings.json`. Preserve key names,
   placeholders such as `{count}`, and valid JSON.
3. Keep `GTA FREE STEM` as the project name unless the maintainer approves a
   localized brand treatment.
4. Prefer plain, natural language over literal word-for-word translation. Avoid
   assumptions about gender, age, ability, family structure, or newcomer status.
5. Have a fluent human reviewer inspect the copy in the running app. Machine
   translation may help draft text, but it is not sufficient review by itself.

The Apple app uses the same catalog shape. When a change should apply to both
platforms, link matching Android and iOS issues or pull requests and keep the
catalog keys aligned; do not silently let the two copies drift.

## Localize a remaining English surface

1. Reuse an existing catalog key when its meaning truly matches.
2. If a new key is necessary, discuss its English meaning and placeholder shape
   in an issue before adding it to every language map.
3. Route visible text, errors, empty states, Toasts, and TalkBack descriptions
   through `AppStringCatalog`; accessibility copy is part of the translation.
4. Use named placeholders instead of English-specific string concatenation.
5. Keep URLs and other non-language configuration outside translated copy.
6. Add or update a focused test for fallback, placeholders, RTL, and the changed
   screen.

Do not combine a large UI rewrite with string extraction. A focused pull request
is easier for language reviewers to validate.

## Verify the contribution

Run:

```bash
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```

The catalog tests must confirm that all 18 language maps contain the complete
English key set. Then select the contributed language inside Account and inspect:

- every changed screen, dialog, empty/loading/error state, and TalkBack label
- placeholders with zero, one, and multiple values where applicable
- the largest Android font and display sizes without clipping
- light and dark themes
- narrow, landscape, and unfolded/foldable layouts when available
- logical order and alignment in Arabic, Farsi, or Urdu

Include screenshots with private information removed, plus the device, Android
version, locale, and human reviewer in the pull request.

## Opportunity content is separate

App-interface strings belong here. Translated opportunity titles, summaries,
and descriptions belong in the
[GTA FREE STEM opportunity-data repository](https://github.com/rupayon123/gta-free-stem-opportunities).
Do not hand-edit the bundled `app/src/main/res/raw/opportunities.json` snapshot as
a substitute for updating its source data.
