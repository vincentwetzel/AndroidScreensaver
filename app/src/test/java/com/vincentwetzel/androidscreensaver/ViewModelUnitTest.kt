package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoOrder
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.viewmodel.GoogleDriveViewModel
import com.vincentwetzel.androidscreensaver.viewmodel.MainViewModel
import com.vincentwetzel.androidscreensaver.ui.main.SourceType as UISourceType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ViewModels
 * Tests core logic without Android dependencies
 */
class ViewModelUnitTest {

    private lateinit var mainViewModel: MainViewModel

    @Before
    fun setup() {
        mainViewModel = MainViewModel()
    }

    @Test
    fun `MainViewModel starts with no enabled sources`() {
        val sources = mainViewModel.enabledSources.value
        assertTrue(sources.isNullOrEmpty())
    }

    @Test
    fun `MainViewModel starts with not authenticated`() {
        val isAuthenticated = mainViewModel.isGoogleDriveAuthenticated.value
        assertFalse(isAuthenticated == true)
    }

    @Test
    fun `enableSource adds source to enabled list`() {
        mainViewModel.enableSource(UISourceType.GOOGLE_DRIVE)
        val sources = mainViewModel.enabledSources.value
        assertNotNull(sources)
        assertTrue(sources?.contains(UISourceType.GOOGLE_DRIVE) == true)
    }

    @Test
    fun `disableSource removes source from enabled list`() {
        mainViewModel.enableSource(UISourceType.GOOGLE_DRIVE)
        mainViewModel.disableSource(UISourceType.GOOGLE_DRIVE)
        val sources = mainViewModel.enabledSources.value
        assertNotNull(sources)
        assertFalse(sources?.contains(UISourceType.GOOGLE_DRIVE) == true)
    }

    @Test
    fun `onGoogleDriveAuthenticated sets authentication state`() {
        mainViewModel.onGoogleDriveAuthenticated(true)
        assertTrue(mainViewModel.isGoogleDriveAuthenticated.value == true)
    }

    @Test
    fun `onGoogleDriveAuthenticated enables Google Drive source`() {
        mainViewModel.onGoogleDriveAuthenticated(true)
        val sources = mainViewModel.enabledSources.value
        assertNotNull(sources)
        assertTrue(sources?.contains(UISourceType.GOOGLE_DRIVE) == true)
    }

    @Test
    fun `enableSource twice does not duplicate`() {
        mainViewModel.enableSource(UISourceType.GOOGLE_DRIVE)
        mainViewModel.enableSource(UISourceType.GOOGLE_DRIVE)
        val sources = mainViewModel.enabledSources.value
        assertNotNull(sources)
        assertEquals(1, sources?.size)
    }

    @Test
    fun `disableSource when not enabled does nothing`() {
        mainViewModel.disableSource(UISourceType.GOOGLE_DRIVE)
        val sources = mainViewModel.enabledSources.value
        assertTrue(sources.isNullOrEmpty())
    }
}
