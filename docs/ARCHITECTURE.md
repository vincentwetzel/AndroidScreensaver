# Architecture

Android Screensaver uses MVVM with a Repository pattern for photo source abstraction.

## Layers

### Presentation Layer (`ui/`)

- **Activities** - Single-screen UIs such as `MainActivity`, settings screens, auth screens, and folder browsers. MainActivity selects the TV-specific dark theme before inflating its Leanback layout; the matching window background avoids a redundant full-screen layout fill.
- **Adapters** - RecyclerView adapters for source and folder lists.
- **Views** - Observe ViewModels through `StateFlow`/`LiveData`.
- **FolderBrowserActivity** - Shared cloud folder browser for Google Drive, Dropbox, and OneDrive. It can launch source-specific re-authentication from the toolbar or auth-error empty state, then reload the current folder for the same account.
- **SlideshowView** - Custom `FrameLayout` that renders photos/videos, transitions, overlays, and playback behavior. Its clock decoration uses a single cancellable ticker for the view lifecycle. Settings refresh once per displayed media item rather than on every video-state poll. Video playback failures clear the pending advance guard before skipping, including failures before the player reaches its ready state; failures received while paused advance after resume.
- **NoSourcesView** - Setup guidance when no sources are configured.
- **ScreensaverPreviewActivity** - Activity-based preview that mirrors DreamService behavior for testing. It remembers activity pause state while source loading is suspended and applies it when the slideshow view is created.

### ViewModel Layer (`viewmodel/`)

- **MainViewModel** - Manages source-card enablement and authenticated account labels for Gallery, Google Drive, Dropbox, and OneDrive. It delegates pure source-card state transitions to `SourceSelectionState` so they can be verified without constructing Android or cloud-repository dependencies.
- **CloudFolderViewModel** - Shared cloud folder browsing state for Google Drive, Dropbox, OneDrive, and future cloud sources, routed by explicit source type and account ID.
- **GalleryViewModel** - Gallery folder browsing state.
- ViewModels expose UI state as flows and should not hold long-lived Android `Context` references.

### Data Layer (`data/`)

- **PhotoRepository** - Unified contract for all photo sources.
- **AbstractPhotoRepository** - Base class holding standardized `ConcurrentHashMap` caches, TTL constraints, and `syncPhotos` lifecycle behavior.
- **BaseCloudPhotoRepository** - Abstract base class for cloud sources (Google Drive, Dropbox) that centralizes thread-safe caching, TTL-aware deduplicated background prefetching, and per-account fallback routing.
- **GalleryPhotoRepository** - MediaStore access for local photos and videos, with cached folder/media counts, content-type-aware media listing, and capture timestamps with an added-time fallback.
- **GoogleDrivePhotoRepository** - Google Drive media access with account-scoped routing, recursive folder traversal, background count prefetching, thumbnail metadata, local cache paths, and interruptible API calls during coroutine cancellation.
- **DropboxPhotoRepository** - Dropbox media access with recursive listing, paginated folder search, background count prefetching, thumbnails, local cache support with validated filename suffixes, and interruptible SDK calls during coroutine cancellation.
- **OneDrivePhotoRepository** - OneDrive Graph API media access with account-scoped routing, recursive folder traversal, folder search, thumbnail metadata, and local cache downloads. Graph page and metadata calls are cancelled with their coroutine, and folder/photo/subfolder listings fail when any Graph page is incomplete. Photo-list results enter the cache only after every page succeeds. Slideshow downloads preserve media filename extensions and refresh expired preauthenticated URLs through Graph metadata.
- **GoogleDriveRepository** - Delegates account selection and Drive service creation to `GoogleAccountManager`, and verifies read-only Drive access before sign-in is finalized.
- **DropboxRepository** - Delegates Dropbox auth, account metadata, and API clients to `DropboxAccountManager`. Saved account restoration runs once on the account manager IO scope, and suspend repository calls await that restore before resolving clients.
- **OneDriveAuthManager** - Manages OneDrive auth with MSAL on phones/tablets, device code flow on Android TV, and encrypted per-account token storage initialized lazily for keystore safety. It asynchronously maintains an in-memory account-ID snapshot so synchronous repository authentication checks do not perform keystore I/O on the caller thread. Expired device-flow tokens refresh through OAuth; mobile tokens refresh through MSAL silent acquisition.
- **SettingsManager** - DataStore-backed preferences for slideshow config, source state, selected folders, account configs, and both autostart/autostop schedules.
- **AccountConfigCodec** - Pure delimiter-safe encoding and decoding for the per-account settings value used by `SettingsManager`.
- **ScheduleConfigCodec** - Pure delimiter-safe encoding and decoding for schedule collections stored by `SettingsManager`.
- **GalleryPermissionPolicy** - Pure filter-to-media permission rules shared by gallery browsing and slideshow source checks, including Android 14 selected-media grants and cache invalidation when permission state changes.

