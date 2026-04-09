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

### Settings Fragment
- [x] **`clear_cache`**: Now clears Coil memory and disk cache
- [x] **`start_by_timer`**: Wired `OnPreferenceChangeListener` with DataStore persistence
- [x] **All 9 video playback settings**: Persisted via PreferencesKeys and consumed in SlideshowView (audio mode, volume, max duration, auto-play, controls, loop short, display mode, fixed seconds, still timestamp)
- [x] **`decoration_customize` handler**: Created `DecorationSettingsActivity` with tabbed UI for date/clock/weather customization

## Enhancements — Active Components
- [x] **Gallery photo preloading**: `SlideshowManager.preloadPhoto()` now routes correctly for Gallery (content:// URIs), Google Drive cached (file:// URIs), and remote URLs
- [x] **Background folder pre-fetching**: Root folders are now fetched on background thread when a source is enabled/authenticated at app launch or via toggle. Cache is populated before the user clicks into the folder browser, making the initial load instant.
- [x] **Photo count per source**: Main screen cards now show "X photos available" under the status text, fetched from repositories on card refresh
- [x] **Source status indicators**: Colored dot indicators (green/orange/red) on source cards show connected/syncing/error states
- [x] **Better error messages in folder browser**: Created `FolderError` sealed class with typed errors (NetworkError, AuthError, PermissionError, ApiError, EmptyError, UnknownError) and user-friendly messages
- [x] **Folder thumbnail previews**: Folder items now show first photo as thumbnail using Coil (Gallery photos via content:// URIs, Google Drive falls back to folder icon)

## New Features
- [ ] Date/clock overlay on photos during slideshow
- [ ] Weather overlay on photos during slideshow
- [ ] Photo info overlay (filename, date, source) during slideshow
- [ ] Schedule-based source enabling (autostart/autostop schedules)
- [ ] Battery-saver aware slideshow (pause on low battery)
- [ ] Local photo cache for offline Google Drive use
- [ ] Thumbnail caching for folder browser

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
- [ ] Dropbox integration
- [ ] Google Photos integration
- [ ] OneDrive integration
- [ ] Local network (SMB/WebDAV) support

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

### In Progress
- (none)

### Planned
- ⏳ Dropbox, Google Photos, OneDrive, local network sources
- ⏳ Photo preloading and caching (SlideshowManager.preloadPhoto for Gallery)
- ⏳ Video playback settings wired to ExoPlayer
- ⏳ Weather/clock/photo-info overlays
- ⏳ Schedule-based source enabling
- ⏳ Unit and instrumentation tests
