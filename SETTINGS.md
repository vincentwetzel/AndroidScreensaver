# Settings Reference

Complete list of all settings available in the Android Screensaver app.

---

## 1. Source Configuration

### Folder Selection
When a user selects a photo source, they can:
- **Browse folders**: Opens a hierarchical tree view of all folders/subfolders from that source
- **Select multiple folders**: Check boxes to include/exclude specific folders
- **Include subfolders**: Toggle to recursively include all nested subfolders
- **Select all/Deselect all**: Quick actions for bulk selection
- **Search folders**: Search bar to filter folder list by name

**Sources Available:**
- Local Gallery
- Dropbox
- Google Drive
- Google Photos
- OneDrive
- Local Network (SMB/WebDAV)

**Per-Source Settings:**
- Enable/Disable source
- Folder selection (multi-select tree)
- Include subfolders toggle
- Last sync timestamp
- Account info (name, email)
- Logout/Re-authenticate button

---

## 2. Slideshow Settings

### Display Time
Determine how long each photo is displayed before transitioning to the next.

**Preset Options:**
- 3 seconds
- 5 seconds
- 10 seconds
- 15 seconds
- 30 seconds
- 1 minute
- 2 minutes
- 5 minutes

**Custom Option:**
- User can enter a custom duration
- Supports both seconds and minutes
- Range: 1 second to 60 minutes
- Input format: `[number] [seconds|minutes]`

### Shuffle
- **Toggle**: Enable/disable random photo order
- **Reshuffle interval**: 
  - After each complete cycle
  - Every X minutes
  - Never (fixed order)

### Photo Order (when shuffle is disabled)
- Date taken (newest first)
- Date taken (oldest first)
- File name (A-Z)
- File name (Z-A)
- File size (largest first)
- File size (smallest first)
- Random

---

## 2. Media Order & Content

### Media Order
Determine the order in which media files are displayed in the slideshow.

**Order Options:**
- **Shuffle/Random**: Random order with configurable reshuffle interval
- **File Name (A-Z)**: Alphabetical ascending
- **File Name (Z-A)**: Alphabetical descending
- **Date Modified (Earliest First)**: Chronological order (oldest first)
- **Date Modified (Latest First)**: Reverse chronological order (newest first)

**Shuffle Configuration:**
- **Reshuffle interval**:
  - After each complete cycle (reshuffle when all items shown)
  - Every 1 minute
  - Every 5 minutes
  - Every 10 minutes
  - Every 30 minutes
  - Never (maintain initial random order)

---

## 3. Content Filter

### Media Type Filter
Control what types of media are displayed in the slideshow.

**Filter Options:**
- **Images AND Videos**: Show both photos and video files (default)
- **Images Only**: Show only photo files (JPG, PNG, GIF, WEBP, HEIC, etc.)
- **Videos Only**: Show only video files (MP4, AVI, MKV, MOV, etc.)

### Orientation Filter
- **Match Device Orientation Only**: Toggle
  - When enabled: Only display images that match the current screen orientation (landscape images on landscape screen, portrait images on portrait screen)
  - When disabled: Display all images regardless of orientation, scaling/cropping as needed
  - Useful for avoiding awkward display of portrait photos on wide screens

### Date Range Filter
- **Start date**: Show media taken/modified after this date
- **End date**: Show media taken/modified before this date
- Quick presets: Last 7 days, Last 30 days, Last year, All time

### File Size Filter
- **Minimum file size**: Skip files smaller than X KB/MB
- **Maximum file size**: Skip files larger than X MB/GB
- Useful for skipping thumbnails or extremely large files

---

## 4. Video Playback Settings

Configure how videos are handled during the slideshow.

### Audio Settings
- **Mute**: Play videos without audio
- **Use System Volume**: Respect device's current volume level
- **Use Custom Volume**: Set a specific volume level for slideshow videos (0-100%)

### Playback Behavior
- **Max video duration**: Skip videos longer than X seconds/minutes (prevents very long videos)
  - Options: 10s, 30s, 1m, 2m, 5m, No limit
- **Auto-play videos**: Toggle (if false, treat videos as still images showing first frame)
- **Show playback controls**: Toggle (display play/pause, seek bar on tap during video)
- **Loop short videos**: Toggle (loop videos under 10 seconds until slide duration expires)

### Video Display Mode
- **Play full duration**: Play video for its entire length (respects max duration setting)
- **Play fixed time**: Play only first X seconds of each video
- **Extract still frame**: Don't play video, show a still frame as a photo (use thumbnail or custom timestamp)
  - Still frame timestamp: Beginning (default), Middle, End, Custom time

---

## 5. Display Effects

How individual photos are rendered on screen.

