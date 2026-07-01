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
 * All methods require an explicit accountId to enforce explicit routing.
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
     * Get the OAuth access token for a specific account.
     */
    fun getAccessToken(accountId: String): String? {
        return accountManager.getAccessToken(accountId)
    }

    /**
     * Get the Drive API service for a specific account.
     */
    fun getDriveService(accountId: String): Drive? {
        return accountManager.getDriveService(accountId)
    }

    /**
     * Get the email for a specific account.
     */
    fun getAccountEmail(accountId: String): String? {
        return accountManager.getAccountEmail(accountId)
    }

    /**
     * Check if a specific account is authenticated.
     */
    fun isAccountAuthenticated(accountId: String): Boolean {
        return accountManager.isAccountAuthenticated(accountId)
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
