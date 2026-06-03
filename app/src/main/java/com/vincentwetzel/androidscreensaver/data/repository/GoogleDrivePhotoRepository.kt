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
import java.net.URLEncoder
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
    @ApplicationContext context: Context,
    private val driveRepository: GoogleDriveRepository
) : BaseCloudPhotoRepository(context, "source_google_drive") {

    private val driveImageQuery = "mimeType contains 'image/'"
    private val driveVideoQuery = "mimeType contains 'video/'"
    private val driveMediaMimeTypeQuery = "($driveImageQuery or $driveVideoQuery)"

    override fun getAuthenticatedAccountIds(): List<String> = driveRepository.getAuthenticatedAccountIds().toList()

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
                val safeAccountId = accountId.hashCode().toString()
                val safePhotoId = photoId.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
                val cacheFile = File(cacheDir, "${safeAccountId}_${safePhotoId}.$ext")
                if (cacheFile.exists()) return@withContext "file://${cacheFile.absolutePath}"

                val driveService = driveRepository.getDriveService(accountId)
                    ?: return@withContext null

                val tempFile = File(cacheDir, "${safeAccountId}_${safePhotoId}.$ext.tmp.${java.util.UUID.randomUUID()}")
                try {
                    tempFile.outputStream().use { out ->
                        driveService.files().get(photoId).executeMediaAndDownloadTo(out)
                    }
                    if (!tempFile.renameTo(cacheFile) && !cacheFile.exists()) {
                        android.util.Log.e("GoogleDrivePhotoRepo", "Failed to rename temp file to cache file")
                        return@withContext null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GoogleDrivePhotoRepo", "Failed to download photo")
                    return@withContext null
                } finally {
                    if (tempFile.exists()) tempFile.delete()
                }

                "file://${cacheFile.absolutePath}"
            } catch (e: Exception) {
                android.util.Log.e("GoogleDrivePhotoRepo", "Error downloading photo: ${e.javaClass.simpleName}")
                null
            }
        }
    }

    /**
     * List folders for a specific account.
     */
    override suspend fun listFoldersForAccount(parentFolderId: String?, forceRefresh: Boolean, accountId: String): List<PhotoFolder> {
        val cacheKey = "${accountId}_${parentFolderId ?: "ROOT"}"
        val cached = folderCache[cacheKey]
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
                    val safeParentId = parentFolderId.replace("'", "\\'")
                    query.append(" and '$safeParentId' in parents")
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
                        val fileId = file.id ?: return@forEach
                        folders.add(
                            PhotoFolder(
                                id = fileId,
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
        android.util.Log.e("GoogleDrivePhotoRepo", "Failed to list folders: ${e.javaClass.simpleName}")
            throw e
            }

            folders.also { folderCache[cacheKey] = CacheEntry(it) }
        }
    }

    /**
     * List photos for a specific account.
     */
    override suspend fun listPhotosForAccount(folderId: String, excludedFolderIds: Set<String>, mediaTypeFilter: String?, accountId: String): List<Photo> = withContext(Dispatchers.IO) {
        val normalizedFilter = normalizeMediaFilter(mediaTypeFilter)
        val exclusionsKey = if (excludedFolderIds.isEmpty()) "none" else excludedFolderIds.sorted().joinToString(",")
        val cacheKey = "${accountId}_${folderId}_${normalizedFilter}_${exclusionsKey}"
        val cached = photoListCache[cacheKey]
        if (cached != null && !cached.isStale) {
            return@withContext cached.data
        }

        val photos = mutableListOf<Photo>()
        collectPhotosFromFolder(folderId, excludedFolderIds, photos, accountId, normalizedFilter)
        
        photoListCache[cacheKey] = PhotoListCacheEntry(photos)
        photos
    }

    /**
     * Recursively collects photos from a folder and all its subfolders for a specific account.
     */
    private fun collectPhotosFromFolder(
        folderId: String,
        excludedFolderIds: Set<String>,
        photos: MutableList<Photo>,
        accountId: String,
        mediaTypeFilter: String?,
        visited: MutableSet<String> = mutableSetOf()
    ) {
        if (!visited.add(folderId)) return // Prevent infinite recursion from Drive shortcut loops
        val driveService = driveRepository.getDriveService(accountId)
            ?: throw IllegalStateException("Not authenticated with Google Drive for account $accountId")

        try {
            val mediaQuery = when (mediaTypeFilter) {
                "images" -> "($driveImageQuery)"
                "videos" -> "($driveVideoQuery)"
                else -> "($driveMediaMimeTypeQuery)"
            }
            val safeFolderId = folderId.replace("'", "\\'")
            val query = "(($mediaQuery) or mimeType='application/vnd.google-apps.folder') and trashed=false and '$safeFolderId' in parents"

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
                    val fileId = file.id ?: return@forEach
                    if (file.mimeType == "application/vnd.google-apps.folder") {
                        if (fileId !in excludedFolderIds) {
                            subfoldersToRecurse.add(fileId)
                        }
                    } else {
                        val isVideo = file.mimeType?.startsWith("video/") == true
                        val name = file.name ?: "Untitled"
                        val originalExt = name.substringAfterLast('.', "")
                        val ext = originalExt.takeIf { it.isNotEmpty() } ?: if (isVideo) "mp4" else "jpg"
                        val finalTitle = if (originalExt.isNotEmpty()) name else "$name.$ext"
                        
                        val encodedAccountId = URLEncoder.encode(accountId, "UTF-8")
                        val safeAccountId = accountId.hashCode().toString()
                        val safeFileId = fileId.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
                        val cacheDir = File(context.cacheDir, "drive_photos")
                        val cacheFile = File(cacheDir, "${safeAccountId}_${safeFileId}.$ext")
                        val uri = if (cacheFile.exists()) "file://${cacheFile.absolutePath}" else "https://www.googleapis.com/drive/v3/files/${fileId}?alt=media&accountId=$encodedAccountId"
                        val thumbnail = file.thumbnailLink
                        photos.add(
                            Photo(
                                id = fileId,
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
                collectPhotosFromFolder(subfolderId, excludedFolderIds, photos, accountId, mediaTypeFilter, visited)
            }

        } catch (e: Exception) {
            android.util.Log.e("GoogleDrivePhotoRepo", "Failed to list photos: ${e.javaClass.simpleName}")
            throw e
        }
    }

    /**
     * Get photo metadata for a specific account.
     */
    override suspend fun getPhotoMetadataForAccount(photoId: String, accountId: String): Photo? = withContext(Dispatchers.IO) {
        val driveService = driveRepository.getDriveService(accountId)
            ?: return@withContext null

        try {
            val file = driveService.files().get(photoId)
                .setFields("id, name, mimeType, size, modifiedTime, imageMediaMetadata, videoMediaMetadata, thumbnailLink")
                .execute()

            val isVideo = file.mimeType?.startsWith("video/") == true
            val name = file.name ?: "Untitled"
            val originalExt = name.substringAfterLast('.', "")
            val ext = originalExt.takeIf { it.isNotEmpty() } ?: if (isVideo) "mp4" else "jpg"
            val finalTitle = if (originalExt.isNotEmpty()) name else "$name.$ext"
            
            val encodedAccountId = URLEncoder.encode(accountId, "UTF-8")
            val safeAccountId = accountId.hashCode().toString()
            val safeFileId = file.id?.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_") ?: "unknown"
            val cacheDir = File(context.cacheDir, "drive_photos")
            val cacheFile = File(cacheDir, "${safeAccountId}_${safeFileId}.$ext")
            val uri = if (cacheFile.exists()) {
                "file://${cacheFile.absolutePath}"
            } else {
                "https://www.googleapis.com/drive/v3/files/${file.id}?alt=media&accountId=$encodedAccountId"
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
            android.util.Log.e("GoogleDrivePhotoRepo", "Failed to get photo metadata for $photoId")
            null
        }
    }

    override suspend fun getPhotoUrlForAccount(photoId: String, accountId: String): String? {
        val encodedAccountId = URLEncoder.encode(accountId, "UTF-8")
        return "https://www.googleapis.com/drive/v3/files/$photoId?alt=media&accountId=$encodedAccountId"
    }

    override suspend fun getThumbnailUrlForAccount(photoId: String, accountId: String): String? {
        // Google Drive v3 API does not have a /thumbnail endpoint.
        // We must return the thumbnailLink from the file's metadata.
        return getPhotoMetadataForAccount(photoId, accountId)?.thumbnailUri
    }

    /**
     * Search folders for a specific account.
     */
    override suspend fun searchFoldersForAccount(query: String, accountId: String): List<PhotoFolder> = withContext(Dispatchers.IO) {
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
                    val fileId = file.id ?: return@forEach
                    folders.add(
                        PhotoFolder(
                            id = fileId,
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
            android.util.Log.e("GoogleDrivePhotoRepo", "Failed to search folders: ${e.javaClass.simpleName}")
            throw e
        }

        return@withContext folders
    }

    /**
     * Get filtered folder media count for a specific account.
     */
    override suspend fun getFilteredFolderMediaCountForAccount(folderId: String, mediaTypeFilter: String?, accountId: String): Int {
        val normalizedFilter = normalizeMediaFilter(mediaTypeFilter)
        val cacheKey = "${accountId}_${folderId}_${normalizedFilter}"
        val cached = photoCountCache[cacheKey]
        if (cached != null && !cached.isStale) {
            return cached.count
        }

        // Instantly return 0 for cloud directories to prevent the UI from blocking for minutes.
        // Actual counting is deferred to background prefetching for selected folders.
        return 0
    }

    override suspend fun getSubfolderIdsForAccount(folderId: String, accountId: String): List<String> = withContext(Dispatchers.IO) {
        val driveService = driveRepository.getDriveService(accountId) ?: return@withContext emptyList()
        val result = mutableListOf<String>()
        collectSubfolderIds(folderId, driveService, result)
        result
    }

    private fun collectSubfolderIds(parentId: String, driveService: Drive, result: MutableList<String>, visited: MutableSet<String> = mutableSetOf()) {
        if (!visited.add(parentId)) return // Prevent infinite recursion from Drive shortcut loops
        try {
            val safeParentId = parentId.replace("'", "\\'")
            val query = "mimeType='application/vnd.google-apps.folder' and trashed=false and '$safeParentId' in parents"
            var nextPageToken: String? = null
            do {
                val files = driveService.files().list()
                    .setQ(query)
                    .setPageSize(1000)
                    .setFields("nextPageToken, files(id)")
                    .setPageToken(nextPageToken)
                    .execute()
                files.files?.forEach { file ->
                    file.id?.let {
                        result.add(it)
                        collectSubfolderIds(it, driveService, result, visited)
                    }
                }
                nextPageToken = files.nextPageToken
            } while (nextPageToken != null)
        } catch (e: Exception) {
            android.util.Log.e("GoogleDrivePhotoRepo", "Failed to get subfolders: ${e.javaClass.simpleName}")
            throw e
        }
    }

    override suspend fun syncPhotos(): Boolean {
        val success = super.syncPhotos()
        withContext(Dispatchers.IO) {
            try {
                File(context.cacheDir, "drive_photos").deleteRecursively()
            } catch (e: Exception) {
                android.util.Log.e("GoogleDrivePhotoRepo", "Failed to clear Google Drive disk cache: ${e.javaClass.simpleName}")
            }
        }
        return success
    }
}