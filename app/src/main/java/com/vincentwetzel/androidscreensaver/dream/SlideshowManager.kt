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
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import com.vincentwetzel.androidscreensaver.data.repository.PhotoRepository
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

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
        android.util.Log.d(TAG, "Config loaded: videoAudioMode=${config.videoAudioMode}, videoCustomVolume=${config.videoCustomVolume}")
    }

    /**
     * Update configuration
     */
    fun updateConfig(newConfig: SlideshowConfig) {
        config = newConfig
        SettingsManager.saveSlideshowConfig(context, newConfig)
    }

    suspend fun downloadPhotoToLocalCache(photo: Photo): String? {
        val accountId = photo.accountId ?: return null
        return when (val repository = photoRepositories[photo.sourceType]) {
            is GoogleDrivePhotoRepository -> repository.downloadPhotoToLocalCache(photo.id, accountId)
            is DropboxPhotoRepository -> repository.downloadPhotoToLocalCache(photo.id, accountId)
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
                            android.util.Log.d(TAG, "Loading $sourceType photos for account: ${account.accountId}")

                            // Google Drive specific authentication check
                            if (sourceType == SourceType.GOOGLE_DRIVE && !driveRepository.isAccountAuthenticated(account.accountId)) {
                                android.util.Log.w(TAG, "Account ${account.accountId} is not authenticated for Google Drive, skipping")
                                continue
                            }

                            // Gallery specific permission check
                            if (sourceType == SourceType.GALLERY && !hasGalleryPermission()) {
                                android.util.Log.w(TAG, "Gallery photo access requires permission for ${account.accountId}, skipping")
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
                                photoRepository.listFolders(null, forceRefresh = false)
                            } else {
                                selectedFolders
                            }

                            for (folder in foldersToLoad) {
                                val folderPhotos = photoRepository.listPhotos(folder.id, excludedFolderIds)
                                allPhotos.addAll(folderPhotos)
                            }

                            android.util.Log.d(TAG, "Loaded ${allPhotos.size} photos so far from $sourceType account: ${account.accountId}")
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "Error loading $sourceType photos for ${account.accountId}", e)
                        }
                    }
                }
            }

            // Deduplicate photos by ID (can happen when a parent folder is cascade-selected
            // along with its subfolders — listPhotos recurses into subfolders, so the same
            // photo can be loaded from both the parent and child folder entries)
            val uniquePhotos = allPhotos.distinctBy { it.id }
            android.util.Log.d(TAG, "Total photos loaded: ${allPhotos.size}, unique: ${uniquePhotos.size}")

            loadedPhotos = uniquePhotos

            // Apply media type filter
            val filteredPhotos = when (config.mediaTypeFilter) {
                com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY ->
                    uniquePhotos.filter { isImage(it.uri) }
                com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY ->
                    uniquePhotos.filter { isVideo(it.uri) }
                else -> uniquePhotos
            }

            android.util.Log.d(TAG, "After media type filter (${config.mediaTypeFilter}): ${filteredPhotos.size} photos")
            if (filteredPhotos.isEmpty() && uniquePhotos.isNotEmpty()) {
                android.util.Log.w(TAG, "Warning: Had ${uniquePhotos.size} photos but filtered to 0. Sample URIs: ${uniquePhotos.take(3).map { it.uri }}")
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
                // Route to correct source based on photo's sourceType
                val photoRepository = photoRepositories[photo.sourceType]
                if (photoRepository == null) {
                    android.util.Log.e(TAG, "No PhotoRepository found for Photo.sourceType: ${photo.sourceType}")
                    return@withContext
                }

                if (photo.uri.startsWith("content://")) {
                    // Gallery photos: content:// URIs - already local, Coil can load directly
                    preloadCache[photo.id] = true
                    android.util.Log.d(TAG, "Preloaded Gallery photo: ${photo.uri}")
                } else if (photo.uri.startsWith("file://")) {
                    // Google Drive cached photos: file:// URIs - already downloaded
                    preloadCache[photo.id] = true
                    android.util.Log.d(TAG, "Preloaded cached Drive photo: ${photo.uri}")
                } else if (photo.uri.startsWith("http")) {
                    // Remote URLs (Google Drive, Dropbox, etc.) - need to handle caching/downloading if not already local
                    val accountId = photo.accountId
                    if (accountId != null) {
                        // For Google Drive, the download is handled by GoogleDrivePhotoRepository
                        // For Dropbox, it will be handled by DropboxPhotoRepository
                        val url = photoRepository.getPhotoUrl(photo.id) // This should trigger the download/cache logic if needed
                        if (url != null) {
                            preloadCache[photo.id] = true
                            android.util.Log.d(TAG, "Preloaded remote photo from ${photo.sourceType}: ${photo.uri}")
                        } else {
                            android.util.Log.w(TAG, "Failed to get URL for ${photo.sourceType} photo: ${photo.id}")
                        }
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
     * Check if a source is enabled (legacy: checks if any account exists for the source)
     */
    private fun isSourceEnabled(sourceType: SourceType): Boolean {
        val accounts = SettingsManager.getAccountsForSource(context, sourceType)
        return accounts.any { it.enabled }
    }

    /**
     * Get selected folders for a source (legacy: returns combined from all accounts)
     */
    private fun getSelectedFolders(sourceType: SourceType): List<com.vincentwetzel.androidscreensaver.data.model.PhotoFolder> {
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

    /**
     * Check if the app has permission to read Gallery photos
     */
    fun hasGalleryPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.w(TAG, "Gallery permission not granted: $permission")
        }
        return hasPermission
    }

    companion object {
        private const val TAG = "SlideshowManager"
    }
}
