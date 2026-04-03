package com.vincentwetzel.androidscreensaver.data.model

/**
 * Represents a photo source type
 */
enum class SourceType {
    GALLERY,
    DROPBOX,
    GOOGLE_DRIVE,
    GOOGLE_PHOTOS,
    ONEDRIVE,
    LOCAL_NETWORK
}

/**
 * Represents the authentication state of a source
 */
data class SourceAuthState(
    val sourceType: SourceType,
    val isAuthenticated: Boolean,
    val accountName: String? = null,
    val lastSyncTime: Long? = null
)

/**
 * Represents a photo in the system
 */
data class Photo(
    val id: String,
    val sourceType: SourceType,
    val uri: String,
    val thumbnailUri: String? = null,
    val title: String? = null,
    val description: String? = null,
    val dateTaken: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val fileSize: Long? = null,
    val cachedLocalPath: String? = null
)

/**
 * Represents a folder/album in a source
 */
data class PhotoFolder(
    val id: String,
    val sourceType: SourceType,
    val name: String,
    val parentFolderId: String? = null,
    val photoCount: Int = 0,
    val thumbnailUri: String? = null
)

/**
 * Slideshow configuration
 */
data class SlideshowConfig(
    // Display time
    val slideDurationSeconds: Int = 5,
    val isCustomDuration: Boolean = false,
    
    // Media order
    val shuffle: Boolean = true,
    val reshuffleInterval: ReshuffleInterval = ReshuffleInterval.AFTER_CYCLE,
    val photoOrder: PhotoOrder = PhotoOrder.DATE_NEWEST_FIRST,
    
    // Content filter
    val mediaTypeFilter: MediaTypeFilter = MediaTypeFilter.IMAGES_AND_VIDEOS,
    val matchDeviceOrientation: Boolean = false,
    val dateRangeStart: Long? = null, // Timestamp
    val dateRangeEnd: Long? = null, // Timestamp
    val minFileSizeKB: Long? = null,
    val maxFileSizeMB: Long? = null,
    
    // Video playback
    val videoAudioMode: VideoAudioMode = VideoAudioMode.SYSTEM_VOLUME,
    val videoCustomVolume: Int = 75,
    val videoMaxDurationSeconds: Int = 120,
    val videoAutoPlay: Boolean = true,
    val videoShowControls: Boolean = false,
    val videoLoopShort: Boolean = true,
    val videoDisplayMode: VideoDisplayMode = VideoDisplayMode.PLAY_FULL,
    val videoFixedPlaySeconds: Int = 30,
    val videoStillTimestamp: VideoStillTimestamp = VideoStillTimestamp.BEGINNING,
    
    // Display effects
    val displayEffect: DisplayEffect = DisplayEffect.CROP_TO_FIT,
    val panDirection: PanDirection = PanDirection.RANDOM,
    val zoomRange: ClosedFloatingPointRange<Float> = 1.0f..1.5f,
    val animationDurationSeconds: Float? = null, // null = same as display time
    
    // Transition effects
    val transitionEffect: TransitionEffect = TransitionEffect.FADE,
    val transitionDurationMs: Int = 1000,
    val transitionEasing: TransitionEasing = TransitionEasing.EASE_IN_OUT,
    val transitionDirection: TransitionDirection = TransitionDirection.LEFT,
    
    // Decorations
    val dateDecoration: DecorationConfig? = null,
    val clockDecoration: ClockDecorationConfig? = null,
    val weatherDecoration: WeatherDecorationConfig? = null,
    val decorationFontFamily: DecorationFontFamily = DecorationFontFamily.SYSTEM_DEFAULT,
    val decorationTextShadow: Boolean = true,
    val decorationShadowIntensity: ShadowIntensity = ShadowIntensity.MEDIUM,
    val decorationMargin: DecorationMargin = DecorationMargin.MEDIUM,
    val decorationSpacing: DecorationSpacing = DecorationSpacing.MEDIUM,
    val unifiedPulseAnimation: Boolean = false,
    
    // Photo information
    val photoInfoConfig: PhotoInfoConfig = PhotoInfoConfig(),
    
    // Appearance
    val backgroundColor: Int = 0xFF000000.toInt(),
    val screenOrientation: ScreenOrientation = ScreenOrientation.SYSTEM_DEFAULT,
    val keepScreenOn: Boolean = false,
    val respectBatterySaver: Boolean = true,
    val dimScreenAfter: Boolean = false,
    val dimScreenAfterMinutes: Int = 5,
    val dimLevel: Int = 50,
    
    // Caching
    val cacheConfig: CacheConfig = CacheConfig(),
    
    // Network
    val wifiOnly: Boolean = true,
    val syncInterval: SyncInterval = SyncInterval.DAILY,
    val networkTimeoutSeconds: Int = 30,
    
    // Behavior
    val startOnTrigger: ScreensaverTrigger = ScreensaverTrigger.DOCK,
    val exitOnTrigger: ScreensaverExitTrigger = ScreensaverExitTrigger.TOUCH,
    val pauseOnNotification: Boolean = false,
    
    // Schedule & Timer
    val autostartSchedules: List<ScheduleConfig> = emptyList(),
    val autostopSchedules: List<ScheduleConfig> = emptyList(),
    val timerConfig: TimerConfig = TimerConfig(),
    
    // Sync & Network
    val syncConfig: SyncConfig = SyncConfig()
)

