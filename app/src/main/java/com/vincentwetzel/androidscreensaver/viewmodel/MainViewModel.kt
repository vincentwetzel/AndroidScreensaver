package com.vincentwetzel.androidscreensaver.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Source type for main screen cards
 */
enum class SourceCardType {
    GALLERY, GOOGLE_DRIVE
}

/**
 * ViewModel for the main screen
 * Manages source selection and authentication state
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val driveRepository: GoogleDriveRepository
) : ViewModel() {

    private val _enabledSources = MutableLiveData<Set<SourceCardType>>()
    val enabledSources: LiveData<Set<SourceCardType>> = _enabledSources

    private val _isGoogleDriveAuthenticated = MutableLiveData<Boolean>()
    val isGoogleDriveAuthenticated: LiveData<Boolean> = _isGoogleDriveAuthenticated

    private val _googleDriveAccountName = MutableLiveData<String?>()
    val googleDriveAccountName: LiveData<String?> = _googleDriveAccountName

    init {
        // Initialize with empty sources
        _enabledSources.value = emptySet()
        checkGoogleDriveAuthState()
    }

    /**
     * Check if user is already signed in and update account email
     */
    private fun checkGoogleDriveAuthState() {
        val accountIds = driveRepository.getAuthenticatedAccountIds()
        if (accountIds.isNotEmpty()) {
            // Use the first account's email for display
            val accountId = accountIds.first()
            val account = driveRepository.getAccount(accountId)
            _googleDriveAccountName.value = account?.email
            _isGoogleDriveAuthenticated.value = true
        } else {
            _googleDriveAccountName.value = null
            _isGoogleDriveAuthenticated.value = false
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
                val account = driveRepository.getAccount(accountId)
                _googleDriveAccountName.value = account?.email
                _isGoogleDriveAuthenticated.value = true
            } else {
                _googleDriveAccountName.value = null
                _isGoogleDriveAuthenticated.value = false
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
            _googleDriveAccountName.value = null
        }
    }
}
