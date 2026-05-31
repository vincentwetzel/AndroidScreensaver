package com.vincentwetzel.androidscreensaver.utils

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.vincentwetzel.androidscreensaver.utils.GoogleOAuthConfig
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
        val account: GoogleSignInAccount?,
        val driveService: Drive,
        val credential: GoogleAccountCredential
    )

    // Google Sign-In client (shared across accounts)
    private val googleSignInClient: GoogleSignInClient by lazy {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
            .requestScopes(Scope(DriveScopes.DRIVE_METADATA_READONLY))
            .apply {
                if (GoogleOAuthConfig.WEB_CLIENT_ID.isNotEmpty()) {
                    requestIdToken(GoogleOAuthConfig.WEB_CLIENT_ID)
                }
            }
            .build()
        GoogleSignIn.getClient(context, signInOptions)
    }

    /**
     * Get the sign-in intent to launch Google authentication.
     */
    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    /**
     * Handle sign-in result and create an AccountState for the signed-in account.
     * Returns the accountId if successful, null otherwise.
     */
    fun handleSignInResult(account: GoogleSignInAccount?): String? {
        if (account == null) return null

        val email = account.email ?: return null

        restoreAccountFromEmail(email, account)
        return "gdrive:$email"
    }

    /**
     * Check for existing sign-in on device and restore any known accounts from settings.
     * This is called at app startup to re-establish Drive services for saved accounts.
     */
    override fun checkExistingSignIn() {
        // First restore the last signed-in account to keep the GoogleSignIn SDK happy
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null && GoogleOAuthConfig.CLIENT_ID.isNotEmpty()) {
            val email = lastAccount.email ?: return
            restoreAccountFromEmail(email, lastAccount)
        }

        // Restore all other known accounts from preferences
        val prefs = context.getSharedPreferences("google_drive_accounts", Context.MODE_PRIVATE)
        val knownEmails = prefs.all.keys
        for (email in knownEmails) {
            restoreAccountFromEmail(email, null)
        }
    }

    private fun restoreAccountFromEmail(email: String, googleSignInAccount: GoogleSignInAccount?) {
        val accountId = "gdrive:$email"
        if (accountStates.containsKey(accountId)) {
            // Update with the real GoogleSignInAccount object if we just got one
            if (googleSignInAccount != null && accountStates[accountId]?.account == null) {
                val currentState = accountStates[accountId]!!
                accountStates[accountId] = currentState.copy(account = googleSignInAccount)
            }
            return
        }

        try {
            val androidAccount = android.accounts.Account(email, "com.google")
            val credential = GoogleAccountCredential.usingOAuth2(context, GoogleOAuthConfig.SCOPES)
            credential.selectedAccount = androidAccount

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Android Screensaver").build()

            accountStates[accountId] = AccountState(googleSignInAccount, driveService, credential)

            // Track this email in preferences
            context.getSharedPreferences("google_drive_accounts", Context.MODE_PRIVATE)
                .edit().putBoolean(email, true).apply()

            android.util.Log.d("GoogleAccountManager", "Restored Google Drive account: $accountId")
        } catch (e: Exception) {
            android.util.Log.e("GoogleAccountManager", "Failed to restore account for $email", e)
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
        return try {
            accountStates[accountId]?.credential?.getToken()
        } catch (e: Exception) {
            android.util.Log.e("GoogleAccountManager", "Failed to get access token for $accountId", e)
            null
        }
    }

    /**
     * Get the GoogleSignInAccount for a specific account.
     */
    fun getAccount(accountId: String): GoogleSignInAccount? {
        return accountStates[accountId]?.account
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
        android.util.Log.d("GoogleAccountManager", "Removed account: $accountId")
    }

    /**
     * Signs out of the current Google Sign-In session to allow for a different
     * account to be chosen. This does NOT remove tracked accounts or revoke tokens.
     * It's used to force the account picker to appear when adding a new account.
     */
    suspend fun forceAccountPicker() {
        try {
            googleSignInClient.signOut().await()
            android.util.Log.d("GoogleAccountManager", "Signed out of current session to force account picker.")
        } catch (e: Exception) {
            android.util.Log.e("GoogleAccountManager", "Failed to sign out to force account picker", e)
        }
    }

    /**
     * Sign out all accounts and clear the Google Sign-In session.
     */
    override fun signOutAll() {
        super.signOutAll()
        context.getSharedPreferences("google_drive_accounts", Context.MODE_PRIVATE).edit().clear().apply()
        googleSignInClient.signOut()
        android.util.Log.d("GoogleAccountManager", "Signed out all accounts")
    }

    /**
     * Revoke access and remove all accounts.
     */
    override suspend fun revokeAll() {
        super.signOutAll()
        context.getSharedPreferences("google_drive_accounts", Context.MODE_PRIVATE).edit().clear().apply()
        googleSignInClient.revokeAccess()
        android.util.Log.d("GoogleAccountManager", "Revoked all accounts")
    }
}
