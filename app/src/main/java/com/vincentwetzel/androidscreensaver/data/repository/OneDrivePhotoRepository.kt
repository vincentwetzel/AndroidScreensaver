package com.vincentwetzel.androidscreensaver.data.repository

import android.content.Context
import com.vincentwetzel.androidscreensaver.auth.OneDriveAuthManager
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OneDrivePhotoRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val authManager: OneDriveAuthManager,
    private val httpClient: OkHttpClient
) : BaseCloudPhotoRepository(context, com.vincentwetzel.androidscreensaver.dream.SourceType.ONEDRIVE) {

    override fun isAuthenticated(): Boolean {
        return getAuthenticatedAccountIds().isNotEmpty()
    }

    override fun getAuthenticatedAccountIds(): List<String> {
        return authManager.getAllAccountIds()
    }

    private fun parseIso8601(dateString: String?): Long {
        if (dateString.isNullOrEmpty()) return 0L
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun listFoldersForAccount(
        parentFolderId: String?,
        forceRefresh: Boolean,
        accountId: String
    ): List<PhotoFolder> = withContext(Dispatchers.IO) {
        val cacheKey = "${accountId}_${parentFolderId ?: "ROOT"}"
        
        val cached = folderCache[cacheKey]
        if (!forceRefresh && cached != null && !cached.isStale) {
            @Suppress("UNCHECKED_CAST")
            return@withContext cached.data as List<PhotoFolder>
        }

        val token = authManager.getAccessToken(accountId) 
            ?: throw IllegalStateException("Not authenticated for account: $accountId")

        val folders = mutableListOf<PhotoFolder>()
        val itemId = parentFolderId ?: "root"
        var url = "https://graph.microsoft.com/v1.0/me/drive/items/$itemId/children?\$filter=folder ne null&\$select=id,name,parentReference"

        try {
            while (url.isNotEmpty()) {
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw RuntimeException("Graph API error: ${response.code}")
                    val body = response.body?.string() ?: break
                    val json = JSONObject(body)
                    val value = json.optJSONArray("value") ?: break

                    for (i in 0 until value.length()) {
                        val item = value.getJSONObject(i)
                        folders.add(
                            PhotoFolder(
                                id = item.getString("id"),
                                sourceType = SourceType.ONEDRIVE,
                                accountId = accountId,
                                name = item.getString("name"),
                                parentFolderId = parentFolderId,
                                photoCount = 0
                            )
                        )
                    }
                    url = json.optString("@odata.nextLink", "")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OneDrivePhotoRepo", "Failed to list folders", e)
            throw e
        }

        folders.also { folderCache[cacheKey] = CacheEntry(it) }
    }

    override suspend fun listPhotosForAccount(
        folderId: String,
        excludedFolderIds: Set<String>,
        mediaTypeFilter: MediaTypeFilter?,
        accountId: String
    ): List<Photo> = withContext(Dispatchers.IO) {
        val normalizedFilter = normalizeMediaFilter(mediaTypeFilter)
        val exclusionsKey = if (excludedFolderIds.isEmpty()) "none" else excludedFolderIds.sorted().joinToString(",")
        val cacheKey = "${accountId}_${folderId}_${normalizedFilter}_$exclusionsKey"
        
        val cached = photoListCache[cacheKey]
        if (cached != null && !cached.isStale) {
            return@withContext cached.data
        }

        val token = authManager.getAccessToken(accountId) 
            ?: throw IllegalStateException("Not authenticated for account: $accountId")

        val photos = mutableListOf<Photo>()
        val visited = mutableSetOf<String>()

        fun collectPhotos(currentFolderId: String) {
            if (!visited.add(currentFolderId)) return
            if (currentFolderId in excludedFolderIds && currentFolderId != folderId) return
            
            var url = "https://graph.microsoft.com/v1.0/me/drive/items/$currentFolderId/children?\$expand=thumbnails&\$select=id,name,file,image,video,folder,lastModifiedDateTime,size,@microsoft.graph.downloadUrl"
            val subfoldersToRecurse = mutableListOf<String>()

            while (url.isNotEmpty()) {
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .build()

                try {
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return
                        val body = response.body?.string() ?: return
                        val json = JSONObject(body)
                        val value = json.optJSONArray("value") ?: return

                        for (i in 0 until value.length()) {
                            val item = value.getJSONObject(i)
                            val id = item.getString("id")
                            
                            if (item.has("folder")) {
                                if (id !in excludedFolderIds) {
                                    subfoldersToRecurse.add(id)
                                }
                            } else if (item.has("file")) {
                                val isImage = item.has("image")
                                val isVideo = item.has("video")
                                val matchesFilter = when (normalizedFilter) {
                                    "images" -> isImage
                                    "videos" -> isVideo
                                    else -> isImage || isVideo
                                }
                                
                                if (matchesFilter) {
                                    val name = item.getString("name")
                                    val downloadUrl = item.optString("@microsoft.graph.downloadUrl")
                                    val size = item.optLong("size", 0L)
                                    val dateStr = item.optString("lastModifiedDateTime")
                                    
                                    val thumbnails = item.optJSONArray("thumbnails")
                                    val thumbnailUrl = if (thumbnails != null && thumbnails.length() > 0) {
                                        thumbnails.getJSONObject(0).optJSONObject("medium")?.optString("url") ?: downloadUrl
                                    } else {
                                        downloadUrl
                                    }
                                    
                                    photos.add(
                                        Photo(
                                            id = id,
                                            sourceType = SourceType.ONEDRIVE,
                                            accountId = accountId,
                                            uri = downloadUrl,
                                            thumbnailUri = thumbnailUrl,
                                            title = name,
                                            dateTaken = parseIso8601(dateStr),
                                            width = item.optJSONObject("image")?.optInt("width") ?: item.optJSONObject("video")?.optInt("width"),
                                            height = item.optJSONObject("image")?.optInt("height") ?: item.optJSONObject("video")?.optInt("height"),
                                            fileSize = size
                                        )
                                    )
                                }
                            }
                        }
                        url = json.optString("@odata.nextLink", "")
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    android.util.Log.e("OneDrivePhotoRepo", "Failed to collect photos", e)
                    break
                }
            }

            for (sub in subfoldersToRecurse) {
                collectPhotos(sub)
            }
        }

        collectPhotos(folderId)

        photoListCache[cacheKey] = PhotoListCacheEntry(photos)
        return@withContext photos
    }

    override suspend fun getFilteredFolderMediaCountForAccount(
        folderId: String,
        mediaFilter: MediaTypeFilter?,
        accountId: String
    ): Int {
        val normalizedFilter = normalizeMediaFilter(mediaFilter)
        val cacheKey = "${accountId}_${folderId}_${normalizedFilter}"
        val cached = photoCountCache[cacheKey]
        if (cached != null && !cached.isStale) {
            return cached.count
        }
        // Instantly return 0 for cloud directories to prevent UI blocking
        return 0
    }

    override suspend fun searchFoldersForAccount(
        query: String,
        accountId: String
    ): List<PhotoFolder> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val token = authManager.getAccessToken(accountId) ?: return@withContext emptyList()

        val folders = mutableListOf<PhotoFolder>()
        val safeQuery = query.replace("'", "''")
        var url = "https://graph.microsoft.com/v1.0/me/drive/root/search(q='$safeQuery')?\$filter=folder ne null&\$select=id,name,parentReference"
        
        try {
            while (url.isNotEmpty()) {
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) break
                    val body = response.body?.string() ?: break
                    val json = JSONObject(body)
                    val value = json.optJSONArray("value") ?: break

                    for (i in 0 until value.length()) {
                        val item = value.getJSONObject(i)
                        folders.add(
                            PhotoFolder(
                                id = item.getString("id"),
                                sourceType = SourceType.ONEDRIVE,
                                accountId = accountId,
                                name = item.getString("name"),
                                parentFolderId = item.optJSONObject("parentReference")?.optString("id"),
                                photoCount = 0
                            )
                        )
                    }
                    url = json.optString("@odata.nextLink", "")
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("OneDrivePhotoRepo", "Failed to search folders", e)
        }
        
        return@withContext folders
    }

    override suspend fun getSubfolderIdsForAccount(
        folderId: String,
        accountId: String
    ): List<String> = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken(accountId) ?: return@withContext emptyList()
        val result = mutableListOf<String>()
        val visited = mutableSetOf<String>()

        fun collectSubfolders(currentId: String) {
            if (!visited.add(currentId)) return
            var url = "https://graph.microsoft.com/v1.0/me/drive/items/$currentId/children?\$filter=folder ne null&\$select=id"

            try {
                while (url.isNotEmpty()) {
                    val request = Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer $token")
                        .build()
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return
                        val body = response.body?.string() ?: return
                        val json = JSONObject(body)
                        val value = json.optJSONArray("value") ?: return
                        for (i in 0 until value.length()) {
                            val id = value.getJSONObject(i).getString("id")
                            result.add(id)
                            collectSubfolders(id)
                        }
                        url = json.optString("@odata.nextLink", "")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OneDrivePhotoRepo", "Failed to get subfolders", e)
            }
        }
        
        collectSubfolders(folderId)
        return@withContext result
    }
    
    suspend fun downloadPhotoToLocalCache(photoId: String, accountId: String, downloadUrl: String?): String? = withContext(Dispatchers.IO) {
        if (downloadUrl.isNullOrEmpty()) return@withContext null
        try {
            val cacheDir = File(context.cacheDir, "onedrive_photos")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val safeAccountId = java.net.URLEncoder.encode(accountId, "UTF-8")
            val safePhotoId = photoId.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
            val cacheFile = File(cacheDir, "${safeAccountId}_${safePhotoId}.jpg")
            
            if (cacheFile.exists()) return@withContext "file://${cacheFile.absolutePath}"

            val request = Request.Builder().url(downloadUrl).build()
            val tempFile = File(cacheDir, "${safeAccountId}_${safePhotoId}.tmp.${java.util.UUID.randomUUID()}")

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.byteStream()?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            if (!tempFile.renameTo(cacheFile) && !cacheFile.exists()) {
                return@withContext null
            }
            "file://${cacheFile.absolutePath}"
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("OneDrivePhotoRepo", "Error downloading OneDrive photo", e)
            null
        }
    }

    override suspend fun getPhotoMetadataForAccount(photoId: String, accountId: String): Photo? = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken(accountId) ?: return@withContext null
        
        val url = "https://graph.microsoft.com/v1.0/me/drive/items/$photoId?\$expand=thumbnails"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()
            
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val item = JSONObject(body)
                
                val id = item.getString("id")
                val name = item.getString("name")
                val downloadUrl = item.optString("@microsoft.graph.downloadUrl")
                val size = item.optLong("size", 0L)
                val dateStr = item.optString("lastModifiedDateTime")
                
                val thumbnails = item.optJSONArray("thumbnails")
                val thumbnailUrl = if (thumbnails != null && thumbnails.length() > 0) {
                    thumbnails.getJSONObject(0).optJSONObject("medium")?.optString("url") ?: downloadUrl
                } else {
                    downloadUrl
                }
                
                return@withContext Photo(
                    id = id,
                    sourceType = SourceType.ONEDRIVE,
                    accountId = accountId,
                    uri = downloadUrl,
                    thumbnailUri = thumbnailUrl,
                    title = name,
                    dateTaken = parseIso8601(dateStr),
                    width = item.optJSONObject("image")?.optInt("width") ?: item.optJSONObject("video")?.optInt("width"),
                    height = item.optJSONObject("image")?.optInt("height") ?: item.optJSONObject("video")?.optInt("height"),
                    fileSize = size
                )
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("OneDrivePhotoRepo", "Failed to get metadata", e)
            return@withContext null
        }
    }

    override suspend fun getPhotoUrlForAccount(photoId: String, accountId: String): String? {
        return getPhotoMetadataForAccount(photoId, accountId)?.uri
    }

    override suspend fun getThumbnailUrlForAccount(photoId: String, accountId: String): String? {
        return getPhotoMetadataForAccount(photoId, accountId)?.thumbnailUri
    }

    override suspend fun syncPhotos(): Boolean {
        val success = super.syncPhotos()
        withContext(Dispatchers.IO) {
            try {
                File(context.cacheDir, "onedrive_photos").deleteRecursively()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OneDrivePhotoRepo", "Failed to clear OneDrive disk cache: ${e.javaClass.simpleName}")
            }
        }
        return success
    }
}