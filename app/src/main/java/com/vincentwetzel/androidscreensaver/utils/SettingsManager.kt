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

        // Decorations
        val DECORATION_DATE = booleanPreferencesKey("decoration_date")
        val DECORATION_CLOCK = booleanPreferencesKey("decoration_clock")
        val DECORATION_WEATHER = booleanPreferencesKey("decoration_weather")

        // Sync & Network
        val SYNC_INTERVAL = stringPreferencesKey("sync_interval")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val NETWORK_TIMEOUT = intPreferencesKey("network_timeout")

        // Cache
        val CACHE_LIMIT = intPreferencesKey("cache_limit_mb")
        val ENABLE_CACHE = booleanPreferencesKey("enable_cache")

        // Sources
        val SOURCE_GOOGLE_DRIVE_ENABLED = booleanPreferencesKey("source_google_drive_enabled")
        val SOURCE_GOOGLE_DRIVE_FOLDERS = stringSetPreferencesKey("source_google_drive_folders")
    }

    /**
     * Get slideshow configuration from settings
     */
    fun getSlideshowConfig(context: Context): SlideshowConfig {
        return runBlocking {
            val preferences = context.dataStore.data.first()

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
                    preferences[PreferencesKeys.MEDIA_TYPE_FILTER] ?: MediaTypeFilter.IMAGES_AND_VIDEOS.name
                ) ?: MediaTypeFilter.IMAGES_AND_VIDEOS,
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

                // Cache
                enableCaching = preferences[PreferencesKeys.ENABLE_CACHE] ?: true,
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
                preferences[PreferencesKeys.ENABLE_CACHE] = config.enableCaching
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
                else -> false
            }
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
                    // Return empty list - will be populated from actual folder data
                    emptyList()
                }
                else -> emptyList()
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
}
