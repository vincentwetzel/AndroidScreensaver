package com.vincentwetzel.androidscreensaver.ui.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.ClockDecorationConfig
import com.vincentwetzel.androidscreensaver.data.model.ClockFormat
import com.vincentwetzel.androidscreensaver.data.model.ClockPosition
import com.vincentwetzel.androidscreensaver.data.model.ClockSize
import com.vincentwetzel.androidscreensaver.data.model.DateFormat
import com.vincentwetzel.androidscreensaver.data.model.DecorationAnimation
import com.vincentwetzel.androidscreensaver.data.model.DecorationBackground
import com.vincentwetzel.androidscreensaver.data.model.DecorationConfig
import com.vincentwetzel.androidscreensaver.data.model.PulseSpeed
import com.vincentwetzel.androidscreensaver.data.model.ShadowIntensity
import com.vincentwetzel.androidscreensaver.data.model.TemperatureUnit
import com.vincentwetzel.androidscreensaver.data.model.WeatherDecorationConfig
import com.vincentwetzel.androidscreensaver.data.model.WeatherIconStyle
import com.vincentwetzel.androidscreensaver.data.model.WeatherWidgetBackground
import com.vincentwetzel.androidscreensaver.databinding.ActivityDecorationSettingsBinding
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Decoration Settings Activity
 * Configures date, clock, and weather decoration appearance and behavior
 */
