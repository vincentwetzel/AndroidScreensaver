package com.vincentwetzel.androidscreensaver.data.repository

import android.content.Context
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Abstract base repository for cloud sources (Google Drive, Dropbox).
 * Unifies thread-safe caching, prefetching, and account routing boilerplate.
 */
abstract class BaseCloudPhotoRepository(
    protected val context: Context,
    protected val dreamSourceType: com.vincentwetzel.androidscreensaver.dream.SourceType
) : AbstractPhotoRepository() {

    /**
     * Must return a list of active authenticated account IDs for this source.
     */
    abstract fun getAuthenticatedAccountIds(): List<String>

    // Abstract methods for API-specific network calls
    abstract suspend fun listFoldersForAccount(parentFolderId: String?, forceRefresh: Boolean, accountId: String): List<PhotoFolder>
    
    abstract suspend fun listPhotosForAccount(folderId: String, excludedFolderIds: Set<String>, mediaTypeFilter: MediaTypeFilter?, accountId: String): List<Photo>

    abstract suspend fun getFilteredFolderMediaCountForAccount(folderId: String, mediaTypeFilter: MediaTypeFilter?, accountId: String): Int

    abstract suspend fun getSubfolderIdsForAccount(folderId: String, accountId: String): List<String>

    abstract suspend fun getPhotoMetadataForAccount(photoId: String, accountId: String): Photo?

    abstract suspend fun getPhotoUrlForAccount(photoId: String, accountId: String): String?

    abstract suspend fun getThumbnailUrlForAccount(photoId: String, accountId: String): String?

    abstract suspend fun searchFoldersForAccount(query: String, accountId: String): List<PhotoFolder>

    // Unified Contract implementation (Requires explicit account routing)
    override suspend fun listFolders(parentFolderId: String?, forceRefresh: Boolean, accountId: String?): List<PhotoFolder> {
        if (accountId == null) return emptyList()
        return listFoldersForAccount(parentFolderId, forceRefresh, accountId)
    }

    override suspend fun listPhotos(folderId: String, excludedFolderIds: Set<String>, mediaTypeFilter: MediaTypeFilter?, accountId: String?): List<Photo> {
        if (accountId == null) return emptyList()
        return listPhotosForAccount(folderId, excludedFolderIds, mediaTypeFilter, accountId)
    }

    override suspend fun getFolderPhotoCount(folderId: String, accountId: String?): Int {
        return getFilteredFolderMediaCount(folderId, null, accountId)
    }

    override suspend fun getFilteredFolderMediaCount(folderId: String, mediaTypeFilter: MediaTypeFilter?, accountId: String?): Int {
        if (accountId == null) return 0
        return getFilteredFolderMediaCountForAccount(folderId, mediaTypeFilter, accountId)
    }

    override suspend fun getSubfolderIds(folderId: String, accountId: String?): List<String> {
        if (accountId == null) return emptyList()
        return getSubfolderIdsForAccount(folderId, accountId)
    }

    override suspend fun getPhotoMetadata(photoId: String, accountId: String?): Photo? {
        if (accountId == null) return null
        return getPhotoMetadataForAccount(photoId, accountId)
    }

    override suspend fun getPhotoUrl(photoId: String, accountId: String?): String? {
        if (accountId == null) return null
        return getPhotoUrlForAccount(photoId, accountId)
    }

    override suspend fun getThumbnailUrl(photoId: String, accountId: String?): String? {
        if (accountId == null) return null
        return getThumbnailUrlForAccount(photoId, accountId)
    }

    override suspend fun searchFolders(query: String, accountId: String?): List<PhotoFolder> {
        if (accountId == null) return emptyList()
        return searchFoldersForAccount(query, accountId)
    }

    // Shared background prefetching
    open fun prefetchRootFolders(mediaFilter: MediaTypeFilter? = null) {
        getAuthenticatedAccountIds().forEach { prefetchRootFolders(it, mediaFilter) }
    }

    fun prefetchRootFolders(accountId: String, mediaFilter: MediaTypeFilter? = null) {
        prefetchScope.launch {
            try {
                listFoldersForAccount(null, true, accountId)
                val account = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getAccount(context, dreamSourceType, accountId)
                val selectedIds = account?.selectedFolders?.map { it.folderId }?.toSet() ?: emptySet()
                if (selectedIds.isNotEmpty()) {
                    var totalCount = 0
                    val normalizedFilter = normalizeMediaFilter(mediaFilter)
                    selectedIds.forEach { folderId ->
                        val photos = listPhotosForAccount(folderId, account?.deselectedFolders ?: emptySet(), mediaFilter, accountId)
                        val count = photos.size
                        photoCountCache["${accountId}_${folderId}_${normalizedFilter}"] = CountCacheEntry(count)
                        totalCount += count
                    }
                    if (account != null && totalCount != account.photoCount) {
                        com.vincentwetzel.androidscreensaver.utils.SettingsManager.updateAccountPhotoCount(context, dreamSourceType, account.accountId, totalCount)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("BaseCloudPhotoRepo", "Prefetch failed for account: ${e.javaClass.simpleName}")
            }
        }
    }
}