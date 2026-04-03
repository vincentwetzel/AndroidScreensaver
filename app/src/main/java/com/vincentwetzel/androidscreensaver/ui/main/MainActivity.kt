package com.vincentwetzel.androidscreensaver.ui.main

import android.os.Bundle
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

            b.btnPreview.setOnClickListener {
                Toast.makeText(this, "Preview coming soon!", Toast.LENGTH_SHORT).show()
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

            b.btnPreview.setOnClickListener {
                Toast.makeText(this, "Preview coming soon!", Toast.LENGTH_LONG).show()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!isTvLayout) {
            menuInflater.inflate(R.menu.menu_main, menu)
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
