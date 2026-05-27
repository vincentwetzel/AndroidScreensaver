# Changelog

## 1.11.0 (In Progress)

### Build System
- **Updated Android Gradle Plugin to 9.2.1** - Bumped the root `com.android.application` plugin from AGP 9.2.0 to 9.2.1.

- **Updated Android Gradle Plugin to 9.2.0** - Bumped the root `com.android.application` plugin to AGP 9.2.0 and validated that the app still builds and runs successfully with the existing Gradle 9.4.1 wrapper.

- **Removed deprecated Gradle properties** - Cleaned up 7 deprecated options from `gradle.properties`:
  - `android.usesSdkInManifest.disallowed`
  - `android.sdk.defaultTargetSdkToCompileSdkIfUnset`
  - `android.enableAppCompileTimeRClass`
  - `android.builtInKotlin`
  - `android.newDsl`
  - `android.r8.optimizedResourceShrinking`
  - `android.defaults.buildfeatures.resvalues`

  These are now handled by AGP 9.2.0 defaults. Retained only essential properties: `android.nonTransitiveRClass` and `android.enableJetifier`.

- **Migrated to AGP 9.1 built-in Kotlin** — Removed explicit `org.jetbrains.kotlin.android` plugin from root and app `build.gradle.kts` files. AGP 9.1+ includes Kotlin as a built-in feature, eliminating plugin conflicts. Added `android.disallowKotlinSourceSets=false` to allow KSP to generate Kotlin sources. Replaced deprecated `kotlinOptions` with modern `kotlin` DSL block.

- **Enabled BuildConfig generation** — Added `buildConfig = true` to buildFeatures for custom debug/release configuration fields.

### Added
- **Debug logcat mirroring** — Automatic logcat output mirroring to file during development. All logcat output is written to `/sdcard/Android/data/com.vincentwetzel.androidscreensaver.debug/files/debug-logcat.txt` for easier debugging. Enabled only in debug builds via `BuildConfig.DEBUG_LOGCAT_MIRROR` (disabled in release builds).
- **Multiple accounts per source type** — Google Drive now supports signing in to multiple accounts simultaneously (e.g., personal + work). Each account gets its own card on the main screen with independent folder selection, enable/disable toggle, and photo count. The slideshow combines photos from all enabled accounts.
- **GoogleAccountManager** — New utility class that replaces the singleton auth state in GoogleDriveRepository. Manages per-account Drive services, OAuth tokens, and sign-out. All authenticated accounts are tracked in-memory with per-account Drive API clients.
- **Per-account folder browsing** — FolderBrowserActivity and GoogleDriveViewModel now route all API calls to the correct account via accountId passed through intent extras.
- **Per-account caching** — GoogleDrivePhotoRepository now uses per-account cache maps for folders and photo counts, preventing cross-account cache collisions. Cache keys include accountId prefix.
- **Font size sliders for decorations** — Date, clock, and weather widgets now have continuous font size sliders (8sp–72sp) in the Customize Overlays settings. Replaces the previous Small/Medium/Large dropdown with precise control. Label shows current value (e.g., "Font Size (24sp)").
- **Remove account option** — Source cards on the main screen now have a "more" menu with a "Remove account" option. This allows users to remove individual accounts (e.g., a second Google Drive account) after a confirmation dialog.

