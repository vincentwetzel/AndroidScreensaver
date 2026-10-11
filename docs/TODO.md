# TODO

This file is the active backlog and quality checklist. Completed release history belongs in `docs/CHANGELOG.md`; durable architecture and behavior details belong in `docs/ARCHITECTURE.md`, `README.md`, `docs/BUILD.md`, or `docs/USER_GUIDE.md`.

## Open Backlog

### New Sources

- [ ] Google Photos integration (Note: Supports Device Flow natively for Android TV).
- [x] OneDrive integration (Note: Implement hybrid auth. MSAL for mobile, Device Flow for Android TV).
- [ ] Local network source support through SMB/WebDAV or similar.

### Device Compatibility
- [ ] Disable/Grey out Dropbox on Android TV. Dropbox does not support Device Authorization Grant and requires a full browser, which TVs lack.
- [ ] Revisit Google Drive Android TV support if Google adds Drive scope support for Device Authorization Grant.
- [ ] Add a release-specific MSAL config and redirect URI after the release application ID and signing certificate are defined.

- [ ] Replace or validate scheduled autostart: Android reserves DreamService binding for the system, and direct service start on the connected Android 13 phone returned `Requires permission android.permission.BIND_DREAM_SERVICE`.
### Quality

- [ ] Complete the weather overlay pipeline: SlideshowView currently renders a hard-coded 72° placeholder, WeatherRepository is not called, and the saved device-location option has no runtime permission/location provider flow. Correct the changelog claim that live conditions are displayed once this is implemented.

- [x] Add a repository-level fail-fast test harness with timestamped logs, end-of-run reports, and optional ADB smoke checks.
- [x] Add opt-in suspects-cache tracking to the test runner so failed checks persist until they pass.
- [x] Revalidate `./gradlew assembleDebug` after the AGP 9.2.1 bump and confirm the app still runs.
- [x] Add JVM coverage for model/account helpers, source authentication state, shared repository routing/cache behavior, cloud folder navigation, and slideshow media rules.
- [x] Add JVM coverage for delimiter-safe account settings serialization and shared account-manager state transitions.
- [x] Persist autostart/autostop schedules and cover schedule serialization and alarm option policies.
- [x] Match Gallery permission checks to the selected image/video filter, handle Android 14 selected-media access, invalidate caches when permission or selected-item state changes, and cover the rules with JVM tests.
- [x] Make source-root prefetch respect cache TTLs and suppress duplicate in-flight requests from screen refreshes.
- [x] Enforce the configured cloud media cache limit with bounded oldest-first cleanup.
- [ ] Add enforceable style/static-analysis tooling such as ktlint, detekt, or Spotless.
- [ ] Add provider-specific repository tests backed by fake API/MediaStore clients.
- [ ] Instrumentation tests for folder browser flows.
- [x] `lintDebug` completes successfully with no errors.
  - The latest report has 290 warnings and no errors. Exact alarm access is guarded with an idle-aware inexact fallback; the checked API call has a narrowly scoped lint suppression. The DuplicateIncludedIds findings for decoration settings are benign: date, clock, and weather are separate included bindings, and the activity accesses each label through its own binding. Locale and cleanup warnings remain for later review. The two TrustAllX509TrustManager findings are in the google-http-client dependency jar; app networking uses default OkHttp and NetHttpTransport configuration with no custom trust override.
- [ ] Memory leak detection with LeakCanary or equivalent tooling.

### Documentation

- [ ] Add `docs/GOOGLE_CLOUD_SETUP.md` if OAuth setup grows beyond the summary in `docs/BUILD.md`.
- [ ] Add `docs/SETTINGS.md` only if the settings reference becomes too large for `docs/ARCHITECTURE.md`.
- [ ] Add `docs/RELEASE_NOTES.md` only when preparing a packaged release.
