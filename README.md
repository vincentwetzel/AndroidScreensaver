# Android Screensaver

A photo slideshow screensaver app for Android phones, tablets, and TV devices. Displays photos and videos from local and cloud sources as a fullscreen Android DreamService.

## Features

- **Multiple photo sources**
  - **Gallery** - Browse and select folders from device photos via MediaStore.
  - **Google Drive** - Browse and select folders from one or more Google accounts with read-only OAuth2 authentication.
  - **Dropbox** - Browse and select Dropbox folders from one or more accounts with PKCE OAuth, thumbnails, and local cache support.
  - **OneDrive** - Browse and select OneDrive folders from one or more Microsoft accounts with hybrid auth: MSAL on phones/tablets and device flow on Android TV.
  - Planned: Google Photos and local network sources.

- **Customizable slideshow**
  - Shuffle/random order, configurable slide duration, and multiple transition effects.
  - Display effects including crop-to-fit, scale-to-fit, zoom, and pan.
  - Photo ordering by date, name, or size.
  - Content Type filter for images only, videos only, or both, enforced through repository and folder-browser filtering.
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
2. Enable a source such as Gallery, Google Drive, Dropbox, or OneDrive. Remote sources can be added more than once for different accounts, and authenticated source cards always show the signed-in account email.
3. Tap the source card and select folders. Folder selections save immediately.
4. Source cards reuse the saved selected-folder count while repositories refresh media counts in the background.
5. Use **Re-authenticate** from a remote source card or folder browser menu if a cloud token expires.
6. Tap **Open Screensaver Settings**, select **Android Screensaver**, then return to the app.
7. Adjust slideshow, video, schedule, overlay, and cache settings from the app settings menu.

## Testing

Run the fail-fast host and optional device suite from the repository root:

```text
python tools/run_tests.py
```

The runner executes project-structure checks, `assembleDebug`, JVM tests, and
then ADB smoke checks when an authorized device is available. Use
`python tools/run_tests.py --device on` when a device is required; this fails
during preflight if ADB is unavailable or no authorized device is connected.
Use `--device off` for a host-only run. Timestamped command logs and both text
and JSON end-of-run reports are written under `build/test-reports/`. Each
console line uses the compact form `[HH:MM:SS + elapsed]`, where `elapsed` is
the cumulative suite runtime, and the complete report is printed after the
final stage. Use `--clean-install` only when a signing mismatch requires
uninstalling the exact debug package; this deletes that package's device data.
Use `--suspects` to maintain the default failed-check cache at
`build/test-reports/suspects.json`, or provide a JSON path after the flag. A
failed check is added, a passing check is removed, and skipped checks remain
unchanged.

The JVM suite covers model/account aggregation, source authentication state,
folder-navigation ViewModel behavior, shared repository account routing and
cache invalidation, settings account serialization, base account-manager state,
media classification, and slideshow sort keys. Separate host-side Python tests
cover runner argument parsing, report generation, device parsing, and suspects
cache updates. Provider OAuth, MediaStore permissions, DreamService lifecycle,
and TV UI behavior still require instrumentation or device verification.

## Tech Stack

- **Language:** Kotlin
- **Architecture:** MVVM with Repository pattern
- **DI:** Hilt
- **Async:** Kotlin Coroutines + Flow
- **Storage:** DataStore Preferences
- **Image Loading:** Coil
- **Video:** Media3/ExoPlayer
- **Build:** Android Gradle Plugin 9.2.1 with Gradle 9.5.0
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
| [BUILD.md](docs/BUILD.md) | Build commands, OAuth setup notes, and troubleshooting |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Architecture, design patterns, key classes, and settings reference |
| [CODING_STANDARDS.md](docs/CODING_STANDARDS.md) | Kotlin/Android coding standards, review checklist, and verification expectations |
| [CHANGELOG.md](docs/CHANGELOG.md) | Release history and completed changes |
| [TODO.md](docs/TODO.md) | Active backlog and quality checklist |
| [USER_GUIDE.md](docs/USER_GUIDE.md) | User-facing setup and usage guide |
| [tools/README.md](tools/README.md) | Test runner usage, device modes, and report output |
| [AGENTS.md](AGENTS.md) | Developer roles, documentation rules, and git restrictions |

`PROGRESS.md` was removed because it duplicated `docs/TODO.md` and `docs/CHANGELOG.md`. Use `docs/TODO.md` for current/planned work and `docs/CHANGELOG.md` for completed change history.

## License

See [LICENSE](LICENSE).
