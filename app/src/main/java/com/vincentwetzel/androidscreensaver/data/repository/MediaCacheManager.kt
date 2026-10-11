package com.vincentwetzel.androidscreensaver.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Keeps downloaded cloud media within the configured cache budget. */
object MediaCacheManager {
    private const val CLEANUP_INTERVAL_MS = 60_000L
    private val cacheDirectories = listOf(
        "drive_photos",
        "dropbox_photos",
        "dropbox_thumbnails",
        "onedrive_photos"
    )
    private var lastCleanupAtMs = 0L
    private var lastLimitBytes = -1L

    suspend fun trimIfNeeded(context: Context, limitBytes: Long, protectedFile: File?) = withContext(Dispatchers.IO) {
        synchronized(this@MediaCacheManager) {
            val now = System.currentTimeMillis()
            if (lastLimitBytes == limitBytes && now - lastCleanupAtMs < CLEANUP_INTERVAL_MS) return@synchronized

            trimDirectories(cacheDirectories.map { File(context.cacheDir, it) }, limitBytes, protectedFile)
            lastLimitBytes = limitBytes
            lastCleanupAtMs = now
        }
    }

    internal fun trimDirectories(directories: List<File>, limitBytes: Long, protectedFile: File?): Long {
        val files = directories.flatMap { dir ->
            dir.listFiles()?.filter { it.isFile && !it.name.contains(".tmp.") }.orEmpty()
        }
        var totalBytes = files.sumOf { it.length() }
        if (limitBytes < 0L || totalBytes <= limitBytes) return totalBytes

        val protectedPath = protectedFile?.absoluteFile?.normalize()
        files.sortedBy { it.lastModified() }.forEach { file ->
            if (totalBytes <= limitBytes) return@forEach
            val filePath = file.absoluteFile.normalize()
            val fileSize = file.length()
            if (protectedPath != filePath && file.delete()) {
                totalBytes -= fileSize
            }
        }
        return totalBytes
    }
}
