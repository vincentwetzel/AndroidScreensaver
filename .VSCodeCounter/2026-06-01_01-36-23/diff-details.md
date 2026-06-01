# Diff Details

Date : 2026-06-01 01:36:23

Directory e:\\coding_workspaces\\Kotlin\\AndroidScreensaver

Total : 47 files,  793 codes, 26 comments, 115 blanks, all 934 lines

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [AGENTS.md](/AGENTS.md) | Markdown | 2 | 0 | 0 | 2 |
| [ARCHITECTURE.md](/ARCHITECTURE.md) | Markdown | 15 | 0 | 3 | 18 |
| [BUILD.md](/BUILD.md) | Markdown | 10 | 0 | 5 | 15 |
| [CHANGELOG.md](/CHANGELOG.md) | Markdown | 39 | 0 | 0 | 39 |
| [CODING\_STANDARDS.md](/CODING_STANDARDS.md) | Markdown | 93 | 0 | 22 | 115 |
| [README.md](/README.md) | Markdown | 4 | 0 | 0 | 4 |
| [TODO.md](/TODO.md) | Markdown | 1 | 0 | 0 | 1 |
| [USER\_GUIDE.md](/USER_GUIDE.md) | Markdown | 8 | 0 | 4 | 12 |
| [app/build.gradle.kts](/app/build.gradle.kts) | Kotlinscript | 9 | 1 | 2 | 12 |
| [app/src/main/AndroidManifest.xml](/app/src/main/AndroidManifest.xml) | XML | 3 | 0 | 1 | 4 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/model/FolderError.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/model/FolderError.kt) | Kotlin | 2 | 0 | 0 | 2 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/AbstractPhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/AbstractPhotoRepository.kt) | Kotlin | 7 | 0 | 1 | 8 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/BaseCloudPhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/BaseCloudPhotoRepository.kt) | Kotlin | 50 | -1 | 9 | 58 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxPhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxPhotoRepository.kt) | Kotlin | -47 | 0 | -6 | -53 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/FolderBrowserActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/FolderBrowserActivity.kt) | Kotlin | 0 | 0 | 1 | 1 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GalleryFolderBrowserActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GalleryFolderBrowserActivity.kt) | Kotlin | 0 | 0 | 1 | 1 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GalleryPhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GalleryPhotoRepository.kt) | Kotlin | 15 | 0 | 1 | 16 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GoogleDrivePhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GoogleDrivePhotoRepository.kt) | Kotlin | -3 | -8 | -5 | -16 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GoogleDriveRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GoogleDriveRepository.kt) | Kotlin | 3 | 3 | 1 | 7 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/di/RepositoryModule.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/di/RepositoryModule.kt) | Kotlin | -52 | -3 | -7 | -62 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/dream/PhotoScreensaverService.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/dream/PhotoScreensaverService.kt) | Kotlin | 9 | 0 | 0 | 9 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/dream/SlideshowManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/dream/SlideshowManager.kt) | Kotlin | -3 | -1 | -1 | -5 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/service/ScheduleService.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/service/ScheduleService.kt) | Kotlin | 5 | 0 | 0 | 5 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/ScreensaverPreviewActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/ScreensaverPreviewActivity.kt) | Kotlin | 4 | 0 | 1 | 5 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/main/MainActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/main/MainActivity.kt) | Kotlin | 54 | 3 | 6 | 63 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/main/SourceType.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/main/SourceType.kt) | Kotlin | -4 | 0 | 0 | -4 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/DebugSettingsActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/DebugSettingsActivity.kt) | Kotlin | 32 | 0 | 2 | 34 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/DecorationSettingsActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/DecorationSettingsActivity.kt) | Kotlin | 25 | -1 | 7 | 31 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/PhotoInfoSettingsActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/PhotoInfoSettingsActivity.kt) | Kotlin | 12 | 0 | 1 | 13 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/ScheduleSettingsActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/ScheduleSettingsActivity.kt) | Kotlin | 29 | 0 | 1 | 30 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/SettingsFragment.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/SettingsFragment.kt) | Kotlin | 57 | 1 | 1 | 59 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/VideoPlaybackSettingsActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/VideoPlaybackSettingsActivity.kt) | Kotlin | 9 | 0 | 1 | 10 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/SlideshowView.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/SlideshowView.kt) | Kotlin | 28 | 3 | 4 | 35 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/DropboxAuthActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/DropboxAuthActivity.kt) | Kotlin | 112 | 12 | 23 | 147 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/FolderAdapter.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/FolderAdapter.kt) | Kotlin | 3 | 0 | 0 | 3 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/FolderBrowserActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/FolderBrowserActivity.kt) | Kotlin | 130 | 9 | 20 | 159 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/GalleryFolderBrowserActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/GalleryFolderBrowserActivity.kt) | Kotlin | 11 | 0 | 2 | 13 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/GoogleDriveAuthActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/GoogleDriveAuthActivity.kt) | Kotlin | 11 | 1 | 3 | 15 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/DropboxAccountManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/DropboxAccountManager.kt) | Kotlin | 32 | 1 | 2 | 35 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/DropboxOAuthConfig.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/DropboxOAuthConfig.kt) | Kotlin | 0 | -2 | 0 | -2 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/GoogleAccountManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/GoogleAccountManager.kt) | Kotlin | 4 | 2 | 1 | 7 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/SettingsManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/SettingsManager.kt) | Kotlin | 13 | 5 | 2 | 20 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/CloudFolderViewModel.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/CloudFolderViewModel.kt) | Kotlin | 131 | 41 | 28 | 200 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/GalleryViewModel.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/GalleryViewModel.kt) | Kotlin | 15 | 1 | 1 | 17 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/GoogleDriveViewModel.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/GoogleDriveViewModel.kt) | Kotlin | -106 | -39 | -25 | -170 |
| [app/src/main/res/layout/activity\_dropbox\_auth.xml](/app/src/main/res/layout/activity_dropbox_auth.xml) | XML | 40 | 0 | 6 | 46 |
| [test.py](/test.py) | Python | -19 | -2 | -4 | -25 |

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details