### Effect Options:
| Effect | Description |
|--------|-------------|
| **Pan** | Slowly pan across the image (Ken Burns effect) |
| **Scale to Fit Center** | Scale image to fit entirely within the screen center, may show letterboxing |
| **Crop to Fit Center** | Scale and crop image to fill the screen, centered |
| **Zoom** | Start zoomed out, slowly zoom into the image |
| **Focus** | Start with blur, slowly sharpen to full clarity |

### Pan/Zoom Configuration (for Pan and Zoom effects):
- **Pan direction**: Left-to-right, Right-to-left, Top-to-bottom, Bottom-to-top, Random
- **Zoom range**: 1.0x to 2.0x (configurable)
- **Animation duration**: How long the effect runs (can be same as display time or shorter with hold time)

### Image Quality
- **Cache quality**: Low, Medium, High
- **Max resolution**: Auto, 720p, 1080p, 4K, Original
- **Preload next image**: Toggle (improves transition smoothness)

---

## 4. Transition Effects

Visual effects when switching between photos.

### Transition Options:
| Category | Effect |
|----------|--------|
| **Basic** | Fade |
| | Cross Fade |
| **Motion** | Wipe |
| | Slide (Left/Right/Up/Down) |
| | Swap |
| **3D** | Cube |
| | Doorway |
| | Radial |
| **Artistic** | Memory (film burn effect) |
| | Illusion (motion blur trail) |
| | Ripple (water ripple) |
| | Flash (quick white flash) |
| | Star (starburst pattern) |
| | Wind (horizontal streaks) |
| | Circle (circular reveal) |

### Transition Configuration:
- **Transition duration**: How long the transition animation plays (0.5s, 1s, 1.5s, 2s, custom)
- **Transition easing**: Linear, Ease-in, Ease-out, Ease-in-out
- **Transition direction** (for directional effects like slide/wipe): Left, Right, Up, Down, Random

---

## 6. Transition Effects

Visual effects when switching between photos.

### Transition Options:
| Category | Effect |
|----------|--------|
| **Basic** | Fade |
| | Cross Fade |
| **Motion** | Wipe |
| | Slide (Left/Right/Up/Down) |
| | Swap |
| **3D** | Cube |
| | Doorway |
| | Radial |
| **Artistic** | Memory (film burn effect) |
| | Illusion (motion blur trail) |
| | Ripple (water ripple) |
| | Flash (quick white flash) |
| | Star (starburst pattern) |
| | Wind (horizontal streaks) |
| | Circle (circular reveal) |

### Transition Configuration:
- **Transition duration**: How long the transition animation plays (0.5s, 1s, 1.5s, 2s, custom)
- **Transition easing**: Linear, Ease-in, Ease-out, Ease-in-out
- **Transition direction** (for directional effects like slide/wipe): Left, Right, Up, Down, Random

---

## 7. Decorations

Overlays displayed on top of the slideshow.

### Date Display
- **Enable/Disable**: Toggle to show/hide date
- **Position**: Top-left, Top-right, Bottom-left, Bottom-right, Center
- **Date Format Options**:
  - Month Day (e.g., "January 15")
  - Weekday (e.g., "Monday")
  - Year (e.g., "2026")
  - Abbreviate Month (e.g., "Jan 15")
  - Abbreviate Weekday (e.g., "Mon")
  - Numeric Date (e.g., "01/15/2026" or "15/01/2026" based on locale)
  - Full Date (e.g., "Monday, January 15, 2026")
  - Short Date (e.g., "Jan 15, 2026")
