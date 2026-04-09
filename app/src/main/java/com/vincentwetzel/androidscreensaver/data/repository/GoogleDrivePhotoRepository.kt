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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive implementation of PhotoRepository
 * Fetches photos and folders from Google Drive
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

    /**
     * Download a photo from Google Drive with OAuth auth headers
     * Returns a local cache file URI that Coil can load without auth
     */
    suspend fun downloadPhotoToLocalCache(photoId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Get access token from the repository
                val accessToken = driveRepository.getAccessToken()
                    ?: return@withContext null

                // Download photo with OAuth header
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

                // Save to local cache
                val cacheDir = File(context.cacheDir, "drive_photos")
                if (!cacheDir.exists()) cacheDir.mkdirs()

                val cacheFile = File(cacheDir, "$photoId.jpg")
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

    // Cache entry with timestamp for TTL-based expiration
    private data class CacheEntry<T>(val data: T, val timestampMs: Long = System.currentTimeMillis()) {
        val isStale: Boolean
            get() = System.currentTimeMillis() - timestampMs > FOLDER_CACHE_TTL_MS
    }

    // Cache for loaded folders (survives Activity recreation)
    private val folderCache = mutableMapOf<String?, CacheEntry<List<PhotoFolder>>>()

    // Cache for folder photo counts (survives Activity recreation)
    private data class CountCacheEntry(val count: Int, val timestampMs: Long = System.currentTimeMillis()) {
        val isStale: Boolean
            get() = System.currentTimeMillis() - timestampMs > PHOTO_COUNT_CACHE_TTL_MS
    }
    private val photoCountCache = mutableMapOf<String, CountCacheEntry>()

    // Supported image file extensions
    private val imageExtensions = listOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "tiff", "tif"
    )

    // Supported video file extensions
    private val videoExtensions = listOf(
        "mp4", "avi", "mov", "mkv", "webm", "wmv", "flv", "m4v"
    )

    override fun isAuthenticated(): Boolean {
        return driveRepository.isAuthenticated.value
    }

    /**
     * Pre-fetch root folders from Google Drive in the background.
     * Results are cached so subsequent listFolders(null) calls return immediately.
     * Safe to call multiple times — respects the folder cache TTL.
     */
    fun prefetchRootFolders() {
        prefetchScope.launch {
            try {
                val driveService = driveRepository.getDriveService()
                    ?: return@launch // Not authenticated yet

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
                            name = file.name ?: "Unknown",
                            parentFolderId = file.parents?.firstOrNull(),
                            photoCount = 0
                        )
                    )
                }

                folderCache[null] = CacheEntry(folders)
                android.util.Log.d("GoogleDrivePhotoRepo", "Prefetched ${folders.size} root folders")
            } catch (e: Exception) {
                android.util.Log.w("GoogleDrivePhotoRepo", "Prefetch failed: ${e.message}")
            }
        }
    }

    override suspend fun listFolders(parentFolderId: String?, forceRefresh: Boolean): List<PhotoFolder> {
        // Check cache first (only if not forcing refresh and cache is fresh)
        val cached = folderCache[parentFolderId]
        if (!forceRefresh && cached != null && !cached.isStale) {
            return cached.data
        }

        return withContext(Dispatchers.IO) {
            val driveService = driveRepository.getDriveService()
                ?: throw IllegalStateException("Not authenticated with Google Drive")

            val folders = mutableListOf<PhotoFolder>()

            try {
                // Build query for folders
                val query = StringBuilder()
                query.append("mimeType='application/vnd.google-apps.folder'")
                query.append(" and trashed=false")

                if (parentFolderId != null) {
                    query.append(" and '$parentFolderId' in parents")
                } else {
                    // Root folder
                    query.append(" and 'root' in parents")
                }

                // Execute the query
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
                            name = file.name ?: "Unknown",
                            parentFolderId = file.parents?.firstOrNull(),
                            photoCount = 0 // Will be populated separately
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception("Failed to list folders: ${e.message}")
            }

            folders.also { folderCache[parentFolderId] = CacheEntry(it) }
        }
    }

    override suspend fun listPhotos(folderId: String, excludedFolderIds: Set<String>): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()
        collectPhotosFromFolder(folderId, excludedFolderIds, photos)
        photos
    }

    /**
     * Recursively collects photos from a folder and all its subfolders,
     * skipping any folders in the excludedFolderIds set.
     */
    private fun collectPhotosFromFolder(folderId: String, excludedFolderIds: Set<String>, photos: MutableList<Photo>) {
        val driveService = driveRepository.getDriveService()
            ?: throw IllegalStateException("Not authenticated with Google Drive")

        try {
            // Build query for images and videos
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
                    .setFields("nextPageToken, files(id, name, mimeType, size, modifiedTime, imageMediaMetadata, videoMediaMetadata, parents)")
                    .setPageToken(nextPageToken)
                    .setOrderBy("folder, name")
                    .execute()

                files.files?.forEach { file ->
                    val isVideo = videoExtensions.any { ext ->
                        file.name?.endsWith(".$ext", ignoreCase = true) == true
                    }

                    val mimeType = file.mimeType ?: if (isVideo) "video/mp4" else "image/jpeg"

                    photos.add(
                        Photo(
                            id = file.id ?: "",
                            sourceType = SourceType.GOOGLE_DRIVE,
                            uri = "", // Will be populated when needed
                            thumbnailUri = null,
                            title = file.name,
                            dateTaken = file.modifiedTime?.value,
                            width = if (isVideo) {
                                file.videoMediaMetadata?.width?.toInt()
                            } else {
                                file.imageMediaMetadata?.width?.toInt()
                            },
                            height = if (isVideo) {
                                file.videoMediaMetadata?.height?.toInt()
                            } else {
                                file.imageMediaMetadata?.height?.toInt()
                            },
                            fileSize = file.size?.toString()?.toLongOrNull()
                        )
                    )
                }

                nextPageToken = files.nextPageToken
            } while (nextPageToken != null)

            // Now recurse into subfolders
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

                // Recurse into each subfolder (skip excluded ones)
                for (subfolderId in subfolderIds) {
                    if (subfolderId !in excludedFolderIds) {
                        collectPhotosFromFolder(subfolderId, excludedFolderIds, photos)
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
        val driveService = driveRepository.getDriveService()
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
                uri = "",
                thumbnailUri = null,
                title = file.name,
                dateTaken = file.modifiedTime?.value,
                width = if (isVideo) {
                    file.videoMediaMetadata?.width?.toInt()
                } else {
                    file.imageMediaMetadata?.width?.toInt()
                },
                height = if (isVideo) {
                    file.videoMediaMetadata?.height?.toInt()
                } else {
                    file.imageMediaMetadata?.height?.toInt()
                },
                fileSize = file.size?.toString()?.toLongOrNull()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getPhotoUrl(photoId: String): String? = withContext(Dispatchers.IO) {
        val driveService = driveRepository.getDriveService()
            ?: return@withContext null

        try {
            // Get a download URL that works with authenticated requests
            "https://www.googleapis.com/drive/v3/files/$photoId?alt=media"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getThumbnailUrl(photoId: String): String? = withContext(Dispatchers.IO) {
        val driveService = driveRepository.getDriveService()
            ?: return@withContext null

        try {
            // Google Drive provides thumbnails at this endpoint
            "https://www.googleapis.com/drive/v3/files/$photoId/thumbnail?sz=w400"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun searchFolders(query: String): List<PhotoFolder> = withContext(Dispatchers.IO) {
        val driveService = driveRepository.getDriveService()
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
        val cached = photoCountCache[folderId]
        if (cached != null && !cached.isStale) {
            return cached.count
        }

        return withContext(Dispatchers.IO) {
            val driveService = driveRepository.getDriveService()
                ?: return@withContext 0

            return@withContext try {
                val mimeTypeQuery = imageExtensions.joinToString(" or ") { ext ->
                    "name contains '.$ext'"
                }
                val videoQuery = videoExtensions.joinToString(" or ") { ext ->
                    "name contains '.$ext'"
                }
                val query = "($mimeTypeQuery or $videoQuery) and trashed=false and '$folderId' in parents"

                // Paginate through all matching files to get the real count
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

                photoCountCache[folderId] = CountCacheEntry(total)
                total
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }

    override suspend fun syncPhotos(): Boolean = withContext(Dispatchers.IO) {
        // For Google Drive, we don't need to sync locally
        // We fetch photos on-demand from the API
        true
    }

    override suspend fun getFilteredFolderMediaCount(folderId: String, mediaTypeFilter: String?): Int {
        return withContext(Dispatchers.IO) {
            val driveService = driveRepository.getDriveService()
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

                // Paginate through all matching files to get the real count
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
}