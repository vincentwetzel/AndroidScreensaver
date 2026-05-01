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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
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

        private val httpClient = OkHttpClient()
    }

    // Per-account cache: outer key is accountId, inner key is folder parent ID
    private data class CacheEntry<T>(val data: T, val timestampMs: Long = System.currentTimeMillis()) {
        val isStale: Boolean
            get() = System.currentTimeMillis() - timestampMs > FOLDER_CACHE_TTL_MS
    }
    private val folderCache = mutableMapOf<String, MutableMap<String?, CacheEntry<List<PhotoFolder>>>>()
    private data class CountCacheEntry(val count: Int, val timestampMs: Long = System.currentTimeMillis()) {
        val isStale: Boolean
            get() = System.currentTimeMillis() - timestampMs > PHOTO_COUNT_CACHE_TTL_MS
    }
    private val photoCountCache = mutableMapOf<String, MutableMap<String, CountCacheEntry>>()

    private val thumbnailCache = mutableMapOf<String, String>()
    
    // Supported image file extensions
    private val imageExtensions = listOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "tiff", "tif"
    )

    // Supported video file extensions
    private val videoExtensions = listOf(
        "mp4", "avi", "mov", "mkv", "webm", "wmv", "flv", "m4v"
    )

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
    suspend fun downloadPhotoToLocalCache(photoId: String, accountId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val accessToken = driveRepository.getAccessToken(accountId)
                    ?: return@withContext null

                val url = "https://www.googleapis.com/drive/v3/files/$photoId?alt=media"
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful || response.body == null) {
                    android.util.Log.e("GoogleDrivePhotoRepo", "Failed to download photo: ${response.code}")
                    return@withContext null
                }

                val cacheDir = File(context.cacheDir, "drive_photos")
                if (!cacheDir.exists()) cacheDir.mkdirs()

                val cacheFile = File(cacheDir, "${accountId}_${photoId}.jpg")
                cacheFile.outputStream().use { out ->
                    response.body!!.byteStream().use { input ->
                        input.copyTo(out)
                    }
                }

                cacheFile.absolutePath
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
                val driveService = driveRepository.getDriveService(accountId)
                    ?: return@launch

                val folders = mutableListOf<PhotoFolder>()
                val query = "mimeType='application/vnd.google-apps.folder' and trashed=false and 'root' in parents"

                val files = driveService.files().list()
                    .setQ(query)
                    .setPageSize(1000)
                    .setFields("nextPageToken, files(id, name, parents)")
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

                getFolderCacheForAccount(accountId)[null] = CacheEntry(folders)
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
        val accountCache = getFolderCacheForAccount(accountId)
        val cached = accountCache[parentFolderId]
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

                val files = driveService.files().list()
                    .setQ(query.toString())
                    .setPageSize(1000)
                    .setFields("nextPageToken, files(id, name, parents)")
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
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception("Failed to list folders: ${e.message}")
            }

            folders.also { accountCache[parentFolderId] = CacheEntry(it) }
        }
    }

    /**
     * List photos for a specific account (from PhotoRepository interface).
     */
    override suspend fun listPhotos(folderId: String, excludedFolderIds: Set<String>): List<Photo> {
        val accountId = driveRepository.getAuthenticatedAccountIds().firstOrNull()
            ?: return emptyList()
        return listPhotosForAccount(folderId, excludedFolderIds, accountId)
    }

    /**
     * List photos for a specific account.
     */
    suspend fun listPhotosForAccount(folderId: String, excludedFolderIds: Set<String>, accountId: String): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()
        collectPhotosFromFolder(folderId, excludedFolderIds, photos, accountId)
        photos
    }

    /**
     * Recursively collects photos from a folder and all its subfolders for a specific account.
     */
    private fun collectPhotosFromFolder(folderId: String, excludedFolderIds: Set<String>, photos: MutableList<Photo>, accountId: String) {
        val driveService = driveRepository.getDriveService(accountId)
            ?: throw IllegalStateException("Not authenticated with Google Drive for account $accountId")

        try {
            val mimeTypeQuery = imageExtensions.joinToString(" or ") { ext ->
                "name contains '.$ext'"
            }
            val videoQuery = videoExtensions.joinToString(" or ") { ext ->
                "name contains '.$ext'"
            }

            val query = "($mimeTypeQuery or $videoQuery) and trashed=false and '$folderId' in parents"

            var nextPageToken: String? = null

            do {
                val files = driveService.files().list()
                    .setQ(query)
                    .setPageSize(1000)
                    .setFields("nextPageToken, files(id, name, mimeType, size, modifiedTime, imageMediaMetadata, videoMediaMetadata, parents, thumbnailLink)")
                    .setPageToken(nextPageToken)
                    .setOrderBy("folder, name")
                    .execute()

                files.files?.forEach { file ->
                    val isVideo = videoExtensions.any { ext ->
                        file.name?.endsWith(".$ext", ignoreCase = true) == true
                    }
                    
                    val cacheDir = File(context.cacheDir, "drive_photos")
                    val cacheFile = File(cacheDir, "${accountId}_${file.id}.jpg")
                    val uri = if (cacheFile.exists()) {
                        cacheFile.absolutePath
                    } else {
                        "https://www.googleapis.com/drive/v3/files/${file.id}?alt=media"
                    }
                    
                    val thumbnail = file.thumbnailLink ?: thumbnailCache[file.id]
                    if (thumbnail != null) {
                        thumbnailCache[file.id] = thumbnail
                    }

                    photos.add(
                        Photo(
                            id = file.id ?: "",
                            sourceType = SourceType.GOOGLE_DRIVE,
                            accountId = accountId,
                            uri = uri,
                            thumbnailUri = thumbnail,
                            title = file.name,
                            dateTaken = file.modifiedTime?.value,
                            width = if (isVideo) file.videoMediaMetadata?.width?.toInt() else file.imageMediaMetadata?.width?.toInt(),
                            height = if (isVideo) file.videoMediaMetadata?.height?.toInt() else file.imageMediaMetadata?.height?.toInt(),
                            fileSize = file.size?.toString()?.toLongOrNull()
                        )
                    )
                }

                nextPageToken = files.nextPageToken
            } while (nextPageToken != null)

            val subfolderQuery = "mimeType='application/vnd.google-apps.folder' and trashed=false and '$folderId' in parents"
            var subfolderPageToken: String? = null

            do {
                val subfolderFiles = driveService.files().list()
                    .setQ(subfolderQuery)
                    .setPageSize(1000)
                    .setFields("nextPageToken, files(id, name, parents)")
                    .setPageToken(subfolderPageToken)
                    .execute()

                val subfolderIds = subfolderFiles.files?.mapNotNull { it.id } ?: emptyList()

                for (subfolderId in subfolderIds) {
                    if (subfolderId !in excludedFolderIds) {
                        collectPhotosFromFolder(subfolderId, excludedFolderIds, photos, accountId)
                    }
                }

                subfolderPageToken = subfolderFiles.nextPageToken
            } while (subfolderPageToken != null)

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
                .setFields("id, name, mimeType, size, modifiedTime, imageMediaMetadata, videoMediaMetadata")
                .execute()

            val isVideo = videoExtensions.any { ext ->
                file.name?.endsWith(".$ext", ignoreCase = true) == true
            }

            Photo(
                id = file.id ?: "",
                sourceType = SourceType.GOOGLE_DRIVE,
                accountId = accountId,
                uri = "",
                thumbnailUri = null,
                title = file.name,
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
        return "https://www.googleapis.com/drive/v3/files/$photoId?alt=media"
    }

    override suspend fun getThumbnailUrl(photoId: String): String? {
        val accountId = driveRepository.getAuthenticatedAccountIds().firstOrNull()
            ?: return null
        return "https://www.googleapis.com/drive/v3/files/$photoId/thumbnail?sz=w400"
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
            val searchQuery = "mimeType='application/vnd.google-apps.folder' and name contains '$query' and trashed=false"

            val files = driveService.files().list()
                .setQ(searchQuery)
                .setPageSize(100)
                .setFields("files(id, name, parents)")
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
            val driveService = driveRepository.getDriveService(accountId)
                ?: return@withContext 0

            return@withContext try {
                val mimeTypeQuery = imageExtensions.joinToString(" or ") { ext ->
                    "name contains '.$ext'"
                }
                val videoQuery = videoExtensions.joinToString(" or ") { ext ->
                    "name contains '.$ext'"
                }
                val query = "($mimeTypeQuery or $videoQuery) and trashed=false and '$folderId' in parents"

                var total = 0
                var nextPageToken: String? = null
                do {
                    val files = driveService.files().list()
                        .setQ(query)
                        .setPageSize(1000)
                        .setFields("nextPageToken, files(id)")
                        .setPageToken(nextPageToken)
                        .execute()

                    total += files.files?.size ?: 0
                    nextPageToken = files.nextPageToken
                } while (nextPageToken != null)

                accountCountCache[folderId] = CountCacheEntry(total)
                total
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }

    override suspend fun syncPhotos(): Boolean = withContext(Dispatchers.IO) { true }

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
            val driveService = driveRepository.getDriveService(accountId)
                ?: return@withContext 0

            return@withContext try {
                val query = when (mediaTypeFilter) {
                    "images" -> {
                        val mimeTypeQuery = imageExtensions.joinToString(" or ") { ext ->
                            "name contains '.$ext'"
                        }
                        "($mimeTypeQuery) and trashed=false and '$folderId' in parents"
                    }
                    "videos" -> {
                        val videoQuery = videoExtensions.joinToString(" or ") { ext ->
                            "name contains '.$ext'"
                        }
                        "($videoQuery) and trashed=false and '$folderId' in parents"
                    }
                    else -> {
                        val mimeTypeQuery = imageExtensions.joinToString(" or ") { ext ->
                            "name contains '.$ext'"
                        }
                        val videoQuery = videoExtensions.joinToString(" or ") { ext ->
                            "name contains '.$ext'"
                        }
                        "($mimeTypeQuery or $videoQuery) and trashed=false and '$folderId' in parents"
                    }
                }

                var total = 0
                var nextPageToken: String? = null
                do {
                    val files = driveService.files().list()
                        .setQ(query)
                        .setPageSize(1000)
                        .setFields("nextPageToken, files(id)")
                        .setPageToken(nextPageToken)
                        .execute()

                    total += files.files?.size ?: 0
                    nextPageToken = files.nextPageToken
                } while (nextPageToken != null)

                total
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }

    // Helper to get or create per-account folder cache
    private fun getFolderCacheForAccount(accountId: String): MutableMap<String?, CacheEntry<List<PhotoFolder>>> {
        return folderCache.getOrPut(accountId) { mutableMapOf() }
    }

    // Helper to get or create per-account photo count cache
    private fun getPhotoCountCacheForAccount(accountId: String): MutableMap<String, CountCacheEntry> {
        return photoCountCache.getOrPut(accountId) { mutableMapOf() }
    }
}