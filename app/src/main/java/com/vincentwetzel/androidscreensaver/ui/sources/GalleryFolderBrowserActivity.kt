package com.vincentwetzel.androidscreensaver.ui.sources

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vincentwetzel.androidscreensaver.R
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

    companion object {
        const val EXTRA_SELECTED_FOLDERS = "selected_folders"
        const val RESULT_SELECTED_FOLDERS = "selected_folders_result"
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.loadFolders()
        } else {
            Toast.makeText(this, "Photo permission is required to browse Gallery folders", Toast.LENGTH_LONG).show()
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        observeViewModel()

        // Check and request permissions before loading folders
        if (hasGalleryPermissions()) {
            viewModel.loadFolders()
        } else {
            requestGalleryPermissions()
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
            if (viewModel.currentFolderId.value != null) {
                viewModel.navigateBack()
            } else {
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = FolderAdapter(
            onSelectionChanged = { selectedIds ->
                updateSummary(selectedIds.size, viewModel.getPhotoCount())
            },
            onFolderClick = { folderId ->
                viewModel.navigateToFolder(folderId)
            }
        )

        binding.recyclerFolders.layoutManager = LinearLayoutManager(this)
        binding.recyclerFolders.adapter = adapter

        // Restore previously saved folder selections
        restoreSelectedFolders()
    }

    private fun restoreSelectedFolders() {
        val savedFolders = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getSelectedFolders(
            this,
            com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY
        )
        val savedFolderIds = savedFolders.map { it.id }.toSet()
        adapter.setSelectedFolders(savedFolderIds)
    }

    private fun setupButtons() {
        binding.btnSelectAll.setOnClickListener {
            adapter.selectAll()
        }

        binding.btnDeselectAll.setOnClickListener {
            adapter.deselectAll()
        }

        binding.btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        binding.btnSave.setOnClickListener {
            val selectedFolders = adapter.getSelectedFolders()
            val intent = intent.apply {
                putStringArrayListExtra(RESULT_SELECTED_FOLDERS, ArrayList(selectedFolders))
            }
            setResult(RESULT_OK, intent)
            finish()
        }

        binding.switchIncludeSubfolders.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIncludeSubfolders(isChecked)
            viewModel.loadFolders(forceRefresh = true)
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
                        it,
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun updateSummary(folderCount: Int, photoCount: Int) {
        binding.summaryText.text = getString(R.string.selected_folders_summary)
            .replace("0 folders", "$folderCount folders")
            .replace("0 photos", "$photoCount photos")
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
