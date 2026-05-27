# Build Instructions

## Requirements

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34 (`compileSdk`), minSdk 26
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

Use this to get the debug keystore SHA-1 fingerprint for Google Cloud Console setup.

## Google Drive OAuth Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials).
2. Select project `androidscreensaver`.
3. Edit the Android OAuth Client:
   - **Package name:** `com.vincentwetzel.androidscreensaver.debug` for debug builds.
   - **SHA-1 fingerprint:** Run `./gradlew signingReport`.
4. Add your test Google account email under **OAuth Consent Screen > Test users**.

## Troubleshooting

### Debug Logcat Mirroring

Debug builds mirror logcat output to:

```text
/sdcard/Android/data/com.vincentwetzel.androidscreensaver.debug/files/debug-logcat.txt
```

View it on device:

```bash
adb shell "cat /sdcard/Android/data/com.vincentwetzel.androidscreensaver.debug/files/debug-logcat.txt"
```

Pull it to the computer:

```bash
adb pull /sdcard/Android/data/com.vincentwetzel.androidscreensaver.debug/files/debug-logcat.txt
```

This feature is disabled in release builds through `BuildConfig.DEBUG_LOGCAT_MIRROR`.

### Kotlin Plugin Conflict with AGP 9.1+

AGP 9.1+ includes Kotlin support. This project does not apply `org.jetbrains.kotlin.android` directly.

- Root and app `build.gradle.kts` files should not apply `id("org.jetbrains.kotlin.android")`.
- Use `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }` instead of deprecated `kotlinOptions`.
- Keep `android.disallowKotlinSourceSets=false` so KSP can generate Kotlin sources.

### KSP Map-Key Error in RepositoryModule

If KSP reports `@Provides methods of type map must declare a map key`, check `RepositoryModule`.

Photo repositories should be injected as concrete Hilt bindings through their `@Inject` constructors, and `provideSlideshowManager()` should assemble the `Map<SourceType, PhotoRepository>` explicitly. Do not reintroduce `@IntoMap` providers for the photo repository map.

### Deprecated Gradle Properties Warning

The following deprecated properties were removed from `gradle.properties`:

- `android.usesSdkInManifest.disallowed`
- `android.sdk.defaultTargetSdkToCompileSdkIfUnset`
- `android.enableAppCompileTimeRClass`
- `android.builtInKotlin`
- `android.newDsl`
- `android.r8.optimizedResourceShrinking`
- `android.defaults.buildfeatures.resvalues`

The only custom properties currently retained are:

- `android.nonTransitiveRClass=true`
- `android.enableJetifier=false`

### Google Sign-In Fails with Status Code 10

- Verify the SHA-1 fingerprint matches the debug keystore.
- Verify the package name includes `.debug` for debug builds.
- Do not call `requestIdToken()` with the Android Client ID. Use a Web Client ID only when ID tokens are needed.

### Activation Card Does Not Hide

- Close and reopen the app to force a system settings re-check.
- Verify **Android Screensaver** is selected in the system screensaver settings.

### Gallery Shows No Folders

- Grant photo permission when prompted.
- Confirm the device has photos indexed in MediaStore.

## Pre-Build Checklist

Before building or releasing a new version:

### Code Quality

- [ ] `./gradlew assembleDebug` passes without errors.
- [ ] No new lint warnings, or warnings are suppressed with justification.
- [ ] Active TODO items reviewed.

### Documentation

- [ ] `CHANGELOG.md` updated with changes.
- [ ] `TODO.md` updated with task status.
- [ ] `README.md` reflects new features/removals.
- [ ] `ARCHITECTURE.md` updated if structure changed.
- [ ] `AGENTS.md` documentation rule followed.

### Testing

- [ ] App launches and main screen renders.
- [ ] Gallery source folders load and photos display.
- [ ] Google Drive sign-in works and folders load.
- [ ] Folder browser navigation works in and out of subfolders.
- [ ] Screensaver is selectable in system settings.
- [ ] Activation card appears when inactive and hides when active.

### Release Build

- [ ] Version code incremented in `build.gradle.kts`.
- [ ] Version name updated.
- [ ] `signingReport` confirms SHA-1 for Google Cloud setup.
- [ ] Release APK builds with `./gradlew assembleRelease`.
