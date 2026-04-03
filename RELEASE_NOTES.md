# Release Notes - v1.6.0

## Android Screensaver
**Release Date**: April 3, 2026
**Version**: 1.6.0 (build 7)
**Min SDK**: Android 8.0 (API 26)
**Target SDK**: Android 14 (API 34)

---

## What's New

### ✨ Complete Feature Set
This release marks the **first stable version** with all core features implemented:

#### Google Drive Integration
- ✅ Full OAuth2 authentication flow
- ✅ Folder browser with search and selection
- ✅ Photo and video fetching from Drive
- ✅ Metadata extraction (dimensions, date, size)
- ✅ Multi-folder support with subfolder inclusion

#### Slideshow Engine
- ✅ 5 display effects (Pan, Scale, Crop, Zoom, Focus)
- ✅ 15 transition effects (Fade, Cross-fade, Slide, Wipe, etc.)
- ✅ Configurable timing (3 seconds to 5 minutes)
- ✅ Shuffle and ordered display modes
- ✅ Video playback with ExoPlayer

#### Comprehensive Settings (13 Categories)
1. **Sources** - Google Drive folder selection
2. **Media & Content** - Filter and order photos
3. **Slideshow** - Display time and video settings
4. **Display & Transitions** - Visual effects
5. **Decorations** - Date, clock, and weather overlays
6. **Photo Information** - Metadata display
7. **Schedule & Timer** - Autostart/autostop scheduling
8. **Display & Power** - Screen rotation and keep-alive
9. **Sync & Network** - Sync intervals and Wi-Fi only
10. **Appearance** - Background color and cache settings
11. **Advanced** - Power management and exit triggers
12. **About** - App info and links
13. **Debug Mode** - Development tools (tap version 7 times)

#### Weather Integration
- ✅ Uses Open-Meteo API (100% free, no API key needed)
- ✅ Global coverage
- ✅ Temperature, conditions, humidity, wind, precipitation
- ✅ Customizable units (Fahrenheit/Celsius)

#### Scheduling
- ✅ Autostart at specific times
- ✅ Autostop at specific times
- ✅ Day-of-week selection
- ✅ Quick presets (Weekdays, Weekends, Every Day)
- ✅ "Only when charging" option

---

## Improvements

### From Previous Versions

**v1.5.0 → v1.6.0**
- Added background opacity slider for photo info
- Added text opacity slider for photo info
- Added text shadow toggle with intensity control
- Added custom cache size input (10-10,000 MB)
- Completed all deferred Phase 6 items

**v1.4.0 → v1.5.0**
- Implemented photo information settings screen
- Added schedule settings (autostart/autostop)
- Added debug mode with diagnostics
- Implemented color picker dialog

**v1.3.0 → v1.4.0**
- Schedule settings with time picker
- Day-of-week checkboxes
- Quick presets for scheduling

**v1.2.0 → v1.3.0**
- Photo information settings screen
- Field toggles for metadata display
- Appearance customization options

**v1.1.0 → v1.2.0**
- Video playback settings screen
- Audio mode selection
- Display mode options

**v1.0.0 → v1.1.0**
- Folder browser with checkboxes
- About screen with links
- Weather integration
- Settings persistence

---

## Known Limitations

### Current Version
1. **Single Source Only**: Only Google Drive is supported in v1.6.0
   - *Coming in v1.7+*: Gallery, Dropbox, Google Photos, OneDrive, Local Network

2. **Photo Caching**: Basic preloading implemented
   - *Coming in v1.7+*: Advanced cache management with statistics

3. **Transitions**: Basic effects implemented
   - *Coming in v1.7+*: Advanced transitions (Cube, Doorway, Ripple, etc.)

4. **TV Optimization**: Basic support for NVidia Shield
   - *Coming in v1.7+*: Full TV-optimized layouts

5. **Preview**: No preview mode for effects
   - *Coming in v1.7+*: Live preview for transitions and decorations

---

## System Requirements

- **Android Version**: 8.0 or higher
- **Storage**: 50 MB for app + cache space
- **RAM**: 2 GB minimum recommended
- **Network**: Wi-Fi recommended for photo loading

---

## Installation

### From GitHub
1. Download the APK from the Releases page
2. Enable "Install from unknown sources" in settings
3. Install the APK
4. Open the app and follow the setup wizard

### From Android Studio (Development)
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on device or emulator

---

## Upgrade Notes

### From Beta Versions
- Settings will be preserved
- Google Drive authentication persists
- Selected folders are maintained

### Clean Install
- No data is imported
- Start fresh with Google Drive connection
- Configure settings from defaults

---

## Feedback

We'd love to hear from you!

- **Report Issues**: https://github.com/vincentwetzel/AndroidScreensaver/issues
- **Feature Requests**: https://github.com/vincentwetzel/AndroidScreensaver/issues
- **Discord Community**: Link available in app (About screen)
- **Rate the App**: If published to Play Store

---

## Credits

### Developer
- Vincent Wetzel - Lead Developer

### Technologies
- Kotlin - Programming language
- Hilt - Dependency injection
- Coil - Image loading
- ExoPlayer (Media3) - Video playback
- Open-Meteo - Weather API
- Google Drive API - Photo source

### Libraries
- AndroidX Core, Lifecycle, Navigation
- Material Design Components
- DataStore - Settings persistence
- Room - Database (future use)

---

## Legal

### License
MIT License - See LICENSE file for details

### Privacy
This app does NOT collect personal data. All settings and configurations are stored locally on your device.

### Open Source
Full source code available at:
https://github.com/vincentwetzel/AndroidScreensaver

---

**Thank you for using Android Screensaver!** 📸✨
