# Approved portfolio application

The owner approved the third GTA visual portfolio with the original rounded Nunito typography restored. Patrick Hand was explicitly rejected and is not bundled.

The content palette uses cool white and blue for daylight and ink-blue surfaces with pale teal actions at night. The original launch palette, logo and loader remain unchanged. Typography uses the exact approved Nunito variable font, with explicit weight-axis values because its file default is ExtraLight. Nunito is distributed under the bundled SIL Open Font License in `app/src/main/res/raw/nunito_ofl.txt`.

The 76 transparent PNG resources are raster exports of original project SVG drawings: nineteen objects, two appearances, and separate illustration/compact variants. Their total size is 353,222 bytes. The book screenshots used during design review are not included in the application. No external illustration or icon library was copied.

All five navigation routes use approved compact artwork. The three additional roles—graduation cap/book, support headset and profile portrait—were visually reviewed before integration. All five navigation destinations and their existing features remain available. Existing system-aware Compose animation uses a restrained 160ms selected-icon response. Runtime verification must include system animation scale zero.

Required verification before tester distribution: compile, unit suite, lint, old/current Android icon-decoding test, actual light/dark screen captures, large text and RTL, rapid tab switching and Back/deep links, search/filter/clear, save/unsave with relaunch persistence, saved-search flows, offline refresh, provider links and map exits. Prototype captures and source inspection do not replace phone verification. Release version, signing, tester delivery and installation are coordinated separately.
