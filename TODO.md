# TODO

This file is the active backlog and quality checklist. Completed release history belongs in `CHANGELOG.md`; durable architecture and behavior details belong in `ARCHITECTURE.md`, `README.md`, `BUILD.md`, or `USER_GUIDE.md`.

## Current Status

- [x] Gallery, Google Drive, and Dropbox sources are implemented.
- [x] Runtime slideshow settings are wired into DreamService, SlideshowManager, and SlideshowView.
- [x] Settings screens auto-save and refresh summaries on resume.
- [x] Google Drive supports multiple accounts with account-scoped folder selection and cache state.
- [x] Cloud and gallery cache/count paths have been hardened for recursive loading and content-type filters.
- [ ] Revalidate `./gradlew assembleDebug` after the AGP 9.2.1 bump.

## Open Backlog

### New Sources

- [ ] Google Photos integration.
- [ ] OneDrive integration.
- [ ] Local network source support through SMB/WebDAV or similar.

### Quality

- [ ] Unit tests for repositories.
- [ ] Instrumentation tests for folder browser flows.
- [ ] Re-run `:app:assembleDebug` after the AGP 9.2.1 bump and confirm the app still runs.
- [ ] Lint checks passing.
- [ ] Memory leak detection with LeakCanary or equivalent tooling.

### Documentation

- [ ] Add `GOOGLE_CLOUD_SETUP.md` if OAuth setup grows beyond the summary in `BUILD.md`.
- [ ] Add `SETTINGS.md` only if the settings reference becomes too large for `ARCHITECTURE.md`.
- [ ] Add `RELEASE_NOTES.md` only when preparing a packaged release.

## Completed Highlights

- [x] Gallery source with MediaStore folder browsing and recursive Android 10+ counts.
- [x] Google Drive source with OAuth2, persistent auth, multi-account support, and account-scoped cache behavior.
- [x] Dropbox source with recursive listing, thumbnail caching, and local media cache paths.
- [x] Folder browser navigation, checkbox cascade, immediate auto-save, pull-to-refresh, thumbnails, and selected-count summaries.
- [x] Android DreamService registration, activation card detection, and preview activity.
- [x] Full slideshow rendering with Coil, transitions, deduplication, preloading, and network-aware cloud loading.
- [x] Video playback with Media3/ExoPlayer, audio modes, min/max duration filtering, and auto-advance.
- [x] Date, clock, weather, and photo-info overlays.
- [x] Schedule, timer, battery-saver, orientation, background, keep-screen-on, and touch-exit runtime behavior.
- [x] Build migration to AGP 9.2.1 and Gradle 9.4.1.