/**
 * Display effects for individual photos
 */
enum class DisplayEffect {
    PAN,                // Ken Burns pan effect
    SCALE_TO_FIT,       // Scale to fit center with letterboxing
    CROP_TO_FIT,        // Crop to fill center
    ZOOM,               // Zoom in effect
    FOCUS               // Blur to sharp transition
}

/**
 * Pan direction for PAN display effect
 */
enum class PanDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP,
    RANDOM
}

/**
 * Transition effects for slideshow
 */
enum class TransitionEffect {
    // Basic
    FADE,
    CROSS_FADE,
    
    // Motion
    WIPE,
    SLIDE,
    SWAP,
    
    // 3D
    CUBE,
    DOORWAY,
    RADIAL,
    
    // Artistic
    MEMORY,
    ILLUSION,
    RIPPLE,
    FLASH,
    STAR,
    WIND,
    CIRCLE
}

/**
 * Transition direction for directional effects
 */
enum class TransitionDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN,
    RANDOM
}

/**
 * Transition easing curves
 */
enum class TransitionEasing {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT
}

/**
 * Image fit modes (legacy, kept for compatibility)
 */
enum class ImageFit {
    CROP,
    FIT_CENTER,
    CENTER_INSIDE
}

/**
 * Reshuffle interval for shuffle mode
 */
enum class ReshuffleInterval {
    AFTER_CYCLE,    // Reshuffle after showing all photos once
    EVERY_MINUTE,   // Reshuffle every minute
    EVERY_5_MINUTES,
    NEVER           // Fixed random order
}

/**
 * Photo ordering when shuffle is disabled
 */
enum class PhotoOrder {
    DATE_NEWEST_FIRST,
    DATE_OLDEST_FIRST,
    NAME_A_Z,
    NAME_Z_A,
    SIZE_LARGEST_FIRST,
    SIZE_SMALLEST_FIRST,
    RANDOM
}

/**
 * Clock position on screen
 */
enum class ClockPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
}

/**
 * Clock time format
 */
enum class ClockFormat {
    HOUR_12,
    HOUR_24
}

/**
 * Clock size
 */
enum class ClockSize {
    SMALL,
    MEDIUM,
    LARGE
}

/**
 * Photo info fields to display
 */
enum class PhotoInfoField {
    FILENAME,
    DATE,
    SOURCE,
    DESCRIPTION
}

/**
 * Cache size limits
 */
enum class CacheSizeLimit {
    MB_100,
    MB_500,
    GB_1,
    GB_2,
    GB_5,
    UNLIMITED
}

/**
 * Sync intervals for checking new photos
 */
enum class SyncInterval {
    NEVER,
    HOURLY,
    DAILY,
    WEEKLY
}

/**
 * Screensaver start triggers
 */
enum class ScreensaverTrigger {
    DOCK,           // Start when device is docked
    CHARGING,       // Start when charging
    IDLE,           // Start after period of inactivity
    MANUAL_ONLY     // Only start manually
}

/**
 * Screensaver exit triggers
 */
enum class ScreensaverExitTrigger {
    TOUCH,          // Exit on screen touch
    REMOTE_BUTTON,  // Exit on remote control button
    SHAKE,          // Exit on device shake
    VOICE_COMMAND   // Exit on voice command
}

/**
 * Represents a source configuration in the system
 */
data class SourceConfig(
    val sourceType: SourceType,
    val enabled: Boolean = false,
    val selectedFolders: List<SelectedFolder> = emptyList(),
    val includeSubfolders: Boolean = true,
    val authState: SourceAuthState? = null,
    val lastSyncTime: Long? = null,
    val photoCount: Int = 0
)

/**
 * Represents a selected folder with its state
 */
