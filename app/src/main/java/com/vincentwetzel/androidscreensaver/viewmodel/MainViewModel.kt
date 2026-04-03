package com.vincentwetzel.androidscreensaver.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vincentwetzel.androidscreensaver.ui.main.SourceType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for the main screen
 * Manages source selection and authentication state
 */
@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _enabledSources = MutableLiveData<Set<SourceType>>()
    val enabledSources: LiveData<Set<SourceType>> = _enabledSources

    private val _isGoogleDriveAuthenticated = MutableLiveData<Boolean>()
    val isGoogleDriveAuthenticated: LiveData<Boolean> = _isGoogleDriveAuthenticated

    init {
        // Initialize with empty sources
        _enabledSources.value = emptySet()
        // TODO: Check actual auth state from repository
        _isGoogleDriveAuthenticated.value = false
    }

    fun enableSource(source: SourceType) {
        val current = _enabledSources.value?.toMutableSet() ?: mutableSetOf()
        current.add(source)
        _enabledSources.value = current
    }

    fun disableSource(source: SourceType) {
        val current = _enabledSources.value?.toMutableSet() ?: mutableSetOf()
        current.remove(source)
        _enabledSources.value = current
    }

    fun onGoogleDriveAuthenticated(isAuthenticated: Boolean) {
        _isGoogleDriveAuthenticated.value = isAuthenticated
        if (isAuthenticated) {
            enableSource(SourceType.GOOGLE_DRIVE)
        }
    }
}
