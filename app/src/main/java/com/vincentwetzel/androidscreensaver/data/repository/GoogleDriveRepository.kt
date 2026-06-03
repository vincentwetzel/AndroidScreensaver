package com.vincentwetzel.androidscreensaver.data.repository

import android.content.Context
import android.content.Intent
import com.google.api.services.drive.Drive
import com.vincentwetzel.androidscreensaver.utils.GoogleAccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive Repository
 * Handles per-account Drive API access via GoogleAccountManager.
 * All methods now accept an accountId to support multiple accounts.
 * Legacy single-account behavior is preserved via accountId = null (uses the first available account).
 */
@Singleton
class GoogleDriveRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountManager: GoogleAccountManager
) {

    init {
        // Automatically check for existing sign-in when repository is created
        accountManager.checkExistingSignIn()
    }

    /**
     * Get the sign-in intent to launch Google authentication
     */
    suspend fun getSignInIntent(): Intent = accountManager.getSignInIntent()

    /**
     * Handle the sign-in result. Returns the accountId if successful, null otherwise.
     */
    fun handleSignInResult(data: Intent?): String? {
        return accountManager.handleSignInResult(data)
    }

    /**
     * Get the OAuth access token for a specific account (for use with OkHttp).
     * If accountId is null, returns the token for the first available account (legacy compat).
     */
    fun getAccessToken(accountId: String? = null): String? {
        val id = accountId ?: accountManager.getAuthenticatedAccountIds().firstOrNull()
            ?: return null
        return accountManager.getAccessToken(id)
    }

    /**
     * Get the Drive API service for a specific account.
     * If accountId is null, returns the service for the first available account (legacy compat).
     */
    fun getDriveService(accountId: String? = null): Drive? {
        val id = accountId ?: accountManager.getAuthenticatedAccountIds().firstOrNull()
            ?: return null
        return accountManager.getDriveService(id)
    }

    /**
     * Get the email for a specific account.
     */
    fun getAccountEmail(accountId: String): String? {
        return accountManager.getAccountEmail(accountId)
    }

    /**
     * Check if a specific account is authenticated.
     * If accountId is null, checks if any account is authenticated (legacy compat).
     */
    fun isAccountAuthenticated(accountId: String? = null): Boolean {
        return if (accountId != null) {
            accountManager.isAccountAuthenticated(accountId)
        } else {
            accountManager.getAuthenticatedAccountIds().isNotEmpty()
        }
    }

    /**
     * Get all currently authenticated account IDs.
     */
    fun getAuthenticatedAccountIds(): Set<String> = accountManager.getAuthenticatedAccountIds()

    /**
     * Sign out and remove a specific account.
     */
    fun signOutAccount(accountId: String) {
        accountManager.signOutAccount(accountId)
    }

    /**
     * Sign out all accounts and clear the Google Sign-In session.
     */
    fun signOutAll() {
        accountManager.signOutAll()
    }

    /**
     * Revoke access and remove all accounts.
     */
    suspend fun revokeAll() {
        accountManager.revokeAll()
    }

    /**
     * Verifies that the app has been granted the required Drive scopes.
     * Throws UserRecoverableAuthIOException if scopes are missing.
     */
    @Throws(Exception::class)
    suspend fun verifyDriveAccess(accountId: String) = withContext(Dispatchers.IO) {
        val service = getDriveService(accountId)
            ?: throw IllegalStateException("Drive service not found for account")

        // Lightweight API call forces the credential to fetch an OAuth token
        service.about().get().setFields("user").execute()
    }
}
