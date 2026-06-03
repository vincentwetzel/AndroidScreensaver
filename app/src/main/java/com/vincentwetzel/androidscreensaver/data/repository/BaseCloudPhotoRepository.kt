package com.vincentwetzel.androidscreensaver.data.repository

import android.content.Context
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Abstract base repository for cloud sources (Google Drive, Dropbox).
 * Unifies thread-safe caching, prefetching, and account routing boilerplate.
 */
abstract class BaseCloudPhotoRepository(
    protected val context: Context,
    protected val sourceType: String // e.g., "source_google_drive", "source_dropbox"
) : AbstractPhotoRepository() {

    /**
     * Must return a list of active authenticated account IDs for this source.
     */
    abstract fun getAuthenticatedAccountIds(): List<String>

    // Abstract methods for API-specific network calls
    abstract suspend fun listFoldersForAccount(parentFolderId: String?, forceRefresh: Boolean, accountId: String): List<PhotoFolder>
    
    abstract suspend fun listPhotosForAccount(folderId: String, excludedFolderIds: Set<String>, mediaTypeFilter: String?, accountId: String): List<Photo>

    abstract suspend fun getFilteredFolderMediaCountForAccount(folderId: String, mediaTypeFilter: String?, accountId: String): Int

    abstract suspend fun getSubfolderIdsForAccount(folderId: String, accountId: String): List<String>

    abstract suspend fun getPhotoMetadataForAccount(photoId: String, accountId: String): Photo?

    abstract suspend fun getPhotoUrlForAccount(photoId: String, accountId: String): String?

    abstract suspend fun getThumbnailUrlForAccount(photoId: String, accountId: String): String?

    abstract suspend fun searchFoldersForAccount(query: String, accountId: String): List<PhotoFolder>

    // Unified Contract implementation (Routing to first available account)
    override suspend fun listFolders(parentFolderId: String?, forceRefresh: Boolean, accountId: String?): List<PhotoFolder> {
        val resolvedAccountId = accountId ?: getAuthenticatedAccountIds().firstOrNull() ?: return emptyList()
        return listFoldersForAccount(parentFolderId, forceRefresh, resolvedAccountId)
    }

    override suspend fun listPhotos(folderId: String, excludedFolderIds: Set<String>, mediaTypeFilter: String?, accountId: String?): List<Photo> {
        val resolvedAccountId = accountId ?: getAuthenticatedAccountIds().firstOrNull() ?: return emptyList()
        return listPhotosForAccount(folderId, excludedFolderIds, mediaTypeFilter, resolvedAccountId)
    }

    override suspend fun getFolderPhotoCount(folderId: String, accountId: String?): Int {
        return getFilteredFolderMediaCount(folderId, null, accountId)
    }

    override suspend fun getFilteredFolderMediaCount(folderId: String, mediaTypeFilter: String?, accountId: String?): Int {
        val resolvedAccountId = accountId ?: getAuthenticatedAccountIds().firstOrNull() ?: return 0
        return getFilteredFolderMediaCountForAccount(folderId, mediaTypeFilter, resolvedAccountId)
    }

    override suspend fun getSubfolderIds(folderId: String, accountId: String?): List<String> {
        val resolvedAccountId = accountId ?: getAuthenticatedAccountIds().firstOrNull() ?: return emptyList()
        return getSubfolderIdsForAccount(folderId, resolvedAccountId)
    }

    override suspend fun getPhotoMetadata(photoId: String, accountId: String?): Photo? {
        val resolvedAccountId = accountId ?: getAuthenticatedAccountIds().firstOrNull() ?: return null
        return getPhotoMetadataForAccount(photoId, resolvedAccountId)
    }

    override suspend fun getPhotoUrl(photoId: String, accountId: String?): String? {
        val resolvedAccountId = accountId ?: getAuthenticatedAccountIds().firstOrNull() ?: return null
        return getPhotoUrlForAccount(photoId, resolvedAccountId)
    }

    override suspend fun getThumbnailUrl(photoId: String, accountId: String?): String? {
        val resolvedAccountId = accountId ?: getAuthenticatedAccountIds().firstOrNull() ?: return null
        return getThumbnailUrlForAccount(photoId, resolvedAccountId)
    }

    override suspend fun searchFolders(query: String, accountId: String?): List<PhotoFolder> {
        val resolvedAccountId = accountId ?: getAuthenticatedAccountIds().firstOrNull() ?: return emptyList()
        return searchFoldersForAccount(query, resolvedAccountId)
    }

    // Shared background prefetching
    open fun prefetchRootFolders(mediaFilter: String? = null) {
        getAuthenticatedAccountIds().forEach { prefetchRootFolders(it, mediaFilter) }
    }

    fun prefetchRootFolders(accountId: String, mediaFilter: String? = null) {
        prefetchScope.launch {
            try {
                listFoldersForAccount(null, true, accountId)
                val dreamSourceType = when(sourceType) {
                    "source_google_drive" -> com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE
                    "source_dropbox" -> com.vincentwetzel.androidscreensaver.dream.SourceType.DROPBOX
                    else -> return@launch
                }
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
                android.util.Log.w("BaseCloudPhotoRepo", "Prefetch failed for account: ${e.javaClass.simpleName}")
            }
        }
    }
}