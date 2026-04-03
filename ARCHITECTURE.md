# Architecture

## Overview

This project follows **MVVM (Model-View-ViewModel)** architecture with a **Repository pattern** for data management.

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Activities │  │  Fragments   │  │    Views     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                          │                                   │
│                     (Observers)                              │
└──────────────────────────┼──────────────────────────────────┘
                           │
┌──────────────────────────┼──────────────────────────────────┐
│                  ViewModel Layer                            │
│  ┌──────────────────────┴──────────────────────────┐       │
│  │              ViewModels                         │       │
│  │  - MainViewModel                                │       │
│  │  - SourceViewModel                              │       │
│  │  - SettingsViewModel                            │       │
│  │  - ScreensaverViewModel                         │       │
│  └─────────────────────────────────────────────────┘       │
│                          │                                   │
│                     (LiveData/Flow)                         │
└──────────────────────────┼──────────────────────────────────┘
                           │
┌──────────────────────────┼──────────────────────────────────┐
│                  Repository Layer                           │
│  ┌──────────────────────┴──────────────────────────┐       │
│  │           PhotoRepository                       │       │
│  │           AuthRepository                        │       │
│  │           SettingsRepository                    │       │
│  └─────────────────────────────────────────────────┘       │
│                          │                                   │
└──────────────────────────┼──────────────────────────────────┘
                           │
┌──────────────────────────┼──────────────────────────────────┐
│                   Data Sources                              │
│  ┌─────────┐ ┌────────┐ ┌──────────┐ ┌──────────────┐     │
│  │ Gallery │ │Dropbox │ │Google    │ │Local Network │     │
│  │         │ │        │ │Drive/Photos│ │(SMB/WebDAV) │     │
│  └─────────┘ └────────┘ └──────────┘ └──────────────┘     │
│  ┌──────────┐                                              │
│  │ OneDrive │                                              │
│  └──────────┘                                              │
└─────────────────────────────────────────────────────────────┘
```

## Modules

```
app/
├── main/
│   ├── java/com/vincentwetzel/androidscreensaver/
│   │   ├── ui/
│   │   │   ├── main/           # Main menu activity
│   │   │   ├── sources/        # Source selection & auth
│   │   │   │   ├── SourceListFragment        # List of all sources
│   │   │   │   ├── GoogleDriveAuthFragment   # Google Drive auth
│   │   │   │   ├── GoogleDriveFolderBrowser  # Folder picker
│   │   │   │   └── ComingSoonFragment        # Placeholder for future sources
│   │   │   ├── settings/       # Settings screens
│   │   │   │   ├── SettingsActivity
│   │   │   │   ├── folder/     # Folder selection UI
│   │   │   │   ├── slideshow/  # Slideshow settings
│   │   │   │   ├── display/    # Display effects settings
│   │   │   │   ├── transitions/# Transition settings
│   │   │   │   ├── decorations/# Date, clock, weather settings
│   │   │   │   ├── schedule/   # Schedule & timer settings
│   │   │   │   ├── network/    # Sync & network settings
│   │   │   │   └── about/      # About screen
│   │   │   └── screensaver/    # Screensaver preview
│   │   ├── viewmodel/
│   │   │   ├── MainViewModel
│   │   │   ├── SourceViewModel
│   │   │   ├── GoogleDriveViewModel
│   │   │   ├── SettingsViewModel
│   │   │   └── ScreensaverViewModel
│   │   ├── repository/
│   │   │   ├── PhotoRepository (interface)
│   │   │   ├── GoogleDriveRepository (implementation)
│   │   │   ├── AuthRepository
│   │   │   └── SettingsRepository
│   │   ├── data/
│   │   │   ├── model/          # Data classes
│   │   │   ├── remote/         # API clients
│   │   │   │   └── GoogleDriveApi
│   │   │   └── local/          # Room database, preferences
│   │   ├── di/                 # Hilt modules
│   │   ├── dream/              # DreamService implementation
│   │   └── utils/              # Utility classes
│   └── res/
│       ├── layout/
│       ├── values/
│       ├── drawable/
│       ├── navigation/
│       └── xml/
├── test/                       # Unit tests
└── androidTest/                # Instrumented tests
```

## Implementation Phases

### Phase 1: Google Drive Only (v1.0)
- Google Drive authentication and folder browsing
- Photo fetching from Google Drive
- All slideshow and settings features
- DreamService implementation
- Release-ready for NVidia Shield and all Android devices

### Phase 2: Additional Sources (v1.1+)
- Local Gallery
- Dropbox
- Google Photos
- OneDrive
- Local Network (SMB/WebDAV)

The architecture is designed to be source-agnostic, making it easy to add new sources by:
1. Implementing the `PhotoRepository` interface
2. Adding OAuth2/authentication flow
3. Creating UI fragments for folder browsing
4. Registering in the source factory

## Key Components

### DreamService (Screensaver)
- Extends `android.service.dreams.DreamService`
- Handles slideshow display when device enters screensaver mode
- Full-screen image display with transitions

### Repository Pattern
- Single source of truth for data
- Abstracts data source complexity
- Provides clean API to ViewModels

### Dependency Injection
- Hilt for DI
- Provides scoped instances of repositories, APIs, and use cases

### Data Flow
- Cloud SDKs → Repository → ViewModel → UI
- Kotlin Flow for async data streams
- LiveData for UI state observation

## Design Patterns

| Pattern | Usage |
|---------|-------|
| MVVM | UI architecture |
| Repository | Data abstraction |
| Singleton | Shared resources |
| Observer | Reactive UI updates |
| Factory | ViewModel creation |

## Dependencies

- **AndroidX Core**: Activity, Fragment, Lifecycle, Navigation
- **UI**: Material Design, ConstraintLayout
- **DI**: Hilt
- **Async**: Coroutines, Flow
- **Image Loading**: Coil
- **Storage**: Room (for caching), DataStore (preferences)
- **Testing**: JUnit, Mockito, Espresso
