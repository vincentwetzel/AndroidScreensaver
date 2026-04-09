package com.vincentwetzel.androidscreensaver.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.vincentwetzel.androidscreensaver.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

// Extension to create DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "screensaver_settings")

/**
 * Manages app settings persistence using DataStore
 */
@Singleton
object SettingsManager {

    // Preference keys
    private object PreferencesKeys {
        // Slideshow
        val SLIDE_DURATION = intPreferencesKey("slide_duration_seconds")
        val SHUFFLE = booleanPreferencesKey("shuffle")
        val PHOTO_ORDER = stringPreferencesKey("photo_order")
        val MEDIA_TYPE_FILTER = stringPreferencesKey("media_type_filter")
        val MATCH_ORIENTATION = booleanPreferencesKey("match_orientation")

        // Display effects
        val DISPLAY_EFFECT = stringPreferencesKey("display_effect")
        val PAN_DIRECTION = stringPreferencesKey("pan_direction")

        // Transition effects
        val TRANSITION_EFFECT = stringPreferencesKey("transition_effect")
        val TRANSITION_DURATION = intPreferencesKey("transition_duration_ms")
        val TRANSITION_EASING = stringPreferencesKey("transition_easing")
        val TRANSITION_DIRECTION = stringPreferencesKey("transition_direction")

        // Appearance
        val BACKGROUND_COLOR = intPreferencesKey("background_color")
        val SCREEN_ORIENTATION = stringPreferencesKey("screen_orientation")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")

        // Photo info
        val PHOTO_INFO_ENABLED = booleanPreferencesKey("photo_info_enabled")
        val PHOTO_INFO_FADE = intPreferencesKey("photo_info_fade_seconds")

        // Video playback
        val VIDEO_AUDIO_MODE = stringPreferencesKey("video_audio_mode")
        val VIDEO_CUSTOM_VOLUME = intPreferencesKey("video_custom_volume")
        val VIDEO_MAX_DURATION = intPreferencesKey("video_max_duration_seconds")
        val VIDEO_AUTO_PLAY = booleanPreferencesKey("video_auto_play")
        val VIDEO_SHOW_CONTROLS = booleanPreferencesKey("video_show_controls")
        val VIDEO_LOOP_SHORT = booleanPreferencesKey("video_loop_short")
        val VIDEO_DISPLAY_MODE = stringPreferencesKey("video_display_mode")
        val VIDEO_FIXED_SECONDS = intPreferencesKey("video_fixed_play_seconds")
        val VIDEO_STILL_TIMESTAMP = stringPreferencesKey("video_still_timestamp")

        // Decorations
        val DECORATION_DATE = booleanPreferencesKey("decoration_date")
        val DECORATION_CLOCK = booleanPreferencesKey("decoration_clock")
        val DECORATION_WEATHER = booleanPreferencesKey("decoration_weather")

        // Sync & Network
        val SYNC_INTERVAL = stringPreferencesKey("sync_interval")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val NETWORK_TIMEOUT = intPreferencesKey("network_timeout")

        // Timer
        val TIMER_ENABLED = booleanPreferencesKey("timer_enabled")

        // Cache
        val CACHE_LIMIT = intPreferencesKey("cache_limit_mb")
        val CACHE_USE_PRESET = booleanPreferencesKey("cache_use_preset")
        val ENABLE_CACHE = booleanPreferencesKey("enable_cache")

        // Sources
        val SOURCE_GOOGLE_DRIVE_ENABLED = booleanPreferencesKey("source_google_drive_enabled")
        val SOURCE_GOOGLE_DRIVE_FOLDERS = stringSetPreferencesKey("source_google_drive_folders")
        val SOURCE_GOOGLE_DRIVE_DESELECTED = stringSetPreferencesKey("source_google_drive_deselected")
        val SOURCE_GALLERY_ENABLED = booleanPreferencesKey("source_gallery_enabled")
        val SOURCE_GALLERY_FOLDERS = stringSetPreferencesKey("source_gallery_folders")
        val SOURCE_GALLERY_DESELECTED = stringSetPreferencesKey("source_gallery_deselected")
    }

