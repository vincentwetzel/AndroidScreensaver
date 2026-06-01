package com.vincentwetzel.androidscreensaver.ui.main

import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.databinding.ActivityMainBinding
import com.vincentwetzel.androidscreensaver.databinding.ActivityMainTvBinding
import com.vincentwetzel.androidscreensaver.data.model.AccountConfig
import com.vincentwetzel.androidscreensaver.data.model.SelectedFolder
import com.vincentwetzel.androidscreensaver.ui.settings.SettingsActivity
import com.vincentwetzel.androidscreensaver.ui.sources.FolderBrowserActivity
import com.vincentwetzel.androidscreensaver.ui.sources.GalleryFolderBrowserActivity
import kotlinx.coroutines.launch
import com.vincentwetzel.androidscreensaver.ui.sources.GoogleDriveAuthActivity
import com.vincentwetzel.androidscreensaver.utils.GoogleAccountManager
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import com.vincentwetzel.androidscreensaver.viewmodel.MainViewModel
import com.vincentwetzel.androidscreensaver.data.repository.GalleryPhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDrivePhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.DropboxPhotoRepository
import com.vincentwetzel.androidscreensaver.utils.DropboxAccountManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity - Entry point of the app
 * Automatically selects TV or phone/tablet layout based on device type
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var galleryPhotoRepository: GalleryPhotoRepository
    @Inject lateinit var googleDrivePhotoRepository: GoogleDrivePhotoRepository
    @Inject lateinit var googleAccountManager: GoogleAccountManager
    @Inject lateinit var dropboxPhotoRepository: DropboxPhotoRepository
    @Inject lateinit var dropboxAccountManager: DropboxAccountManager

    private var binding: ActivityMainBinding? = null
    private var bindingTv: ActivityMainTvBinding? = null

    private val viewModel: MainViewModel by viewModels()
    private var isTvLayout = false
    private var isRestoringToggleState = false

    // Direct references to source card views for reliable refresh
    // Source card tracking - now supports multiple cards per source type
    private data class SourceCardData(
        val cardView: View,
        val info: CardInfo,
        val sourceType: SourceType,
        val accountId: String? = null
    )
    private val sourceCards = mutableListOf<SourceCardData>()

    // Activity result launcher for Google Drive auth
    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch {
                val accountName = result.data?.getStringExtra(GoogleDriveAuthActivity.EXTRA_ACCOUNT_NAME)
                val accountId = result.data?.getStringExtra("account_id")

                if (accountId != null) {
                    buildSourceCards() // Rebuild cards with new account
                    refreshSourceCards()
                    
                    showSnackbar("Google Drive connected!")
                    openFolderBrowser(SourceType.GOOGLE_DRIVE, accountId)
                }
            }
        }
    }

    // Activity result launcher for Dropbox auth
    private val dropboxAuthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch {
                val accountEmail = result.data?.getStringExtra("account_name") ?: "Unknown"
                val accountId = result.data?.getStringExtra("account_id")

                if (accountId != null) {
                    buildSourceCards()
                    refreshSourceCards()
                    
                    showSnackbar("Dropbox connected!")
                    openFolderBrowser(SourceType.DROPBOX, accountId)
                }
            }
        }
    }

    private val folderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Selections are auto-saved in the folder browser
        showSnackbar("Folders updated")
    }

    private val galleryFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Selections are auto-saved in the folder browser
        showSnackbar("Folders updated")
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
    }

    private suspend fun buildSourceCards() {
        // Get all accounts from settings first (suspending) to prevent concurrent modification exceptions
        val gdriveAccounts = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getAccountsForSource(
            this, com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE
        )
        val galleryAccounts = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getAccountsForSource(
            this, com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY
        )
        val dropboxAccounts = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getAccountsForSource(
            this, com.vincentwetzel.androidscreensaver.dream.SourceType.DROPBOX
        )

        val container = if (isTvLayout) bindingTv?.connectedSourcesContainer else binding?.connectedSourcesContainer
        container?.let { c ->
            c.removeAllViews()
            sourceCards.clear()

            val padding = if (isTvLayout) 32 else 16
            val iconSize = if (isTvLayout) 72 else 40
            val switchScale = if (isTvLayout) 1.5f else 1f

            // Gallery account cards
            galleryAccounts.forEach { account ->
                val (card, info) = createSourceCard(
                    "Gallery",
                    if (account.isAuthenticated) "Signed in" else getString(R.string.not_authenticated),
                    R.drawable.ic_source_gallery,
                    padding, iconSize, switchScale, SourceType.GALLERY,
                    accountId = account.accountId
                )
                c.addView(card)
                sourceCards.add(SourceCardData(card, info, SourceType.GALLERY, account.accountId))
            }

            // Google Drive account cards
            gdriveAccounts.forEach { account ->
                val (card, info) = createSourceCard(
                    "Google Drive",
                    account.accountEmail,
                    R.drawable.ic_source_google_drive,
                    padding, iconSize, switchScale, SourceType.GOOGLE_DRIVE,
                    accountId = account.accountId
                )
                c.addView(card)
                sourceCards.add(SourceCardData(card, info, SourceType.GOOGLE_DRIVE, account.accountId))
            }
            
            // Dropbox account cards
            dropboxAccounts.forEach { account ->
                // Fallback to android standard icon if ic_source_dropbox hasn't been created yet
                val iconRes = resources.getIdentifier("ic_source_dropbox", "drawable", packageName).takeIf { it != 0 } ?: android.R.drawable.ic_menu_gallery
                val (card, info) = createSourceCard(
                    "Dropbox",
                    account.accountEmail,
                    iconRes,
                    padding, iconSize, switchScale, SourceType.DROPBOX,
                    accountId = account.accountId
                )
                c.addView(card)
                sourceCards.add(SourceCardData(card, info, SourceType.DROPBOX, account.accountId))
            }
        }
    }

    private fun createSourceCard(
        name: String,
        initialSubtitle: String,
        iconRes: Int,
        padding: Int,
        iconSize: Int,
        switchScale: Float,
        sourceType: SourceType,
        accountId: String? = null
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

        val statusIndicator = View(this).apply {
            val size = dpToPx(8)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = dpToPx(8)
                marginEnd = dpToPx(8)
            }
            setBackgroundResource(R.drawable.bg_status_indicator)
            visibility = View.GONE // Hidden until status is known
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

        val photoCountText = TextView(this).apply {
            text = ""
            textSize = if (isTvLayout) 13f else 12f
            if (isTvLayout) {
                setTextColor(android.graphics.Color.parseColor("#80CBC4"))
            } else {
                setTextColor(android.graphics.Color.parseColor("#1976D2"))
            }
            visibility = View.GONE // Hidden until photo count is available
        }

        textContainer.addView(title)
        textContainer.addView(statusText)
        textContainer.addView(photoCountText)

        val menuButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_more) // Fallback standard icon
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dpToPx(48), dpToPx(48)).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            contentDescription = "More options"
        }

        val switchView = MaterialSwitch(this).apply {
            if (switchScale > 1f) { scaleX = switchScale; scaleY = switchScale }
            setOnCheckedChangeListener { _, isChecked ->
                if (isRestoringToggleState) return@setOnCheckedChangeListener
                val dreamSourceType = sourceType.toDreamSourceType()
                lifecycleScope.launch {
                    handleSourceToggle(dreamSourceType, isChecked, accountId)
                }
            }
        }

        row.addView(icon)
        row.addView(statusIndicator)
        row.addView(textContainer)
        row.addView(switchView)
        row.addView(menuButton)
        card.addView(row)

        card.setOnClickListener {
            lifecycleScope.launch {
                when (sourceType) {
                    SourceType.GALLERY -> {
                        val account = accountId?.let { id ->
                            com.vincentwetzel.androidscreensaver.utils.SettingsManager.getAccount(
                                this@MainActivity,
                                com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY,
                                id
                            )
                        }
                        if (account?.isAuthenticated == true) openGalleryFolderBrowser(accountId)
                    }
                    SourceType.GOOGLE_DRIVE -> {
                        val account = accountId?.let { id ->
                            com.vincentwetzel.androidscreensaver.utils.SettingsManager.getAccount(
                                this@MainActivity,
                                com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE,
                                id
                            )
                        }
                        if (account?.isAuthenticated == true) openFolderBrowser(SourceType.GOOGLE_DRIVE, accountId)
                        else startGoogleDriveAuth(accountId)
                    }
                    SourceType.DROPBOX -> {
                        val account = accountId?.let { id ->
                            com.vincentwetzel.androidscreensaver.utils.SettingsManager.getAccount(
                                this@MainActivity,
                                com.vincentwetzel.androidscreensaver.dream.SourceType.DROPBOX,
                                id
                            )
                        }
                        if (account?.isAuthenticated == true) openFolderBrowser(SourceType.DROPBOX, accountId)
                        else startDropboxAuth(accountId)
                    }
                    else -> {}
                }
            }
        }

        menuButton.setOnClickListener { view ->
            lifecycleScope.launch {
                val account = accountId?.let { id ->
                    val dreamSourceType = sourceType.toDreamSourceType()
                    SettingsManager.getAccount(this@MainActivity, dreamSourceType, id)
                }
                showAccountMenu(view, sourceType, accountId, account?.accountEmail ?: "this account")
            }
        }

        val cardInfo = CardInfo(card, title, statusText, photoCountText, statusIndicator, switchView, menuButton)
        return card to cardInfo
    }

    private fun showAccountMenu(anchorView: View, sourceType: SourceType, accountId: String?, email: String) {
        if (accountId == null) return

        val popup = PopupMenu(this, anchorView)
        
        if (sourceType == SourceType.GOOGLE_DRIVE || sourceType == SourceType.DROPBOX) {
            popup.menu.add(0, 1, 0, "Re-authenticate")
        }
        
        popup.menu.add(0, 2, 0, "Remove account")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    when (sourceType) {
                        SourceType.GOOGLE_DRIVE -> startGoogleDriveAuth(accountId)
                        SourceType.DROPBOX -> startDropboxAuth(accountId)
                        else -> {}
                    }
                }
                2 -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Remove Account?")
                        .setMessage("Are you sure you want to remove the account '$email'? This action cannot be undone.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove") { _, _ ->
                            lifecycleScope.launch {
                                removeAccount(sourceType, accountId, email)
                            }
                        }
                        .show()
                }
            }
            true
        }
        popup.show()
    }

    private suspend fun removeAccount(sourceType: SourceType, accountId: String, email: String) {
        val dreamSourceType = sourceType.toDreamSourceType()

        // Remove from SettingsManager
        SettingsManager.removeAccount(this, dreamSourceType, accountId)

        // If it's a Google Drive account, also sign it out from the manager
        if (sourceType == SourceType.GOOGLE_DRIVE) {
            googleAccountManager.signOutAccount(accountId)
        }
        // If it's a Dropbox account, sign it out from its manager
        if (sourceType == SourceType.DROPBOX) {
            dropboxAccountManager.signOutAccount(accountId)
        }

        // Refresh UI
        buildSourceCards()
        refreshSourceCards()

        showSnackbar("Account '$email' removed.")
    }

    private suspend fun handleSourceToggle(dreamSourceType: com.vincentwetzel.androidscreensaver.dream.SourceType, isChecked: Boolean, accountId: String?) {
        if (accountId == null) return
        
        if (isChecked) {
            val account = SettingsManager.getAccount(this, dreamSourceType, accountId)
            account?.copy(enabled = true)?.let { SettingsManager.saveAccount(this, it) }
            showSnackbar("${account?.accountEmail ?: dreamSourceType.name} enabled")
        } else {
            val account = SettingsManager.getAccount(this, dreamSourceType, accountId)
            account?.copy(enabled = false)?.let { SettingsManager.saveAccount(this, it) }
            showSnackbar("${account?.accountEmail ?: dreamSourceType.name} disabled")
        }
    }

    private suspend fun refreshSourceCards() {
        isRestoringToggleState = true

        sourceCards.toList().forEach { cardData ->
            val account = cardData.accountId?.let { id ->
                val dreamType = cardData.sourceType.toDreamSourceType()
                SettingsManager.getAccount(this, dreamType, id)
            }

            // Update switch state from account's enabled flag
            cardData.info.switchView.isChecked = account?.enabled == true

            if (account?.isAuthenticated == true) {
                cardData.info.statusIndicator.visibility = View.VISIBLE
                setStatusIndicatorColor(cardData.info.statusIndicator, SourceStatus.CONNECTED)
                cardData.info.statusText.text = account.accountEmail

                val label = getContentFilterLabel()
                val photoCount = account.photoCount
                
                cardData.info.photoCountText.visibility = if (photoCount > 0) View.VISIBLE else View.GONE
                cardData.info.photoCountText.text = if (photoCount > 0) "$photoCount $label available" else ""

                // Pre-fetch root folders in the background to warm the cache so the browser opens instantly
                if (account.enabled) {
                    cardData.accountId?.let { id ->
                        val filter = getContentFilterFilter()
                        when (cardData.sourceType) {
                            SourceType.GOOGLE_DRIVE -> googleDrivePhotoRepository.prefetchRootFolders(id, filter)
                            SourceType.GALLERY -> galleryPhotoRepository.prefetchRootFolders(filter)
                            SourceType.DROPBOX -> dropboxPhotoRepository.prefetchRootFolders(id, filter)
                            else -> {}
                        }
                    }
                }
            } else {
                cardData.info.statusIndicator.visibility = View.VISIBLE
                setStatusIndicatorColor(cardData.info.statusIndicator, SourceStatus.ERROR)
                cardData.info.statusText.text = account?.accountEmail ?: getString(R.string.not_authenticated)
                cardData.info.photoCountText.visibility = View.GONE
            }
        }

        isRestoringToggleState = false
    }

    private enum class SourceStatus {
        CONNECTED, SYNCING, ERROR
    }

    private fun setStatusIndicatorColor(indicator: View?, status: SourceStatus) {
        indicator?.let {
            val colorRes = when (status) {
                SourceStatus.CONNECTED -> R.color.status_connected
                SourceStatus.SYNCING -> R.color.status_syncing
                SourceStatus.ERROR -> R.color.status_error
            }
            val color = getColor(colorRes)
            it.background?.setTint(color)
        }
    }

    /**
     * Get the content filter as a string for the repository: "images", "videos", or null for both
     */
    private suspend fun getContentFilterFilter(): String? {
        val config = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSlideshowConfig(this)
        return when (config.mediaTypeFilter) {
            com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY -> "images"
            com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY -> "videos"
            else -> null // BOTH
        }
    }

    private fun showAddSourcesDialog() {
        lifecycleScope.launch {
            val sources = mutableListOf<Pair<String, SourceType>>()

            // Gallery is a singleton source, only show it if it hasn't been added yet
            val galleryAccounts = SettingsManager.getAccountsForSource(
                this@MainActivity, com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY
            )
            if (galleryAccounts.isEmpty()) {
                sources.add("Gallery" to SourceType.GALLERY)
            }

            // Google Drive supports multiple accounts, always show it
            sources.add("Google Drive" to SourceType.GOOGLE_DRIVE)
            
            // Dropbox supports multiple accounts, always show it
            sources.add("Dropbox" to SourceType.DROPBOX)

            val sourceNames = sources.map { it.first }.toTypedArray()
            var selectedIndex = 0

            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("Select source to add")
                .setSingleChoiceItems(sourceNames, selectedIndex) { _, which -> selectedIndex = which }
                .setPositiveButton("Add") { _, _ ->
                    if (selectedIndex >= 0 && selectedIndex < sources.size) {
                        lifecycleScope.launch { handleAddSource(sources[selectedIndex].second) }
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private suspend fun handleAddSource(sourceType: SourceType) {
        when (sourceType) {
            SourceType.GALLERY -> {
                // Gallery is a singleton source, only add if it doesn't exist.
                val galleryAccounts = SettingsManager.getAccountsForSource(this, com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY)
                if (galleryAccounts.isEmpty()) {
                    val accountId = "gallery:${System.currentTimeMillis()}"
                    val account = AccountConfig(
                        accountId = accountId,
                        sourceType = com.vincentwetzel.androidscreensaver.data.model.SourceType.GALLERY,
                        accountEmail = "Device Photos",
                        enabled = true,
                        isAuthenticated = true // Gallery doesn't need auth
                    )
                    SettingsManager.saveAccount(this, account)
                    buildSourceCards()
                    refreshSourceCards()
                    showSnackbar("Gallery source added")
                } else {
                    showSnackbar("Gallery source already exists.")
                }
            }
            SourceType.GOOGLE_DRIVE -> {
                startGoogleDriveAuth()
            }
            SourceType.DROPBOX -> {
                startDropboxAuth()
            }
            else -> {}
        }
    }

    private fun setupAnimations() {
        val headerView = if (isTvLayout) bindingTv?.tvHeader else binding?.tvHeader
        headerView?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(500)
            ?.setInterpolator(android.view.animation.DecelerateInterpolator())?.start()
    }

    /**
     * Get the content filter label based on settings: "photos", "videos", or "items"
     */
    private suspend fun getContentFilterLabel(): String {
        val config = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSlideshowConfig(this)
        return when (config.mediaTypeFilter) {
            com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY -> "photos"
            com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY -> "videos"
            else -> "items"
        }
    }

    private fun updateSourceStatus(sourceType: SourceType, isAuthenticated: Boolean, accountName: String? = null, accountId: String? = null) {
        val displayText = if (isAuthenticated) {
            accountName?.let { "Signed in as $it" } ?: "Signed in as Unknown"
        } else {
            getString(R.string.not_authenticated)
        }
        
        // Update the specific card if accountId is provided, otherwise update all Drive cards
        if (accountId != null) {
            sourceCards.find { it.accountId == accountId && it.sourceType == sourceType }
                ?.info?.statusText?.text = displayText
        } else {
            sourceCards.filter { it.sourceType == sourceType }
                .forEach { it.info.statusText.text = displayText }
        }
    }

    private fun startGoogleDriveAuth(existingAccountId: String? = null) {
        val intent = android.content.Intent(this, GoogleDriveAuthActivity::class.java).apply {
            existingAccountId?.let { putExtra("account_id", it) }
        }
        authLauncher.launch(intent)
    }

    private fun startDropboxAuth(existingAccountId: String? = null) {
        val intent = android.content.Intent(this, com.vincentwetzel.androidscreensaver.ui.sources.DropboxAuthActivity::class.java).apply {
            existingAccountId?.let { putExtra("account_id", it) }
        }
        dropboxAuthLauncher.launch(intent)
    }

    private fun openFolderBrowser(sourceType: SourceType, accountId: String? = null) {
        val intent = android.content.Intent(this, FolderBrowserActivity::class.java).apply {
            val dreamType = sourceType.toDreamSourceType()
            putExtra(FolderBrowserActivity.EXTRA_SOURCE_TYPE, dreamType.name)
            accountId?.let { putExtra(FolderBrowserActivity.EXTRA_ACCOUNT_ID, it) }
        }
        folderLauncher.launch(intent)
    }

    private fun openGalleryFolderBrowser(accountId: String? = null) {
        val intent = android.content.Intent(this, GalleryFolderBrowserActivity::class.java).apply {
            accountId?.let { putExtra("account_id", it) }
        }
        galleryFolderLauncher.launch(intent)
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
        lifecycleScope.launch {
            buildSourceCards() // Forces the views to rebuild and grab any newly migrated Account IDs
            refreshSourceCards()
        }
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
        val photoCountText: TextView,
        val statusIndicator: View,
        val switchView: MaterialSwitch,
        val menuButton: ImageButton
    )

}
