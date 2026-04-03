package com.vincentwetzel.androidscreensaver.ui.sources

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vincentwetzel.androidscreensaver.R
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

        // Load folders
        viewModel.loadFolders()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = FolderAdapter { selectedIds ->
            updateSummary(selectedIds.size, viewModel.getPhotoCount())
        }

        binding.recyclerFolders.layoutManager = LinearLayoutManager(this)
        binding.recyclerFolders.adapter = adapter
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
            // Reload folders with subfolder setting
            viewModel.setIncludeSubfolders(isChecked)
            viewModel.loadFolders()
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
                }
                binding.progressBar.visibility = View.GONE
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
                    // Show error
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
