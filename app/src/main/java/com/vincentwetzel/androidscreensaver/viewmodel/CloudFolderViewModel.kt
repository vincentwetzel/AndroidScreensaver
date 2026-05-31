package com.vincentwetzel.androidscreensaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vincentwetzel.androidscreensaver.data.model.FolderError
import com.vincentwetzel.androidscreensaver.data.model.FolderError.Companion.fromException
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.data.repository.PhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.BaseCloudPhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Generic ViewModel for cloud folder browsing (Google Drive, Dropbox, etc.).
 * Now supports per-account operations via accountId parameter.
 */
@HiltViewModel
class CloudFolderViewModel @Inject constructor(
    private val photoRepositories: Map<SourceType, @JvmSuppressWildcards PhotoRepository>
) : ViewModel() {

    // Account ID for per-account routing (null = first available account)
    private var accountId: String? = null
    private var sourceType: SourceType? = null

    // Folder state
    private val _folders = MutableStateFlow<List<PhotoFolder>>(emptyList())
    val folders: StateFlow<List<PhotoFolder>> = _folders.asStateFlow()

    private val _currentFolderId = MutableStateFlow<String?>(null)
    val currentFolderId: StateFlow<String?> = _currentFolderId.asStateFlow()

    // Track the active media filter so it persists during back navigation
    private var currentMediaFilter: String? = null

    // Back stack for folder navigation
    private val _navigationBackStack = mutableListOf<String?>()

    // Track current jobs to prevent race conditions
    private var currentJob: Job? = null

    fun getNavigationBackStack(): List<String?> = _navigationBackStack.toList()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<FolderError?>(null)
    val error: StateFlow<FolderError?> = _error.asStateFlow()

    /**
     * Set the source type and account ID for this ViewModel instance. Must be called before loading folders.
     */
    fun setSourceContext(type: SourceType, id: String) {
        sourceType = type
        accountId = id
    }

    private fun getRepository(): BaseCloudPhotoRepository {
        val type = sourceType ?: throw IllegalStateException("SourceType not set")
        val repo = photoRepositories[type] ?: throw IllegalStateException("No repository configured for $type")
        // Cast to BaseCloudPhotoRepository to access the unified multi-account folder methods
        return repo as? BaseCloudPhotoRepository 
            ?: throw IllegalStateException("Repository for $type must implement BaseCloudPhotoRepository")
    }

    /**
     * Load folders from the cloud source for the configured account.
     */
    fun loadFolders(parentFolderId: String? = null, forceRefresh: Boolean = false, addToBackStack: Boolean = true, mediaFilter: String? = currentMediaFilter) {
        currentMediaFilter = mediaFilter
        val id = accountId ?: run {
            _error.value = FolderError.UnknownError("No account configured for folder browsing")
            return
        }

        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                if (addToBackStack) {
                    _navigationBackStack.add(_currentFolderId.value)
                }
                _currentFolderId.value = parentFolderId
                
                val repo = getRepository()
                val folderList = repo.listFoldersForAccount(parentFolderId, forceRefresh, id)

                // Get media counts for each folder, filtered by the content filter
                val foldersWithCounts = folderList.map { folder ->
                    val count = repo.getFilteredFolderMediaCountForAccount(folder.id, mediaFilter, id)
                    folder.copy(photoCount = count)
                }

                _folders.value = foldersWithCounts
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (currentJob == coroutineContext[Job]) {
                    _error.value = fromException(e)
                }
            } finally {
                if (currentJob == coroutineContext[Job]) {
                    _isLoading.value = false
                }
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
     * Search folders by name for the configured source/account.
     */
    fun searchFolders(query: String) {
        val id = accountId ?: run {
            _error.value = FolderError.UnknownError("No account configured for folder browsing")
            return
        }

        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            if (query.isBlank()) {
                loadFolders(_currentFolderId.value, addToBackStack = false)
                return@launch
            }

            _isLoading.value = true
            try {
                _folders.value = getRepository().searchFoldersForAccount(query, id)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (currentJob == coroutineContext[Job]) {
                    _error.value = fromException(e)
                }
            } finally {
                if (currentJob == coroutineContext[Job]) {
                    _isLoading.value = false
                }
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
        getRepository().getSubfolderIdsForAccount(folderId, id)
    }
}