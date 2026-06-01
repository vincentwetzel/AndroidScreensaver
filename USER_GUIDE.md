# User Guide

## Getting Started

### 1. Enable a Photo Source

Open the app and add or toggle on the sources you want to use:

- **Gallery** - Photos stored on your device. No sign-in required.
- **Google Drive** - Photos stored in Google Drive. Requires Google account sign-in.
- **Dropbox** - Photos stored in Dropbox. Requires Dropbox sign-in.

Google Drive and Dropbox can each be added multiple times for separate accounts.

### 2. Select Folders

After enabling a source, tap it to browse folders:

- Check boxes next to folders you want to include.
- Tap a folder name to open it and view subfolders.
- Use the toolbar back arrow to return to the previous folder.
- Selected folders include nested subfolders automatically.
- Deselect a nested folder to exclude it.
- Pull down to refresh and detect new or removed folders.
- Pull-to-refresh reloads the folder you are currently viewing without changing your folder navigation history.
- Use **Re-authenticate** from the folder browser menu, or the in-screen button that appears after an authentication error, to refresh cloud account access without removing the account.
- Use **Select All** and **Deselect All** for quick selection.
- Source cards show the last saved selected-folder media count while refreshed counts are prepared in the background.

Selections save immediately; there are no Save or Cancel buttons.

### 3. Activate the Screensaver

1. The **Activate Screensaver** card appears when this app is not yet your active screensaver.
2. Tap **Open Screensaver Settings**.
3. Select **Android Screensaver** from the system list.
4. Configure when to start, such as while charging or docked.
5. Return to the app. The activation card should disappear automatically.

### 4. Test It

Tap the preview icon in the top toolbar to instantly launch the screensaver without waiting for the system screen timeout.

After activation, lock the screen or place the device on a charger. The screensaver should start after the configured system delay.

## Screensaver Settings

Settings are accessed through **Menu > Settings**.

### Media & Content

- Media order: shuffle, date, and name sorting.
- Content Type filter: Images Only, Videos Only, or Both.
- Match device orientation for letterboxed photos when orientation differs.

### Slideshow

- Slide duration.
- Video playback settings.
- Display effects such as crop-to-fit, scale-to-fit, zoom, pan, and focus.
- Transition effects such as fade, cross fade, wipe, slide, swap, cube, doorway, and radial.
- Transition duration.

### Decorations

- Date overlay.
- Clock overlay.
- Weather overlay.
- Photo information overlay.
- Persistent overlays gently shift position during playback to reduce OLED/AMOLED burn-in risk.

### Schedule & Timer

- Autostart schedule.
- Autostop schedule.
- Auto-exit timeout.

### Display & Power

- Screen rotation.
- Keep screen on.
- Background color.
- Exit trigger.

### Sync & Network

- Sync timeout.
- Wi-Fi only cloud loading. Ethernet also satisfies network-only cloud loading.
- Cache limit and clear-cache action.

## Sources

### Gallery

Shows photo folders from your device, such as Camera, Screenshots, and Downloads. Android 13+ uses scoped media access and can browse when either image or video permission is granted; older Android versions may require photo read permission.

### Google Drive

Shows folders from one or more Google Drive accounts. The app requests read-only access and cannot modify or delete Drive files.

Authenticated source cards must show `Signed in as [account email]`.

If sign-in expires or a token is revoked, open the source card's three-dot menu and choose **Re-authenticate**. Existing folder selections are kept for the account whenever the provider returns a matching account identity.

### Dropbox

Shows folders from one or more Dropbox accounts. The app uses the Dropbox OAuth browser flow, then opens the shared cloud folder browser for the signed-in account. It can cache thumbnails and downloaded media locally so selected Dropbox photos and videos load faster after first access.

Authenticated source cards must show `Signed in as [account email]`.

Dropbox sign-in requires `DROPBOX_APP_KEY` to be configured at build time. If authentication expires, use **Re-authenticate** from the source card or folder browser instead of removing and re-adding the account.

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Activation card still shows after selecting | Close and reopen the app to refresh detection. |
| Screensaver does not start | Check that **Android Screensaver** is selected in system screen saver settings. |
| No photos showing | Verify folders are selected and contain photos or videos matching the Content Type filter. |
| Google Drive sign-in fails | Check that your device has a Google account added. |
| 403 error when adding Google account | If the app is unpublished, add the Google account email to Google Cloud Console test users. |
| Dropbox sign-in does not start | Confirm `DROPBOX_APP_KEY` is set in `local.properties` before building the app. |
| Cloud folders fail after prior sign-in | Use **Re-authenticate** from the source card menu or folder browser. |
| Gallery shows empty | Ensure the device has photos and permission is granted. |
