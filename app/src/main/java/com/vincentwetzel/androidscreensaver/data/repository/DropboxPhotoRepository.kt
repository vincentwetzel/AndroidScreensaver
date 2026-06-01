package com.vincentwetzel.androidscreensaver.data.repository

import android.content.Context
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.FileMetadata
import android.util.Log
import com.dropbox.core.v2.files.ListFolderResult
import com.dropbox.core.v2.files.ThumbnailFormat
import com.dropbox.core.v2.files.ThumbnailSize
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Singleton
class DropboxPhotoRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val dropboxRepository: DropboxRepository
) : BaseCloudPhotoRepository(context, "source_dropbox") {

    companion object {
        private const val TAG = "DropboxPhotoRepository"

        private val dropboxImageExtensions = setOf(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "tiff", "tif", "svg"
        )
        private val dropboxVideoExtensions = setOf(
            "mp4", "mov", "avi", "wmv", "flv", "mkv", "webm", "m4v"
        )
    }

    override fun getAuthenticatedAccountIds(): List<String> = dropboxRepository.getAuthenticatedAccountIds().toList()

    // region PhotoRepository Interface Implementation
    override fun isAuthenticated(): Boolean {
        return dropboxRepository.getAuthenticatedAccountIds().isNotEmpty()
    }
    // endregion

    // region Dropbox-specific methods
    override suspend fun listFoldersForAccount(parentFolderId: String?, forceRefresh: Boolean, accountId: String): List<PhotoFolder> {
        val cacheKey = "${accountId}_${parentFolderId ?: "ROOT"}"
        val cached = folderCache[cacheKey]
        if (!forceRefresh && cached != null && !cached.isStale) {
            Log.d(TAG, "Returning cached folders for account $accountId, folder $parentFolderId")
            return cached.data
        }

        return withContext(Dispatchers.IO) {
            val client = dropboxRepository.getDbxClientV2(accountId)
                ?: throw IllegalStateException("Not authenticated with Dropbox for account $accountId")

            val folders = mutableListOf<PhotoFolder>()
            val path = parentFolderId ?: "" // Root is empty string for Dropbox API

            try {
                var result: ListFolderResult
                var cursor: String? = null

                do {
                    result = if (cursor == null) {
                        client.files().listFolder(path)
                    } else {
                        client.files().listFolderContinue(cursor)
                    }

                    for (entry in result.entries) {
                        if (entry is FolderMetadata) {
                            folders.add(
                                PhotoFolder(
                                    id = entry.pathLower, // Use pathLower as unique ID
                                    sourceType = SourceType.DROPBOX,
                                    accountId = accountId,
                                    name = entry.name,
                                    parentFolderId = entry.pathLower.substringBeforeLast("/").ifEmpty { null },
                                    photoCount = 0 // Will be updated later if needed
                                )
                            )
                        }
                    }
                    cursor = result.cursor
                } while (result.hasMore)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to list Dropbox folders for $accountId, path $path", e)
                throw e
            }

            folders.sortBy { it.name.lowercase() }
            folders.also { folderCache[cacheKey] = CacheEntry(it) }
        }
    }

    override suspend fun listPhotosForAccount(folderId: String, excludedFolderIds: Set<String>, mediaTypeFilter: String?, accountId: String): List<Photo> = withContext(Dispatchers.IO) {
        val normalizedFilter = normalizeMediaFilter(mediaTypeFilter)
        val cacheKey = "${accountId}_${folderId}_${normalizedFilter}_${excludedFolderIds.hashCode()}"
        val cached = photoListCache[cacheKey]
        if (cached != null && !cached.isStale) {
            return@withContext cached.data
        }

        val photos = mutableListOf<Photo>()
        // Dropbox paths start with '/'. Root is defined as empty string "".
        val rootPath = if (folderId.isEmpty()) "" else if (!folderId.startsWith("/")) "/$folderId" else folderId
        val client = dropboxRepository.getDbxClientV2(accountId)
            ?: throw IllegalStateException("Not authenticated with Dropbox for account $accountId")

        try {
            var result: ListFolderResult
            var cursor: String? = null

            do {
                result = if (cursor == null) {
                    client.files().listFolderBuilder(rootPath)
                        .withRecursive(true)
                        .start()
                } else {
                    client.files().listFolderContinue(cursor)
                }

                for (entry in result.entries) {
                    if (entry is FileMetadata) {
                        val isExcluded = excludedFolderIds.any { excludedId ->
                            entry.pathLower.startsWith("$excludedId/") || entry.pathLower == excludedId
                        }
                        if (isExcluded) continue

                        val extension = entry.name.substringAfterLast('.', "").lowercase()
                        val isImage = dropboxImageExtensions.contains(extension)
                        val isVideo = dropboxVideoExtensions.contains(extension)

                        val matchesFilter = when (normalizedFilter) {
                            "images" -> isImage
                            "videos" -> isVideo
                            else -> isImage || isVideo
                        }

                        if (matchesFilter) {
                            val safeFileName = entry.pathLower.replace("/", "_")
                            val photoCacheDir = File(context.cacheDir, "dropbox_photos")
                            val photoCacheFile = File(photoCacheDir, "${accountId}_$safeFileName")
                            val uri = if (photoCacheFile.exists()) {
                                "file://${photoCacheFile.absolutePath}"
                            } else {
                                entry.pathLower // Will be resolved to temporary link later
                            }

                            val thumbCacheDir = File(context.cacheDir, "dropbox_thumbnails")
                            val thumbCacheFile = File(thumbCacheDir, "${accountId}_${safeFileName}_thumb.jpeg")
                            val thumbnailUri = if (thumbCacheFile.exists()) {
                                "file://${thumbCacheFile.absolutePath}"
                            } else {
                                null // Will be resolved to temporary link later
                            }

                            val photo = Photo(
                                id = entry.pathLower, // Use pathLower as unique ID
                                sourceType = SourceType.DROPBOX,
                                accountId = accountId,
                                uri = uri,
                                thumbnailUri = thumbnailUri,
                                title = entry.name,
                                dateTaken = entry.clientModified.time,
                                width = null, // Dropbox API does not directly provide dimensions in FileMetadata
                                height = null,
                                fileSize = entry.size
                            )
                            photos.add(photo)
                        }
                    }
                }
                cursor = result.cursor
            } while (result.hasMore)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to collect Dropbox photos for $accountId, path $rootPath", e)
            throw e
        }

        photoListCache[cacheKey] = PhotoListCacheEntry(photos)
        photos
    }

    override suspend fun getPhotoMetadataForAccount(photoId: String, accountId: String): Photo? = withContext(Dispatchers.IO) {
        val client = dropboxRepository.getDbxClientV2(accountId)
            ?: return@withContext null

        try {
            val entry = client.files().getMetadata(photoId)
            if (entry is FileMetadata) {
                // Check for cached full photo
                val photoCacheDir = File(context.cacheDir, "dropbox_photos")
                val safeFileName = entry.pathLower.replace("/", "_")
                val photoCacheFile = File(photoCacheDir, "${accountId}_$safeFileName")
                val uri = if (photoCacheFile.exists()) {
                    "file://${photoCacheFile.absolutePath}"
                } else {
                    entry.pathLower // Default to path, to be resolved later
                }

                // Check for cached thumbnail
                val thumbCacheDir = File(context.cacheDir, "dropbox_thumbnails")
                val thumbCacheFile = File(thumbCacheDir, "${accountId}_${entry.pathLower.replace("/", "_")}_thumb.jpeg")
                val thumbnailUri = if (thumbCacheFile.exists()) {
                    "file://${thumbCacheFile.absolutePath}"
                } else {
                    null // Default to null, to be resolved later
                }

                return@withContext Photo(
                    id = entry.pathLower,
                    sourceType = SourceType.DROPBOX,
                    accountId = accountId,
                    uri = uri,
                    thumbnailUri = thumbnailUri,
                    title = entry.name,
                    dateTaken = entry.clientModified.time,
                    width = null,
                    height = null,
                    fileSize = entry.size
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get metadata for Dropbox photo $photoId, account $accountId", e)
        }
        null
    }

    override suspend fun getPhotoUrlForAccount(photoId: String, accountId: String): String? = withContext(Dispatchers.IO) {
        val client = dropboxRepository.getDbxClientV2(accountId)
            ?: return@withContext null

        try {
            val link = client.files().getTemporaryLink(photoId)
            return@withContext link.link
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get temporary link for Dropbox photo $photoId, account $accountId", e)
        }
        null
    }

    override suspend fun getThumbnailUrlForAccount(photoId: String, accountId: String): String? = withContext(Dispatchers.IO) {
        val client = dropboxRepository.getDbxClientV2(accountId)
            ?: return@withContext null

        // Dropbox's getTemporaryLink can provide the direct download, but for thumbnails,
        // we need to use getThumbnail. This returns a stream, not a URL.
        // So, we'll download the thumbnail to a local cache and return its URI.

        val cacheDir = File(context.cacheDir, "dropbox_thumbnails")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val cacheFile = File(cacheDir, "${accountId}_${photoId.replace("/", "_")}_thumb.jpeg")

        if (cacheFile.exists()) {
            return@withContext "file://${cacheFile.absolutePath}"
        }

        val tempFile = File(cacheDir, "${accountId}_${photoId.replace("/", "_")}_thumb.tmp.${java.util.UUID.randomUUID()}")
        try {
            tempFile.outputStream().use { out ->
                client.files().getThumbnailBuilder(photoId)
                    .withFormat(ThumbnailFormat.JPEG)
                    .withSize(ThumbnailSize.W256H256)
                    .start()
                    .download(out)
            }
            if (!tempFile.renameTo(cacheFile) && !cacheFile.exists()) {
                Log.e(TAG, "Failed to rename thumbnail temp file to cache file: ${cacheFile.absolutePath}")
                return@withContext null
            }
            return@withContext "file://${cacheFile.absolutePath}"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get thumbnail for Dropbox photo $photoId, account $accountId", e)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
        null
    }

    override suspend fun searchFoldersForAccount(query: String, accountId: String): List<PhotoFolder> = withContext(Dispatchers.IO) {
        val client = dropboxRepository.getDbxClientV2(accountId)
            ?: return@withContext emptyList()

        val folders = mutableListOf<PhotoFolder>()
        try {
            var result = client.files().searchV2Builder(query).withOptions(
                com.dropbox.core.v2.files.SearchOptions.newBuilder()
                    .withPath("") // Search entire Dropbox
                    .withFileStatus(com.dropbox.core.v2.files.FileStatus.ACTIVE)
                    .withMaxResults(100L)
                    .build()
            ).withIncludeHighlights(false).start()

            while (true) {
                result.matches.forEach { match ->
                    val entry = match.metadata.metadataValue
                    if (entry is FolderMetadata) {
                        folders.add(
                            PhotoFolder(
                                id = entry.pathLower,
                                sourceType = SourceType.DROPBOX,
                                accountId = accountId,
                                name = entry.name,
                                parentFolderId = entry.pathLower.substringBeforeLast("/").ifEmpty { null },
                                photoCount = 0
                            )
                        )
                    }
                }
                if (result.hasMore) {
                    result = client.files().searchContinueV2(result.cursor)
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search Dropbox folders for account $accountId, query '$query'", e)
        }
        folders
    }

    override suspend fun getFilteredFolderMediaCountForAccount(folderId: String, mediaTypeFilter: String?, accountId: String): Int {
        val normalizedFilter = normalizeMediaFilter(mediaTypeFilter)
        val cacheKey = "${accountId}_${folderId}_${normalizedFilter}"
        val cached = photoCountCache[cacheKey]
        if (cached != null && !cached.isStale) {
            return cached.count
        }
        return 0 // Instantly return 0 to prevent the UI from blocking. Actual counting is done in prefetch.
    }

    suspend fun downloadPhotoToLocalCache(photoId: String, accountId: String): String? = withContext(Dispatchers.IO) {
        val client = dropboxRepository.getDbxClientV2(accountId)
            ?: return@withContext null

        try {
            val cacheDir = File(context.cacheDir, "dropbox_photos")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            // Use the full path with safe character replacement to prevent collisions
            val safeFileName = photoId.replace("/", "_")
            val cacheFile = File(cacheDir, "${accountId}_$safeFileName")

            if (cacheFile.exists()) {
                Log.d(TAG, "Photo already in cache: ${cacheFile.absolutePath}")
                return@withContext "file://${cacheFile.absolutePath}"
            }

            val tempFile = File(cacheDir, "${accountId}_${safeFileName}.tmp.${java.util.UUID.randomUUID()}")
            try {
                tempFile.outputStream().use { out ->
                    client.files().downloadBuilder(photoId).start().download(out)
                }
                if (!tempFile.renameTo(cacheFile) && !cacheFile.exists()) {
                    Log.e(TAG, "Failed to rename temp file to cache file: ${cacheFile.absolutePath}")
                    return@withContext null
                }
            } finally {
                if (tempFile.exists()) tempFile.delete()
            }

            Log.d(TAG, "Downloaded Dropbox photo to local cache: ${cacheFile.absolutePath}")
            "file://${cacheFile.absolutePath}"
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading Dropbox photo $photoId to local cache", e)
            null
        }
    }

    // endregion

    override suspend fun getSubfolderIdsForAccount(folderId: String, accountId: String): List<String> = withContext(Dispatchers.IO) {
        val client = dropboxRepository.getDbxClientV2(accountId) ?: return@withContext emptyList()
        val subfolders = mutableListOf<String>()
        val rootPath = if (folderId.isEmpty()) "" else if (!folderId.startsWith("/")) "/$folderId" else folderId

        try {
            var result = client.files().listFolderBuilder(rootPath).withRecursive(true).start()
            while (true) {
                for (entry in result.entries) {
                    if (entry is FolderMetadata) {
                        subfolders.add(entry.pathLower)
                    }
                }
                if (!result.hasMore) break
                result = client.files().listFolderContinue(result.cursor)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get subfolders for Dropbox folder $folderId", e)
        }
        subfolders
    }
}
