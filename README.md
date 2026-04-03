# AndroidScreensaver

A photo screensaver app for Android devices, optimized for NVidia Shield but compatible with all Android devices.

## Overview

AndroidScreensaver allows users to create a slideshow screensaver from multiple photo sources including:
- Local gallery
- Dropbox
- Google Drive
- Google Photos
- OneDrive
- Local network (SMB/WebDAV)

## Features

- **Multi-source photo access**: Authenticate and browse photos from various cloud storage providers
- **Folder selection**: Select specific folders/subfolders from each source with a hierarchical tree view
- **Customizable slideshow**: Configure display timing, effects, and transitions
- **Daydream integration**: Uses Android's built-in DreamService for screensaver functionality
- **Material Design UI**: Clean, modern interface following Android design guidelines

### v1.0 Focus: Google Drive
The initial release will focus exclusively on **Google Drive** as the photo source to ensure a polished, stable experience. Future updates will add support for additional sources including Gallery, Dropbox, Google Photos, OneDrive, and local network storage.

## Tech Stack

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Async**: Kotlin Coroutines & Flow
- **Image Loading**: Coil

## Cloud Storage APIs

### v1.0 (Implemented First)
- **Google Drive**: Google Play Services / Google Sign-In / Google Drive API

### Future Releases (v1.1+)
- **Dropbox**: Dropbox SDK
- **Google Photos**: Google Photos API
- **OneDrive**: Microsoft Graph SDK
- **Local Network**: jCIFS (SMB) / Sardine (WebDAV)
- **Local Gallery**: Android MediaStore API

## Getting Started

1. Clone the repository
2. **Verify Google Cloud Setup** (see `GOOGLE_CLOUD_SETUP.md`):
   - Ensure Google Drive API is enabled
   - Ensure OAuth consent screen is configured with `drive.readonly` scope
   - OAuth Client ID is already configured in the code
3. Open in Android Studio
4. Sync Gradle and build

```bash
git clone https://github.com/vincentwetzel/AndroidScreensaver.git
cd AndroidScreensaver
```

## Quick Links

- 📋 **TODO List**: See `TODO.md` for implementation roadmap
- ⚙️ **Settings Spec**: See `SETTINGS.md` for all app settings
- 🏗️ **Architecture**: See `ARCHITECTURE.md` for technical design
- ☁️ **Google Cloud Setup**: See `GOOGLE_CLOUD_SETUP.md` for OAuth configuration
- 🎨 **Resources to Update**: See `RESOURCES_TO_UPDATE.md` for icon placeholders
- ✅ **Pre-Build Checklist**: See `PRE_BUILD_CHECKLIST.md` for configuration status
- 📊 **Project Summary**: See `PROJECT_SUMMARY.md` for complete overview
- 📝 **Changelog**: See `CHANGELOG.md` for version history

## License

MIT License
