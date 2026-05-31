package com.vincentwetzel.androidscreensaver.ui.sources

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.FolderError.Companion.userMessage
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

    companion object {
        const val EXTRA_SOURCE_TYPE = "source_type"
        const val EXTRA_ACCOUNT_ID = "account_id"
        const val EXTRA_SELECTED_FOLDERS = "selected_folders"
        const val RESULT_SELECTED_FOLDERS = "selected_folders_result"
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

        if (accountId == null) {
            android.util.Log.e("FolderBrowserActivity", "accountId is missing from intent!")
            com.google.android.material.snackbar.Snackbar.make(binding.root, "Error: Account ID is missing", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
        }

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        observeViewModel()

        if (dreamSourceType == null || accountId == null) {
            android.util.Log.e("FolderBrowserActivity", "Required context missing. Closing browser.")
            finish()
            return
        }

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

        // Clear back stack on fresh start, then load root folders
        viewModel.clearNavigationBackStack()
        lifecycleScope.launch {
            val mediaFilter = getContentFilter()
            viewModel.loadFolders(parentFolderId = null, forceRefresh = false, addToBackStack = false, mediaFilter = mediaFilter)
        }
    }

    private suspend fun getContentFilter(): String? {
        val config = SettingsManager.getSlideshowConfig(this)
        return when (config.mediaTypeFilter) {
            com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY -> "images"
            com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY -> "videos"
            else -> null
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            val didNavigate = viewModel.navigateBack()
            if (!didNavigate) finish()
        }
    }

    private fun setupRecyclerView() {
        lifecycleScope.launch {
            val mediaFilter = getContentFilter()

            adapter = FolderAdapter(
                onSelectionStateChanged = { selectedIds, deselectedIds ->
                    lifecycleScope.launch {
                        if (accountId != null && dreamSourceType != null) {
                            val account = SettingsManager.getAccount(this@FolderBrowserActivity, dreamSourceType!!, accountId!!)
                            account?.let {
                                val updated = it.copy(
                                    selectedFolders = selectedIds.map { folderId ->
                                        com.vincentwetzel.androidscreensaver.data.model.SelectedFolder(
                                            folderId = folderId, folderName = folderId, path = folderId, isSelected = true
                                        )
                                    },
                                    deselectedFolders = deselectedIds
                                )
                                SettingsManager.saveAccount(this@FolderBrowserActivity, updated)
                            }
                        }
                        updateSummary(selectedIds.size, adapter.getPhotoCount())
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
                viewModel.loadFolders(forceRefresh = true, mediaFilter = mediaFilter)
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
                            adapter.submitList(folders)
                            restoreSelectedFolders()
                            updateSummary(adapter.getSelectedFolders().size, adapter.getPhotoCount())
                            adapter.setCurrentParentFolderId(viewModel.currentFolderId.value)
                            val currentFolder = folders.find { it.id == viewModel.currentFolderId.value }
                            supportActionBar?.title = currentFolder?.name ?: "Browse Folders"
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
            val label = when (getContentFilter()) {
                "images" -> "photos"
                "videos" -> "videos"
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
                        viewModel.loadFolders(parentFolderId = viewModel.currentFolderId.value, mediaFilter = mediaFilter)
                    }
                }
                return true
            }
        })

        return true
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
