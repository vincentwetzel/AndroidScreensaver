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
        val STOP_ON_LOW_BATTERY = booleanPreferencesKey("stop_on_low_battery")
        val LOW_BATTERY_THRESHOLD = intPreferencesKey("low_battery_threshold")

        // Photo info
        val PHOTO_INFO_ENABLED = booleanPreferencesKey("photo_info_enabled")
        val PHOTO_INFO_SHOW_FILE_NAME = booleanPreferencesKey("photo_info_show_file_name")
        val PHOTO_INFO_SHOW_FILE_NAME_EXT = booleanPreferencesKey("photo_info_show_file_name_ext")
        val PHOTO_INFO_SHOW_FOLDER_NAME = booleanPreferencesKey("photo_info_show_folder_name")
        val PHOTO_INFO_SHOW_FOLDER_PATH = booleanPreferencesKey("photo_info_show_folder_path")
        val PHOTO_INFO_SHOW_DATE_TAKEN = booleanPreferencesKey("photo_info_show_date_taken")
        val PHOTO_INFO_DATE_FORMAT = stringPreferencesKey("photo_info_date_format")
        val PHOTO_INFO_SHOW_SOURCE_NAME = booleanPreferencesKey("photo_info_show_source_name")
        val PHOTO_INFO_SHOW_DESCRIPTION = booleanPreferencesKey("photo_info_show_description")
        val PHOTO_INFO_SHOW_DIMENSIONS = booleanPreferencesKey("photo_info_show_dimensions")
        val PHOTO_INFO_SHOW_FILE_SIZE = booleanPreferencesKey("photo_info_show_file_size")
        val PHOTO_INFO_FADE = intPreferencesKey("photo_info_fade_seconds")
        val PHOTO_INFO_FADE_ENABLED = booleanPreferencesKey("photo_info_fade_enabled")
        val PHOTO_INFO_FADE_ANIM_DUR = intPreferencesKey("photo_info_fade_anim_dur")
        val PHOTO_INFO_POSITION = stringPreferencesKey("photo_info_position")
        val PHOTO_INFO_LAYOUT = stringPreferencesKey("photo_info_layout")
        val PHOTO_INFO_SEPARATOR = stringPreferencesKey("photo_info_separator")
        val PHOTO_INFO_FONT_SIZE = stringPreferencesKey("photo_info_font_size")
        val PHOTO_INFO_FONT_COLOR = intPreferencesKey("photo_info_font_color")
        val PHOTO_INFO_FONT_FAMILY = stringPreferencesKey("photo_info_font_family")
        val PHOTO_INFO_TEXT_SHADOW = booleanPreferencesKey("photo_info_text_shadow")
        val PHOTO_INFO_SHADOW_INTENSITY = stringPreferencesKey("photo_info_shadow_intensity")
        val PHOTO_INFO_BACKGROUND = stringPreferencesKey("photo_info_background")
        val PHOTO_INFO_BG_OPACITY = intPreferencesKey("photo_info_bg_opacity")
        val PHOTO_INFO_TEXT_OPACITY = intPreferencesKey("photo_info_text_opacity")

        // Video playback
        val VIDEO_AUDIO_MODE = stringPreferencesKey("video_audio_mode")
        val VIDEO_CUSTOM_VOLUME = intPreferencesKey("video_custom_volume")
        val VIDEO_MAX_DURATION = intPreferencesKey("video_max_duration_seconds")
        val VIDEO_MIN_DURATION = intPreferencesKey("video_min_duration_seconds")
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
        val TIMER_TIMEOUT_MINUTES = stringPreferencesKey("timer_timeout_minutes")
        val TIMER_TIMEOUT_CUSTOM_MINUTES = intPreferencesKey("timer_timeout_custom_minutes")
        val TIMER_TIMEOUT_CUSTOM_UNIT = stringPreferencesKey("timer_timeout_custom_unit") // "minutes" or "hours"

        // Cache
        val CACHE_LIMIT = intPreferencesKey("cache_limit_mb")
        val CACHE_USE_PRESET = booleanPreferencesKey("cache_use_preset")
        val ENABLE_CACHE = booleanPreferencesKey("enable_cache")

        // Sources (multi-account - stores JSON-like serialized account configs)
        // Format: accountId|sourceType|email|enabled|folderId1,folderId2|deselectedId1,id2|authTime|syncTime|photoCount
        // Multiple accounts separated by ;;
        val SOURCE_ACCOUNTS = stringPreferencesKey("source_accounts")
    }

    /**
     * Get slideshow configuration from settings
     */
    suspend fun getSlideshowConfig(context: Context): SlideshowConfig {
        val preferences = context.dataStore.data.first()

        val cacheEnabled = preferences[PreferencesKeys.ENABLE_CACHE] ?: true

        return SlideshowConfig(
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
            stopOnLowBattery = preferences[PreferencesKeys.STOP_ON_LOW_BATTERY] ?: false,
            lowBatteryThreshold = preferences[PreferencesKeys.LOW_BATTERY_THRESHOLD] ?: 20,

            // Decorations
            dateDecoration = if (preferences[PreferencesKeys.DECORATION_DATE] == true) DecorationConfig() else null,
            clockDecoration = if (preferences[PreferencesKeys.DECORATION_CLOCK] == true) ClockDecorationConfig() else null,
            weatherDecoration = if (preferences[PreferencesKeys.DECORATION_WEATHER] == true) WeatherDecorationConfig() else null,

            // Photo info
            photoInfoConfig = PhotoInfoConfig(
                enabled = preferences[PreferencesKeys.PHOTO_INFO_ENABLED] ?: false,
                showFileName = preferences[PreferencesKeys.PHOTO_INFO_SHOW_FILE_NAME] ?: true,
                showFileNameWithExtension = preferences[PreferencesKeys.PHOTO_INFO_SHOW_FILE_NAME_EXT] ?: false,
                showFolderName = preferences[PreferencesKeys.PHOTO_INFO_SHOW_FOLDER_NAME] ?: false,
                showFolderFullPath = preferences[PreferencesKeys.PHOTO_INFO_SHOW_FOLDER_PATH] ?: false,
                showDateTaken = preferences[PreferencesKeys.PHOTO_INFO_SHOW_DATE_TAKEN] ?: true,
                dateFormat = enumValueOfOrNull<PhotoInfoDateFormat>(
                    preferences[PreferencesKeys.PHOTO_INFO_DATE_FORMAT] ?: PhotoInfoDateFormat.SHORT_DATE.name
                ) ?: PhotoInfoDateFormat.SHORT_DATE,
                showSourceName = preferences[PreferencesKeys.PHOTO_INFO_SHOW_SOURCE_NAME] ?: false,
                showDescription = preferences[PreferencesKeys.PHOTO_INFO_SHOW_DESCRIPTION] ?: false,
                showDimensions = preferences[PreferencesKeys.PHOTO_INFO_SHOW_DIMENSIONS] ?: false,
                showFileSize = preferences[PreferencesKeys.PHOTO_INFO_SHOW_FILE_SIZE] ?: false,
                fadeOutAfterSeconds = preferences[PreferencesKeys.PHOTO_INFO_FADE] ?: 5,
                fadeOutEnabled = preferences[PreferencesKeys.PHOTO_INFO_FADE_ENABLED] ?: true,
                fadeAnimationDurationMs = preferences[PreferencesKeys.PHOTO_INFO_FADE_ANIM_DUR] ?: 1000,
                position = enumValueOfOrNull<ClockPosition>(
                    preferences[PreferencesKeys.PHOTO_INFO_POSITION] ?: ClockPosition.BOTTOM_LEFT.name
                ) ?: ClockPosition.BOTTOM_LEFT,
                layout = enumValueOfOrNull<PhotoInfoLayout>(
                    preferences[PreferencesKeys.PHOTO_INFO_LAYOUT] ?: PhotoInfoLayout.HORIZONTAL.name
                ) ?: PhotoInfoLayout.HORIZONTAL,
                separator = enumValueOfOrNull<PhotoInfoSeparator>(
                    preferences[PreferencesKeys.PHOTO_INFO_SEPARATOR] ?: PhotoInfoSeparator.BULLET.name
                ) ?: PhotoInfoSeparator.BULLET,
                fontSize = enumValueOfOrNull<ClockSize>(
                    preferences[PreferencesKeys.PHOTO_INFO_FONT_SIZE] ?: ClockSize.MEDIUM.name
                ) ?: ClockSize.MEDIUM,
                fontColor = preferences[PreferencesKeys.PHOTO_INFO_FONT_COLOR] ?: 0xFFFFFFFF.toInt(),
                fontFamily = enumValueOfOrNull<DecorationFontFamily>(
                    preferences[PreferencesKeys.PHOTO_INFO_FONT_FAMILY] ?: DecorationFontFamily.SYSTEM_DEFAULT.name
                ) ?: DecorationFontFamily.SYSTEM_DEFAULT,
                textShadow = preferences[PreferencesKeys.PHOTO_INFO_TEXT_SHADOW] ?: true,
                shadowIntensity = enumValueOfOrNull<ShadowIntensity>(
                    preferences[PreferencesKeys.PHOTO_INFO_SHADOW_INTENSITY] ?: ShadowIntensity.MEDIUM.name
                ) ?: ShadowIntensity.MEDIUM,
                background = enumValueOfOrNull<PhotoInfoBackground>(
                    preferences[PreferencesKeys.PHOTO_INFO_BACKGROUND] ?: PhotoInfoBackground.SEMI_TRANSPARENT.name
                ) ?: PhotoInfoBackground.SEMI_TRANSPARENT,
                backgroundOpacity = preferences[PreferencesKeys.PHOTO_INFO_BG_OPACITY] ?: 60,
                textOpacity = preferences[PreferencesKeys.PHOTO_INFO_TEXT_OPACITY] ?: 100,
            ),

            // Video playback
            videoAudioMode = enumValueOfOrNull<VideoAudioMode>(
                preferences[PreferencesKeys.VIDEO_AUDIO_MODE] ?: VideoAudioMode.SYSTEM_VOLUME.name
            ) ?: VideoAudioMode.SYSTEM_VOLUME,
            videoCustomVolume = preferences[PreferencesKeys.VIDEO_CUSTOM_VOLUME] ?: 75,
            videoMinDurationSeconds = preferences[PreferencesKeys.VIDEO_MIN_DURATION] ?: 0,
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
                timeoutMinutes = parseTimeoutMinutes(preferences[PreferencesKeys.TIMER_TIMEOUT_MINUTES] ?: "MINUTES_30"),
                customTimeoutValue = preferences[PreferencesKeys.TIMER_TIMEOUT_CUSTOM_MINUTES] ?: 30,
                customTimeoutUnit = enumValueOfOrNull<TimeoutUnit>(
                    preferences[PreferencesKeys.TIMER_TIMEOUT_CUSTOM_UNIT] ?: TimeoutUnit.MINUTES.name
                ) ?: TimeoutUnit.MINUTES,
            ),
        )
    }

    /**
     * Save slideshow configuration
     */
    suspend fun saveSlideshowConfig(context: Context, config: SlideshowConfig) {
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
            preferences[PreferencesKeys.STOP_ON_LOW_BATTERY] = config.stopOnLowBattery
            preferences[PreferencesKeys.LOW_BATTERY_THRESHOLD] = config.lowBatteryThreshold

            // Decorations
            preferences[PreferencesKeys.DECORATION_DATE] = config.dateDecoration != null
            preferences[PreferencesKeys.DECORATION_CLOCK] = config.clockDecoration != null
            preferences[PreferencesKeys.DECORATION_WEATHER] = config.weatherDecoration != null

            // Photo info
            preferences[PreferencesKeys.PHOTO_INFO_ENABLED] = config.photoInfoConfig.enabled
            preferences[PreferencesKeys.PHOTO_INFO_SHOW_FILE_NAME] = config.photoInfoConfig.showFileName
            preferences[PreferencesKeys.PHOTO_INFO_SHOW_FILE_NAME_EXT] = config.photoInfoConfig.showFileNameWithExtension
            preferences[PreferencesKeys.PHOTO_INFO_SHOW_FOLDER_NAME] = config.photoInfoConfig.showFolderName
            preferences[PreferencesKeys.PHOTO_INFO_SHOW_FOLDER_PATH] = config.photoInfoConfig.showFolderFullPath
            preferences[PreferencesKeys.PHOTO_INFO_SHOW_DATE_TAKEN] = config.photoInfoConfig.showDateTaken
            preferences[PreferencesKeys.PHOTO_INFO_DATE_FORMAT] = config.photoInfoConfig.dateFormat.name
            preferences[PreferencesKeys.PHOTO_INFO_SHOW_SOURCE_NAME] = config.photoInfoConfig.showSourceName
            preferences[PreferencesKeys.PHOTO_INFO_SHOW_DESCRIPTION] = config.photoInfoConfig.showDescription
            preferences[PreferencesKeys.PHOTO_INFO_SHOW_DIMENSIONS] = config.photoInfoConfig.showDimensions
            preferences[PreferencesKeys.PHOTO_INFO_SHOW_FILE_SIZE] = config.photoInfoConfig.showFileSize
            preferences[PreferencesKeys.PHOTO_INFO_FADE] = config.photoInfoConfig.fadeOutAfterSeconds
            preferences[PreferencesKeys.PHOTO_INFO_FADE_ENABLED] = config.photoInfoConfig.fadeOutEnabled
            preferences[PreferencesKeys.PHOTO_INFO_FADE_ANIM_DUR] = config.photoInfoConfig.fadeAnimationDurationMs
            preferences[PreferencesKeys.PHOTO_INFO_POSITION] = config.photoInfoConfig.position.name
            preferences[PreferencesKeys.PHOTO_INFO_LAYOUT] = config.photoInfoConfig.layout.name
            preferences[PreferencesKeys.PHOTO_INFO_SEPARATOR] = config.photoInfoConfig.separator.name
            preferences[PreferencesKeys.PHOTO_INFO_FONT_SIZE] = config.photoInfoConfig.fontSize.name
            preferences[PreferencesKeys.PHOTO_INFO_FONT_COLOR] = config.photoInfoConfig.fontColor
            preferences[PreferencesKeys.PHOTO_INFO_FONT_FAMILY] = config.photoInfoConfig.fontFamily.name
            preferences[PreferencesKeys.PHOTO_INFO_TEXT_SHADOW] = config.photoInfoConfig.textShadow
            preferences[PreferencesKeys.PHOTO_INFO_SHADOW_INTENSITY] = config.photoInfoConfig.shadowIntensity.name
            preferences[PreferencesKeys.PHOTO_INFO_BACKGROUND] = config.photoInfoConfig.background.name
            preferences[PreferencesKeys.PHOTO_INFO_BG_OPACITY] = config.photoInfoConfig.backgroundOpacity
            preferences[PreferencesKeys.PHOTO_INFO_TEXT_OPACITY] = config.photoInfoConfig.textOpacity

            // Video playback
            preferences[PreferencesKeys.VIDEO_AUDIO_MODE] = config.videoAudioMode.name
            preferences[PreferencesKeys.VIDEO_CUSTOM_VOLUME] = config.videoCustomVolume
            preferences[PreferencesKeys.VIDEO_MIN_DURATION] = config.videoMinDurationSeconds
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
            preferences[PreferencesKeys.TIMER_TIMEOUT_MINUTES] = config.timerConfig.timeoutMinutes.name
            preferences[PreferencesKeys.TIMER_TIMEOUT_CUSTOM_MINUTES] = config.timerConfig.customTimeoutValue
            preferences[PreferencesKeys.TIMER_TIMEOUT_CUSTOM_UNIT] = config.timerConfig.customTimeoutUnit.name
        }
    }

    /**
     * Check if a source is enabled (checks if ANY account for the source is enabled).
     * For multi-account support, checks if at least one account is enabled.
     */
    suspend fun isSourceEnabled(context: Context, sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType): Boolean {
        val preferences = context.dataStore.data.first()
        val accountsJson = preferences[PreferencesKeys.SOURCE_ACCOUNTS] ?: ""
        if (accountsJson.isEmpty()) return false
        
        val accounts = parseAccountsJson(accountsJson)
        val matchingAccounts = accounts.filter {
            it.sourceType == sourceType.toModelSourceType()
        }
        return matchingAccounts.any { it.enabled && it.isAuthenticated }
    }

    /**
     * Check if any sources are configured with selected folders.
     * Returns true if at least one account is enabled AND has folders selected.
     */
    suspend fun hasAnySourceConfigured(context: Context): Boolean {
        val preferences = context.dataStore.data.first()
        val accountsJson = preferences[PreferencesKeys.SOURCE_ACCOUNTS] ?: ""
        if (accountsJson.isEmpty()) return false

        val accounts = parseAccountsJson(accountsJson)
        val enabledAccounts = accounts.filter { it.enabled && it.isAuthenticated }
        return enabledAccounts.any { it.selectedFolders.isNotEmpty() }
    }

    /**
     * Get selected folders for a source
     */
    suspend fun getSelectedFolders(
        context: Context,
        sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType
    ): List<com.vincentwetzel.androidscreensaver.data.model.PhotoFolder> {
        val accounts = getAccountsForSource(context, sourceType)
        val enabledAccounts = accounts.filter { it.enabled && it.isAuthenticated }
        return enabledAccounts.flatMap { account ->
            account.selectedFolders.map { sf ->
                com.vincentwetzel.androidscreensaver.data.model.PhotoFolder(
                    id = sf.folderId,
                    sourceType = account.sourceType,
                    accountId = account.accountId,
                    name = sf.folderName,
                    parentFolderId = null,
                    photoCount = 0
                )
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

    // Helper function to parse timeout minutes from string
    private fun parseTimeoutMinutes(value: String): TimeoutMinutes {
        return try {
            enumValueOf<TimeoutMinutes>(value)
        } catch (e: IllegalArgumentException) {
            TimeoutMinutes.MINUTES_30
        }
    }

    // ============================================================================
    // Multi-Account Support
    // ============================================================================

    /**
     * Convert dream.SourceType to model.SourceType
     */
    private fun com.vincentwetzel.androidscreensaver.dream.SourceType.toModelSourceType(): com.vincentwetzel.androidscreensaver.data.model.SourceType {
        return when (this) {
            com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE -> com.vincentwetzel.androidscreensaver.data.model.SourceType.GOOGLE_DRIVE
            com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY -> com.vincentwetzel.androidscreensaver.data.model.SourceType.GALLERY
            com.vincentwetzel.androidscreensaver.dream.SourceType.DROPBOX -> com.vincentwetzel.androidscreensaver.data.model.SourceType.DROPBOX
            com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_PHOTOS -> com.vincentwetzel.androidscreensaver.data.model.SourceType.GOOGLE_PHOTOS
            com.vincentwetzel.androidscreensaver.dream.SourceType.ONEDRIVE -> com.vincentwetzel.androidscreensaver.data.model.SourceType.ONEDRIVE
            com.vincentwetzel.androidscreensaver.dream.SourceType.LOCAL_NETWORK -> com.vincentwetzel.androidscreensaver.data.model.SourceType.LOCAL_NETWORK
        }
    }

    /**
     * Get all accounts for a specific source type
     */
    suspend fun getAccountsForSource(
        context: Context,
        sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType
    ): List<AccountConfig> {
        val preferences = context.dataStore.data.first()
        val accountsJson = preferences[PreferencesKeys.SOURCE_ACCOUNTS] ?: ""
        if (accountsJson.isEmpty()) return emptyList()

        val allAccounts = parseAccountsJson(accountsJson)
        return allAccounts.filter { it.sourceType == sourceType.toModelSourceType() }
    }

    /**
     * Add or update an account
     */
    suspend fun saveAccount(context: Context, account: AccountConfig) {
        context.dataStore.edit { prefs ->
            val accountsJson = prefs[PreferencesKeys.SOURCE_ACCOUNTS] ?: ""
            val allAccounts = parseAccountsJson(accountsJson).toMutableList()
            
            // Remove existing account with same ID if present
            allAccounts.removeAll { it.accountId == account.accountId }
            allAccounts.add(account)
            
            prefs[PreferencesKeys.SOURCE_ACCOUNTS] = serializeAccountsJson(allAccounts)
        }
    }

    /**
     * Remove an account by ID
     */
    suspend fun removeAccount(
        context: Context,
        sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType,
        accountId: String
    ) {
        context.dataStore.edit { prefs ->
            val accountsJson = prefs[PreferencesKeys.SOURCE_ACCOUNTS] ?: ""
            val allAccounts = parseAccountsJson(accountsJson).toMutableList()
            
            allAccounts.removeAll { 
                it.accountId == accountId && it.sourceType == sourceType.toModelSourceType() 
            }
            
            prefs[PreferencesKeys.SOURCE_ACCOUNTS] = serializeAccountsJson(allAccounts)
        }
    }

    /**
     * Get a specific account by ID
     */
    suspend fun getAccount(
        context: Context,
        sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType,
        accountId: String
    ): AccountConfig? {
        val accounts = getAccountsForSource(context, sourceType)
        return accounts.find { it.accountId == accountId }
    }

    /**
     * Update only the photo count for a specific account.
     * Performs an atomic transaction to avoid read-modify-write data loss on selected folders
     * if the user alters them in the UI while a long background prefetch is running.
     */
    suspend fun updateAccountPhotoCount(
        context: Context,
        sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType,
        accountId: String,
        photoCount: Int
    ) {
        context.dataStore.edit { prefs ->
            val accountsJson = prefs[PreferencesKeys.SOURCE_ACCOUNTS] ?: ""
            val allAccounts = parseAccountsJson(accountsJson).toMutableList()
            
            val index = allAccounts.indexOfFirst { it.accountId == accountId && it.sourceType == sourceType.toModelSourceType() }
            if (index != -1) {
                allAccounts[index] = allAccounts[index].copy(photoCount = photoCount)
                prefs[PreferencesKeys.SOURCE_ACCOUNTS] = serializeAccountsJson(allAccounts)
            }
        }
    }

    /**
     * Parse accounts from JSON string
     * Simple format: accountId|sourceType|email|enabled|folderId1,folderId2|deselectedId1,id2|isAuth|authTime|syncTime|photoCount
     * Multiple accounts separated by ;;
     */
    private fun parseAccountsJson(json: String): List<AccountConfig> {
        if (json.isEmpty()) return emptyList()
        
        return json.split(";;").filter { it.isNotEmpty() }.mapNotNull { accountStr ->
            try {
                val parts = accountStr.split("|")
                if (parts.size < 4) return@mapNotNull null
                
                val accountId = java.net.URLDecoder.decode(parts[0], "UTF-8")
                val sourceType = try {
                    com.vincentwetzel.androidscreensaver.data.model.SourceType.valueOf(parts[1])
                } catch (e: IllegalArgumentException) {
                    return@mapNotNull null
                }
                val email = java.net.URLDecoder.decode(parts[2], "UTF-8")
                val enabled = parts[3].toBoolean()
                
                val selectedFolderIds = if (parts.size > 4 && parts[4].isNotEmpty()) {
                    parts[4].split(",").mapNotNull { folderStr ->
                        val folderParts = folderStr.split("^")
                        if (folderParts.size == 3) {
                            SelectedFolder(
                                folderId = java.net.URLDecoder.decode(folderParts[0], "UTF-8"),
                                folderName = java.net.URLDecoder.decode(folderParts[1], "UTF-8"),
                                path = java.net.URLDecoder.decode(folderParts[2], "UTF-8"),
                                isSelected = true
                            )
                        } else if (folderParts.isNotEmpty()) {
                            val decodedId = java.net.URLDecoder.decode(folderParts[0], "UTF-8")
                            SelectedFolder(folderId = decodedId, folderName = decodedId, path = decodedId, isSelected = true)
                        } else null
                    }
                } else emptyList()
                
                val deselectedIds = if (parts.size > 5 && parts[5].isNotEmpty()) {
                    parts[5].split(",").map { java.net.URLDecoder.decode(it, "UTF-8") }.toSet()
                } else emptySet()
                
                // Local sources do not have auth flows; guarantee they are always authenticated
                val isAuth = if (sourceType == com.vincentwetzel.androidscreensaver.data.model.SourceType.GALLERY || 
                                 sourceType == com.vincentwetzel.androidscreensaver.data.model.SourceType.LOCAL_NETWORK) {
                    true
                } else if (parts.size > 6) parts[6].toBoolean() else false

                val authTime = if (parts.size > 7 && parts[7].isNotEmpty()) parts[7].toLongOrNull() else null
                val syncTime = if (parts.size > 8 && parts[8].isNotEmpty()) parts[8].toLongOrNull() else null
                val photoCount = if (parts.size > 9) parts[9].toIntOrNull() ?: 0 else 0
                
                AccountConfig(
                    accountId = accountId,
                    sourceType = sourceType,
                    accountEmail = email,
                    enabled = enabled,
                    selectedFolders = selectedFolderIds,
                    deselectedFolders = deselectedIds,
                    isAuthenticated = isAuth,
                    lastAuthTime = authTime,
                    lastSyncTime = syncTime,
                    photoCount = photoCount
                )
            } catch (e: Exception) {
                android.util.Log.e("SettingsManager", "Failed to parse individual account JSON")
                null
            }
        }
    }

    /**
     * Serialize accounts to JSON string
     */
    private fun serializeAccountsJson(accounts: List<AccountConfig>): String {
        return accounts.joinToString(";;") { account ->
            val folderIds = account.selectedFolders.joinToString(",") { 
                val eId = java.net.URLEncoder.encode(it.folderId, "UTF-8")
                val eName = java.net.URLEncoder.encode(it.folderName, "UTF-8")
                val ePath = java.net.URLEncoder.encode(it.path, "UTF-8")
                "$eId^$eName^$ePath"
            }
            val deselectedIds = account.deselectedFolders.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") }
            val email = java.net.URLEncoder.encode(account.accountEmail, "UTF-8")
            val encodedAccountId = java.net.URLEncoder.encode(account.accountId, "UTF-8")
            
            "$encodedAccountId|${account.sourceType.name}|$email|${account.enabled}|$folderIds|$deselectedIds|${account.isAuthenticated}|${account.lastAuthTime ?: ""}|${account.lastSyncTime ?: ""}|${account.photoCount}"
        }
    }
}