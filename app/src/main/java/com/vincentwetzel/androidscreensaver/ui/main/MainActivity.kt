package com.vincentwetzel.androidscreensaver.ui.main

import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.databinding.ActivityMainBinding
import com.vincentwetzel.androidscreensaver.databinding.ActivityMainTvBinding
import com.vincentwetzel.androidscreensaver.ui.settings.SettingsActivity
import com.vincentwetzel.androidscreensaver.ui.sources.FolderBrowserActivity
import com.vincentwetzel.androidscreensaver.ui.sources.GalleryFolderBrowserActivity
import com.vincentwetzel.androidscreensaver.ui.sources.GoogleDriveAuthActivity
import com.vincentwetzel.androidscreensaver.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity - Entry point of the app
 * Automatically selects TV or phone/tablet layout based on device type
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Phone/Tablet binding
    private var binding: ActivityMainBinding? = null
    
    // TV binding
    private var bindingTv: ActivityMainTvBinding? = null
    
    private val viewModel: MainViewModel by viewModels()
    private var isTvLayout = false

    // Activity result launcher for Google Drive auth
    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // User successfully authenticated
            viewModel.onGoogleDriveAuthenticated(true)
            updateDriveStatus(true)
            showSnackbar("Google Drive connected!")
            // Now open folder browser
            openFolderBrowser()
        }
    }

    // Activity result launcher for folder browser
    private val folderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedFolders = result.data?.getStringArrayListExtra(FolderBrowserActivity.RESULT_SELECTED_FOLDERS)
            selectedFolders?.let { folders ->
                com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSelectedFolders(
                    this,
                    com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE,
                    folders.toSet()
                )
            }
            showSnackbar("${selectedFolders?.size ?: 0} folders selected")
        }
    }

    // Activity result launcher for Gallery folder browser
    private val galleryFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedFolders = result.data?.getStringArrayListExtra(GalleryFolderBrowserActivity.RESULT_SELECTED_FOLDERS)
            selectedFolders?.let { folders ->
                com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSelectedFolders(
                    this,
                    com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY,
                    folders.toSet()
                )
            }
            showSnackbar("${selectedFolders?.size ?: 0} folders selected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Detect if running on TV
        isTvLayout = isTelevision()
        
        if (isTvLayout) {
            bindingTv = ActivityMainTvBinding.inflate(layoutInflater)
            setContentView(bindingTv!!.root)
            setupTvUI()
        } else {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding!!.root)
            setupPhoneUI()
        }

        updateActivationCardVisibility()

        setupToolbar()
        setupAnimations()
        observeViewModel()
    }

    /**
     * Check if device is a TV (Android TV / NVidia Shield)
     */
    private fun isTelevision(): Boolean {
        val pm = packageManager
        return pm.hasSystemFeature("android.software.leanback") ||
               pm.hasSystemFeature("android.hardware.type.television")
    }

    private fun setupPhoneUI() {
        binding?.let { b ->
            b.cardGoogleDrive.setOnClickListener {
                if (viewModel.isGoogleDriveAuthenticated.value == true) {
                    openFolderBrowser()
                } else {
                    startGoogleDriveAuth()
                }
            }

            b.cardGallery?.setOnClickListener {
                val isEnabled = com.vincentwetzel.androidscreensaver.utils.SettingsManager.isSourceEnabled(
                    this,
                    com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY
                )
                if (isEnabled) {
                    openGalleryFolderBrowser()
                }
            }

            b.switchGallery?.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.enableSource(SourceType.GALLERY)
                    com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSourceEnabled(
                        this,
                        com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY,
                        true
                    )
                    showSnackbar("Gallery enabled")
                    openGalleryFolderBrowser()
                } else {
                    viewModel.disableSource(SourceType.GALLERY)
                    com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSourceEnabled(
                        this,
                        com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY,
                        false
                    )
                    showSnackbar("Gallery disabled")
                }
            }

            b.switchGoogleDrive.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (viewModel.isGoogleDriveAuthenticated.value == true) {
                        viewModel.enableSource(SourceType.GOOGLE_DRIVE)
                        com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSourceEnabled(
                            this,
                            com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE,
                            true
                        )
                        showSnackbar("Google Drive enabled")
                    } else {
                        startGoogleDriveAuth()
                        b.switchGoogleDrive.isChecked = false
                    }
                } else {
                    viewModel.disableSource(SourceType.GOOGLE_DRIVE)
                    com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSourceEnabled(
                        this,
                        com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE,
                        false
                    )
                    showSnackbar("Google Drive disabled")
                }
            }

            b.btnActivateScreensaver?.setOnClickListener {
                openScreensaverSettings()
            }
        }
    }

    private fun setupTvUI() {
        bindingTv?.let { b ->
            b.cardGoogleDrive.setOnClickListener {
                if (viewModel.isGoogleDriveAuthenticated.value == true) {
                    openFolderBrowser()
                } else {
                    startGoogleDriveAuth()
                }
            }

            b.cardGallery?.setOnClickListener {
                val isEnabled = com.vincentwetzel.androidscreensaver.utils.SettingsManager.isSourceEnabled(
                    this,
                    com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY
                )
                if (isEnabled) {
                    openGalleryFolderBrowser()
                }
            }

            b.switchGallery?.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.enableSource(SourceType.GALLERY)
                    com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSourceEnabled(
                        this,
                        com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY,
                        true
                    )
                    showSnackbar("Gallery enabled")
                    openGalleryFolderBrowser()
                } else {
                    viewModel.disableSource(SourceType.GALLERY)
                    com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSourceEnabled(
                        this,
                        com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY,
                        false
                    )
                    showSnackbar("Gallery disabled")
                }
            }

            b.switchGoogleDrive.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (viewModel.isGoogleDriveAuthenticated.value == true) {
                        viewModel.enableSource(SourceType.GOOGLE_DRIVE)
                        com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSourceEnabled(
                            this,
                            com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE,
                            true
                        )
                        showSnackbar("Google Drive enabled")
                    } else {
                        startGoogleDriveAuth()
                        b.switchGoogleDrive.isChecked = false
                    }
                } else {
                    viewModel.disableSource(SourceType.GOOGLE_DRIVE)
                    com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSourceEnabled(
                        this,
                        com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE,
                        false
                    )
                    showSnackbar("Google Drive disabled")
                }
            }

            b.btnActivateScreensaver?.setOnClickListener {
                openScreensaverSettings()
            }

            b.btnSettings.setOnClickListener {
                startActivity(android.content.Intent(this, SettingsActivity::class.java))
            }
        }
    }

    private fun setupToolbar() {
        if (!isTvLayout) {
            binding?.let { setSupportActionBar(it.toolbar) }
        }
    }

    private fun setupAnimations() {
        // Animate header entrance
        val headerView = if (isTvLayout) bindingTv?.tvHeader else binding?.tvHeader
        headerView?.animate()
            ?.alpha(1f)
            ?.translationY(0f)
            ?.setDuration(500)
            ?.setInterpolator(android.view.animation.DecelerateInterpolator())
            ?.start()
    }

    private fun observeViewModel() {
        viewModel.isGoogleDriveAuthenticated.observe(this) { isAuthenticated ->
            updateDriveStatus(isAuthenticated == true)
        }

        viewModel.enabledSources.observe(this) { sources ->
            // Update UI based on enabled sources
            android.util.Log.d("MainActivity", "Enabled sources: $sources")
        }
    }

    private fun updateDriveStatus(isAuthenticated: Boolean) {
        if (isTvLayout) {
            bindingTv?.statusGoogleDrive?.text = if (isAuthenticated) {
                getString(R.string.authenticated)
            } else {
                getString(R.string.not_authenticated)
            }
        } else {
            binding?.statusGoogleDrive?.text = if (isAuthenticated) {
                getString(R.string.authenticated)
            } else {
                getString(R.string.not_authenticated)
            }
        }
    }

    private fun startGoogleDriveAuth() {
        val intent = android.content.Intent(this, GoogleDriveAuthActivity::class.java)
        authLauncher.launch(intent)
    }

    private fun openFolderBrowser() {
        val intent = android.content.Intent(this, FolderBrowserActivity::class.java)
        folderLauncher.launch(intent)
    }

    private fun openGalleryFolderBrowser() {
        val intent = android.content.Intent(this, GalleryFolderBrowserActivity::class.java)
        galleryFolderLauncher.launch(intent)
    }

    private fun openScreensaverSettings() {
        val intent = android.content.Intent(Settings.ACTION_DREAM_SETTINGS)
        startActivity(intent)
    }

    /**
     * Launch the screensaver immediately for testing
     * Uses a dedicated PreviewActivity that mimics the DreamService
     * since apps cannot directly trigger DreamService (system-controlled)
     */
    private fun launchScreensaver() {
        try {
            val intent = android.content.Intent(this, com.vincentwetzel.androidscreensaver.ui.ScreensaverPreviewActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error launching screensaver", e)
            showSnackbar("Error: ${e.message}")
        }
    }

    /**
     * Check if this app is set as the active screensaver
     * Returns true if our DreamService component is currently selected
     */
    private fun isScreensaverActive(): Boolean {
        return try {
            // Check multiple possible setting keys (Samsung/OneUI may use different keys)
            val settingsToCheck = listOf(
                "dream_components",
                "screensaver_components",
                "dream_component",
            )

            for (key in settingsToCheck) {
                val value = Settings.Secure.getString(contentResolver, key)
                if (value?.contains(packageName) == true) {
                    return true
                }
            }

            // Fallback: check if our service is the only DreamService registered
            // and the dream setting has any value set
            val dreamServices = packageManager.queryIntentServices(
                android.content.Intent(android.service.dreams.DreamService.SERVICE_INTERFACE),
                0
            )

            if (dreamServices.size == 1 && dreamServices[0].serviceInfo.packageName == packageName) {
                // We're the only DreamService — check if any screensaver is enabled
                val enabled = Settings.Secure.getInt(contentResolver, "screensaver_enabled", 0)
                return enabled == 1
            }

            false
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error checking screensaver status", e)
            false
        }
    }

    /**
     * Check if DreamService is supported on this device
     */
    private fun isDreamServiceSupported(): Boolean {
        return try {
            val intent = android.content.Intent(android.service.dreams.DreamService.SERVICE_INTERFACE)
            val services = packageManager.queryIntentServices(intent, 0)
            services.isNotEmpty()
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "DreamService not supported: ${e.message}")
            false
        }
    }

    /**
     * Hide or show the activation card based on whether this app is the active screensaver
     */
    private fun updateActivationCardVisibility() {
        val dreamSupported = isDreamServiceSupported()
        android.util.Log.d("MainActivity", "DreamService supported: $dreamSupported")

        if (!dreamSupported) {
            // DreamService not supported — hide card or show alternative message
            binding?.cardActivation?.visibility = View.GONE
            bindingTv?.cardActivate?.visibility = View.GONE
            showSnackbar("Note: This device doesn't support Android screensaver (DreamService)")
            return
        }

        val isActive = isScreensaverActive()
        binding?.cardActivation?.visibility = if (isActive) View.GONE else View.VISIBLE
        bindingTv?.cardActivate?.visibility = if (isActive) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        // Re-check when returning to the app
        updateActivationCardVisibility()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!isTvLayout) {
            menuInflater.inflate(R.menu.menu_main, menu)
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_test_screensaver -> {
                launchScreensaver()
                true
            }
            R.id.action_settings -> {
                startActivity(android.content.Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSnackbar(message: String) {
        val rootView = if (isTvLayout) bindingTv?.root else binding?.root
        rootView?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }
}
