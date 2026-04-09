package com.vincentwetzel.androidscreensaver.dream

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.SlideshowConfig
import com.vincentwetzel.androidscreensaver.data.repository.GalleryPhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDrivePhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages slideshow configuration and photo loading
 */
@Singleton
class SlideshowManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val driveRepository: GoogleDriveRepository,
    private val photoRepository: GoogleDrivePhotoRepository,
    private val galleryPhotoRepository: GalleryPhotoRepository
) {
    // Current slideshow configuration
    lateinit var config: SlideshowConfig
        private set

    // Loaded photos
    private var loadedPhotos: List<Photo> = emptyList()

    // Preloaded photo cache
    private val preloadCache = mutableMapOf<String, Boolean>()

    init {
        // Load config from settings
        loadConfig()
    }

    /**
     * Load configuration from settings
     */
    fun loadConfig() {
        config = SettingsManager.getSlideshowConfig(context)
    }

    /**
     * Update configuration
     */
    fun updateConfig(newConfig: SlideshowConfig) {
        config = newConfig
        SettingsManager.saveSlideshowConfig(context, newConfig)
    }

    /**
     * Load photos from all enabled sources
     */
    suspend fun loadPhotos(): List<Photo> {
        return withContext(Dispatchers.IO) {
            val allPhotos = mutableListOf<Photo>()

            // Check if Google Drive is authenticated and enabled
            if (driveRepository.isAuthenticated.value && isSourceEnabled(com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE)) {
                // Respect wifi_only setting
                if (config.wifiOnly && !isOnWifi()) {
                    android.util.Log.w(TAG, "Skipping Google Drive: not on Wi-Fi and wifi_only is enabled")
                } else {
                    try {
                        val selectedFolders = getSelectedFolders(com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE)
                        val excludedFolderIds = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getDeselectedFolders(
                            context, com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE
                        )
                        val driveFolders = if (selectedFolders.isEmpty()) {
                            photoRepository.listFolders(null, forceRefresh = false)
                        } else {
                            selectedFolders
                        }

                        for (folder in driveFolders) {
                            val folderPhotos = photoRepository.listPhotos(folder.id, excludedFolderIds)
                            // Download each photo to local cache (Google Drive API requires OAuth headers)
                            val photosWithLocalUris = folderPhotos.mapNotNull { photo ->
                                val localPath = photoRepository.downloadPhotoToLocalCache(photo.id)
                                    ?: return@mapNotNull null
                                photo.copy(uri = "file://$localPath")
                            }
                            allPhotos.addAll(photosWithLocalUris)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Error loading Google Drive photos", e)
                    }
                }
            }

            // Check if Gallery is enabled
            if (isSourceEnabled(com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY)) {
                try {
                    val selectedFolders = getSelectedFolders(com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY)
                    val excludedFolderIds = com.vincentwetzel.androidscreensaver.utils.SettingsManager.getDeselectedFolders(
                        context, com.vincentwetzel.androidscreensaver.dream.SourceType.GALLERY
                    )

                    if (selectedFolders.isEmpty()) {
                        val allFolders = galleryPhotoRepository.listFolders(null, forceRefresh = false)
                        for (folder in allFolders) {
                            allPhotos.addAll(galleryPhotoRepository.listPhotos(folder.id, excludedFolderIds))
                        }
                    } else {
                        for (folder in selectedFolders) {
                            allPhotos.addAll(galleryPhotoRepository.listPhotos(folder.id, excludedFolderIds))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Deduplicate photos by ID (can happen when a parent folder is cascade-selected
            // along with its subfolders — listPhotos recurses into subfolders, so the same
            // photo can be loaded from both the parent and child folder entries)
            val uniquePhotos = allPhotos.distinctBy { it.id }

            loadedPhotos = uniquePhotos

            // Apply media type filter
            val filteredPhotos = when (config.mediaTypeFilter) {
                com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY ->
                    uniquePhotos.filter { isImage(it.uri) }
                com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY ->
                    uniquePhotos.filter { isVideo(it.uri) }
                else -> uniquePhotos
            }

            loadedPhotos = filteredPhotos
            filteredPhotos
        }
    }

    /**
     * Check if a URI points to an image file
     */
    private fun isImage(uri: String): Boolean {
        val lower = uri.lowercase()
        // File extension check (Google Drive cached photos)
        val hasImageExtension = lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
               lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp") ||
               lower.endsWith(".heic") || lower.endsWith(".heif") || lower.endsWith(".svg") ||
               lower.endsWith(".tiff") || lower.endsWith(".tif")
        // Content URI check (Gallery photos - MediaStore path)
        val isImageContentUri = lower.contains("/images/media/") || lower.contains("media_type_image")
        return hasImageExtension || isImageContentUri
    }

    /**
     * Check if a URI points to a video file
     */
    private fun isVideo(uri: String): Boolean {
        val lower = uri.lowercase()
        // File extension check (Google Drive cached photos)
        val hasVideoExtension = lower.endsWith(".mp4") || lower.endsWith(".avi") || lower.endsWith(".mov") ||
               lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".wmv") ||
               lower.endsWith(".flv") || lower.endsWith(".m4v")
        // Content URI check (Gallery photos - MediaStore path)
        val isVideoContentUri = lower.contains("/video/media/") || lower.contains("media_type_video")
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
                (photo.title ?: "").reversed()
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
        if (preloadCache[photo.id] == true) return

        withContext(Dispatchers.IO) {
            try {
                // Route to correct source based on photo URI
                if (photo.uri.startsWith("content://")) {
                    // Gallery photos: content:// URIs - already local, Coil can load directly
                    preloadCache[photo.id] = true
                    android.util.Log.d(TAG, "Preloaded Gallery photo: ${photo.uri}")
                } else if (photo.uri.startsWith("file://")) {
                    // Google Drive cached photos: file:// URIs - already downloaded
                    preloadCache[photo.id] = true
                    android.util.Log.d(TAG, "Preloaded cached Drive photo: ${photo.uri}")
                } else if (photo.uri.startsWith("http")) {
                    // Google Drive remote URLs - need to download
                    val url = photoRepository.getPhotoUrl(photo.id)
                    if (url != null) {
                        preloadCache[photo.id] = true
                        android.util.Log.d(TAG, "Preloaded remote Drive photo: ${photo.uri}")
                    } else {
                        android.util.Log.w(TAG, "Failed to get URL for Drive photo: ${photo.id}")
                    }
                } else {
                    android.util.Log.w(TAG, "Unknown URI scheme for preloading: ${photo.uri}")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error preloading photo: ${photo.uri}", e)
            }
        }
    }

    /**
     * Check if a source is enabled
     */
    private fun isSourceEnabled(sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType): Boolean {
        return SettingsManager.isSourceEnabled(context, sourceType)
    }

    /**
     * Get selected folders for a source
     */
    private fun getSelectedFolders(sourceType: com.vincentwetzel.androidscreensaver.dream.SourceType): List<com.vincentwetzel.androidscreensaver.data.model.PhotoFolder> {
        return SettingsManager.getSelectedFolders(context, sourceType)
    }

    /**
     * Check if the device is currently connected to Wi-Fi
     */
    private fun isOnWifi(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    companion object {
        private const val TAG = "SlideshowManager"
    }
}

/**
 * Source type enum for slideshow manager
 */
enum class SourceType {
    GOOGLE_DRIVE,
    GALLERY,
    DROPBOX,
    GOOGLE_PHOTOS,
    ONEDRIVE,
    LOCAL_NETWORK
}