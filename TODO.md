# TODO

## Bug Fixes — Active Components (Highest Priority)

### Settings → Runtime Wiring (settings saved but not consumed at slideshow runtime)
- [x] **Background color**: `SlideshowView` now applies `config.backgroundColor`
- [x] **Keep screen on**: DreamService sets `FLAG_KEEP_SCREEN_ON` when enabled
- [x] **Screen rotation**: DreamService sets orientation via WindowManager attributes
- [x] **Exit trigger**: DreamService uses `GestureDetector` for touch exit
- [x] **Wi-Fi only**: `SlideshowManager.loadPhotos()` checks `ConnectivityManager` before Google Drive fetches
- [x] **Match orientation**: `SlideshowView` adjusts `scaleType` based on photo vs device orientation

### DreamService Lifecycle
- [x] **onDetachedFromWindow cleanup**: Nulls out slideshowView and gestureDetector references

### SlideshowView
- [x] **Video double-advance**: Added `isAdvancing` guard flag to prevent ExoPlayer + auto-advance race

### UI / MainActivity
- [x] **Add Source Dialog**: Filtered out singleton sources (like Gallery) if they already exist, preventing duplicate selections.

### Build / Dependency Injection
- [x] **KSP repository map binding failure**: Removed Hilt `@IntoMap` usage for `PhotoRepository` providers and explicitly assembles the source repository map in `RepositoryModule`.
- [x] **Debug Kotlin compile cascade**: Fixed illegal mid-file imports, the missing `SlideshowView` brace, missing coroutine/Hilt imports, `dream.SourceType` references, Dropbox SDK 5.4 method usage, the photo info background enum mismatch, and the direct SlideshowView repository reference.

### Settings Fragment
- [x] **`clear_cache`**: Now clears Coil memory and disk cache
- [x] **`start_by_timer`**: Wired `OnPreferenceChangeListener` with DataStore persistence
- [x] **All video playback settings fully wired**: Audio mode, custom volume, max/min duration, and controls persist to DataStore and are consumed in SlideshowView
- [x] **Settings summaries refresh on resume**: Main Settings screen refreshes all preference summaries from DataStore when returning from sub-screens. Navigation preferences now show current state.
- [x] **`decoration_customize` handler**: Created `DecorationSettingsActivity` with tabbed UI for date/clock/weather customization
- [x] **Decoration opacity sliders**: All 9 sliders now initialize from saved config instead of showing XML defaults
- [x] **Schedule tab switching**: Autostop tab now correctly loads its own settings when selected

