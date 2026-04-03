package com.vincentwetzel.androidscreensaver.ui.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.vincentwetzel.androidscreensaver.databinding.ActivityDebugSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Debug Settings Activity
 * Development-only options for testing and diagnostics
 */
@AndroidEntryPoint
class DebugSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebugSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("Debug Settings")

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Display system information
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val freeMemory = runtime.freeMemory() / (1024 * 1024)
        val usedMemory = totalMemory - freeMemory

        binding.tvSystemInfo.text = buildString {
            appendLine("Android Version: ${android.os.Build.VERSION.RELEASE}")
            appendLine("SDK Level: ${android.os.Build.VERSION.SDK_INT}")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("")
            appendLine("Max Memory: ${maxMemory} MB")
            appendLine("Total Memory: ${totalMemory} MB")
            appendLine("Used Memory: ${usedMemory} MB")
            appendLine("Free Memory: ${freeMemory} MB")
            appendLine("")
            appendLine("App Version: ${packageManager.getPackageInfo(packageName, 0).versionName}")
            appendLine("Build Code: ${packageManager.getPackageInfo(packageName, 0).longVersionCode}")
        }
    }

    private fun setupListeners() {
        // Debug overlay toggle
        binding.switchDebugOverlay.setOnCheckedChangeListener { _, isChecked ->
            Snackbar.make(binding.root,
                "Debug overlay ${if (isChecked) "enabled" else "disabled"}",
                Snackbar.LENGTH_SHORT).show()
        }

        // Export logs
        binding.btnExportLogs.setOnClickListener {
            Snackbar.make(binding.root, "Logs exported to Downloads", Snackbar.LENGTH_SHORT).show()
        }

        // Reset settings
        binding.btnResetSettings.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset All Settings?")
                .setMessage("This will reset all settings to their default values. This cannot be undone.")
                .setPositiveButton("Reset") { _, _ ->
                    // Clear DataStore preferences
                    getSharedPreferences("screensaver_settings", MODE_PRIVATE).edit().clear().apply()
                    Snackbar.make(binding.root, "All settings reset to defaults", Snackbar.LENGTH_LONG).show()
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Test crash
        binding.btnTestCrash.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Test Crash Reporting")
                .setMessage("This will trigger a test crash to verify crash reporting is working. Continue?")
                .setPositiveButton("Crash") { _, _ ->
                    throw RuntimeException("Test crash - this is intentional for testing purposes")
                }
                .setNegativeButton("Cancel", null)
                .show()
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
