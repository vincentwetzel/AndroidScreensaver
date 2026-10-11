package com.vincentwetzel.androidscreensaver.auth

/** Provider account key and email returned after OneDrive authentication. */
data class OneDriveAuthenticatedAccount(
    val accountId: String,
    val email: String
)

internal object OneDriveAccountProfilePolicy {
    fun fromFields(
        graphId: String?,
        mail: String?,
        userPrincipalName: String?,
        fallbackEmail: String?
    ): OneDriveAuthenticatedAccount? {
        val email = mail?.takeIf(String::isNotBlank)
            ?: userPrincipalName?.takeIf(String::isNotBlank)
            ?: fallbackEmail?.takeIf(String::isNotBlank)
            ?: return null
        val accountId = graphId?.takeIf(String::isNotBlank)
            ?: fallbackEmail?.takeIf(String::isNotBlank)
            ?: return null
        return OneDriveAuthenticatedAccount(accountId, email)
    }
}

internal object OneDriveTokenPolicy {
    const val REFRESH_SKEW_MILLIS = 60_000L

    fun shouldRefresh(expiresAtMillis: Long, nowMillis: Long): Boolean =
        expiresAtMillis <= nowMillis + REFRESH_SKEW_MILLIS

    fun canUseAfterRefreshFailure(expiresAtMillis: Long, nowMillis: Long): Boolean =
        expiresAtMillis > nowMillis
}
