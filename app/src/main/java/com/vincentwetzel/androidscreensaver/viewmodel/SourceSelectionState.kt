package com.vincentwetzel.androidscreensaver.viewmodel

/**
 * Pure source-card state used by [MainViewModel]. Keeping this state separate
 * makes source-selection behavior testable without constructing Android or
 * cloud-repository dependencies.
 */
class SourceSelectionState {
    var enabledSources: Set<SourceCardType> = emptySet()
        private set

    var isGoogleDriveAuthenticated: Boolean = false
        private set

    var googleDriveAccountName: String? = null
        private set

    var isDropboxAuthenticated: Boolean = false
        private set

    var dropboxAccountName: String? = null
        private set

    fun enableSource(source: SourceCardType) {
        enabledSources = enabledSources + source
    }

    fun disableSource(source: SourceCardType) {
        enabledSources = enabledSources - source
    }

    fun setGoogleDriveAuthenticated(
        isAuthenticated: Boolean,
        accountName: String? = null,
        updateEnabledSource: Boolean = true
    ) {
        isGoogleDriveAuthenticated = isAuthenticated
        googleDriveAccountName = accountName.takeIf { isAuthenticated }
        if (updateEnabledSource) {
            if (isAuthenticated) enableSource(SourceCardType.GOOGLE_DRIVE)
            else disableSource(SourceCardType.GOOGLE_DRIVE)
        }
    }

    fun setDropboxAuthenticated(
        isAuthenticated: Boolean,
        accountName: String? = null,
        updateEnabledSource: Boolean = true
    ) {
        isDropboxAuthenticated = isAuthenticated
        dropboxAccountName = accountName.takeIf { isAuthenticated }
        if (updateEnabledSource) {
            if (isAuthenticated) enableSource(SourceCardType.DROPBOX)
            else disableSource(SourceCardType.DROPBOX)
        }
    }
}
