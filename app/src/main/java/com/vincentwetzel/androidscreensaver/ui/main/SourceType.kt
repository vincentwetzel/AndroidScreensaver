package com.vincentwetzel.androidscreensaver.ui.main

import com.vincentwetzel.androidscreensaver.dream.SourceType as DreamSourceType

/**
 * Source types for the main menu
 * Mirrors data.model.SourceType for UI purposes
 */
enum class SourceType {
    GALLERY,
    GOOGLE_DRIVE,
    DROPBOX,
    ONEDRIVE;

    fun toDreamSourceType(): DreamSourceType {
        return when (this) {
            GALLERY -> DreamSourceType.GALLERY
            GOOGLE_DRIVE -> DreamSourceType.GOOGLE_DRIVE
            DROPBOX -> DreamSourceType.DROPBOX
            ONEDRIVE -> DreamSourceType.ONEDRIVE
        }
    }
}
