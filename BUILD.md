# Build Instructions

## Requirements

- Android Studio Hedgehog or newer
- JDK 17 (configured in project)
- Android SDK 34 (compileSdk), minSdk 26
- Android Gradle Plugin 9.2.1 with Gradle 9.4.1 wrapper

## Build Commands

### Debug Build
```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Install on Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Clean Build
```bash
./gradlew clean assembleDebug
```

### Run Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

### Signing Report
```bash
./gradlew signingReport
```

Shows debug keystore SHA-1 fingerprint for Google Cloud Console setup.

## Google Drive OAuth Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Select project `androidscreensaver`
3. Edit the Android OAuth Client:
   - **Package name:** `com.vincentwetzel.androidscreensaver.debug` (debug build)
   - **SHA-1 fingerprint:** Run `./gradlew signingReport` to get it
4. Add your test Google account email under **OAuth Consent Screen → Test users**

See [GOOGLE_CLOUD_SETUP.md](GOOGLE_CLOUD_SETUP.md) for full details.

## Troubleshooting

### Debug Logcat Mirroring
During development, all logcat output is automatically mirrored to a file for easier debugging:
- **Location**: `/sdcard/Android/data/com.vincentwetzel.androidscreensaver.debug/files/debug-logcat.txt`
- **Enabled**: Debug builds only (disabled in release builds via `BuildConfig.DEBUG_LOGCAT_MIRROR`)
- **View the log**:
  ```bash
  adb shell "cat /sdcard/Android/data/com.vincentwetzel.androidscreensaver.debug/files/debug-logcat.txt"
  ```
- **Pull to computer**:
  ```bash
  adb pull /sdcard/Android/data/com.vincentwetzel.androidscreensaver.debug/files/debug-logcat.txt
  ```

This feature is developer-only and will NOT be active in release builds.

### Kotlin Plugin Conflict with AGP 9.1+
With AGP 9.1+ (including the current AGP 9.2.1 setup), Kotlin is now built-in and the explicit `org.jetbrains.kotlin.android` plugin should **NOT** be applied. This project has been migrated:
- Removed `id("org.jetbrains.kotlin.android")` from both root and app `build.gradle.kts`
- Replaced deprecated `kotlinOptions { jvmTarget = "17" }` with modern `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }`
- Added `android.disallowKotlinSourceSets=false` to allow KSP to generate Kotlin sources

### KSP map-key error in RepositoryModule
If KSP reports `@Provides methods of type map must declare a map key`, check `RepositoryModule`. Photo repositories should be injected as concrete Hilt bindings via their `@Inject` constructors, and `provideSlideshowManager()` should assemble the `Map<SourceType, PhotoRepository>` explicitly. Do not reintroduce `@IntoMap` providers for the photo repository map.

### Deprecated Gradle Properties Warning
The following deprecated properties were removed from `gradle.properties`:
- `android.usesSdkInManifest.disallowed`
- `android.sdk.defaultTargetSdkToCompileSdkIfUnset`
- `android.enableAppCompileTimeRClass`
- `android.builtInKotlin`
- `android.newDsl`
- `android.r8.optimizedResourceShrinking`
- `android.defaults.buildfeatures.resvalues`

These are now handled by AGP 9.2.0 defaults. The only custom properties retained are:
- `android.nonTransitiveRClass=true` (for smaller R classes)
- `android.enableJetifier=false` (to prevent Hilt annotation corruption)

### Google Sign-In fails with status code 10
- Verify SHA-1 fingerprint matches your debug keystore
- Verify package name includes `.debug` suffix for debug builds
- Check that `requestIdToken()` is NOT called with the Android Client ID (only use Web Client ID for ID tokens)

### Activation card doesn't hide after selecting screensaver
- Close and reopen the app to force re-checking the system settings
- Verify you actually selected "Android Screensaver" in the system screensaver settings

### Gallery shows no folders
- Grant photo permission when prompted
- Ensure device has photos in MediaStore

## Pre-Build Checklist

Before building or releasing a new version:

### Code Quality
- [ ] `./gradlew assembleDebug` passes without errors
- [ ] No new lint warnings (or they are suppressed with justification)
- [ ] All TODO items reviewed

### Documentation
- [ ] `CHANGELOG.md` updated with changes
- [ ] `TODO.md` updated with task status
- [ ] `README.md` reflects new features/removals
- [ ] `ARCHITECTURE.md` updated if structure changed
- [ ] `AGENTS.md` documentation rule followed

### Testing
- [ ] App launches and main screen renders
- [ ] Gallery source: folders load and photos display
- [ ] Google Drive source: sign-in works, folders load
- [ ] Folder browser: navigation in/out of subfolders works
- [ ] Screensaver: selectable in system settings, activation card auto-hides when selected
- [ ] Activation card: appears when not set as screensaver, disappears when set

### Build
- [ ] Version code incremented in `build.gradle.kts`
- [ ] Version name updated (semver: `major.minor.patch`)
- [ ] `signingReport` confirms SHA-1 for Google Cloud setup
- [ ] Release APK builds: `./gradlew assembleRelease`
