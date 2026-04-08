# TODO

## Critical
- [x] Wire full slideshow logic back into DreamService (now uses SlideshowView)
- [ ] Add DreamService `onDreamFinished()` and `onDetachedFromWindow()` cleanup

## Photo Sources
- [ ] Dropbox integration
- [ ] Google Photos integration
- [ ] OneDrive integration
- [ ] Local network (SMB/WebDAV) support

## Caching & Performance
- [ ] Local photo cache for offline use
- [x] Photo count caching with TTL (5 min) for both Gallery and Google Drive
- [x] TTL-based folder cache (60s) to detect new/removed folders
- [ ] Photo preloading for next photo (SlideshowManager.preloadPhoto for Gallery)
- [x] Thumbnail caching for folder browser (Coil handles this automatically)

## UI/UX
- [ ] Better error messages in folder browser
- [ ] Folder thumbnail previews in browser
- [ ] Source status indicators (connected, syncing, error)
- [ ] Photo count display per source
- [x] TEST button to launch screensaver without waiting for timeout
- [x] Pull-to-refresh in folder browser for manual refresh
- [x] Source toggle switches restore saved state on launch
- [x] Account email displayed on source status and sign-in toast

## Features
- [x] Video playback in slideshow (ExoPlayer with auto-advance on completion)
- [ ] Date/clock overlay on photos
- [ ] Weather overlay
- [ ] Photo info overlay (filename, date, source)
- [ ] Schedule-based source enabling
- [ ] Battery-saver aware slideshow (pause on low battery)

## Quality
- [ ] Unit tests for repositories
- [ ] Instrumentation tests for folder browser
- [ ] Lint checks passing
- [ ] Memory leak detection (LeakCanary)

---

## Progress Status

### Completed
- ✅ Gallery source with MediaStore API, folder browsing, caching
- ✅ Google Drive source with OAuth2, folder browsing, persistent auth, caching
- ✅ Google Drive photos now display in slideshow (downloaded with OAuth headers to local cache)
- ✅ Google Drive auth persists across app launches (checkExistingSignIn called in repository init)
- ✅ Folder browser with subfolder navigation and checkbox selection (state persists)
- ✅ Folder selection persistence fixed (setSelectedFolders/saveSelectedFolders added to SettingsManager)
- ✅ Activation card that auto-hides when app is set as screensaver
- ✅ DreamService registered and selectable in system settings
- ✅ Hilt DI for all repositories
- ✅ DataStore-backed settings persistence
- ✅ Material Design 3 UI for phones, tablets, and TV devices
- ✅ All API calls on background threads (Dispatchers.IO)
- ✅ Full photo slideshow with crossfade transitions (SlideshowView)
- ✅ TEST button to instantly preview screensaver
- ✅ No sources configured warning screen
- ✅ Coil image loading for Gallery content:// URIs

### In Progress
- 🔄 DreamService `onDreamFinished()` cleanup (lifecycle management)

### Planned
- ⏳ Dropbox, Google Photos, OneDrive, local network sources
- ⏳ Photo preloading and caching (SlideshowManager.preloadPhoto for Gallery)
- ⏳ Video playback, weather/clock decorations
- ⏳ Schedule-based source enabling
