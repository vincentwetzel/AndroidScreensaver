package com.vincentwetzel.androidscreensaver.ui.sources

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.FolderError.Companion.userMessage
import com.vincentwetzel.androidscreensaver.databinding.ActivityFolderBrowserBinding
import com.vincentwetzel.androidscreensaver.viewmodel.GalleryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Gallery Folder Browser Activity
 * Allows user to select which Gallery folders to include in the screensaver
 */
@AndroidEntryPoint
class GalleryFolderBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFolderBrowserBinding
    private val viewModel: GalleryViewModel by viewModels()
    private lateinit var adapter: FolderAdapter
    private var accountId: String? = null

    companion object {
        const val EXTRA_SELECTED_FOLDERS = "selected_folders"
        const val RESULT_SELECTED_FOLDERS = "selected_folders_result"
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.clearNavigationBackStack()
            val mediaFilter = getContentFilter()
            viewModel.loadFolders(parentFolderId = null, forceRefresh = true, addToBackStack = false, mediaFilter = mediaFilter)
        } else {
            Toast.makeText(this, "Photo permission is required to browse Gallery folders", Toast.LENGTH_LONG).show()
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        accountId = intent.getStringExtra("account_id")

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

        // Check and request permissions before loading folders
        if (hasGalleryPermissions()) {
            viewModel.clearNavigationBackStack()
            val mediaFilter = getContentFilter()
            viewModel.loadFolders(parentFolderId = null, forceRefresh = false, addToBackStack = false, mediaFilter = mediaFilter)
        } else {
            requestGalleryPermissions()
        }
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

    private fun hasGalleryPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestGalleryPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
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
                saveSelections()
                updateSummary(selectedIds.size, adapter.getPhotoCount())
            },
            onFolderClick = { folderId ->
                viewModel.navigateToFolder(folderId)
            },
            onDeselectionChanged = { deselectedIds ->
                saveSelections()
            },
            onFolderChecked = { folderId, isChecked ->
                // Cascade: fetch subfolder IDs and apply cascade
                lifecycleScope.launch {
                    val childIds = viewModel.getSubfolderIds(folderId).toSet()
                    if (childIds.isNotEmpty()) {
                        adapter.cascadeSelection(folderId, isChecked, childIds)
                        saveSelections()
                        updateSummary(adapter.getSelectedFolders().size, adapter.getPhotoCount())
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
        val account = accountId?.let {
            com.vincentwetzel.androidscreensaver.utils.SettingsManager.getAccount(
                this,
                com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY,
                it
            )
        }
        val savedFolderIds = account?.selectedFolders?.map { it.folderId }?.toSet() ?: emptySet()
        adapter.setSelectedFolders(savedFolderIds)

        // Restore deselected folders
        val deselectedIds = account?.deselectedFolders ?: emptySet()
        adapter.setDeselectedFolders(deselectedIds)
    }

    private fun setupButtons() {
        binding.btnSelectAll.setOnClickListener {
            adapter.selectAll()
        }

        binding.btnDeselectAll.setOnClickListener {
            adapter.deselectAll()
        }
    }

    private fun saveSelections() {
        val dreamSourceType = com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY
        accountId?.let { id ->
            val account = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getAccount(this, dreamSourceType, id)
            account?.let {
                val updated = it.copy(
                    selectedFolders = adapter.getSelectedFolders().map { folderId ->
                        com.vincentwetzel.androidscreensaver.data.model.SelectedFolder(
                            folderId = folderId, folderName = folderId, path = folderId, isSelected = true
                        )
                    },
                    deselectedFolders = adapter.getDeselectedFolders()
                )
                com.vincentwetzel.androidscreensaver.utils.SettingsManager.saveAccount(this, updated)
            }
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
                binding.progressBar.visibility = View.GONE
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
            }
        }

        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    binding.progressBar.visibility = View.GONE
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
