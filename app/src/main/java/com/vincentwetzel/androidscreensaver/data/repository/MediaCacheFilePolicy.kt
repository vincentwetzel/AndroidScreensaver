package com.vincentwetzel.androidscreensaver.data.repository

import java.security.MessageDigest
import java.nio.charset.StandardCharsets

/** Keeps provider controlled filename suffixes from becoming path components. */
internal object MediaCacheFilePolicy {
    private val validExtension = Regex("[A-Za-z0-9]{1,10}")

    fun extension(fileName: String?): String? = fileName
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.substringAfterLast('.', "")
        ?.takeIf(validExtension::matches)

    /** Creates a stable, account-scoped filename without lossy provider-ID sanitization. */
    fun cacheKey(accountId: String, mediaId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(accountId.toByteArray(StandardCharsets.UTF_8))
        digest.update(0)
        val hash = digest.digest(mediaId.toByteArray(StandardCharsets.UTF_8))
        val alphabet = "0123456789abcdef"
        return buildString(hash.size * 2) {
            hash.forEach { byte ->
                val value = byte.toInt() and 0xff
                append(alphabet[value ushr 4])
                append(alphabet[value and 0x0f])
            }
        }
    }
}
