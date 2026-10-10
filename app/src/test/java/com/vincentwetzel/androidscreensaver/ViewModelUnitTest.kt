package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.viewmodel.SourceCardType
import com.vincentwetzel.androidscreensaver.viewmodel.SourceSelectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for pure ViewModel source-selection state.
 */
class ViewModelUnitTest {

    private lateinit var sourceState: SourceSelectionState

    @Before
    fun setup() {
        sourceState = SourceSelectionState()
    }

    @Test
    fun `source state starts with no enabled sources`() {
        assertTrue(sourceState.enabledSources.isEmpty())
    }

    @Test
    fun `source state starts with Google Drive unauthenticated`() {
        assertFalse(sourceState.isGoogleDriveAuthenticated)
    }

    @Test
    fun `enableSource adds source to enabled set`() {
        sourceState.enableSource(SourceCardType.GOOGLE_DRIVE)
        assertTrue(sourceState.enabledSources.contains(SourceCardType.GOOGLE_DRIVE))
    }

    @Test
    fun `disableSource removes source from enabled set`() {
        sourceState.enableSource(SourceCardType.GOOGLE_DRIVE)
        sourceState.disableSource(SourceCardType.GOOGLE_DRIVE)
        assertFalse(sourceState.enabledSources.contains(SourceCardType.GOOGLE_DRIVE))
    }

    @Test
    fun `Google Drive authentication records email and enables source`() {
        sourceState.setGoogleDriveAuthenticated(true, "user@example.com")

        assertTrue(sourceState.isGoogleDriveAuthenticated)
        assertEquals("user@example.com", sourceState.googleDriveAccountName)
        assertTrue(sourceState.enabledSources.contains(SourceCardType.GOOGLE_DRIVE))
    }

    @Test
    fun `enabling source twice does not duplicate it`() {
        sourceState.enableSource(SourceCardType.GOOGLE_DRIVE)
        sourceState.enableSource(SourceCardType.GOOGLE_DRIVE)

        assertEquals(1, sourceState.enabledSources.size)
    }

    @Test
    fun `disabling a source that is not enabled is safe`() {
        sourceState.disableSource(SourceCardType.GOOGLE_DRIVE)
        assertTrue(sourceState.enabledSources.isEmpty())
    }

    @Test
    fun `signing out clears account email and disables the source`() {
        sourceState.setGoogleDriveAuthenticated(true, "user@example.com")

        sourceState.setGoogleDriveAuthenticated(false, "stale@example.com")

        assertFalse(sourceState.isGoogleDriveAuthenticated)
        assertEquals(null, sourceState.googleDriveAccountName)
        assertFalse(sourceState.enabledSources.contains(SourceCardType.GOOGLE_DRIVE))
    }

    @Test
    fun `authentication can update status without changing existing source enablement`() {
        sourceState.enableSource(SourceCardType.GOOGLE_DRIVE)

        sourceState.setGoogleDriveAuthenticated(true, "user@example.com", updateEnabledSource = false)
        sourceState.setDropboxAuthenticated(true, "dropbox@example.com", updateEnabledSource = false)

        assertTrue(sourceState.enabledSources.contains(SourceCardType.GOOGLE_DRIVE))
        assertFalse(sourceState.enabledSources.contains(SourceCardType.DROPBOX))
        assertEquals("dropbox@example.com", sourceState.dropboxAccountName)
    }

    @Test
    fun `signed out Dropbox cannot retain a stale account name`() {
        sourceState.setDropboxAuthenticated(true, "dropbox@example.com")

        sourceState.setDropboxAuthenticated(false)

        assertFalse(sourceState.isDropboxAuthenticated)
        assertEquals(null, sourceState.dropboxAccountName)
    }
}
