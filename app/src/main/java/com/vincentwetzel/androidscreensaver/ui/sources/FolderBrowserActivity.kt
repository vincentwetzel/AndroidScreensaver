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
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.databinding.ActivityFolderBrowserBinding
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import com.vincentwetzel.androidscreensaver.viewmodel.GoogleDriveViewModel
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
    private val viewModel: GoogleDriveViewModel by viewModels()
    private lateinit var adapter: FolderAdapter
    private var accountId: String? = null

    companion object {
        const val EXTRA_SELECTED_FOLDERS = "selected_folders"
        const val RESULT_SELECTED_FOLDERS = "selected_folders_result"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get accountId from intent
        accountId = intent.getStringExtra("account_id")

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        observeViewModel()

        // Configure ViewModel with accountId
        accountId?.let { id ->
            viewModel.setAccountId(id)
        }

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
        val mediaFilter = getContentFilter()
        viewModel.loadFolders(parentFolderId = null, forceRefresh = false, addToBackStack = false, mediaFilter = mediaFilter)
    }

    private fun getContentFilter(): String? {
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
        val mediaFilter = getContentFilter()
        val dreamSourceType = com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE

        adapter = FolderAdapter(
            onSelectionChanged = { selectedIds ->
                accountId?.let { id ->
                    val account = SettingsManager.getAccount(this, dreamSourceType, id)
                    account?.let {
                        val updated = it.copy(
                            selectedFolders = selectedIds.map { folderId ->
                                com.vincentwetzel.androidscreensaver.data.model.SelectedFolder(
                                    folderId = folderId, folderName = folderId, path = folderId, isSelected = true
                                )
                            },
                            deselectedFolders = adapter.getDeselectedFolders()
                        )
                        SettingsManager.saveAccount(this, updated)
                    }
                }
                updateSummary(selectedIds.size, adapter.getPhotoCount())
            },
            onFolderClick = { folderId ->
                viewModel.navigateToFolder(folderId)
            },
            onDeselectionChanged = { deselectedIds ->
                accountId?.let { id ->
                    val account = SettingsManager.getAccount(this, dreamSourceType, id)
                    account?.let {
                        val updated = it.copy(
                            selectedFolders = adapter.getSelectedFolders().map { fId ->
                                com.vincentwetzel.androidscreensaver.data.model.SelectedFolder(
                                    folderId = fId, folderName = fId, path = fId, isSelected = true
                                )
                            },
                            deselectedFolders = deselectedIds
                        )
                        SettingsManager.saveAccount(this, updated)
                    }
                }
            },
            onFolderChecked = { folderId, isChecked ->
                lifecycleScope.launch {
                    val childIds = viewModel.getSubfolderIds(folderId).toSet()
                    if (childIds.isNotEmpty()) {
                        adapter.cascadeSelection(folderId, isChecked, childIds)
                        accountId?.let { id ->
                            val account = SettingsManager.getAccount(this@FolderBrowserActivity, dreamSourceType, id)
                            account?.let {
                                val updated = it.copy(
                                    selectedFolders = adapter.getSelectedFolders().map { fId ->
                                        com.vincentwetzel.androidscreensaver.data.model.SelectedFolder(
                                            folderId = fId, folderName = fId, path = fId, isSelected = true
                                        )
                                    },
                                    deselectedFolders = adapter.getDeselectedFolders()
                                )
                                SettingsManager.saveAccount(this@FolderBrowserActivity, updated)
                            }
                        }
                        updateSummary(adapter.getSelectedFolders().size, adapter.getPhotoCount())
                    }
                }
            },
            mediaFilter = mediaFilter
        )

        binding.recyclerFolders.layoutManager = LinearLayoutManager(this)
        binding.recyclerFolders.adapter = adapter

        restoreSelectedFolders()
    }

    private fun restoreSelectedFolders() {
        val account = accountId?.let {
            SettingsManager.getAccount(this, com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE, it)
        }
        val savedFolderIds = account?.selectedFolders?.map { it.folderId }?.toSet() ?: emptySet()
        adapter.setSelectedFolders(savedFolderIds)

        val deselectedIds = account?.deselectedFolders ?: emptySet()
        adapter.setDeselectedFolders(deselectedIds)
    }

    private fun setupButtons() {
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
                    restoreSelectedFolders()
                    adapter.setCurrentParentFolderId(viewModel.currentFolderId.value)
                    val currentFolder = folders.find { it.id == viewModel.currentFolderId.value }
                    supportActionBar?.title = currentFolder?.name ?: "Browse Folders"
                }
            }
        }

        lifecycleScope.launch {
            viewModel.currentFolderId.collectLatest { folderId ->
                supportActionBar?.title = if (folderId != null) "📁 Subfolder" else "Browse Folders"
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