### Service Layer (`dream/`)

- **PhotoScreensaverService** - Android DreamService that hosts the slideshow; the OS binds it as part of system-managed dream activation.
- **ScheduleService / AlarmReceiver** - Rebuild enabled start/stop alarms from persisted schedule settings, enforce charging-only autostart, and disable non-repeating entries after successful execution.
- **SlideshowManager** - Loads media from enabled sources using explicit account IDs for every cloud folder and photo query, applies settings, filters and deduplicates each completed result batch by a structured source/account/media ID tuple to limit peak retained metadata, sorts/shuffles media, and routes cacheable cloud downloads through the correct repository.
- **PhotoMediaClassifier** - Pure media-extension/content-URI classification including 3GPP, MPEG program-stream, and MPEG transport-stream video suffixes, cloud download routing, video minimum-duration rules, and configured photo sort-key rules shared by `SlideshowManager` and `SlideshowView`. SVG images use Coil's SVG decoder; slideshow raster requests allow hardware bitmaps because they are only drawn, not pixel-read or mutated. Cloud videos with opaque download URLs are routed to Media3 by filename, Google Drive video bytes are downloaded through the authenticated repository cache, and Dropbox paths are downloaded before Coil or Media3 receives them.

### Dependency Injection (`di/`)

- **RepositoryModule** - Hilt module providing repositories and `SlideshowManager`.
- Photo repositories are concrete injected singletons. The repository map is assembled explicitly instead of using Hilt map multibindings.
- OneDrive auth is split between `OneDriveAuthManager` for token handling and `OneDriveAuthActivity` for the UI launch surface. Authentication returns the same stable Microsoft Graph account ID used as the token key, plus the Graph mail or user principal name used by account status UI.

## Test Tooling (`tools/`)

- `tools/run_tests.py` is the fail-fast host entry point. It runs project-structure checks, the `assembleDebug` compile gate, JVM unit tests, and optional ADB smoke checks in that order.
- JVM tests cover model/account helpers, source authentication state, shared repository routing and cache invalidation, cloud folder navigation/error handling, settings account serialization, base account-manager transitions, and media classification/sorting. Separate Python tests cover host-runner arguments, reports, device parsing, and suspects-cache behavior. Coroutine ViewModel tests use `kotlinx-coroutines-test` with a controlled main dispatcher.
- The optional `--suspects` runner mode persists failed runner check names in a JSON cache, removes names when those checks pass, and leaves skipped checks unchanged. The cache is updated atomically and is not touched unless the flag is supplied.
- The runner uses `--device auto` by default: an authorized device enables device checks, while no device is recorded as a skip. `--device on` makes ADB availability and authorization a preflight requirement; `--device off` runs host checks only.
- Each command receives a timestamped log under `build/test-reports/<run timestamp>/`. Console and command-log lines use local clock time plus cumulative suite runtime; the runner always writes `summary.txt` and `summary.json`, including full start/end timestamps, duration, result counts, failure details, and the fail-fast abort reason.
- Device smoke checks install the debug APK by default and verify the package, launcher activity, DreamService resolution, and launcher startup. `--clean-install` explicitly removes only the debug package before installation when a signing mismatch requires it. OAuth, permissions, folder browsing, and screensaver lifecycle behavior still require instrumentation or manual device coverage.

## Data Flow

```text
User enables a source
-> SettingsManager saves account state

User selects folders
-> SettingsManager saves selected folder metadata, deselected folder IDs, and cached media counts

Preview or DreamService starts
-> SlideshowManager loads fresh config
-> SlideshowManager checks enabled sources and selected folders
-> Repositories load media with the active Content Type filter and account ID
-> SlideshowManager filters, deduplicates, sorts/shuffles
-> SlideshowView renders media with Coil and ExoPlayer
-> SlideshowView advances based on slideshow/video settings
```

## Key Classes

