package com.vincentwetzel.androidscreensaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vincentwetzel.androidscreensaver.data.model.FolderError
import com.vincentwetzel.androidscreensaver.data.model.FolderError.Companion.fromException
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDrivePhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for Google Drive folder browsing.
 * Now supports per-account operations via accountId parameter.
 */
@HiltViewModel
class GoogleDriveViewModel @Inject constructor(
    private val driveRepository: GoogleDriveRepository,
    private val photoRepository: GoogleDrivePhotoRepository
) : ViewModel() {

    // Account ID for per-account routing (null = first available account)
    private var accountId: String? = null

    // Folder state
    private val _folders = MutableStateFlow<List<PhotoFolder>>(emptyList())
    val folders: StateFlow<List<PhotoFolder>> = _folders.asStateFlow()

    private val _currentFolderId = MutableStateFlow<String?>(null)
    val currentFolderId: StateFlow<String?> = _currentFolderId.asStateFlow()

    // Back stack for folder navigation
    private val _navigationBackStack = mutableListOf<String?>()

    fun getNavigationBackStack(): List<String?> = _navigationBackStack.toList()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<FolderError?>(null)
    val error: StateFlow<FolderError?> = _error.asStateFlow()

    /**
     * Set the account ID for this ViewModel instance. Must be called before loading folders.
     */
    fun setAccountId(id: String) {
        accountId = id
    }

    /**
     * Load folders from Google Drive for the configured account.
     */
    fun loadFolders(parentFolderId: String? = null, forceRefresh: Boolean = false, addToBackStack: Boolean = true, mediaFilter: String? = null) {
        val id = accountId ?: run {
            _error.value = FolderError.UnknownError("No account configured for Google Drive folder browsing")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                if (addToBackStack) {
                    _navigationBackStack.add(_currentFolderId.value)
                }
                _currentFolderId.value = parentFolderId
                val folderList = photoRepository.listFoldersForAccount(parentFolderId, forceRefresh, id)

                // Get media counts for each folder, filtered by the content filter
                val foldersWithCounts = folderList.map { folder ->
                    val count = photoRepository.getFilteredFolderMediaCountForAccount(folder.id, mediaFilter, id)
                    folder.copy(photoCount = count)
                }

                _folders.value = foldersWithCounts
            } catch (e: Exception) {
                _error.value = fromException(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Navigate to a subfolder
     */
    fun navigateToFolder(folderId: String) {
        loadFolders(folderId)
    }

    /**
     * Navigate back to the previously visited folder.
     * Returns true if a back navigation occurred (even if it was to root).
     * Returns false if there was no navigation history (caller should finish Activity).
     */
    fun navigateBack(): Boolean {
        if (_navigationBackStack.isEmpty()) return false
        val previousFolderId = _navigationBackStack.removeAt(_navigationBackStack.size - 1)
        loadFolders(previousFolderId, forceRefresh = false, addToBackStack = false)
        return true
    }

    /**
     * Clear the navigation back stack (call when Activity is created fresh).
     */
    fun clearNavigationBackStack() {
        _navigationBackStack.clear()
    }

    /**
     * Search folders by name for the configured account.
     */
    fun searchFolders(query: String) {
        val id = accountId ?: return

        viewModelScope.launch {
            if (query.isBlank()) {
                loadFolders(_currentFolderId.value)
                return@launch
            }

            _isLoading.value = true
            try {
                _folders.value = photoRepository.searchFoldersForAccount(query, id)
            } catch (e: Exception) {
                _error.value = fromException(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Get total photo count from currently loaded folders
     */
    fun getPhotoCount(): Int {
        return folders.value.sumOf { it.photoCount }
    }

    /**
     * Get ALL subfolder IDs recursively for a given folder (used for cascade selection).
     * Routes to the correct account.
     */
    suspend fun getSubfolderIds(folderId: String): List<String> = withContext(Dispatchers.IO) {
        val id = accountId ?: return@withContext emptyList()
        val driveService = driveRepository.getDriveService(id) ?: return@withContext emptyList()
        val allSubfolders = mutableListOf<String>()
        collectSubfolderIds(folderId, driveService, allSubfolders)
        allSubfolders
    }

    private fun collectSubfolderIds(parentId: String, driveService: com.google.api.services.drive.Drive, result: MutableList<String>) {
        try {
            val query = "mimeType='application/vnd.google-apps.folder' and trashed=false and '$parentId' in parents"
            var nextPageToken: String? = null
            do {
                val files = driveService.files().list()
                    .setQ(query)
                    .setPageSize(1000)
                    .setFields("nextPageToken, files(id)")
                    .setPageToken(nextPageToken)
                    .execute()
                files.files?.forEach { file ->
                    file.id?.let {
                        result.add(it)
                        collectSubfolderIds(it, driveService, result)
                    }
                }
                nextPageToken = files.nextPageToken
            } while (nextPageToken != null)
        } catch (e: Exception) {
            android.util.Log.w("GoogleDriveVM", "Failed to get subfolders: ${e.message}")
        }
    }
}