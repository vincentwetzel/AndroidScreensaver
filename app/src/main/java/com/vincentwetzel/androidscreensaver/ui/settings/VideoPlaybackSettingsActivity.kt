package com.vincentwetzel.androidscreensaver.ui.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.VideoAudioMode
import com.vincentwetzel.androidscreensaver.data.model.VideoDisplayMode
import com.vincentwetzel.androidscreensaver.data.model.VideoStillTimestamp
import com.vincentwetzel.androidscreensaver.databinding.ActivityVideoPlaybackSettingsBinding
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Video Playback Settings Activity
 */
@AndroidEntryPoint
class VideoPlaybackSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlaybackSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlaybackSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("Video Playback Settings")

        setupSpinners()
        loadCurrentSettings()
        setupListeners()
    }

    private fun setupSpinners() {
        // Max duration spinner
        val maxDurations = arrayOf("10 seconds", "30 seconds", "1 minute", "2 minutes", "5 minutes", "No limit")
        val maxDurationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, maxDurations)
        maxDurationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMaxDuration.adapter = maxDurationAdapter

        // Still timestamp spinner
        val stillTimestamps = arrayOf("Beginning", "Middle", "End", "Custom")
        val stillAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stillTimestamps)
        stillAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStillTimestamp.adapter = stillAdapter
    }

    private fun loadCurrentSettings() {
        val config = SettingsManager.getSlideshowConfig(this)

        // Audio mode
        when (config.videoAudioMode) {
            VideoAudioMode.MUTE -> binding.radioMute.isChecked = true
            VideoAudioMode.SYSTEM_VOLUME -> binding.radioSystemVolume.isChecked = true
            VideoAudioMode.CUSTOM_VOLUME -> binding.radioCustomVolume.isChecked = true
        }

        binding.sliderVolume.isEnabled = config.videoAudioMode == VideoAudioMode.CUSTOM_VOLUME
        binding.sliderVolume.value = config.videoCustomVolume.toFloat()

        // Playback toggles
        binding.switchAutoplay.isChecked = config.videoAutoPlay
        binding.switchLoopShort.isChecked = config.videoLoopShort
        binding.switchControls.isChecked = config.videoShowControls

        // Max duration
        val maxDurationIndex = when (config.videoMaxDurationSeconds) {
            10 -> 0
            30 -> 1
            60 -> 2
            120 -> 3
            300 -> 4
            else -> 5
        }
        binding.spinnerMaxDuration.setSelection(maxDurationIndex)

        // Display mode
        when (config.videoDisplayMode) {
            VideoDisplayMode.PLAY_FULL -> binding.radioPlayFull.isChecked = true
            VideoDisplayMode.PLAY_FIXED -> binding.radioPlayFixed.isChecked = true
            VideoDisplayMode.EXTRACT_STILL -> binding.radioStillFrame.isChecked = true
        }

        binding.layoutStillTimestamp.visibility =
            if (config.videoDisplayMode == VideoDisplayMode.EXTRACT_STILL) View.VISIBLE else View.GONE

        // Still timestamp
        val stillIndex = when (config.videoStillTimestamp) {
            VideoStillTimestamp.BEGINNING -> 0
            VideoStillTimestamp.MIDDLE -> 1
            VideoStillTimestamp.END -> 2
            VideoStillTimestamp.CUSTOM -> 3
        }
        binding.spinnerStillTimestamp.setSelection(stillIndex)
    }

    private fun setupListeners() {
        // Audio mode radio buttons
        binding.radioAudio.setOnCheckedChangeListener { _, checkedId ->
            binding.sliderVolume.isEnabled = checkedId == R.id.radio_custom_volume
            when (checkedId) {
                R.id.radio_mute -> binding.sliderVolume.value = 0f
                R.id.radio_custom_volume -> binding.sliderVolume.value = 75f
            }
        }

        // Display mode radio buttons
        binding.radioDisplayMode.setOnCheckedChangeListener { _, checkedId ->
            binding.layoutStillTimestamp.visibility =
                if (checkedId == R.id.radio_still_frame) View.VISIBLE else View.GONE
        }

        // Save button
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        val config = SettingsManager.getSlideshowConfig(this).copy(
            // Audio mode
            videoAudioMode = when (binding.radioAudio.checkedRadioButtonId) {
                R.id.radio_mute -> VideoAudioMode.MUTE
                R.id.radio_system_volume -> VideoAudioMode.SYSTEM_VOLUME
                R.id.radio_custom_volume -> VideoAudioMode.CUSTOM_VOLUME
                else -> VideoAudioMode.SYSTEM_VOLUME
            },
            videoCustomVolume = binding.sliderVolume.value.toInt(),

            // Playback toggles
            videoAutoPlay = binding.switchAutoplay.isChecked,
            videoLoopShort = binding.switchLoopShort.isChecked,
            videoShowControls = binding.switchControls.isChecked,

            // Max duration
            videoMaxDurationSeconds = when (binding.spinnerMaxDuration.selectedItemPosition) {
                0 -> 10
                1 -> 30
                2 -> 60
                3 -> 120
                4 -> 300
                else -> Int.MAX_VALUE
            },

            // Display mode
            videoDisplayMode = when (binding.radioDisplayMode.checkedRadioButtonId) {
                R.id.radio_play_full -> VideoDisplayMode.PLAY_FULL
                R.id.radio_play_fixed -> VideoDisplayMode.PLAY_FIXED
                R.id.radio_still_frame -> VideoDisplayMode.EXTRACT_STILL
                else -> VideoDisplayMode.PLAY_FULL
            },

            // Still timestamp
            videoStillTimestamp = when (binding.spinnerStillTimestamp.selectedItemPosition) {
                0 -> VideoStillTimestamp.BEGINNING
                1 -> VideoStillTimestamp.MIDDLE
                2 -> VideoStillTimestamp.END
                3 -> VideoStillTimestamp.CUSTOM
                else -> VideoStillTimestamp.BEGINNING
            }
        )

        SettingsManager.saveSlideshowConfig(this, config)
        Snackbar.make(binding.root, "Settings saved!", Snackbar.LENGTH_SHORT).show()
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