| Class | Responsibility |
|-------|---------------|
| `PhotoRepository` | Interface defining photo source operations |
| `AbstractPhotoRepository` | Unifies TTL maps and cache clearing |
| `BaseCloudPhotoRepository` | Unifies common cloud caching, routing, and prefetch logic |
| `GalleryPhotoRepository` | MediaStore-based local photo/video access |
| `GoogleDrivePhotoRepository` | Google Drive media access with account-scoped URLs and cache paths |
| `DropboxPhotoRepository` | Dropbox media access with thumbnail and local cache support |
| `GoogleDriveRepository` | Google account selection, Drive access verification, and Drive service client facade |
| `DropboxRepository` | Dropbox account and client facade |
| `BaseAccountManager` | Abstract class centralizing common multi-account maps and auth queries |
| `GoogleAccountManager` | Per-account Google auth, credentials, and Drive services |
| `DropboxAccountManager` | Per-account Dropbox PKCE credentials, emails, and clients |
| `CloudFolderViewModel` | Shared folder browsing, search, back stack, and recursive subfolder lookup for account-scoped cloud sources |
| `SlideshowManager` | Combines sources and applies slideshow config |
| `PhotoMediaClassifier` | Pure media classification and slideshow sort-key rules |
| `SlideshowView` | Displays photos/videos, transitions, overlays, and playback |
| `NoSourcesView` | Shows setup guidance when no source is configured |
| `ScreensaverPreviewActivity` | Activity preview of screensaver behavior |
| `PhotoScreensaverService` | DreamService fullscreen slideshow host |
| `SettingsManager` | DataStore persistence for app settings |
| `AccountConfigCodec` | Pure delimiter-safe account settings serialization |
| `ScheduleConfigCodec` | Pure delimiter-safe schedule serialization |
| `GalleryPermissionPolicy` | Gallery media access, selected-media handling, and permission-based cache invalidation rules |
| `MediaCacheManager` | Cloud media cache size enforcement and oldest-first cleanup |
| `RepositoryCacheKey` | Collision-safe encoding for account-scoped repository cache keys |

## Threading

- Repository API calls use `withContext(Dispatchers.IO)`. Blocking Google Drive list, search, metadata, and verification calls run with `runInterruptible`; Dropbox SDK operations run with `runInterruptible`, and OneDrive HTTP requests cancel their underlying OkHttp calls.
- Folder lists, photo lists, and photo counts are cached in repository singletons.
- Shared repository caches use concurrent maps where background prefetch and slideshow loading can overlap.
- `SlideshowManager` chunk-loads selected folders in small concurrent batches to limit memory/API spikes.
- ViewModel work runs in `viewModelScope`.
- Main source cards read persisted per-account selected media counts so card refreshes do not block on recursive cloud API counts; repository prefetch refreshes those counts in the background.
- Folder browser ViewModels retain the active `MediaTypeFilter` enum directly so navigation, refresh, and summary labels stay aligned with repository filtering.
- UI settings/account reads are suspend calls and must run from coroutines. Activities use `lifecycleScope`, while `ScheduleService` owns one cancellable IO job, replaces stale scheduling runs, and stops after its alarm updates finish.
- Settings and folder-browser save paths snapshot current UI or adapter state before DataStore reads, then merge the snapshot into the freshly loaded config to avoid stale lifecycle-resume state overwriting newer user choices.

## Screensaver Implementation

The app uses Android DreamService. The activation card in `MainActivity` detects the active screensaver with:

1. Multi-key settings detection: `dream_components`, `screensaver_components`, and `dream_component`.
2. Fallback detection through `screensaver_enabled` when this is the only DreamService installed.
3. DreamService support detection through `queryIntentServices()` before reading settings.

The card hides once the app is detected as the active screensaver.

## Settings Reference

All settings are persisted through DataStore Preferences.

### Source Settings

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `source_google_drive_enabled` | Boolean | `false` | Google Drive source enabled |
| `source_google_drive_folders` | StringSet | `{}` | Selected Google Drive folder IDs |
| `source_dropbox_enabled` | Boolean | `false` | Dropbox source enabled |
| `source_dropbox_folders` | StringSet | `{}` | Selected Dropbox folder IDs |
| `source_onedrive_enabled` | Boolean | `false` | OneDrive source enabled |
| `source_onedrive_folders` | StringSet | `{}` | Selected OneDrive folder IDs |
| `source_gallery_enabled` | Boolean | `false` | Gallery source enabled |
| `source_gallery_folders` | StringSet | `{}` | Selected Gallery folder IDs |
| `source_accounts` | String | `""` | Serialized per-source account configs, URL-encoded account IDs/emails, selected folder IDs/names/paths, deselected folders, auth state, and cached selected media counts |

