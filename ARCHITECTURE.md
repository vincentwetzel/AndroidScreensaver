# Architecture

Android Screensaver uses MVVM with a Repository pattern for photo source abstraction.

## Layers

### Presentation Layer (`ui/`)

- **Activities** - Single-screen UIs such as `MainActivity`, settings screens, and folder browsers.
- **Adapters** - RecyclerView adapters for source and folder lists.
- **Views** - Observe ViewModels through `StateFlow`/`LiveData`.
- **SlideshowView** - Custom `FrameLayout` that renders photos/videos, transitions, overlays, and playback behavior.
- **NoSourcesView** - Setup guidance when no sources are configured.
- **ScreensaverPreviewActivity** - Activity-based preview that mirrors DreamService behavior for testing.

### ViewModel Layer (`viewmodel/`)

- **MainViewModel** - Manages source selection state.
- **GoogleDriveViewModel** - Google Drive account and folder browsing state.
- **GalleryViewModel** - Gallery folder browsing state.
- ViewModels expose UI state as flows and should not hold long-lived Android `Context` references.

### Data Layer (`data/`)

- **PhotoRepository** - Unified contract for all photo sources.
- **GalleryPhotoRepository** - MediaStore access for local photos and videos.
- **GoogleDrivePhotoRepository** - Google Drive media access with account-scoped routing, recursive folder traversal, thumbnail metadata, and local cache paths.
- **DropboxPhotoRepository** - Dropbox media access with recursive listing, paginated folder search, thumbnails, and local cache support.
- **GoogleDriveRepository** - Delegates auth and Drive service creation to `GoogleAccountManager`.
- **SettingsManager** - DataStore-backed preferences for slideshow config, source state, selected folders, and account configs.

### Service Layer (`dream/`)

- **PhotoScreensaverService** - Android DreamService that hosts the slideshow.
- **SlideshowManager** - Loads media from enabled sources, applies settings, filters content, deduplicates entries, sorts/shuffles media, and routes cacheable cloud downloads through the correct repository.

### Dependency Injection (`di/`)

- **RepositoryModule** - Hilt module providing repositories and `SlideshowManager`.
- Photo repositories are concrete injected singletons. The repository map is assembled explicitly instead of using Hilt map multibindings.

## Data Flow

```text
User enables a source
-> SettingsManager saves source state

User selects folders
-> SettingsManager saves selected folder IDs

Preview or DreamService starts
-> SlideshowManager loads fresh config
-> SlideshowManager checks enabled sources and selected folders
-> Repositories load media
-> SlideshowManager filters, deduplicates, sorts/shuffles
-> SlideshowView renders media with Coil and ExoPlayer
-> SlideshowView advances based on slideshow/video settings
```

## Key Classes

| Class | Responsibility |
|-------|---------------|
| `PhotoRepository` | Interface defining photo source operations |
| `GalleryPhotoRepository` | MediaStore-based local photo/video access |
| `GoogleDrivePhotoRepository` | Google Drive media access with account-scoped URLs and cache paths |
| `DropboxPhotoRepository` | Dropbox media access with thumbnail and local cache support |
| `GoogleDriveRepository` | Google Sign-In and Drive service client facade |
| `GoogleAccountManager` | Per-account Google auth, credentials, and Drive services |
| `SlideshowManager` | Combines sources and applies slideshow config |
| `SlideshowView` | Displays photos/videos, transitions, overlays, and playback |
| `NoSourcesView` | Shows setup guidance when no source is configured |
| `ScreensaverPreviewActivity` | Activity preview of screensaver behavior |
| `PhotoScreensaverService` | DreamService fullscreen slideshow host |
| `SettingsManager` | DataStore persistence for app settings |

## Threading

- Repository API calls use `withContext(Dispatchers.IO)`.
- Folder lists and photo counts are cached in repository singletons.
- Shared repository caches use concurrent maps where background prefetch and slideshow loading can overlap.
- `SlideshowManager` chunk-loads selected folders in small concurrent batches to limit memory/API spikes.
- ViewModel work runs in `viewModelScope`.

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
| `source_gallery_enabled` | Boolean | `false` | Gallery source enabled |
| `source_gallery_folders` | StringSet | `{}` | Selected Gallery folder IDs |

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
| `enable_cache` | Boolean | `true` | Enable media caching |
| `cache_limit_mb` | Int | `500` | Custom cache size limit |
| `cache_use_preset` | Boolean | `true` | Use preset cache limit |
| `wifi_only` | Boolean | `true` | Fetch cloud sources only on Wi-Fi/Ethernet |
| `network_timeout` | Int | `30` | HTTP request timeout in seconds |
| `exit_trigger` | String | `TOUCH` | How to exit screensaver |
| `timer_enabled` | Boolean | `false` | Enable timer behavior |
| `photo_info_enabled` | Boolean | `false` | Show photo metadata overlay |
| `decoration_date` | Boolean | `false` | Show date overlay |
| `decoration_clock` | Boolean | `false` | Show clock overlay |
| `decoration_weather` | Boolean | `false` | Show weather overlay |

## Settings Runtime Behavior

- `PhotoScreensaverService.onAttachedToWindow()` refreshes slideshow config.
- `SlideshowView.initialize()` applies background color and starts rendering.
- DreamService applies keep-screen-on and screen-orientation behavior.
- `SlideshowManager.loadPhotos()` checks network restrictions before cloud fetches.
- `SlideshowView.showPhoto()` applies match-orientation behavior.
- `SlideshowView.startAutoAdvance()` reloads config each cycle so setting changes can take effect mid-slideshow.
- DreamService handles touch exit, receiver cleanup, timeout cleanup, and battery-saver pause behavior.

## Accessing Settings

- Slideshow config: `SettingsManager.getSlideshowConfig(context)` / `saveSlideshowConfig(context, config)`
- Source state: `SettingsManager.isSourceEnabled(context, sourceType)` / `setSourceEnabled(...)`
- Selected folders: `SettingsManager.getSelectedFolders(context, sourceType)` / `setSelectedFolders(...)`
- Any source configured: `SettingsManager.hasAnySourceConfigured(context)`
