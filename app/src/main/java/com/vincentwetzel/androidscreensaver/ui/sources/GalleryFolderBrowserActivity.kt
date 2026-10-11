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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.databinding.ActivityFolderBrowserBinding
import com.vincentwetzel.androidscreensaver.viewmodel.GalleryViewModel
import com.vincentwetzel.androidscreensaver.utils.GalleryPermissionPolicy
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
    private var lastPermissionSnapshot: List<Boolean>? = null

    companion object {
        const val EXTRA_ACCOUNT_ID = "account_id"
        private const val MENU_CHANGE_SELECTED_MEDIA = 1001
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        invalidateOptionsMenu()
        lifecycleScope.launch {
            viewModel.invalidateGalleryCaches()
            lastPermissionSnapshot = readPermissionSnapshot()
            val mediaFilter = getContentFilter()
            if (hasGalleryPermissions(mediaFilter)) {
                viewModel.clearNavigationBackStack()
                viewModel.loadFolders(parentFolderId = null, forceRefresh = true, addToBackStack = false, mediaFilter = mediaFilter)
            } else {
                binding.progressBar.visibility = View.GONE
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    com.vincentwetzel.androidscreensaver.data.model.FolderError.PermissionError().userMessage(),
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
        lifecycleScope.launch {
            val currentSnapshot = readPermissionSnapshot()
            val selectedMediaAccess = currentSnapshot[2]
            if (selectedMediaAccess || (lastPermissionSnapshot != null && lastPermissionSnapshot != currentSnapshot)) {
                viewModel.invalidateGalleryCaches()
            }
            lastPermissionSnapshot = currentSnapshot
        }
    }

    private fun readPermissionSnapshot(): List<Boolean> = listOf(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED,
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED,
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(this, GalleryPermissionPolicy.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED,
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)

        if (accountId == null) {
            android.util.Log.e("GalleryFolderBrowser", "accountId is missing from intent!")
            android.widget.Toast.makeText(this, "Error: Account ID is missing", android.widget.Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()

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

        lifecycleScope.launch {
            val mediaFilter = getContentFilter()
            
            setupRecyclerView(mediaFilter)
            setupButtons()
            observeViewModel()

            // Check and request permissions before loading folders
            if (hasGalleryPermissions(mediaFilter)) {
                viewModel.clearNavigationBackStack()
                viewModel.loadFolders(parentFolderId = null, forceRefresh = false, addToBackStack = false, mediaFilter = mediaFilter)
            } else {
                requestGalleryPermissions(mediaFilter)
            }
        }
    }

    /**
     * Get the current content filter setting and map it to a string for the repository.
     */
    private suspend fun getContentFilter(): MediaTypeFilter {
        val config = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSlideshowConfig(this)
        // The ViewModel now expects MediaTypeFilter directly, so we can pass it as is.
        return config.mediaTypeFilter
    }

    private fun hasGalleryPermissions(mediaFilter: MediaTypeFilter): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasImages = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVideo = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val hasSelectedMedia = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                ContextCompat.checkSelfPermission(this, GalleryPermissionPolicy.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            GalleryPermissionPolicy.hasAccess(mediaFilter, hasImages, hasVideo, hasSelectedMedia)
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestGalleryPermissions(mediaFilter: MediaTypeFilter) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GalleryPermissionPolicy.requiredPermissions(
                mediaFilter,
                includeSelectedMedia = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ).toTypedArray()
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

    private suspend fun setupRecyclerView(mediaFilter: MediaTypeFilter?) {
        adapter = FolderAdapter(
            onSelectionStateChanged = { selectedIds, deselectedIds ->
                // Capture synchronous state to prevent corruption if the user navigates
                // before the coroutine resumes from reading DataStore.
                val visibleFolders = adapter.getVisibleFolders().associateBy { f -> f.id }
                val photoCount = adapter.getPhotoCount()
                
                lifecycleScope.launch {
                    saveSelections(selectedIds, deselectedIds, visibleFolders)
                    updateSummary(selectedIds.size, photoCount)
                }
            },
            onFolderClick = { folderId ->
                viewModel.navigateToFolder(folderId)
            },
            mediaFilter = mediaFilter
        )
        binding.recyclerFolders.layoutManager = LinearLayoutManager(this@GalleryFolderBrowserActivity)
        binding.recyclerFolders.adapter = adapter

        // Restore previously saved folder selections
        restoreSelectedFolders()
    }

    private suspend fun restoreSelectedFolders() {
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

    private suspend fun saveSelections(
        selectedIds: Set<String>, 
        deselectedIds: Set<String>, 
        visibleFolders: Map<String, com.vincentwetzel.androidscreensaver.data.model.PhotoFolder>
    ) {
        val dreamSourceType = com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY
        accountId?.let { id ->
            val account = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getAccount(this, dreamSourceType, id)
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
                com.vincentwetzel.androidscreensaver.utils.SettingsManager.saveAccount(this, updated)
            }
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
                            updateSummary(adapter.getSelectedFolders().size, adapter.getPhotoCount())
                            // Set parent folder context for auto-checking subfolders
                            adapter.setCurrentParentFolderId(viewModel.currentFolderId.value)
                        }
                        binding.progressBar.visibility = View.GONE
                    }
                }

                launch {
                    viewModel.currentFolderId.collectLatest { folderId ->
                        supportActionBar?.title = if (folderId != null) {
                            "📁 Subfolder"
                        } else {
                            "Browse Folders"
                        }
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
                            binding.progressBar.visibility = View.GONE
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(this, GalleryPermissionPolicy.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        ) {
            menu.add(Menu.NONE, MENU_CHANGE_SELECTED_MEDIA, Menu.NONE, getString(R.string.change_selected_media))
                .setOnMenuItemClickListener {
                    lifecycleScope.launch { requestGalleryPermissions(getContentFilter()) }
                    true
                }
        }

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

        return true
    }

}
