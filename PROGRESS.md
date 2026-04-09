# Progress

## Current Status

All known bugs from the settings audit have been fixed. The app is stable with all 14+ settings properly wired from UI → DataStore → runtime consumption.

## Recent Fixes (Latest Session)

### Video Playback Settings (9 settings fully wired)
- All 9 video fields now have `PreferencesKey` entries in SettingsManager
- `getSlideshowConfig()` reads all video fields from DataStore
- `saveSlideshowConfig()` writes all video fields to DataStore
- `SlideshowView.showPhoto()` applies all video config at runtime:
  - ExoPlayer volume based on `videoAudioMode` + `videoCustomVolume`
  - `playWhenReady` respects `videoAutoPlay`
  - `repeatMode` set to `REPEAT_MODE_ONE` when `videoLoopShort`
  - `useController` toggled based on `videoShowControls`
  - `videoMaxDurationSeconds` enforced via cancelable coroutine timer
  - `PLAY_FIXED` mode stops video after `videoFixedPlaySeconds`
  - `EXTRACT_STILL` mode seeks to timestamp and pauses

### Settings → Runtime Wiring (7 fixes)
- Background color now applied to SlideshowView
- Keep screen on now sets FLAG_KEEP_SCREEN_ON
- Screen rotation applied via WindowManager
- Exit trigger (touch) handled via GestureDetector
- Wi-Fi only checks ConnectivityManager before cloud fetches
- Match orientation adjusts scaleType for mismatched photos
- Video double-advance race condition fixed with isAdvancing guard

### Settings Persistence (4 fixes)
- Clear cache now actually clears Coil memory and disk cache
- Start by timer switch persists timerConfig.enabled
- Cache limit (cacheSizeLimitMB + usePresetLimit) fully persisted
- All 14+ preferences now save, read back, and sync UI state

### Lifecycle & Cleanup
- DreamService onDetachedFromWindow nulls out references
- Proper cleanup logging

## Build Status

- Debug build: PASSING
- No compilation errors
- Only pre-existing warnings (deprecated APIs, unused variables in repos)

## Next Priorities

1. Gallery photo preloading in SlideshowManager
2. Photo count display per source on main screen
3. Source status indicators (connected/syncing/error)
4. Date/clock/weather overlay rendering in SlideshowView
5. Photo info overlay during slideshow
