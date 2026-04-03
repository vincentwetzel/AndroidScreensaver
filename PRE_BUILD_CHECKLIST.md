# Pre-Build Checklist

Items to review and configure before starting implementation.

## 1. Google Cloud Console Setup (REQUIRED for Google Drive)

### Status: ✅ Project Exists
- User has existing Google Cloud Project
- **NEXT STEP**: Connect to app (see instructions below)

### OAuth2 Credentials (TODO)
- [ ] Enable Google Drive API in existing project
- [ ] Create OAuth 2.0 Client ID (Android type)
- [ ] Obtain OAuth Client ID
- [ ] Configure OAuth consent screen
  - App name: Android Screensaver
  - User support email
  - Developer contact information
  - Scopes: `https://www.googleapis.com/auth/drive.readonly`
- [ ] Add package name: `com.vincentwetzel.androidscreensaver`
- [ ] Add SHA-1 certificate fingerprint
  - Debug keystore SHA-1 (for development)
  - Release keystore SHA-1 (for production)
- [ ] Download `google-services.json`
- [ ] **PLACE** `google-services.json` in `app/` directory
- [ ] **ADD** `google-services.json` to `.gitignore` (NEVER commit)

### How to Get SHA-1 Fingerprint (Debug)
Run this command in terminal:
```bash
# Windows
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

# Mac/Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### API Keys
- [ ] Note API key for documentation
- [ ] Store securely (not in code)

---

## 2. Missing Dependencies

### Video Playback (ExoPlayer/Media3)
The current build.gradle.kts is missing video player dependency for video slideshow support:

```kotlin
// Video Playback - ExoPlayer (Media3)
implementation 'androidx.media3:media3-exoplayer:1.2.0'
implementation 'androidx.media3:media3-ui:1.2.0'
```

### WorkManager (Background Sync)
Required for scheduled background sync tasks:

```kotlin
// WorkManager (Background Tasks)
implementation 'androidx.work:work-runtime-ktx:2.9.0'
```

### Security (EncryptedSharedPreferences)
Required for secure token storage:

```kotlin
// Security
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
```

### Location Services (Weather)
Required for weather location-based features:

```kotlin
// Location Services
implementation 'com.google.android.gms:play-services-location:21.0.1'
```

---

## 3. Missing Permissions

### Location Permission (Weather Feature)
Add to `AndroidManifest.xml`:

```xml
<!-- Location for weather -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### Foreground Service Permission (Screensaver)
May be needed for keeping screensaver alive:

```xml
<!-- Foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### Notification Permission (Android 13+)
For pause on notification feature:

```xml
<!-- Notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## 4. Build Configuration Questions

### Google Services Plugin
Need to add to root `build.gradle.kts`:

```kotlin
plugins {
    id 'com.google.gms.google-services' version '4.4.0' apply false
}
```

And in app's `build.gradle.kts`:

```kotlin
plugins {
    id 'com.google.gms.google-services'
}
```

### ProGuard Rules for Google Drive
Need to add Google Drive API ProGuard rules to `proguard-rules.pro`

---

## 5. Resource Files Needed

### Missing Drawable Resources
- [ ] `ic_launcher.png` - App icon
- [ ] `ic_launcher_round.png` - Round app icon
- [ ] `ic_no_photos.png` - Placeholder when no photos found
- [ ] Source icons (gallery, dropbox, drive, photos, onedrive, network)
- [ ] Transition effect icons
- [ ] Display effect icons

### Mipmap Directories
- [ ] Create `mipmap-hdpi/`, `mipmap-mdpi/`, `mipmap-xhdpi/`, `mipmap-xxhdpi/`, `mipmap-xxxhdpi/`

---

## 6. Google Drive Specific Configuration

### Required Scopes
- `https://www.googleapis.com/auth/drive.readonly` - Read-only access to Drive
- `https://www.googleapis.com/auth/drive.metadata.readonly` - Read metadata only

### API Rate Limits
- Google Drive API has quota limits (queries per 100 seconds)
- Need to implement rate limiting and retry logic
- Consider implementing request batching

### File Types to Support
- Images: JPEG, PNG, GIF, WEBP, BMP, HEIC
- Videos: MP4, AVI, MOV, MKV, WEBM

---

## 7. Testing Strategy

### Device Testing Matrix
- [ ] NVidia Shield TV (primary target)
- [ ] Android phone (various sizes)
- [ ] Android tablet
- [ ] Different Android versions (8.0, 9, 10, 11, 12, 13, 14)

### Google Account Testing
- Personal Google accounts
- Google Workspace accounts
- Multiple accounts on device
- Accounts with 2FA enabled

---

## 8. Weather API Configuration

### Provider Selection
**CHOSEN: Open-Meteo** (100% free, no API key required)

- [x] **Open-Meteo** (Primary choice for v1.0)
  - ✅ Completely FREE - no API key needed
  - ✅ No rate limits on free tier
  - ✅ Global coverage (works worldwide)
  - ✅ Weather.gov fallback for US locations
  - ✅ Current weather, forecasts, historical data
  - ✅ No registration required
  - Documentation: https://open-meteo.com/

### Alternative (US-only fallback)
- weather.gov API (US only, completely free, no key needed)
- Can be used as additional data source for US users

### Action Required
- [x] None! Open-Meteo requires no API key
- [ ] Implement weather repository using Open-Meteo API

---

## 9. Additional Considerations

### Google Drive Shared Drives
- **DECISION**: Not implementing at this time
- Can be added in future release if requested

### Google Drive Shared With Me
- **DECISION**: Not implementing at this time
- Can be added in future release if requested

### Large Google Drive Libraries
- Pagination strategy for folders with 10,000+ photos
- Need efficient incremental sync
- Cache invalidation strategy

### Offline Mode
- Should screensaver work with cached photos when offline?
- How to handle when cache is empty?

---

## 10. Documentation to Prepare

### Before First Release
- [ ] Setup instructions in README
- [ ] Google Cloud setup guide for users
- [ ] Troubleshooting guide
- [ ] FAQ
- [ ] Privacy Policy (required for Google Play)
- [ ] Terms of Service

---

## Immediate Next Steps

### Before Coding:
1. **Set up Google Cloud Project** (CRITICAL - blocks all Google Drive work)
2. **Add missing dependencies** to build.gradle.kts
3. **Add missing permissions** to AndroidManifest.xml
4. **Choose weather API provider**
5. **Create/collect app icons**

### First Implementation Tasks:
1. Google Cloud project setup and credentials
2. Basic UI skeleton (main menu, settings)
3. Google Drive authentication
4. Folder browser
5. Basic slideshow with Google Drive photos
