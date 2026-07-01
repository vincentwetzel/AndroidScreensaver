package com.vincentwetzel.androidscreensaver.ui.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch
import com.vincentwetzel.androidscreensaver.data.model.PhotoInfoBackground
import com.vincentwetzel.androidscreensaver.data.model.ClockPosition
import com.vincentwetzel.androidscreensaver.data.model.ShadowIntensity
import com.vincentwetzel.androidscreensaver.data.model.PhotoInfoConfig
import com.vincentwetzel.androidscreensaver.data.model.PhotoInfoLayout
import com.vincentwetzel.androidscreensaver.data.model.PhotoInfoSeparator
import com.vincentwetzel.androidscreensaver.databinding.ActivityPhotoInfoSettingsBinding
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Photo Information Settings Activity
 * Configures what metadata is displayed during slideshow
 */
@AndroidEntryPoint
class PhotoInfoSettingsActivity : AppCompatActivity() {

    private var _binding: ActivityPhotoInfoSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentConfig: PhotoInfoConfig
    private var isInitializing = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPhotoInfoSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("Photo Information")

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
        // Fade duration
        val fadeDurations = arrayOf("2 seconds", "3 seconds", "5 seconds", "8 seconds", "10 seconds", "15 seconds", "Never")
        val fadeDurationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fadeDurations)
        fadeDurationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFadeDuration.adapter = fadeDurationAdapter

        // Fade animation duration
        val animDurations = arrayOf("0.5 seconds", "1 second", "1.5 seconds", "2 seconds")
        val animDurationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, animDurations)
        animDurationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFadeAnimation.adapter = animDurationAdapter

        // Position
        val positions = arrayOf("Bottom Left", "Bottom Right", "Top Left", "Top Right", "Center")
        val positionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, positions)
        positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPosition.adapter = positionAdapter

        // Layout
        val layouts = arrayOf("Horizontal", "Vertical", "Compact")
        val layoutAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, layouts)
        layoutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLayout.adapter = layoutAdapter

        // Separator
        val separators = arrayOf("Bullet (•)", "Pipe (|)", "Dash (—)", "Slash (/)", "Comma (,)")
        val separatorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, separators)
        separatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSeparator.adapter = separatorAdapter

        // Background
        val backgrounds = arrayOf("None", "Semi-Transparent", "Solid", "Gradient Fade")
        val backgroundAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, backgrounds)
        backgroundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBackground.adapter = backgroundAdapter

        // Shadow intensity
        val shadowIntensities = arrayOf("Light", "Medium", "Heavy")
        val shadowAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, shadowIntensities)
        shadowAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerShadowIntensity.adapter = shadowAdapter
    }

    private suspend fun loadCurrentSettings() {
        val config = SettingsManager.getSlideshowConfig(this).photoInfoConfig
        currentConfig = config

        // Master toggle
        binding.switchEnabled.isChecked = config.enabled

        // Field toggles
        binding.switchFilename.isChecked = config.showFileName
        binding.switchFilenameExt.isChecked = config.showFileNameWithExtension
        binding.switchFolder.isChecked = config.showFolderName
        binding.switchFolderPath.isChecked = config.showFolderFullPath
        binding.switchDate.isChecked = config.showDateTaken
        binding.switchSource.isChecked = config.showSourceName
        binding.switchDescription.isChecked = config.showDescription
        binding.switchDimensions.isChecked = config.showDimensions
        binding.switchFilesize.isChecked = config.showFileSize
        
        // Update visibility based on loaded state
        updateFieldVisibility(config.enabled)

        // Fade duration
        val fadeIndex = when (config.fadeOutAfterSeconds) {
            2 -> 0
            3 -> 1
            5 -> 2
            8 -> 3
            10 -> 4
            15 -> 5
            else -> 6 // Never
        }
        binding.spinnerFadeDuration.setSelection(fadeIndex)

        // Fade animation duration
        val animIndex = when (config.fadeAnimationDurationMs) {
            500 -> 0
            1000 -> 1
            1500 -> 2
            2000 -> 3
            else -> 1
        }
        binding.spinnerFadeAnimation.setSelection(animIndex)

        // Position
        val positionIndex = when (config.position) {
            ClockPosition.BOTTOM_LEFT -> 0
            ClockPosition.BOTTOM_RIGHT -> 1
            ClockPosition.TOP_LEFT -> 2
            ClockPosition.TOP_RIGHT -> 3
            ClockPosition.CENTER -> 4
            else -> 0
        }
        binding.spinnerPosition.setSelection(positionIndex)

        // Layout
        val layoutIndex = when (config.layout) {
            PhotoInfoLayout.HORIZONTAL -> 0
            PhotoInfoLayout.VERTICAL -> 1
            PhotoInfoLayout.COMPACT -> 2
        }
        binding.spinnerLayout.setSelection(layoutIndex)

        // Separator
        val separatorIndex = when (config.separator) {
            PhotoInfoSeparator.BULLET -> 0
            PhotoInfoSeparator.PIPE -> 1
            PhotoInfoSeparator.DASH -> 2
            PhotoInfoSeparator.SLASH -> 3
            PhotoInfoSeparator.COMMA -> 4
        }
        binding.spinnerSeparator.setSelection(separatorIndex)

        // Background
        val backgroundIndex = when (config.background) {
            PhotoInfoBackground.NONE -> 0
            PhotoInfoBackground.SEMI_TRANSPARENT -> 1
            PhotoInfoBackground.SOLID -> 2
            PhotoInfoBackground.GRADIENT_FADE -> 3
        }
        binding.spinnerBackground.setSelection(backgroundIndex)

        // Opacity sliders
        binding.sliderBgOpacity.value = config.backgroundOpacity.toFloat()
        binding.sliderTextOpacity.value = config.textOpacity.toFloat()

        // Text shadow
        binding.switchTextShadow.isChecked = config.textShadow

        // Shadow intensity
        val shadowIndex = when (config.shadowIntensity) {
            ShadowIntensity.LIGHT -> 0
            ShadowIntensity.MEDIUM -> 1
            ShadowIntensity.HEAVY -> 2
        }
        binding.spinnerShadowIntensity.setSelection(shadowIndex)
    }

    private fun setupListeners() {
        // Master toggle
        binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            updateFieldVisibility(isChecked)
            saveCurrentSettings()
        }

        // Filename options
        binding.switchFilename.setOnCheckedChangeListener { _, isChecked ->
            binding.switchFilenameExt.visibility = if (isChecked) View.VISIBLE else View.GONE
            saveCurrentSettings()
        }

        binding.switchFilenameExt.setOnCheckedChangeListener { _, _ ->
            saveCurrentSettings()
        }

        // Folder options
        binding.switchFolder.setOnCheckedChangeListener { _, isChecked ->
            binding.switchFolderPath.visibility = if (isChecked) View.VISIBLE else View.GONE
            saveCurrentSettings()
        }

        binding.switchFolderPath.setOnCheckedChangeListener { _, _ ->
            saveCurrentSettings()
        }

        // Other field toggles — auto-save
        binding.switchDate.setOnCheckedChangeListener { _, _ ->
            saveCurrentSettings()
        }

        binding.switchSource.setOnCheckedChangeListener { _, _ ->
            saveCurrentSettings()
        }

        binding.switchDescription.setOnCheckedChangeListener { _, _ ->
            saveCurrentSettings()
        }

        binding.switchDimensions.setOnCheckedChangeListener { _, _ ->
            saveCurrentSettings()
        }

        binding.switchFilesize.setOnCheckedChangeListener { _, _ ->
            saveCurrentSettings()
        }

        // Fade duration spinner — auto-save
        binding.spinnerFadeDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveCurrentSettings()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Fade animation spinner — auto-save
        binding.spinnerFadeAnimation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveCurrentSettings()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Position spinner — auto-save
        binding.spinnerPosition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveCurrentSettings()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Layout spinner — auto-save
        binding.spinnerLayout.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveCurrentSettings()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Separator spinner — auto-save
        binding.spinnerSeparator.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveCurrentSettings()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Background spinner — auto-save
        binding.spinnerBackground.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveCurrentSettings()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Opacity sliders — auto-save
        val opacityTouchListener = object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                saveCurrentSettings()
            }
        }
        
        binding.sliderBgOpacity.addOnSliderTouchListener(opacityTouchListener)
        binding.sliderTextOpacity.addOnSliderTouchListener(opacityTouchListener)

        // Text shadow — auto-save
        binding.switchTextShadow.setOnCheckedChangeListener { _, _ ->
            saveCurrentSettings()
        }

        // Shadow intensity spinner — auto-save
        binding.spinnerShadowIntensity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveCurrentSettings()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateFieldVisibility(enabled: Boolean) {
        val visibility = if (enabled) View.VISIBLE else View.GONE
        binding.switchFilename.visibility = visibility
        binding.switchFolder.visibility = visibility
        binding.switchDate.visibility = visibility
        binding.switchSource.visibility = visibility
        binding.switchDescription.visibility = visibility
        binding.switchDimensions.visibility = visibility
        binding.switchFilesize.visibility = visibility

        // Also hide dependent fields if master is disabled. 
        // If master is enabled, restore their visibility based on their parent toggle's state.
        binding.switchFilenameExt.visibility = if (enabled && binding.switchFilename.isChecked) View.VISIBLE else View.GONE
        binding.switchFolderPath.visibility = if (enabled && binding.switchFolder.isChecked) View.VISIBLE else View.GONE
    }

    /**
     * Read current UI values and persist to DataStore
     */
    private fun saveCurrentSettings() {
        if (isInitializing || _binding == null) return
        
        // Capture synchronous UI state before suspending to prevent data corruption
        // if the user interacts with the UI during the DataStore read.
        val currentEnabled = binding.switchEnabled.isChecked
        val currentShowFileName = binding.switchFilename.isChecked
        val currentShowFileNameExt = binding.switchFilenameExt.isChecked
        val currentShowFolderName = binding.switchFolder.isChecked
        val currentShowFolderFullPath = binding.switchFolderPath.isChecked
        val currentShowDateTaken = binding.switchDate.isChecked
        val currentShowSourceName = binding.switchSource.isChecked
        val currentShowDescription = binding.switchDescription.isChecked
        val currentShowDimensions = binding.switchDimensions.isChecked
        val currentShowFileSize = binding.switchFilesize.isChecked

        val currentFadeOutAfterSeconds = when (binding.spinnerFadeDuration.selectedItemPosition) {
            0 -> 2
            1 -> 3
            2 -> 5
            3 -> 8
            4 -> 10
            5 -> 15
            else -> Int.MAX_VALUE // Never
        }
        val currentFadeOutEnabled = binding.spinnerFadeDuration.selectedItemPosition < 6
        val currentFadeAnimationDurationMs = when (binding.spinnerFadeAnimation.selectedItemPosition) {
            0 -> 500
            1 -> 1000
            2 -> 1500
            3 -> 2000
            else -> 1000
        }

        val currentPosition = when (binding.spinnerPosition.selectedItemPosition) {
            0 -> ClockPosition.BOTTOM_LEFT
            1 -> ClockPosition.BOTTOM_RIGHT
            2 -> ClockPosition.TOP_LEFT
            3 -> ClockPosition.TOP_RIGHT
            4 -> ClockPosition.CENTER
            else -> ClockPosition.BOTTOM_LEFT
        }
        val currentLayout = when (binding.spinnerLayout.selectedItemPosition) {
            0 -> PhotoInfoLayout.HORIZONTAL
            1 -> PhotoInfoLayout.VERTICAL
            2 -> PhotoInfoLayout.COMPACT
            else -> PhotoInfoLayout.HORIZONTAL
        }
        val currentSeparator = when (binding.spinnerSeparator.selectedItemPosition) {
            0 -> PhotoInfoSeparator.BULLET
            1 -> PhotoInfoSeparator.PIPE
            2 -> PhotoInfoSeparator.DASH
            3 -> PhotoInfoSeparator.SLASH
            4 -> PhotoInfoSeparator.COMMA
            else -> PhotoInfoSeparator.BULLET
        }
        val currentBackground = when (binding.spinnerBackground.selectedItemPosition) {
            0 -> PhotoInfoBackground.NONE
            1 -> PhotoInfoBackground.SEMI_TRANSPARENT
            2 -> PhotoInfoBackground.SOLID
            3 -> PhotoInfoBackground.GRADIENT_FADE
            else -> PhotoInfoBackground.SEMI_TRANSPARENT
        }

        // Opacity sliders
        val currentBgOpacity = binding.sliderBgOpacity.value.toInt()
        val currentTextOpacity = binding.sliderTextOpacity.value.toInt()

        // Text shadow
        val currentTextShadow = binding.switchTextShadow.isChecked
        val currentShadowIntensity = when (binding.spinnerShadowIntensity.selectedItemPosition) {
            0 -> ShadowIntensity.LIGHT
            1 -> ShadowIntensity.MEDIUM
            2 -> ShadowIntensity.HEAVY
            else -> ShadowIntensity.MEDIUM
        }

        lifecycleScope.launch {
            val config = SettingsManager.getSlideshowConfig(this@PhotoInfoSettingsActivity)
            
            val newConfig = config.photoInfoConfig.copy(
                enabled = currentEnabled,
                showFileName = currentShowFileName,
                showFileNameWithExtension = currentShowFileNameExt,
                showFolderName = currentShowFolderName,
                showFolderFullPath = currentShowFolderFullPath,
                showDateTaken = currentShowDateTaken,
                showSourceName = currentShowSourceName,
                showDescription = currentShowDescription,
                showDimensions = currentShowDimensions,
                showFileSize = currentShowFileSize,
                fadeOutAfterSeconds = currentFadeOutAfterSeconds,
                fadeOutEnabled = currentFadeOutEnabled,
                fadeAnimationDurationMs = currentFadeAnimationDurationMs,
                position = currentPosition,
                layout = currentLayout,
                separator = currentSeparator,
                background = currentBackground,
                backgroundOpacity = currentBgOpacity,
                textOpacity = currentTextOpacity,
                textShadow = currentTextShadow,
                shadowIntensity = currentShadowIntensity
            )

            SettingsManager.saveSlideshowConfig(this@PhotoInfoSettingsActivity, config.copy(photoInfoConfig = newConfig))
            currentConfig = newConfig
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