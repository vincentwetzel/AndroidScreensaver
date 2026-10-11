package com.vincentwetzel.androidscreensaver.dream

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import com.vincentwetzel.androidscreensaver.data.model.AccountConfig
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.SlideshowConfig
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.data.repository.DropboxPhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GalleryPhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDrivePhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.OneDrivePhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import com.vincentwetzel.androidscreensaver.data.repository.PhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.MediaCacheManager
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import com.vincentwetzel.androidscreensaver.utils.GalleryPermissionPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
        val cachedUri = when (val repository = photoRepositories[photo.sourceType]) {
            is GoogleDrivePhotoRepository -> repository.downloadPhotoToLocalCache(photo.id, accountId, photo.title)
            is DropboxPhotoRepository -> repository.downloadPhotoToLocalCache(photo.id, accountId)
            is OneDrivePhotoRepository -> repository.downloadPhotoToLocalCache(photo.id, accountId, photo.uri, photo.title)
            else -> null
        }
        if (cachedUri != null) trimCache(cachedUri)
        return cachedUri
    }

    suspend fun trimCache(protectedMediaUri: String? = null) {
        if (!config.cacheConfig.enabled) return
        val limitMb = if (config.cacheConfig.usePresetLimit) {
            when (config.cacheConfig.presetLimit) {
                com.vincentwetzel.androidscreensaver.data.model.CacheSizeLimit.MB_100 -> 100
                com.vincentwetzel.androidscreensaver.data.model.CacheSizeLimit.MB_500 -> 500
                com.vincentwetzel.androidscreensaver.data.model.CacheSizeLimit.GB_1 -> 1024
                com.vincentwetzel.androidscreensaver.data.model.CacheSizeLimit.GB_2 -> 2048
                com.vincentwetzel.androidscreensaver.data.model.CacheSizeLimit.GB_5 -> 5120
                com.vincentwetzel.androidscreensaver.data.model.CacheSizeLimit.UNLIMITED -> Int.MAX_VALUE
            }
        } else config.cacheConfig.cacheSizeLimitMB
        val limitBytes = if (limitMb == Int.MAX_VALUE) Long.MAX_VALUE else limitMb.toLong() * 1024L * 1024L
        val protectedFile = protectedMediaUri?.toUri()?.path?.let { java.io.File(it) }
        MediaCacheManager.trimIfNeeded(context, limitBytes, protectedFile)
    }

    /**
     * Load photos from all enabled accounts across all source types.
     * For Google Drive, iterates over each authenticated account.
     * For Gallery, loads from the single gallery source.
     */
    suspend fun loadPhotos(): List<Photo> {
        return withContext(Dispatchers.IO) {
            val filteredUniquePhotos = mutableListOf<Photo>()
            val seenPhotoKeys = mutableSetOf<Triple<SourceType, String?, String>>()
            var loadedPhotoCount = 0

            fun addPhotos(photos: List<Photo>) {
                loadedPhotoCount += photos.size
                photos.forEach { photo ->
                    val matchesFilter = PhotoMediaClassifier.matchesFilter(photo, config.mediaTypeFilter)
                    val key = PhotoMediaClassifier.mediaKey(photo)
                    if (matchesFilter && seenPhotoKeys.add(key)) filteredUniquePhotos.add(photo)
                }
            }

            val galleryRepository = photoRepositories[SourceType.GALLERY] as? GalleryPhotoRepository
            if (galleryRepository?.hasSelectedMediaAccess() == true) {
                // The selected item set can change while permission bits stay the same.
                galleryRepository.syncPhotos()
            }

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
                                photoRepository.listFolders(parentFolderId = null, forceRefresh = false, accountId = account.accountId)
                            } else {
                                selectedFolders
                            }

                            // Chunk execution to prevent OOM spikes and Rate Limiting on dozens of folders
                            foldersToLoad.chunked(5).forEach { chunk ->
                                val results = chunk.map { folder ->
                                    async { 
                                        try {
                                            photoRepository.listPhotos(folder.id, excludedFolderIds, config.mediaTypeFilter, accountId = account.accountId)
                                        } catch (e: Exception) {
                                            if (e is kotlinx.coroutines.CancellationException) throw e
                                            // Isolate the failure so it doesn't cancel the entire parent IO coroutine scope
                                            android.util.Log.e(TAG, "Failed to load a $sourceType folder: ${e.javaClass.simpleName}")
                                            emptyList<Photo>()
                                        }
                                    }
                                }.awaitAll()
                                results.forEach(::addPhotos)
                            }

                            android.util.Log.d(TAG, "Loaded ${filteredUniquePhotos.size} unique photos so far from $sourceType account: $safeAccountId")
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            val safeAccountId = account.accountId.hashCode().toString()
                            android.util.Log.e(TAG, "Error loading $sourceType photos for $safeAccountId: ${e.javaClass.simpleName}")
                        }
                    }
                }
            }

            android.util.Log.d(TAG, "Loaded $loadedPhotoCount photos, kept ${filteredUniquePhotos.size} unique matches for ${config.mediaTypeFilter}")
            if (filteredUniquePhotos.isEmpty() && loadedPhotoCount > 0) {
                android.util.Log.w(TAG, "Warning: Had $loadedPhotoCount photos but filtered to 0")
            }

            filteredUniquePhotos
        }
    }

    /**
     * Get sort key for a photo based on configured order
     */
    fun getSortKey(photo: Photo): Comparable<*> {
        return PhotoMediaClassifier.sortKey(photo, config.photoOrder)
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
            val hasSelectedMedia = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                ContextCompat.checkSelfPermission(context, GalleryPermissionPolicy.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            val hasPermission = GalleryPermissionPolicy.hasAccess(config.mediaTypeFilter, hasImage, hasVideo, hasSelectedMedia)
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
    }
}
