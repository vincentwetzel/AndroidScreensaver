package com.vincentwetzel.androidscreensaver.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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

/**
 * Gallery implementation of PhotoRepository
 * Uses MediaStore API to access local device photos
 */
@Singleton
class GalleryPhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : PhotoRepository {

    // Background scope for prefetch operations that outlive individual callers
    private val prefetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        // Cache TTL: folders are considered stale after this many milliseconds
        // 60 seconds - balances snappy UX with detecting changes
        private const val FOLDER_CACHE_TTL_MS = 60_000L
        private const val PHOTO_COUNT_CACHE_TTL_MS = 300_000L // 5 minutes for counts
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
    private val imageMimeTypes = setOf(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp",
        "image/heic", "image/heif", "image/svg+xml", "image/tiff", "image/x-ms-bmp"
    )

    // Supported video file extensions
    private val videoMimeTypes = setOf(
        "video/mp4", "video/avi", "video/quicktime", "video/x-matroska",
        "video/webm", "video/x-ms-wmv", "video/x-flv", "video/mp4v-es"
    )

    override fun isAuthenticated(): Boolean {
        // Gallery doesn't need authentication
        return true
    }

    /**
     * Pre-fetch Gallery folders (MediaStore buckets) in the background.
     * Results are cached so subsequent listFolders(null) calls return immediately.
     * Safe to call multiple times — respects the folder cache TTL.
     */
    fun prefetchRootFolders() {
        prefetchScope.launch {
            try {
                val folders = mutableListOf<PhotoFolder>()

                val contentResolver = context.contentResolver
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Files.getContentUri("external")
                }

                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.BUCKET_ID,
                    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Files.FileColumns.MEDIA_TYPE
                )

                val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
                val selectionArgs = arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
                )

                val sortOrder = "${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} ASC"

                val cursor = contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use {
                    val bucketIdIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
                    val bucketNameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

                    val seenBuckets = mutableSetOf<String>()

                    while (it.moveToNext()) {
                        val bucketId = it.getString(bucketIdIndex)
                        if (seenBuckets.contains(bucketId)) continue
                        seenBuckets.add(bucketId)

                        val bucketName = it.getString(bucketNameIndex) ?: "Unknown"

                        folders.add(
                            PhotoFolder(
                                id = bucketId,
                                sourceType = SourceType.GALLERY,
                                name = bucketName,
                                parentFolderId = null,
                                photoCount = 0
                            )
                        )
                    }
                }

