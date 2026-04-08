package com.vincentwetzel.androidscreensaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.repository.GalleryPhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Gallery folder browsing
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: GalleryPhotoRepository
) : ViewModel() {

    // Folder state
    private val _folders = MutableStateFlow<List<PhotoFolder>>(emptyList())
    val folders: StateFlow<List<PhotoFolder>> = _folders.asStateFlow()

    private val _currentFolderId = MutableStateFlow<String?>(null)
    val currentFolderId: StateFlow<String?> = _currentFolderId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _includeSubfolders = MutableStateFlow(true)
    val includeSubfolders: StateFlow<Boolean> = _includeSubfolders.asStateFlow()

    /**
     * Load folders from Gallery (buckets)
     * Cache is in the repository, so it persists across Activity recreations
     */
    fun loadFolders(parentFolderId: String? = null, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _currentFolderId.value = parentFolderId
                val folderList = photoRepository.listFolders(parentFolderId, forceRefresh)

                // Get photo counts for each folder
                val foldersWithCounts = folderList.map { folder ->
                    val count = photoRepository.getFolderPhotoCount(folder.id)
                    folder.copy(photoCount = count)
                }

                _folders.value = foldersWithCounts
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load folders"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Navigate into a subfolder
     */
    fun navigateToFolder(folderId: String) {
        loadFolders(folderId)
    }

    /**
     * Navigate back to parent folder or root
     */
    fun navigateBack() {
        val currentId = _currentFolderId.value ?: return
        loadFolders(null, forceRefresh = false) // Use cache
    }

    /**
     * Search folders by name
     */
    fun searchFolders(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                loadFolders(_currentFolderId.value)
                return@launch
            }

            _isLoading.value = true
            try {
                _folders.value = photoRepository.searchFolders(query)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to search folders"
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
     * Set include subfolders preference
     */
    fun setIncludeSubfolders(include: Boolean) {
        _includeSubfolders.value = include
    }

    /**
     * Get total photo count from currently loaded folders
     */
    fun getPhotoCount(): Int {
        return folders.value.sumOf { it.photoCount }
    }
}
