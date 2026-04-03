# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Additional cloud storage providers (Dropbox, OneDrive, etc.)
- TV-optimized layouts for NVidia Shield
- Performance optimizations
- Preview functionality for effects/transitions

---

## [1.8.0] - 2026-04-03

### TV Optimization & Release Preparation
- **NVidia Shield / Android TV Support**
  - TV-optimized layout (activity_main_tv.xml)
  - Larger cards, bigger buttons for 10-foot UI
  - Dark theme optimized for TV environments
  - Auto-detection of TV vs phone/tablet
  - Focusable UI elements for D-pad navigation
- **Release Preparation**
  - LICENSE file (MIT)
  - CONTRIBUTING.md with guidelines
  - Updated .gitignore (google-services.json, keystores)
  - RELEASE_NOTES.md updated

### Updated
- **Version**: 1.7.0 → 1.8.0, build 8 → 9
- App ready for GitHub release preparation

---

## [1.7.0] - 2026-04-03

### Testing & Documentation
- **Unit Tests**
  - ViewModel unit tests (MainViewModel, GoogleDriveViewModel)
  - Data model unit tests (Photo, PhotoFolder, SlideshowConfig, etc.)
  - Repository unit tests (GoogleDriveRepository)
- **Documentation**
  - User Guide (USER_GUIDE.md) - Complete usage instructions
  - Release Notes (RELEASE_NOTES.md) - Version history and known issues
  - Updated README with feature list
  - Updated TODO with completion status

### Improvements
- Added entrance animation to main menu header
- Improved error messages in Google Drive authentication
- Enhanced settings persistence coverage

### Updated
- **Version**: 1.6.0 → 1.7.0, build 7 → 8
- App ready for device testing and release preparation

---

## [1.6.0] - 2026-04-03

### Completed Deferred Items
- **Photo Info Settings - Complete**
  - Background opacity slider (0-100%)
  - Text opacity slider (0-100%)
  - Text shadow toggle
  - Shadow intensity selector (Light, Medium, Heavy)
- **Cache Settings - Complete**
  - Custom cache size input dialog (10-10,000 MB with validation)
  - Numeric keyboard input
  - Error handling for invalid values

### Updated
- **Version**: 1.5.0 → 1.6.0, build 6 → 7
- All deferred items from Phase 6 now completed
- Settings screen fully functional with no deferred features

---

## [1.5.0] - 2026-04-03

### Added
- **Photo Information Settings** - Metadata overlay during slideshow
  - 7 field toggles (file name, folder, date, source, description, dimensions, file size)
  - Fade out duration presets
  - 6 position options, 3 layouts, 5 separators
  - 4 background styles
- **Schedule Settings** - Autostart/autostop scheduling
  - Time picker with AM/PM display
  - Quick presets: Weekdays, Weekends, Every Day
  - Day of week checkboxes
  - Repeat weekly toggle
  - Only when charging toggle (autostart)
- **Debug Mode** - Development tools
  - Tap version 7 times to access
  - Debug overlay toggle
  - Export logs button
  - Reset all settings with confirmation
  - Test crash reporting
  - System information display
- **Color Picker** - Background color selection
  - 14 preset colors
  - Live preview with hex display
  - Saves to settings persistence

### Updated
- **Version**: 1.4.0 → 1.5.0, build 5 → 6
- Settings screen now fully functional with all categories

---

## [1.1.0] - 2026-04-03

### Added
- **Folder Browser** - Complete UI for selecting Google Drive folders
  - RecyclerView with checkboxes
  - Search/filter functionality
  - Select All/Deselect All buttons
  - Include subfolders toggle
  - Summary card showing folder count and photo count
- **About Screen** - App information and links
  - Version display (read from build.gradle)
  - GitHub repository link
  - Discord community link (obfuscated URL)
  - MIT License link
  - Privacy policy link
- **Weather Integration** - Open-Meteo API (100% free, no key needed)
  - WeatherRepository with global coverage
  - Temperature, condition, humidity, wind, precipitation
  - WMO weather code to human-readable conditions
- **Settings Persistence** - DataStore layer
  - SettingsManager for all preferences
  - Slideshow config save/load
  - Source enabled/disabled state persistence
  - Folder selection persistence

### Updated
- **Main Menu** - Google Drive card now opens folder browser after auth
- **Settings Screen** - All 13 categories wired to launch respective screens
- **Version**: 1.0.0 → 1.1.0, build 1 → 2

### Fixed
- OAuth2 flow properly integrated with folder browser
- Navigation between screens working correctly

---

## [1.0.0] - 2026-04-03

### Initial Release
- Project scaffolding and documentation
- Basic Android project structure with Gradle
- Data models and settings configuration
- Architecture design for multi-source support
- Google Drive selected as primary source for v1.0

### Added
- Comprehensive settings specification (SETTINGS.md)
- MVVM architecture with Repository pattern
- All data models for photos, sources, and configurations
- Hilt dependency injection setup
- ProGuard rules for release builds

---

## Version History Summary

| Version | Date | Description |
|---------|------|-------------|
| 1.8.0 | 2026-04-03 | TV optimization, release preparation |
| 1.7.0 | 2026-04-03 | Unit tests, documentation, animations |
| 1.6.0 | 2026-04-03 | Completed all deferred items (opacity, shadow, cache input) |
| 1.5.0 | 2026-04-03 | Photo info, Schedule, Debug mode, Color picker |
| 1.4.0 | 2026-04-03 | Schedule settings (autostart/autostop) |
| 1.3.0 | 2026-04-03 | Photo information settings |
| 1.2.0 | 2026-04-03 | Video playback settings |
| 1.1.0 | 2026-04-03 | Folder browser, About screen, Weather, Settings persistence |
| 1.0.0 | 2026-04-03 | Initial project setup and documentation |

---

## Version Update Guidelines

When updating the version in `app/build.gradle.kts`:

### Major Version (X.0.0)
- Breaking changes to settings or API
- Major architectural changes
- First stable release after beta

### Minor Version (0.X.0)
- New feature added (e.g., Google Drive integration)
- Existing feature completed/updated
- New settings section added

### Patch Version (0.0.X)
- Bug fixes
- UI improvements
- Documentation updates
- Minor tweaks

### Build Code
- Increment by 1 for EVERY update
- Used by Play Store for version tracking
- Never reuse build codes
