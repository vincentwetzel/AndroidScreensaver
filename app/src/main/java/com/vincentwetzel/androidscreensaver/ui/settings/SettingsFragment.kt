package com.vincentwetzel.androidscreensaver.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.ui.sources.FolderBrowserActivity
import com.vincentwetzel.androidscreensaver.ui.about.AboutActivity
import com.vincentwetzel.androidscreensaver.ui.settings.VideoPlaybackSettingsActivity
import com.vincentwetzel.androidscreensaver.ui.settings.PhotoInfoSettingsActivity
import com.vincentwetzel.androidscreensaver.ui.settings.ScheduleSettingsActivity
import com.vincentwetzel.androidscreensaver.ui.settings.DebugSettingsActivity
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
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selectedFolders = result.data?.getStringArrayListExtra(FolderBrowserActivity.RESULT_SELECTED_FOLDERS)
            Toast.makeText(requireContext(), "${selectedFolders?.size ?: 0} folders selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)

        setupPreferences()
        syncSettingsFromDataStore()
    }

    private fun setupPreferences() {
        // Google Drive source preference
        findPreference<Preference>("source_google_drive")?.setOnPreferenceClickListener {
            // Open folder browser
            val intent = Intent(requireContext(), FolderBrowserActivity::class.java)
            folderLauncher.launch(intent)
            true
        }

        // Video playback preference
        findPreference<Preference>("video_playback")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), VideoPlaybackSettingsActivity::class.java))
            true
        }

        // Photo info master toggle
        findPreference<SwitchPreferenceCompat>("photo_info_enabled")?.setOnPreferenceChangeListener { _, newValue ->
            val config = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSlideshowConfig(requireContext())
            val newConfig = config.copy(photoInfoConfig = config.photoInfoConfig.copy(enabled = newValue as Boolean))
            com.vincentwetzel.androidscreensaver.utils.SettingsManager.saveSlideshowConfig(requireContext(), newConfig)
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
            // TODO: Clear cache with confirmation
            Toast.makeText(requireContext(), "Cache cleared!", Toast.LENGTH_SHORT).show()
            true
        }

        // Power management
        findPreference<Preference>("power_management")?.setOnPreferenceClickListener {
            // TODO: Open power management settings
            Toast.makeText(requireContext(), "Power management coming soon!", Toast.LENGTH_SHORT).show()
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
            config.copy(displayEffect = enumValueOfOrNull(value))
        }
        setupListPreferenceWithSave("transition_effect") { _, config, value ->
            config.copy(transitionEffect = enumValueOfOrNull(value))
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
            // Sync interval is stored as minutes in preferences, but as enum in DataStore
            val minutes = value.toIntOrNull()
            val interval = when {
                value == "auto" || value == "custom" || value == "manual" -> config.syncInterval
                minutes != null && minutes <= 30 -> com.vincentwetzel.androidscreensaver.data.model.SyncInterval.HOURLY
                minutes != null && minutes <= 90 -> com.vincentwetzel.androidscreensaver.data.model.SyncInterval.DAILY
                else -> com.vincentwetzel.androidscreensaver.data.model.SyncInterval.WEEKLY
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
            val config = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSlideshowConfig(requireContext())
            com.vincentwetzel.androidscreensaver.utils.SettingsManager.saveSlideshowConfig(
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
            val config = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSlideshowConfig(requireContext())
            com.vincentwetzel.androidscreensaver.utils.SettingsManager.saveSlideshowConfig(
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

            val config = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSlideshowConfig(requireContext())
            val newConfig = transform(preference, config, value)
            com.vincentwetzel.androidscreensaver.utils.SettingsManager.saveSlideshowConfig(requireContext(), newConfig)
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
            val config = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSlideshowConfig(requireContext())
            com.vincentwetzel.androidscreensaver.utils.SettingsManager.saveSlideshowConfig(
                requireContext(),
                config.copy(slideDurationSeconds = seconds)
            )
            true
        }
    }

    /**
     * Read current config from DataStore and sync summaries with actual saved values
     */
    private fun syncSettingsFromDataStore() {
        val config = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSlideshowConfig(requireContext())

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

        // Media order (enum: DATE_NEWEST_FIRST, NAME_A_Z -> pref: shuffle, name_asc)
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

        // Content filter (enum: IMAGES_ONLY -> pref: images, etc)
        findPreference<ListPreference>("content_filter")?.let { pref ->
            val value = when (config.mediaTypeFilter) {
                com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY -> "images"
                com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY -> "videos"
                else -> "both"
            }
            pref.value = value
            pref.summary = pref.entry
        }

        // Match orientation
        findPreference<SwitchPreferenceCompat>("match_orientation")?.let { pref ->
            pref.isChecked = config.matchDeviceOrientation
        }

        // Display effect (enum: CROP_TO_FIT -> pref: crop_to_fit)
        findPreference<ListPreference>("display_effect")?.let { pref ->
            val value = config.displayEffect.name.lowercase()
            pref.value = value
            pref.summary = pref.entry
        }

        // Transition effect (enum: FADE -> pref: fade)
        findPreference<ListPreference>("transition_effect")?.let { pref ->
            val value = config.transitionEffect.name.lowercase().replace("cross_fade", "cross_fade")
            pref.value = value
            pref.summary = pref.entry
        }

        // Transition duration
        findPreference<ListPreference>("transition_duration")?.let { pref ->
            val value = config.transitionDurationMs.toString()
            pref.value = value
            pref.summary = pref.entry
        }

        // Screen rotation (enum: SYSTEM_DEFAULT -> pref: system)
        findPreference<ListPreference>("screen_rotation")?.let { pref ->
            val value = config.screenOrientation.name.lowercase()
            pref.value = value
            pref.summary = pref.entry
        }

        // Keep screen on
        findPreference<SwitchPreferenceCompat>("keep_screen_on")?.let { pref ->
            pref.isChecked = config.keepScreenOn
        }

        // Sync interval
        findPreference<ListPreference>("sync_interval")?.let { pref ->
            val value = config.syncInterval.name.lowercase()
            pref.value = value
            pref.summary = pref.entry
        }

        // Wi-Fi only
        findPreference<SwitchPreferenceCompat>("wifi_only")?.let { pref ->
            pref.isChecked = config.wifiOnly
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

        // Exit trigger (enum: TOUCH -> pref: touch, REMOTE_BUTTON -> pref: remote)
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

        // Sync interval (stored as minutes, not enum)
        findPreference<ListPreference>("sync_interval")?.let { pref ->
            pref.summary = pref.entry ?: "Auto"
        }
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
                val config = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSlideshowConfig(requireContext())
                com.vincentwetzel.androidscreensaver.utils.SettingsManager.saveSlideshowConfig(
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
                    val config = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSlideshowConfig(requireContext())
                    com.vincentwetzel.androidscreensaver.utils.SettingsManager.saveSlideshowConfig(
                        requireContext(),
                        config.copy(cacheConfig = config.cacheConfig.copy(cacheSizeLimitMB = input))
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
}