### Fixed
- **Repository cache thread safety and refresh behavior** - Gallery, Google Drive, Dropbox, and slideshow preload caches now use concurrent maps where needed, and `syncPhotos()` clears stale folder/photo count caches before reporting success.
- **Recursive media counts and folder exclusions** - Gallery, Google Drive, and Dropbox count logic now matches the recursive photo loading behavior more closely, including content-type filters and deselected subfolder handling.
- **Google Drive media loading metadata** - Drive photo metadata now preserves thumbnails, account-scoped download URLs, cached `file://` paths, video dimensions, and file extensions for media filtering and local cache downloads.
- **Dropbox recursive listing and caching** - Dropbox listing now uses recursive API traversal, handles paginated folder search, skips excluded folders, avoids cache filename collisions by using full safe paths, writes thumbnails through temp files, and caches downloaded files with stable `file://` URIs.
- **Gallery MediaStore URI and counts** - Gallery now resolves image/video URLs through metadata, returns correct thumbnail URIs, treats MediaStore buckets as a flat root list, and uses `RELATIVE_PATH LIKE` for recursive Android 10+ counts.
- **DreamService lifecycle safety** - Broadcast receivers are registered via `ContextCompat.registerReceiver()`, unregister cleanup is guarded, timeout callbacks are fully cleared, and battery-saver pause is applied immediately when the screensaver starts.
- **Slideshow filtering and loading reliability** - Media filtering now checks photo titles as well as URIs, deduplication includes source/account IDs, remote preload downloads through repository cache paths, folder loads are chunked to reduce API/memory spikes, and Ethernet satisfies Wi-Fi-only network checks.
- **Weather API resilience** - Weather parsing now uses optional JSON fields with defaults, and the connectivity test calls a valid Open-Meteo forecast endpoint.
- **KSP repository map binding failure** - Replaced Hilt `@IntoMap` multibindings for `PhotoRepository` with explicit repository map assembly in `RepositoryModule`. This fixes the KSP error `@Provides methods of type map must declare a map key` during `:app:kspDebugKotlin`.
- **Add Source dialog filtering** - Automatically hides singleton sources (like Gallery) from the dropdown if they have already been added. This prevents accidental duplicate additions and correctly surfaces Google Drive as the primary option for adding multiple accounts.
- **Debug Kotlin compile failures** - Fixed malformed Kotlin imports in DreamService, Schedule settings, and SlideshowView; restored the missing SlideshowView brace; aligned Dropbox SDK calls with the 5.4 API; mapped photo info backgrounds to decoration backgrounds for overlay styling; and routed remote photo cache downloads through SlideshowManager instead of direct view-to-repository access.

## 1.10.0

### Added
- **Screensaver timeout** — Automatically exit the screensaver after a specified duration. Preset options: 5min, 15min, 30min, 45min, 1hr, 1.5hr, 2hr. Custom option allows entering manual minutes (1-480) or hours (1-24). Requires "Start by Timer" to be enabled.

## 1.9.2

### Fixed
- **RADIAL transition not working** — Fixed RADIAL transition to use `ViewAnimationUtils.createCircularReveal()` instead of simple scale animation. The transition now creates a true circular reveal effect that expands from the center of the image, properly revealing the new photo with a radial wipe effect.

## 1.9.1

### Fixed
- **RADIAL transition black screen** — Fixed black screen during RADIAL transition by adding alpha fade-in animation. The target view now starts at alpha 0 and fades in while scaling from 0 to 1, preventing brief black screen artifacts.
- **Loading overlay not hidden on error** — Fixed black screen when photos fail to load or no photos are found. The loading overlay is now properly hidden when an error occurs or the photo list is empty, allowing error messages to be visible.
- **Gallery photos not loading (black screen)** — Added photo permission check before loading Gallery photos. Without the check, Coil silently failed to load `content://` URIs when permissions weren't granted, showing a permanent black screen. Now displays a clear error message: "Gallery photo access requires permission. Please grant photo permissions in Settings > Apps > Android Screensaver > Permissions."

## 1.9.0

### Added
- **Min Video Duration setting** — Exclude short videos from the slideshow. Options: No minimum, 5s, 10s, 15s, 30s, 1 minute. Videos shorter than the threshold are automatically skipped.
- **Custom volume now uses absolute system volume** — "Use Custom Volume" now sets the actual device volume via `AudioManager` instead of just applying a player volume multiplier. Original system volume is restored when video ends or screensaver stops.
- **Diagnostic logging for video settings** — Added logcat output at save, config load, and playback to trace video audio mode and custom volume values.

### Removed
- **Auto-play / Loop toggles** — "Auto-play videos" and "Loop short videos" options removed from Video Playback Settings. Videos now always auto-play and short videos always loop.
- **Video display mode options** — "Play full duration", "Play fixed time", and "Extract still frame" options removed. Videos always play their full duration (up to Max Video Duration cap).

