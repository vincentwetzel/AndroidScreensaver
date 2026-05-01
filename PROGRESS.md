# Progress

## Current Status

All settings are fully wired and audited. The app is stable with all features properly implemented. Recent work focused on fixing video playback custom volume, simplifying video settings UI, and ensuring all settings summaries refresh correctly.

## Recent Fixes (Latest Session)
- **AGP 9.2 build validation**: Root Android Gradle Plugin was bumped to 9.2.0 and the app was confirmed to still build and run successfully with the Gradle 9.4.1 wrapper.
- **Debug Kotlin compile cleanup**: Repaired malformed imports, the missing `SlideshowView` function brace, Dropbox SDK 5.4 thumbnail/search usage, the missing `dream.SourceType` alias, and the photo info background enum mismatch used by overlay styling.
- **Slideshow remote cache routing**: `SlideshowView` now asks `SlideshowManager` to download cacheable remote photos, keeping repository-specific cache calls out of the view layer.
- **KSP repository map binding fix**: `RepositoryModule` now lets Hilt construct the concrete photo repositories and explicitly assembles the `SourceType -> PhotoRepository` map for `SlideshowManager`, avoiding the KSP map-key processing failure during debug builds.
- **Just-in-Time Google Drive Photo Loading**: Removed the front-loaded download bottleneck. Slideshow now starts instantly by utilizing remote URLs and a custom OkHttp Interceptor in Coil that injects the appropriate multi-account OAuth tokens.
- **SlideshowView Cache Fix**: Repaired a performance regression where a new `ImageLoader` was created per photo. The app now properly utilizes the Coil singleton `imageLoader` to respect disk and memory caching.

## Previous Fixes
### Settings Summary Refresh
- **Main Settings screen now refreshes on resume** — Added `onResume()` to `SettingsFragment` that calls `syncSettingsFromDataStore()` and `updateNavigationPreferenceSummaries()` so all preference summaries reflect current values when returning from sub-screens.
- **Navigation preference summaries show state** — Video Playback, Schedule, Photo Info, and Decoration preferences now display their current values (e.g., "Muted", "Custom volume (75%)", "Enabled at 8:00 PM", "Date, Clock").

### Video Playback
- **Custom volume now uses AudioManager** — "Use Custom Volume" now sets the actual device system volume via `AudioManager.setStreamVolume()` instead of just applying a relative player volume multiplier. Original system volume is saved and restored when video ends or screensaver stops.
- **Min Video Duration setting added** — New spinner with options: No minimum, 5s, 10s, 15s, 30s, 1 minute. Videos shorter than the threshold are automatically skipped during slideshow.
- **Removed autoplay and loop toggles** — Videos now always auto-play and short videos always loop. These were removed from the UI.
- **Removed display mode options** — "Play full duration", "Play fixed time", and "Extract still frame" removed. Videos always play full duration.
- **Volume slider save improved** — Changed to `OnSliderTouchListener` to avoid double-saves.

### Other Settings Fixes
- **Decoration settings sliders fixed** — All 9 opacity sliders (Date, Clock, Weather tabs) now initialize from saved config instead of showing XML defaults.
- **Schedule tab switching fixed** — Autostop tab now correctly loads its own settings instead of showing Autostart values.
- **Folder browser summary fixed** — Both Gallery and Google Drive folder browsers now use `adapter.getPhotoCount()` (selected folders only) instead of `viewModel.getPhotoCount()` (all loaded folders) for the summary text.
- **Content Filter renamed to Content Type** — Display title changed for clarity.

## Build Status

- Debug build: confirmed successful after the Android Gradle Plugin 9.2.0 bump; app was also run successfully
- No known source-level compilation errors after the import, brace, SourceType alias, Dropbox SDK, photo info background mapping, and SlideshowManager routing fixes
- Only pre-existing warnings (deprecated APIs, unused variables in repos)

## Completed (Latest Session)
- ✅ **Date/clock/weather overlay rendering in SlideshowView**: Enabled by default and fully implemented.
- ✅ **Photo info overlay during slideshow**: Enabled by default and fully implemented.
- ✅ **Schedule-based source enabling (autostart/autostop schedules)**: Enabled by default and fully implemented.
- ✅ **Battery-saver aware slideshow (pause on low battery)**: Enabled by default and fully implemented.

## Next Priorities
(None)
