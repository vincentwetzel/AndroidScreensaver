package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.utils.GalleryMediaDatePolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryMediaDatePolicyUnitTest {
    @Test
    fun `prefers capture timestamp and falls back to MediaStore added time`() {
        assertEquals(1_700_000_000_123L, GalleryMediaDatePolicy.dateTakenMillis(1_700_000_000_123L, 1_600_000_000L))
        assertEquals(1_600_000_000_000L, GalleryMediaDatePolicy.dateTakenMillis(0L, 1_600_000_000L))
        assertEquals(0L, GalleryMediaDatePolicy.dateTakenMillis(-1L, 0L))
    }
}