- **Custom format**: Allow custom date format pattern (using Android's DateFormat)
- **Font size**: Small, Medium, Large, Custom
- **Font color**: White (default), Black, Custom color picker
- **Opacity**: 0-100% slider
- **Background**: None, Semi-transparent overlay, Solid background
- **Animation**: Pulse softly (gentle opacity oscillation) OR Static display

### Clock Display
- **Enable/Disable**: Toggle to show/hide clock
- **Position**: Top-left, Top-right, Bottom-left, Bottom-right, Center
- **Clock Format**:
  - 12-hour (e.g., "2:30 PM")
  - 24-hour (e.g., "14:30")
- **Show Seconds**: Toggle (display "2:30:45 PM" vs "2:30 PM")
- **Font size**: Small, Medium, Large, Custom
- **Font color**: White (default), Black, Custom color picker
- **Opacity**: 0-100% slider
- **Background**: None, Semi-transparent overlay, Solid background
- **Animation**: Pulse softly (gentle opacity oscillation) OR Static display
- **Pulse configuration** (when pulse animation enabled):
  - Pulse speed: Slow, Medium, Fast
  - Pulse range: Min opacity 40-80%, Max opacity 80-100%

### Weather Display
- **Enable/Disable**: Toggle to show/hide weather
- **Position**: Top-left, Top-right, Bottom-left, Bottom-right, Center
- **Location**:
  - Use device location (GPS/network)
  - Manual location entry (city name or coordinates)
  - Location update interval: 15 min, 30 min, 1 hour, 6 hours
- **Weather Information Displayed**:
  - Current temperature
  - Weather condition icon (sunny, cloudy, rainy, snowy, etc.)
  - Condition text (e.g., "Partly Cloudy", "Heavy Rain")
  - Chance of rain (percentage)
  - Humidity level (percentage)
  - Wind speed and direction
  - Feels like temperature
  - High/Low for the day
- **Unit System**:
  - **Temperature**: Fahrenheit, Celsius
  - **Wind Speed**: mph, km/h, m/s
  - **Pressure**: inHg, hPa, mbar
  - **Visibility**: miles, km
- **Weather Customize Button**: Opens detailed weather configuration
  - Select which data points to display (temp, rain chance, cloudy/sunny, etc.)
  - Choose weather icon style (minimal, detailed, animated)
  - Background for weather widget: Transparent, Frosted glass, Solid
- **Font size**: Small, Medium, Large, Custom
- **Font color**: White (default), Black, Custom color picker
- **Opacity**: 0-100% slider
- **Animation**: Pulse softly (gentle opacity oscillation) OR Static display
- **Weather Provider**: OpenWeatherMap (default), WeatherAPI, AccuWeather
- **API Key**: For weather service (free tier available, user can enter their own)

### Decoration Styling (Applied to All Enabled Decorations)
- **Unified animation toggle**: Enable/disable pulse for all decorations together
- **Per-decoration override**: Allow individual decorations to have different animation settings
- **Font family**: System default, Sans-serif, Serif, Monospace, Custom font
- **Text shadow**: Toggle (improves readability on varied backgrounds)
- **Text shadow intensity**: Light, Medium, Heavy
- **Margin from edge**: Small, Medium, Large (spacing from screen edges)
- **Spacing between decorations**: Small, Medium, Large (gap between date, time, weather when multiple enabled)

### Decoration Layout Preview
- Live preview showing how decorations will look on screen
- Drag-and-drop positioning (alternative to preset positions)
- WYSIWYG editor for font, color, size adjustments

---

## 8. Photo Information Settings

Configure what photo metadata is displayed during the slideshow and how it behaves.

### Master Toggle
- **Show Photo Information**: Master on/off toggle for all photo info overlays
  - When disabled: No photo information is displayed regardless of individual toggles
  - When enabled: Displays configured photo info fields with specified behavior

### Photo Info Fields
Select which information fields to display when a photo is shown:

- **File Name**: Toggle on/off
  - Displays the file name (without extension by default)
  - Option: Show with extension (e.g., "IMG_1234.JPG" vs "IMG_1234")
  
- **Folder Name**: Toggle on/off
  - Displays the folder/album name where the photo is from
  - Useful when photos come from multiple sources/folders
  - Shows parent folder name or full path (configurable)

- **Date Taken**: Toggle on/off
  - Displays the photo's EXIF date taken or file modification date
  - Format options:
    - Full date (e.g., "January 15, 2026")
    - Short date (e.g., "Jan 15, 2026")
    - Numeric (e.g., "01/15/2026")
    - Relative (e.g., "2 weeks ago", "3 years ago")

- **Source Name**: Toggle on/off
  - Displays which source the photo is from (e.g., "Dropbox", "Google Photos", "Gallery")
  - Helpful when using multiple sources in slideshow

- **Description/Caption**: Toggle on/off
  - Displays photo description if available from the source
  - Falls back to empty if no description exists

- **Dimensions**: Toggle on/off
  - Displays photo resolution (e.g., "4032 x 3024")
  - Useful for showcasing high-resolution photos

- **File Size**: Toggle on/off
  - Displays file size (e.g., "4.2 MB")

### Photo Info Display Behavior
- **Fade Out After**: Toggle + duration selector
  - Default: 5 seconds
  - Preset options: 2s, 3s, 5s, 8s, 10s, 15s, Never (stay visible)
  - Custom: User can enter custom seconds (1-60s)
  - Behavior: Photo info appears when new photo is shown, then fades out after configured duration
  - Reappears on next photo transition

- **Fade Duration**: How long the fade-out animation takes
  - Options: 0.5s, 1s (default), 1.5s, 2s

### Photo Info Appearance
- **Position**: 
  - Bottom-left (default)
  - Bottom-right
  - Top-left
  - Top-right
  - Bottom center
  - Top center
  
- **Layout**:
  - Horizontal (fields in a row)
  - Vertical (fields stacked)
  - Compact (single line with separators)

- **Field Separator** (for horizontal/compact layout):
  - Bullet (•)
  - Pipe (|)
  - Dash (—)
  - Slash (/)
  - Comma (,)

- **Font Size**: Small, Medium (default), Large, Custom

- **Font Color**: White (default), Black, Custom color picker

- **Background**: 
  - None (text directly on photo)
  - Semi-transparent overlay (frosted glass effect)
  - Solid background strip
  - Gradient fade (dark gradient behind text)

- **Background Opacity**: 0-100% slider (when using semi-transparent or solid background)

- **Text Opacity**: 0-100% slider

- **Text Shadow**: Toggle
  - Intensity: Light, Medium (default), Heavy
  - Improves readability on varied photo backgrounds

- **Font Family**: System default, Sans-serif, Serif, Monospace

### Photo Info Preview
- Live preview button showing how photo info will appear
- Sample overlay displayed on current photo in settings preview

---

## 9. Appearance Settings

### Background
- **Background color**: Black (default), White, Custom color picker
- **Show photo info**: Toggle
  - Display: Filename, Date taken, Source name, Description
  - Position overlay: Top-left, Top-right, Bottom-left, Bottom-right
  - Opacity: 0-100%

### Orientation
- **Screen orientation**: 
  - **Portrait**: Force portrait orientation for screensaver
  - **Landscape**: Force landscape orientation for screensaver
  - **Use System Settings**: Follow device's current orientation setting
- **Handle mixed orientations**: Auto-rotate images to match screen
  - When enabled: Automatically rotates photos to match the screen orientation
  - When disabled: Displays photos in their native orientation

### Multi-Monitor / Extended Display
- **Span across displays**: Toggle (for devices with multiple outputs)
- **Different photos per display**: Toggle

---

### Keep Screen On
Override system screen sleep settings to keep the display active during screensaver.

- **Keep Screen On**: Toggle
  - When enabled: Prevents the device from entering sleep mode while the screensaver is active
  - Overrides Android's system screen timeout settings
  - Uses WakeLock to maintain screen state
  - Useful for dedicated photo frame setups or kiosk mode
  - **Warning**: May increase battery drain and screen burn-in risk on OLED displays
- **Respect Battery Saver**: Toggle
  - When enabled: Disables "Keep Screen On" when device is in battery saver mode
  - When disabled: Keeps screen on even in battery saver mode (not recommended)
- **Dim Screen After**: Toggle + duration selector
  - Dims screen brightness after period of inactivity (while keeping it on)
  - Options: 1 min, 2 min, 5 min, 10 min, Never
  - **Dim Level**: 10%, 25%, 50%, 75% of current brightness
  - Does NOT turn off screen, only reduces brightness

---

## 10. Advanced Settings

### Caching
- **Enable caching**: Toggle
- **Cache size limit**: 
  - Presets: 100MB, 500MB, 1GB, 2GB, 5GB, Unlimited
  - **Custom**: User enters number of MB (10-10000 MB)
    - Input field with numeric keyboard
    - Validation: Minimum 10 MB, Maximum 10,000 MB (10 GB)
    - Display converted value in GB when > 1024 MB
- **Preload count**: Number of photos to cache ahead (1-10)
- **Clear cache**: Button to manually clear cache
  - Shows confirmation dialog before clearing
  - Displays current cache size before clearing (e.g., "Clear 456 MB of cached photos?")
  - Shows progress indicator while clearing
  - Success message after completion
- **Cache Location**: Display current cache path (read-only)
- **Cache Statistics**:
  - Total cached photos count
  - Total cache size
  - Oldest cached photo date
  - Cache hit rate percentage

### Network
- **Only on Wi-Fi**: Toggle (prevent mobile data usage)
- **Sync interval**: How often to check for new photos (Never, Hourly, Daily, Weekly)
- **Timeout duration**: Network request timeout (10s, 30s, 60s)

### Power Management
- **Keep screen on**: See "Keep Screen On" section above (Appearance > Keep Screen On)
- **Screen brightness**: Auto, Custom level (0-100%)
- **Wake lock**: Toggle (prevent device from sleeping during screensaver)

### Behavior
- **Start screensaver on**: Dock, Charging, Idle, Manual only, Custom Schedule, Timer
- **Exit screensaver on**: Touch, Remote button, Shake, Voice command
- **Pause on notification**: Toggle (pause when notifications appear)

---

### Autostart Schedule
Automatically start the screensaver at specific times and days.

- **Enable Schedule**: Toggle to enable/disable scheduled autostart
- **Start Time**: Time picker to set when screensaver should start
  - Format: Follows device's 12/24 hour setting
  - Precision: Hours and minutes (e.g., 7:30 PM)
- **Days of Week**: Multi-select checkboxes for each day
  - Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
  - Quick presets:
    - Weekdays (Mon-Fri)
    - Weekends (Sat-Sun)
    - Every Day
    - Custom (manual selection)
- **Repeat**: Toggle
  - When enabled: Screensaver starts every selected day at the configured time
  - When disabled: Screensaver starts only once on the next occurrence of the selected day(s)
- **Start Only When Charging**: Toggle
  - When enabled: Screensaver will only start at the scheduled time if device is charging
  - When disabled: Screensaver starts at scheduled time regardless of charging state
  - Useful for avoiding screensaver during scheduled time when using device on battery
- **Multiple Schedules**: Allow user to add multiple schedule entries
  - Add Schedule button
  - Each schedule can have different times and days
  - Enable/disable individual schedules
  - Reorder schedules (drag and drop)
  - Delete schedule option
  - Maximum: 5 schedules

**Example Use Cases:**
- Start at 8:00 PM every night when charging
- Start at 12:00 PM on weekends only
- Start at 6:00 PM on weekdays when charging, 10:00 AM on weekends

---

### Autostop Schedule
Automatically stop the screensaver at specific times and days.

- **Enable Schedule**: Toggle to enable/disable scheduled autostop
- **Stop Time**: Time picker to set when screensaver should stop
  - Format: Follows device's 12/24 hour setting
  - Precision: Hours and minutes (e.g., 7:00 AM)
- **Days of Week**: Multi-select checkboxes for each day
  - Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
  - Quick presets:
    - Weekdays (Mon-Fri)
    - Weekends (Sat-Sun)
    - Every Day
    - Custom (manual selection)
  - **Sync with Autostart Days**: Toggle (when enabled, uses same days as autostart schedule)
- **Repeat**: Toggle
  - When enabled: Screensaver stops every selected day at the configured time
  - When disabled: Screensaver stops only once on the next occurrence
- **Multiple Schedules**: Allow user to add multiple stop schedule entries
  - Add Schedule button
  - Each schedule can have different times and days
  - Enable/disable individual schedules
  - Reorder schedules (drag and drop)
  - Delete schedule option
  - Maximum: 5 schedules

**Example Use Cases:**
- Stop at 7:00 AM every morning
- Stop at 11:00 PM on weekdays, 9:00 AM on weekends
- Stop at sunrise (using location-based sunrise time)

---

### Start by Timer
Start the screensaver after a period of inactivity or manually with a countdown timer.

- **Enable Timer**: Toggle to enable/disable timer-based start
- **Timer Mode**: 
  - **Idle Timer**: Start after device is idle for X time (default behavior for Android screensaver)
  - **Manual Countdown**: User manually starts timer, screensaver begins when timer expires
  - **Both**: Allow both modes

**Idle Timer Settings** (when Idle Timer mode is enabled):
- **Idle Duration**: How long device must be idle before screensaver starts
  - Presets (Minutes): 1, 2, 3, 5, 10, 15, 30
  - Presets (Hours): 1, 2, 3, 6, 12
  - Custom: User can enter custom number of minutes (1-120) or hours (1-24)
  - Input format: `[number] [minutes|hours]`
  - Default: 5 minutes
- **Reset on Interaction**: Toggle (reset timer when user interacts with device)

**Manual Countdown Settings** (when Manual Countdown mode is enabled):
- **Countdown Duration**: How long until screensaver starts when manually triggered
  - Presets (Minutes): 1, 2, 3, 5, 10, 15, 30, 60
  - Presets (Hours): 1, 2, 3, 6
  - Custom: User can enter custom number
  - Default: 5 minutes
- **Show Countdown**: Toggle (display countdown overlay on screen)
  - Position: Top-right, Bottom-right, Center
  - Size: Small, Medium, Large
  - Opacity: 0-100%

**Timer Override**:
- Allow timer to be overridden by schedule (when both are enabled)
- Priority: Schedule > Timer > Manual (configurable)

---

## 11. Sync & Network Settings

### Sync Interval
Control how often the app checks for new photos from enabled sources.

- **Sync Mode**:
  - **Automatic**: App automatically syncs at regular intervals (default)
  - **Custom**: User defines custom sync interval
  - **Manual Only**: Sync only when user manually triggers it

**Automatic Sync Settings**:
- **Default Intervals**:
  - Every 15 minutes
  - Every 30 minutes
  - Every 1 hour
  - Every 3 hours
  - Every 6 hours
  - Every 12 hours
  - Every 24 hours (Daily)
  - Default: Every 1 hour

**Custom Sync Interval** (when Custom mode is selected):
- **Custom Duration**: User enters custom number
  - Minutes: 1-120 minutes
  - Hours: 1-48 hours
  - Input format: `[number] [minutes|hours]`
  - Example: "45 minutes" or "2 hours"
- **Minimum Interval**: 5 minutes (to prevent excessive API calls)

### Sync Behavior
- **Sync on App Open**: Toggle (sync when app is opened)
- **Sync on Source Enable**: Toggle (sync immediately when a source is enabled)
- **Sync on Schedule**: Toggle (sync at specific times, separate from screensaver schedule)
- **Background Sync**: Toggle (allow sync even when app is not open)
  - Requires WorkManager scheduling
  - Respects Android's battery optimization settings

### Network Settings
- **Only on Wi-Fi**: Toggle (prevent mobile data usage for sync and photo loading)
- **Allow on Mobile Data**: Toggle (when enabled, allow syncing/loading on mobile data)
  - **Mobile Data Limit**: Toggle + limit (GB per month) to prevent excessive data usage
- **Timeout Duration**: Network request timeout (10s, 30s, 60s, 120s)
- **Retry Failed Sync**: Toggle (automatically retry failed sync attempts)
  - **Max Retries**: 1, 2, 3, 5
  - **Retry Delay**: 1 min, 5 min, 15 min, 30 min, 1 hour

### Sync Status
- **Last Sync Time**: Display when last sync occurred
- **Next Sync Time**: Display when next sync is scheduled
- **Sync History**: Button to view recent sync logs
- **Force Sync Now**: Button to manually trigger immediate sync
- **Sync Progress**: Show progress bar when sync is in progress

---

## 12. Source-Specific Settings

### Dropbox
- App key/secret (for advanced users)
- Sync folder path
- Include shared folders: Toggle

### Google Drive / Google Photos
- Account selection
- Include shared with me: Toggle
- Include albums: Toggle
- Photo quality: Original, Storage saver

### OneDrive
- Account selection
- Include shared folders: Toggle
- Personal/Business account selection

### Local Network (SMB/WebDAV)
- Server address
- Port
- Username/Password
- Domain (for SMB)
- Path/Share name
- Anonymous access: Toggle
- Test connection button

---

## Settings Organization

### Settings Screen Layout:
```
┌─────────────────────────────────────────┐
│  Sources                                │
│  ├─ Gallery               [✓]      >   │
│  ├─ Dropbox               [✓]      >   │
│  ├─ Google Drive           [ ]      >   │
│  ├─ Google Photos          [✓]      >   │
│  ├─ OneDrive               [ ]      >   │
│  └─ Local Network          [ ]      >   │
├─────────────────────────────────────────┤
│  Media & Content                        │
│  ├─ Media Order          [Shuffle]  >   │
│  ├─ Content Filter    [Images+Videos]>  │
│  └─ Match Orientation     [OFF]         │
├─────────────────────────────────────────┤
│  Slideshow                              │
│  ├─ Display Time          [5s]       >   │
│  └─ Video Playback        [Muted]    >   │
├─────────────────────────────────────────┤
│  Display Effect       [Crop to Fit] >   │
├─────────────────────────────────────────┤
│  Transition Effect       [Fade]      >   │
├─────────────────────────────────────────┤
│  Decorations                            │
│  ├─ Date                  [OFF]         │
│  ├─ Clock                 [OFF]         │
│  └─ Weather               [OFF]         │
├─────────────────────────────────────────┤
│  Photo Info              [ON]       >   │
│  ├─ File Name             [ON]          │
│  ├─ Folder Name           [OFF]         │
│  ├─ Date Taken            [ON]          │
│  └─ Fade After            [5s]      >   │
├─────────────────────────────────────────┤
│  Schedule & Timer                       │
│  ├─ Autostart        [Scheduled]    >   │
│  ├─ Autostop         [7:00 AM]     >   │
│  └─ Start by Timer      [OFF]           │
├─────────────────────────────────────────┤
│  Display & Power                        │
│  ├─ Screen Rotation    [System]     >   │
│  └─ Keep Screen On        [ON]          │
├─────────────────────────────────────────┤
│  Sync & Network                         │
│  ├─ Sync Interval      [Auto]       >   │
│  ├─ Wi-Fi Only          [ON]            │
│  └─ Timeout              [30s]      >   │
├─────────────────────────────────────────┤
│  Appearance                             │
│  ├─ Background            [Black]    >   │
│  └─ Cache Limit         [500MB]     >   │
├─────────────────────────────────────────┤
│  Advanced                               │
│  ├─ Power Management        >           │
│  ├─ Exit Trigger         [Touch]    >   │
│  └─ About                   >           │
├─────────────────────────────────────────┤
│  [Preview Screensaver]                  │
│  [Reset to Defaults]                    │
├─────────────────────────────────────────┤
│  Version 1.0.0 (build 1)                │
│  (Tap 7 times for debug mode)           │
└─────────────────────────────────────────┘
```

### Folder Selection Screen:
```
┌─────────────────────────────────────────┐
│  ← Dropbox Folders                      │
│  [Search folders...]              [✕]   │
├─────────────────────────────────────────┤
│  [Select All] [Deselect All]            │
├─────────────────────────────────────────┤
│  ☑ 📁 Camera Uploads          (1,234)   │
│  │  ☑ 📁 2024                  (456)    │
│  │  ☑ 📁 2023                  (778)    │
│  ☑ 📁 Screenshots             (89)      │
│  ☐ 📁 Shared                  (234)     │
│  ☑ 📁 Work Projects           (567)     │
│  │  ☑ 📁 Design Assets       (123)     │
│  │  ☐ 📁 Documents           (444)     │
│  ☐ 📁 Personal                (345)     │
├─────────────────────────────────────────┤
│  Include subfolders       [ON]          │
├─────────────────────────────────────────┤
│  Selected: 3 folders (1,890 photos)     │
│                                         │
│  [Cancel]              [Save]           │
└─────────────────────────────────────────┘
```

### Decorations Customize Screen:
```
┌─────────────────────────────────────────┐
│  ← Customize Decorations                │
├─────────────────────────────────────────┤
│  DATE                                   │
│  Enable                   [OFF]         │
│  Format         [January 15, 2026]  >   │
│  Position            [Bottom Left]  >   │
│  Size                  [Medium]     >   │
│  Color               [White]       >    │
│  Animation         [Pulse Softly]   >   │
├─────────────────────────────────────────┤
│  CLOCK                                  │
│  Enable                    [ON]         │
│  Format                 [12-hour]   >   │
│  Show Seconds           [OFF]           │
│  Position           [Bottom Right]  >   │
│  Size                  [Large]      >   │
│  Animation         [Pulse Softly]   >   │
├─────────────────────────────────────────┤
│  WEATHER                                │
│  Enable                    [ON]         │
│  Location         [San Francisco]   >   │
│  Units               [Fahrenheit]   >   │
│  Customize                     [⚙️] >   │
│  Position             [Top Right]   >   │
│  Animation            [Static]      >   │
├─────────────────────────────────────────┤
│  [Preview Decorations]                  │
└─────────────────────────────────────────┘
```

### Video Playback Settings Screen:
```
┌─────────────────────────────────────────┐
│  ← Video Playback Settings              │
├─────────────────────────────────────────┤
│  Audio                                  │
│  ○ Mute                                 │
│  ● Use System Volume                    │
│  ○ Use Custom Volume        [75%]   >   │
├─────────────────────────────────────────┤
│  Playback                               │
│  Auto-play videos          [ON]         │
│  Max video duration       [2 min]   >   │
│  Loop short videos         [ON]         │
│  Show playback controls   [OFF]         │
├─────────────────────────────────────────┤
│  Video Display Mode                     │
│  ○ Play full duration                   │
│  ○ Play fixed time        [30s]     >   │
│  ● Extract still frame                  │
│     Still timestamp   [Beginning]   >   │
├─────────────────────────────────────────┤
│  [Preview Video Handling]               │
└─────────────────────────────────────────┘
```

### Photo Information Settings Screen:
```
┌─────────────────────────────────────────┐
│  ← Photo Information                    │
├─────────────────────────────────────────┤
│  Show Photo Information    [ON]         │
├─────────────────────────────────────────┤
│  Fields to Display                      │
│  ☑ File Name                            │
│     Show extension          [OFF]       │
│  ☐ Folder Name                          │
│  ☑ Date Taken            [Short]    >   │
│  ☐ Source Name                          │
│  ☐ Description/Caption                  │
│  ☐ Dimensions                           │
│  ☐ File Size                            │
├─────────────────────────────────────────┤
│  Display Behavior                       │
│  Fade out after           [5s]      >   │
│  Fade duration             [1s]     >   │
├─────────────────────────────────────────┤
│  Appearance                             │
│  Position          [Bottom Left]    >   │
│  Layout            [Horizontal]     >   │
│  Separator            [Bullet •]    >   │
│  Size                [Medium]       >   │
│  Color               [White]       >    │
│  Background      [Semi-Transparent] >   │
│  Background opacity       [60%]     >   │
│  Text shadow              [ON]          │
│  Shadow intensity       [Medium]    >   │
├─────────────────────────────────────────┤
│  [Preview Photo Info]                   │
└─────────────────────────────────────────┘
```

### Schedule & Timer Settings Screen:
```
┌─────────────────────────────────────────┐
│  ← Schedule & Timer                     │
├─────────────────────────────────────────┤
│  AUTOSTART                              │
│  Enable Autostart          [ON]         │
│  Mode                [Scheduled]    >   │
├─────────────────────────────────────────┤
│  Schedule 1                  [ON]   >   │
│  └─ 8:00 PM • Weekdays • Charging      │
│  Schedule 2                  [ON]   >   │
│  └─ 12:00 PM • Weekends                │
│  [+ Add Schedule]                       │
├─────────────────────────────────────────┤
│  AUTOSTOP                               │
│  Enable Autostop           [ON]         │
├─────────────────────────────────────────┤
│  Schedule 1                  [ON]   >   │
│  └─ 7:00 AM • Every day                │
│  [+ Add Schedule]                       │
├─────────────────────────────────────────┤
│  START BY TIMER                         │
│  Enable Timer              [OFF]        │
│  Timer Mode             [Idle]      >   │
│  Idle Duration         [5 min]      >   │
├─────────────────────────────────────────┤
│  [View Full Schedule]                   │
└─────────────────────────────────────────┘
```

### Autostart Schedule Edit Screen:
```
┌─────────────────────────────────────────┐
│  ← Edit Autostart Schedule              │
├─────────────────────────────────────────┤
│  Enable Schedule           [ON]         │
├─────────────────────────────────────────┤
│  Start Time              [8:00 PM]  >   │
├─────────────────────────────────────────┤
│  Days of Week                           │
│  ☑ Mon  ☑ Tue  ☑ Wed  ☑ Thu  ☑ Fri   │
│  ☐ Sat  ☐ Sun                           │
│                                         │
│  Quick: [Weekdays] [Weekends] [Every]   │
├─────────────────────────────────────────┤
│  Repeat                    [ON]         │
├─────────────────────────────────────────┤
│  Start only when charging  [ON]         │
├─────────────────────────────────────────┤
│  ────────────────────────────────────   │
│                                         │
│  [Cancel]              [Save]           │
└─────────────────────────────────────────┘
```

### Sync & Network Settings Screen:
```
┌─────────────────────────────────────────┐
│  ← Sync & Network                       │
├─────────────────────────────────────────┤
│  SYNC INTERVAL                          │
│  Mode                   [Auto]      >   │
│  Interval              [1 hour]     >   │
├─────────────────────────────────────────┤
│  Sync on app open          [ON]         │
│  Background sync             [ON]       │
├─────────────────────────────────────────┤
│  Last sync: 15 minutes ago              │
│  Next sync: 45 minutes                  │
│  [Sync Now]                             │
├─────────────────────────────────────────┤
│  NETWORK                                │
│  Wi-Fi only                [ON]         │
│  Timeout duration         [30s]     >   │
│  Retry failed sync           [ON]       │
│  Max retries                [3]     >   │
├─────────────────────────────────────────┤
│  [View Sync History]                    │
└─────────────────────────────────────────┘
```

---

## 12. Source-Specific Settings

### Dropbox
- App key/secret (for advanced users)
- Sync folder path
- Include shared folders: Toggle

### Google Drive / Google Photos
- Account selection
- Include shared with me: Toggle
- Include albums: Toggle
- Photo quality: Original, Storage saver

### OneDrive
- Account selection
- Include shared folders: Toggle
- Personal/Business account selection

### Local Network (SMB/WebDAV)
- Server address
- Port
- Username/Password
- Domain (for SMB)
- Path/Share name
- Anonymous access: Toggle
- Test connection button

---

## 13. Development & Support

### Development Resources
- **Discord Community**: Join our development Discord server for updates, beta testing, and community support
  - Link: Available in the app's About section or visit our GitHub repository
  - Get early access to beta features
  - Report bugs and suggest features
  - Connect with other users and developers

### About
- **App Version**: Display current version number (read from build.gradle versionName)
  - Format: "Version X.Y.Z (build N)"
  - Example: "Version 1.2.0 (build 15)"
  - Location: Displayed at bottom of settings screen AND in About screen
  - Tap 7 times to enable debug mode (Easter egg)
- **Build Number**: Display build number (read from build.gradle versionCode)
- **GitHub Repository**: Link to source code
- **License**: View license information
- **Privacy Policy**: Link to privacy policy
- **Terms of Service**: Link to terms of service

### Version Update Policy
The version number in `app/build.gradle.kts` MUST be updated whenever:
- **New feature added**: Increment minor version (e.g., `1.0.0` → `1.1.0`)
- **Existing feature completed/updated**: Increment minor version (e.g., `1.0.0` → `1.1.0`)
- **Bug fix or minor change**: Increment patch version (e.g., `1.0.0` → `1.0.1`)
- **Major release**: Increment major version (e.g., `1.0.0` → `2.0.0`)
- **Every update**: Increment versionCode by 1

All version changes must be documented in `CHANGELOG.md`.

### Debug (Development Mode Only)
- **Enable Debug Mode**: Toggle (requires tapping version number 7 times)
- **Show Debug Overlay**: Toggle (shows FPS, memory usage, cache stats)
- **Export Logs**: Button to export app logs for troubleshooting
- **Reset All Settings**: Button to factory reset all settings (with confirmation)
- **Test Crash Reporting**: Button to test crash reporting (development only)

---

## Settings Storage

All settings are stored using:
- **DataStore Preferences**: For user preferences
- **Room Database**: For source configurations and folder selections
- **EncryptedSharedPreferences**: For OAuth tokens and sensitive credentials
