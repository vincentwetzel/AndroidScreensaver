package com.vincentwetzel.androidscreensaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vincentwetzel.androidscreensaver.data.model.FolderError
import com.vincentwetzel.androidscreensaver.data.model.FolderError.Companion.fromException
import com.vincentwetzel.androidscreensaver.data.model.FolderError.Companion.userMessage
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.repository.GalleryPhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // Back stack for folder navigation
    private val _navigationBackStack = mutableListOf<String?>()

    fun getNavigationBackStack(): List<String?> = _navigationBackStack.toList()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<FolderError?>(null)
    val error: StateFlow<FolderError?> = _error.asStateFlow()

    /**
     * Load folders from Gallery (buckets)
     * Cache is in the repository, so it persists across Activity recreations
     */
    fun loadFolders(parentFolderId: String? = null, forceRefresh: Boolean = false, addToBackStack: Boolean = true, mediaFilter: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                if (addToBackStack) {
                    _navigationBackStack.add(_currentFolderId.value)
                }
                _currentFolderId.value = parentFolderId
                val folderList = photoRepository.listFolders(parentFolderId, forceRefresh)

                // Get media counts for each folder, filtered by the content filter
                val foldersWithCounts = folderList.map { folder ->
                    val count = photoRepository.getFilteredFolderMediaCount(folder.id, mediaFilter)
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
     * Uses MediaStore RELATIVE_PATH to find buckets whose path starts with the folder's path.
     */
    suspend fun getSubfolderIds(folderId: String): List<String> = withContext(Dispatchers.IO) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            // No RELATIVE_PATH available on Android < 10
            return@withContext emptyList()
        }
        val subfolders = mutableListOf<String>()
        try {
            val contextField = photoRepository::class.java.getDeclaredField("context").apply { isAccessible = true }
            val context = contextField.get(photoRepository) as android.content.Context
            val contentResolver = context.contentResolver
            val collection = android.provider.MediaStore.Files.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL)

            // Get the RELATIVE_PATH for the given folder (bucket ID)
            val pathProjection = arrayOf(
                android.provider.MediaStore.Files.FileColumns.BUCKET_ID,
                android.provider.MediaStore.Files.FileColumns.RELATIVE_PATH
            )
            val pathSelection = "${android.provider.MediaStore.Files.FileColumns.BUCKET_ID} = ?"
            val pathCursor = contentResolver.query(collection, pathProjection, pathSelection, arrayOf(folderId), null)
            val folderRelativePath = pathCursor?.use {
                if (it.moveToFirst()) {
                    val pathIdx = it.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.RELATIVE_PATH)
                    it.getString(pathIdx)
                } else null
            }

            if (folderRelativePath == null) return@withContext emptyList()

            // Find all buckets whose RELATIVE_PATH starts with this folder's path (excluding the folder itself)
            val allCursor = contentResolver.query(collection, pathProjection, null, null, null)
            allCursor?.use {
                val bucketIdIdx = it.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.BUCKET_ID)
                val pathIdx = it.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.RELATIVE_PATH)
                val seen = mutableSetOf<String>()
                while (it.moveToNext()) {
                    val bucketId = it.getString(bucketIdIdx)
                    val path = it.getString(pathIdx)
                    if (bucketId !in seen && path != null && path.startsWith(folderRelativePath) && path != folderRelativePath && bucketId != folderId) {
                        seen.add(bucketId)
                        subfolders.add(bucketId)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GalleryViewModel", "Failed to get subfolders for $folderId: ${e.message}")
        }
        subfolders
    }
}
