package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.data.model.AccountConfig
import com.vincentwetzel.androidscreensaver.data.model.SelectedFolder
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.utils.AccountConfigCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCodecUnitTest {

    @Test
    fun `account codec round trips delimiter-rich account and folder values`() {
        val account = AccountConfig(
            accountId = "gdrive:work|team;;primary",
            sourceType = SourceType.GOOGLE_DRIVE,
            accountEmail = "person+photos@example.com|archive",
            enabled = false,
            selectedFolders = listOf(
                SelectedFolder(
                    folderId = "folder|1,;;^",
                    folderName = "Summer | 2026",
                    path = "DCIM/Trip; 1",
                    isSelected = true
                )
            ),
            deselectedFolders = setOf("excluded|folder", "another;;folder"),
            isAuthenticated = true,
            lastAuthTime = 123L,
            lastSyncTime = 456L,
            photoCount = 17
        )

        val encoded = AccountConfigCodec.encode(listOf(account))

        assertEquals(listOf(account), AccountConfigCodec.decode(encoded))
    }

    @Test
    fun `empty account lists encode and decode as empty`() {
        assertEquals("", AccountConfigCodec.encode(emptyList()))
        assertTrue(AccountConfigCodec.decode("").isEmpty())
    }

    @Test
    fun `local source accounts are always authenticated when decoded`() {
        val gallery = AccountConfig(
            accountId = "gallery",
            sourceType = SourceType.GALLERY,
            accountEmail = "local",
            isAuthenticated = false
        )

        val decoded = AccountConfigCodec.decode(AccountConfigCodec.encode(listOf(gallery))).single()

        assertTrue(decoded.isAuthenticated)
    }

    @Test
    fun `malformed and unknown-source records are ignored without dropping valid records`() {
        val valid = AccountConfig("valid", SourceType.DROPBOX, "valid@example.com", isAuthenticated = true)
        val serialized = listOf(
            AccountConfigCodec.encode(listOf(valid)),
            "too-short|record",
            "broken|NOT_A_SOURCE|email@example.com|true"
        ).joinToString(";;")

        assertEquals(listOf(valid), AccountConfigCodec.decode(serialized))
    }

    @Test
    fun `invalid optional account metadata falls back safely`() {
        val serialized = "account|DROPBOX|email%40example.com|true||||true|not-a-time|also-not-a-time|not-a-count"

        val decoded = AccountConfigCodec.decode(serialized).single()

        assertEquals(null, decoded.lastAuthTime)
        assertEquals(null, decoded.lastSyncTime)
        assertEquals(0, decoded.photoCount)
    }
}
