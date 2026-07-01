# TODO

This file is the active backlog and quality checklist. Completed release history belongs in `CHANGELOG.md`; durable architecture and behavior details belong in `ARCHITECTURE.md`, `README.md`, `BUILD.md`, or `USER_GUIDE.md`.

## Open Backlog

### New Sources

- [ ] Google Photos integration (Note: Supports Device Flow natively for Android TV).
- [x] OneDrive integration (Note: Implement hybrid auth. MSAL for mobile, Device Flow for Android TV).
- [ ] Local network source support through SMB/WebDAV or similar.

### Device Compatibility
- [ ] Disable/Grey out Dropbox on Android TV. Dropbox does not support Device Authorization Grant and requires a full browser, which TVs lack.
- [ ] Revisit Google Drive Android TV support if Google adds Drive scope support for Device Authorization Grant.

### Quality

- [ ] Revalidate `./gradlew assembleDebug` after the AGP 9.2.1 bump and confirm the app still runs.
- [ ] Add enforceable style/static-analysis tooling such as ktlint, detekt, or Spotless.
- [ ] Unit tests for repositories.
- [ ] Instrumentation tests for folder browser flows.
- [ ] Lint checks passing.
- [ ] Memory leak detection with LeakCanary or equivalent tooling.

### Documentation

- [ ] Add `GOOGLE_CLOUD_SETUP.md` if OAuth setup grows beyond the summary in `BUILD.md`.
- [ ] Add `SETTINGS.md` only if the settings reference becomes too large for `ARCHITECTURE.md`.
- [ ] Add `RELEASE_NOTES.md` only when preparing a packaged release.
