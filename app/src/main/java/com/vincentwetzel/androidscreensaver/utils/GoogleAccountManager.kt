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
    @ApplicationContext private val context: Context
) {
    /** Represents the per-account auth state */
    data class AccountState(
        val account: GoogleSignInAccount,
        val driveService: Drive,
        val credential: GoogleAccountCredential
    )

    // Map from accountId to account state
    private val accountStates = mutableMapOf<String, AccountState>()

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
        val accountId = "gdrive:$email"

        // If already authenticated for this account, update the existing state
        val driveService = initializeDriveService(account)
        if (driveService != null) {
            val credential = accountStates[accountId]?.credential
                ?: GoogleAccountCredential.usingOAuth2(
                    context, listOf(DriveScopes.DRIVE_READONLY)
                ).apply { selectedAccount = account.account }

            accountStates[accountId] = AccountState(account, driveService, credential)
            android.util.Log.d("GoogleAccountManager", "Authenticated account: $accountId")
            return accountId
        }

        return null
    }

    /**
     * Check for existing sign-in on device and restore any known accounts from settings.
     * This is called at app startup to re-establish Drive services for saved accounts.
     */
    fun checkExistingSignIn() {
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null && GoogleOAuthConfig.CLIENT_ID.isNotEmpty()) {
            val email = lastAccount.email ?: return
            val accountId = "gdrive:$email"

            // Don't re-add if already tracked
            if (accountStates.containsKey(accountId)) return

            val driveService = initializeDriveService(lastAccount)
            if (driveService != null) {
                val credential = GoogleAccountCredential.usingOAuth2(
                    context, listOf(DriveScopes.DRIVE_READONLY)
                ).apply { selectedAccount = lastAccount.account }

                accountStates[accountId] = AccountState(lastAccount, driveService, credential)
                android.util.Log.d("GoogleAccountManager", "Restored existing account: $accountId")
            }
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
    fun getAccessToken(accountId: String): String? {
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
     * Check if a specific account is authenticated and has a valid Drive service.
     */
    fun isAccountAuthenticated(accountId: String): Boolean {
        return accountStates.containsKey(accountId) && accountStates[accountId]?.driveService != null
    }

    /**
     * Get all currently authenticated account IDs.
     */
    fun getAuthenticatedAccountIds(): Set<String> {
        return accountStates.keys.toSet()
    }

    /**
     * Sign out and remove a specific account.
     */
    fun signOutAccount(accountId: String) {
        accountStates.remove(accountId)
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
    fun signOutAll() {
        accountStates.clear()
        googleSignInClient.signOut()
        android.util.Log.d("GoogleAccountManager", "Signed out all accounts")
    }

    /**
     * Revoke access and remove all accounts.
     */
    fun revokeAll() {
        accountStates.clear()
        googleSignInClient.revokeAccess()
        android.util.Log.d("GoogleAccountManager", "Revoked all accounts")
    }

    /**
     * Initialize a Drive service for the given account.
     */
    private fun initializeDriveService(account: GoogleSignInAccount): Drive? {
        return try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_READONLY)
            )
            credential.selectedAccount = account.account

            Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Android Screensaver")
                .build()
        } catch (e: Exception) {
            android.util.Log.e("GoogleAccountManager", "Failed to initialize Drive service", e)
            null
        }
    }
}