                folders.sortBy { it.name.lowercase() }
                folderCache[null] = CacheEntry(folders)
                android.util.Log.d("GalleryPhotoRepo", "Prefetched ${folders.size} Gallery folders")
            } catch (e: Exception) {
                android.util.Log.w("GalleryPhotoRepo", "Prefetch failed: ${e.message}")
            }
        }
    }

    /**
     * Helper: Get the RELATIVE_PATH for a folder (bucket) ID.
     * Used to include subfolder photos when listing photos for a folder.
     */
    private fun getFolderRelativePath(folderId: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        try {
            val contentResolver = context.contentResolver
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val projection = arrayOf(MediaStore.Files.FileColumns.RELATIVE_PATH)
            val selection = "${MediaStore.Files.FileColumns.BUCKET_ID} = ?"
            val selectionArgs = arrayOf(folderId)

            val cursor = contentResolver.query(collection, projection, selection, selectionArgs, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val pathIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)
                    return it.getString(pathIndex)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GalleryPhotoRepo", "Failed to get relative path for folder $folderId: ${e.message}")
        }
        return null
    }

    override suspend fun listFolders(parentFolderId: String?, forceRefresh: Boolean): List<PhotoFolder> {
        // Check cache first (only if not forcing refresh and cache is fresh)
        val cached = folderCache[parentFolderId]
        if (!forceRefresh && cached != null && !cached.isStale) {
            return cached.data
        }

        return withContext(Dispatchers.IO) {
            val folders = mutableListOf<PhotoFolder>()

            try {
                val contentResolver = context.contentResolver
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Files.getContentUri("external")
                }

                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.BUCKET_ID,
                    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Files.FileColumns.MEDIA_TYPE
                )

                val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
                val selectionArgs = arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
                )

                val sortOrder = "${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} ASC"

                val cursor = contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use {
                    val bucketIdIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
                    val bucketNameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

                    val seenBuckets = mutableSetOf<String>()

                    while (it.moveToNext()) {
                        val bucketId = it.getString(bucketIdIndex)
                        if (seenBuckets.contains(bucketId)) continue
                        seenBuckets.add(bucketId)

                        val bucketName = it.getString(bucketNameIndex) ?: "Unknown"

                        folders.add(
                            PhotoFolder(
                                id = bucketId,
                                sourceType = SourceType.GALLERY,
                                name = bucketName,
                                parentFolderId = null,
                                photoCount = 0
                            )
                        )
                    }
                }

                // Sort folders by name
                folders.sortBy { it.name.lowercase() }
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception("Failed to list folders: ${e.message}")
            }

            folders.also { folderCache[parentFolderId] = CacheEntry(it) }
        }
    }

    override suspend fun listPhotos(folderId: String, excludedFolderIds: Set<String>): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()

        try {
            val contentResolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            // First, get the folder's relative path to include subfolders
            val folderRelativePath = getFolderRelativePath(folderId)

            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.WIDTH,
                MediaStore.Files.FileColumns.HEIGHT,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.BUCKET_ID,
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATA
            )

            // Build exclusion paths from deselected folder IDs
            val excludedRelativePaths = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                excludedFolderIds.mapNotNull { getFolderRelativePath(it) }
            } else {
                emptyList()
            }

            val (selection, selectionArgs) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && folderRelativePath != null) {
                if (excludedRelativePaths.isNotEmpty()) {
                    // Include photos from this folder AND subfolders, minus excluded paths
                    val excludeClause = excludedRelativePaths.joinToString(" AND ") { path ->
                        "${MediaStore.Files.FileColumns.RELATIVE_PATH} NOT LIKE ?"
                    }
                    val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?) AND ($excludeClause)"
                    val excludeArgs = excludedRelativePaths.map { "$it%" }
                    selection to arrayOf(folderRelativePath, MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()) + excludeArgs.toTypedArray()
                } else {
                    // Include photos from this folder AND all subfolders using RELATIVE_PATH
                    val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
                    selection to arrayOf("$folderRelativePath%", MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
                }
            } else {
                // Fallback: only exact bucket match (Android < 10 or no RELATIVE_PATH available)
                val selection = "${MediaStore.Files.FileColumns.BUCKET_ID} = ? AND ${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
                selection to arrayOf(folderId, MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
            }

            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

            val cursor = contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val titleIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE)
                val dateAddedIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val mediaTypeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val widthIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
                val heightIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
                val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeTypeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dataIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val mediaType = it.getInt(mediaTypeIndex)
                    val uri = ContentUris.withAppendedId(
                        if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        else
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val width = it.getLong(widthIndex).toInt()
                    val height = it.getLong(heightIndex).toInt()
                    val mimeType = it.getString(mimeTypeIndex)
                    val localPath = it.getString(dataIndex)

                    photos.add(
                        Photo(
                            id = id.toString(),
                            sourceType = SourceType.GALLERY,
                            uri = uri.toString(),
                            thumbnailUri = uri.toString(),
                            title = it.getString(titleIndex) ?: it.getString(nameIndex),
                            dateTaken = it.getLong(dateAddedIndex) * 1000, // Convert to milliseconds
                            width = width,
                            height = height,
                            fileSize = it.getLong(sizeIndex),
                            cachedLocalPath = localPath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to list photos: ${e.message}")
        }

        return@withContext photos
    }

    override suspend fun getPhotoMetadata(photoId: String): Photo? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.WIDTH,
                MediaStore.Files.FileColumns.HEIGHT,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATA
            )

            val selection = "${MediaStore.Files.FileColumns._ID} = ?"
            val selectionArgs = arrayOf(photoId)

            val cursor = contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val titleIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE)
                    val dateAddedIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                    val mediaTypeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val widthIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
                    val heightIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
                    val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    val mimeTypeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                    val dataIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                    val id = it.getLong(idIndex)
                    val mediaType = it.getInt(mediaTypeIndex)
                    val uri = ContentUris.withAppendedId(
                        if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        else
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val localPath = it.getString(dataIndex)

                    return@withContext Photo(
                        id = id.toString(),
                        sourceType = SourceType.GALLERY,
                        uri = uri.toString(),
                        thumbnailUri = uri.toString(),
                        title = it.getString(titleIndex) ?: it.getString(nameIndex),
                        dateTaken = it.getLong(dateAddedIndex) * 1000,
                        width = it.getLong(widthIndex).toInt(),
                        height = it.getLong(heightIndex).toInt(),
                        fileSize = it.getLong(sizeIndex),
                        cachedLocalPath = localPath
                    )
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getPhotoUrl(photoId: String): String? = withContext(Dispatchers.IO) {
        // For Gallery, return the content URI directly
        try {
            val uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                photoId.toLong()
            )
            uri.toString()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getThumbnailUrl(photoId: String): String? = withContext(Dispatchers.IO) {
        // For Gallery, return the same URI (MediaStore provides thumbnails automatically)
        try {
            val uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                photoId.toLong()
            )
            uri.toString()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun searchFolders(query: String): List<PhotoFolder> = withContext(Dispatchers.IO) {
        val allFolders = listFolders(null, false)
        allFolders.filter { it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun getFolderPhotoCount(folderId: String): Int {
        // Check cache first (only if fresh)
        val cached = photoCountCache[folderId]
        if (cached != null && !cached.isStale) {
            return cached.count
        }

        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Files.getContentUri("external")
                }

                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID
                )

                val selection = "${MediaStore.Files.FileColumns.BUCKET_ID} = ? AND ${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
                val selectionArgs = arrayOf(
                    folderId,
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
                )

                val cursor = contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )

                cursor?.use {
                    val count = it.count
                    photoCountCache[folderId] = CountCacheEntry(count)
                    count
                } ?: 0
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }

    override suspend fun syncPhotos(): Boolean = withContext(Dispatchers.IO) {
        // Gallery doesn't need to sync
        true
    }

    override suspend fun getFilteredFolderMediaCount(folderId: String, mediaTypeFilter: String?): Int {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver

                // Query the appropriate MediaStore collection based on filter
                val (collection, bucketIdColumn) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val volume = MediaStore.VOLUME_EXTERNAL
                    when (mediaTypeFilter) {
                        "images" -> MediaStore.Images.Media.getContentUri(volume) to MediaStore.Images.Media.BUCKET_ID
                        "videos" -> MediaStore.Video.Media.getContentUri(volume) to MediaStore.Video.Media.BUCKET_ID
                        else -> MediaStore.Files.getContentUri(volume) to MediaStore.Files.FileColumns.BUCKET_ID
                    }
                } else {
                    when (mediaTypeFilter) {
                        "images" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI to MediaStore.Images.Media.BUCKET_ID
                        "videos" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI to MediaStore.Video.Media.BUCKET_ID
                        else -> MediaStore.Files.getContentUri("external") to MediaStore.Files.FileColumns.BUCKET_ID
                    }
                }

                val projection = arrayOf("_id")
                val selection = "$bucketIdColumn = ?"
                val selectionArgs = arrayOf(folderId)

                val cursor = contentResolver.query(collection, projection, selection, selectionArgs, null)
                cursor?.use { it.count } ?: 0
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }
}
