package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.data.model.AccountConfig
import com.vincentwetzel.androidscreensaver.data.model.FolderError
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder
import com.vincentwetzel.androidscreensaver.data.model.SelectedFolder
import com.vincentwetzel.androidscreensaver.data.model.SourceConfig
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.ui.main.SourceType as UiSourceType
import com.vincentwetzel.androidscreensaver.utils.SecureLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class ModelBehaviorUnitTest {

    @Test
    fun `source config only returns enabled authenticated accounts`() {
        val source = SourceConfig(
            sourceType = SourceType.GOOGLE_DRIVE,
            accounts = listOf(
                AccountConfig("one", SourceType.GOOGLE_DRIVE, "one@example.com", enabled = true, isAuthenticated = true),
                AccountConfig("two", SourceType.GOOGLE_DRIVE, "two@example.com", enabled = false, isAuthenticated = true),
                AccountConfig("three", SourceType.GOOGLE_DRIVE, "three@example.com", enabled = true, isAuthenticated = false)
            )
        )

        assertEquals(listOf("one"), source.getEnabledAccounts().map { it.accountId })
        assertEquals(0, source.getTotalPhotoCount())
        assertEquals("two@example.com", source.getAccount("two")?.accountEmail)
        assertNull(source.getAccount("missing"))
    }

    @Test
    fun `source config totals photo counts across all accounts`() {
        val accounts = listOf(
            AccountConfig("one", SourceType.DROPBOX, "one@example.com", photoCount = 4),
            AccountConfig("two", SourceType.DROPBOX, "two@example.com", photoCount = 7)
        )

        assertEquals(11, SourceConfig(SourceType.DROPBOX, accounts).getTotalPhotoCount())
    }

    @Test
    fun `selected folder preserves metadata and supports copy updates`() {
        val folder = SelectedFolder("id", "Vacation", path = "Pictures/Vacation", photoCount = 12)

        assertEquals("Pictures/Vacation", folder.path)
        assertEquals(12, folder.photoCount)
        assertTrue(folder.copy(isSelected = false).isSelected.not())
    }

    @Test
    fun `folder errors classify network auth permission api empty and unknown failures`() {
        assertTrue(FolderError.fromException(UnknownHostException()).userMessage().contains("internet"))
        assertTrue(FolderError.fromException(SecurityException("not allowed")).userMessage().contains("sign in"))
        assertTrue(FolderError.fromException(Exception("permission denied")).userMessage().contains("Permission"))
        assertTrue(FolderError.fromException(Exception("server returned 503")).userMessage().contains("later"))
        assertTrue(FolderError.fromException(Exception("no folders available")).userMessage().contains("No folders"))
        assertTrue(FolderError.fromException(IllegalStateException("unexpected state")).userMessage().contains("IllegalStateException"))
    }

    @Test
    fun `folder error custom messages are retained`() {
        val error = FolderError.ApiError("provider is temporarily unavailable")

        assertEquals("provider is temporarily unavailable", error.userMessage())
    }

    @Test
    fun `ui source types map to matching dream source types`() {
        UiSourceType.values().forEach { uiType ->
            assertEquals(uiType.name, uiType.toDreamSourceType().name)
        }
    }

    @Test
    fun `secure links expose expected https destinations`() {
        assertTrue(SecureLinks.discordInvite.startsWith("https://discord.gg/"))
        assertEquals("https://github.com/vincentwetzel/AndroidScreensaver", SecureLinks.githubRepo)
    }
}
