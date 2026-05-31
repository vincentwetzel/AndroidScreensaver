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
        _enabledSources.value = emptySet()
        checkGoogleDriveAuthState()
        checkDropboxAuthState()
    }

    /**
     * Check if user is already signed in and update account email
     */
    private fun checkGoogleDriveAuthState() {
        val accountIds = driveRepository.getAuthenticatedAccountIds()
        if (accountIds.isNotEmpty()) {
            // Use the first account's email for display
            val accountId = accountIds.first()
            _googleDriveAccountName.value = driveRepository.getAccountEmail(accountId)
            _isGoogleDriveAuthenticated.value = true
        } else {
            _googleDriveAccountName.value = null
            _isGoogleDriveAuthenticated.value = false
        }
    }

    /**
     * Check if user is already signed in to Dropbox and update account email
     */
    private fun checkDropboxAuthState() {
        val accountIds = dropboxRepository.getAuthenticatedAccountIds()
        if (accountIds.isNotEmpty()) {
            val accountId = accountIds.first()
            _dropboxAccountName.value = dropboxRepository.getAccountEmail(accountId)
            _isDropboxAuthenticated.value = true
        } else {
            _dropboxAccountName.value = null
            _isDropboxAuthenticated.value = false
        }
    }

    /**
     * Refresh account email from the repository (call after auth or sign-in)
     */
    fun refreshGoogleDriveAccountName() {
        viewModelScope.launch {
            val accountIds = driveRepository.getAuthenticatedAccountIds()
            if (accountIds.isNotEmpty()) {
                val accountId = accountIds.first()
                _googleDriveAccountName.value = driveRepository.getAccountEmail(accountId)
                _isGoogleDriveAuthenticated.value = true
            } else {
                _googleDriveAccountName.value = null
                _isGoogleDriveAuthenticated.value = false
                disableSource(SourceCardType.GOOGLE_DRIVE)
            }
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
                _dropboxAccountName.value = dropboxRepository.getAccountEmail(accountId)
                _isDropboxAuthenticated.value = true
            } else {
                _dropboxAccountName.value = null
                _isDropboxAuthenticated.value = false
                disableSource(SourceCardType.DROPBOX)
            }
        }
    }

    fun enableSource(source: SourceCardType) {
        val current = _enabledSources.value?.toMutableSet() ?: mutableSetOf()
        current.add(source)
        _enabledSources.value = current
    }

    fun disableSource(source: SourceCardType) {
        val current = _enabledSources.value?.toMutableSet() ?: mutableSetOf()
        current.remove(source)
        _enabledSources.value = current
    }

    fun onGoogleDriveAuthenticated(isAuthenticated: Boolean, accountName: String? = null) {
        _isGoogleDriveAuthenticated.value = isAuthenticated
        if (isAuthenticated) {
            enableSource(SourceCardType.GOOGLE_DRIVE)
            _googleDriveAccountName.value = accountName
        } else {
            disableSource(SourceCardType.GOOGLE_DRIVE)
            _googleDriveAccountName.value = null
        }
    }

    fun onDropboxAuthenticated(isAuthenticated: Boolean, accountName: String? = null) {
        _isDropboxAuthenticated.value = isAuthenticated
        if (isAuthenticated) {
            enableSource(SourceCardType.DROPBOX)
            _dropboxAccountName.value = accountName
        } else {
            disableSource(SourceCardType.DROPBOX)
            _dropboxAccountName.value = null
        }
    }
}
