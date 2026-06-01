package com.vincentwetzel.androidscreensaver.ui.sources

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.AccountConfig
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import com.vincentwetzel.androidscreensaver.databinding.ActivityGoogleDriveAuthBinding
import com.vincentwetzel.androidscreensaver.utils.GoogleAccountManager
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import com.vincentwetzel.androidscreensaver.dream.SourceType as DreamSourceType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Google Drive Authentication Activity
 * Handles Google Sign-In flow for adding new accounts or re-authenticating existing ones.
 */
@AndroidEntryPoint
class GoogleDriveAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoogleDriveAuthBinding

    @Inject
    lateinit var driveRepository: GoogleDriveRepository

    @Inject
    lateinit var accountManager: GoogleAccountManager

    // Activity result launcher for Google Sign-In
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleSignInResult(result.data)
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
                val account = SettingsManager.getAccount(this@GoogleDriveAuthActivity, DreamSourceType.GOOGLE_DRIVE, existingAccountId)
                binding.btnSignIn.text = "Re-authenticate${account?.accountEmail?.let { " as $it" } ?: ""}"
            }
        }

        binding.btnSignIn.filterTouchesWhenObscured = true
        binding.btnSignIn.setOnClickListener {
            startGoogleSignIn()
        }
    }

    private fun startGoogleSignIn() {
        val existingAccountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)

        lifecycleScope.launch {
            // If we are adding a NEW account (not re-authenticating), we must sign out first
            // to ensure the Google account picker dialog is shown. Otherwise, Google Sign-In
            // might silently re-authenticate the last used account.
            if (existingAccountId == null) {
                accountManager.forceAccountPicker()
            }
            val signInIntent = driveRepository.getSignInIntent()
            signInLauncher.launch(signInIntent)
        }
    }

    private fun handleSignInResult(data: Intent?) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignIn.isEnabled = false

        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            onAuthenticated(account)
        } catch (e: ApiException) {
            android.util.Log.e("GoogleDriveAuth", "Google Sign-In failed: statusCode=${e.statusCode}, message=${e.statusMessage ?: "none"}")
            binding.progressBar.visibility = View.GONE
            binding.btnSignIn.isEnabled = true

            val errorMessage = when (e.statusCode) {
                10 -> "Developer error: Check SHA-1 fingerprint and package name in Google Cloud Console"
                12500 -> "Sign-in failed. Please try again."
                12501 -> "Sign-in cancelled"
                12502 -> "No Google account found on this device"
                else -> "Sign-in failed (code ${e.statusCode}): ${e.statusMessage}"
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun onAuthenticated(account: GoogleSignInAccount) {
        lifecycleScope.launch {
            val accountEmail = account.email ?: "Unknown"

            // Register with the account manager
            val returnedAccountId = driveRepository.handleSignInResult(account)
            if (returnedAccountId == null) {
                binding.progressBar.visibility = View.GONE
                binding.btnSignIn.isEnabled = true
                Toast.makeText(this@GoogleDriveAuthActivity, "Failed to authenticate with Google Drive", Toast.LENGTH_LONG).show()
                return@launch
            }

            val existingAccountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)
            val accountId = returnedAccountId

            // Save/update the account in SettingsManager, migrating existing selections if the ID changed
            var existingAccount = SettingsManager.getAccount(this@GoogleDriveAuthActivity, DreamSourceType.GOOGLE_DRIVE, accountId)
            if (existingAccount == null && existingAccountId != null) {
                existingAccount = SettingsManager.getAccount(this@GoogleDriveAuthActivity, DreamSourceType.GOOGLE_DRIVE, existingAccountId)
            }

            val updatedAccount = AccountConfig(
                accountId = accountId,
                sourceType = SourceType.GOOGLE_DRIVE,
                accountEmail = accountEmail,
                accountDisplayName = account.displayName,
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
                SettingsManager.removeAccount(this@GoogleDriveAuthActivity, DreamSourceType.GOOGLE_DRIVE, existingAccountId)
            }

            val message = "Successfully signed in as $accountEmail"
            Toast.makeText(this@GoogleDriveAuthActivity, message, Toast.LENGTH_LONG).show()

            val resultIntent = Intent().apply {
                putExtra(EXTRA_ACCOUNT_NAME, accountEmail)
                putExtra(EXTRA_ACCOUNT_ID, accountId)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}