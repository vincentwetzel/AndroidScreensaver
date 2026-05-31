package com.vincentwetzel.androidscreensaver.data.repository

import android.content.Context
import android.content.Intent
import com.vincentwetzel.androidscreensaver.utils.DropboxAccountManager
import com.dropbox.core.v2.DbxClientV2
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dropbox Repository
 * Handles per-account Dropbox API access via DropboxAccountManager.
 * Supports multiple accounts.
 */
@Singleton
class DropboxRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountManager: DropboxAccountManager
) {

    init {
        accountManager.checkExistingSignIn()
    }

    /**
     * Get the sign-in intent to launch Dropbox authentication
     */
    fun getSignInIntent(): Intent {
        return accountManager.getSignInIntent()
    }

    /**
     * Handle the sign-in result. Returns the accountId if successful, null otherwise.
     */
    suspend fun handleSignInResult(): String? {
        return accountManager.handleSignInResult()
    }

    /**
     * Get the OAuth access token for a specific account.
     */
    fun getAccessToken(accountId: String): String? {
        return accountManager.getAccessToken(accountId)
    }

    /**
     * Get the email for a specific account.
     */
    fun getAccountEmail(accountId: String): String? {
        return accountManager.getAccountEmail(accountId)
    }

    /**
     * Get the Dropbox API service client for a specific account.
     */
    fun getDbxClientV2(accountId: String): DbxClientV2? {
        return accountManager.getDbxClientV2(accountId)
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
    fun getAuthenticatedAccountIds(): Set<String> {
        return accountManager.getAuthenticatedAccountIds()
    }

    /**
     * Sign out and remove a specific account.
     */
    fun signOutAccount(accountId: String) {
        accountManager.signOutAccount(accountId)
    }

    /**
     * Sign out all accounts.
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
}
