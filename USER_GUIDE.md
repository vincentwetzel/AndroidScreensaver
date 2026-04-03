# User Guide

Welcome to Android Screensaver - your photos, beautifully displayed!

---

## Quick Start

### 1. Connect Google Drive

1. Open the app
2. Toggle **Google Drive** ON
3. Sign in with your Google account
4. Grant permission to access your photos

### 2. Select Folders

1. After signing in, the folder browser opens automatically
2. Check the folders you want to display
3. Tap **Save**

### 3. Configure Screensaver

1. Open **Settings** (gear icon in toolbar)
2. Configure your preferences:
   - **Display Time**: How long each photo shows
   - **Transition Effects**: Fade, slide, cross-fade, and more
   - **Schedule**: When to start/stop automatically
   - **Decorations**: Add date, clock, or weather

### 4. Activate Screensaver

1. Go to your device's **Settings**
2. Navigate to **Display** → **Screen saver** (or **Daydream** on some devices)
3. Select **Photo Screensaver**
4. Tap **Start now** to preview

---

## Settings Guide

### Sources
Manage your photo sources. For v1.0, Google Drive is the primary source.

**Folder Selection:**
- Tap **Select All** to include all folders
- Use search to find specific folders
- Toggle **Include subfolders** to get nested photos

### Media & Content
Control what photos appear in your slideshow.

- **Media Order**: Shuffle or sort by name/date
- **Content Filter**: Show images, videos, or both
- **Match Device Orientation**: Only show photos that match screen orientation

### Slideshow
Configure slideshow timing and behavior.

- **Display Time**: 3 seconds to 5 minutes (or custom)
- **Video Playback**: Mute, use system volume, or custom volume

### Display & Transitions
Visual effects for your slideshow.

- **Display Effect**: How photos render (pan, zoom, crop, etc.)
- **Transition Effect**: How photos change (fade, slide, wipe, etc.)
- **Transition Duration**: Speed of transitions

### Decorations
Add overlays to your slideshow.

- **Date**: Show current date with customizable format
- **Clock**: Show time with 12/24 hour options
- **Weather**: Show current weather (uses Open-Meteo, no API key needed)

### Photo Information
Show metadata about the current photo.

- **Fields**: File name, folder, date, source, dimensions, file size
- **Fade Out**: Info disappears after X seconds
- **Appearance**: Position, layout, colors, opacity

### Schedule & Timer
Automate when screensaver runs.

- **Autostart**: Start at specific times on specific days
- **Autostop**: Stop at specific times
- **Only when charging**: Prevent battery drain

### Display & Power
Control device behavior during screensaver.

- **Screen Rotation**: Portrait, landscape, or system default
- **Keep Screen On**: Override system sleep settings

### Sync & Network
Control when and how photos sync.

- **Sync Interval**: How often to check for new photos
- **Wi-Fi Only**: Prevent mobile data usage
- **Timeout**: Network request timeout

### Appearance
Visual customization.

- **Background Color**: Choose from 14 presets or custom
- **Cache Limit**: Control storage usage
- **Clear Cache**: Free up space

### Advanced
Power user options.

- **Power Management**: Screen brightness, wake lock
- **Exit Trigger**: How to exit screensaver (touch, remote, shake)
- **About**: App info, links, version

---

## Troubleshooting

### Google Drive Connection Issues

**"Sign-in failed"**
- Check your internet connection
- Verify Google Drive API is enabled in your Google Cloud project
- Ensure your Google account is added as a test user

**"No folders found"**
- Verify you have photos in Google Drive
- Check folder permissions in Google Drive
- Try searching for specific folder names

### Screensaver Not Starting

**"Screensaver won't start automatically"**
- Check Schedule settings are enabled
- Verify device is charging (if "Only when charging" is enabled)
- Ensure screensaver is enabled in Android settings

**"Black screen instead of photos"**
- Check that folders are selected and contain photos
- Verify Wi-Fi Only setting if on mobile network
- Try clearing cache in Appearance settings

### Performance Issues

**"Photos load slowly"**
- Increase cache limit in Appearance settings
- Check network connection
- Reduce max photo resolution in settings (future feature)

**"App uses too much storage"**
- Go to Settings → Appearance → Cache Limit
- Set a lower limit (e.g., 100 MB)
- Tap "Clear Cache" to free space immediately

### Weather Not Showing

**"Weather shows 'Unknown'"**
- Enable location services on your device
- Check internet connection
- Open-Meteo API may be temporarily unavailable (rare)

---

## Keyboard Shortcuts (TV Remote)

| Button | Action |
|--------|--------|
| D-Pad Up/Down | Navigate settings |
| D-Pad Center/OK | Select/Enter |
| Back | Return to previous screen |
| Home | Exit screensaver |

---

## Tips & Tricks

1. **Mix Photos and Videos**: Enable both in Content Filter for dynamic slideshows
2. **Use Schedules**: Set different schedules for weekdays vs weekends
3. **Save Battery**: Enable "Only when charging" to prevent battery drain
4. **Custom Weather Location**: Set a specific city instead of device location
5. **Debug Mode**: Tap version 7 times in settings to access developer tools

---

## Privacy

- **Data Collection**: This app does NOT collect personal data
- **Cloud Storage**: Your photos stay on your device or in your cloud account
- **Weather**: Location data is only used for weather and is not stored
- **Google Drive**: We only request read-only access to your Drive

---

## Support

- **Discord**: Join our community server (link in About screen)
- **GitHub**: Report issues at github.com/vincentwetzel/AndroidScreensaver
- **Email**: Contact the developer through GitHub issues

---

## License

MIT License - See LICENSE file for details

Open source project by Vincent Wetzel
