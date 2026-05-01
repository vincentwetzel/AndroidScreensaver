package com.vincentwetzel.androidscreensaver.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.*
import com.vincentwetzel.androidscreensaver.ui.sources.FolderBrowserActivity
import com.vincentwetzel.androidscreensaver.ui.about.AboutActivity
import com.vincentwetzel.androidscreensaver.ui.settings.VideoPlaybackSettingsActivity
import com.vincentwetzel.androidscreensaver.ui.settings.PhotoInfoSettingsActivity
import com.vincentwetzel.androidscreensaver.ui.settings.ScheduleSettingsActivity
import com.vincentwetzel.androidscreensaver.ui.settings.DebugSettingsActivity
import com.vincentwetzel.androidscreensaver.ui.settings.DecorationSettingsActivity
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import com.vincentwetzel.androidscreensaver.utils.VersionUtils
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings Fragment
 * Contains all app preferences organized by category
 */
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    private var tapCount = 0

    // Activity result launcher for folder browser
    private val folderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Selections are auto-saved in the folder browser
        Toast.makeText(requireContext(), "Folders updated", Toast.LENGTH_SHORT).show()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)

        setupPreferences()
        syncSettingsFromDataStore()
    }

    override fun onResume() {
        super.onResume()
        // Refresh all preference summaries from DataStore when returning from sub-screens
        syncSettingsFromDataStore()
        updateNavigationPreferenceSummaries()
    }

    private fun setupPreferences() {
        // Google Drive source preference
        findPreference<Preference>("source_google_drive")?.setOnPreferenceClickListener {
            // Open folder browser
            val intent = Intent(requireContext(), FolderBrowserActivity::class.java)
            folderLauncher.launch(intent)
            true
        }

        // Video playback enabled toggle
        findPreference<SwitchPreferenceCompat>("video_playback_enabled")?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            val config = SettingsManager.getSlideshowConfig(requireContext())
            val newConfig = config.copy(videoPlaybackEnabled = isEnabled)
            SettingsManager.saveSlideshowConfig(requireContext(), newConfig)

            // Enable/disable the video settings preference
            findPreference<Preference>("video_playback")?.isEnabled = isEnabled
            true
        }

        // Video playback preference
        findPreference<Preference>("video_playback")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), VideoPlaybackSettingsActivity::class.java))
            true
        }

        // Photo info master toggle
        findPreference<SwitchPreferenceCompat>("photo_info_enabled")?.setOnPreferenceChangeListener { _, newValue ->
            val config = SettingsManager.getSlideshowConfig(requireContext())
            val newConfig = config.copy(photoInfoConfig = config.photoInfoConfig.copy(enabled = newValue as Boolean))
            SettingsManager.saveSlideshowConfig(requireContext(), newConfig)
            true
        }

        // Customize photo info
        findPreference<Preference>("photo_info_customize")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), PhotoInfoSettingsActivity::class.java))
            true
        }

        // Autostart schedule
        findPreference<Preference>("autostart")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), ScheduleSettingsActivity::class.java))
            true
        }

        // Autostop schedule
        findPreference<Preference>("autostop")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), ScheduleSettingsActivity::class.java))
            true
        }

        // Background color
        findPreference<Preference>("background_color")?.setOnPreferenceClickListener {
            showColorPickerDialog()
            true
        }

        // Cache limit
        findPreference<Preference>("cache_limit")?.setOnPreferenceClickListener {
            showCustomCacheSizeDialog()
            true
        }

        // Clear cache
        findPreference<Preference>("clear_cache")?.setOnPreferenceClickListener {
            // Clear Coil's memory and disk cache
            val imageLoader = coil.Coil.imageLoader(requireContext())
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
            Toast.makeText(requireContext(), "Image cache cleared!", Toast.LENGTH_SHORT).show()
            true
        }

        // Power management
        findPreference<Preference>("power_management")?.setOnPreferenceClickListener {
            // TODO: Open power management settings
            Toast.makeText(requireContext(), "Power management coming soon!", Toast.LENGTH_SHORT).show()
            true
        }

        // Match orientation - add persistence
        findPreference<SwitchPreferenceCompat>("match_orientation")?.setOnPreferenceChangeListener { _, newValue ->
            val config = SettingsManager.getSlideshowConfig(requireContext())
            SettingsManager.saveSlideshowConfig(
                requireContext(), config.copy(matchDeviceOrientation = newValue as Boolean)
            )
            true
        }

        // Keep screen on - add persistence
        findPreference<SwitchPreferenceCompat>("keep_screen_on")?.setOnPreferenceChangeListener { _, newValue ->
            val config = SettingsManager.getSlideshowConfig(requireContext())
            SettingsManager.saveSlideshowConfig(
                requireContext(), config.copy(keepScreenOn = newValue as Boolean)
            )
            true
        }

        // Wi-Fi only - add persistence
        findPreference<SwitchPreferenceCompat>("wifi_only")?.setOnPreferenceChangeListener { _, newValue ->
            val config = SettingsManager.getSlideshowConfig(requireContext())
            SettingsManager.saveSlideshowConfig(
                requireContext(), config.copy(wifiOnly = newValue as Boolean)
            )
            true
        }

        // Screensaver timeout - add persistence and show custom dialog for CUSTOM option
        setupScreensaverTimeoutPreference()

        // Decoration date - add persistence
        findPreference<SwitchPreferenceCompat>("decoration_date")?.setOnPreferenceChangeListener { _, newValue ->
            val config = SettingsManager.getSlideshowConfig(requireContext())
            val newConfig = if (newValue as Boolean) {
                config.copy(dateDecoration = com.vincentwetzel.androidscreensaver.data.model.DecorationConfig())
            } else {
                config.copy(dateDecoration = null)
            }
            SettingsManager.saveSlideshowConfig(requireContext(), newConfig)
            true
        }

        // Decoration clock - add persistence
        findPreference<SwitchPreferenceCompat>("decoration_clock")?.setOnPreferenceChangeListener { _, newValue ->
            val config = SettingsManager.getSlideshowConfig(requireContext())
            val newConfig = if (newValue as Boolean) {
                config.copy(clockDecoration = com.vincentwetzel.androidscreensaver.data.model.ClockDecorationConfig())
            } else {
                config.copy(clockDecoration = null)
            }
            SettingsManager.saveSlideshowConfig(requireContext(), newConfig)
            true
        }

        // Decoration weather - add persistence
        findPreference<SwitchPreferenceCompat>("decoration_weather")?.setOnPreferenceChangeListener { _, newValue ->
            val config = SettingsManager.getSlideshowConfig(requireContext())
            val newConfig = if (newValue as Boolean) {
                config.copy(weatherDecoration = com.vincentwetzel.androidscreensaver.data.model.WeatherDecorationConfig())
            } else {
                config.copy(weatherDecoration = null)
            }
            SettingsManager.saveSlideshowConfig(requireContext(), newConfig)
            true
        }

        // Customize decorations
        findPreference<Preference>("decoration_customize")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), DecorationSettingsActivity::class.java))
            true
        }

        // About
        findPreference<Preference>("about")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
            true
        }

        // Version - tap 7 times for debug mode
        findPreference<Preference>("version")?.setOnPreferenceClickListener {
            tapCount++
            if (tapCount >= 7) {
                // Open debug settings
                startActivity(Intent(requireContext(), DebugSettingsActivity::class.java))
                tapCount = 0
            } else if (tapCount > 3) {
                Toast.makeText(requireContext(), "${7 - tapCount} more taps to enable debug mode", Toast.LENGTH_SHORT).show()
            }
            true
        }

        // Set up summary updates for list preferences (with DataStore sync)
        setupListPreferenceWithSave("display_effect") { _, config, value ->
            config.copy(displayEffect = enumValueOfOrNull<com.vincentwetzel.androidscreensaver.data.model.DisplayEffect>(value))
        }
        setupListPreferenceWithSave("transition_effect") { _, config, value ->
            config.copy(transitionEffect = enumValueOfOrNull<com.vincentwetzel.androidscreensaver.data.model.TransitionEffect>(value))
        }
        setupListPreferenceWithSave("transition_duration") { _, config, value ->
            config.copy(transitionDurationMs = value.toIntOrNull() ?: 1000)
        }
        setupListPreferenceWithSave("screen_rotation") { _, config, value ->
            val orientation = when (value) {
                "portrait" -> com.vincentwetzel.androidscreensaver.data.model.ScreenOrientation.PORTRAIT
                "landscape" -> com.vincentwetzel.androidscreensaver.data.model.ScreenOrientation.LANDSCAPE
                else -> com.vincentwetzel.androidscreensaver.data.model.ScreenOrientation.SYSTEM_DEFAULT
            }
            config.copy(screenOrientation = orientation)
        }
        setupListPreferenceWithSave("sync_interval") { _, config, value ->
            // Sync interval XML values are minute numbers or "auto"/"custom"/"manual"
            // Map to SyncInterval enum: HOURLY, DAILY, WEEKLY, NEVER
            val interval = when {
                value == "manual" -> com.vincentwetzel.androidscreensaver.data.model.SyncInterval.NEVER
                value == "auto" -> config.syncInterval // Keep current
                value == "custom" -> config.syncInterval // Keep current
                else -> {
                    val minutes = value.toIntOrNull() ?: 60
                    when {
                        minutes <= 30 -> com.vincentwetzel.androidscreensaver.data.model.SyncInterval.HOURLY
                        minutes <= 90 -> com.vincentwetzel.androidscreensaver.data.model.SyncInterval.DAILY
                        else -> com.vincentwetzel.androidscreensaver.data.model.SyncInterval.WEEKLY
                    }
                }
            }
            config.copy(syncInterval = interval)
        }
        setupListPreferenceWithSave("network_timeout") { _, config, value ->
            config.copy(networkTimeoutSeconds = value.toIntOrNull() ?: 30)
        }
        setupListPreferenceWithSave("exit_trigger") { _, config, value ->
            val trigger = when (value) {
                "touch" -> com.vincentwetzel.androidscreensaver.data.model.ScreensaverExitTrigger.TOUCH
                "remote" -> com.vincentwetzel.androidscreensaver.data.model.ScreensaverExitTrigger.REMOTE_BUTTON
                "shake" -> com.vincentwetzel.androidscreensaver.data.model.ScreensaverExitTrigger.SHAKE
                "voice" -> com.vincentwetzel.androidscreensaver.data.model.ScreensaverExitTrigger.VOICE_COMMAND
                else -> com.vincentwetzel.androidscreensaver.data.model.ScreensaverExitTrigger.TOUCH
            }
            config.copy(exitOnTrigger = trigger)
        }

        // Display time: update summary AND save to DataStore
        setupDisplayTimePreference()

        // Content filter: update summary AND save to DataStore
        findPreference<ListPreference>("content_filter")?.setOnPreferenceChangeListener { preference, newValue ->
            val listPref = preference as ListPreference
            val index = listPref.findIndexOfValue(newValue.toString())
            if (index >= 0) preference.summary = listPref.entries[index]

            val filter = when (newValue.toString()) {
                "images" -> com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY
                "videos" -> com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY
                else -> com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_AND_VIDEOS
            }
            val config = SettingsManager.getSlideshowConfig(requireContext())
            SettingsManager.saveSlideshowConfig(
                requireContext(), config.copy(mediaTypeFilter = filter)
            )
            true
        }

        // Media order: update summary AND save to DataStore
        findPreference<ListPreference>("media_order")?.setOnPreferenceChangeListener { preference, newValue ->
            val listPref = preference as ListPreference
            val index = listPref.findIndexOfValue(newValue.toString())
            if (index >= 0) preference.summary = listPref.entries[index]

            val order = when (newValue.toString()) {
                "name_asc" -> com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.NAME_A_Z
                "name_desc" -> com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.NAME_Z_A
                "date_asc" -> com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.DATE_OLDEST_FIRST
                "date_desc" -> com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.DATE_NEWEST_FIRST
                else -> com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.DATE_NEWEST_FIRST // shuffle
            }
            val config = SettingsManager.getSlideshowConfig(requireContext())
            SettingsManager.saveSlideshowConfig(
                requireContext(),
                config.copy(shuffle = newValue.toString() == "shuffle", photoOrder = order)
            )
            true
        }
    }

    /**
     * Automatically update the summary when a ListPreference value changes
     */
    private fun setupListPreferenceSummary(key: String) {
        findPreference<ListPreference>(key)?.setOnPreferenceChangeListener { preference, newValue ->
            val listPref = preference as ListPreference
            val index = listPref.findIndexOfValue(newValue.toString())
            preference.summary = if (index >= 0) {
                listPref.entries[index]
            } else {
                ""
            }
            true
        }
    }

    /**
     * Update summary AND save to DataStore when a ListPreference changes
     */
    private fun setupListPreferenceWithSave(
        key: String,
        transform: (Preference, com.vincentwetzel.androidscreensaver.data.model.SlideshowConfig, String) -> com.vincentwetzel.androidscreensaver.data.model.SlideshowConfig
    ) {
        findPreference<ListPreference>(key)?.setOnPreferenceChangeListener { preference, newValue ->
            val listPref = preference as ListPreference
            val value = newValue.toString()
            val index = listPref.findIndexOfValue(value)
            if (index >= 0) preference.summary = listPref.entries[index]

            val config = SettingsManager.getSlideshowConfig(requireContext())
            val newConfig = transform(preference, config, value)
            SettingsManager.saveSlideshowConfig(requireContext(), newConfig)
            true
        }
    }

    /**
     * Safely convert a string to an enum value, returning the default if not found
     */
    private inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T {
        return try {
            enumValueOf<T>(name.uppercase())
        } catch (e: IllegalArgumentException) {
            // Return the first enum constant as fallback
            enumValues<T>().first()
        }
    }

    /**
     * Display time preference: updates summary AND saves to DataStore
     */
    private fun setupDisplayTimePreference() {
        findPreference<ListPreference>("display_time")?.setOnPreferenceChangeListener { preference, newValue ->
            val listPref = preference as ListPreference
            val index = listPref.findIndexOfValue(newValue.toString())
            // Update summary text
            if (index >= 0) {
                preference.summary = listPref.entries[index]
            }
            // Save to DataStore so slideshow picks it up
            val seconds = newValue.toString().toIntOrNull() ?: 5
            val config = SettingsManager.getSlideshowConfig(requireContext())
            SettingsManager.saveSlideshowConfig(
                requireContext(),
                config.copy(slideDurationSeconds = seconds)
            )
            true
        }
    }

    /**
     * Screensaver timeout preference: updates summary AND saves to DataStore
     * Shows custom dialog when CUSTOM option is selected
     */
    private fun setupScreensaverTimeoutPreference() {
        findPreference<ListPreference>("screensaver_timeout")?.setOnPreferenceChangeListener { preference, newValue ->
            val listPref = preference as ListPreference
            val value = newValue.toString()
            val index = listPref.findIndexOfValue(value)
            
            // Update summary text
            if (index >= 0) {
                preference.summary = listPref.entries[index]
            }

            // If CUSTOM is selected, show dialog to enter custom value
            if (value == "CUSTOM") {
                showCustomTimeoutDialog { timeoutValue, timeoutUnit ->
                    // Save the custom timeout after user enters it
                    val config = SettingsManager.getSlideshowConfig(requireContext())
                    val newConfig = config.copy(
                        timerConfig = config.timerConfig.copy(
                            timeoutMinutes = TimeoutMinutes.CUSTOM,
                            customTimeoutValue = timeoutValue,
                            customTimeoutUnit = timeoutUnit
                        )
                    )
                    SettingsManager.saveSlideshowConfig(requireContext(), newConfig)
                    
                    // Update summary to show custom value
                    val unitText = if (timeoutUnit == TimeoutUnit.MINUTES) "minutes" else "hours"
                    preference.summary = "Custom: $timeoutValue $unitText"
                }
                // Return false to prevent saving CUSTOM value immediately
                false
            } else {
                // Save the preset value to DataStore
                val timeoutMinutes = try {
                    enumValueOf<TimeoutMinutes>(value)
                } catch (e: IllegalArgumentException) {
                    TimeoutMinutes.MINUTES_30
                }
                
                val config = SettingsManager.getSlideshowConfig(requireContext())
                val newConfig = config.copy(
                    timerConfig = config.timerConfig.copy(
                        timeoutMinutes = timeoutMinutes
                    )
                )
                SettingsManager.saveSlideshowConfig(requireContext(), newConfig)
                true
            }
        }
    }

    /**
     * Read current config from DataStore and sync summaries with actual saved values
     */
    private fun syncSettingsFromDataStore() {
        val config = SettingsManager.getSlideshowConfig(requireContext())

        // Video playback enabled
        findPreference<SwitchPreferenceCompat>("video_playback_enabled")?.isChecked = config.videoPlaybackEnabled
        findPreference<Preference>("video_playback")?.isEnabled = config.videoPlaybackEnabled

        // Display time
        findPreference<ListPreference>("display_time")?.let { pref ->
            val value = config.slideDurationSeconds.toString()
            pref.value = value
            pref.summary = config.slideDurationSeconds.let { s ->
                when (s) {
                    3 -> "3 seconds"
                    5 -> "5 seconds"
                    10 -> "10 seconds"
                    15 -> "15 seconds"
                    30 -> "30 seconds"
                    60 -> "1 minute"
                    120 -> "2 minutes"
                    300 -> "5 minutes"
                    else -> "$s seconds"
                }
            }
        }

        // Media order
        findPreference<ListPreference>("media_order")?.let { pref ->
            val value = if (config.shuffle) {
                "shuffle"
            } else {
                when (config.photoOrder) {
                    com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.NAME_A_Z -> "name_asc"
                    com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.NAME_Z_A -> "name_desc"
                    com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.DATE_OLDEST_FIRST -> "date_asc"
                    com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.DATE_NEWEST_FIRST -> "date_desc"
                    else -> "shuffle"
                }
            }
            pref.value = value
            pref.summary = pref.entry
        }

        // Content filter
        findPreference<ListPreference>("content_filter")?.let { pref ->
            val value = when (config.mediaTypeFilter) {
                com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY -> "images"
                com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY -> "videos"
                else -> "both"
            }
            pref.value = value
            pref.summary = pref.entry
        }

        // Match orientation - init state and persistence handled in setupPreferences

        // Display effect
        findPreference<ListPreference>("display_effect")?.let { pref ->
            val value = config.displayEffect.name.lowercase()
            pref.value = value
            pref.summary = pref.entry
        }

        // Transition effect
        findPreference<ListPreference>("transition_effect")?.let { pref ->
            val value = config.transitionEffect.name.lowercase()
            pref.value = value
            pref.summary = pref.entry
        }

        // Transition duration
        findPreference<ListPreference>("transition_duration")?.let { pref ->
            val value = config.transitionDurationMs.toString()
            pref.value = value
            pref.summary = pref.entry
        }

        // Screen rotation
        findPreference<ListPreference>("screen_rotation")?.let { pref ->
            val value = when (config.screenOrientation) {
                com.vincentwetzel.androidscreensaver.data.model.ScreenOrientation.PORTRAIT -> "portrait"
                com.vincentwetzel.androidscreensaver.data.model.ScreenOrientation.LANDSCAPE -> "landscape"
                else -> "system"
            }
            pref.value = value
            pref.summary = pref.entry
        }

        // Keep screen on - init state
        findPreference<SwitchPreferenceCompat>("keep_screen_on")?.isChecked = config.keepScreenOn

        // Sync interval
        findPreference<ListPreference>("sync_interval")?.let { pref ->
            val value = when (config.syncInterval) {
                com.vincentwetzel.androidscreensaver.data.model.SyncInterval.HOURLY -> "30"
                com.vincentwetzel.androidscreensaver.data.model.SyncInterval.DAILY -> "60"
                com.vincentwetzel.androidscreensaver.data.model.SyncInterval.WEEKLY -> "1440"
                com.vincentwetzel.androidscreensaver.data.model.SyncInterval.NEVER -> "manual"
            }
            pref.value = value
            pref.summary = pref.entry
        }

        // Wi-Fi only - init state
        findPreference<SwitchPreferenceCompat>("wifi_only")?.isChecked = config.wifiOnly

        // Screensaver timeout
        findPreference<ListPreference>("screensaver_timeout")?.let { pref ->
            val timeoutMinutes = config.timerConfig.timeoutMinutes
            if (timeoutMinutes == TimeoutMinutes.CUSTOM) {
                // Show custom value in summary
                val unitText = if (config.timerConfig.customTimeoutUnit == TimeoutUnit.MINUTES) "minutes" else "hours"
                pref.summary = "Custom: ${config.timerConfig.customTimeoutValue} $unitText"
                // Set the value to CUSTOM so it shows correctly
                pref.value = "CUSTOM"
            } else {
                pref.value = timeoutMinutes.name
                val entry = pref.entry
                pref.summary = entry ?: "Disabled"
            }
        }

        // Network timeout
        findPreference<ListPreference>("network_timeout")?.let { pref ->
            val value = config.networkTimeoutSeconds.toString()
            pref.value = value
            pref.summary = pref.entry
        }

        // Background color
        findPreference<Preference>("background_color")?.summary =
            String.format("#%06X", 0xFFFFFF and config.backgroundColor)

        // Cache limit
        val cacheLimit = if (config.cacheConfig.usePresetLimit) {
            config.cacheConfig.presetLimit.name.replace("MB_", "").replace("GB_", "000 ").let { "$it MB" }
        } else {
            "${config.cacheConfig.cacheSizeLimitMB} MB"
        }
        findPreference<Preference>("cache_limit")?.summary = cacheLimit

        // Exit trigger
        findPreference<ListPreference>("exit_trigger")?.let { pref ->
            val value = when (config.exitOnTrigger) {
                com.vincentwetzel.androidscreensaver.data.model.ScreensaverExitTrigger.TOUCH -> "touch"
                com.vincentwetzel.androidscreensaver.data.model.ScreensaverExitTrigger.REMOTE_BUTTON -> "remote"
                com.vincentwetzel.androidscreensaver.data.model.ScreensaverExitTrigger.SHAKE -> "shake"
                com.vincentwetzel.androidscreensaver.data.model.ScreensaverExitTrigger.VOICE_COMMAND -> "voice"
                else -> "touch"
            }
            pref.value = value
            pref.summary = pref.entry
        }

        // Photo info enabled - init state
        findPreference<SwitchPreferenceCompat>("photo_info_enabled")?.isChecked = config.photoInfoConfig.enabled

        // Decoration switches - init from config
        findPreference<SwitchPreferenceCompat>("decoration_date")?.isChecked = config.dateDecoration != null
        findPreference<SwitchPreferenceCompat>("decoration_clock")?.isChecked = config.clockDecoration != null
        findPreference<SwitchPreferenceCompat>("decoration_weather")?.isChecked = config.weatherDecoration != null
    }

    /**
     * Update summaries for navigation preferences that launch sub-screens
     * These show the current setting value (e.g., "Muted", "Enabled")
     */
    private fun updateNavigationPreferenceSummaries() {
        // Video playback - show audio mode
        findPreference<Preference>("video_playback")?.let { pref ->
            val config = SettingsManager.getSlideshowConfig(requireContext())
            val audioSummary = when (config.videoAudioMode) {
                com.vincentwetzel.androidscreensaver.data.model.VideoAudioMode.MUTE -> "Audio: Muted"
                com.vincentwetzel.androidscreensaver.data.model.VideoAudioMode.SYSTEM_VOLUME -> "Audio: System volume"
                com.vincentwetzel.androidscreensaver.data.model.VideoAudioMode.CUSTOM_VOLUME -> "Audio: Custom (${config.videoCustomVolume}%)"
            }
            pref.summary = audioSummary
        }

        // Autostart schedule - show enabled/disabled and time
        findPreference<Preference>("autostart")?.let { pref ->
            val config = SettingsManager.getSlideshowConfig(requireContext())
            val schedule = config.autostartSchedules.firstOrNull()
            pref.summary = if (schedule != null && schedule.enabled) {
                val timeStr = formatTime(schedule.timeHour, schedule.timeMinute)
                "Enabled at $timeStr"
            } else {
                "Disabled"
            }
        }

        // Autostop schedule - show enabled/disabled and time
        findPreference<Preference>("autostop")?.let { pref ->
            val config = SettingsManager.getSlideshowConfig(requireContext())
            val schedule = config.autostopSchedules.firstOrNull()
            pref.summary = if (schedule != null && schedule.enabled) {
                val timeStr = formatTime(schedule.timeHour, schedule.timeMinute)
                "Enabled at $timeStr"
            } else {
                "Disabled"
            }
        }

        // Customize photo info - show enabled/disabled
        findPreference<Preference>("photo_info_customize")?.let { pref ->
            val config = SettingsManager.getSlideshowConfig(requireContext())
            pref.summary = if (config.photoInfoConfig.enabled) {
                val fieldCount = listOf(
                    config.photoInfoConfig.showFileName,
                    config.photoInfoConfig.showFolderName,
                    config.photoInfoConfig.showDateTaken,
                    config.photoInfoConfig.showSourceName,
                    config.photoInfoConfig.showDescription,
                    config.photoInfoConfig.showDimensions,
                    config.photoInfoConfig.showFileSize
                ).count { it }
                "Enabled - $fieldCount fields"
            } else {
                "Disabled"
            }
        }

        // Customize decorations - show which decorations are enabled
        findPreference<Preference>("decoration_customize")?.let { pref ->
            val config = SettingsManager.getSlideshowConfig(requireContext())
            val enabledDecorations = mutableListOf<String>()
            if (config.dateDecoration != null) enabledDecorations.add("Date")
            if (config.clockDecoration != null) enabledDecorations.add("Clock")
            if (config.weatherDecoration != null) enabledDecorations.add("Weather")

            pref.summary = if (enabledDecorations.isNotEmpty()) {
                enabledDecorations.joinToString(", ")
            } else {
                "Disabled"
            }
        }
    }

    /**
     * Format hour and minute to human-readable time (e.g., "8:00 PM")
     */
    private fun formatTime(hour: Int, minute: Int): String {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
        cal.set(java.util.Calendar.MINUTE, minute)
        val format = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return format.format(cal.time)
    }

    private fun showAboutDialog() {
        val version = VersionUtils.getFormattedVersion(requireContext())
        Toast.makeText(requireContext(), "Android Screensaver\n$version", Toast.LENGTH_LONG).show()
    }

    private fun showColorPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        var selectedColor = 0xFF000000.toInt()

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Background Color")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                // Save the color
                val config = SettingsManager.getSlideshowConfig(requireContext())
                SettingsManager.saveSlideshowConfig(
                    requireContext(),
                    config.copy(backgroundColor = selectedColor)
                )
                // Update summary
                findPreference<Preference>("background_color")?.summary = String.format("#%06X", 0xFFFFFF and selectedColor)
                Toast.makeText(requireContext(), "Background color saved!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Setup color clicks
        val colorMap = mapOf(
            dialogView.findViewById<View>(R.id.color_black) to 0xFF000000.toInt(),
            dialogView.findViewById<View>(R.id.color_white) to 0xFFFFFFFF.toInt(),
            dialogView.findViewById<View>(R.id.color_red) to 0xFFF44336.toInt(),
            dialogView.findViewById<View>(R.id.color_green) to 0xFF4CAF50.toInt(),
            dialogView.findViewById<View>(R.id.color_blue) to 0xFF2196F3.toInt(),
            dialogView.findViewById<View>(R.id.color_orange) to 0xFFFF9800.toInt(),
            dialogView.findViewById<View>(R.id.color_purple) to 0xFF9C27B0.toInt(),
            dialogView.findViewById<View>(R.id.color_cyan) to 0xFF00BCD4.toInt(),
            dialogView.findViewById<View>(R.id.color_teal) to 0xFF009688.toInt(),
            dialogView.findViewById<View>(R.id.color_pink) to 0xFFE91E63.toInt(),
            dialogView.findViewById<View>(R.id.color_amber) to 0xFFFFC107.toInt(),
            dialogView.findViewById<View>(R.id.color_indigo) to 0xFF3F51B5.toInt(),
            dialogView.findViewById<View>(R.id.color_brown) to 0xFF795548.toInt(),
            dialogView.findViewById<View>(R.id.color_grey) to 0xFF9E9E9E.toInt()
        )

        val previewColor = dialogView.findViewById<View>(R.id.preview_color)
        val tvColorHex = dialogView.findViewById<TextView>(R.id.tv_color_hex)

        colorMap.forEach { (view: View?, color: Int) ->
            view?.setOnClickListener {
                selectedColor = color
                previewColor?.setBackgroundColor(color)
                tvColorHex?.text = String.format("#%06X", 0xFFFFFF and color)
            }
        }

        dialog.show()
    }

    private fun showCustomCacheSizeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_cache_size, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.et_cache_size)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Custom Cache Size")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val input = editText?.text?.toString()?.toIntOrNull()
                if (input != null && input in 10..10000) {
                    val config = SettingsManager.getSlideshowConfig(requireContext())
                    SettingsManager.saveSlideshowConfig(
                        requireContext(),
                        config.copy(cacheConfig = config.cacheConfig.copy(
                            cacheSizeLimitMB = input,
                            usePresetLimit = false
                        ))
                    )
                    findPreference<Preference>("cache_limit")?.summary = "$input MB"
                    Toast.makeText(requireContext(), "Cache size set to $input MB", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Please enter a value between 10 and 10,000", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    /**
     * Show dialog to enter custom timeout duration
     */
    private fun showCustomTimeoutDialog(onSave: (value: Int, unit: TimeoutUnit) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_timeout, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.et_timeout_value)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rg_timeout_unit)
        val minutesRadio = dialogView.findViewById<RadioButton>(R.id.rb_minutes)
        val hoursRadio = dialogView.findViewById<RadioButton>(R.id.rb_hours)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Custom Auto-Exit Timeout")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val input = editText?.text?.toString()?.toIntOrNull()
                val unit = if (minutesRadio.isChecked) TimeoutUnit.MINUTES else TimeoutUnit.HOURS
                
                when (unit) {
                    TimeoutUnit.MINUTES -> {
                        if (input != null && input in 1..480) {
                            onSave(input, TimeoutUnit.MINUTES)
                            Toast.makeText(requireContext(), "Auto-exit timeout set to $input minutes", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Please enter a value between 1 and 480 minutes", Toast.LENGTH_LONG).show()
                        }
                    }
                    TimeoutUnit.HOURS -> {
                        if (input != null && input in 1..24) {
                            onSave(input, TimeoutUnit.HOURS)
                            Toast.makeText(requireContext(), "Auto-exit timeout set to $input hours", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Please enter a value between 1 and 24 hours", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }
}
