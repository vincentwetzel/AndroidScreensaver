package com.vincentwetzel.androidscreensaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.vincentwetzel.androidscreensaver.data.model.FolderError
import com.vincentwetzel.androidscreensaver.data.model.FolderError.Companion.fromException
import com.vincentwetzel.androidscreensaver.data.model.FolderError.Companion.userMessage
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDrivePhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for Google Drive authentication and folder browsing
 */
@HiltViewModel
class GoogleDriveViewModel @Inject constructor(
    private val driveRepository: GoogleDriveRepository,
    private val photoRepository: GoogleDrivePhotoRepository
) : ViewModel() {

    // Authentication state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Account info
    private val _accountName = MutableStateFlow<String?>(null)
    val accountName: StateFlow<String?> = _accountName.asStateFlow()

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

    init {
        // Check if user is already signed in
        driveRepository.checkExistingSignIn()
        if (driveRepository.isAuthenticated.value) {
            _authState.value = AuthState.Authenticated
            _accountName.value = driveRepository.currentAccount.value?.displayName
        }
    }

    /**
     * Handle successful authentication
     */
    fun onAuthenticated(account: GoogleSignInAccount) {
        viewModelScope.launch {
            val success = driveRepository.handleSignInResult(account)
            if (success) {
                _authState.value = AuthState.Authenticated
                _accountName.value = account.displayName
                _error.value = null
            } else {
                _authState.value = AuthState.Error("Failed to authenticate")
            }
        }
    }

    /**
     * Sign out
     */
    fun signOut() {
        viewModelScope.launch {
            driveRepository.signOut()
            _authState.value = AuthState.Unauthenticated
            _accountName.value = null
            _folders.value = emptyList()
        }
    }

    /**
     * Load folders from Google Drive
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
     */
    suspend fun getSubfolderIds(folderId: String): List<String> = withContext(Dispatchers.IO) {
        val driveService = driveRepository.getDriveService() ?: return@withContext emptyList()
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
                        collectSubfolderIds(it, driveService, result) // recurse
                    }
                }
                nextPageToken = files.nextPageToken
            } while (nextPageToken != null)
        } catch (e: Exception) {
            android.util.Log.w("GoogleDriveVM", "Failed to get subfolders: ${e.message}")
        }
    }
}

/**
 * Authentication state
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticated : AuthState()
    object Authenticating : AuthState()
    data class Error(val message: String) : AuthState()
}