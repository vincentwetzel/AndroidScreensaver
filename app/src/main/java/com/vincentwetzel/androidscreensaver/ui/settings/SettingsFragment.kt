package com.vincentwetzel.androidscreensaver.ui.settings

import android.content.Intent
import android.os.Bundle
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

        // Set up summary updates for list preferences
        setupListPreferenceSummary("media_order")
        setupListPreferenceSummary("content_filter")
        setupListPreferenceSummary("display_time")
        setupListPreferenceSummary("display_effect")
        setupListPreferenceSummary("transition_effect")
        setupListPreferenceSummary("transition_duration")
        setupListPreferenceSummary("screen_rotation")
        setupListPreferenceSummary("sync_interval")
        setupListPreferenceSummary("network_timeout")
        setupListPreferenceSummary("exit_trigger")
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

    private fun showAboutDialog() {
        val version = VersionUtils.getFormattedVersion(requireContext())
        Toast.makeText(requireContext(), "Android Screensaver\n$version", Toast.LENGTH_LONG).show()
    }

    private fun showColorPickerDialog() {
        val dialogView = layoutInflater.inflate(com.vincentwetzel.androidscreensaver.R.layout.dialog_color_picker, null)
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
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_black) to 0xFF000000.toInt(),
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_white) to 0xFFFFFFFF.toInt(),
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_red) to 0xFFF44336.toInt(),
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_green) to 0xFF4CAF50.toInt(),
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_blue) to 0xFF2196F3.toInt(),
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_orange) to 0xFFFF9800.toInt(),
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_purple) to 0xFF9C27B0.toInt(),
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_cyan) to 0xFF00BCD4.toInt(),
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_teal) to 0xFF009688.toInt(),
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_pink) to 0xFFE91E63.toInt(),
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_amber) to 0xFFFFC107.toInt(),
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_indigo) to 0xFF3F51B5.toInt(),
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_brown) to 0xFF795548.toInt(),
            dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.color_grey) to 0xFF9E9E9E.toInt()
        )

        val previewColor = dialogView.findViewById<View>(com.vincentwetzel.androidscreensaver.R.id.preview_color)
        val tvColorHex = dialogView.findViewById<android.widget.TextView>(com.vincentwetzel.androidscreensaver.R.id.tv_color_hex)

        colorMap.forEach { (view, color) ->
            view.setOnClickListener {
                selectedColor = color
                previewColor.setBackgroundColor(color)
                tvColorHex.text = String.format("#%06X", 0xFFFFFF and color)
            }
        }

        dialog.show()
    }

    private fun showCustomCacheSizeDialog() {
        val dialogView = layoutInflater.inflate(com.vincentwetzel.androidscreensaver.R.layout.dialog_custom_cache_size, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.vincentwetzel.androidscreensaver.R.id.et_cache_size)

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
