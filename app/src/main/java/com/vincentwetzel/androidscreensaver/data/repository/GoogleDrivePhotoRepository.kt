package com.vincentwetzel.androidscreensaver.data.repository

import android.content.Context
import com.google.api.services.drive.Drive
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive implementation of PhotoRepository
 * Fetches photos and folders from Google Drive.
 * All operations are per-account — each method accepts an accountId to route to the correct Drive service.
 */
@Singleton
class GoogleDrivePhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val driveRepository: GoogleDriveRepository
) : PhotoRepository {

    // Background scope for prefetch operations that outlive individual callers
    private val prefetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        // Cache TTL: folders are considered stale after this many milliseconds
        // 60 seconds - balances snappy UX with detecting changes
        private const val FOLDER_CACHE_TTL_MS = 60_000L
        private const val PHOTO_COUNT_CACHE_TTL_MS = 300_000L // 5 minutes for counts
    }

    // Per-account cache: outer key is accountId, inner key is folder parent ID
    private data class CacheEntry<T>(val data: T, val timestampMs: Long = System.currentTimeMillis()) {
        val isStale: Boolean
            get() = System.currentTimeMillis() - timestampMs > FOLDER_CACHE_TTL_MS
    }
    private val folderCache = ConcurrentHashMap<String, ConcurrentHashMap<String, CacheEntry<List<PhotoFolder>>>>()
    private data class CountCacheEntry(val count: Int, val timestampMs: Long = System.currentTimeMillis()) {
        val isStale: Boolean
            get() = System.currentTimeMillis() - timestampMs > PHOTO_COUNT_CACHE_TTL_MS
    }
    private val photoCountCache = ConcurrentHashMap<String, ConcurrentHashMap<String, CountCacheEntry>>()
    
    private val driveImageQuery = "mimeType contains 'image/'"
    private val driveVideoQuery = "mimeType contains 'video/'"
    private val driveMediaMimeTypeQuery = "($driveImageQuery or $driveVideoQuery)"

    /**
     * Check if a specific account is authenticated.
     */
    fun isAccountAuthenticated(accountId: String): Boolean {
        return driveRepository.isAccountAuthenticated(accountId)
    }

    /**
     * Check if any account is authenticated (legacy compat for interface).
     */
    override fun isAuthenticated(): Boolean {
        return driveRepository.getAuthenticatedAccountIds().isNotEmpty()
    }

    /**
     * Download a photo from Google Drive with OAuth auth headers.
     * Returns a local cache file URI that Coil can load without auth.
     */
    suspend fun downloadPhotoToLocalCache(photoId: String, accountId: String, title: String? = null): String? {
        return withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "drive_photos")
                if (!cacheDir.exists()) cacheDir.mkdirs()

                val ext = title?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() } ?: "jpg"
                val cacheFile = File(cacheDir, "${accountId}_${photoId}.$ext")
                if (cacheFile.exists()) return@withContext "file://${cacheFile.absolutePath}"

                val driveService = driveRepository.getDriveService(accountId)
                    ?: return@withContext null

                val tempFile = File(cacheDir, "${accountId}_${photoId}.$ext.tmp.${java.util.UUID.randomUUID()}")
                try {
                    tempFile.outputStream().use { out ->
                        driveService.files().get(photoId).executeMediaAndDownloadTo(out)
                    }
                    if (!tempFile.renameTo(cacheFile) && !cacheFile.exists()) {
                        android.util.Log.e("GoogleDrivePhotoRepo", "Failed to rename temp file to cache file")
                        return@withContext null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GoogleDrivePhotoRepo", "Failed to download photo: ${e.message}")
                    return@withContext null
                } finally {
                    if (tempFile.exists()) tempFile.delete()
                }

                "file://${cacheFile.absolutePath}"
            } catch (e: Exception) {
                android.util.Log.e("GoogleDrivePhotoRepo", "Error downloading photo", e)
                null
            }
        }
    }

    /**
     * Pre-fetch root folders from Google Drive in the background for a specific account.
     */
    fun prefetchRootFolders(accountId: String) {
        prefetchScope.launch {
            try {
                // Re-use the existing folder listing logic which also updates the cache
                val folders = listFoldersForAccount(null, true, accountId)
                android.util.Log.d("GoogleDrivePhotoRepo", "Prefetched ${folders.size} root folders for $accountId")
            } catch (e: Exception) {
                android.util.Log.w("GoogleDrivePhotoRepo", "Prefetch failed for $accountId: ${e.message}")
            }
        }
    }

    /**
     * List folders for a specific account. Implements PhotoRepository interface (accountId from Photo).
     */
    override suspend fun listFolders(parentFolderId: String?, forceRefresh: Boolean): List<PhotoFolder> {
        // Default to first available account for interface compat
        val accountId = driveRepository.getAuthenticatedAccountIds().firstOrNull()
            ?: return emptyList()
        return listFoldersForAccount(parentFolderId, forceRefresh, accountId)
    }

    /**
     * List folders for a specific account.
     */
    suspend fun listFoldersForAccount(parentFolderId: String?, forceRefresh: Boolean, accountId: String): List<PhotoFolder> {
        val cacheKey = parentFolderId ?: "ROOT"
        val accountCache = getFolderCacheForAccount(accountId)
        val cached = accountCache[cacheKey]
        if (!forceRefresh && cached != null && !cached.isStale) {
            return cached.data
        }

        return withContext(Dispatchers.IO) {
            val driveService = driveRepository.getDriveService(accountId)
                ?: throw IllegalStateException("Not authenticated with Google Drive for account $accountId")

            val folders = mutableListOf<PhotoFolder>()

            try {
                val query = StringBuilder()
                query.append("mimeType='application/vnd.google-apps.folder'")
                query.append(" and trashed=false")

                if (parentFolderId != null) {
                    query.append(" and '$parentFolderId' in parents")
                } else {
                    query.append(" and 'root' in parents")
                }

                var nextPageToken: String? = null
                do {
                    val files = driveService.files().list()
                        .setQ(query.toString())
                        .setPageSize(1000)
                        .setFields("nextPageToken, files(id, name, parents)")
                        .setPageToken(nextPageToken)
                        .setOrderBy("name")
                        .execute()

                    files.files?.forEach { file ->
                        folders.add(
                            PhotoFolder(
                                id = file.id ?: "",
                                sourceType = SourceType.GOOGLE_DRIVE,
                                accountId = accountId,
                                name = file.name ?: "Unknown",
                                parentFolderId = file.parents?.firstOrNull(),
                                photoCount = 0
                            )
                        )
                    }
                    nextPageToken = files.nextPageToken
                } while (nextPageToken != null)
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception("Failed to list folders: ${e.message}")
            }

            folders.also { accountCache[cacheKey] = CacheEntry(it) }
        }
    }

    /**
     * List photos for a specific account (from PhotoRepository interface).
     */
    override suspend fun listPhotos(folderId: String, excludedFolderIds: Set<String>): List<Photo> {
        val accountId = driveRepository.getAuthenticatedAccountIds().firstOrNull()
            ?: return emptyList()
        return listPhotosForAccount(folderId, excludedFolderIds, accountId, null)
    }

    /**
     * List photos for a specific account.
     */
    suspend fun listPhotosForAccount(folderId: String, excludedFolderIds: Set<String>, accountId: String, mediaTypeFilter: String?): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()
        collectPhotosFromFolder(folderId, excludedFolderIds, photos, accountId, mediaTypeFilter)
        photos
    }

    /**
     * Recursively collects photos from a folder and all its subfolders for a specific account.
     */
    private fun collectPhotosFromFolder(folderId: String, excludedFolderIds: Set<String>, photos: MutableList<Photo>, accountId: String, mediaTypeFilter: String?) {
        val driveService = driveRepository.getDriveService(accountId)
            ?: throw IllegalStateException("Not authenticated with Google Drive for account $accountId")

        try {
            val mediaQuery = when (mediaTypeFilter) {
                "images" -> "($driveImageQuery)"
                "videos" -> "($driveVideoQuery)"
                else -> "($driveMediaMimeTypeQuery)"
            }
            val query = "(($mediaQuery) or mimeType='application/vnd.google-apps.folder') and trashed=false and '$folderId' in parents"

            var nextPageToken: String? = null
            val subfoldersToRecurse = mutableListOf<String>()

            do {
                val files = driveService.files().list()
                    .setQ(query)
                    .setPageSize(1000)
                    .setFields("nextPageToken, files(id, name, mimeType, size, modifiedTime, imageMediaMetadata, videoMediaMetadata, parents, thumbnailLink)")
                    .setPageToken(nextPageToken)
                    .setOrderBy("folder, name")
                    .execute()

                files.files?.forEach { file ->
                    if (file.mimeType == "application/vnd.google-apps.folder") {
                        if (file.id !in excludedFolderIds) {
                            subfoldersToRecurse.add(file.id)
                        }
                    } else {
                        val isVideo = file.mimeType?.startsWith("video/") == true
                        val originalExt = file.name?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() }
                        val ext = originalExt ?: if (isVideo) "mp4" else "jpg"
                        val finalTitle = if (originalExt != null) file.name else "${file.name}.$ext"
                        val cacheDir = File(context.cacheDir, "drive_photos")
                        val cacheFile = File(cacheDir, "${accountId}_${file.id}.$ext")
                        val uri = if (cacheFile.exists()) "file://${cacheFile.absolutePath}" else "https://www.googleapis.com/drive/v3/files/${file.id}?alt=media&accountId=$accountId"
                        val thumbnail = file.thumbnailLink
                        photos.add(
                            Photo(
                                id = file.id ?: "",
                                sourceType = SourceType.GOOGLE_DRIVE,
                                accountId = accountId,
                                uri = uri,
                                thumbnailUri = thumbnail,
                                title = finalTitle,
                                dateTaken = file.modifiedTime?.value,
                                width = if (isVideo) file.videoMediaMetadata?.width?.toInt() else file.imageMediaMetadata?.width?.toInt(),
                                height = if (isVideo) file.videoMediaMetadata?.height?.toInt() else file.imageMediaMetadata?.height?.toInt(),
                                fileSize = file.size?.toString()?.toLongOrNull()
                            )
                        )
                    }
                }

                nextPageToken = files.nextPageToken
            } while (nextPageToken != null)

            for (subfolderId in subfoldersToRecurse) {
                collectPhotosFromFolder(subfolderId, excludedFolderIds, photos, accountId, mediaTypeFilter)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to list photos: ${e.message}")
        }
    }

    override suspend fun getPhotoMetadata(photoId: String): Photo? = withContext(Dispatchers.IO) {
        val accountId = driveRepository.getAuthenticatedAccountIds().firstOrNull()
            ?: return@withContext null
        getPhotoMetadataForAccount(photoId, accountId)
    }

    /**
     * Get photo metadata for a specific account.
     */
    suspend fun getPhotoMetadataForAccount(photoId: String, accountId: String): Photo? = withContext(Dispatchers.IO) {
        val driveService = driveRepository.getDriveService(accountId)
            ?: return@withContext null

        try {
            val file = driveService.files().get(photoId)
                .setFields("id, name, mimeType, size, modifiedTime, imageMediaMetadata, videoMediaMetadata, thumbnailLink")
                .execute()

            val isVideo = file.mimeType?.startsWith("video/") == true
            val originalExt = file.name?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() }
            val ext = originalExt ?: if (isVideo) "mp4" else "jpg"
            val finalTitle = if (originalExt != null) file.name else "${file.name}.$ext"

            val cacheDir = File(context.cacheDir, "drive_photos")
            val cacheFile = File(cacheDir, "${accountId}_${file.id}.$ext")
            val uri = if (cacheFile.exists()) {
                "file://${cacheFile.absolutePath}"
            } else {
                "https://www.googleapis.com/drive/v3/files/${file.id}?alt=media&accountId=$accountId"
            }

            Photo(
                id = file.id ?: "",
                sourceType = SourceType.GOOGLE_DRIVE,
                accountId = accountId,
                uri = uri,
                thumbnailUri = file.thumbnailLink,
                title = finalTitle,
                dateTaken = file.modifiedTime?.value,
                width = if (isVideo) file.videoMediaMetadata?.width?.toInt() else file.imageMediaMetadata?.width?.toInt(),
                height = if (isVideo) file.videoMediaMetadata?.height?.toInt() else file.imageMediaMetadata?.height?.toInt(),
                fileSize = file.size?.toString()?.toLongOrNull()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getPhotoUrl(photoId: String): String? {
        val accountId = driveRepository.getAuthenticatedAccountIds().firstOrNull()
            ?: return null
        return "https://www.googleapis.com/drive/v3/files/$photoId?alt=media&accountId=$accountId"
    }

    override suspend fun getThumbnailUrl(photoId: String): String? {
        // Google Drive v3 API does not have a /thumbnail endpoint.
        // We must return the thumbnailLink from the file's metadata.
        return getPhotoMetadata(photoId)?.thumbnailUri
    }

    override suspend fun searchFolders(query: String): List<PhotoFolder> {
        val accountId = driveRepository.getAuthenticatedAccountIds().firstOrNull()
            ?: return emptyList()
        return searchFoldersForAccount(query, accountId)
    }

    /**
     * Search folders for a specific account.
     */
    suspend fun searchFoldersForAccount(query: String, accountId: String): List<PhotoFolder> = withContext(Dispatchers.IO) {
        val driveService = driveRepository.getDriveService(accountId)
            ?: return@withContext emptyList<PhotoFolder>()

        val folders = mutableListOf<PhotoFolder>()

        try {
            val safeQuery = query.replace("'", "\\'")
            val searchQuery = "mimeType='application/vnd.google-apps.folder' and name contains '$safeQuery' and trashed=false"

            var nextPageToken: String? = null
            do {
                val files = driveService.files().list()
                    .setQ(searchQuery)
                    .setPageSize(100)
                    .setFields("nextPageToken, files(id, name, parents)")
                    .setPageToken(nextPageToken)
                    .execute()

                files.files?.forEach { file ->
                    folders.add(
                        PhotoFolder(
                            id = file.id ?: "",
                            sourceType = SourceType.GOOGLE_DRIVE,
                            accountId = accountId,
                            name = file.name ?: "Unknown",
                            parentFolderId = file.parents?.firstOrNull(),
                            photoCount = 0
                        )
                    )
                }
                nextPageToken = files.nextPageToken
            } while (nextPageToken != null)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext folders
    }

    override suspend fun getFolderPhotoCount(folderId: String): Int {
        val accountId = driveRepository.getAuthenticatedAccountIds().firstOrNull()
            ?: return 0
        return getFolderPhotoCountForAccount(folderId, accountId)
    }

    /**
     * Get folder photo count for a specific account.
     */
    suspend fun getFolderPhotoCountForAccount(folderId: String, accountId: String): Int {
        val accountCountCache = getPhotoCountCacheForAccount(accountId)
        val cached = accountCountCache[folderId]
        if (cached != null && !cached.isStale) {
            return cached.count
        }

        return withContext(Dispatchers.IO) {
            try {
                // This is now recursive to match listPhotos, providing a correct count.
                // It ignores exclusions, which is consistent with other repository implementations.
                // The result is cached, so the expensive operation only runs periodically.
                val count = listPhotosForAccount(folderId, emptySet(), accountId, null).size
                accountCountCache[folderId] = CountCacheEntry(count)
                count
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }

    override suspend fun syncPhotos(): Boolean = withContext(Dispatchers.IO) {
        folderCache.clear()
        photoCountCache.clear()
        true 
    }

    override suspend fun getFilteredFolderMediaCount(folderId: String, mediaTypeFilter: String?): Int {
        val accountId = driveRepository.getAuthenticatedAccountIds().firstOrNull()
            ?: return 0
        return getFilteredFolderMediaCountForAccount(folderId, mediaTypeFilter, accountId)
    }

    /**
     * Get filtered folder media count for a specific account.
     */
    suspend fun getFilteredFolderMediaCountForAccount(folderId: String, mediaTypeFilter: String?, accountId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                // This is now recursive to match listPhotos, providing a correct count.
                // It is also efficient as it passes the filter to the API.
                listPhotosForAccount(folderId, emptySet(), accountId, mediaTypeFilter).size
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }

    // Helper to get or create per-account folder cache
    private fun getFolderCacheForAccount(accountId: String): ConcurrentHashMap<String, CacheEntry<List<PhotoFolder>>> {
        return folderCache.getOrPut(accountId) { ConcurrentHashMap() }
    }

    // Helper to get or create per-account photo count cache
    private fun getPhotoCountCacheForAccount(accountId: String): ConcurrentHashMap<String, CountCacheEntry> {
        return photoCountCache.getOrPut(accountId) { ConcurrentHashMap() }
    }
}