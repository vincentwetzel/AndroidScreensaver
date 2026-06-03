# Android Screensaver

A photo slideshow screensaver app for Android phones, tablets, and TV devices. Displays photos and videos from local and cloud sources as a fullscreen Android DreamService.

## Features

- **Multiple photo sources**
  - **Gallery** - Browse and select folders from device photos via MediaStore.
  - **Google Drive** - Browse and select folders from one or more Google accounts with read-only OAuth2 authentication.
  - **Dropbox** - Browse and select Dropbox folders from one or more accounts with PKCE OAuth, thumbnails, and local cache support.
  - Planned: Google Photos, OneDrive, and local network sources.

- **Customizable slideshow**
  - Shuffle/random order, configurable slide duration, and multiple transition effects.
  - Display effects including crop-to-fit, scale-to-fit, zoom, and pan.
  - Photo ordering by date, name, or size.
  - Content Type filter for images only, videos only, or both.
  - Background color, screen rotation, keep-screen-on, low-battery auto-exit, match-orientation, and touch-exit settings.
  - Network-only cloud loading works on Wi-Fi or Ethernet.
  - Burn-in protection gently shifts persistent overlays during long-running screensaver sessions.

- **Video playback**
  - Videos play with Media3/ExoPlayer and auto-advance when finished.
  - Audio modes: mute, system volume, or custom absolute device volume.
  - Minimum and maximum video duration filters.
  - Short videos loop automatically with a bounded timeout so playback still advances.

- **Screensaver activation**
  - Android DreamService integration, configurable from **Settings > Display > Screen saver**.
  - Activation card on the main screen guides users through setup and auto-hides once active.
  - Preview button instantly launches the screensaver for testing.

- **Modern UI**
  - Material Design 3 components.
  - Phone/tablet and TV-oriented layouts.
  - Dark/light theme support.

## Quick Start

1. Build and run in Android Studio or with `./gradlew installDebug`.
2. Enable a source such as Gallery, Google Drive, or Dropbox. Remote sources can be added more than once for different accounts.
3. Tap the source card and select folders. Folder selections save immediately.
4. Source cards reuse the saved selected-folder count while repositories refresh media counts in the background.
5. Use **Re-authenticate** from a remote source card or folder browser menu if a cloud token expires.
6. Tap **Open Screensaver Settings**, select **Android Screensaver**, then return to the app.
7. Adjust slideshow, video, schedule, overlay, and cache settings from the app settings menu.

## Tech Stack

- **Language:** Kotlin
- **Architecture:** MVVM with Repository pattern
- **DI:** Hilt
- **Async:** Kotlin Coroutines + Flow
- **Storage:** DataStore Preferences
- **Image Loading:** Coil
- **Video:** Media3/ExoPlayer
- **Build:** Android Gradle Plugin 9.2.1 with Gradle 9.4.1
- **Compile SDK:** 36
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)

## Project Structure

```text
app/src/main/java/com/vincentwetzel/androidscreensaver/
|-- data/
|   |-- model/          # Data classes
|   `-- repository/     # PhotoRepository implementations
|-- di/                 # Hilt dependency injection modules
|-- dream/              # DreamService and SlideshowManager
|-- ui/
|   |-- main/           # MainActivity and source cards
|   |-- settings/       # Settings screens
|   |-- slideshow/      # SlideshowView and NoSourcesView
|   `-- sources/        # Source auth and folder browser screens
|-- utils/              # SettingsManager and account helpers
`-- viewmodel/          # Main, Gallery, shared cloud folder, and source ViewModels
```

## Documentation

| File | Purpose |
|------|---------|
| [BUILD.md](BUILD.md) | Build commands, OAuth setup notes, and troubleshooting |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Architecture, design patterns, key classes, and settings reference |
| [CODING_STANDARDS.md](CODING_STANDARDS.md) | Kotlin/Android coding standards, review checklist, and verification expectations |
| [CHANGELOG.md](CHANGELOG.md) | Release history and completed changes |
| [TODO.md](TODO.md) | Active backlog and quality checklist |
| [USER_GUIDE.md](USER_GUIDE.md) | User-facing setup and usage guide |
| [AGENTS.md](AGENTS.md) | Developer roles, documentation rules, and git restrictions |

`PROGRESS.md` was removed because it duplicated `TODO.md` and `CHANGELOG.md`. Use `TODO.md` for current/planned work and `CHANGELOG.md` for completed change history.

## License

See [LICENSE](LICENSE).