data class SelectedFolder(
    val folderId: String,
    val folderName: String,
    val parentFolderId: String? = null,
    val path: String,
    val isSelected: Boolean = true,
    val includeSubfolders: Boolean = true,
    val photoCount: Int = 0
)

/**
 * Media type filter for content filter
 */
enum class MediaTypeFilter {
    IMAGES_AND_VIDEOS,
    IMAGES_ONLY,
    VIDEOS_ONLY
}

/**
 * Video audio mode
 */
enum class VideoAudioMode {
    MUTE,
    SYSTEM_VOLUME,
    CUSTOM_VOLUME
}

/**
 * Video display mode
 */
enum class VideoDisplayMode {
    PLAY_FULL,         // Play full duration (respects max)
    PLAY_FIXED,        // Play fixed time
    EXTRACT_STILL      // Extract still frame
}

/**
 * Video still timestamp
 */
enum class VideoStillTimestamp {
    BEGINNING,
    MIDDLE,
    END,
    CUSTOM
}

/**
 * Date format for decoration
 */
enum class DateFormat {
    MONTH_DAY,            // January 15
    WEEKDAY,              // Monday
    YEAR,                 // 2026
    ABBREVIATE_MONTH,     // Jan 15
    ABBREVIATE_WEEKDAY,   // Mon
    NUMERIC_DATE,         // 01/15/2026
    FULL_DATE,            // Monday, January 15, 2026
    SHORT_DATE,           // Jan 15, 2026
    CUSTOM                // Custom pattern
}

/**
 * Weather unit system
 */
enum class TemperatureUnit {
    FAHRENHEIT,
    CELSIUS
}

enum class WindSpeedUnit {
    MPH,
    KMH,
    MS
}

enum class PressureUnit {
    INHG,
    HPA,
    MBAR
}

enum class VisibilityUnit {
    MILES,
    KILOMETERS
}

/**
 * Weather provider
 */
enum class WeatherProvider {
    OPEN_METEO,         // Primary: Open-Meteo (100% free, no API key)
    WEATHER_GOV,        // US-only fallback (weather.gov, free)
    OPENWEATHERMAP,     // Future option
    WEATHERAPI,         // Future option
    ACCUWEATHER         // Future option
}

/**
 * Weather data points to display
 */
data class WeatherDataPoints(
    val showTemperature: Boolean = true,
    val showConditionIcon: Boolean = true,
    val showConditionText: Boolean = true,
    val showRainChance: Boolean = true,
    val showHumidity: Boolean = false,
    val showWind: Boolean = false,
    val showFeelsLike: Boolean = false,
    val showHighLow: Boolean = false
)

/**
 * Weather icon style
 */
enum class WeatherIconStyle {
    MINIMAL,
    DETAILED,
    ANIMATED
}

/**
 * Weather widget background
 */
enum class WeatherWidgetBackground {
    TRANSPARENT,
    FROSTED_GLASS,
    SOLID
}

/**
 * Decoration animation mode
 */
enum class DecorationAnimation {
    STATIC,
    PULSE_SOFTLY
}

/**
 * Pulse speed for decoration animation
 */
enum class PulseSpeed {
    SLOW,
    MEDIUM,
    FAST
}

/**
 * Decoration font family
 */
enum class DecorationFontFamily {
    SYSTEM_DEFAULT,
    SANS_SERIF,
    SERIF,
    MONOSPACE,
    CUSTOM
}

/**
 * Shadow intensity for text
 */
enum class ShadowIntensity {
    LIGHT,
    MEDIUM,
    HEAVY
}

/**
 * Decoration margin from edge
 */
enum class DecorationMargin {
    SMALL,
    MEDIUM,
    LARGE
}

/**
 * Decoration spacing
 */
enum class DecorationSpacing {
    SMALL,
    MEDIUM,
    LARGE
}

/**
 * Date decoration configuration
 */
data class DecorationConfig(
    val enabled: Boolean = false,
    val position: ClockPosition = ClockPosition.BOTTOM_LEFT,
    val dateFormat: DateFormat = DateFormat.FULL_DATE,
    val customDateFormatPattern: String? = null,
    val fontSize: ClockSize = ClockSize.MEDIUM,
    val fontColor: Int = 0xFFFFFFFF.toInt(),
    val opacity: Int = 100,
    val background: DecorationBackground = DecorationBackground.NONE,
    val animation: DecorationAnimation = DecorationAnimation.STATIC,
    val pulseSpeed: PulseSpeed = PulseSpeed.MEDIUM,
    val pulseMinOpacity: Int = 60,
    val pulseMaxOpacity: Int = 100
)

