package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter
import com.vincentwetzel.androidscreensaver.utils.GalleryPermissionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryPermissionPolicyUnitTest {

    @Test
    fun `specific media filter requires its matching permission`() {
        assertTrue(GalleryPermissionPolicy.hasAccess(MediaTypeFilter.IMAGES_ONLY, imagesGranted = true, videosGranted = false))
        assertFalse(GalleryPermissionPolicy.hasAccess(MediaTypeFilter.IMAGES_ONLY, imagesGranted = false, videosGranted = true))
        assertTrue(GalleryPermissionPolicy.hasAccess(MediaTypeFilter.VIDEOS_ONLY, imagesGranted = false, videosGranted = true))
        assertFalse(GalleryPermissionPolicy.hasAccess(MediaTypeFilter.VIDEOS_ONLY, imagesGranted = true, videosGranted = false))
    }

    @Test
    fun `all media mode accepts either available media permission`() {
        assertTrue(GalleryPermissionPolicy.hasAccess(MediaTypeFilter.IMAGES_AND_VIDEOS, imagesGranted = true, videosGranted = false))
        assertTrue(GalleryPermissionPolicy.hasAccess(MediaTypeFilter.IMAGES_AND_VIDEOS, imagesGranted = false, videosGranted = true))
        assertFalse(GalleryPermissionPolicy.hasAccess(MediaTypeFilter.IMAGES_AND_VIDEOS, imagesGranted = false, videosGranted = false))
    }

    @Test
    fun `selected media access satisfies the current media filter`() {
        assertTrue(
            GalleryPermissionPolicy.hasAccess(
                MediaTypeFilter.VIDEOS_ONLY,
                imagesGranted = false,
                videosGranted = false,
                selectedMediaGranted = true
            )
        )
    }

    @Test
    fun `permission request matches selected content filter`() {
        assertEquals(
            listOf("android.permission.READ_MEDIA_IMAGES"),
            GalleryPermissionPolicy.requiredPermissions(MediaTypeFilter.IMAGES_ONLY, includeSelectedMedia = false)
        )
        assertEquals(
            listOf("android.permission.READ_MEDIA_VIDEO", GalleryPermissionPolicy.READ_MEDIA_VISUAL_USER_SELECTED),
            GalleryPermissionPolicy.requiredPermissions(MediaTypeFilter.VIDEOS_ONLY, includeSelectedMedia = true)
        )
        assertEquals(3, GalleryPermissionPolicy.requiredPermissions(MediaTypeFilter.IMAGES_AND_VIDEOS, includeSelectedMedia = true).size)
    }
}
