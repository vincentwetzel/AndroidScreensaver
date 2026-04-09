package com.vincentwetzel.androidscreensaver.data.repository

import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import kotlinx.coroutines.flow.Flow

/**
 * Photo Repository Interface
 * Abstracts photo source operations for any provider
 */
interface PhotoRepository {
    
    /**
     * Check if the source is authenticated
     */
    fun isAuthenticated(): Boolean
    
    /**
     * List all folders from the source
     */
    suspend fun listFolders(parentFolderId: String?, forceRefresh: Boolean): List<PhotoFolder>
    
    /**
     * List all photos in a folder, optionally excluding subfolders by ID
     */
    suspend fun listPhotos(folderId: String, excludedFolderIds: Set<String> = emptySet()): List<Photo>
    
    /**
     * Get photo metadata
     */
    suspend fun getPhotoMetadata(photoId: String): Photo?
    
    /**
     * Get photo download/stream URL
     */
    suspend fun getPhotoUrl(photoId: String): String?
    
    /**
     * Get thumbnail URL for a photo
     */
    suspend fun getThumbnailUrl(photoId: String): String?
    
    /**
     * Search folders by name
     */
    suspend fun searchFolders(query: String): List<PhotoFolder>
    
    /**
     * Get total photo count for a folder
     */
    suspend fun getFolderPhotoCount(folderId: String): Int

    /**
     * Get media count for a folder filtered by media type.
     * @param mediaTypeFilter one of: "images", "videos", or null for both
     */
    suspend fun getFilteredFolderMediaCount(folderId: String, mediaTypeFilter: String?): Int
    
    /**
     * Sync photos from source
     */
    suspend fun syncPhotos(): Boolean
}
