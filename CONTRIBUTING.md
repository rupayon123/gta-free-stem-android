# Contributing to GTA FREE STEM for Android

Thank you for helping make free STEM opportunities easier to find. Code,
accessibility testing, documentation, design feedback, and human-reviewed
translations are all welcome.

By participating, you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
Unless a contribution says otherwise, it is submitted under the repository's
[MIT License](LICENSE).

## Before you start

- Search existing issues before opening a new one.
- Use the translation issue form for language help that does not require code.
- Discuss large features or architecture changes in an issue before investing
  significant work.
- Never include passwords, signing keys, precise personal locations, children's
  information, or other sensitive data in an issue, pull request, screenshot,
  fixture, or log.
- Report vulnerabilities through the private process in [SECURITY.md](SECURITY.md),
  not in a public issue.

Opportunity listings and their translated titles or summaries come from the
[public opportunity-data repository](https://github.com/rupayon123/gta-free-stem-opportunities).
This repository is for the Android app itself.

## Set up the project

You need Git, Java 17, Android Studio, and Android SDK Platform 36. The minimum
supported Android version is Android 8.0 (API 26). Use the checked-in Gradle
wrapper; a global Gradle installation is not required.

1. Fork and clone the repository.
2. Open it in Android Studio and allow the IDE to create `local.properties`, or
   point `sdk.dir` at your Android SDK.
3. Confirm the toolchain and run the core checks:

   ```bash
   ./gradlew --version
   ./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
   ```

4. For a UI or navigation change, also run the instrumented test on an emulator
   or physical test device:

   ```bash
   ./gradlew --no-daemon connectedDebugAndroidTest
   ```

Release signing credentials are not needed for normal contributions. Do not
create or commit `keystore.properties`, a keystore, or Play Console credentials.

## Choose a focused change

- **Translations:** follow [the localization guide](docs/LOCALIZATION.md).
- **Accessibility:** test TalkBack, large text, light and dark themes, and
  touch-target clarity. Include the device and Android version in your report.
- **Foldables and layouts:** test both compact and expanded layouts when
  practical, and describe posture, orientation, and navigation mode.
- **App behaviour:** add or update focused unit tests for data, search,
  validation, or repository logic.
- **Documentation:** keep commands and claims tied to behaviour you verified.

Keep pull requests small enough to review. Avoid unrelated formatting or
dependency changes.

## Pull request checklist

- Explain the user-facing problem and the chosen solution.
- Link the relevant issue when one exists.
- Include screenshots or a short recording for visible changes, with personal
  information removed.
- State exactly which devices, Android versions, navigation modes, and commands
  you tested.
- Add or update tests in proportion to the change.
- Confirm that no credentials, generated build output, or personal data were
  added.
- For translations, name the language reviewer and complete the localization
  checklist.

A maintainer may ask for a narrower change or additional evidence before
merging. Submission does not guarantee inclusion in a release.
