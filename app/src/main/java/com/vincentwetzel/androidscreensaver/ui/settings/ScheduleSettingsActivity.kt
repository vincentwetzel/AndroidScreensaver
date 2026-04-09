package com.vincentwetzel.androidscreensaver.ui.settings

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.DayOfWeek
import com.vincentwetzel.androidscreensaver.data.model.ScheduleConfig
import com.vincentwetzel.androidscreensaver.data.model.SchedulePreset
import com.vincentwetzel.androidscreensaver.databinding.ActivityScheduleSettingsBinding
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

/**
 * Schedule Settings Activity
 * Configure autostart/autostop schedules
 */
@AndroidEntryPoint
class ScheduleSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleSettingsBinding
    private var isAutostart = true
    private var selectedHour = 20
    private var selectedMinute = 0
    private val selectedDays = mutableSetOf<DayOfWeek>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("Schedule Settings")

        setupTabs()
        loadCurrentSettings()
        setupListeners()
    }

    private fun setupTabs() {
        binding.tabScheduleType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isAutostart = tab?.position == 0
                updateDescription()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateDescription() {
        binding.tvScheduleDescription.text = if (isAutostart) {
            "Automatically start screensaver at scheduled time"
        } else {
            "Automatically stop screensaver at scheduled time"
        }
    }

    private fun loadCurrentSettings() {
        val config = SettingsManager.getSlideshowConfig(this)
        val schedules = if (isAutostart) config.autostartSchedules else config.autostopSchedules

        if (schedules.isNotEmpty()) {
            val first = schedules.first()
            binding.switchEnabled.isChecked = first.enabled
            selectedHour = first.timeHour
            selectedMinute = first.timeMinute
            selectedDays.clear()
            selectedDays.addAll(first.daysOfWeek)
            binding.switchRepeat.isChecked = first.repeat
            binding.switchCharging.isChecked = first.onlyWhenCharging
        }

        updateTimeButton()
        updateDayCheckboxes()
    }

    private fun setupListeners() {
        // Time picker
        binding.btnTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedHour = hour
                    selectedMinute = minute
                    updateTimeButton()
                    saveCurrentSettings()
                },
                selectedHour,
                selectedMinute,
                false
            ).show()
        }

        // Quick presets — auto-save
        binding.btnWeekdays.setOnClickListener {
            selectedDays.clear()
            selectedDays.addAll(listOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            ))
            updateDayCheckboxes()
            saveCurrentSettings()
        }

        binding.btnWeekends.setOnClickListener {
            selectedDays.clear()
            selectedDays.addAll(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
            updateDayCheckboxes()
            saveCurrentSettings()
        }

        binding.btnEveryday.setOnClickListener {
            selectedDays.clear()
            selectedDays.addAll(DayOfWeek.values().toList())
            updateDayCheckboxes()
            saveCurrentSettings()
        }

        // Day checkboxes — auto-save
        mapOf(
            binding.cbMon to DayOfWeek.MONDAY,
            binding.cbTue to DayOfWeek.TUESDAY,
            binding.cbWed to DayOfWeek.WEDNESDAY,
            binding.cbThu to DayOfWeek.THURSDAY,
            binding.cbFri to DayOfWeek.FRIDAY,
            binding.cbSat to DayOfWeek.SATURDAY,
            binding.cbSun to DayOfWeek.SUNDAY
        ).forEach { (checkbox, day) ->
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedDays.add(day)
                } else {
                    selectedDays.remove(day)
                }
                saveCurrentSettings()
            }
        }

        // Toggle switches — auto-save
        binding.switchEnabled.setOnCheckedChangeListener { _, _ ->
            saveCurrentSettings()
        }

        binding.switchRepeat.setOnCheckedChangeListener { _, _ ->
            saveCurrentSettings()
        }

        binding.switchCharging.setOnCheckedChangeListener { _, _ ->
            saveCurrentSettings()
        }
    }

    /**
     * Read current UI values and persist to DataStore
     */
    private fun saveCurrentSettings() {
        val config = SettingsManager.getSlideshowConfig(this)

        val newSchedule = ScheduleConfig(
            enabled = binding.switchEnabled.isChecked,
            timeHour = selectedHour,
            timeMinute = selectedMinute,
            daysOfWeek = selectedDays,
            schedulePreset = if (selectedDays.size == 5 && !selectedDays.contains(DayOfWeek.SATURDAY) && !selectedDays.contains(DayOfWeek.SUNDAY)) {
                SchedulePreset.WEEKDAYS
            } else if (selectedDays.size == 2 && selectedDays.contains(DayOfWeek.SATURDAY) && selectedDays.contains(DayOfWeek.SUNDAY)) {
                SchedulePreset.WEEKENDS
            } else if (selectedDays.size == 7) {
                SchedulePreset.EVERY_DAY
            } else {
                SchedulePreset.CUSTOM
            },
            repeat = binding.switchRepeat.isChecked,
            onlyWhenCharging = binding.switchCharging.isChecked
        )

        val newConfig = if (isAutostart) {
            config.copy(autostartSchedules = listOf(newSchedule))
        } else {
            config.copy(autostopSchedules = listOf(newSchedule))
        }

        SettingsManager.saveSlideshowConfig(this, newConfig)
    }

    private fun updateTimeButton() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, selectedHour)
        cal.set(Calendar.MINUTE, selectedMinute)
        val format = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        binding.btnTime.text = format.format(cal.time)
    }

    private fun updateDayCheckboxes() {
        binding.cbMon.isChecked = DayOfWeek.MONDAY in selectedDays
        binding.cbTue.isChecked = DayOfWeek.TUESDAY in selectedDays
        binding.cbWed.isChecked = DayOfWeek.WEDNESDAY in selectedDays
        binding.cbThu.isChecked = DayOfWeek.THURSDAY in selectedDays
        binding.cbFri.isChecked = DayOfWeek.FRIDAY in selectedDays
        binding.cbSat.isChecked = DayOfWeek.SATURDAY in selectedDays
        binding.cbSun.isChecked = DayOfWeek.SUNDAY in selectedDays
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
