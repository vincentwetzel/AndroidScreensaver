package com.vincentwetzel.androidscreensaver.data.repository

import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Base class providing standardized thread-safe caching maps,
 * TTL logic, and cache sync functionality for all Photo Repositories.
 */
abstract class AbstractPhotoRepository : PhotoRepository {

    // Shared background scope for prefetch operations that outlive individual callers
    protected val prefetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Thread-safe generic caches
    protected val folderCache = ConcurrentHashMap<String, CacheEntry<List<PhotoFolder>>>()
    protected val photoListCache = ConcurrentHashMap<String, PhotoListCacheEntry>()
    protected val photoCountCache = ConcurrentHashMap<String, CountCacheEntry>()

    companion object {
        const val FOLDER_CACHE_TTL_MS = 60 * 1000L // 1 minute
        const val PHOTO_COUNT_CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }

    protected data class CacheEntry<T>(val data: T, val timestampMs: Long = System.currentTimeMillis()) {
        val isStale: Boolean get() = System.currentTimeMillis() - timestampMs > FOLDER_CACHE_TTL_MS
    }
    protected data class CountCacheEntry(val count: Int, val timestampMs: Long = System.currentTimeMillis()) {
        val isStale: Boolean get() = System.currentTimeMillis() - timestampMs > PHOTO_COUNT_CACHE_TTL_MS
    }
    protected data class PhotoListCacheEntry(val data: List<Photo>, val timestampMs: Long = System.currentTimeMillis()) {
        val isStale: Boolean get() = System.currentTimeMillis() - timestampMs > PHOTO_COUNT_CACHE_TTL_MS
    }

    override suspend fun syncPhotos(): Boolean = withContext(Dispatchers.IO) {
        folderCache.clear()
        photoCountCache.clear()
        photoListCache.clear()
        true
    }

    protected fun normalizeMediaFilter(mediaTypeFilter: MediaTypeFilter?): String {
        return when (mediaTypeFilter) {
            MediaTypeFilter.IMAGES_ONLY -> "images"
            MediaTypeFilter.VIDEOS_ONLY -> "videos"
            else -> "all"
        }
    }
}