### Fixed
- **Settings summaries not refreshing** — Main Settings screen now refreshes all preference summaries from DataStore when returning from sub-screens (added `onResume()`). Navigation preferences (Video Playback, Schedule, Photo Info, Decorations) now show their current state (e.g., "Muted", "Custom volume (75%)", "Enabled at 8:00 PM").
- **Decoration settings sliders not initialized** — All 9 opacity sliders across Date, Clock, and Weather tabs now load their saved values instead of showing XML defaults.
- **Schedule tab switching not reloading data** — Switching between Autostart and Autostop tabs now loads the correct schedule's settings instead of showing the previously loaded values.
- **Folder browser summary summing unselected folders** — Both Gallery and Google Drive folder browsers now correctly sum only selected folders' counts (respecting the content filter) in the summary text at the bottom.
- **Volume slider double-save race condition** — Changed from `OnChangeListener` to `OnSliderTouchListener` to prevent redundant saves when the radio button programmatically changes the slider value.
- **Rename**: "Content Filter" setting renamed to "Content Type" for clarity.
- **Date, Clock, and Weather decoration overlays rendered** — Decorations now display as overlay TextViews on top of the slideshow. Clock updates every second. Date, clock, and weather respect configured position, font size, opacity, and background style.
- **DecorationSettingsActivity save fix** — Removed `if (config.enabled)` checks that caused decorations to always save as `null` since `enabled` defaulted to `false`. All three decorations now always save with `enabled = true`.

## 1.8.0 (Current)

### Changed
- **Just-in-Time Photo Loading** — Eliminated the severe bottleneck where the app would synchronously download all photos from a Google Drive folder before starting the slideshow. The app now fetches lightweight metadata instantly and lets Coil download the photos just-in-time via remote URLs.
- **Multi-Account OAuth Interceptor** — Configured a global Coil `ImageLoader` with an OkHttp interceptor that dynamically injects the correct Google Drive OAuth Bearer token based on an `accountId` query parameter attached to the image URI.
- **SlideshowView Caching Fix** — Fixed a major performance bug in `SlideshowView` where a new `ImageLoader` was instantiated for every photo, breaking memory caching and connection pooling. It now correctly uses the singleton `context.imageLoader`.

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
- **Pull-to-refresh** — Swipe down on folder browser to force a manual refresh from remote source (Google Drive, Gallery)
- **Account email display** — Remote source status shows "Signed in as [email]" instead of generic "Authenticated". Toast notification on sign-in shows "Successfully signed in as [email]". Applies to all sources (Google Drive, and future: Dropbox, OneDrive, Google Photos)
- **Video playback support** — Videos now play with ExoPlayer (Media3) in the slideshow, auto-advancing when finished. Content filter "Videos Only" now works correctly

### Fixed
- Google Sign-In `DEVELOPER_ERROR` (status 10) caused by incorrect `requestIdToken()` usage
- **Content filter now correctly restricts media types** — Fixed 2 bugs:
  - `arrays.xml` content filter values were misaligned with entries (selecting "Images Only" actually set "Images AND Videos" and vice versa)
  - `SlideshowManager` config was never reloaded before `loadPhotos()`, so stale filter settings from singleton init were used
