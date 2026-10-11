package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.data.repository.MediaCacheFilePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaCacheFilePolicyUnitTest {
    @Test
    fun `accepts simple media suffixes and drops provider path components`() {
        assertEquals("jpg", MediaCacheFilePolicy.extension("photo.jpg"))
        assertEquals("mp4", MediaCacheFilePolicy.extension("/folder/photo.mp4"))
        assertEquals("png", MediaCacheFilePolicy.extension("../../outside.png"))
    }

    @Test
    fun `rejects empty long and separator containing suffixes`() {
        assertNull(MediaCacheFilePolicy.extension("photo."))
        assertNull(MediaCacheFilePolicy.extension("photo.x/y"))
        assertNull(MediaCacheFilePolicy.extension("photo.abcdefghijkl"))
        assertNull(MediaCacheFilePolicy.extension(null))
    }

    @Test
    fun `cache keys preserve distinct provider identities and scope them to accounts`() {
        val nestedPathKey = MediaCacheFilePolicy.cacheKey("dropbox:account", "/album/photo.jpg")
        val underscorePathKey = MediaCacheFilePolicy.cacheKey("dropbox:account", "/album_photo.jpg")

        assertNotEquals(nestedPathKey, underscorePathKey)
        assertNotEquals(nestedPathKey, MediaCacheFilePolicy.cacheKey("other-account", "/album/photo.jpg"))
        assertTrue(nestedPathKey.matches(Regex("[0-9a-f]{64}")))
    }
}
