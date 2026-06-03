package com.vincentwetzel.androidscreensaver.utils

import android.content.Context
import android.content.Intent
import com.google.android.gms.common.AccountPicker
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Manages multiple Google accounts for Drive access.
 * Each account has its own Drive service, credential, and auth state.
 * This replaces the singleton auth approach in GoogleDriveRepository.
 */
@Singleton
class GoogleAccountManager @Inject constructor(
    @ApplicationContext context: Context
) : BaseAccountManager<GoogleAccountManager.AccountState>(context) {

    /** Represents the per-account auth state */
    data class AccountState(
        val driveService: Drive?,
        val credential: GoogleAccountCredential?,
        val accessToken: String?
    )

    /**
     * Get the sign-in intent to launch Google authentication.
     */
    suspend fun getSignInIntent(): Intent {
        return AccountPicker.newChooseAccountIntent(
            AccountPicker.AccountChooserOptions.Builder()
                .setAllowableAccountsTypes(listOf("com.google"))
                .build()
        )
    }

    /**
     * Handle sign-in result and create an AccountState for the signed-in account.
     * Returns the accountId if successful, null otherwise.
     */
    fun handleSignInResult(data: Intent?): String? {
        if (data == null) return null
        return try {
            val email = data.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME) ?: return null
            restoreAccountFromEmail(email)
            "gdrive:$email"
        } catch (e: Exception) {
                android.util.Log.e("GoogleAccountManager", "Failed to parse sign in result")
            null
        }
    }

    /**
     * Check for existing sign-in on device and restore any known accounts from settings.
     * This is called at app startup to re-establish Drive services for saved accounts.
     */
    override fun checkExistingSignIn() {
        val prefs = context.getSharedPreferences("google_drive_accounts", Context.MODE_PRIVATE)
        val knownEmails = prefs.all.keys
        for (email in knownEmails) {
            restoreAccountFromEmail(email)
        }
    }

    private fun restoreAccountFromEmail(email: String) {
        val accountId = "gdrive:$email"
        if (accountStates.containsKey(accountId)) return
        
        try {
            android.util.Log.d("GoogleAccountManager", "Attempting to restore account from email")
            val androidAccount = android.accounts.Account(email, "com.google")
            android.util.Log.d("GoogleAccountManager", "Requested Drive Scope: ${DriveScopes.DRIVE_READONLY}")
            val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_READONLY))
            credential.selectedAccount = androidAccount

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Android Screensaver").build()
            
            accountStates[accountId] = AccountState(driveService, credential, null)
            
            // Track this email in preferences
            context.getSharedPreferences("google_drive_accounts", Context.MODE_PRIVATE)
                .edit().putBoolean(email, true).apply()
                
            android.util.Log.d("GoogleAccountManager", "Restored Google Drive account")
        } catch (e: Exception) {
            android.util.Log.e("GoogleAccountManager", "Failed to restore Google Drive account: ${e.javaClass.simpleName}")
        }
    }

    /**
     * Get the Drive service for a specific account.
     */
    fun getDriveService(accountId: String): Drive? {
        return accountStates[accountId]?.driveService
    }

    /**
     * Get the OAuth credential for a specific account.
     */
    fun getCredential(accountId: String): GoogleAccountCredential? {
        return accountStates[accountId]?.credential
    }

    /**
     * Get the access token for a specific account (for use with OkHttp).
     */
    override fun getAccessToken(accountId: String): String? {
        accountStates[accountId]?.accessToken?.let { return it }
        return try {
            accountStates[accountId]?.credential?.getToken()
        } catch (e: Exception) {
            android.util.Log.e("GoogleAccountManager", "Failed to get access token: ${e.javaClass.simpleName}")
            null
        }
    }

    /**
     * Get the email for a specific account.
     */
    fun getAccountEmail(accountId: String): String? {
        return accountId.removePrefix("gdrive:")
    }

    /**
     * Check if a specific account is authenticated and has a valid Drive service.
     */
    override fun isAccountAuthenticated(accountId: String): Boolean {
        return super.isAccountAuthenticated(accountId) && accountStates[accountId]?.driveService != null
    }

    /**
     * Sign out and remove a specific account.
     */
    override fun signOutAccount(accountId: String) {
        super.signOutAccount(accountId)
        val email = accountId.removePrefix("gdrive:")
        context.getSharedPreferences("google_drive_accounts", Context.MODE_PRIVATE)
            .edit().remove(email).apply()
        android.util.Log.d("GoogleAccountManager", "Removed account")
    }

    /**
     * Sign out all accounts and clear the Google Sign-In session.
     */
    override fun signOutAll() {
        super.signOutAll()
        context.getSharedPreferences("google_drive_accounts", Context.MODE_PRIVATE).edit().clear().apply()
        android.util.Log.d("GoogleAccountManager", "Signed out all accounts")
    }

    /**
     * Revoke access and remove all accounts.
     */
    override suspend fun revokeAll() {
        super.signOutAll()
        context.getSharedPreferences("google_drive_accounts", Context.MODE_PRIVATE).edit().clear().apply()
        android.util.Log.d("GoogleAccountManager", "Revoked all accounts")
    }
}
