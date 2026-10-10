package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoOrder
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.dream.PhotoMediaClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoMediaClassifierUnitTest {

    @Test
    fun `classifies supported image and video extensions case insensitively`() {
        assertTrue(PhotoMediaClassifier.isImage(photo("holiday.JPEG")))
        assertTrue(PhotoMediaClassifier.isImage(photo("raw.heic")))
        assertTrue(PhotoMediaClassifier.isVideo(photo("clip.MP4")))
        assertTrue(PhotoMediaClassifier.isVideo(photo("movie.webm")))
        assertFalse(PhotoMediaClassifier.isImage(photo("notes.txt")))
        assertFalse(PhotoMediaClassifier.isVideo(photo("notes.txt")))
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

    private fun photo(uri: String, title: String? = uri, dateTaken: Long? = null, fileSize: Long? = null) = Photo(
        id = uri,
        sourceType = SourceType.GALLERY,
        uri = uri,
        title = title,
        dateTaken = dateTaken,
        fileSize = fileSize
    )
}
