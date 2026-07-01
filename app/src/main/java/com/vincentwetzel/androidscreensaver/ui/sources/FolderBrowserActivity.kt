package com.vincentwetzel.androidscreensaver.ui.sources

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.databinding.ActivityFolderBrowserBinding
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import com.vincentwetzel.androidscreensaver.viewmodel.CloudFolderViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Folder Browser Activity
 * Allows user to select which Google Drive folders to include in the screensaver.
 * Now supports per-account browsing via accountId intent extra.
 */
@AndroidEntryPoint
class FolderBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFolderBrowserBinding
    private val viewModel: CloudFolderViewModel by viewModels()
    private lateinit var adapter: FolderAdapter
    private var accountId: String? = null
    private var dreamSourceType: com.vincentwetzel.androidscreensaver.dream.SourceType? = null
    private var reauthButton: com.google.android.material.button.MaterialButton? = null

    companion object {
        const val EXTRA_SOURCE_TYPE = "source_type"
        const val EXTRA_ACCOUNT_ID = "account_id"
    }

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch {
                reauthButton?.visibility = View.GONE

                // Update the account ID if the auth flow generated a new one
                val returnedAccountId = result.data?.getStringExtra(EXTRA_ACCOUNT_ID)
                if (returnedAccountId != null && returnedAccountId != accountId) {
                    accountId = returnedAccountId
                    intent.putExtra(EXTRA_ACCOUNT_ID, accountId) // Save for recreation
                    viewModel.setSourceContext(dreamSourceType!!.toModelSourceType(), accountId!!)
                }

                // Re-enable the account's authenticated state in settings so the repository allows the request
                if (accountId != null && dreamSourceType != null) {
                    val account = SettingsManager.getAccount(this@FolderBrowserActivity, dreamSourceType!!, accountId!!)
                    account?.let {
                        val updated = it.copy(
                            isAuthenticated = true,
                            lastAuthTime = System.currentTimeMillis()
                        )
                        SettingsManager.saveAccount(this@FolderBrowserActivity, updated)
                    }
                }

                // Wait a moment for the updated DataStore state to propagate to the repository's internal flows.
                // Without this delay, the repository might instantly reject the loadFolders request 
                // because its cached account state still reads isAuthenticated = false.
                kotlinx.coroutines.delay(600)

                viewModel.clearError()

                val mediaFilter = getContentFilter()
                viewModel.loadFolders(parentFolderId = viewModel.currentFolderId.value, forceRefresh = true, addToBackStack = false, mediaFilter = mediaFilter)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get accountId from intent
        accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)

        // Initialize dreamSourceType from intent
        val sourceTypeName = intent.getStringExtra(EXTRA_SOURCE_TYPE)
        if (sourceTypeName != null) {
            try {
                dreamSourceType = com.vincentwetzel.androidscreensaver.dream.SourceType.valueOf(sourceTypeName)
            } catch (e: IllegalArgumentException) {
                android.util.Log.e("FolderBrowserActivity", "Invalid source type extra")
            }
        }

        if (dreamSourceType == null || accountId == null) {
            android.util.Log.e("FolderBrowserActivity", "Required context missing. Closing browser.")
            android.widget.Toast.makeText(this, "Error: Account ID or Source Type missing", android.widget.Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()

        injectReauthButton()

        // Configure ViewModel with sourceType and accountId
        viewModel.setSourceContext(dreamSourceType!!.toModelSourceType(), accountId!!)

        // Handle system back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val didNavigate = viewModel.navigateBack()
                if (!didNavigate) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        lifecycleScope.launch {
            val mediaFilter = getContentFilter()
            
            setupRecyclerView(mediaFilter)
            setupButtons()
            observeViewModel()
            
            // Clear back stack on fresh start, then load root folders
            viewModel.clearNavigationBackStack()
            viewModel.loadFolders(parentFolderId = null, forceRefresh = false, addToBackStack = false, mediaFilter = mediaFilter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        reauthButton = null
    }

    private fun injectReauthButton() {
        val root = binding.root as? android.view.ViewGroup ?: return
        reauthButton = com.google.android.material.button.MaterialButton(this).apply {
            text = "Re-authenticate"
            visibility = View.GONE
            filterTouchesWhenObscured = true
            setOnClickListener {
                startReauthFlow()
            }
        }

        // Try adding it to emptyState first to keep it clustered with the empty text
        val emptyStateGroup = binding.emptyState as? android.view.ViewGroup
        if (emptyStateGroup is android.widget.LinearLayout) {
            val params = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                topMargin = (24 * resources.displayMetrics.density).toInt()
            }
            emptyStateGroup.addView(reauthButton, params)
        } else {
            // Fallback to center of root layout if emptyState is not a ViewGroup
            val params = when (root) {
                is androidx.constraintlayout.widget.ConstraintLayout -> {
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                        topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                        startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                        endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    }
                }
                is android.widget.FrameLayout -> {
                    android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { gravity = android.view.Gravity.CENTER }
                }
                is android.widget.RelativeLayout -> {
                    android.widget.RelativeLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { addRule(android.widget.RelativeLayout.CENTER_IN_PARENT, android.widget.RelativeLayout.TRUE) }
                }
                else -> android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            root.addView(reauthButton, params)
        }
    }

    private suspend fun getContentFilter(): MediaTypeFilter {
        val config = SettingsManager.getSlideshowConfig(this)
        // The ViewModel now expects MediaTypeFilter directly, so we can pass it as is.
        return config.mediaTypeFilter
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            val didNavigate = viewModel.navigateBack()
            if (!didNavigate) finish()
        }
    }

    private suspend fun setupRecyclerView(mediaFilter: MediaTypeFilter?) {
        adapter = FolderAdapter(
            onSelectionStateChanged = { selectedIds, deselectedIds ->
                // Capture synchronous state to prevent corruption if the user navigates
                // before the coroutine resumes from reading DataStore.
                val visibleFolders = adapter.getVisibleFolders().associateBy { f -> f.id }
                val photoCount = adapter.getPhotoCount()
                
                lifecycleScope.launch {
                    if (accountId != null && dreamSourceType != null) {
                        val account = SettingsManager.getAccount(this@FolderBrowserActivity, dreamSourceType!!, accountId!!)
                        account?.let {
                            val existingFolders = it.selectedFolders.associateBy { sf -> sf.folderId }
                            val updated = it.copy(
                                selectedFolders = selectedIds.map { folderId ->
                                    val visible = visibleFolders[folderId]
                                    val existing = existingFolders[folderId]
                                    com.vincentwetzel.androidscreensaver.data.model.SelectedFolder(
                                        folderId = folderId,
                                        folderName = visible?.name ?: existing?.folderName ?: folderId,
                                        path = existing?.path ?: folderId,
                                        isSelected = true
                                    )
                                },
                                deselectedFolders = deselectedIds
                            )
                            SettingsManager.saveAccount(this@FolderBrowserActivity, updated)
                        }
                    }
                    updateSummary(selectedIds.size, photoCount)
                }
            },
            onFolderClick = { folderId ->
                viewModel.navigateToFolder(folderId)
            },
            mediaFilter = mediaFilter
        )

        binding.recyclerFolders.layoutManager = LinearLayoutManager(this@FolderBrowserActivity)
        binding.recyclerFolders.adapter = adapter

        restoreSelectedFolders()
    }

    private suspend fun restoreSelectedFolders() {
        val account = if (accountId != null && dreamSourceType != null) {
            SettingsManager.getAccount(this, dreamSourceType!!, accountId!!)
        } else {
            null
        }
        val savedFolderIds = account?.selectedFolders?.map { it.folderId }?.toSet() ?: emptySet()
        adapter.setSelectedFolders(savedFolderIds)

        val deselectedIds = account?.deselectedFolders ?: emptySet()
        adapter.setDeselectedFolders(deselectedIds)
    }

    private fun setupButtons() {
        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                val mediaFilter = getContentFilter()
                viewModel.loadFolders(parentFolderId = viewModel.currentFolderId.value, forceRefresh = true, addToBackStack = false, mediaFilter = mediaFilter)
            }
        }

        binding.btnSelectAll.setOnClickListener {
            adapter.selectAll()
        }

        binding.btnDeselectAll.setOnClickListener {
            adapter.deselectAll()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.folders.collectLatest { folders ->
                        if (folders.isEmpty()) {
                            binding.recyclerFolders.visibility = View.GONE
                            binding.emptyState.visibility = View.VISIBLE
                        } else {
                            binding.recyclerFolders.visibility = View.VISIBLE
                            binding.emptyState.visibility = View.GONE
                            reauthButton?.visibility = View.GONE
                            adapter.submitList(folders)
                            updateSummary(adapter.getSelectedFolders().size, adapter.getPhotoCount())
                            adapter.setCurrentParentFolderId(viewModel.currentFolderId.value)
                        }
                    }
                }

                launch {
                    viewModel.currentFolderId.collectLatest { folderId ->
                        supportActionBar?.title = if (folderId != null) "📁 Subfolder" else "Browse Folders"
                        adapter.setCurrentParentFolderId(folderId)
                    }
                }

                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.swipeRefresh.isRefreshing = isLoading
                    }
                }

                launch {
                    viewModel.error.collectLatest { error ->
                        error?.let {
                            binding.swipeRefresh.isRefreshing = false

                            // Spawns the auth button if the error is related to authentication
                            val isAuthError = it is com.vincentwetzel.androidscreensaver.data.model.FolderError.AuthError

                            if (isAuthError && binding.recyclerFolders.visibility == View.GONE) {
                                reauthButton?.visibility = View.VISIBLE
                            }

                            com.google.android.material.snackbar.Snackbar.make(
                                binding.root,
                                it.userMessage(),
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                            ).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateSummary(folderCount: Int, itemCount: Int) {
        if (itemCount > 0) {
            val label = when (getContentFilter()) { // getContentFilter() now returns MediaTypeFilter
                MediaTypeFilter.IMAGES_ONLY -> "photos"
                MediaTypeFilter.VIDEOS_ONLY -> "videos"
                else -> "items"
            }
            binding.summaryText.text = "$folderCount folders selected, $itemCount $label"
        } else {
            binding.summaryText.text = "$folderCount folders selected"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_folder_browser, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchFolders(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    lifecycleScope.launch {
                        val mediaFilter = getContentFilter()
                        viewModel.loadFolders(parentFolderId = viewModel.currentFolderId.value, addToBackStack = false, mediaFilter = mediaFilter)
                    }
                }
                return true
            }
        })

        if (dreamSourceType == com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE ||
            dreamSourceType == com.vincentwetzel.androidscreensaver.dream.SourceType.ONEDRIVE ||
            dreamSourceType == com.vincentwetzel.androidscreensaver.dream.SourceType.DROPBOX) {
            menu.add(Menu.NONE, 1001, Menu.NONE, "Re-authenticate").apply {
                setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1001 -> {
                startReauthFlow()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startReauthFlow() {
        val intent = when (dreamSourceType) {
            com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE -> android.content.Intent(this, GoogleDriveAuthActivity::class.java)
            com.vincentwetzel.androidscreensaver.dream.SourceType.ONEDRIVE -> android.content.Intent(this, OneDriveAuthActivity::class.java)
            else -> android.content.Intent(this, DropboxAuthActivity::class.java)
        }.apply {
            putExtra(EXTRA_ACCOUNT_ID, accountId)
        }
        authLauncher.launch(intent)
    }

    private fun com.vincentwetzel.androidscreensaver.dream.SourceType.toModelSourceType(): SourceType {
        return when (this) {
            com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE -> SourceType.GOOGLE_DRIVE
            com.vincentwetzel.androidscreensaver.dream.SourceType.DROPBOX -> SourceType.DROPBOX
            com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY -> SourceType.GALLERY
            com.vincentwetzel.androidscreensaver.dream.SourceType.ONEDRIVE -> SourceType.ONEDRIVE
            else -> SourceType.GOOGLE_DRIVE
        }
    }
}