- **Settings wiring — 8 bugs fixed:**
  - `screen_rotation` sync: `SYSTEM_DEFAULT.name.lowercase()` produced "system_default" but XML value is "system"; now maps explicitly
  - `sync_interval` sync/readback: used `.name.lowercase()` which gave "daily" but XML values are minute numbers ("30", "60", "1440"); both save and sync now use the minute-value mapping
  - `exit_trigger` sync: `REMOTE_BUTTON.name.lowercase()` produced "remote_button" but XML value is "remote"; now maps explicitly
  - `match_orientation`, `keep_screen_on`, `wifi_only` switches: had no `OnPreferenceChangeListener`, so toggling them never persisted to DataStore
  - `decoration_date/clock/weather` switches: saved as raw Android preferences but never read into `SlideshowConfig`; now persist to DataStore and reconstruct on config load
  - `photo_info_enabled`: saved to `SlideshowConfig` but `saveSlideshowConfig()` never persisted `photoInfoConfig` fields and `getSlideshowConfig()` never read them back
  - `cache_limit` custom value: saved `cacheSizeLimitMB` but left `usePresetLimit = true`, so the custom value was ignored at read time; now sets `usePresetLimit = false`
  - `cache_limit` persistence: `cacheSizeLimitMB` and `usePresetLimit` were never written to or read from DataStore; now fully persisted
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
- **NavigateBack no longer forces unnecessary refresh** — changing `forceRefresh = true` to `false` so cache is respected
- **Folder photo count caching** — `getFolderPhotoCount()` was hitting the API every time with no cache; now uses TTL-based cache
- **Pull-to-refresh spinner no longer spins forever** — fixed by tying `isRefreshing` to `isLoading` flow instead of `folders` flow
- **Cache not detecting new/removed folders** — switched from permanent cache to TTL-based cache (60s for folders, 5min for photo counts) so changes are automatically detected
- **Google Drive photos now display in slideshow** — photos were showing black screen because Coil couldn't load Drive URLs without OAuth headers; now downloads photos to local cache with auth headers and loads via file:// URI
- **SlideshowView error logging** — added Coil load listeners to diagnose image loading failures
- **Google Drive auth now restores on app launch** — `checkExistingSignIn()` was never called at startup, so `isAuthenticated` stayed false and slideshow skipped Drive photos; now auto-checks in repository `init` block
- **`decoration_customize` had no handler** — Created `DecorationSettingsActivity` with tabbed UI for date/clock/weather customization (position, format, font size, background, animation, opacity, pulse)
- **Gallery photo preloading not working** — `SlideshowManager.preloadPhoto()` now routes correctly for Gallery (content:// URIs), Google Drive cached (file:// URIs), and remote URLs

### Enhancements
- **Photo count on source cards** — Main screen cards now show "X photos available" under the status text, fetched from repositories when cards are refreshed
- **Source status indicators** — Colored dot indicators (green = connected, orange = syncing, red = error) on source cards for at-a-glance status
- **Better folder browser error messages** — Created `FolderError` sealed class with typed errors (NetworkError, AuthError, PermissionError, ApiError, EmptyError, UnknownError) and user-friendly messages instead of raw exception text
- **Folder thumbnail previews** — Folder items now show the first photo as a thumbnail using Coil (Gallery photos via content:// URIs, Google Drive falls back to folder icon). Folder item layout updated with 48dp rounded thumbnail card
- **Background folder pre-fetching** — Root folders are now fetched on a background thread (`Dispatchers.IO`) when a source is enabled and authenticated. Both `GoogleDrivePhotoRepository` and `GalleryPhotoRepository` have `prefetchRootFolders()` that populate the folder cache before the user clicks into the folder browser, making the initial load instant. Triggered from `MainActivity.refreshSourceCards()` and the Google Drive auth callback.

### Changed
- **All settings now auto-save** — Removed manual Save buttons from Video Playback, Photo Info, Schedule, and Decoration settings screens. All toggles, sliders, spinners, and radio buttons persist to DataStore immediately on change, consistent with the rest of the app.
- **Content filter-aware media counts and labels** — Folder counts and labels now respect the content filter setting. When filter is "Photos Only", shows "X photos"; when "Videos Only", shows "X videos"; when "Both", shows "X items". Applies to folder browser list items, folder browser summary text, and main screen source cards. Added `getFilteredFolderMediaCount()` to both repositories to query counts by media type.
- **Folder browser navigation back stack** — Replaced the hardcoded "go to root" back button with a proper navigation stack. Each folder visit is pushed onto the stack; back button pops and returns to the previous folder. When stack is empty, finishes Activity to return to main menu. Toolbar title shows the current folder name.
- **Photo deduplication in slideshow** — `SlideshowManager.loadPhotos()` now deduplicates photos by ID using `distinctBy { it.id }` to prevent double-counting when a parent folder is cascade-selected alongside its subfolders.
- **Deselected folder persistence fixed** — Unchecking a subfolder now properly removes it from `selectedFolderIds` AND adds it to `deselectedFolderIds`. Previously the folder stayed in the selected set, causing the deselection to be ignored on the next bind.
- **Gallery content filter counts fixed** — `getFilteredFolderMediaCount()` now queries the correct MediaStore table (`Images.Media` for photos, `Videos.Media` for videos, `Files` for both) instead of querying `Files` with a `media_type` filter which was unreliable across devices.
- **Google Drive count pagination fixed** — `getFilteredFolderMediaCount()` and `getFolderPhotoCount()` now paginate through all results with `setPageSize(1000)` and accumulate the total. Previously `setPageSize(1)` capped counts at 1 file.
- **Removed "Include subfolders" toggle** — Subfolder inclusion is now always-on by default. The toggle switch has been removed from both folder browser activities (Google Drive and Gallery), the `includeSubfolders` state and `setIncludeSubfolders()` methods have been removed from both ViewModels, and the `includeSubfolders` fields have been removed from `SourceConfig` and `SelectedFolder` data models.
- **Fixed subfolder photo inclusion** — Checking a folder now correctly includes photos from all subfolders recursively:
  - **Google Drive**: `listPhotos()` now uses `collectPhotosFromFolder()` which recursively traverses the folder tree, fetching photos from each subfolder level via the Drive API
  - **Gallery (Android 10+)**: Uses `MediaStore.RELATIVE_PATH` with a `LIKE 'path%'` query to match photos from the selected folder and all subfolders. Falls back to exact bucket match on Android < 10
- **Subfolder checkbox cascade** — Checking a folder's checkbox immediately checks all its descendant subfolders recursively. Unchecking a folder immediately unchecks all descendants. This happens in real-time by fetching the full subfolder tree from the Drive API and applying the state to the adapter's selected/deselected sets. The user can navigate into any subfolder later and override its state individually. Both selections and deselections are persisted to DataStore.
- Removed arrow button from folder list; folder name clicks now navigate into folder
- **Folder selection now auto-saves** — checking/unchecking a folder immediately saves the selection. No more Save/Cancel confirmation buttons.
- Simplified DreamService manifest (removed `category` and `meta-data`)
- "Preview Screensaver" button replaced with activation card
- **Folder browser result handling simplified** — MainActivity and SettingsFragment no longer process folder selection results (auto-save happens in the browser)
- Default content filter changed from "Images and Videos" to "Images Only"
- Folder and photo count caches use TTL expiration instead of permanent caching

### Runtime — Settings now consumed at slideshow time
- **Background color** — `SlideshowView` now applies `config.backgroundColor` instead of hardcoding black
- **Keep screen on** — DreamService now sets `FLAG_KEEP_SCREEN_ON` when enabled
- **Screen rotation** — DreamService now applies `config.screenOrientation` via WindowManager
- **Exit trigger (touch)** — DreamService now uses `GestureDetector` to catch single-tap and call `finish()` when exit trigger is set to TOUCH
- **Wi-Fi only** — `SlideshowManager.loadPhotos()` now checks `ConnectivityManager` and skips Google Drive fetches when on cellular and `wifi_only` is enabled
- **Match orientation** — `SlideshowView` now adjusts `scaleType` to `FIT_CENTER` when photo orientation doesn't match device orientation
- **Clear cache** — Now actually clears Coil's memory and disk cache instead of showing a no-op toast
- **Start by timer** — Switch now persists `timerConfig.enabled` to DataStore with proper readback
- **DreamService lifecycle** — `onDetachedFromWindow` now nulls out references; added proper cleanup logging
- **Video double-advance fix** — Added `isAdvancing` guard to prevent ExoPlayer end callback and auto-advance coroutine from racing

### Video playback settings — 9 settings fully wired end-to-end
- **Persistence** — All 9 video fields now have `PreferencesKey` entries and are saved/read in `SettingsManager`:
  - `videoAudioMode` (mute / system volume / custom volume)
  - `videoCustomVolume` (0-100 slider value)
  - `videoMaxDurationSeconds` (hard cap on video playback length)
  - `videoAutoPlay` (auto-start video vs require manual play)
  - `videoShowControls` (show/hide ExoPlayer playback controls)
  - `videoLoopShort` (loop short videos with `REPEAT_MODE_ONE`)
  - `videoDisplayMode` (play full / play fixed seconds / extract still)
  - `videoFixedPlaySeconds` (duration for PLAY_FIXED mode)
  - `videoStillTimestamp` (where to seek for EXTRACT_STILL mode)
- **Runtime** — `SlideshowView.showPhoto()` now applies all video config:
  - ExoPlayer volume set based on `videoAudioMode` + `videoCustomVolume`
  - `playWhenReady` respects `videoAutoPlay`
  - `repeatMode` set to `REPEAT_MODE_ONE` when `videoLoopShort` enabled
  - `useController` toggled based on `videoShowControls`
  - `videoMaxDurationSeconds` enforced via cancelable coroutine timer
  - `PLAY_FIXED` mode stops video after `videoFixedPlaySeconds`
  - `EXTRACT_STILL` mode seeks to `videoStillTimestamp` and pauses

### Known Issues
- Google Drive requires Web Client ID for `requestIdToken()` (currently disabled)
- SlideshowManager.preloadPhoto() only preloads Google Drive photos (Gallery preloading needs wiring)
- getSelectedFolders() reconstructs PhotoFolder with name=id (human-readable names not persisted)
