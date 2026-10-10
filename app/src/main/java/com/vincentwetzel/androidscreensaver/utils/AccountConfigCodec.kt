package com.vincentwetzel.androidscreensaver.utils

import com.vincentwetzel.androidscreensaver.data.model.AccountConfig
import com.vincentwetzel.androidscreensaver.data.model.SelectedFolder
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import java.net.URLDecoder
import java.net.URLEncoder

/** Pure delimiter-safe codec for the persisted per-account settings value. */
internal object AccountConfigCodec {

    fun decode(serialized: String): List<AccountConfig> {
        if (serialized.isEmpty()) return emptyList()

        return serialized.split(";;").filter { it.isNotEmpty() }.mapNotNull { accountString ->
            try {
                val parts = accountString.split("|")
                if (parts.size < 4) return@mapNotNull null

                val accountId = decodeValue(parts[0])
                val sourceType = try {
                    SourceType.valueOf(parts[1])
                } catch (_: IllegalArgumentException) {
                    return@mapNotNull null
                }
                val email = decodeValue(parts[2])
                val enabled = parts[3].toBoolean()

                val selectedFolders = if (parts.size > 4 && parts[4].isNotEmpty()) {
                    parts[4].split(",").mapNotNull { folderString ->
                        val folderParts = folderString.split("^")
                        when {
                            folderParts.size == 3 -> SelectedFolder(
                                folderId = decodeValue(folderParts[0]),
                                folderName = decodeValue(folderParts[1]),
                                path = decodeValue(folderParts[2]),
                                isSelected = true
                            )
                            folderParts.isNotEmpty() -> {
                                val folderId = decodeValue(folderParts[0])
                                SelectedFolder(folderId, folderId, path = folderId, isSelected = true)
                            }
                            else -> null
                        }
                    }
                } else {
                    emptyList()
                }

                val deselectedFolders = if (parts.size > 5 && parts[5].isNotEmpty()) {
                    parts[5].split(",").map { decodeValue(it) }.toSet()
                } else {
                    emptySet()
                }

                val isAuthenticated = when (sourceType) {
                    SourceType.GALLERY, SourceType.LOCAL_NETWORK -> true
                    else -> parts.size > 6 && parts[6].toBoolean()
                }
                val lastAuthTime = if (parts.size > 7 && parts[7].isNotEmpty()) parts[7].toLongOrNull() else null
                val lastSyncTime = if (parts.size > 8 && parts[8].isNotEmpty()) parts[8].toLongOrNull() else null
                val photoCount = if (parts.size > 9) parts[9].toIntOrNull() ?: 0 else 0

                AccountConfig(
                    accountId = accountId,
                    sourceType = sourceType,
                    accountEmail = email,
                    enabled = enabled,
                    selectedFolders = selectedFolders,
                    deselectedFolders = deselectedFolders,
                    isAuthenticated = isAuthenticated,
                    lastAuthTime = lastAuthTime,
                    lastSyncTime = lastSyncTime,
                    photoCount = photoCount
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    fun encode(accounts: List<AccountConfig>): String = accounts.joinToString(";;") { account ->
        val selectedFolders = account.selectedFolders.joinToString(",") { folder ->
            "${encode(folder.folderId)}^${encode(folder.folderName)}^${encode(folder.path)}"
        }
        val deselectedFolders = account.deselectedFolders.joinToString(",", transform = ::encode)
        val accountId = encode(account.accountId)
        val email = encode(account.accountEmail)

        "$accountId|${account.sourceType.name}|$email|${account.enabled}|$selectedFolders|$deselectedFolders|${account.isAuthenticated}|${account.lastAuthTime ?: ""}|${account.lastSyncTime ?: ""}|${account.photoCount}"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun decodeValue(value: String): String = URLDecoder.decode(value, "UTF-8")
}
