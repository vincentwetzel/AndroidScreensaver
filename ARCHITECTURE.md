# Architecture

Android Screensaver uses **MVVM (Model-View-ViewModel)** with a **Repository** pattern for data abstraction.

## Layers

### Presentation Layer (`ui/`)
- **Activities** — Single screen UIs (MainActivity, SettingsActivity, FolderBrowserActivity, etc.)
- **Adapters** — RecyclerView adapters for lists
- **Views** observe **ViewModels** via `StateFlow`/`LiveData`
- **SlideshowView** — Custom `FrameLayout` that renders the photo slideshow with crossfade transitions
- **NoSourcesView** — Custom `LinearLayout` that displays guidance when no sources are configured
- **ScreensaverPreviewActivity** — Activity-based preview of the screensaver for testing

### ViewModel Layer (`viewmodel/`)
- **MainViewModel** — Manages source selection state
- **GoogleDriveViewModel** — Google Drive auth + folder browsing
- **GalleryViewModel** — Gallery folder browsing
- Expose UI state as flows; never hold Android Context references

### Data Layer (`data/`)
- **PhotoRepository** (interface) — Unified contract for all photo sources
  - `GalleryPhotoRepository` — MediaStore API for device photos
  - `GoogleDrivePhotoRepository` — Google Drive API for cloud photos
- **GoogleDriveRepository** — Handles Google Sign-In + Drive API client
- **SettingsManager** — DataStore-backed preferences (slideshow config, source state)

### Service Layer (`dream/`)
- **PhotoScreensaverService** — DreamService that runs the slideshow
  - Uses `SlideshowView` to display photos with crossfade transitions
  - Shows `NoSourcesView` when no sources are configured
  - Injected with `SlideshowManager` via Hilt
- **SlideshowManager** — Central orchestrator for photo loading and slideshow configuration
  - Loads photos from all enabled sources (Gallery, Google Drive)
  - Applies shuffle and sort based on user settings
  - Manages preload cache (partial — Gallery preloading needs wiring)

### Dependency Injection (`di/`)
- **RepositoryModule** — Hilt module providing singletons for all repositories and `SlideshowManager`

## Data Flow

```
User enables Gallery → SettingsManager saves source state
User selects folders → SettingsManager saves folder IDs to DataStore
TEST button pressed → ScreensaverPreviewActivity launched
                    → checks SettingsManager.hasAnySourceConfigured()
                    → if false: shows NoSourcesView
                    → if true: creates SlideshowView

Slideshow starts → SlideshowView.initialize(slideshowManager)
                 → calls SlideshowManager.loadPhotos()
                 → SlideshowManager checks SettingsManager for enabled sources
                 → for each enabled source, gets selected folders
                 → loads photos from GalleryPhotoRepository / GoogleDrivePhotoRepository
                 → applies shuffle or sort based on config
                 → returns combined photo list to SlideshowView

Slideshow runs → SlideshowView loads photos via Coil from content:// URIs
               → auto-advances based on slide duration
               → crossfades between photos using two ImageViews
               → handles pause/resume on activity lifecycle
```

## Key Classes

| Class | Responsibility |
|-------|---------------|
| `PhotoRepository` | Interface defining photo source operations |
| `GalleryPhotoRepository` | MediaStore-based local photo access |
| `GoogleDrivePhotoRepository` | Google Drive API photo access |
| `GoogleDriveRepository` | Google Sign-In + Drive service client |
| `SlideshowManager` | Combines sources, loads photos, applies slideshow config |
| `SlideshowView` | Custom view that displays photos with crossfade transitions |
| `NoSourcesView` | Custom view that shows setup guidance when no sources configured |
| `ScreensaverPreviewActivity` | Activity that mirrors DreamService for testing |
| `PhotoScreensaverService` | DreamService — fullscreen slideshow |
| `SettingsManager` | DataStore persistence for all settings |
| `GoogleOAuthConfig` | OAuth client ID configuration |

## Threading

- All repository API calls use `withContext(Dispatchers.IO)` to avoid blocking the main thread
- Folder lists are cached in repositories (`@Singleton`) to avoid re-fetching on Activity recreation
- ViewModel coroutines use `viewModelScope.launch` for lifecycle-aware async

## Screensaver Implementation

The app uses Android's **DreamService** API for screensaver functionality. The activation card in MainActivity detects whether the app is set as the active screensaver using:

1. Multi-key settings detection (`dream_components`, `screensaver_components`, `dream_component`)
2. Fallback: checks `screensaver_enabled` when this is the only DreamService installed
3. DreamService support detection via `queryIntentServices()` before attempting to read settings

The card automatically hides once the app is detected as the active screensaver.

## Settings Reference

All settings are persisted via **DataStore Preferences**.

### Source Settings

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `source_google_drive_enabled` | Boolean | `false` | Google Drive source enabled |
| `source_google_drive_folders` | StringSet | `{}` | Selected Google Drive folder IDs |
| `source_gallery_enabled` | Boolean | `false` | Gallery source enabled |
| `source_gallery_folders` | StringSet | `{}` | Selected Gallery folder IDs (bucket IDs) |

### Slideshow Settings

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `slide_duration_seconds` | Int | `5` | Seconds per photo |
| `shuffle` | Boolean | `true` | Randomize photo order |
| `photo_order` | String | `DATE_NEWEST_FIRST` | Sort order |
| `media_type_filter` | String | `IMAGES_ONLY` | Filter by media type |
| `match_orientation` | Boolean | `false` | Match device orientation |
| `display_effect` | String | `CROP_TO_FIT` | Photo display effect |
| `pan_direction` | String | `RANDOM` | Pan direction for pan effect |
| `transition_effect` | String | `FADE` | Transition between photos |
| `transition_duration_ms` | Int | `1000` | Transition animation duration |
| `transition_easing` | String | `EASE_IN_OUT` | Transition easing curve |
| `transition_direction` | String | `LEFT` | Transition direction |
| `background_color` | Int | `0xFF000000` | Background color |
| `screen_orientation` | String | `SYSTEM_DEFAULT` | Screen orientation lock |
| `keep_screen_on` | Boolean | `false` | Prevent screen dimming |
| `enable_cache` | Boolean | `true` | Enable photo caching |

### Accessing Settings

- **Slideshow config:** `SettingsManager.getSlideshowConfig(context)` / `saveSlideshowConfig(context, config)`
- **Source state:** `SettingsManager.isSourceEnabled(context, sourceType)` / `setSourceEnabled(...)`
- **Selected folders:** `SettingsManager.getSelectedFolders(context, sourceType)` / `setSelectedFolders(...)`
- **Any source configured:** `SettingsManager.hasAnySourceConfigured(context)`