### Slideshow Settings

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `slide_duration_seconds` | Int | `5` | Seconds per photo |
| `shuffle` | Boolean | `true` | Randomize photo order |
| `photo_order` | String | `DATE_NEWEST_FIRST` | Sort order |
| `media_type_filter` | String | `IMAGES_ONLY` | Filter by media type |
| `match_orientation` | Boolean | `false` | Use `FIT_CENTER` for mismatched orientation |
| `display_effect` | String | `CROP_TO_FIT` | Photo display effect |
| `transition_effect` | String | `FADE` | Transition between photos |
| `transition_duration_ms` | Int | `1000` | Transition animation duration |
| `background_color` | Int | `0xFF000000` | Slideshow background color |
| `screen_orientation` | String | `SYSTEM_DEFAULT` | Screen orientation lock |
| `keep_screen_on` | Boolean | `false` | Prevent screen dimming |
| `stop_on_low_battery` | Boolean | `false` | Exit the DreamService when battery level is at or below the configured threshold and the device is not charging |
| `low_battery_threshold` | Int | `20` | Battery percentage threshold for low-battery auto-exit |
| `enable_cache` | Boolean | `true` | Enable media caching |
| `cache_limit_mb` | Int | `500` | Custom cache size limit |
| `cache_use_preset` | Boolean | `true` | Use preset cache limit |
| `wifi_only` | Boolean | `true` | Fetch cloud sources only on Wi-Fi/Ethernet |
| `network_timeout` | Int | `30` | HTTP request timeout in seconds |
| `exit_trigger` | String | `TOUCH` | How to exit screensaver |
| `timer_enabled` | Boolean | `false` | Enable timer behavior |
| `photo_info_enabled` | Boolean | `false` | Show photo metadata overlay |
| `photo_info_*` | Boolean/String/Int | varies | Persisted photo-info fields, layout, position, separator, date format, font, opacity, background, shadow, and fade behavior |
| `decoration_date` | Boolean | `false` | Show date overlay |
| `decoration_clock` | Boolean | `false` | Show clock overlay |
| `decoration_weather` | Boolean | `false` | Show weather overlay |

## Settings Runtime Behavior

- `PhotoScreensaverService.onAttachedToWindow()` refreshes slideshow config.
- `SlideshowView.initialize()` applies background color and starts rendering.
- `SlideshowView` starts a cancelable burn-in protection job that periodically pixel-shifts visible persistent overlays while playback is active.
- DreamService applies keep-screen-on and screen-orientation behavior. A pause request made during source loading is retained until playback is explicitly resumed.
- `SlideshowManager.loadPhotos()` checks network restrictions before cloud fetches.
- `SlideshowView.showPhoto()` applies match-orientation behavior.
- `SlideshowView.startAutoAdvance()` reloads config each cycle so setting changes can take effect mid-slideshow.
- Slideshow pauses tracked display-effect and transition animators with playback, and cancels them when media changes or the view stops. Slideshow display duration begins after the current image/video reports ready, so remote download latency does not consume its on-screen time. Photo metadata overlays update at that same readiness point so labels stay paired with the visible media.
- Cloud media cache filenames use SHA-256 of account and provider media IDs to avoid path-sanitization collisions. Cloud media downloads pass through `MediaCacheManager`, which periodically trims provider download caches to the configured cache budget by oldest file first. Temporary downloads are excluded and the item just downloaded or displayed from cache is protected from eviction. Cleanup runs at most once per minute even if every slideshow item is already cached.
- `ScreensaverPreviewActivity` loads source state and slideshow config asynchronously before rendering, matching the suspend settings contract used by DreamService.
- DreamService handles touch exit, receiver cleanup, timeout cleanup, battery-saver pause behavior, and optional low-battery auto-exit while not charging.

## Coding Standards

Engineering standards live in [`CODING_STANDARDS.md`](CODING_STANDARDS.md). Keep this file focused on architecture, data flow, key classes, settings behavior, and system design decisions.

## Accessing Settings

- Slideshow config: `SettingsManager.getSlideshowConfig(context)` / `saveSlideshowConfig(context, config)`
- Source state: `SettingsManager.isSourceEnabled(context, sourceType)` / `setSourceEnabled(...)`
- Account state: `SettingsManager.getAccountsForSource(context, sourceType)`, `getAccount(...)`, `saveAccount(...)`, and `removeAccount(...)`
- Selected folders: `SettingsManager.getSelectedFolders(context, sourceType)` / `setSelectedFolders(...)`
- Any source configured: `SettingsManager.hasAnySourceConfigured(context)`

Most `SettingsManager` reads and writes are suspend functions. Call them from `viewModelScope`, `lifecycleScope`, repository IO work, or another coroutine context rather than blocking the main thread.
