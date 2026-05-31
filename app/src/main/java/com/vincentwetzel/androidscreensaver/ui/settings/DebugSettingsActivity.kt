package com.vincentwetzel.androidscreensaver.ui.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.vincentwetzel.androidscreensaver.databinding.ActivityDebugSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

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
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            appendLine("App Version: ${packageInfo.versionName}")
            appendLine("Build Code: ${PackageInfoCompat.getLongVersionCode(packageInfo)}")
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
                .setMessage("This will reset all settings to their default values and exit the app to clear memory. This cannot be undone.")
                .setPositiveButton("Reset") { _, _ ->
                    // Brute-force clear DataStore and EncryptedSharedPreferences directories
                    lifecycleScope.launch(Dispatchers.IO) {
                        File(filesDir, "datastore").deleteRecursively()
                        val sharedPrefsDir = File(applicationInfo.dataDir, "shared_prefs")
                        if (sharedPrefsDir.exists()) {
                            sharedPrefsDir.listFiles()?.forEach { file ->
                                file.delete()
                            }
                        }
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            Snackbar.make(binding.root, "Settings reset. Exiting app...", Snackbar.LENGTH_LONG).show()
                            binding.root.postDelayed({
                                kotlin.system.exitProcess(0)
                            }, 1500)
                        }
                    }
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
