package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.data.repository.downloadOneDrivePhotoWithRefresh
import com.vincentwetzel.androidscreensaver.data.repository.oneDriveCacheExtension
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OneDriveDownloadPolicyUnitTest {

    @Test
    fun `successful cached URL avoids an extra Graph metadata lookup`() = runBlocking {
        var refreshCount = 0
        val result = downloadOneDrivePhotoWithRefresh(
            initialUrl = "https://download.example/valid",
            refreshUrl = { refreshCount++; "https://download.example/refreshed" },
            download = { true }
        )

        assertTrue(result)
        assertEquals(0, refreshCount)
    }

    @Test
    fun `expired URL retries with fresh Graph metadata URL`() = runBlocking {
        val attemptedUrls = mutableListOf<String>()
        val result = downloadOneDrivePhotoWithRefresh(
            initialUrl = "https://download.example/expired",
            refreshUrl = { "https://download.example/refreshed" },
            download = { url -> attemptedUrls.add(url); url.endsWith("refreshed") }
        )

        assertTrue(result)
        assertEquals(
            listOf("https://download.example/expired", "https://download.example/refreshed"),
            attemptedUrls
        )
    }

    @Test
    fun `unchanged or unavailable refreshed URL does not repeat the failed request`() = runBlocking {
        var attemptCount = 0
        val sameUrlResult = downloadOneDrivePhotoWithRefresh(
            initialUrl = "https://download.example/expired",
            refreshUrl = { "https://download.example/expired" },
            download = { attemptCount++; false }
        )
        val missingUrlResult = downloadOneDrivePhotoWithRefresh(
            initialUrl = null,
            refreshUrl = { null },
            download = { attemptCount++; false }
        )

        assertFalse(sameUrlResult)
        assertFalse(missingUrlResult)
        assertEquals(1, attemptCount)
    }

    @Test
    fun `cache filename retains safe media extension`() {
        assertEquals("mp4", oneDriveCacheExtension("movie.MP4"))
        assertEquals("jpg", oneDriveCacheExtension("photo.jpg"))
        assertEquals("bin", oneDriveCacheExtension("filename"))
        assertEquals("bin", oneDriveCacheExtension(null))
    }
}
