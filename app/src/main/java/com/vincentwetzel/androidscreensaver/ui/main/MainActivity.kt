package com.vincentwetzel.androidscreensaver.ui.main

import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    private var binding: ActivityMainBinding? = null
    private var bindingTv: ActivityMainTvBinding? = null

    private val viewModel: MainViewModel by viewModels()
    private var isTvLayout = false
    private var isRestoringToggleState = false

    // Direct references to source card views for reliable refresh
    private var galleryCardInfo: CardInfo? = null
    private var driveCardInfo: CardInfo? = null

    // Activity result launcher for Google Drive auth
    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val accountName = result.data?.getStringExtra(GoogleDriveAuthActivity.EXTRA_ACCOUNT_NAME)
            viewModel.onGoogleDriveAuthenticated(true, accountName)
            refreshSourceCards()
            showSnackbar("Google Drive connected!")
            openFolderBrowser()
        }
    }

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

        isTvLayout = isTelevision()

        if (isTvLayout) {
            bindingTv = ActivityMainTvBinding.inflate(layoutInflater)
            setContentView(bindingTv!!.root)
        } else {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding!!.root)
        }

        setupCommonUI()
        setupAnimations()
        observeViewModel()
    }

    private fun isTelevision(): Boolean {
        val pm = packageManager
        return pm.hasSystemFeature("android.software.leanback") ||
               pm.hasSystemFeature("android.hardware.type.television")
    }

    private fun setupCommonUI() {
        if (!isTvLayout) {
            binding?.let { setSupportActionBar(it.toolbar) }
        }

        binding?.btnActivateScreensaver?.setOnClickListener { openScreensaverSettings() }
        bindingTv?.btnActivateScreensaver?.setOnClickListener { openScreensaverSettings() }
        bindingTv?.btnSettings?.setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
        }
        binding?.btnAddSources?.setOnClickListener { showAddSourcesDialog() }
        bindingTv?.btnAddSources?.setOnClickListener { showAddSourcesDialog() }

        buildSourceCards()
        refreshSourceCards()
        updateActivationCardVisibility()
    }

    private fun buildSourceCards() {
        val container = if (isTvLayout) bindingTv?.connectedSourcesContainer else binding?.connectedSourcesContainer
        container?.let { c ->
            c.removeAllViews()

            val padding = if (isTvLayout) 32 else 16
            val iconSize = if (isTvLayout) 72 else 40
            val switchScale = if (isTvLayout) 1.5f else 1f

            // Gallery card
            val (galleryCard, galleryInfo) = createSourceCard(
                "Gallery", getString(R.string.device_photos), R.drawable.ic_source_gallery,
                padding, iconSize, switchScale, SourceType.GALLERY
            )
            c.addView(galleryCard)
            galleryCardInfo = galleryInfo

            // Google Drive card
            val (driveCard, driveInfo) = createSourceCard(
                "Google Drive", getString(R.string.not_authenticated), R.drawable.ic_source_google_drive,
                padding, iconSize, switchScale, SourceType.GOOGLE_DRIVE
            )
            c.addView(driveCard)
            driveCardInfo = driveInfo
        }
    }

    private fun createSourceCard(
        name: String,
        initialSubtitle: String,
        iconRes: Int,
        padding: Int,
        iconSize: Int,
        switchScale: Float,
        sourceType: SourceType
    ): Pair<MaterialCardView, CardInfo> {
        val card = MaterialCardView(this).apply {
            val margin = dpToPx(if (isTvLayout) 8 else 6)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(margin, margin, margin, margin)
            }
            isClickable = true
            isFocusable = true
            cardElevation = if (isTvLayout) 8f else 4f
            radius = dpToPx(if (isTvLayout) 20 else 16).toFloat()
            strokeWidth = dpToPx(1)
            if (isTvLayout) {
                setCardBackgroundColor(getColor(R.color.card_background_tv))
                strokeColor = android.graphics.Color.parseColor("#33FFFFFF")
            } else {
                setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
                strokeColor = android.graphics.Color.parseColor("#E0E0E0")
            }
            // Add ripple effect
            foreground = android.graphics.drawable.Drawable.createFromPath("")
            tag = sourceType
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val iconSizePx = dpToPx(iconSize)
        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            layoutParams = LinearLayout.LayoutParams(iconSizePx, iconSizePx)
            contentDescription = name
            scaleType = ImageView.ScaleType.FIT_CENTER
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dpToPx(16)
        }

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val title = TextView(this).apply {
            text = name
            textSize = if (isTvLayout) 24f else 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            if (isTvLayout) {
                setTextColor(getColor(R.color.text_primary_tv))
            } else {
                setTextColor(android.graphics.Color.parseColor("#212121"))
            }
        }

        val statusText = TextView(this).apply {
            text = initialSubtitle
            textSize = if (isTvLayout) 14f else 13f
            if (isTvLayout) {
                setTextColor(getColor(R.color.text_secondary_tv))
            } else {
                setTextColor(android.graphics.Color.parseColor("#757575"))
            }
        }

        textContainer.addView(title)
        textContainer.addView(statusText)

        val switchView = MaterialSwitch(this).apply {
            if (switchScale > 1f) { scaleX = switchScale; scaleY = switchScale }
            setOnCheckedChangeListener { _, isChecked ->
                if (isRestoringToggleState) return@setOnCheckedChangeListener
                handleSourceToggle(sourceType, isChecked, this)
            }
        }

        row.addView(icon)
        row.addView(textContainer)
        row.addView(switchView)
        card.addView(row)

        card.setOnClickListener {
            when (sourceType) {
                SourceType.GALLERY -> {
                    val isEnabled = com.vincentwetzel.androidscreensaver.utils.SettingsManager.isSourceEnabled(
                        this@MainActivity, com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY
                    )
                    if (isEnabled) openGalleryFolderBrowser()
                }
                SourceType.GOOGLE_DRIVE -> {
                    if (viewModel.isGoogleDriveAuthenticated.value == true) openFolderBrowser()
                    else startGoogleDriveAuth()
                }
                else -> {}
            }
        }

        val cardInfo = CardInfo(card, title, statusText, switchView)
        return card to cardInfo
    }

    private fun handleSourceToggle(sourceType: SourceType, isChecked: Boolean, switchView: MaterialSwitch) {
        when (sourceType) {
            SourceType.GALLERY -> {
                if (isChecked) {
                    viewModel.enableSource(SourceType.GALLERY)
                    com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSourceEnabled(
                        this, com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY, true
                    )
                    showSnackbar("Gallery enabled")
                } else {
                    viewModel.disableSource(SourceType.GALLERY)
                    com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSourceEnabled(
                        this, com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY, false
                    )
                    showSnackbar("Gallery disabled")
                }
            }
            SourceType.GOOGLE_DRIVE -> {
                if (isChecked) {
                    if (viewModel.isGoogleDriveAuthenticated.value == true) {
                        viewModel.enableSource(SourceType.GOOGLE_DRIVE)
                        com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSourceEnabled(
                            this, com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE, true
                        )
                        showSnackbar("Google Drive enabled")
                    } else {
                        startGoogleDriveAuth()
                        switchView.isChecked = false
                    }
                } else {
                    viewModel.disableSource(SourceType.GOOGLE_DRIVE)
                    com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSourceEnabled(
                        this, com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE, false
                    )
                    showSnackbar("Google Drive disabled")
                }
            }
            else -> {}
        }
    }

    private fun refreshSourceCards() {
        isRestoringToggleState = true

        // Gallery
        val galleryEnabled = com.vincentwetzel.androidscreensaver.utils.SettingsManager.isSourceEnabled(
            this, com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY
        )
        galleryCardInfo?.switchView?.isChecked = galleryEnabled

        // Google Drive
        val driveEnabled = com.vincentwetzel.androidscreensaver.utils.SettingsManager.isSourceEnabled(
            this, com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE
        )
        driveCardInfo?.let { card ->
            card.switchView.isChecked = driveEnabled
            val accountName = viewModel.googleDriveAccountName.value
            card.statusText.text = if (viewModel.isGoogleDriveAuthenticated.value == true) {
                accountName?.let { "Signed in as $it" } ?: getString(R.string.authenticated)
            } else {
                getString(R.string.not_authenticated)
            }
        }

        isRestoringToggleState = false
    }

    private fun showAddSourcesDialog() {
        val galleryEnabled = com.vincentwetzel.androidscreensaver.utils.SettingsManager.isSourceEnabled(
            this, com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY
        )
        val driveEnabled = com.vincentwetzel.androidscreensaver.utils.SettingsManager.isSourceEnabled(
            this, com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE
        )

        val sources = mutableListOf<Pair<String, SourceType>>()
        if (!galleryEnabled) sources.add("Gallery" to SourceType.GALLERY)
        if (!driveEnabled) sources.add("Google Drive" to SourceType.GOOGLE_DRIVE)

        if (sources.isEmpty()) {
            showSnackbar("All available sources are already added")
            return
        }

        val sourceNames = sources.map { it.first }.toTypedArray()
        var selectedIndex = 0

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.available_sources)
            .setSingleChoiceItems(sourceNames, selectedIndex) { _, which -> selectedIndex = which }
            .setPositiveButton("Add") { _, _ ->
                if (selectedIndex >= 0 && selectedIndex < sources.size) {
                    handleAddSource(sources[selectedIndex].second)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun handleAddSource(sourceType: SourceType) {
        when (sourceType) {
            SourceType.GALLERY -> {
                viewModel.enableSource(SourceType.GALLERY)
                com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSourceEnabled(
                    this, com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY, true
                )
                refreshSourceCards()
                showSnackbar("Gallery enabled")
            }
            SourceType.GOOGLE_DRIVE -> {
                startGoogleDriveAuth()
            }
            else -> {}
        }
    }

    private fun setupAnimations() {
        val headerView = if (isTvLayout) bindingTv?.tvHeader else binding?.tvHeader
        headerView?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(500)
            ?.setInterpolator(android.view.animation.DecelerateInterpolator())?.start()
    }

    private fun observeViewModel() {
        viewModel.isGoogleDriveAuthenticated.observe(this) { isAuthenticated ->
            updateDriveStatus(isAuthenticated == true, viewModel.googleDriveAccountName.value)
        }
        viewModel.googleDriveAccountName.observe(this) { accountName ->
            updateDriveStatus(viewModel.isGoogleDriveAuthenticated.value == true, accountName)
        }
    }

    private fun updateDriveStatus(isAuthenticated: Boolean, accountName: String? = null) {
        val displayText = if (isAuthenticated) {
            accountName?.let { "Signed in as $it" } ?: getString(R.string.authenticated)
        } else {
            getString(R.string.not_authenticated)
        }
        driveCardInfo?.statusText?.text = displayText
    }

    private fun startGoogleDriveAuth() {
        authLauncher.launch(android.content.Intent(this, GoogleDriveAuthActivity::class.java))
    }

    private fun openFolderBrowser() {
        folderLauncher.launch(android.content.Intent(this, FolderBrowserActivity::class.java))
    }

    private fun openGalleryFolderBrowser() {
        galleryFolderLauncher.launch(android.content.Intent(this, GalleryFolderBrowserActivity::class.java))
    }

    private fun openScreensaverSettings() {
        startActivity(android.content.Intent(Settings.ACTION_DREAM_SETTINGS))
    }

    private fun launchScreensaver() {
        try {
            startActivity(android.content.Intent(this, com.vincentwetzel.androidscreensaver.ui.ScreensaverPreviewActivity::class.java))
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error launching screensaver", e)
            showSnackbar("Error: ${e.message}")
        }
    }

    private fun isScreensaverActive(): Boolean {
        return try {
            val settingsToCheck = listOf("dream_components", "screensaver_components", "dream_component")
            for (key in settingsToCheck) {
                if (Settings.Secure.getString(contentResolver, key)?.contains(packageName) == true) return true
            }
            val dreamServices = packageManager.queryIntentServices(
                android.content.Intent(android.service.dreams.DreamService.SERVICE_INTERFACE), 0
            )
            if (dreamServices.size == 1 && dreamServices[0].serviceInfo.packageName == packageName) {
                return Settings.Secure.getInt(contentResolver, "screensaver_enabled", 0) == 1
            }
            false
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error checking screensaver status", e)
            false
        }
    }

    private fun isDreamServiceSupported(): Boolean {
        return try {
            val services = packageManager.queryIntentServices(
                android.content.Intent(android.service.dreams.DreamService.SERVICE_INTERFACE), 0
            )
            services.isNotEmpty()
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "DreamService not supported: ${e.message}")
            false
        }
    }

    private fun updateActivationCardVisibility() {
        val dreamSupported = isDreamServiceSupported()
        if (!dreamSupported) {
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
        updateActivationCardVisibility()
        refreshSourceCards()
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
            R.id.action_test_screensaver -> { launchScreensaver(); true }
            R.id.action_settings -> {
                startActivity(android.content.Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSnackbar(message: String) {
        val rootView = if (isTvLayout) bindingTv?.root else binding?.root
        rootView?.let { Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show() }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private data class CardInfo(
        val card: MaterialCardView,
        val titleView: TextView,
        val statusText: TextView,
        val switchView: MaterialSwitch
    )
}
