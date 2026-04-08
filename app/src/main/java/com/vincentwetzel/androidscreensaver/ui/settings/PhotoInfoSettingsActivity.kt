package com.vincentwetzel.androidscreensaver.ui.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.PhotoInfoBackground
import com.vincentwetzel.androidscreensaver.data.model.PhotoInfoDateFormat
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

    private lateinit var binding: ActivityPhotoInfoSettingsBinding
    private lateinit var currentConfig: PhotoInfoConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoInfoSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("Photo Information")

        setupSpinners()
        loadCurrentSettings()
        setupListeners()
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
        val positions = arrayOf("Bottom Left", "Bottom Right", "Top Left", "Top Right", "Bottom Center", "Top Center")
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

    private fun loadCurrentSettings() {
        val config = SettingsManager.getSlideshowConfig(this).photoInfoConfig
        currentConfig = config

        // Master toggle
        binding.switchEnabled.isChecked = config.enabled
        updateFieldVisibility(config.enabled)

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
            com.vincentwetzel.androidscreensaver.data.model.ClockPosition.BOTTOM_LEFT -> 0
            com.vincentwetzel.androidscreensaver.data.model.ClockPosition.BOTTOM_RIGHT -> 1
            com.vincentwetzel.androidscreensaver.data.model.ClockPosition.TOP_LEFT -> 2
            com.vincentwetzel.androidscreensaver.data.model.ClockPosition.TOP_RIGHT -> 3
            com.vincentwetzel.androidscreensaver.data.model.ClockPosition.CENTER -> 4
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
            com.vincentwetzel.androidscreensaver.data.model.ShadowIntensity.LIGHT -> 0
            com.vincentwetzel.androidscreensaver.data.model.ShadowIntensity.MEDIUM -> 1
            com.vincentwetzel.androidscreensaver.data.model.ShadowIntensity.HEAVY -> 2
        }
        binding.spinnerShadowIntensity.setSelection(shadowIndex)
    }

    private fun setupListeners() {
        // Master toggle
        binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            updateFieldVisibility(isChecked)
        }

        // Filename options
        binding.switchFilename.setOnCheckedChangeListener { _, isChecked ->
            binding.switchFilenameExt.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Folder options
        binding.switchFolder.setOnCheckedChangeListener { _, isChecked ->
            binding.switchFolderPath.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Save button
        binding.btnSave.setOnClickListener {
            saveSettings()
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
    }

    private fun saveSettings() {
        val newConfig = currentConfig.copy(
            enabled = binding.switchEnabled.isChecked,
            showFileName = binding.switchFilename.isChecked,
            showFileNameWithExtension = binding.switchFilenameExt.isChecked,
            showFolderName = binding.switchFolder.isChecked,
            showFolderFullPath = binding.switchFolderPath.isChecked,
            showDateTaken = binding.switchDate.isChecked,
            showSourceName = binding.switchSource.isChecked,
            showDescription = binding.switchDescription.isChecked,
            showDimensions = binding.switchDimensions.isChecked,
            showFileSize = binding.switchFilesize.isChecked,

            fadeOutAfterSeconds = when (binding.spinnerFadeDuration.selectedItemPosition) {
                0 -> 2
                1 -> 3
                2 -> 5
                3 -> 8
                4 -> 10
                5 -> 15
                else -> Int.MAX_VALUE // Never
            },
            fadeOutEnabled = binding.spinnerFadeDuration.selectedItemPosition < 6,
            fadeAnimationDurationMs = when (binding.spinnerFadeAnimation.selectedItemPosition) {
                0 -> 500
                1 -> 1000
                2 -> 1500
                3 -> 2000
                else -> 1000
            },

            position = when (binding.spinnerPosition.selectedItemPosition) {
                0 -> com.vincentwetzel.androidscreensaver.data.model.ClockPosition.BOTTOM_LEFT
                1 -> com.vincentwetzel.androidscreensaver.data.model.ClockPosition.BOTTOM_RIGHT
                2 -> com.vincentwetzel.androidscreensaver.data.model.ClockPosition.TOP_LEFT
                3 -> com.vincentwetzel.androidscreensaver.data.model.ClockPosition.TOP_RIGHT
                4 -> com.vincentwetzel.androidscreensaver.data.model.ClockPosition.CENTER
                else -> com.vincentwetzel.androidscreensaver.data.model.ClockPosition.BOTTOM_LEFT
            },
            layout = when (binding.spinnerLayout.selectedItemPosition) {
                0 -> PhotoInfoLayout.HORIZONTAL
                1 -> PhotoInfoLayout.VERTICAL
                2 -> PhotoInfoLayout.COMPACT
                else -> PhotoInfoLayout.HORIZONTAL
            },
            separator = when (binding.spinnerSeparator.selectedItemPosition) {
                0 -> PhotoInfoSeparator.BULLET
                1 -> PhotoInfoSeparator.PIPE
                2 -> PhotoInfoSeparator.DASH
                3 -> PhotoInfoSeparator.SLASH
                4 -> PhotoInfoSeparator.COMMA
                else -> PhotoInfoSeparator.BULLET
            },
            background = when (binding.spinnerBackground.selectedItemPosition) {
                0 -> PhotoInfoBackground.NONE
                1 -> PhotoInfoBackground.SEMI_TRANSPARENT
                2 -> PhotoInfoBackground.SOLID
                3 -> PhotoInfoBackground.GRADIENT_FADE
                else -> PhotoInfoBackground.SEMI_TRANSPARENT
            },

            // Opacity sliders
            backgroundOpacity = binding.sliderBgOpacity.value.toInt(),
            textOpacity = binding.sliderTextOpacity.value.toInt(),

            // Text shadow
            textShadow = binding.switchTextShadow.isChecked,
            shadowIntensity = when (binding.spinnerShadowIntensity.selectedItemPosition) {
                0 -> com.vincentwetzel.androidscreensaver.data.model.ShadowIntensity.LIGHT
                1 -> com.vincentwetzel.androidscreensaver.data.model.ShadowIntensity.MEDIUM
                2 -> com.vincentwetzel.androidscreensaver.data.model.ShadowIntensity.HEAVY
                else -> com.vincentwetzel.androidscreensaver.data.model.ShadowIntensity.MEDIUM
            }
        )

        val config = SettingsManager.getSlideshowConfig(this)
        SettingsManager.saveSlideshowConfig(this, config.copy(photoInfoConfig = newConfig))

        Snackbar.make(binding.root, "Photo info settings saved!", Snackbar.LENGTH_SHORT).show()
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