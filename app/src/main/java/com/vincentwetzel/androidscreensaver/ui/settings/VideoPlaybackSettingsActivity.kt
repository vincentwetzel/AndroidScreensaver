package com.vincentwetzel.androidscreensaver.ui.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.slider.Slider
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.VideoAudioMode
import com.vincentwetzel.androidscreensaver.data.model.VideoDisplayMode
import com.vincentwetzel.androidscreensaver.data.model.VideoStillTimestamp
import com.vincentwetzel.androidscreensaver.data.model.VideoPlaybackSpeed
import com.vincentwetzel.androidscreensaver.databinding.ActivityVideoPlaybackSettingsBinding
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Video Playback Settings Activity
 */
@AndroidEntryPoint
class VideoPlaybackSettingsActivity : AppCompatActivity() {

    private var _binding: ActivityVideoPlaybackSettingsBinding? = null
    private val binding get() = _binding!!
    private var isInitializing = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityVideoPlaybackSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("Video Playback Settings")

        setupSpinners()
        lifecycleScope.launch {
            loadCurrentSettings()
            setupListeners()
            binding.root.post { isInitializing = false }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setupSpinners() {
        // Min duration spinner
        val minDurations = arrayOf("No minimum", "5 seconds", "10 seconds", "15 seconds", "30 seconds", "1 minute")
        val minDurationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, minDurations)
        minDurationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMinDuration.adapter = minDurationAdapter

        // Max duration spinner
        val maxDurations = arrayOf("10 seconds", "30 seconds", "1 minute", "2 minutes", "5 minutes", "No limit")
        val maxDurationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, maxDurations)
        maxDurationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMaxDuration.adapter = maxDurationAdapter

        // Playback speed spinner
        val playbackSpeeds = arrayOf("0.5x", "Normal", "1.5x", "2x")
        val playbackSpeedAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, playbackSpeeds)
        playbackSpeedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPlaybackSpeed.adapter = playbackSpeedAdapter
    }

    private suspend fun loadCurrentSettings() {
        val config = SettingsManager.getSlideshowConfig(this)

        // Audio mode
        when (config.videoAudioMode) {
            VideoAudioMode.MUTE -> binding.radioMute.isChecked = true
            VideoAudioMode.SYSTEM_VOLUME -> binding.radioSystemVolume.isChecked = true
            VideoAudioMode.CUSTOM_VOLUME -> binding.radioCustomVolume.isChecked = true
        }

        binding.sliderVolume.isEnabled = config.videoAudioMode == VideoAudioMode.CUSTOM_VOLUME
        binding.sliderVolume.value = config.videoCustomVolume.toFloat()

        // Playback controls toggle
        binding.switchControls.isChecked = config.videoShowControls

        // Min duration
        val minDurationIndex = when (config.videoMinDurationSeconds) {
            0 -> 0
            5 -> 1
            10 -> 2
            15 -> 3
            30 -> 4
            60 -> 5
            else -> 0
        }
        binding.spinnerMinDuration.setSelection(minDurationIndex)

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

        // Playback speed
        val playbackSpeedIndex = when (config.videoPlaybackSpeed) {
            VideoPlaybackSpeed.SLOW_0_5X -> 0
            VideoPlaybackSpeed.NORMAL -> 1
            VideoPlaybackSpeed.FAST_1_5X -> 2
            VideoPlaybackSpeed.FAST_2X -> 3
        }
        binding.spinnerPlaybackSpeed.setSelection(playbackSpeedIndex)
    }

    private fun setupListeners() {
        // Audio mode radio buttons — auto-save
        binding.radioAudio.setOnCheckedChangeListener { _, checkedId ->
            binding.sliderVolume.isEnabled = checkedId == R.id.radio_custom_volume
            // Save after slider value is set - the slider's listener will also fire
            // but that's fine since both will save the same value
            saveCurrentSettings()
        }

        // Volume slider — auto-save (use value change end to avoid excessive saves)
        binding.sliderVolume.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                saveCurrentSettings()
            }
        })

        // Playback controls toggle — auto-save
        binding.switchControls.setOnCheckedChangeListener { _, _ ->
            saveCurrentSettings()
        }

        // Min duration spinner — auto-save
        binding.spinnerMinDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveCurrentSettings()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Max duration spinner — auto-save
        binding.spinnerMaxDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveCurrentSettings()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Playback speed spinner — auto-save
        binding.spinnerPlaybackSpeed.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveCurrentSettings()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * Read current UI values and persist to DataStore
     */
    private fun saveCurrentSettings() {
        if (isInitializing || _binding == null) return
        
        val videoAudioMode = when (binding.radioAudio.checkedRadioButtonId) {
            R.id.radio_mute -> VideoAudioMode.MUTE
            R.id.radio_system_volume -> VideoAudioMode.SYSTEM_VOLUME
            R.id.radio_custom_volume -> VideoAudioMode.CUSTOM_VOLUME
            else -> VideoAudioMode.SYSTEM_VOLUME
        }
        val videoCustomVolume = binding.sliderVolume.value.toInt()
        val videoShowControls = binding.switchControls.isChecked
        val videoMinDurationSeconds = when (binding.spinnerMinDuration.selectedItemPosition) {
            0 -> 0
            1 -> 5
            2 -> 10
            3 -> 15
            4 -> 30
            5 -> 60
            else -> 0
        }
        val videoMaxDurationSeconds = when (binding.spinnerMaxDuration.selectedItemPosition) {
            0 -> 10
            1 -> 30
            2 -> 60
            3 -> 120
            4 -> 300
            else -> Int.MAX_VALUE
        }
        val videoPlaybackSpeed = when (binding.spinnerPlaybackSpeed.selectedItemPosition) {
            0 -> VideoPlaybackSpeed.SLOW_0_5X
            1 -> VideoPlaybackSpeed.NORMAL
            2 -> VideoPlaybackSpeed.FAST_1_5X
            3 -> VideoPlaybackSpeed.FAST_2X
            else -> VideoPlaybackSpeed.NORMAL
        }

        lifecycleScope.launch {
            val config = SettingsManager.getSlideshowConfig(this@VideoPlaybackSettingsActivity).copy(
                videoAudioMode = videoAudioMode,
                videoCustomVolume = videoCustomVolume,
                videoAutoPlay = true, // Always autoplay
                videoLoopShort = true, // Always loop short videos
                videoShowControls = videoShowControls,
                videoMinDurationSeconds = videoMinDurationSeconds,
                videoMaxDurationSeconds = videoMaxDurationSeconds,
                videoDisplayMode = VideoDisplayMode.PLAY_FULL, // Always play full
                videoStillTimestamp = VideoStillTimestamp.BEGINNING,
                videoPlaybackSpeed = videoPlaybackSpeed
            )

            SettingsManager.saveSlideshowConfig(this@VideoPlaybackSettingsActivity, config)
        }
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
