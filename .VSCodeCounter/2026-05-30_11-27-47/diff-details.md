# Diff Details

Date : 2026-05-30 11:27:47

Directory e:\\coding_workspaces\\Kotlin\\AndroidScreensaver

Total : 25 files,  17 codes, 8 comments, 19 blanks, all 44 lines

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [ARCHITECTURE.md](/ARCHITECTURE.md) | Markdown | 6 | 0 | 0 | 6 |
| [CHANGELOG.md](/CHANGELOG.md) | Markdown | 11 | 0 | 0 | 11 |
| [README.md](/README.md) | Markdown | 1 | 0 | 0 | 1 |
| [TODO.md](/TODO.md) | Markdown | -18 | 0 | -4 | -22 |
| [USER\_GUIDE.md](/USER_GUIDE.md) | Markdown | 1 | 0 | 0 | 1 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/AbstractPhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/AbstractPhotoRepository.kt) | Kotlin | 33 | 6 | 7 | 46 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/BaseCloudPhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/BaseCloudPhotoRepository.kt) | Kotlin | 38 | 11 | 13 | 62 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxPhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxPhotoRepository.kt) | Kotlin | -35 | -3 | -10 | -48 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxRepository.kt) | Kotlin | 3 | 3 | 1 | 7 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GalleryPhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GalleryPhotoRepository.kt) | Kotlin | 54 | -11 | 2 | 45 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GoogleDrivePhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GoogleDrivePhotoRepository.kt) | Kotlin | -8 | -15 | -2 | -25 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/PhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/PhotoRepository.kt) | Kotlin | 1 | 3 | 1 | 5 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/WeatherRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/WeatherRepository.kt) | Kotlin | 1 | 0 | 0 | 1 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/dream/SlideshowManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/dream/SlideshowManager.kt) | Kotlin | 5 | 0 | 1 | 6 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/main/MainActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/main/MainActivity.kt) | Kotlin | -61 | -2 | -3 | -66 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/FolderAdapter.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/FolderAdapter.kt) | Kotlin | -9 | -6 | 0 | -15 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/FolderBrowserActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/FolderBrowserActivity.kt) | Kotlin | -3 | 0 | 1 | -2 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/GalleryFolderBrowserActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/GalleryFolderBrowserActivity.kt) | Kotlin | -4 | -1 | 0 | -5 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/BaseAccountManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/BaseAccountManager.kt) | Kotlin | 21 | 25 | 10 | 56 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/DropboxAccountManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/DropboxAccountManager.kt) | Kotlin | -5 | -4 | -1 | -10 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/GoogleAccountManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/GoogleAccountManager.kt) | Kotlin | -4 | -4 | -1 | -9 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/SettingsManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/SettingsManager.kt) | Kotlin | 5 | 1 | 1 | 7 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/GalleryViewModel.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/GalleryViewModel.kt) | Kotlin | -37 | -2 | -2 | -41 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/GoogleDriveViewModel.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/GoogleDriveViewModel.kt) | Kotlin | -21 | 1 | 0 | -20 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/MainViewModel.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/MainViewModel.kt) | Kotlin | 42 | 6 | 5 | 53 |

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details