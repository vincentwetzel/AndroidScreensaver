package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.data.repository.MediaCacheManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MediaCacheManagerUnitTest {
    @Test
    fun `evicts oldest media while preserving the item just downloaded`() {
        val root = Files.createTempDirectory("media-cache").toFile()
        try {
            val drive = File(root, "drive_photos").apply { mkdirs() }
            val oldFile = File(drive, "old.jpg").apply { writeBytes(ByteArray(6)) }
            val activeFile = File(drive, "active.jpg").apply { writeBytes(ByteArray(7)) }
            val partialDownload = File(drive, "active.tmp.123").apply { writeBytes(ByteArray(100)) }
            oldFile.setLastModified(1_000L)
            activeFile.setLastModified(2_000L)

            val sizeAfterTrim = MediaCacheManager.trimDirectories(listOf(drive), 7L, activeFile)

            assertEquals(7L, sizeAfterTrim)
            assertFalse(oldFile.exists())
            assertTrue(activeFile.exists())
            assertTrue(partialDownload.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `leaves media alone when cache is under limit or unlimited`() {
        val root = Files.createTempDirectory("media-cache").toFile()
        try {
            val file = File(root, "cached.jpg").apply { writeBytes(ByteArray(5)) }
            assertEquals(5L, MediaCacheManager.trimDirectories(listOf(root), 5L, null))
            assertEquals(5L, MediaCacheManager.trimDirectories(listOf(root), Long.MAX_VALUE, null))
            assertTrue(file.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
