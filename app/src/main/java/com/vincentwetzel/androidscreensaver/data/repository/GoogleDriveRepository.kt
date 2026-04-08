package com.vincentwetzel.androidscreensaver.data.repository

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive Repository
 * Handles authentication and provides Drive API client
 */
@Singleton
class GoogleDriveRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Authentication state
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val currentAccount: StateFlow<GoogleSignInAccount?> = _currentAccount.asStateFlow()

    // Drive API client
    private var driveService: Drive? = null
    private var driveCredential: GoogleAccountCredential? = null

    // Google Sign-In client
    private val googleSignInClient: GoogleSignInClient by lazy {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
            .requestScopes(Scope(DriveScopes.DRIVE_METADATA_READONLY))
            .apply {
                // Only request ID token if WEB_CLIENT_ID is configured
                if (GoogleOAuthConfig.WEB_CLIENT_ID.isNotEmpty()) {
                    requestIdToken(GoogleOAuthConfig.WEB_CLIENT_ID)
                }
            }
            .build()

        GoogleSignIn.getClient(context, signInOptions)
    }

    /**
     * Get the sign-in intent to launch Google authentication
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Handle the sign-in result
     */
    suspend fun handleSignInResult(account: GoogleSignInAccount?): Boolean {
        return try {
            if (account != null) {
                _currentAccount.value = account
                initializeDriveService(account)
                _isAuthenticated.value = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Initialize the Drive API service with the authenticated account
     */
    private fun initializeDriveService(account: GoogleSignInAccount) {
        driveCredential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_READONLY)
        )
        driveCredential!!.selectedAccount = account.account

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            driveCredential
        )
            .setApplicationName("Android Screensaver")
            .build()
    }

    /**
     * Get the OAuth access token for the current account (for use with OkHttp)
     */
    fun getAccessToken(): String? {
        return try {
            driveCredential?.getToken()
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveRepo", "Failed to get access token", e)
            null
        }
    }

    /**
     * Get the Drive API service (must be authenticated first)
     */
    fun getDriveService(): Drive? {
        return driveService
    }

    /**
     * Sign out the current user
     */
    suspend fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            _isAuthenticated.value = false
            _currentAccount.value = null
            driveService = null
        }
    }

    /**
     * Revoke access and sign out
     */
    suspend fun revokeAccess() {
        googleSignInClient.revokeAccess().addOnCompleteListener {
            _isAuthenticated.value = false
            _currentAccount.value = null
            driveService = null
        }
    }

    /**
     * Check if user is already signed in (silent sign-in)
     */
    fun checkExistingSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleOAuthConfig.CLIENT_ID.isNotEmpty()) {
            _currentAccount.value = account
            _isAuthenticated.value = true
            initializeDriveService(account)
        }
    }
}