/**
 * Clock decoration configuration
 */
data class ClockDecorationConfig(
    val enabled: Boolean = false,
    val position: ClockPosition = ClockPosition.BOTTOM_RIGHT,
    val clockFormat: ClockFormat = ClockFormat.HOUR_12,
    val showSeconds: Boolean = false,
    val fontSize: ClockSize = ClockSize.MEDIUM,
    val fontColor: Int = 0xFFFFFFFF.toInt(),
    val opacity: Int = 100,
    val background: DecorationBackground = DecorationBackground.NONE,
    val animation: DecorationAnimation = DecorationAnimation.STATIC,
    val pulseSpeed: PulseSpeed = PulseSpeed.MEDIUM,
    val pulseMinOpacity: Int = 60,
    val pulseMaxOpacity: Int = 100
)

/**
 * Weather decoration configuration
 */
data class WeatherDecorationConfig(
    val enabled: Boolean = false,
    val position: ClockPosition = ClockPosition.TOP_RIGHT,
    val useDeviceLocation: Boolean = true,
    val manualLocation: String? = null,
    val locationUpdateInterval: LocationUpdateInterval = LocationUpdateInterval.HOUR_1,
    val dataPoints: WeatherDataPoints = WeatherDataPoints(),
    val temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    val windSpeedUnit: WindSpeedUnit = WindSpeedUnit.MPH,
    val pressureUnit: PressureUnit = PressureUnit.INHG,
    val visibilityUnit: VisibilityUnit = VisibilityUnit.MILES,
    val iconStyle: WeatherIconStyle = WeatherIconStyle.DETAILED,
    val widgetBackground: WeatherWidgetBackground = WeatherWidgetBackground.TRANSPARENT,
    val fontSize: ClockSize = ClockSize.MEDIUM,
    val fontColor: Int = 0xFFFFFFFF.toInt(),
    val opacity: Int = 100,
    val animation: DecorationAnimation = DecorationAnimation.STATIC,
    val pulseSpeed: PulseSpeed = PulseSpeed.MEDIUM,
    val pulseMinOpacity: Int = 60,
    val pulseMaxOpacity: Int = 100,
    val weatherProvider: WeatherProvider = WeatherProvider.OPEN_METEO,
    val apiKey: String? = null  // Not needed for Open-Meteo, but kept for future providers
)

/**
 * Decoration background options
 */
enum class DecorationBackground {
    NONE,
    SEMI_TRANSPARENT,
    SOLID
}

/**
 * Location update interval for weather
 */
enum class LocationUpdateInterval {
    MIN_15,
    MIN_30,
    HOUR_1,
    HOUR_6
}

/**
 * Day of week for schedules
 */
enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}

/**
 * Schedule preset for quick selection
 */
enum class SchedulePreset {
    WEEKDAYS,       // Mon-Fri
    WEEKENDS,       // Sat-Sun
    EVERY_DAY,      // All days
    CUSTOM          // Manual selection
}

/**
 * Screensaver start mode
 */
enum class StartMode {
    SCHEDULED,      // Start by schedule
    IDLE,           // Start after idle time
    MANUAL_TIMER,   // Start by manual countdown
    BOTH            // Both idle and manual timer
}

/**
 * Timer mode for start by timer
 */
enum class TimerMode {
    IDLE_TIMER,         // Start after device is idle
    MANUAL_COUNTDOWN,   // Manual start with countdown
    BOTH                // Both modes enabled
}

/**
 * Sync mode
 */
enum class SyncMode {
    AUTOMATIC,      // Auto sync at intervals
    CUSTOM,         // Custom sync interval
    MANUAL_ONLY     // Manual sync only
}

/**
 * Schedule configuration for autostart/autostop
 */
data class ScheduleConfig(
    val enabled: Boolean = false,
    val timeHour: Int = 20, // 0-23
    val timeMinute: Int = 0, // 0-59
    val daysOfWeek: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
    val schedulePreset: SchedulePreset = SchedulePreset.WEEKDAYS,
    val repeat: Boolean = true,
    val onlyWhenCharging: Boolean = false, // For autostart only
    val syncWithAutostartDays: Boolean = false // For autostop only
)

/**
 * Timer configuration for start by timer
 */
data class TimerConfig(
    val enabled: Boolean = false,
    val timerMode: TimerMode = TimerMode.IDLE_TIMER,
    
    // Idle timer settings
    val idleDurationMinutes: Int = 5,
    val resetOnInteraction: Boolean = true,
    
    // Manual countdown settings
    val countdownDurationMinutes: Int = 5,
    val showCountdown: Boolean = true,
    val countdownPosition: ClockPosition = ClockPosition.TOP_RIGHT,
    val countdownSize: ClockSize = ClockSize.MEDIUM,
    val countdownOpacity: Int = 100,
    
    // Override priority
    val scheduleOverridesTimer: Boolean = true
)

