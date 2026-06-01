package com.vincentwetzel.androidscreensaver.utils

import android.content.Context
import android.content.Intent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.http.OkHttp3Requestor
import okhttp3.OkHttpClient
import com.dropbox.core.v2.DbxClientV2
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.dropbox.core.oauth.DbxCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.dropbox.core.v2.users.FullAccount

@Singleton
class DropboxAccountManager @Inject constructor(
    @ApplicationContext context: Context
) : BaseAccountManager<DropboxAccountManager.AccountState>(context) {
    private val ACCESS_TOKEN_PREF_NAME = "dropbox_access_tokens"
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        ACCESS_TOKEN_PREF_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    data class AccountState(
        val accountId: String,
        val credential: DbxCredential,
        val client: DbxClientV2,
        val email: String? = null // Store email for display/identification
    )

    init {
        checkExistingSignIn()
    }

    /**
     * Get the sign-in intent to launch Dropbox authentication.
     */
    fun getSignInIntent(): Intent {
        // The Dropbox Auth.startOAuth2Authentication takes Context and App Key.
        // It internally handles launching the browser and redirecting.
        // We're providing an empty intent here because the actual auth process
        // is started by the AuthActivity and handled by the browser.
        // The handleSignInResult method will be called when the AuthActivity resumes.
        // This is primarily for consistency with how GoogleSignInClient.getSignInIntent() works.
        return Intent() // Actual auth starts via AuthActivity
    }

    /**
     * Handle sign-in result and create an AccountState for the signed-in account.
     * Returns the accountId if successful, null otherwise.
     */
    suspend fun handleSignInResult(): String? {
        return withContext(Dispatchers.IO) {
            val credential = Auth.getDbxCredential()
            if (credential == null) {
                Log.e("DropboxAccountManager", "Failed to get DbxCredential from PKCE flow")
                return@withContext null
            }

            // Create DbxClientV2 to get user info
            val config = DbxRequestConfig.newBuilder("android-screensaver")
                .withHttpRequestor(OkHttp3Requestor(OkHttpClient()))
                .build()
            val client = DbxClientV2(config, credential)

            try {
                val currentAccount: FullAccount = client.users().currentAccount
                val accountId = "dropbox:${currentAccount.accountId}" // Use Dropbox account ID for uniqueness
                val email = currentAccount.email

                if (accountStates.containsKey(accountId)) {
                    Log.d("DropboxAccountManager", "Account $accountId already authenticated. Updating token.")
                }

                accountStates[accountId] = AccountState(accountId, credential, client, email)
                Log.d("DropboxAccountManager", "Authenticated Dropbox account: $accountId")

                val credJson = JSONObject().apply {
                    put("accessToken", credential.accessToken)
                    credential.expiresAt?.let { put("expiresAt", it) }
                    credential.refreshToken?.let { put("refreshToken", it) }
                    credential.appKey?.let { put("appKey", it) }
                }.toString()

                // Save credential securely
                sharedPreferences.edit()
                    .putString(accountId, credJson)
                    .putString("${accountId}_email", email)
                    .apply()

                accountId
            } catch (e: Exception) {
                Log.e("DropboxAccountManager", "Error getting Dropbox user info or creating client", e)
                null
            }
        }
    }

    /**
     * Check for existing sign-in.
     * Loads saved access tokens from secure storage and initializes accountStates.
     */
    override fun checkExistingSignIn() {
        val savedAccountIds = sharedPreferences.all.keys
        for (accountId in savedAccountIds) {
            // Skip email auxiliary keys during the main iteration
            if (accountId.endsWith("_email")) continue
            
            val savedData = sharedPreferences.getString(accountId, null)
            if (!savedData.isNullOrEmpty()) {
                try {
                    val json = JSONObject(savedData)
                    val expiresAt = if (json.isNull("expiresAt")) null else json.getLong("expiresAt")
                    val refreshToken = if (json.isNull("refreshToken")) null else json.getString("refreshToken")
                    val appKey = if (json.isNull("appKey")) null else json.getString("appKey")
                    
                    val credential = DbxCredential(
                        json.getString("accessToken"),
                        expiresAt,
                        refreshToken,
                        appKey
                    )
                    
                    val config = DbxRequestConfig.newBuilder("android-screensaver")
                        .withHttpRequestor(OkHttp3Requestor(OkHttpClient()))
                        .build()
                    val client = DbxClientV2(config, credential)
                    val email = sharedPreferences.getString("${accountId}_email", null)
                    
                    accountStates[accountId] = AccountState(accountId, credential, client, email)
                    Log.d("DropboxAccountManager", "Restored existing Dropbox account: $accountId")
                } catch (e: Exception) {
                    Log.e("DropboxAccountManager", "Failed to restore existing Dropbox account $accountId", e)
                    // Clean up obsolete/legacy token formats (Zero Backward Compatibility)
                    sharedPreferences.edit()
                        .remove(accountId)
                        .remove("${accountId}_email")
                        .apply()
                }
            }
        }
    }

    /**
     * Get the Dropbox API client for a specific account.
     */
    fun getDbxClientV2(accountId: String): DbxClientV2? {
        return accountStates[accountId]?.client
    }

    /**
     * Get the access token for a specific account.
     */
    override fun getAccessToken(accountId: String): String? {
        return accountStates[accountId]?.credential?.accessToken
    }

    /**
     * Get the email for a specific account.
     */
    fun getAccountEmail(accountId: String): String? {
        return accountStates[accountId]?.email
    }

    /**
     * Check if a specific account is authenticated.
     */
    override fun isAccountAuthenticated(accountId: String): Boolean {
        return super.isAccountAuthenticated(accountId) && accountStates[accountId]?.credential != null
    }

    /**
     * Sign out and remove a specific account.
     * This deletes the stored token locally.
     */
    override fun signOutAccount(accountId: String) {
        super.signOutAccount(accountId)
        sharedPreferences.edit()
            .remove(accountId)
            .remove("${accountId}_email")
            .apply()
        Log.d("DropboxAccountManager", "Removed Dropbox account: $accountId")
    }

    /**
     * Sign out all accounts.
     * Clears all locally stored access tokens.
     */
    override fun signOutAll() {
        super.signOutAll()
        sharedPreferences.edit().clear().apply()
        Log.d("DropboxAccountManager", "Signed out all Dropbox accounts")
    }

    /**
     * Revoke access and remove all accounts.
     * This calls the Dropbox API to invalidate the access token.
     */
    override suspend fun revokeAll() {
        withContext(Dispatchers.IO) {
            val accountsToRevoke = accountStates.values.toList() // Avoid ConcurrentModificationException
            for (accountState in accountsToRevoke) {
                try {
                    accountState.client.auth().tokenRevoke()
                    Log.d("DropboxAccountManager", "Revoked token for account: ${accountState.accountId}")
                } catch (e: Exception) {
                    Log.e("DropboxAccountManager", "Failed to revoke token for ${accountState.accountId}", e)
                }
            }
            signOutAll() // Clear local state after attempting to revoke remotely
            Log.d("DropboxAccountManager", "Revoked all Dropbox accounts and cleared local state.")
        }
    }
}
