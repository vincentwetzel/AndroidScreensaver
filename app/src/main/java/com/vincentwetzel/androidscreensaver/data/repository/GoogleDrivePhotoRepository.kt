package com.vincentwetzel.androidscreensaver.data.repository

import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive implementation of PhotoRepository
 * Fetches photos and folders from Google Drive
 */
@Singleton
class GoogleDrivePhotoRepository @Inject constructor(
    private val driveRepository: GoogleDriveRepository
) : PhotoRepository {

    // Supported image file extensions
    private val imageExtensions = listOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "tiff", "tif"
    )

    // Supported video file extensions
    private val videoExtensions = listOf(
        "mp4", "avi", "mov", "mkv", "webm", "wmv", "flv", "m4v"
    )

    override fun isAuthenticated(): Boolean {
        return driveRepository.isAuthenticated().value
    }

    override suspend fun listFolders(parentFolderId: String?): List<PhotoFolder> {
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

        return folders
    }

    override suspend fun listPhotos(folderId: String): List<Photo> {
        val driveService = driveRepository.getDriveService()
            ?: throw IllegalStateException("Not authenticated with Google Drive")

        val photos = mutableListOf<Photo>()

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

        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to list photos: ${e.message}")
        }

        return photos
    }

    override suspend fun getPhotoMetadata(photoId: String): Photo? {
        val driveService = driveRepository.getDriveService()
            ?: throw IllegalStateException("Not authenticated with Google Drive")

        return try {
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

    override suspend fun getPhotoUrl(photoId: String): String? {
        val driveService = driveRepository.getDriveService()
            ?: throw IllegalStateException("Not authenticated with Google Drive")

        return try {
            // Get a download URL that works with authenticated requests
            "https://www.googleapis.com/drive/v3/files/$photoId?alt=media"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getThumbnailUrl(photoId: String): String? {
        val driveService = driveRepository.getDriveService()
            ?: throw IllegalStateException("Not authenticated with Google Drive")

        return try {
            // Google Drive provides thumbnails at this endpoint
            "https://www.googleapis.com/drive/v3/files/$photoId/thumbnail?sz=w400"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun searchFolders(query: String): List<PhotoFolder> {
        val driveService = driveRepository.getDriveService()
            ?: throw IllegalStateException("Not authenticated with Google Drive")

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

        return folders
    }

    override suspend fun getFolderPhotoCount(folderId: String): Int {
        val driveService = driveRepository.getDriveService()
            ?: throw IllegalStateException("Not authenticated with Google Drive")

        return try {
            val mimeTypeQuery = imageExtensions.joinToString(" or ") { ext ->
                "name contains '.$ext'"
            }

            val query = "($mimeTypeQuery) and trashed=false and '$folderId' in parents"

            val files = driveService.files().list()
                .setQ(query)
                .setPageSize(1)
                .setFields("files(id)")
                .execute()

            // Google Drive doesn't provide count directly, so we'd need to paginate
            // For now, return approximate count from a single request
            files.files?.size ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    override suspend fun syncPhotos(): Boolean {
        // For Google Drive, we don't need to sync locally
        // We fetch photos on-demand from the API
        return true
    }
}
