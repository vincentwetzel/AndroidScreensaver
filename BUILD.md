# Build Instructions

## Requirements

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 36 (`compileSdk`), minSdk 26, targetSdk 34
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
3. Enable **APIs & Services > Library > Google Drive API** for the project.
4. Configure the OAuth consent screen:
   - Add `https://www.googleapis.com/auth/drive.readonly` under **Data Access** or **Scopes**.
   - Add your test Google account email under **Audience > Test users** if the app is unpublished.
5. Edit or create the Android OAuth Client:
   - **Package name:** `com.vincentwetzel.androidscreensaver.debug` for debug builds.
   - **Package name:** `com.vincentwetzel.androidscreensaver` for release builds.
   - **SHA-1 fingerprint:** Run `./gradlew signingReport`.

## Dropbox OAuth Setup

1. Create or open the Dropbox app in the Dropbox App Console.
2. Add the app key to `local.properties`:

```properties
DROPBOX_APP_KEY=your_app_key_here
```

The Gradle build sanitizes quotes and whitespace before injecting this value into `BuildConfig.DROPBOX_APP_KEY`. Dropbox sign-in uses PKCE with explicit read-only scopes: `files.content.read`, `files.metadata.read`, and `account_info.read`.

If `DROPBOX_APP_KEY` is missing, Dropbox authentication shows an error instead of launching OAuth.

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

### Google Drive Auth Fails with InvalidScope

- Enable the **Google Drive API** in the same Cloud project as the Android OAuth client.
- Add `https://www.googleapis.com/auth/drive.readonly` to the OAuth consent screen's **Data Access** or **Scopes** list.
- If the app is unpublished or in testing, add the signed-in Google account under **Audience > Test users**.
- Verify the Android OAuth client package name matches the installed build:
  - Debug: `com.vincentwetzel.androidscreensaver.debug`
  - Release: `com.vincentwetzel.androidscreensaver`
- Verify the Android OAuth client SHA-1 matches the `signingReport` entry for the same build variant you installed.
- Google Drive sign-in uses Android's Google AccountPicker and `GoogleAccountCredential` to request read-only Drive access for the selected account.
- Debug builds log `GoogleDriveAuthEnv`, `GoogleAccountManager`, and `GoogleDriveDiagnostic` entries during sign-in, including package name, signing certificate SHA-1/SHA-256, Google Play Services version/status, selected account details, credential state, and token-probe results without printing OAuth tokens. The diagnostic probes test full userinfo scopes, Drive metadata, and Drive readonly through both Account-object and account-name token APIs.

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
- [ ] `CODING_STANDARDS.md` updated if engineering rules changed.
- [ ] `AGENTS.md` documentation rule followed.

### Testing

- [ ] App launches and main screen renders.
- [ ] Gallery source folders load and photos display.
- [ ] Google Drive sign-in works and folders load.
- [ ] Dropbox sign-in works and folders load.
- [ ] Folder browser navigation works in and out of subfolders.
- [ ] Screensaver is selectable in system settings.
- [ ] Activation card appears when inactive and hides when active.

### Release Build

- [ ] Version code incremented in `build.gradle.kts`.
- [ ] Version name updated.
- [ ] `signingReport` confirms SHA-1 for Google Cloud setup.
- [ ] Release APK builds with `./gradlew assembleRelease`.