/**
 * Sync configuration
 */
data class SyncConfig(
    val syncMode: SyncMode = SyncMode.AUTOMATIC,
    val syncIntervalMinutes: Int = 60, // Default 1 hour
    val syncOnAppOpen: Boolean = true,
    val syncOnSourceEnable: Boolean = true,
    val backgroundSync: Boolean = true,
    val onlyOnWifi: Boolean = true,
    val allowMobileData: Boolean = false,
    val mobileDataLimitGB: Int? = null, // null = unlimited
    val timeoutSeconds: Int = 30,
    val retryFailedSync: Boolean = true,
    val maxRetries: Int = 3,
    val retryDelayMinutes: Int = 15,
    val lastSyncTime: Long? = null,
    val nextSyncTime: Long? = null
)

/**
 * Photo info field types
 */
enum class PhotoInfoFieldType {
    FILE_NAME,
    FOLDER_NAME,
    DATE_TAKEN,
    SOURCE_NAME,
    DESCRIPTION,
    DIMENSIONS,
    FILE_SIZE
}

/**
 * Photo info date format
 */
enum class PhotoInfoDateFormat {
    FULL_DATE,      // January 15, 2026
    SHORT_DATE,     // Jan 15, 2026
    NUMERIC,        // 01/15/2026
    RELATIVE        // 2 weeks ago
}

/**
 * Photo info layout orientation
 */
enum class PhotoInfoLayout {
    HORIZONTAL,
    VERTICAL,
    COMPACT
}

/**
 * Photo info field separator
 */
enum class PhotoInfoSeparator {
    BULLET,     // •
    PIPE,       // |
    DASH,       // —
    SLASH,      // /
    COMMA       // ,
}

/**
 * Photo info background style
 */
enum class PhotoInfoBackground {
    NONE,
    SEMI_TRANSPARENT,
    SOLID,
    GRADIENT_FADE
}

/**
 * Screen orientation mode
 */
enum class ScreenOrientation {
    PORTRAIT,
    LANDSCAPE,
    SYSTEM_DEFAULT
}

/**
 * Cache configuration
 */
data class CacheConfig(
    val enabled: Boolean = true,
    val cacheSizeLimitMB: Int = 500, // Custom MB value
    val usePresetLimit: Boolean = true,
    val presetLimit: CacheSizeLimit = CacheSizeLimit.MB_500,
    val preloadCount: Int = 3,
    val cachePath: String? = null, // null = default
    val totalCachedPhotos: Int = 0,
    val totalCacheSizeMB: Int = 0,
    val oldestCachedPhotoDate: Long? = null,
    val cacheHitRate: Float = 0.0f
)

/**
 * Photo information display configuration
 */
data class PhotoInfoConfig(
    val enabled: Boolean = false,
    val showFileName: Boolean = true,
    val showFileNameWithExtension: Boolean = false,
    val showFolderName: Boolean = false,
    val showFolderFullPath: Boolean = false,
    val showDateTaken: Boolean = true,
    val dateFormat: PhotoInfoDateFormat = PhotoInfoDateFormat.SHORT_DATE,
    val showSourceName: Boolean = false,
    val showDescription: Boolean = false,
    val showDimensions: Boolean = false,
    val showFileSize: Boolean = false,
    
    // Behavior
    val fadeOutAfterSeconds: Int = 5,
    val fadeOutEnabled: Boolean = true,
    val fadeAnimationDurationMs: Int = 1000,
    
    // Appearance
    val position: ClockPosition = ClockPosition.BOTTOM_LEFT,
    val layout: PhotoInfoLayout = PhotoInfoLayout.HORIZONTAL,
    val separator: PhotoInfoSeparator = PhotoInfoSeparator.BULLET,
    val fontSize: ClockSize = ClockSize.MEDIUM,
    val fontColor: Int = 0xFFFFFFFF.toInt(),
    val fontFamily: DecorationFontFamily = DecorationFontFamily.SYSTEM_DEFAULT,
    val textShadow: Boolean = true,
    val shadowIntensity: ShadowIntensity = ShadowIntensity.MEDIUM,
    val background: PhotoInfoBackground = PhotoInfoBackground.SEMI_TRANSPARENT,
    val backgroundOpacity: Int = 60,
    val textOpacity: Int = 100
)