    /**
     * Get slideshow configuration from settings
     */
    fun getSlideshowConfig(context: Context): SlideshowConfig {
        return runBlocking {
            val preferences = context.dataStore.data.first()

            val cacheEnabled = preferences[PreferencesKeys.ENABLE_CACHE] ?: true

            SlideshowConfig(
                // Display time
                slideDurationSeconds = preferences[PreferencesKeys.SLIDE_DURATION] ?: 5,

                // Shuffle & order
                shuffle = preferences[PreferencesKeys.SHUFFLE] ?: true,
                photoOrder = enumValueOfOrNull<PhotoOrder>(
                    preferences[PreferencesKeys.PHOTO_ORDER] ?: PhotoOrder.DATE_NEWEST_FIRST.name
                ) ?: PhotoOrder.DATE_NEWEST_FIRST,

                // Content filter
                mediaTypeFilter = enumValueOfOrNull<MediaTypeFilter>(
                    preferences[PreferencesKeys.MEDIA_TYPE_FILTER] ?: MediaTypeFilter.IMAGES_ONLY.name
                ) ?: MediaTypeFilter.IMAGES_ONLY,
                matchDeviceOrientation = preferences[PreferencesKeys.MATCH_ORIENTATION] ?: false,

                // Display effects
                displayEffect = enumValueOfOrNull<DisplayEffect>(
                    preferences[PreferencesKeys.DISPLAY_EFFECT] ?: DisplayEffect.CROP_TO_FIT.name
                ) ?: DisplayEffect.CROP_TO_FIT,
                panDirection = enumValueOfOrNull<PanDirection>(
                    preferences[PreferencesKeys.PAN_DIRECTION] ?: PanDirection.RANDOM.name
                ) ?: PanDirection.RANDOM,

                // Transition effects
                transitionEffect = enumValueOfOrNull<TransitionEffect>(
                    preferences[PreferencesKeys.TRANSITION_EFFECT] ?: TransitionEffect.FADE.name
                ) ?: TransitionEffect.FADE,
                transitionDurationMs = preferences[PreferencesKeys.TRANSITION_DURATION] ?: 1000,
                transitionEasing = enumValueOfOrNull<TransitionEasing>(
                    preferences[PreferencesKeys.TRANSITION_EASING] ?: TransitionEasing.EASE_IN_OUT.name
                ) ?: TransitionEasing.EASE_IN_OUT,
                transitionDirection = enumValueOfOrNull<TransitionDirection>(
                    preferences[PreferencesKeys.TRANSITION_DIRECTION] ?: TransitionDirection.LEFT.name
                ) ?: TransitionDirection.LEFT,

                // Appearance
                backgroundColor = preferences[PreferencesKeys.BACKGROUND_COLOR] ?: 0xFF000000.toInt(),
                screenOrientation = enumValueOfOrNull<ScreenOrientation>(
                    preferences[PreferencesKeys.SCREEN_ORIENTATION] ?: ScreenOrientation.SYSTEM_DEFAULT.name
                ) ?: ScreenOrientation.SYSTEM_DEFAULT,
                keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: false,

                // Decorations
                dateDecoration = if (preferences[PreferencesKeys.DECORATION_DATE] == true) DecorationConfig() else null,
                clockDecoration = if (preferences[PreferencesKeys.DECORATION_CLOCK] == true) ClockDecorationConfig() else null,
                weatherDecoration = if (preferences[PreferencesKeys.DECORATION_WEATHER] == true) WeatherDecorationConfig() else null,

                // Photo info
                photoInfoConfig = PhotoInfoConfig(
                    enabled = preferences[PreferencesKeys.PHOTO_INFO_ENABLED] ?: false,
                    fadeOutAfterSeconds = preferences[PreferencesKeys.PHOTO_INFO_FADE] ?: 5,
                ),

                // Video playback
                videoAudioMode = enumValueOfOrNull<VideoAudioMode>(
                    preferences[PreferencesKeys.VIDEO_AUDIO_MODE] ?: VideoAudioMode.SYSTEM_VOLUME.name
                ) ?: VideoAudioMode.SYSTEM_VOLUME,
                videoCustomVolume = preferences[PreferencesKeys.VIDEO_CUSTOM_VOLUME] ?: 75,
                videoMaxDurationSeconds = preferences[PreferencesKeys.VIDEO_MAX_DURATION] ?: 120,
                videoAutoPlay = preferences[PreferencesKeys.VIDEO_AUTO_PLAY] ?: true,
                videoShowControls = preferences[PreferencesKeys.VIDEO_SHOW_CONTROLS] ?: false,
                videoLoopShort = preferences[PreferencesKeys.VIDEO_LOOP_SHORT] ?: true,
                videoDisplayMode = enumValueOfOrNull<VideoDisplayMode>(
                    preferences[PreferencesKeys.VIDEO_DISPLAY_MODE] ?: VideoDisplayMode.PLAY_FULL.name
                ) ?: VideoDisplayMode.PLAY_FULL,
                videoFixedPlaySeconds = preferences[PreferencesKeys.VIDEO_FIXED_SECONDS] ?: 30,
                videoStillTimestamp = enumValueOfOrNull<VideoStillTimestamp>(
                    preferences[PreferencesKeys.VIDEO_STILL_TIMESTAMP] ?: VideoStillTimestamp.BEGINNING.name
                ) ?: VideoStillTimestamp.BEGINNING,

                // Cache
                cacheConfig = CacheConfig(
                    enabled = cacheEnabled,
                    cacheSizeLimitMB = preferences[PreferencesKeys.CACHE_LIMIT] ?: 500,
                    usePresetLimit = preferences[PreferencesKeys.CACHE_USE_PRESET] ?: true,
                ),

                // Network
                wifiOnly = preferences[PreferencesKeys.WIFI_ONLY] ?: true,
                networkTimeoutSeconds = preferences[PreferencesKeys.NETWORK_TIMEOUT] ?: 30,

                // Timer
                timerConfig = TimerConfig(
                    enabled = preferences[PreferencesKeys.TIMER_ENABLED] ?: false,
                ),
            )
        }
    }

