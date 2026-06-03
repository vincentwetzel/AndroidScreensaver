package com.vincentwetzel.androidscreensaver.ui.sources

import android.accounts.Account
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.content.res.Configuration
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.GoogleAuthUtil
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.drive.DriveScopes
import com.vincentwetzel.androidscreensaver.data.model.AccountConfig
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import com.vincentwetzel.androidscreensaver.databinding.ActivityGoogleDriveAuthBinding
import com.vincentwetzel.androidscreensaver.utils.GoogleAccountManager
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Google Drive Authentication Activity
 * Handles Google Sign-In via hybrid flow (Play Services for Mobile, Device Flow for Android TV).
 */
@AndroidEntryPoint
class GoogleDriveAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoogleDriveAuthBinding

    @Inject
    lateinit var driveRepository: GoogleDriveRepository

    @Inject
    lateinit var accountManager: GoogleAccountManager

    private var pendingAccountId: String? = null
    private var pendingAccountEmail: String? = null
    private var pendingAccountDisplayName: String? = null
    private var pendingGoogleAccount: Account? = null

    // Activity result launcher for Google Sign-In
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleSignInResult(result.data)
    }

    // Activity result launcher for Drive permission consent screen
    private val drivePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            finalizeSignIn()
        } else {
            binding.progressBar.visibility = View.GONE
            binding.btnSignIn.isEnabled = true
            Toast.makeText(this, "Google Drive permission is required.", Toast.LENGTH_LONG).show()
            pendingAccountId?.let {
                accountManager.signOutAccount(it)
            }
        }
    }

    companion object {
        const val EXTRA_ACCOUNT_NAME = "account_name"
        const val EXTRA_ACCOUNT_ID = "account_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityGoogleDriveAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // Check if we're re-authenticating an existing account
        val existingAccountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)
        if (existingAccountId != null) {
            lifecycleScope.launch {
                val account = SettingsManager.getAccount(this@GoogleDriveAuthActivity, SourceType.GOOGLE_DRIVE, existingAccountId)
                binding.btnSignIn.text = "Re-authenticate${account?.accountEmail?.let { " as $it" } ?: ""}"
            }
        }

        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            binding.btnSignIn.isEnabled = false
            binding.btnSignIn.text = "Not Supported on TV"
            Toast.makeText(this, "Google Drive authentication requires a web browser and is not supported on Android TV.", Toast.LENGTH_LONG).show()
        } else {
            binding.btnSignIn.filterTouchesWhenObscured = true
            binding.btnSignIn.setOnClickListener {
                startGoogleSignIn()
            }
        }
    }

    private fun startGoogleSignIn() {
        binding.btnSignIn.isEnabled = false

        lifecycleScope.launch {
            try {
                val signInIntent = driveRepository.getSignInIntent()
                android.util.Log.d("GoogleDriveAuth", "Launching Google account picker intent")
                signInLauncher.launch(signInIntent)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("GoogleDriveAuth", "Failed to start Google sign-in: ${e.javaClass.simpleName}")
                binding.btnSignIn.isEnabled = true
                Toast.makeText(this@GoogleDriveAuthActivity, "Error starting sign in. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleSignInResult(data: Intent?) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignIn.isEnabled = false

        lifecycleScope.launch {
            try {
                val returnedAccountId = driveRepository.handleSignInResult(data)
                if (returnedAccountId == null) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSignIn.isEnabled = true

                    android.util.Log.e("GoogleDriveAuth", "Sign-in result returned null. Intent data present: ${data != null}")

                    Toast.makeText(this@GoogleDriveAuthActivity, "Sign-in cancelled or failed.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                pendingAccountId = returnedAccountId
                val email = driveRepository.getAccountEmail(returnedAccountId) ?: "Unknown"
                pendingAccountEmail = email
                pendingAccountDisplayName = null
                pendingGoogleAccount = Account(email, "com.google")

                // Verify Drive access. This will throw UserRecoverableAuthIOException if
                // the user has not yet granted Drive permissions for this app.
                try {
                    android.util.Log.d("GoogleDriveAuth", "Starting authentication process")
                    android.util.Log.d("GoogleDriveAuth", "Requested scopes: ${DriveScopes.DRIVE_READONLY}")

                    // Identity API fails on Android TV. Using official Google API Client which supports TV dialogs.
                    driveRepository.verifyDriveAccess(returnedAccountId)
                    
                    android.util.Log.d("GoogleDriveAuth", "Drive verification successful")
                    finalizeSignIn()
                } catch (e: UserRecoverableAuthIOException) {
                android.util.Log.w("GoogleDriveAuth", "User interaction required to approve scopes. Launching Android TV native consent...")
                    drivePermissionLauncher.launch(e.intent)
                } catch (e: GoogleAuthIOException) {
                    val cause = e.cause
                    android.util.Log.e("GoogleDriveAuth", "Fatal Google Auth Error: ${e.javaClass.simpleName}")

                    if (cause is GoogleAuthException && cause.message?.contains("InvalidScope") == true) {
                        logInvalidScopeGuidance()
                        Toast.makeText(
                            this@GoogleDriveAuthActivity,
                            "Google Drive OAuth setup is incomplete. Check Drive API, consent scope, package, and SHA-1.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(this@GoogleDriveAuthActivity, "Authentication failed. Please try again.", Toast.LENGTH_LONG).show()
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.btnSignIn.isEnabled = true
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("GoogleDriveAuth", "Unexpected Exception during Auth: ${e.javaClass.simpleName}")
                Toast.makeText(this@GoogleDriveAuthActivity, "Authentication error occurred.", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.btnSignIn.isEnabled = true
            }
        }
    }

    private fun logInvalidScopeGuidance() {
        val packageName = packageName
        android.util.Log.e("GoogleDriveAuth", "Google Drive OAuth InvalidScope checklist:")
        android.util.Log.e("GoogleDriveAuth", "1. Enable the Google Drive API for the Cloud project.")
        android.util.Log.e("GoogleDriveAuth", "2. Add https://www.googleapis.com/auth/drive.readonly to OAuth consent/Data Access scopes.")
        android.util.Log.e("GoogleDriveAuth", "3. Add this signed-in account as an OAuth test user if the app is unpublished.")
        android.util.Log.e("GoogleDriveAuth", "4. Verify the Android OAuth client package name is $packageName.")
        android.util.Log.e("GoogleDriveAuth", "5. Verify the Android OAuth client SHA-1 matches ./gradlew signingReport for this build.")
    }

    private fun finalizeSignIn() {
        lifecycleScope.launch {
            try {
                val accountId = pendingAccountId ?: return@launch
                val accountEmail = pendingAccountEmail ?: "Unknown"
                val existingAccountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)

                // Save/update the account in SettingsManager, migrating existing selections if the ID changed
                var existingAccount = SettingsManager.getAccount(this@GoogleDriveAuthActivity, SourceType.GOOGLE_DRIVE, accountId)
                if (existingAccount == null && existingAccountId != null) {
                    existingAccount = SettingsManager.getAccount(this@GoogleDriveAuthActivity, SourceType.GOOGLE_DRIVE, existingAccountId)
                }

                val updatedAccount = AccountConfig(
                    accountId = accountId,
                    sourceType = SourceType.GOOGLE_DRIVE,
                    accountEmail = accountEmail,
                    accountDisplayName = pendingAccountDisplayName,
                    enabled = existingAccount?.enabled ?: true,
                    selectedFolders = existingAccount?.selectedFolders ?: emptyList(),
                    deselectedFolders = existingAccount?.deselectedFolders ?: emptySet(),
                    isAuthenticated = true,
                    lastAuthTime = System.currentTimeMillis(),
                    lastSyncTime = existingAccount?.lastSyncTime,
                    photoCount = existingAccount?.photoCount ?: 0
                )
                SettingsManager.saveAccount(this@GoogleDriveAuthActivity, updatedAccount)

                // Cleanup old account reference if the ID changed during re-auth
                if (existingAccountId != null && existingAccountId != accountId && existingAccount != null) {
                    SettingsManager.removeAccount(this@GoogleDriveAuthActivity, SourceType.GOOGLE_DRIVE, existingAccountId)
                }

                val message = "Successfully signed in as $accountEmail"
                Toast.makeText(this@GoogleDriveAuthActivity, message, Toast.LENGTH_LONG).show()

                val resultIntent = Intent().apply {
                    putExtra(EXTRA_ACCOUNT_NAME, accountEmail)
                    putExtra(EXTRA_ACCOUNT_ID, accountId)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("GoogleDriveAuth", "Error finalizing sign in: ${e.javaClass.simpleName}")
                Toast.makeText(this@GoogleDriveAuthActivity, "Error saving account configuration.", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.btnSignIn.isEnabled = true
            }
        }
    }
}
