package com.vincentwetzel.androidscreensaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDrivePhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _includeSubfolders = MutableStateFlow(true)
    val includeSubfolders: StateFlow<Boolean> = _includeSubfolders.asStateFlow()

    init {
        // Check if user is already signed in
        driveRepository.checkExistingSignIn()
        if (driveRepository.isAuthenticated().value) {
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
     */
    fun loadFolders(parentFolderId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _currentFolderId.value = parentFolderId
                val folderList = photoRepository.listFolders(parentFolderId)

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
        // Store this preference for use when loading folders
        _includeSubfolders.value = include
    }

    /**
     * Get total photo count from currently loaded folders
     */
    fun getPhotoCount(): Int {
        return folders.value.sumOf { it.photoCount }
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
