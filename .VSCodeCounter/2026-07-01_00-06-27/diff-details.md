# Diff Details

Date : 2026-07-01 00:06:27

Directory e:\\coding_workspaces\\Kotlin\\AndroidScreensaver

Total : 63 files,  2139 codes, -966 comments, 149 blanks, all 1322 lines

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [ARCHITECTURE.md](/ARCHITECTURE.md) | Markdown | 4 | 0 | 0 | 4 |
| [BUILD.md](/BUILD.md) | Markdown | 14 | 0 | 2 | 16 |
| [CHANGELOG.md](/CHANGELOG.md) | Markdown | 31 | 0 | 0 | 31 |
| [CODING\_STANDARDS.md](/CODING_STANDARDS.md) | Markdown | 1 | 0 | 0 | 1 |
| [OneDriveAuthManager.kt](/OneDriveAuthManager.kt) | Kotlin | 0 | 0 | 1 | 1 |
| [TODO.md](/TODO.md) | Markdown | 3 | 0 | 1 | 4 |
| [USER\_GUIDE.md](/USER_GUIDE.md) | Markdown | 2 | 0 | 0 | 2 |
| [app/build.gradle.kts](/app/build.gradle.kts) | Kotlin | 4 | 3 | 3 | 10 |
| [app/src/main/AndroidManifest.xml](/app/src/main/AndroidManifest.xml) | XML | 17 | 2 | 2 | 21 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ScreensaverApplication.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ScreensaverApplication.kt) | Kotlin | 4 | -4 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/auth/OneDriveAuthManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/auth/OneDriveAuthManager.kt) | Kotlin | 195 | 4 | 30 | 229 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/model/FolderError.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/model/FolderError.kt) | Kotlin | 15 | -10 | 0 | 5 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/model/Models.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/model/Models.kt) | Kotlin | 199 | -197 | 0 | 2 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/AbstractPhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/AbstractPhotoRepository.kt) | Kotlin | 4 | -4 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/BaseCloudPhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/BaseCloudPhotoRepository.kt) | Kotlin | 7 | -7 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxPhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxPhotoRepository.kt) | Kotlin | 4 | -1 | 0 | 3 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/DropboxRepository.kt) | Kotlin | 35 | -35 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GalleryPhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GalleryPhotoRepository.kt) | Kotlin | 13 | -13 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GoogleDrivePhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GoogleDrivePhotoRepository.kt) | Kotlin | 35 | -33 | 0 | 2 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GoogleDriveRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/GoogleDriveRepository.kt) | Kotlin | 47 | -41 | 1 | 7 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/OneDrivePhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/OneDrivePhotoRepository.kt) | Kotlin | 346 | 1 | 53 | 400 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/PhotoRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/PhotoRepository.kt) | Kotlin | 38 | -38 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/WeatherRepository.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/data/repository/WeatherRepository.kt) | Kotlin | 21 | -20 | 0 | 1 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/di/NetworkModule.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/di/NetworkModule.kt) | Kotlin | 21 | 0 | 3 | 24 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/dream/PhotoScreensaverService.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/dream/PhotoScreensaverService.kt) | Kotlin | 48 | -10 | 3 | 41 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/dream/SlideshowManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/dream/SlideshowManager.kt) | Kotlin | 44 | -39 | 0 | 5 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/ScreensaverPreviewActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/ScreensaverPreviewActivity.kt) | Kotlin | 11 | -11 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/about/AboutActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/about/AboutActivity.kt) | Kotlin | 4 | -4 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/main/MainActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/main/MainActivity.kt) | Kotlin | 69 | -6 | 4 | 67 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/main/SourceType.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/main/SourceType.kt) | Kotlin | 4 | -4 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/DebugSettingsActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/DebugSettingsActivity.kt) | Kotlin | 41 | -3 | 4 | 42 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/DecorationSettingsActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/DecorationSettingsActivity.kt) | Kotlin | 43 | -9 | 6 | 40 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/PhotoInfoSettingsActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/PhotoInfoSettingsActivity.kt) | Kotlin | 29 | -2 | 5 | 32 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/ScheduleSettingsActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/ScheduleSettingsActivity.kt) | Kotlin | 13 | -5 | 1 | 9 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/ScreensaverSettingsActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/ScreensaverSettingsActivity.kt) | Kotlin | 4 | -4 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/SettingsActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/SettingsActivity.kt) | Kotlin | 4 | -4 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/SettingsFragment.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/SettingsFragment.kt) | Kotlin | 78 | -31 | 11 | 58 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/VideoPlaybackSettingsActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/settings/VideoPlaybackSettingsActivity.kt) | Kotlin | -19 | -7 | -3 | -29 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/NoSourcesView.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/NoSourcesView.kt) | Kotlin | 3 | -3 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/SlideshowView.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/slideshow/SlideshowView.kt) | Kotlin | 94 | -78 | 2 | 18 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/DropboxAuthActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/DropboxAuthActivity.kt) | Kotlin | 47 | -4 | 7 | 50 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/FolderAdapter.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/FolderAdapter.kt) | Kotlin | 15 | -12 | 1 | 4 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/FolderBrowserActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/FolderBrowserActivity.kt) | Kotlin | 3 | -3 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/GalleryFolderBrowserActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/GalleryFolderBrowserActivity.kt) | Kotlin | 33 | -7 | 3 | 29 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/GoogleDriveAuthActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/GoogleDriveAuthActivity.kt) | Kotlin | 82 | -4 | 8 | 86 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/OneDriveAuthActivity.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/ui/sources/OneDriveAuthActivity.kt) | Kotlin | 55 | 4 | 11 | 70 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/BaseAccountManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/BaseAccountManager.kt) | Kotlin | 25 | -25 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/DropboxAccountManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/DropboxAccountManager.kt) | Kotlin | 44 | -35 | 0 | 9 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/DropboxOAuthConfig.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/DropboxOAuthConfig.kt) | Kotlin | 14 | -14 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/GoogleAccountManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/GoogleAccountManager.kt) | Kotlin | 11 | -53 | -6 | -48 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/GoogleOAuthConfig.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/GoogleOAuthConfig.kt) | Kotlin | -12 | -34 | -8 | -54 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/SecureLinks.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/SecureLinks.kt) | Kotlin | 11 | -11 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/SettingsManager.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/SettingsManager.kt) | Kotlin | 139 | -48 | 0 | 91 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/utils/VersionUtils.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/utils/VersionUtils.kt) | Kotlin | 18 | -18 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/CloudFolderViewModel.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/CloudFolderViewModel.kt) | Kotlin | 34 | -34 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/GalleryViewModel.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/GalleryViewModel.kt) | Kotlin | 31 | -31 | 0 | 0 |
| [app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/MainViewModel.kt](/app/src/main/java/com/vincentwetzel/androidscreensaver/viewmodel/MainViewModel.kt) | Kotlin | 19 | -19 | 0 | 0 |
| [app/src/main/res/xml/settings\_main.xml](/app/src/main/res/xml/settings_main.xml) | XML | 14 | 0 | 2 | 16 |
| [app/src/test/java/com/vincentwetzel/androidscreensaver/DataModelUnitTest.kt](/app/src/test/java/com/vincentwetzel/androidscreensaver/DataModelUnitTest.kt) | Kotlin | 3 | -3 | 0 | 0 |
| [app/src/test/java/com/vincentwetzel/androidscreensaver/ViewModelUnitTest.kt](/app/src/test/java/com/vincentwetzel/androidscreensaver/ViewModelUnitTest.kt) | Kotlin | 4 | -4 | 0 | 0 |
| [gradle/wrapper/gradle-wrapper.properties](/gradle/wrapper/gradle-wrapper.properties) | Properties | 0 | 1 | 1 | 2 |
| [msal\_auth\_config.json](/msal_auth_config.json) | JSON | 16 | 0 | 0 | 16 |
| [settings.gradle.kts](/settings.gradle.kts) | Kotlin | 3 | 1 | 1 | 5 |

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details