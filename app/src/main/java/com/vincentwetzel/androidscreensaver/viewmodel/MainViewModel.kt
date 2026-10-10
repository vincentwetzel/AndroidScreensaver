package com.vincentwetzel.androidscreensaver.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vincentwetzel.androidscreensaver.data.repository.DropboxRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Source type for main screen cards
 */
enum class SourceCardType {
    GALLERY, GOOGLE_DRIVE, DROPBOX
}

/**
 * ViewModel for the main screen
 * Manages source selection and authentication state
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val driveRepository: GoogleDriveRepository,
    private val dropboxRepository: DropboxRepository
) : ViewModel() {

    private val sourceSelectionState = SourceSelectionState()

    private val _enabledSources = MutableLiveData<Set<SourceCardType>>()
    val enabledSources: LiveData<Set<SourceCardType>> = _enabledSources

    private val _isGoogleDriveAuthenticated = MutableLiveData<Boolean>()
    val isGoogleDriveAuthenticated: LiveData<Boolean> = _isGoogleDriveAuthenticated

    private val _googleDriveAccountName = MutableLiveData<String?>()
    val googleDriveAccountName: LiveData<String?> = _googleDriveAccountName

    private val _isDropboxAuthenticated = MutableLiveData<Boolean>()
    val isDropboxAuthenticated: LiveData<Boolean> = _isDropboxAuthenticated

    private val _dropboxAccountName = MutableLiveData<String?>()
    val dropboxAccountName: LiveData<String?> = _dropboxAccountName

    init {
        // Initialize with empty sources
        publishSourceState()
        checkGoogleDriveAuthState()
        checkDropboxAuthState()
    }

    private fun publishSourceState() {
        _enabledSources.value = sourceSelectionState.enabledSources
    }

    private fun publishGoogleDriveState() {
        _isGoogleDriveAuthenticated.value = sourceSelectionState.isGoogleDriveAuthenticated
        _googleDriveAccountName.value = sourceSelectionState.googleDriveAccountName
    }

    private fun publishDropboxState() {
        _isDropboxAuthenticated.value = sourceSelectionState.isDropboxAuthenticated
        _dropboxAccountName.value = sourceSelectionState.dropboxAccountName
    }

    /**
     * Check if user is already signed in and update account email
     */
    private fun checkGoogleDriveAuthState() {
        val accountIds = driveRepository.getAuthenticatedAccountIds()
        if (accountIds.isNotEmpty()) {
            // Use the first account's email for display
            val accountId = accountIds.first()
            sourceSelectionState.setGoogleDriveAuthenticated(
                true,
                driveRepository.getAccountEmail(accountId),
                updateEnabledSource = false
            )
        } else {
            sourceSelectionState.setGoogleDriveAuthenticated(false)
        }
        publishSourceState()
        publishGoogleDriveState()
    }

    /**
     * Check if user is already signed in to Dropbox and update account email
     */
    private fun checkDropboxAuthState() {
        val accountIds = dropboxRepository.getAuthenticatedAccountIds()
        if (accountIds.isNotEmpty()) {
            val accountId = accountIds.first()
            sourceSelectionState.setDropboxAuthenticated(
                true,
                dropboxRepository.getAccountEmail(accountId),
                updateEnabledSource = false
            )
        } else {
            sourceSelectionState.setDropboxAuthenticated(false)
        }
        publishSourceState()
        publishDropboxState()
    }

    /**
     * Refresh account email from the repository (call after auth or sign-in)
     */
    fun refreshGoogleDriveAccountName() {
        viewModelScope.launch {
            val accountIds = driveRepository.getAuthenticatedAccountIds()
            if (accountIds.isNotEmpty()) {
                val accountId = accountIds.first()
                sourceSelectionState.setGoogleDriveAuthenticated(
                    true,
                    driveRepository.getAccountEmail(accountId),
                    updateEnabledSource = false
                )
            } else {
                sourceSelectionState.setGoogleDriveAuthenticated(false)
            }
            publishSourceState()
            publishGoogleDriveState()
        }
    }

    /**
     * Refresh Dropbox account email from the repository (call after auth or sign-in)
     */
    fun refreshDropboxAccountName() {
        viewModelScope.launch {
            val accountIds = dropboxRepository.getAuthenticatedAccountIds()
            if (accountIds.isNotEmpty()) {
                val accountId = accountIds.first()
                sourceSelectionState.setDropboxAuthenticated(
                    true,
                    dropboxRepository.getAccountEmail(accountId),
                    updateEnabledSource = false
                )
            } else {
                sourceSelectionState.setDropboxAuthenticated(false)
            }
            publishSourceState()
            publishDropboxState()
        }
    }

    fun enableSource(source: SourceCardType) {
        sourceSelectionState.enableSource(source)
        publishSourceState()
    }

    fun disableSource(source: SourceCardType) {
        sourceSelectionState.disableSource(source)
        publishSourceState()
    }

    fun onGoogleDriveAuthenticated(isAuthenticated: Boolean, accountName: String? = null) {
        sourceSelectionState.setGoogleDriveAuthenticated(isAuthenticated, accountName)
        publishSourceState()
        publishGoogleDriveState()
    }

    fun onDropboxAuthenticated(isAuthenticated: Boolean, accountName: String? = null) {
        sourceSelectionState.setDropboxAuthenticated(isAuthenticated, accountName)
        publishSourceState()
        publishDropboxState()
    }
}
