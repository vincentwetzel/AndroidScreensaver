package com.vincentwetzel.androidscreensaver.dream

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.vincentwetzel.androidscreensaver.data.model.AccountConfig
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.SlideshowConfig
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.data.repository.DropboxPhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDrivePhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.OneDrivePhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import com.vincentwetzel.androidscreensaver.data.repository.PhotoRepository
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages slideshow configuration and photo loading.
 * Supports multiple accounts per source type (e.g., 2 Google Drive accounts).
 */
@Singleton
class SlideshowManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val driveRepository: GoogleDriveRepository, // Still needed for Google Drive specific auth checks
    private val photoRepositories: Map<SourceType, @JvmSuppressWildcards PhotoRepository>
) {
    // Current slideshow configuration
    lateinit var config: SlideshowConfig
        private set

    // Loaded photos
    private var loadedPhotos: List<Photo> = emptyList()

    // Preloaded photo cache
    private val preloadCache = ConcurrentHashMap<String, Boolean>()

    private class ReverseString(val str: String) : Comparable<ReverseString> {
        override fun compareTo(other: ReverseString) = other.str.compareTo(this.str)
    }

    /**
     * Load configuration from settings
     */
    suspend fun loadConfig() {
        config = SettingsManager.getSlideshowConfig(context)
        android.util.Log.d(TAG, "Config loaded: videoAudioMode=${config.videoAudioMode}, videoCustomVolume=${config.videoCustomVolume}")
    }

    /**
     * Update configuration
     */
    suspend fun updateConfig(newConfig: SlideshowConfig) {
        config = newConfig
        SettingsManager.saveSlideshowConfig(context, newConfig)
    }

    suspend fun downloadPhotoToLocalCache(photo: Photo): String? {
        val accountId = photo.accountId ?: return null
        return when (val repository = photoRepositories[photo.sourceType]) {
            is GoogleDrivePhotoRepository -> repository.downloadPhotoToLocalCache(photo.id, accountId, photo.title)
            is DropboxPhotoRepository -> repository.downloadPhotoToLocalCache(photo.id, accountId)
            is OneDrivePhotoRepository -> repository.downloadPhotoToLocalCache(photo.id, accountId, photo.uri)
            else -> null
        }
    }

    /**
     * Load photos from all enabled accounts across all source types.
     * For Google Drive, iterates over each authenticated account.
     * For Gallery, loads from the single gallery source.
     */
    suspend fun loadPhotos(): List<Photo> {
        return withContext(Dispatchers.IO) {
            val allPhotos = mutableListOf<Photo>()
            
            // Clear previous preloads to prevent memory leaks over time
            preloadCache.clear()

            // Iterate over all available source types
            SourceType.entries.forEach { sourceType ->
                val accounts = SettingsManager.getAccountsForSource(context, sourceType)
                val enabledAccounts = accounts.filter { it.enabled && it.isAuthenticated }

                if (enabledAccounts.isNotEmpty()) {
                    // Respect wifi_only setting for remote sources
                    if (sourceType != SourceType.GALLERY && config.wifiOnly && !isOnWifi()) {
                        android.util.Log.w(TAG, "Skipping $sourceType: not on Wi-Fi and wifi_only is enabled")
                        return@forEach // Continue to next source type
                    }

                    val photoRepository = photoRepositories[sourceType]
                    if (photoRepository == null) {
                        android.util.Log.e(TAG, "No PhotoRepository found for SourceType: $sourceType")
                        return@forEach
                    }

                    for (account in enabledAccounts) {
                        try {
                            val safeAccountId = account.accountId.hashCode().toString()
                            android.util.Log.d(TAG, "Loading $sourceType photos for account: $safeAccountId")

                            // Google Drive specific authentication check
                            if (sourceType == SourceType.GOOGLE_DRIVE && !driveRepository.isAccountAuthenticated(account.accountId)) {
                                android.util.Log.w(TAG, "Account $safeAccountId is not authenticated for Google Drive, skipping")
                                continue
                            }

                            // Gallery specific permission check
                            if (sourceType == SourceType.GALLERY && !hasGalleryPermission()) {
                                android.util.Log.w(TAG, "Gallery photo access requires permission for $safeAccountId, skipping")
                                continue
                            }
                            
                            val selectedFolders = account.selectedFolders
                                .map { sf ->
                                    com.vincentwetzel.androidscreensaver.data.model.PhotoFolder(
                                        id = sf.folderId,
                                        sourceType = sourceType,
                                        accountId = account.accountId,
                                        name = sf.folderName,
                                        parentFolderId = sf.parentFolderId,
                                        photoCount = sf.photoCount
                                    )
                                }
                            val excludedFolderIds = account.deselectedFolders

                            val foldersToLoad = if (selectedFolders.isEmpty()) {
                                photoRepository.listFolders(account.accountId, forceRefresh = false)
                            } else {
                                selectedFolders
                            }

                            // Chunk execution to prevent OOM spikes and Rate Limiting on dozens of folders
                            val folderPhotosList = mutableListOf<List<Photo>>()
                            foldersToLoad.chunked(5).forEach { chunk ->
                                val results = chunk.map { folder ->
                                    async { 
                                        try {
                                            photoRepository.listPhotos(folder.id, excludedFolderIds, config.mediaTypeFilter) 
                                        } catch (e: Exception) {
                                            if (e is kotlinx.coroutines.CancellationException) throw e
                                            // Isolate the failure so it doesn't cancel the entire parent IO coroutine scope
                                            android.util.Log.e(TAG, "Failed to load folder ${folder.id}: ${e.message}")
                                            emptyList<Photo>()
                                        }
                                    }
                                }.awaitAll()
                                folderPhotosList.addAll(results)
                            }

                            folderPhotosList.forEach { allPhotos.addAll(it) }

                            android.util.Log.d(TAG, "Loaded ${allPhotos.size} photos so far from $sourceType account: $safeAccountId")
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            val safeAccountId = account.accountId.hashCode().toString()
                            android.util.Log.e(TAG, "Error loading $sourceType photos for $safeAccountId: ${e.javaClass.simpleName}")
                        }
                    }
                }
            }

            // Deduplicate photos by ID (can happen when a parent folder is cascade-selected
            // along with its subfolders — listPhotos recurses into subfolders, so the same
            // photo can be loaded from both the parent and child folder entries)
            val uniquePhotos = allPhotos.distinctBy { "${it.sourceType}_${it.accountId}_${it.id}" }
            android.util.Log.d(TAG, "Total photos loaded: ${allPhotos.size}, unique: ${uniquePhotos.size}")

            loadedPhotos = uniquePhotos

            // Apply media type filter
            val filteredPhotos = when (config.mediaTypeFilter) {
                com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY ->
                    uniquePhotos.filter { isImage(it) }
                com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY ->
                    uniquePhotos.filter { isVideo(it) }
                else -> uniquePhotos
            }

            android.util.Log.d(TAG, "After media type filter (${config.mediaTypeFilter}): ${filteredPhotos.size} photos")
            if (filteredPhotos.isEmpty() && uniquePhotos.isNotEmpty()) {
                android.util.Log.w(TAG, "Warning: Had ${uniquePhotos.size} photos but filtered to 0. Sample IDs: ${uniquePhotos.take(3).map { it.id }}")
            }

            loadedPhotos = filteredPhotos
            filteredPhotos
        }
    }

    /**
     * Check if a photo represents an image file
     */
    private fun isImage(photo: Photo): Boolean {
        val nameToCheck = photo.title ?: photo.uri
        val hasImageExtension = IMAGE_EXTENSIONS.any { nameToCheck.endsWith(it, ignoreCase = true) }
        val isImageContentUri = photo.uri.contains("/images/media/", ignoreCase = true) || photo.uri.contains("media_type_image", ignoreCase = true)
        return hasImageExtension || isImageContentUri
    }

    /**
     * Check if a photo represents a video file
     */
    private fun isVideo(photo: Photo): Boolean {
        val nameToCheck = photo.title ?: photo.uri
        val hasVideoExtension = VIDEO_EXTENSIONS.any { nameToCheck.endsWith(it, ignoreCase = true) }
        val isVideoContentUri = photo.uri.contains("/video/media/", ignoreCase = true) || photo.uri.contains("media_type_video", ignoreCase = true)
        return hasVideoExtension || isVideoContentUri
    }

    /**
     * Get sort key for a photo based on configured order
     */
    fun getSortKey(photo: Photo): Comparable<*> {
        return when (config.photoOrder) {
            com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.DATE_NEWEST_FIRST ->
                -(photo.dateTaken ?: 0L)
            com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.DATE_OLDEST_FIRST ->
                (photo.dateTaken ?: 0L)
            com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.NAME_A_Z ->
                photo.title ?: ""
            com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.NAME_Z_A ->
                ReverseString(photo.title ?: "")
            com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.SIZE_LARGEST_FIRST ->
                -(photo.fileSize ?: 0L)
            com.vincentwetzel.androidscreensaver.data.model.PhotoOrder.SIZE_SMALLEST_FIRST ->
                (photo.fileSize ?: 0L)
            else -> 0
        }
    }

    /**
     * Preload a photo into cache
     */
    suspend fun preloadPhoto(photo: Photo) {
        val cacheKey = "${photo.sourceType}_${photo.accountId}_${photo.id}"
        if (preloadCache[cacheKey] == true) return

        withContext(Dispatchers.IO) {
            try {
                // Route to correct source based on photo's sourceType
                val photoRepository = photoRepositories[photo.sourceType]
                if (photoRepository == null) {
                    android.util.Log.e(TAG, "No PhotoRepository found for Photo.sourceType: ${photo.sourceType}")
                    return@withContext
                }

                if (photo.uri.startsWith("content://")) {
                    // Gallery photos: content:// URIs - already local, Coil can load directly
                    preloadCache[cacheKey] = true
                    android.util.Log.d(TAG, "Preloaded Gallery photo: ${photo.id}")
                } else if (photo.uri.startsWith("file://")) {
                    // Google Drive cached photos: file:// URIs - already downloaded
                    preloadCache[cacheKey] = true
                    android.util.Log.d(TAG, "Preloaded cached Drive photo: ${photo.id}")
                } else {
                    // Remote URLs or Cloud IDs (Google Drive, Dropbox, etc.)
                    val localPath = downloadPhotoToLocalCache(photo)
                    if (localPath != null) {
                        preloadCache[cacheKey] = true
                        android.util.Log.d(TAG, "Preloaded remote photo from ${photo.sourceType}: $localPath")
                    } else {
                        android.util.Log.w(TAG, "Failed to download/preload ${photo.sourceType} photo: ${photo.id}")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e(TAG, "Error preloading photo: ${e.javaClass.simpleName}")
            }
        }
    }

    /**
     * Check if the device is currently connected to Wi-Fi
     */
    private fun isOnWifi(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /**
     * Check if the app has permission to read Gallery photos
     */
    fun hasGalleryPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasImage = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVideo = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val hasPermission = hasImage || hasVideo
            if (!hasPermission) Log.w(TAG, "Gallery permission not granted (needs READ_MEDIA_IMAGES or READ_MEDIA_VIDEO)")
            return hasPermission
        } else {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            val hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) Log.w(TAG, "Gallery permission not granted: $permission")
            return hasPermission
        }
    }

    companion object {
        private const val TAG = "SlideshowManager"
        private val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".heic", ".heif", ".svg", ".tiff", ".tif")
        private val VIDEO_EXTENSIONS = listOf(".mp4", ".avi", ".mov", ".mkv", ".webm", ".wmv", ".flv", ".m4v")
    }
}
