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

### Quality

- [x] Add a repository-level fail-fast test harness with timestamped logs, end-of-run reports, and optional ADB smoke checks.
- [x] Add opt-in suspects-cache tracking to the test runner so failed checks persist until they pass.
- [x] Revalidate `./gradlew assembleDebug` after the AGP 9.2.1 bump and confirm the app still runs.
- [x] Add JVM coverage for model/account helpers, source authentication state, shared repository routing/cache behavior, cloud folder navigation, and slideshow media rules.
- [x] Add JVM coverage for delimiter-safe account settings serialization and shared account-manager state transitions.
- [ ] Add enforceable style/static-analysis tooling such as ktlint, detekt, or Spotless.
- [ ] Add provider-specific repository tests backed by fake API/MediaStore clients.
- [ ] Instrumentation tests for folder browser flows.
- [ ] Lint checks passing.
- [ ] Memory leak detection with LeakCanary or equivalent tooling.

### Documentation

- [ ] Add `docs/GOOGLE_CLOUD_SETUP.md` if OAuth setup grows beyond the summary in `docs/BUILD.md`.
- [ ] Add `docs/SETTINGS.md` only if the settings reference becomes too large for `docs/ARCHITECTURE.md`.
- [ ] Add `docs/RELEASE_NOTES.md` only when preparing a packaged release.