    /**
     * Save slideshow configuration
     */
    fun saveSlideshowConfig(context: Context, config: SlideshowConfig) {
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SLIDE_DURATION] = config.slideDurationSeconds
                preferences[PreferencesKeys.SHUFFLE] = config.shuffle
                preferences[PreferencesKeys.PHOTO_ORDER] = config.photoOrder.name
                preferences[PreferencesKeys.MEDIA_TYPE_FILTER] = config.mediaTypeFilter.name
                preferences[PreferencesKeys.MATCH_ORIENTATION] = config.matchDeviceOrientation
                preferences[PreferencesKeys.DISPLAY_EFFECT] = config.displayEffect.name
                preferences[PreferencesKeys.PAN_DIRECTION] = config.panDirection.name
                preferences[PreferencesKeys.TRANSITION_EFFECT] = config.transitionEffect.name
                preferences[PreferencesKeys.TRANSITION_DURATION] = config.transitionDurationMs
                preferences[PreferencesKeys.TRANSITION_EASING] = config.transitionEasing.name
                preferences[PreferencesKeys.TRANSITION_DIRECTION] = config.transitionDirection.name
                preferences[PreferencesKeys.BACKGROUND_COLOR] = config.backgroundColor
                preferences[PreferencesKeys.SCREEN_ORIENTATION] = config.screenOrientation.name
                preferences[PreferencesKeys.KEEP_SCREEN_ON] = config.keepScreenOn

                // Decorations
                preferences[PreferencesKeys.DECORATION_DATE] = config.dateDecoration != null
                preferences[PreferencesKeys.DECORATION_CLOCK] = config.clockDecoration != null
                preferences[PreferencesKeys.DECORATION_WEATHER] = config.weatherDecoration != null

                // Photo info
                preferences[PreferencesKeys.PHOTO_INFO_ENABLED] = config.photoInfoConfig.enabled
                preferences[PreferencesKeys.PHOTO_INFO_FADE] = config.photoInfoConfig.fadeOutAfterSeconds

                // Video playback
                preferences[PreferencesKeys.VIDEO_AUDIO_MODE] = config.videoAudioMode.name
                preferences[PreferencesKeys.VIDEO_CUSTOM_VOLUME] = config.videoCustomVolume
                preferences[PreferencesKeys.VIDEO_MAX_DURATION] = config.videoMaxDurationSeconds
                preferences[PreferencesKeys.VIDEO_AUTO_PLAY] = config.videoAutoPlay
                preferences[PreferencesKeys.VIDEO_SHOW_CONTROLS] = config.videoShowControls
                preferences[PreferencesKeys.VIDEO_LOOP_SHORT] = config.videoLoopShort
                preferences[PreferencesKeys.VIDEO_DISPLAY_MODE] = config.videoDisplayMode.name
                preferences[PreferencesKeys.VIDEO_FIXED_SECONDS] = config.videoFixedPlaySeconds
                preferences[PreferencesKeys.VIDEO_STILL_TIMESTAMP] = config.videoStillTimestamp.name

                // Cache
                preferences[PreferencesKeys.ENABLE_CACHE] = config.cacheConfig.enabled
                preferences[PreferencesKeys.CACHE_LIMIT] = config.cacheConfig.cacheSizeLimitMB
                preferences[PreferencesKeys.CACHE_USE_PRESET] = config.cacheConfig.usePresetLimit

                // Network
                preferences[PreferencesKeys.WIFI_ONLY] = config.wifiOnly
                preferences[PreferencesKeys.NETWORK_TIMEOUT] = config.networkTimeoutSeconds

