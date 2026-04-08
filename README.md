# Android Screensaver

A photo slideshow screensaver app for Android phones, tablets, and TV devices. Displays photos from multiple sources as a fullscreen screensaver.

## Features

- **Multiple Photo Sources**
  - **Gallery** — Browse and select folders from device photos (via MediaStore API)
  - **Google Drive** — Browse and select folders from Google Drive (OAuth2 authenticated)
  - *More sources planned: Dropbox, Google Photos, OneDrive, Local Network*

- **Customizable Slideshow**
  - Shuffle/random order
  - Configurable slide duration
  - Multiple transition effects with crossfade animation
  - Display effects (crop-to-fit, scale-to-fit, zoom, pan)
  - Photo ordering (date, name, size)

- **Testing & Debugging**
  - **TEST button** — Instantly preview the screensaver without waiting for screen timeout
  - Informative "No Photos" screen when no sources are configured

- **Screensaver Activation**
  - System DreamService (screensaver) — configure via **Settings → Display → Screen saver**
  - Activation card on main screen guides users through setup; auto-hides when active

- **Modern UI**
  - Material Design 3 components
  - Phone/tablet and TV (leanback) layouts
  - Dark/light theme support

## Quick Start

1. **Build and run** in Android Studio or via `./gradlew installDebug`
2. **Enable a photo source** — Toggle Gallery or Google Drive ON
3. **Select folders** — Tap the source to browse and select folders
4. **Activate screensaver** — The activation card at the top will guide you. Tap "Open Screensaver Settings", select "Android Screensaver", return to the app and the card will disappear.
5. **Configure** — Set when to start (while charging, while docked, etc.) in system settings.

## Tech Stack

- **Language:** Kotlin
- **Architecture:** MVVM with Repository pattern
- **DI:** Hilt
- **Async:** Kotlin Coroutines + Flow
- **Storage:** DataStore (preferences)
- **Image Loading:** Coil
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)

## Project Structure

```
app/src/main/java/com/vincentwetzel/androidscreensaver/
├── data/
│   ├── model/          # Data classes (Photo, PhotoFolder, SourceType, etc.)
│   └── repository/     # PhotoRepository implementations
├── di/                 # Hilt dependency injection modules
├── dream/              # DreamService (screensaver service), SlideshowManager
├── ui/
│   ├── main/           # MainActivity (source selection)
│   ├── settings/       # Settings activities
│   ├── slideshow/      # SlideshowView (photo display with transitions), NoSourcesView
│   └── sources/        # Source auth + folder browser activities
├── utils/              # Utilities (SettingsManager, OAuth config)
└── viewmodel/          # ViewModels (MainViewModel, GalleryViewModel, GoogleDriveViewModel)
```

## Documentation

| File | Purpose |
|------|---------|
| [BUILD.md](BUILD.md) | Build commands, Google Drive OAuth setup, troubleshooting |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Architecture, design patterns, key classes, settings reference |
| [CHANGELOG.md](CHANGELOG.md) | Version history and changes |
| [TODO.md](TODO.md) | Backlog and current progress |
| [USER_GUIDE.md](USER_GUIDE.md) | User-facing setup and usage guide |
| [AGENTS.md](AGENTS.md) | Developer roles, contributing guidelines, documentation rules |

## License

See [LICENSE](LICENSE).
