package com.vincentwetzel.androidscreensaver

import android.content.ContextWrapper
import com.vincentwetzel.androidscreensaver.data.model.FolderError
import com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.data.repository.BaseCloudPhotoRepository
import com.vincentwetzel.androidscreensaver.viewmodel.CloudFolderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class CloudFolderViewModelUnitTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCloudRepository
    private lateinit var viewModel: CloudFolderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCloudRepository()
        viewModel = CloudFolderViewModel(mapOf(SourceType.GOOGLE_DRIVE to repository))
    }

    @After
    fun tearDown() {
        viewModel.clearNavigationBackStack()
        Dispatchers.resetMain()
    }

    @Test
    fun `load maps filtered counts and maintains back stack`() = runTest(dispatcher) {
        viewModel.setSourceContext(SourceType.GOOGLE_DRIVE, "account")

        viewModel.loadFolders(mediaFilter = MediaTypeFilter.IMAGES_ONLY)
        advanceUntilIdle()

        assertEquals(null, viewModel.currentFolderId.value)
        assertEquals(listOf<String?>(null), viewModel.getNavigationBackStack())
        assertEquals(listOf("child"), viewModel.folders.value.map { it.id })
        assertEquals(3, viewModel.getPhotoCount())
        assertEquals(MediaTypeFilter.IMAGES_ONLY, repository.lastMediaFilter)

        viewModel.navigateToFolder("child")
        advanceUntilIdle()
        assertEquals("child", viewModel.currentFolderId.value)
        assertEquals(listOf<String?>(null, null), viewModel.getNavigationBackStack())

        assertTrue(viewModel.navigateBack())
        advanceUntilIdle()
        assertEquals(null, viewModel.currentFolderId.value)
        assertEquals(listOf<String?>(null), viewModel.getNavigationBackStack())
        assertTrue(viewModel.navigateBack())
        advanceUntilIdle()
        assertFalse(viewModel.navigateBack())
    }

    @Test
    fun `missing account reports an actionable error without launching a job`() = runTest(dispatcher) {
        viewModel.loadFolders()

        assertEquals("No account configured for folder browsing", viewModel.error.value?.userMessage())
        assertFalse(viewModel.isLoading.value)
        assertEquals(0, repository.listFoldersCalls)
    }

    @Test
    fun `provider failure becomes a network error and can be cleared`() = runTest(dispatcher) {
        repository.failWithNetworkError = true
        viewModel.setSourceContext(SourceType.GOOGLE_DRIVE, "account")

        viewModel.loadFolders()
        advanceUntilIdle()

        assertTrue(viewModel.error.value is FolderError.NetworkError)
        assertFalse(viewModel.isLoading.value)
        viewModel.clearError()
        assertEquals(null, viewModel.error.value)
    }

    @Test
    fun `missing cloud credentials map to an authentication error`() {
        val error = FolderError.fromException(IllegalStateException("Not authenticated for account"))
        assertTrue(error is FolderError.AuthError)
    }
    @Test
    fun `search results use the configured account and blank search reloads current folder`() = runTest(dispatcher) {
        viewModel.setSourceContext(SourceType.GOOGLE_DRIVE, "account")
        viewModel.loadFolders()
        advanceUntilIdle()

        viewModel.searchFolders("vacation")
        advanceUntilIdle()
        assertEquals(listOf("search-result"), viewModel.folders.value.map { it.id })
        assertEquals("account", repository.lastAccountId)

        viewModel.searchFolders("   ")
        advanceUntilIdle()
        assertEquals(listOf("child"), viewModel.folders.value.map { it.id })
    }

    private class FakeCloudRepository : BaseCloudPhotoRepository(
        ContextWrapper(null),
        com.vincentwetzel.androidscreensaver.dream.SourceType.GOOGLE_DRIVE
    ) {
        var listFoldersCalls = 0
        var lastAccountId: String? = null
        var lastMediaFilter: MediaTypeFilter? = null
        var failWithNetworkError = false

        override fun isAuthenticated() = true
        override fun getAuthenticatedAccountIds() = listOf("account")

        override suspend fun listFoldersForAccount(parentFolderId: String?, forceRefresh: Boolean, accountId: String): List<PhotoFolder> {
            listFoldersCalls++
            lastAccountId = accountId
            if (failWithNetworkError) throw UnknownHostException("offline")
            return if (parentFolderId == "child") {
                listOf(PhotoFolder("grandchild", SourceType.GOOGLE_DRIVE, accountId = accountId, name = "Grandchild"))
            } else {
                listOf(PhotoFolder("child", SourceType.GOOGLE_DRIVE, accountId = accountId, name = "Child"))
            }
        }

        override suspend fun listPhotosForAccount(folderId: String, excludedFolderIds: Set<String>, mediaTypeFilter: MediaTypeFilter?, accountId: String) = emptyList<Photo>()

        override suspend fun getFilteredFolderMediaCountForAccount(folderId: String, mediaTypeFilter: MediaTypeFilter?, accountId: String): Int {
            lastAccountId = accountId
            lastMediaFilter = mediaTypeFilter
            return if (folderId == "child") 3 else 1
        }

        override suspend fun getSubfolderIdsForAccount(folderId: String, accountId: String) = listOf("grandchild")
        override suspend fun getPhotoMetadataForAccount(photoId: String, accountId: String): Photo? = null
        override suspend fun getPhotoUrlForAccount(photoId: String, accountId: String): String? = null
        override suspend fun getThumbnailUrlForAccount(photoId: String, accountId: String): String? = null

        override suspend fun searchFoldersForAccount(query: String, accountId: String): List<PhotoFolder> {
            lastAccountId = accountId
            return listOf(PhotoFolder("search-result", SourceType.GOOGLE_DRIVE, accountId = accountId, name = query))
        }
    }
}
