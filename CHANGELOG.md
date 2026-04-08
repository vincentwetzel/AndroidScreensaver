# Changelog

## 1.8.0 (Current)

### Added
- **Gallery source** — Browse and select device photo folders via MediaStore API
- **Folder navigation** — Click into subfolders in the folder browser
- **Caching** — Folder lists cached in repository to avoid re-fetching on Activity recreation
- **Persistent Google Drive auth** — Auto-sign-in on app relaunch if previously authenticated
- **Activation card** — Top-of-screen card prompts user to set app as screensaver; auto-hides when active
- **TEST button** — Toolbar button to instantly launch screensaver for testing (no need to wait for screen timeout)
- **No sources warning** — Informative message displayed when screensaver is launched without configured sources, guiding users to add photos
- **Full photo slideshow** — SlideshowView renders photos from Gallery/Google Drive with crossfade transitions
- **SlideshowView** — Reusable custom view that handles photo loading (via Coil), auto-advance, transitions, and lifecycle management
- **ScreensaverPreviewActivity** — Activity-based preview that mirrors DreamService for testing

### Fixed
- Google Sign-In `DEVELOPER_ERROR` (status 10) caused by incorrect `requestIdToken()` usage
- `FolderBrowserActivity` ActionBar theme conflict (crash on launch)
- Google Drive API calls blocking main thread (wrapped in `Dispatchers.IO`)
- Checkbox two-tap issue in folder list (switched to `setOnClickListener`)
- Restored DreamService `BIND_DREAM_SERVICE` permission and `DEFAULT` category so screensaver selection persists instead of falling back to Colors
- Empty folder list when returning to Gallery after source re-enable
- Activation card now correctly detects when app is set as active screensaver (multi-key Samsung/OneUI settings detection + fallback to `screensaver_enabled`)
- **Gallery and Google Drive folder selections now persist** - Fixed 5 bugs:
  - Added `setSelectedFolders()` method to SettingsManager to save folder IDs to DataStore
  - Fixed `getSelectedFolders()` to return actual PhotoFolder objects instead of empty list
  - MainActivity now saves folder selection results when user taps Save
  - GalleryFolderBrowserActivity now restores checkbox state from saved preferences
  - FolderBrowserActivity now restores checkbox state from saved preferences (Google Drive)
- **Source toggle switches now restore saved state on app launch** — toggles reflect persisted settings instead of defaulting to "off"

### Changed
- Removed arrow button from folder list; folder name clicks now navigate into folder
- Simplified DreamService manifest (removed `category` and `meta-data`)
- "Preview Screensaver" button replaced with activation card

### Known Issues
- Google Drive requires Web Client ID for `requestIdToken()` (currently disabled)
- SlideshowManager.preloadPhoto() only preloads Google Drive photos (Gallery preloading needs wiring)
- getSelectedFolders() reconstructs PhotoFolder with name=id (human-readable names not persisted)
