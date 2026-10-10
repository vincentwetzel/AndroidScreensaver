package com.vincentwetzel.androidscreensaver

import android.content.ContextWrapper
import com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.data.repository.AbstractPhotoRepository
import com.vincentwetzel.androidscreensaver.data.repository.BaseCloudPhotoRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepositoryBehaviorUnitTest {

    @Test
    fun `abstract repository normalizes media filters for cache keys`() {
        val repository = CacheRepository()

        assertEquals("images", repository.normalized(MediaTypeFilter.IMAGES_ONLY))
        assertEquals("videos", repository.normalized(MediaTypeFilter.VIDEOS_ONLY))
        assertEquals("all", repository.normalized(MediaTypeFilter.IMAGES_AND_VIDEOS))
        assertEquals("all", repository.normalized(null))
    }

    @Test
    fun `sync clears every repository cache`() = runBlocking {
        val repository = CacheRepository()
        repository.seedCaches()

        assertEquals(Triple(1, 1, 1), repository.cacheSizes())
        assertEquals(true, repository.syncPhotos())
        assertEquals(Triple(0, 0, 0), repository.cacheSizes())
    }

    @Test
    fun `cloud repository rejects missing account ids without calling provider`() = runBlocking {
        val repository = CloudRepository()

        assertEquals(emptyList<PhotoFolder>(), repository.listFolders(null, false, null))
        assertEquals(emptyList<Photo>(), repository.listPhotos("root", emptySet(), null, null))
        assertEquals(0, repository.getFilteredFolderMediaCount("root", null, null))
        assertEquals(emptyList<String>(), repository.getSubfolderIds("root", null))
        assertNull(repository.getPhotoMetadata("photo", null))
        assertNull(repository.getPhotoUrl("photo", null))
        assertNull(repository.getThumbnailUrl("photo", null))
        assertEquals(emptyList<PhotoFolder>(), repository.searchFolders("vacation", null))
        assertEquals(0, repository.providerCalls)
    }

    @Test
    fun `cloud repository routes every operation to explicit account`() = runBlocking {
        val repository = CloudRepository()

        repository.listFolders("parent", true, "account")
        repository.listPhotos("folder", setOf("excluded"), MediaTypeFilter.VIDEOS_ONLY, "account")
        repository.getFilteredFolderMediaCount("folder", MediaTypeFilter.IMAGES_ONLY, "account")
        repository.getSubfolderIds("folder", "account")
        repository.getPhotoMetadata("photo", "account")
        repository.getPhotoUrl("photo", "account")
        repository.getThumbnailUrl("photo", "account")
        repository.searchFolders("vacation", "account")

        assertEquals(8, repository.providerCalls)
        assertEquals("account", repository.lastAccountId)
        assertEquals(listOf(MediaTypeFilter.VIDEOS_ONLY, MediaTypeFilter.IMAGES_ONLY), repository.mediaFilters)
    }

    private class CacheRepository : AbstractPhotoRepository() {
        fun normalized(filter: MediaTypeFilter?): String = normalizeMediaFilter(filter)

        fun seedCaches() {
            folderCache["folder"] = CacheEntry(emptyList())
            photoListCache["photos"] = PhotoListCacheEntry(emptyList())
            photoCountCache["count"] = CountCacheEntry(1)
        }

        fun cacheSizes() = Triple(folderCache.size, photoListCache.size, photoCountCache.size)

        override fun isAuthenticated() = true
        override suspend fun listFolders(parentFolderId: String?, forceRefresh: Boolean, accountId: String?) = emptyList<PhotoFolder>()
        override suspend fun listPhotos(folderId: String, excludedFolderIds: Set<String>, mediaTypeFilter: MediaTypeFilter?, accountId: String?) = emptyList<Photo>()
        override suspend fun getPhotoMetadata(photoId: String, accountId: String?) = null
        override suspend fun getPhotoUrl(photoId: String, accountId: String?) = null
        override suspend fun getThumbnailUrl(photoId: String, accountId: String?) = null
        override suspend fun searchFolders(query: String, accountId: String?) = emptyList<PhotoFolder>()
        override suspend fun getFolderPhotoCount(folderId: String, accountId: String?) = 0
        override suspend fun getFilteredFolderMediaCount(folderId: String, mediaTypeFilter: MediaTypeFilter?, accountId: String?) = 0
        override suspend fun getSubfolderIds(folderId: String, accountId: String?) = emptyList<String>()
    }

    private class CloudRepository : BaseCloudPhotoRepository(
        ContextWrapper(null),
        com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE
    ) {
        var providerCalls = 0
        var lastAccountId: String? = null
        var lastMediaFilter: MediaTypeFilter? = null
        val mediaFilters = mutableListOf<MediaTypeFilter>()

        override fun isAuthenticated() = true
        override fun getAuthenticatedAccountIds() = listOf("account")

        override suspend fun listFoldersForAccount(parentFolderId: String?, forceRefresh: Boolean, accountId: String): List<PhotoFolder> {
            record(accountId)
            return emptyList()
        }

        override suspend fun listPhotosForAccount(folderId: String, excludedFolderIds: Set<String>, mediaTypeFilter: MediaTypeFilter?, accountId: String): List<Photo> {
            record(accountId, mediaTypeFilter)
            return emptyList()
        }

        override suspend fun getFilteredFolderMediaCountForAccount(folderId: String, mediaTypeFilter: MediaTypeFilter?, accountId: String): Int {
            record(accountId, mediaTypeFilter)
            return 0
        }

        override suspend fun getSubfolderIdsForAccount(folderId: String, accountId: String): List<String> {
            record(accountId)
            return emptyList()
        }

        override suspend fun getPhotoMetadataForAccount(photoId: String, accountId: String): Photo? {
            record(accountId)
            return null
        }

        override suspend fun getPhotoUrlForAccount(photoId: String, accountId: String): String? {
            record(accountId)
            return null
        }

        override suspend fun getThumbnailUrlForAccount(photoId: String, accountId: String): String? {
            record(accountId)
            return null
        }

        override suspend fun searchFoldersForAccount(query: String, accountId: String): List<PhotoFolder> {
            record(accountId)
            return emptyList()
        }

        private fun record(accountId: String, filter: MediaTypeFilter? = null) {
            providerCalls++
            lastAccountId = accountId
            if (filter != null) {
                lastMediaFilter = filter
                mediaFilters += filter
            }
        }
    }
}
