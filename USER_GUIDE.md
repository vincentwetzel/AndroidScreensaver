# User Guide

## Getting Started

### 1. Enable a Photo Source

Open the app and toggle ON the sources you want to use:

- **Gallery** - Photos stored on your device. No sign-in required.
- **Google Drive** - Photos stored in your Google Drive. Requires Google account sign-in.
- **Dropbox** - Photos stored in Dropbox. Requires Dropbox sign-in.

### 2. Select Folders

After enabling a source, tap it to browse folders:

- **Check boxes** next to folders you want to include (selections are saved automatically)
- **Tap a folder name** to open it and see subfolders
- Use the **back arrow** in the toolbar to go up one level
- Selected folders include nested subfolders automatically. You can deselect a nested folder to exclude it.
- **Pull down** to refresh and detect new/removed folders from the source
- **Select All / Deselect All** buttons for quick selection
- Selections are saved **immediately** — no Save/Cancel buttons needed

### 3. Activate the Screensaver

1. The **"Activate Screensaver"** card at the top of the app will appear when this app is not yet your active screensaver
2. Tap **"Open Screensaver Settings"** to go to your device's screensaver settings
3. Select **"Android Screensaver"** from the list
4. Configure when to start (while charging, while docked, etc.) and the delay
5. Once selected, return to the app — the activation card will automatically disappear

### 4. Test It

**Quick Preview:** Tap the **preview icon** (👁) in the top toolbar to instantly launch the screensaver — no need to wait for screen timeout.

**Full Activation:** After activation, lock your screen or put the device on a charger. The screensaver should start after your configured delay.
If the system sends you back to **"Colors"**, update to a build that includes the DreamService permission fix or verify that your device supports DreamService.

## Screensaver Settings

Within the app, you can customize:

**Media & Content:**
- Media order (shuffle, date, name sorting)
- Content Type filter (Images Only, Videos Only, or Both)
- Match device orientation (shows letterboxed photos when orientation differs)

**Slideshow:**
- Slide duration (how long each photo is shown)
- Video playback settings

**Display & Transitions:**
- Display effects (crop-to-fit, scale-to-fit, zoom, pan, focus)
- Transition effects (fade, cross fade, wipe, slide, swap, cube, doorway, radial, etc.)
- Transition duration

**Decorations:**
- Date, clock, and weather overlay toggles

**Schedule & Timer:**
- Autostart schedule (when to automatically start the screensaver)
- Autostop schedule (when to automatically stop the screensaver)
- Auto-exit timeout (how long before screensaver exits: Disabled, 30s, 5min, 15min, 30min, 45min, 1hr, 1.5hr, 2hr, or custom)

**Display & Power:**
- Screen rotation (portrait, landscape, or system default)
- Keep screen on (prevent screen dimming)

**Sync & Network:**
- Sync timeout (how long to wait for network requests before timing out)
- Wi-Fi only (only fetch cloud sources on Wi-Fi)

**Appearance:**
- Background color
- Cache limit (preset or custom MB value)
- Clear cache

**Advanced:**
- Exit trigger (touch, remote button, shake, or voice command)

These are accessed via **Menu → Settings**.

## Sources

### Gallery

Shows photo folders from your device (Camera, Screenshots, Downloads, etc.). No permissions needed on Android 13+. On older Android, grants photo read permission.

### Google Drive

Shows folders from your Google Drive account. Requires Google account authentication. The app requests **read-only** access to your Drive files - it cannot modify or delete anything.

### Dropbox

Shows folders from your Dropbox account. The app can cache thumbnails and downloaded media locally so selected Dropbox photos and videos can load faster after the first access.

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Activation card still shows after selecting | Close and reopen the app to refresh detection |
| Screensaver doesn't start | Check that "Android Screensaver" is selected in system screen saver settings |
| No photos showing | Verify folders are selected and contain photos |
| Google Drive sign-in fails | Check that your device has a Google account added |
| 403 Error when adding Google account | If the app is unpublished, the Google account email must be added to the "Test users" list in Google Cloud Console (APIs & Services > OAuth consent screen). |
| Gallery shows empty | Ensure device has photos and permission is granted |