@AndroidEntryPoint
class DecorationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecorationSettingsBinding
    
    // Current decoration configs
    private lateinit var dateConfig: DecorationConfig
    private lateinit var clockConfig: ClockDecorationConfig
    private lateinit var weatherConfig: WeatherDecorationConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDecorationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("Customize Decorations")

        loadCurrentSettings()
        setupTabLayout()
        setupDateTab()
        setupClockTab()
        setupWeatherTab()
        setupSaveButton()
    }

    private fun loadCurrentSettings() {
        val config = SettingsManager.getSlideshowConfig(this)
        dateConfig = config.dateDecoration ?: DecorationConfig()
        clockConfig = config.clockDecoration ?: ClockDecorationConfig()
        weatherConfig = config.weatherDecoration ?: WeatherDecorationConfig()
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.contentDate.root.visibility = View.VISIBLE
                        binding.contentClock.root.visibility = View.GONE
                        binding.contentWeather.root.visibility = View.GONE
                    }
                    1 -> {
                        binding.contentDate.root.visibility = View.GONE
                        binding.contentClock.root.visibility = View.VISIBLE
                        binding.contentWeather.root.visibility = View.GONE
                    }
                    2 -> {
                        binding.contentDate.root.visibility = View.GONE
                        binding.contentClock.root.visibility = View.GONE
                        binding.contentWeather.root.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        // Select first tab by default
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
    }

    private fun setupDateTab() {
        // Position
        setupSpinner(binding.contentDate.spinnerPosition, arrayOf("Top Left", "Top Right", "Bottom Left", "Bottom Right", "Center"))
        binding.contentDate.spinnerPosition.setSelection(when (dateConfig.position) {
            ClockPosition.TOP_LEFT -> 0
            ClockPosition.TOP_RIGHT -> 1
            ClockPosition.BOTTOM_LEFT -> 2
            ClockPosition.BOTTOM_RIGHT -> 3
            ClockPosition.CENTER -> 4
        })

        // Date format
        val formats = arrayOf("Full Date", "Short Date", "Month Day", "Numeric Date", "Abbreviated", "Custom")
        setupSpinner(binding.contentDate.spinnerDateFormat, formats)
        binding.contentDate.spinnerDateFormat.setSelection(when (dateConfig.dateFormat) {
            DateFormat.FULL_DATE -> 0
            DateFormat.SHORT_DATE -> 1
            DateFormat.MONTH_DAY -> 2
            DateFormat.NUMERIC_DATE -> 3
            DateFormat.ABBREVIATE_MONTH -> 4
            else -> 5 // CUSTOM
        })

        // Font size
        setupSpinner(binding.contentDate.spinnerFontSize, arrayOf("Small", "Medium", "Large"))
        binding.contentDate.spinnerFontSize.setSelection(when (dateConfig.fontSize) {
            ClockSize.SMALL -> 0
            ClockSize.MEDIUM -> 1
            ClockSize.LARGE -> 2
        })

        // Background
        setupSpinner(binding.contentDate.spinnerBackground, arrayOf("None", "Semi-Transparent", "Solid"))
        binding.contentDate.spinnerBackground.setSelection(when (dateConfig.background) {
            DecorationBackground.NONE -> 0
            DecorationBackground.SEMI_TRANSPARENT -> 1
            DecorationBackground.SOLID -> 2
        })

        // Animation
        setupSpinner(binding.contentDate.spinnerAnimation, arrayOf("Static", "Pulse Softly"))
        binding.contentDate.spinnerAnimation.setSelection(if (dateConfig.animation == DecorationAnimation.PULSE_SOFTLY) 1 else 0)

        // Pulse speed (only visible if animation is pulse)
        binding.contentDate.layoutPulseSpeed.visibility = if (dateConfig.animation == DecorationAnimation.PULSE_SOFTLY) View.VISIBLE else View.GONE
        setupSpinner(binding.contentDate.spinnerPulseSpeed, arrayOf("Slow", "Medium", "Fast"))
        binding.contentDate.spinnerPulseSpeed.setSelection(when (dateConfig.pulseSpeed) {
            PulseSpeed.SLOW -> 0
            PulseSpeed.MEDIUM -> 1
            PulseSpeed.FAST -> 2
        })

        // Opacity sliders
        binding.contentDate.sliderOpacity.value = dateConfig.opacity.toFloat()
        binding.contentDate.sliderPulseMinOpacity.value = dateConfig.pulseMinOpacity.toFloat()
        binding.contentDate.sliderPulseMaxOpacity.value = dateConfig.pulseMaxOpacity.toFloat()

        // Animation change listener
        binding.contentDate.spinnerAnimation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.contentDate.layoutPulseSpeed.visibility = if (position == 1) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupClockTab() {
        // Position
        setupSpinner(binding.contentClock.spinnerPosition, arrayOf("Top Left", "Top Right", "Bottom Left", "Bottom Right", "Center"))
        binding.contentClock.spinnerPosition.setSelection(when (clockConfig.position) {
            ClockPosition.TOP_LEFT -> 0
            ClockPosition.TOP_RIGHT -> 1
            ClockPosition.BOTTOM_LEFT -> 2
            ClockPosition.BOTTOM_RIGHT -> 3
            ClockPosition.CENTER -> 4
        })

        // Clock format
        setupSpinner(binding.contentClock.spinnerFormat, arrayOf("12-Hour", "24-Hour"))
        binding.contentClock.spinnerFormat.setSelection(if (clockConfig.clockFormat == ClockFormat.HOUR_24) 1 else 0)

        // Show seconds
        binding.contentClock.switchShowSeconds.isChecked = clockConfig.showSeconds

        // Font size
        setupSpinner(binding.contentClock.spinnerFontSize, arrayOf("Small", "Medium", "Large"))
        binding.contentClock.spinnerFontSize.setSelection(when (clockConfig.fontSize) {
            ClockSize.SMALL -> 0
            ClockSize.MEDIUM -> 1
            ClockSize.LARGE -> 2
        })

        // Background
        setupSpinner(binding.contentClock.spinnerBackground, arrayOf("None", "Semi-Transparent", "Solid"))
        binding.contentClock.spinnerBackground.setSelection(when (clockConfig.background) {
            DecorationBackground.NONE -> 0
            DecorationBackground.SEMI_TRANSPARENT -> 1
            DecorationBackground.SOLID -> 2
        })

        // Animation
        setupSpinner(binding.contentClock.spinnerAnimation, arrayOf("Static", "Pulse Softly"))
        binding.contentClock.spinnerAnimation.setSelection(if (clockConfig.animation == DecorationAnimation.PULSE_SOFTLY) 1 else 0)

        // Pulse speed
        binding.contentClock.layoutPulseSpeed.visibility = if (clockConfig.animation == DecorationAnimation.PULSE_SOFTLY) View.VISIBLE else View.GONE
        setupSpinner(binding.contentClock.spinnerPulseSpeed, arrayOf("Slow", "Medium", "Fast"))
        binding.contentClock.spinnerPulseSpeed.setSelection(when (clockConfig.pulseSpeed) {
            PulseSpeed.SLOW -> 0
            PulseSpeed.MEDIUM -> 1
            PulseSpeed.FAST -> 2
        })

        // Opacity sliders
        binding.contentClock.sliderOpacity.value = clockConfig.opacity.toFloat()
        binding.contentClock.sliderPulseMinOpacity.value = clockConfig.pulseMinOpacity.toFloat()
        binding.contentClock.sliderPulseMaxOpacity.value = clockConfig.pulseMaxOpacity.toFloat()

        // Animation change listener
        binding.contentClock.spinnerAnimation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.contentClock.layoutPulseSpeed.visibility = if (position == 1) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupWeatherTab() {
        // Position
        setupSpinner(binding.contentWeather.spinnerPosition, arrayOf("Top Left", "Top Right", "Bottom Left", "Bottom Right", "Center"))
        binding.contentWeather.spinnerPosition.setSelection(when (weatherConfig.position) {
            ClockPosition.TOP_LEFT -> 0
            ClockPosition.TOP_RIGHT -> 1
            ClockPosition.BOTTOM_LEFT -> 2
            ClockPosition.BOTTOM_RIGHT -> 3
            ClockPosition.CENTER -> 4
        })

        // Use device location
        binding.contentWeather.switchUseDeviceLocation.isChecked = weatherConfig.useDeviceLocation
        binding.contentWeather.layoutManualLocation.visibility = if (weatherConfig.useDeviceLocation) View.GONE else View.VISIBLE

        binding.contentWeather.switchUseDeviceLocation.setOnCheckedChangeListener { _, isChecked ->
            binding.contentWeather.layoutManualLocation.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        // Temperature unit
        setupSpinner(binding.contentWeather.spinnerTempUnit, arrayOf("Fahrenheit", "Celsius"))
        binding.contentWeather.spinnerTempUnit.setSelection(if (weatherConfig.temperatureUnit == TemperatureUnit.CELSIUS) 1 else 0)

        // Icon style
        setupSpinner(binding.contentWeather.spinnerIconStyle, arrayOf("Minimal", "Detailed", "Animated"))
        binding.contentWeather.spinnerIconStyle.setSelection(when (weatherConfig.iconStyle) {
            WeatherIconStyle.MINIMAL -> 0
            WeatherIconStyle.DETAILED -> 1
            WeatherIconStyle.ANIMATED -> 2
        })

        // Widget background
        setupSpinner(binding.contentWeather.spinnerWidgetBackground, arrayOf("Transparent", "Frosted Glass", "Solid"))
        binding.contentWeather.spinnerWidgetBackground.setSelection(when (weatherConfig.widgetBackground) {
            WeatherWidgetBackground.TRANSPARENT -> 0
            WeatherWidgetBackground.FROSTED_GLASS -> 1
            WeatherWidgetBackground.SOLID -> 2
        })

        // Font size
        setupSpinner(binding.contentWeather.spinnerFontSize, arrayOf("Small", "Medium", "Large"))
        binding.contentWeather.spinnerFontSize.setSelection(when (weatherConfig.fontSize) {
            ClockSize.SMALL -> 0
            ClockSize.MEDIUM -> 1
            ClockSize.LARGE -> 2
        })

        // Animation
        setupSpinner(binding.contentWeather.spinnerAnimation, arrayOf("Static", "Pulse Softly"))
        binding.contentWeather.spinnerAnimation.setSelection(if (weatherConfig.animation == DecorationAnimation.PULSE_SOFTLY) 1 else 0)

        // Pulse speed
        binding.contentWeather.layoutPulseSpeed.visibility = if (weatherConfig.animation == DecorationAnimation.PULSE_SOFTLY) View.VISIBLE else View.GONE
        setupSpinner(binding.contentWeather.spinnerPulseSpeed, arrayOf("Slow", "Medium", "Fast"))
        binding.contentWeather.spinnerPulseSpeed.setSelection(when (weatherConfig.pulseSpeed) {
            PulseSpeed.SLOW -> 0
            PulseSpeed.MEDIUM -> 1
            PulseSpeed.FAST -> 2
        })

        // Opacity sliders
        binding.contentWeather.sliderOpacity.value = weatherConfig.opacity.toFloat()
        binding.contentWeather.sliderPulseMinOpacity.value = weatherConfig.pulseMinOpacity.toFloat()
        binding.contentWeather.sliderPulseMaxOpacity.value = weatherConfig.pulseMaxOpacity.toFloat()

        // Animation change listener
        binding.contentWeather.spinnerAnimation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.contentWeather.layoutPulseSpeed.visibility = if (position == 1) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSpinner(spinner: android.widget.Spinner, items: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        // Save date config
        val newDateConfig = if (dateConfig.enabled) {
            DecorationConfig(
                enabled = true,
                position = when (binding.contentDate.spinnerPosition.selectedItemPosition) {
                    0 -> ClockPosition.TOP_LEFT
                    1 -> ClockPosition.TOP_RIGHT
                    2 -> ClockPosition.BOTTOM_LEFT
                    3 -> ClockPosition.BOTTOM_RIGHT
                    4 -> ClockPosition.CENTER
                    else -> ClockPosition.BOTTOM_LEFT
                },
                dateFormat = when (binding.contentDate.spinnerDateFormat.selectedItemPosition) {
                    0 -> DateFormat.FULL_DATE
                    1 -> DateFormat.SHORT_DATE
                    2 -> DateFormat.MONTH_DAY
                    3 -> DateFormat.NUMERIC_DATE
                    4 -> DateFormat.ABBREVIATE_MONTH
                    else -> DateFormat.CUSTOM
                },
                fontSize = when (binding.contentDate.spinnerFontSize.selectedItemPosition) {
                    0 -> ClockSize.SMALL
                    1 -> ClockSize.MEDIUM
                    2 -> ClockSize.LARGE
                    else -> ClockSize.MEDIUM
                },
                background = when (binding.contentDate.spinnerBackground.selectedItemPosition) {
                    0 -> DecorationBackground.NONE
                    1 -> DecorationBackground.SEMI_TRANSPARENT
                    2 -> DecorationBackground.SOLID
                    else -> DecorationBackground.NONE
                },
                animation = if (binding.contentDate.spinnerAnimation.selectedItemPosition == 1) 
                    DecorationAnimation.PULSE_SOFTLY else DecorationAnimation.STATIC,
                pulseSpeed = when (binding.contentDate.spinnerPulseSpeed.selectedItemPosition) {
                    0 -> PulseSpeed.SLOW
                    1 -> PulseSpeed.MEDIUM
                    2 -> PulseSpeed.FAST
                    else -> PulseSpeed.MEDIUM
                },
                opacity = binding.contentDate.sliderOpacity.value.toInt(),
                pulseMinOpacity = binding.contentDate.sliderPulseMinOpacity.value.toInt(),
                pulseMaxOpacity = binding.contentDate.sliderPulseMaxOpacity.value.toInt()
            )
        } else {
            null
        }

        // Save clock config
        val newClockConfig = if (clockConfig.enabled) {
            ClockDecorationConfig(
                enabled = true,
                position = when (binding.contentClock.spinnerPosition.selectedItemPosition) {
                    0 -> ClockPosition.TOP_LEFT
                    1 -> ClockPosition.TOP_RIGHT
                    2 -> ClockPosition.BOTTOM_LEFT
                    3 -> ClockPosition.BOTTOM_RIGHT
                    4 -> ClockPosition.CENTER
                    else -> ClockPosition.BOTTOM_RIGHT
                },
                clockFormat = if (binding.contentClock.spinnerFormat.selectedItemPosition == 1) 
                    ClockFormat.HOUR_24 else ClockFormat.HOUR_12,
                showSeconds = binding.contentClock.switchShowSeconds.isChecked,
                fontSize = when (binding.contentClock.spinnerFontSize.selectedItemPosition) {
                    0 -> ClockSize.SMALL
                    1 -> ClockSize.MEDIUM
                    2 -> ClockSize.LARGE
                    else -> ClockSize.MEDIUM
                },
                background = when (binding.contentClock.spinnerBackground.selectedItemPosition) {
                    0 -> DecorationBackground.NONE
                    1 -> DecorationBackground.SEMI_TRANSPARENT
                    2 -> DecorationBackground.SOLID
                    else -> DecorationBackground.NONE
                },
                animation = if (binding.contentClock.spinnerAnimation.selectedItemPosition == 1) 
                    DecorationAnimation.PULSE_SOFTLY else DecorationAnimation.STATIC,
                pulseSpeed = when (binding.contentClock.spinnerPulseSpeed.selectedItemPosition) {
                    0 -> PulseSpeed.SLOW
                    1 -> PulseSpeed.MEDIUM
                    2 -> PulseSpeed.FAST
                    else -> PulseSpeed.MEDIUM
                },
                opacity = binding.contentClock.sliderOpacity.value.toInt(),
                pulseMinOpacity = binding.contentClock.sliderPulseMinOpacity.value.toInt(),
                pulseMaxOpacity = binding.contentClock.sliderPulseMaxOpacity.value.toInt()
            )
        } else {
            null
        }

        // Save weather config
        val newWeatherConfig = if (weatherConfig.enabled) {
            WeatherDecorationConfig(
                enabled = true,
                position = when (binding.contentWeather.spinnerPosition.selectedItemPosition) {
                    0 -> ClockPosition.TOP_LEFT
                    1 -> ClockPosition.TOP_RIGHT
                    2 -> ClockPosition.BOTTOM_LEFT
                    3 -> ClockPosition.BOTTOM_RIGHT
                    4 -> ClockPosition.CENTER
                    else -> ClockPosition.TOP_RIGHT
                },
                useDeviceLocation = binding.contentWeather.switchUseDeviceLocation.isChecked,
                temperatureUnit = if (binding.contentWeather.spinnerTempUnit.selectedItemPosition == 1) 
                    TemperatureUnit.CELSIUS else TemperatureUnit.FAHRENHEIT,
                iconStyle = when (binding.contentWeather.spinnerIconStyle.selectedItemPosition) {
                    0 -> WeatherIconStyle.MINIMAL
                    1 -> WeatherIconStyle.DETAILED
                    2 -> WeatherIconStyle.ANIMATED
                    else -> WeatherIconStyle.MINIMAL
                },
                widgetBackground = when (binding.contentWeather.spinnerWidgetBackground.selectedItemPosition) {
                    0 -> WeatherWidgetBackground.TRANSPARENT
                    1 -> WeatherWidgetBackground.FROSTED_GLASS
                    2 -> WeatherWidgetBackground.SOLID
                    else -> WeatherWidgetBackground.TRANSPARENT
                },
                fontSize = when (binding.contentWeather.spinnerFontSize.selectedItemPosition) {
                    0 -> ClockSize.SMALL
                    1 -> ClockSize.MEDIUM
                    2 -> ClockSize.LARGE
                    else -> ClockSize.MEDIUM
                },
                animation = if (binding.contentWeather.spinnerAnimation.selectedItemPosition == 1) 
                    DecorationAnimation.PULSE_SOFTLY else DecorationAnimation.STATIC,
                pulseSpeed = when (binding.contentWeather.spinnerPulseSpeed.selectedItemPosition) {
                    0 -> PulseSpeed.SLOW
                    1 -> PulseSpeed.MEDIUM
                    2 -> PulseSpeed.FAST
                    else -> PulseSpeed.MEDIUM
                },
                opacity = binding.contentWeather.sliderOpacity.value.toInt(),
                pulseMinOpacity = binding.contentWeather.sliderPulseMinOpacity.value.toInt(),
                pulseMaxOpacity = binding.contentWeather.sliderPulseMaxOpacity.value.toInt()
            )
        } else {
            null
        }

        // Save all to DataStore
        val config = SettingsManager.getSlideshowConfig(this)
        SettingsManager.saveSlideshowConfig(
            this,
            config.copy(
                dateDecoration = newDateConfig,
                clockDecoration = newClockConfig,
                weatherDecoration = newWeatherConfig
            )
        )

        Snackbar.make(binding.root, "Decoration settings saved!", Snackbar.LENGTH_SHORT).show()
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
