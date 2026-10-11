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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Singleton
import kotlinx.coroutines.suspendCancellableCoroutine

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
        return authManager.getCachedAccountIds()
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
        val cacheKey = RepositoryCacheKey.of(accountId, parentFolderId)
        
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

                httpClient.newCall(request).await().use { response ->
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
            android.util.Log.e("OneDrivePhotoRepo", "Failed to list folders: ${e.javaClass.simpleName}")
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
        val cacheKey = RepositoryCacheKey.of(accountId, folderId, normalizedFilter, RepositoryCacheKey.set(excludedFolderIds))
        
        val cached = photoListCache[cacheKey]
        if (cached != null && !cached.isStale) {
            return@withContext cached.data
        }

        val token = authManager.getAccessToken(accountId) 
            ?: throw IllegalStateException("Not authenticated for account: $accountId")

        val photos = mutableListOf<Photo>()
        val visited = mutableSetOf<String>()

        suspend fun collectPhotos(currentFolderId: String) {
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
                    httpClient.newCall(request).await().use { response ->
                        val body = requireOneDrivePageBody(response.code, response.body?.string())
                        val json = JSONObject(body)
                        val value = json.optJSONArray("value")
                            ?: throw IOException("Graph API page did not contain a value array")

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
                    android.util.Log.e("OneDrivePhotoRepo", "Failed to collect photos: ${e.javaClass.simpleName}")
                    throw e
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
        mediaTypeFilter: MediaTypeFilter?,
        accountId: String
    ): Int {
        val normalizedFilter = normalizeMediaFilter(mediaTypeFilter)
        val cacheKey = RepositoryCacheKey.of(accountId, folderId, normalizedFilter)
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
        val token = authManager.getAccessToken(accountId)
            ?: throw IllegalStateException("Not authenticated for account: $accountId")

        val folders = mutableListOf<PhotoFolder>()
        val safeQuery = query.replace("'", "''")
        var url = "https://graph.microsoft.com/v1.0/me/drive/root/search(q='$safeQuery')?\$filter=folder ne null&\$select=id,name,parentReference"
        
        try {
            while (url.isNotEmpty()) {
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .build()

                httpClient.newCall(request).await().use { response ->
                    val body = requireOneDrivePageBody(response.code, response.body?.string())
                    val json = JSONObject(body)
                    val value = json.optJSONArray("value")
                        ?: throw IOException("Graph API page did not contain a value array")

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
            android.util.Log.e("OneDrivePhotoRepo", "Failed to search folders: ${e.javaClass.simpleName}")
            throw e
        }
        
        return@withContext folders
    }

    override suspend fun getSubfolderIdsForAccount(
        folderId: String,
        accountId: String
    ): List<String> = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken(accountId)
            ?: throw IllegalStateException("Not authenticated for account: $accountId")
        val result = mutableListOf<String>()
        val visited = mutableSetOf<String>()

        suspend fun collectSubfolders(currentId: String) {
            if (!visited.add(currentId)) return
            var url = "https://graph.microsoft.com/v1.0/me/drive/items/$currentId/children?\$filter=folder ne null&\$select=id"

            try {
                while (url.isNotEmpty()) {
                    val request = Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer $token")
                        .build()
                    httpClient.newCall(request).await().use { response ->
                        val body = requireOneDrivePageBody(response.code, response.body?.string())
                        val json = JSONObject(body)
                        val value = json.optJSONArray("value")
                            ?: throw IOException("Graph API page did not contain a value array")
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
                android.util.Log.e("OneDrivePhotoRepo", "Failed to get subfolders: ${e.javaClass.simpleName}")
                throw e
            }
        }
        
        collectSubfolders(folderId)
        return@withContext result
    }
    
    suspend fun downloadPhotoToLocalCache(
        photoId: String,
        accountId: String,
        downloadUrl: String?,
        fileName: String?
    ): String? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "onedrive_photos")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val cacheKey = MediaCacheFilePolicy.cacheKey(accountId, photoId)

            val extension = oneDriveCacheExtension(fileName)

            val cacheFile = File(cacheDir, "${cacheKey}.$extension")
            if (cacheFile.exists()) return@withContext "file://${cacheFile.absolutePath}"

            val tempFile = File(cacheDir, "${cacheKey}.tmp.${java.util.UUID.randomUUID()}")
            try {
                val downloaded = downloadOneDrivePhotoWithRefresh(
                    downloadUrl,
                    refreshUrl = { getPhotoMetadataForAccount(photoId, accountId)?.uri },
                    download = { url -> downloadToTempFile(url, tempFile) }
                )
                if (!downloaded) return@withContext null
                if (!tempFile.renameTo(cacheFile) && !cacheFile.exists()) return@withContext null
                "file://${cacheFile.absolutePath}"
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("OneDrivePhotoRepo", "Error downloading OneDrive photo: ${e.javaClass.simpleName}")
            null
        }
    }

    private suspend fun downloadToTempFile(downloadUrl: String, tempFile: File): Boolean =
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            tempFile.delete()
            val request = Request.Builder().url(downloadUrl).build()
            val call = httpClient.newCall(request)
            continuation.invokeOnCancellation {
                call.cancel()
                tempFile.delete()
            }
            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    if (continuation.isActive) continuation.resume(false)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    try {
                        val downloaded = response.use {
                            if (!it.isSuccessful) return@use false
                            val body = it.body ?: return@use false
                            body.byteStream().use { input ->
                                tempFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            tempFile.length() > 0L
                        }
                        if (continuation.isActive) continuation.resume(downloaded)
                    } catch (e: Exception) {
                        tempFile.delete()
                        if (continuation.isActive) continuation.resumeWith(Result.failure(e))
                    }
                }
            })
        }

    override suspend fun getPhotoMetadataForAccount(photoId: String, accountId: String): Photo? = withContext(Dispatchers.IO) {
        val token = authManager.getAccessToken(accountId) ?: return@withContext null
        
        val url = "https://graph.microsoft.com/v1.0/me/drive/items/$photoId?\$expand=thumbnails"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()
            
        try {
            httpClient.newCall(request).await().use { response ->
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
            android.util.Log.e("OneDrivePhotoRepo", "Failed to get metadata: ${e.javaClass.simpleName}")
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

internal fun requireOneDrivePageBody(responseCode: Int, body: String?): String {
    if (responseCode !in 200..299) {
        throw IOException("Graph API request failed with HTTP $responseCode")
    }
    return body ?: throw IOException("Graph API page had no response body")
}

internal fun oneDriveCacheExtension(fileName: String?): String =
    fileName?.substringAfterLast('.', "")
        ?.lowercase(Locale.US)
        ?.takeIf { it.matches(Regex("[a-z0-9]{1,10}")) }
        ?: "bin"

internal suspend fun downloadOneDrivePhotoWithRefresh(
    initialUrl: String?,
    refreshUrl: suspend () -> String?,
    download: suspend (String) -> Boolean
): Boolean {
    if (!initialUrl.isNullOrBlank() && download(initialUrl)) return true
    val refreshedUrl = refreshUrl()
    return !refreshedUrl.isNullOrBlank() && refreshedUrl != initialUrl && download(refreshedUrl)
}

/** Suspend an OkHttp call and cancel its socket request when the caller is cancelled. */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) {
                continuation.resume(response)
            } else {
                response.close()
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    })
    continuation.invokeOnCancellation { cancel() }
}
