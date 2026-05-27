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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

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
    private val folderCache = ConcurrentHashMap<String, CacheEntry<List<PhotoFolder>>>()

    // Cache for folder photo counts (survives Activity recreation)
    private data class CountCacheEntry(val count: Int, val timestampMs: Long = System.currentTimeMillis()) {
        val isStale: Boolean
            get() = System.currentTimeMillis() - timestampMs > PHOTO_COUNT_CACHE_TTL_MS
    }
    private val photoCountCache = ConcurrentHashMap<String, CountCacheEntry>()

    private data class PhotoListCacheEntry(val data: List<Photo>, val timestampMs: Long = System.currentTimeMillis()) {
        val isStale: Boolean
            get() = System.currentTimeMillis() - timestampMs > PHOTO_COUNT_CACHE_TTL_MS
    }
    private val photoListCache = ConcurrentHashMap<String, PhotoListCacheEntry>()

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
    fun prefetchRootFolders(mediaFilter: String? = null) {
        prefetchScope.launch {
            try {
                // Re-use existing listFolders logic which populates the cache
                val folders = listFolders(null, forceRefresh = true)
                android.util.Log.d("GalleryPhotoRepo", "Prefetched ${folders.size} Gallery folders")
                
                val account = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getAccountsForSource(
                    context, com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY
                ).firstOrNull()
                
                val selectedIds = account?.selectedFolders?.map { it.folderId }?.toSet() ?: emptySet()
                
                if (selectedIds.isNotEmpty()) {
                    var totalCount = 0
                    selectedIds.forEach { folderId ->
                        val photos = listPhotos(folderId, account?.deselectedFolders ?: emptySet(), mediaFilter)
                        val count = photos.size
                        val cacheKey = "${folderId}_${mediaFilter ?: "all"}"
                        photoCountCache[cacheKey] = CountCacheEntry(count)
                        totalCount += count
                    }
                    if (account != null && totalCount != account.photoCount) {
                        com.vincentwetzel.androidscreensaver.utils.SettingsManager.saveAccount(
                            context, account.copy(photoCount = totalCount)
                        )
                    }
                } else {
                    folders.forEach { folder ->
                        getFilteredFolderMediaCount(folder.id, mediaFilter)
                    }
                }
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
        // MediaStore buckets are flat. There are no sub-buckets.
        if (parentFolderId != null) return emptyList()

        val cacheKey = "ROOT"
        // Check cache first (only if not forcing refresh and cache is fresh)
        val cached = folderCache[cacheKey]
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

                // Sort folders by name (case-insensitive without extra string allocations)
                folders.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception("Failed to list folders: ${e.message}")
            }

            folders.also { folderCache[cacheKey] = CacheEntry(it) }
        }
    }

    override suspend fun listPhotos(folderId: String, excludedFolderIds: Set<String>, mediaTypeFilter: String?): List<Photo> = withContext(Dispatchers.IO) {
        val cacheKey = "${folderId}_${mediaTypeFilter}_${excludedFolderIds.hashCode()}"
        val cached = photoListCache[cacheKey]
        if (cached != null && !cached.isStale) {
            return@withContext cached.data
        }

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

            // Build exclusion paths concurrently
            val excludedRelativePaths = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                excludedFolderIds.map { id ->
                    async { getFolderRelativePath(id) }
                }.awaitAll().filterNotNull()
            } else {
                emptyList()
            }
            
            val imageType = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString()
            val videoType = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            
            val mediaTypeSelection = when(mediaTypeFilter) {
                "images" -> "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
                "videos" -> "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
                else -> "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
            }
            val mediaTypeArgs = when(mediaTypeFilter) {
                "images" -> arrayOf(imageType)
                "videos" -> arrayOf(videoType)
                else -> arrayOf(imageType, videoType)
            }

            val (selection, selectionArgs) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && folderRelativePath != null) {
                if (excludedRelativePaths.isNotEmpty()) {
                    val excludeClause = excludedRelativePaths.joinToString(" AND ") { path ->
                        "${MediaStore.Files.FileColumns.RELATIVE_PATH} NOT LIKE ?"
                    }
                    val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ? AND $mediaTypeSelection AND ($excludeClause)"
                    val excludeArgs = excludedRelativePaths.map { "$it%" }
                    selection to arrayOf("$folderRelativePath%") + mediaTypeArgs + excludeArgs.toTypedArray()
                } else {
                    val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ? AND $mediaTypeSelection"
                    selection to arrayOf("$folderRelativePath%") + mediaTypeArgs
                }
            } else {
                val selection = "${MediaStore.Files.FileColumns.BUCKET_ID} = ? AND $mediaTypeSelection"
                selection to arrayOf(folderId) + mediaTypeArgs
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

        photoListCache[cacheKey] = PhotoListCacheEntry(photos)
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
        // Resolves correctly to either MediaStore.Images or MediaStore.Video based on DB type
        getPhotoMetadata(photoId)?.uri
    }

    override suspend fun getThumbnailUrl(photoId: String): String? = withContext(Dispatchers.IO) {
        getPhotoMetadata(photoId)?.thumbnailUri
    }

    override suspend fun searchFolders(query: String): List<PhotoFolder> = withContext(Dispatchers.IO) {
        val allFolders = listFolders(null, false)
        allFolders.filter { it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun getFolderPhotoCount(folderId: String): Int {
        return getFilteredFolderMediaCount(folderId, null)
    }

    override suspend fun syncPhotos(): Boolean = withContext(Dispatchers.IO) {
        folderCache.clear()
        photoCountCache.clear()
        photoListCache.clear()
        true
    }

    override suspend fun getFilteredFolderMediaCount(folderId: String, mediaTypeFilter: String?): Int {
        val cacheKey = "${folderId}_${mediaTypeFilter ?: "all"}"
        val cached = photoCountCache[cacheKey]
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

                val folderRelativePath = getFolderRelativePath(folderId)
                val bucketCol = MediaStore.Files.FileColumns.BUCKET_ID
                val mediaTypeCol = MediaStore.Files.FileColumns.MEDIA_TYPE
                val imageType = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString()
                val videoType = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()

                val (selection, selectionArgs) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && folderRelativePath != null) {
                    val relPathCol = MediaStore.Files.FileColumns.RELATIVE_PATH
                    when (mediaTypeFilter) {
                        "images" -> "$relPathCol LIKE ? AND $mediaTypeCol = ?" to arrayOf("$folderRelativePath%", imageType)
                        "videos" -> "$relPathCol LIKE ? AND $mediaTypeCol = ?" to arrayOf("$folderRelativePath%", videoType)
                        else -> "$relPathCol LIKE ? AND $mediaTypeCol IN (?, ?)" to arrayOf("$folderRelativePath%", imageType, videoType)
                    }
                } else {
                    when (mediaTypeFilter) {
                        "images" -> "$bucketCol = ? AND $mediaTypeCol = ?" to arrayOf(folderId, imageType)
                        "videos" -> "$bucketCol = ? AND $mediaTypeCol = ?" to arrayOf(folderId, videoType)
                        else -> "$bucketCol = ? AND $mediaTypeCol IN (?, ?)" to arrayOf(folderId, imageType, videoType)
                    }
                }

                val projection = arrayOf(MediaStore.Files.FileColumns._ID)

                val cursor = contentResolver.query(collection, projection, selection, selectionArgs, null)
                val count = cursor?.use { it.count } ?: 0
                photoCountCache[cacheKey] = CountCacheEntry(count)
                count
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }
}