                // Timer
                preferences[PreferencesKeys.TIMER_ENABLED] = config.timerConfig.enabled
            }
        }
    }

    /**
     * Check if a source is enabled
     */
    fun isSourceEnabled(context: Context, sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType): Boolean {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            when (sourceType) {
                com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE ->
                    preferences[PreferencesKeys.SOURCE_GOOGLE_DRIVE_ENABLED] ?: false
                com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY ->
                    preferences[PreferencesKeys.SOURCE_GALLERY_ENABLED] ?: false
                else -> false
            }
        }
    }

    /**
     * Check if any sources are configured with selected folders
     * Returns true if at least one source is enabled AND has folders selected
     */
    fun hasAnySourceConfigured(context: Context): Boolean {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            
            // Check Google Drive
            val googleDriveEnabled = preferences[PreferencesKeys.SOURCE_GOOGLE_DRIVE_ENABLED] ?: false
            if (googleDriveEnabled) {
                val googleDriveFolders = preferences[PreferencesKeys.SOURCE_GOOGLE_DRIVE_FOLDERS] ?: emptySet()
                if (googleDriveFolders.isNotEmpty()) return@runBlocking true
            }
            
            // Check Gallery
            val galleryEnabled = preferences[PreferencesKeys.SOURCE_GALLERY_ENABLED] ?: false
            if (galleryEnabled) {
                val galleryFolders = preferences[PreferencesKeys.SOURCE_GALLERY_FOLDERS] ?: emptySet()
                if (galleryFolders.isNotEmpty()) return@runBlocking true
            }
            
            false
        }
    }

    /**
     * Get selected folders for a source
     */
    fun getSelectedFolders(
        context: Context,
        sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType
    ): List<com.vincentwetzel.androidscreensaver.data.model.PhotoFolder> {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            when (sourceType) {
                com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE -> {
                    val folderIds = preferences[PreferencesKeys.SOURCE_GOOGLE_DRIVE_FOLDERS] ?: emptySet()
                    folderIds.map { id ->
                        com.vincentwetzel.androidscreensaver.data.model.PhotoFolder(
                            id = id,
                            sourceType = com.vincentwetzel.androidscreensaver.data.model.SourceType.GOOGLE_DRIVE,
                            name = id,
                            parentFolderId = null,
                            photoCount = 0
                        )
                    }
                }
                com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY -> {
                    val folderIds = preferences[PreferencesKeys.SOURCE_GALLERY_FOLDERS] ?: emptySet()
                    folderIds.map { id ->
                        com.vincentwetzel.androidscreensaver.data.model.PhotoFolder(
                            id = id,
                            sourceType = com.vincentwetzel.androidscreensaver.data.model.SourceType.GALLERY,
                            name = id,
                            parentFolderId = null,
                            photoCount = 0
                        )
                    }
                }
                else -> emptyList()
            }
        }
    }

    /**
     * Save selected folders for a source
     */
    fun setSelectedFolders(
        context: Context,
        sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType,
        folderIds: Set<String>
    ) {
        runBlocking {
            context.dataStore.edit { preferences ->
                when (sourceType) {
                    com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE ->
                        preferences[PreferencesKeys.SOURCE_GOOGLE_DRIVE_FOLDERS] = folderIds
                    com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY ->
                        preferences[PreferencesKeys.SOURCE_GALLERY_FOLDERS] = folderIds
                    else -> {}
                }
            }
        }
    }

    /**
     * Enable/disable a source
     */
    fun setSourceEnabled(context: Context, sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType, enabled: Boolean) {
        runBlocking {
            context.dataStore.edit { preferences ->
                when (sourceType) {
                    com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE ->
                        preferences[PreferencesKeys.SOURCE_GOOGLE_DRIVE_ENABLED] = enabled
                    com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY ->
                        preferences[PreferencesKeys.SOURCE_GALLERY_ENABLED] = enabled
                    else -> {}
                }
            }
        }
    }

    /**
     * Get deselected folders for a source (subfolders explicitly unchecked by user)
     */
    fun getDeselectedFolders(
        context: Context,
        sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType
    ): Set<String> {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            when (sourceType) {
                com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE ->
                    preferences[PreferencesKeys.SOURCE_GOOGLE_DRIVE_DESELECTED] ?: emptySet()
                com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY ->
                    preferences[PreferencesKeys.SOURCE_GALLERY_DESELECTED] ?: emptySet()
                else -> emptySet()
            }
        }
    }

    /**
     * Save deselected folders for a source
     */
    fun setDeselectedFolders(
        context: Context,
        sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType,
        folderIds: Set<String>
    ) {
        runBlocking {
            context.dataStore.edit { preferences ->
                when (sourceType) {
                    com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE ->
                        preferences[PreferencesKeys.SOURCE_GOOGLE_DRIVE_DESELECTED] = folderIds
                    com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY ->
                        preferences[PreferencesKeys.SOURCE_GALLERY_DESELECTED] = folderIds
                    else -> {}
                }
            }
        }
    }

    // Helper function to safely parse enums
    private inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? {
        return try {
            enumValueOf<T>(name)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}