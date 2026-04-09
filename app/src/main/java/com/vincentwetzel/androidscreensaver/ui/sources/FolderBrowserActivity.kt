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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.FolderError.Companion.userMessage
import com.vincentwetzel.androidscreensaver.databinding.ActivityFolderBrowserBinding
import com.vincentwetzel.androidscreensaver.viewmodel.GoogleDriveViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Folder Browser Activity
 * Allows user to select which Google Drive folders to include in the screensaver
 */
@AndroidEntryPoint
class FolderBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFolderBrowserBinding
    private val viewModel: GoogleDriveViewModel by viewModels()
    private lateinit var adapter: FolderAdapter

    companion object {
        const val EXTRA_SELECTED_FOLDERS = "selected_folders"
        const val RESULT_SELECTED_FOLDERS = "selected_folders_result"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        observeViewModel()

        // Handle system back button — navigate to previous folder, or finish if none
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val didNavigate = viewModel.navigateBack()
                if (!didNavigate) {
                    // No history left — finish and return to main menu
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Clear back stack on fresh start, then load root folders
        viewModel.clearNavigationBackStack()
        val mediaFilter = getContentFilter()
        viewModel.loadFolders(parentFolderId = null, forceRefresh = false, addToBackStack = false, mediaFilter = mediaFilter)
    }

    /**
     * Get the current content filter setting and map it to a string for the repository.
     */
    private fun getContentFilter(): String? {
        val config = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSlideshowConfig(this)
        return when (config.mediaTypeFilter) {
            com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY -> "images"
            com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY -> "videos"
            else -> null // BOTH
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            val didNavigate = viewModel.navigateBack()
            if (!didNavigate) {
                // No history left — return to main menu
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        val mediaFilter = getContentFilter()
        adapter = FolderAdapter(
            onSelectionChanged = { selectedIds ->
                // Auto-save selections immediately
                com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSelectedFolders(
                    this,
                    com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE,
                    selectedIds
                )
                updateSummary(selectedIds.size, viewModel.getPhotoCount())
            },
            onFolderClick = { folderId ->
                viewModel.navigateToFolder(folderId)
            },
            onDeselectionChanged = { deselectedIds ->
                // Auto-save deselections immediately
                com.vincentwetzel.androidscreensaver.utils.SettingsManager.setDeselectedFolders(
                    this,
                    com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE,
                    deselectedIds
                )
            },
            onFolderChecked = { folderId, isChecked ->
                // Cascade: fetch subfolder IDs and apply cascade
                lifecycleScope.launch {
                    val childIds = viewModel.getSubfolderIds(folderId).toSet()
                    if (childIds.isNotEmpty()) {
                        adapter.cascadeSelection(folderId, isChecked, childIds)
                        // Re-persist selections and deselections after cascade
                        com.vincentwetzel.androidscreensaver.utils.SettingsManager.setSelectedFolders(
                            this@FolderBrowserActivity,
                            com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE,
                            adapter.getSelectedFolders()
                        )
                        com.vincentwetzel.androidscreensaver.utils.SettingsManager.setDeselectedFolders(
                            this@FolderBrowserActivity,
                            com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE,
                            adapter.getDeselectedFolders()
                        )
                        updateSummary(adapter.getSelectedFolders().size, viewModel.getPhotoCount())
                    }
                }
            },
            mediaFilter = mediaFilter
        )

        binding.recyclerFolders.layoutManager = LinearLayoutManager(this)
        binding.recyclerFolders.adapter = adapter

        // Restore previously saved folder selections
        restoreSelectedFolders()
    }

    private fun restoreSelectedFolders() {
        val savedFolders = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSelectedFolders(
            this,
            com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE
        )
        val savedFolderIds = savedFolders.map { it.id }.toSet()
        adapter.setSelectedFolders(savedFolderIds)

        // Restore deselected folders
        val deselectedIds = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getDeselectedFolders(
            this,
            com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE
        )
        adapter.setDeselectedFolders(deselectedIds)
    }

    private fun setupButtons() {
        // Pull-to-refresh for force reload
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadFolders(forceRefresh = true)
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
            viewModel.folders.collectLatest { folders ->
                if (folders.isEmpty()) {
                    binding.recyclerFolders.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                } else {
                    binding.recyclerFolders.visibility = View.VISIBLE
                    binding.emptyState.visibility = View.GONE
                    adapter.submitList(folders)
                    // Restore selected folders after list is submitted
                    restoreSelectedFolders()
                    // Set parent folder context for auto-checking subfolders
                    adapter.setCurrentParentFolderId(viewModel.currentFolderId.value)
                    // Update title to show current folder name
                    val currentFolder = folders.find { it.id == viewModel.currentFolderId.value }
                    supportActionBar?.title = currentFolder?.name ?: "Browse Folders"
                }
            }
        }

        lifecycleScope.launch {
            viewModel.currentFolderId.collectLatest { folderId ->
                supportActionBar?.title = if (folderId != null) {
                    "📁 Subfolder"
                } else {
                    "Browse Folders"
                }
                adapter.setCurrentParentFolderId(folderId)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.swipeRefresh.isRefreshing = isLoading
            }
        }

        lifecycleScope.launch {
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

    private fun updateSummary(folderCount: Int, itemCount: Int) {
        val label = when (getContentFilter()) {
            "images" -> "photos"
            "videos" -> "videos"
            else -> "items"
        }
        binding.summaryText.text = "$folderCount folders selected, $itemCount $label"
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
                    viewModel.loadFolders()
                }
                return true
            }
        })

        return true
    }
}
