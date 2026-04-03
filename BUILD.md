# Build Instructions

## How to Build the Project

### Option 1: Android Studio (Recommended)

1. **Open Android Studio**
2. **Open the project**:
   - File → Open
   - Navigate to `i:\coding_workspaces\Kotlin\AndroidScreensaver`
   - Click "OK"
3. **Sync Gradle**:
   - Android Studio will prompt to sync automatically
   - Wait for "Gradle sync finished" message
4. **Build the project**:
   - Build → Make Project (Ctrl+F9)
   - Or click the hammer icon in the toolbar
5. **Run on device/emulator**:
   - Run → Run 'app' (Shift+F10)
   - Select connected device or emulator

### Option 2: Command Line (After Android Studio sync)

After opening in Android Studio once, the Gradle wrapper will be created:

```bash
cd i:\coding_workspaces\Kotlin\AndroidScreensaver
.\gradlew assembleDebug
```

---

## Build Requirements

- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: Version 17 (bundled with Android Studio)
- **Android SDK**: 
  - Compile SDK: 34
  - Min SDK: 26
  - Target SDK: 34
- **Gradle**: 8.2 (managed by Android Studio)

---

## Troubleshooting

### "Gradle sync failed" errors:
1. Check that you have Android Studio installed
2. Go to File → Invalidate Caches → Invalidate and Restart
3. Wait for Gradle to sync

### "SDK not found" errors:
1. File → Project Structure → SDK Location
2. Ensure Android SDK path is correct
3. Install SDK 34 if not already installed

### "Dependency resolution failed" errors:
1. Check internet connection
2. File → Sync Project with Gradle Files
3. Wait for all dependencies to download

### Build succeeds but app crashes:
1. Check logcat for error messages
2. Verify Google Drive API is enabled in Google Cloud Console
3. Ensure OAuth consent screen is configured

---

## Current Build Status

- ✅ Project structure created
- ✅ All dependencies configured
- ✅ Main UI implemented
- ✅ Settings screen implemented
- ✅ All resources created (icons, strings, arrays)
- ⏭️ **Ready to build in Android Studio**

---

## Next Steps After Successful Build

1. Test the main menu displays correctly
2. Test settings screen opens and shows all categories
3. Verify all placeholder icons display
4. Verify all string resources load correctly
5. Move to Phase 3: Google Drive Integration