## Enhancements — Active Components
- [x] **Font size sliders for widgets**: Date, clock, and weather widgets now have continuous font size sliders (8sp–72sp) in Customize Overlays settings. Replaces the previous Small/Medium/Large dropdown with precise control.
- [x] **Gallery photo preloading**: `SlideshowManager.preloadPhoto()` now routes correctly for Gallery (content:// URIs), Google Drive cached (file:// URIs), and remote URLs
- [x] **Background folder pre-fetching**: Root folders are now fetched on background thread when a source is enabled/authenticated at app launch or via toggle. Cache is populated before the user clicks into the folder browser, making the initial load instant.
- [x] **Photo count per source**: Main screen cards now show "X photos available" under the status text, fetched from repositories on card refresh
- [x] **Source status indicators**: Colored dot indicators (green/orange/red) on source cards show connected/syncing/error states
- [x] **Better error messages in folder browser**: Created `FolderError` sealed class with typed errors (NetworkError, AuthError, PermissionError, ApiError, EmptyError, UnknownError) and user-friendly messages
- [x] **Folder thumbnail previews**: Folder items now show first photo as thumbnail using Coil (Gallery photos via content:// URIs, Google Drive falls back to folder icon)
- [x] **Just-in-Time Photo Loading**: Eliminated the front-loaded mass download bottleneck for Google Drive. Photos are now loaded instantly via remote URLs with OAuth headers injected by a Coil Interceptor.
- [x] **SlideshowView Cache Fix**: Switched to singleton `context.imageLoader` to restore proper memory caching and connection pooling.

## New Features
- [x] Multiple accounts per source type — Google Drive now supports signing in to multiple accounts simultaneously (e.g., 2 Google Drive accounts). Each account has independent folder selection, enable/disable toggle, and photo loading. The slideshow combines photos from all enabled accounts. UI shows one card per account with "Signed in as [email]" status.
- [x] Remove account option on source cards.
- [x] Date/clock overlay on photos during slideshow
- [x] Weather overlay on photos during slideshow
- [x] Photo info overlay (filename, date, source) during slideshow
- [x] Schedule-based source enabling (autostart/autostop schedules)
- [x] Battery-saver aware slideshow (pause on low battery)
- [x] Local photo cache for offline Google Drive use
- [x] Thumbnail caching for folder browser

### Cleanup
- [x] **Removed "Include subfolders" toggle** — Subfolder inclusion is now always-on by default. Removed toggle switch from folder browser UI, ViewModels, and `includeSubfolders` fields from `SourceConfig` and `SelectedFolder` data models.
- [x] **Content filter-aware media counts and labels** — Folder counts and labels now respect the content filter setting. When filter is "Photos Only", shows "X photos"; when "Videos Only", shows "X videos"; when "Both", shows "X items". Applies to folder browser list items, folder browser summary text, and main screen source cards. Added `getFilteredFolderMediaCount()` to both repositories to query by media type.
- [x] **Fixed subfolder photo inclusion** — `listPhotos()` in both repositories now recursively includes photos from all subfolders:
  - **Google Drive**: `collectPhotosFromFolder()` recursively traverses the folder tree, fetching photos from each subfolder level
  - **Gallery (Android 10+)**: Uses `RELATIVE_PATH` with `LIKE 'path%'` query to match photos from the selected folder and all subfolders. Falls back to exact bucket match on Android < 10.
- [x] **Subfolder checkbox cascade (all sources)** — Checking a folder's checkbox immediately checks all its descendant subfolders recursively. Unchecking a folder immediately unchecks all descendants. The cascade works for **all sources**:
  - **Google Drive**: `GoogleDriveViewModel.getSubfolderIds()` recursively collects ALL descendant folder IDs via the Drive API, then `FolderAdapter.cascadeSelection()` applies the check/uncheck to all descendants
  - **Gallery (Android 10+)**: `GalleryViewModel.getSubfolderIds()` queries MediaStore `RELATIVE_PATH` to find all buckets whose path starts with the selected folder's path (cascade is a no-op on Android < 10 where `RELATIVE_PATH` isn't available)
  - **Both**: `FolderAdapter.cascadeSelection()` updates selected/deselected sets, both are persisted to DataStore. The user can later override any subfolder's state individually.

## New Sources (Low Priority)
- [x] Dropbox integration
- [ ] Google Photos integration
- [ ] OneDrive integration
- [ ] Local network (SMB/WebDAV) support

## Quality
- [ ] Unit tests for repositories
- [ ] Instrumentation tests for folder browser
- [x] Re-run `:app:assembleDebug` after the AGP 9.2.0 bump and confirm the app still runs
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
- ✅ **Folder selection auto-saves** — checking/unchecking a folder immediately saves; no Save/Cancel buttons
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
- ✅ Video playback in slideshow (ExoPlayer with auto-advance)
- ✅ Content filter correctly restricts media types (arrays.xml + config reload fixes)
- ✅ Slideshow label ("photos"/"videos"/"items") adapts to content filter setting
- ✅ Settings wiring: 14 preferences now correctly save, read back, and sync UI state
- ✅ `cache_limit` custom value persistence (usePresetLimit + cacheSizeLimitMB)
- ✅ **Runtime settings now consumed**: background_color, keep_screen_on, screen_rotation, exit_trigger (touch), wifi_only, match_orientation
- ✅ **DreamService lifecycle**: proper cleanup in onDetachedFromWindow
- ✅ **Video double-advance fix**: isAdvancing guard prevents race between ExoPlayer callback and auto-advance coroutine
- ✅ **Clear cache**: actually clears Coil memory and disk cache
- ✅ **Start by timer**: switch persists timerConfig.enabled to DataStore
- ✅ **Video playback settings**: All 9 settings (audio mode, volume, max duration, auto-play, controls, loop short, display mode, fixed seconds, still timestamp) now persist to DataStore and are consumed by ExoPlayer in SlideshowView
- ✅ **Decoration customization**: `DecorationSettingsActivity` created with tabbed UI for date/clock/weather settings (position, format, font size, background, animation, opacity, pulse)
- ✅ **Gallery photo preloading**: `SlideshowManager.preloadPhoto()` now routes correctly for Gallery (content://), Google Drive cached (file://), and remote URLs
- ✅ **Photo count on source cards**: Main screen cards display "X photos available" fetched from repositories
- ✅ **Source status indicators**: Colored dot (green/orange/red) on source cards shows connected/syncing/error state
- ✅ **Better folder browser errors**: `FolderError` sealed class provides user-friendly error messages instead of raw exceptions
- ✅ **Folder thumbnail previews**: Folder items show first photo as thumbnail via Coil (Gallery photos) or folder icon fallback
- ✅ **Background folder pre-fetching**: Both repositories now have `prefetchRootFolders()` that runs on `Dispatchers.IO` via a dedicated `CoroutineScope`. Called from `MainActivity.refreshSourceCards()` when a source is enabled + authenticated, and from the auth callback after Google Drive sign-in. Folders are cached before the user opens the folder browser.
- ✅ **Folder browser navigation back stack**: Proper back stack tracks each folder visited. Back button returns to the previously-visited folder; when stack is empty, returns to main menu. Toolbar title shows current folder name.
- ✅ **Photo deduplication**: `SlideshowManager.loadPhotos()` deduplicates photos by ID to prevent double-counting when a parent folder is cascade-selected along with its subfolders.
- ✅ **Deselected folder persistence**: Unchecking a subfolder that was cascade-selected properly removes it from `selectedFolderIds` and adds to `deselectedFolderIds`, both persisted to DataStore and respected by the slideshow.
- ✅ **Gallery content filter counts**: `getFilteredFolderMediaCount()` now queries the correct MediaStore table (`Images.Media`, `Videos.Media`, or `Files`) instead of filtering by media_type column which was unreliable.
- ✅ **Google Drive content filter counts**: `getFilteredFolderMediaCount()` and `getFolderPhotoCount()` now paginate through all results (was capped at 1 due to `setPageSize(1)`).
- ✅ **All settings auto-save**: Removed manual Save buttons from Video Playback, Photo Info, Schedule, and Decoration settings screens. All toggles, sliders, spinners, and radio buttons now persist to DataStore immediately on change.
- ✅ **Settings summaries refresh on resume**: Main Settings screen now calls `syncSettingsFromDataStore()` in `onResume()` so all summaries reflect current values when returning from sub-screens.
- ✅ **Navigation preference summaries**: Video Playback, Schedule, Photo Info, and Decoration preferences now display current state (e.g., "Muted", "Custom volume (75%)", "Enabled at 8:00 PM").
- ✅ **Custom volume uses AudioManager**: "Use Custom Volume" now sets actual device volume via `AudioManager` instead of just a player volume multiplier. Original volume is restored on video end.
- ✅ **Min Video Duration setting**: New spinner lets users exclude videos shorter than a threshold (5s, 10s, 15s, 30s, 1min). Short videos are auto-skipped in slideshow.
- ✅ **Video playback settings simplified**: Removed autoplay/loop toggles (always on). Removed display mode options (always plays full). Content Type renamed from Content Filter.
- ✅ **Folder browser summary counts fixed**: Both Gallery and Drive folder browsers now correctly sum only selected folders' counts using `adapter.getPhotoCount()` instead of `viewModel.getPhotoCount()`.
- ✅ **Add Source dialog filtering**: Singleton sources like Gallery are now hidden from the Add Source dialog once they are added, making it easier to select Google Drive for a secondary account.

- ✅ **Multiple accounts per source**: Full multi-account support for Google Drive. Each account has its own card on the main screen, independent folder selection, independent enable/disable toggle, and per-account photo loading. The slideshow combines photos from all enabled accounts. Per-account caching in GoogleDrivePhotoRepository prevents cross-account cache collisions.
- ✅ **GoogleAccountManager**: New utility class replaces singleton auth state in GoogleDriveRepository. Manages per-account Drive services, OAuth tokens, and sign-out.
- ✅ **Per-account folder browsing**: FolderBrowserActivity and GoogleDriveViewModel route all API calls to the correct account via accountId.
- ✅ **Per-account settings persistence**: SettingsManager.isSourceEnabled() and hasAnySourceConfigured() now check multi-account state.

### In Progress
- (none)

### Planned
- ⏳ Dropbox, Google Photos, OneDrive, local network sources
- ⏳ Weather/clock/photo-info overlays during slideshow
- ⏳ Schedule-based source enabling
- ⏳ Unit and instrumentation tests
