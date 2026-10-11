package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoOrder
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.dream.PhotoMediaClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoMediaClassifierUnitTest {

    @Test
    fun `classifies supported image and video extensions case insensitively`() {
        assertTrue(PhotoMediaClassifier.isImage(photo("holiday.JPEG")))
        assertTrue(PhotoMediaClassifier.isImage(photo("raw.heic")))
        assertTrue(PhotoMediaClassifier.isVideo(photo("clip.MP4")))
        assertTrue(PhotoMediaClassifier.isVideo(photo("movie.webm")))
        assertTrue(PhotoMediaClassifier.isVideo(photo("mobile.3gp")))
        assertTrue(PhotoMediaClassifier.isVideo(photo("recording.m2ts")))
        assertTrue(PhotoMediaClassifier.isVideo(photo("broadcast.ts")))
        assertFalse(PhotoMediaClassifier.isImage(photo("notes.txt")))
        assertFalse(PhotoMediaClassifier.isVideo(photo("notes.txt")))
    }

    @Test
    fun `media filter keeps only matching media types`() {
        val image = photo("holiday.jpg")
        val video = photo("clip.mp4")

        assertTrue(PhotoMediaClassifier.matchesFilter(image, MediaTypeFilter.IMAGES_ONLY))
        assertFalse(PhotoMediaClassifier.matchesFilter(video, MediaTypeFilter.IMAGES_ONLY))
        assertTrue(PhotoMediaClassifier.matchesFilter(video, MediaTypeFilter.VIDEOS_ONLY))
        assertFalse(PhotoMediaClassifier.matchesFilter(image, MediaTypeFilter.VIDEOS_ONLY))
        assertTrue(PhotoMediaClassifier.matchesFilter(image, MediaTypeFilter.IMAGES_AND_VIDEOS))
        assertTrue(PhotoMediaClassifier.matchesFilter(video, MediaTypeFilter.IMAGES_AND_VIDEOS))
    }

    @Test
    fun `classifies MediaStore content URIs even without file extensions`() {
        assertTrue(PhotoMediaClassifier.isImage(photo("content://media/external/images/media/42", title = null)))
        assertTrue(PhotoMediaClassifier.isVideo(photo("content://media/external/video/media/43", title = null)))
        assertTrue(PhotoMediaClassifier.isImage(photo("content://media_type_image/44", title = null)))
        assertTrue(PhotoMediaClassifier.isVideo(photo("content://media_type_video/45", title = null)))
    }

    @Test
    fun `title takes precedence over a misleading URI extension`() {
        assertTrue(PhotoMediaClassifier.isImage(photo("remote/item", title = "photo.png")))
        assertFalse(PhotoMediaClassifier.isVideo(photo("remote/item.mp4", title = "photo.png")))
    }

    @Test
    fun `sort keys support date name and size orders with null defaults`() {
        val photo = photo("remote", title = "Zebra", dateTaken = 100L, fileSize = 500L)

        assertEquals(-100L, PhotoMediaClassifier.sortKey(photo, PhotoOrder.DATE_NEWEST_FIRST))
        assertEquals(100L, PhotoMediaClassifier.sortKey(photo, PhotoOrder.DATE_OLDEST_FIRST))
        assertEquals("Zebra", PhotoMediaClassifier.sortKey(photo, PhotoOrder.NAME_A_Z))
        assertEquals(-500L, PhotoMediaClassifier.sortKey(photo, PhotoOrder.SIZE_LARGEST_FIRST))
        assertEquals(500L, PhotoMediaClassifier.sortKey(photo, PhotoOrder.SIZE_SMALLEST_FIRST))
        assertEquals(0, PhotoMediaClassifier.sortKey(photo, PhotoOrder.RANDOM))

        val missing = photo("remote", title = null, dateTaken = null, fileSize = null)
        assertEquals("", PhotoMediaClassifier.sortKey(missing, PhotoOrder.NAME_A_Z))
        assertEquals(0L, PhotoMediaClassifier.sortKey(missing, PhotoOrder.DATE_OLDEST_FIRST))
    }

    @Test
    fun `descending name key orders names from z to a`() {
        val names = listOf("alpha", "zulu", "middle").map { photo("remote", title = it) }
        val sorted = names.sortedWith(compareBy { PhotoMediaClassifier.sortKey(it, PhotoOrder.NAME_Z_A) })

        assertEquals(listOf("zulu", "middle", "alpha"), sorted.map { it.title })
    }

    @Test
    fun `media filter enum retains all explicit modes`() {
        assertEquals(3, MediaTypeFilter.values().size)
    }

    @Test
    fun `deduplication keeps distinct account and media ID tuples`() {
        val first = Photo(
            id = "gamma",
            sourceType = SourceType.GALLERY,
            accountId = "alpha_beta",
            uri = "content://first"
        )
        val second = Photo(
            id = "beta_gamma",
            sourceType = SourceType.GALLERY,
            accountId = "alpha",
            uri = "content://second"
        )

        assertEquals("${first.sourceType}_${first.accountId}_${first.id}", "${second.sourceType}_${second.accountId}_${second.id}")
        assertNotEquals(PhotoMediaClassifier.mediaKey(first), PhotoMediaClassifier.mediaKey(second))
        assertEquals(PhotoMediaClassifier.mediaKey(first), PhotoMediaClassifier.mediaKey(first.copy(uri = "content://duplicate")))
    }

    @Test
    fun `classifies cloud videos from filenames when download URLs are opaque`() {
        val driveVideo = Photo(
            id = "drive-id",
            sourceType = SourceType.GOOGLE_DRIVE,
            uri = "https://www.googleapis.com/drive/v3/files/drive-id?alt=media&accountId=user%40example.com",
            title = "vacation.mp4"
        )
        val oneDriveVideo = Photo(
            id = "onedrive-id",
            sourceType = SourceType.ONEDRIVE,
            uri = "https://download.example/content?auth=temporary-token",
            title = "movie.webm"
        )

        assertTrue(PhotoMediaClassifier.isVideo(driveVideo))
        assertTrue(PhotoMediaClassifier.isVideo(oneDriveVideo))
    }
    @Test
    fun `requires local download for uncached Dropbox paths and remote Drive URLs`() {
        val dropboxPhoto = Photo(
            id = "/camera/holiday.jpg",
            sourceType = SourceType.DROPBOX,
            uri = "/camera/holiday.jpg",
            title = "holiday.jpg"
        )
        val cachedDropboxPhoto = dropboxPhoto.copy(uri = "file:///cache/holiday.jpg")
        val drivePhoto = Photo(
            id = "drive-id",
            sourceType = SourceType.GOOGLE_DRIVE,
            uri = "https://www.googleapis.com/drive/v3/files/drive-id?alt=media",
            title = "holiday.jpg"
        )
        val oneDrivePhoto = Photo(
            id = "onedrive-id",
            sourceType = SourceType.ONEDRIVE,
            uri = "https://download.example/content?auth=temporary-token",
            title = "holiday.jpg"
        )

        assertTrue(PhotoMediaClassifier.requiresLocalDownload(dropboxPhoto))
        assertFalse(PhotoMediaClassifier.requiresLocalDownload(cachedDropboxPhoto))
        assertTrue(PhotoMediaClassifier.requiresLocalDownload(drivePhoto))
        assertTrue(PhotoMediaClassifier.requiresLocalDownload(oneDrivePhoto))
    }
    @Test
    fun `minimum duration rule waits for known duration and honors disabled threshold`() {
        assertFalse(PhotoMediaClassifier.isBelowMinimumVideoDuration(-1L, 10))
        assertFalse(PhotoMediaClassifier.isBelowMinimumVideoDuration(500L, 0))
        assertTrue(PhotoMediaClassifier.isBelowMinimumVideoDuration(9_999L, 10))
        assertFalse(PhotoMediaClassifier.isBelowMinimumVideoDuration(10_000L, 10))
    }
    private fun photo(uri: String, title: String? = uri, dateTaken: Long? = null, fileSize: Long? = null) = Photo(
        id = uri,
        sourceType = SourceType.GALLERY,
        uri = uri,
        title = title,
        dateTaken = dateTaken,
        fileSize = fileSize
    )
}
