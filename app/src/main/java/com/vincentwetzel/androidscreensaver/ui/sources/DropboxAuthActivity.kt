package com.vincentwetzel.androidscreensaver.ui.sources

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dropbox.core.android.Auth
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.http.OkHttp3Requestor
import okhttp3.OkHttpClient
import com.vincentwetzel.androidscreensaver.BuildConfig
import com.vincentwetzel.androidscreensaver.data.model.AccountConfig
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.databinding.ActivityDropboxAuthBinding
import com.vincentwetzel.androidscreensaver.utils.DropboxAccountManager
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dropbox Authentication Activity
 * Handles Dropbox OAuth web flow for adding new accounts or re-authenticating existing ones.
 */
@AndroidEntryPoint
class DropboxAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDropboxAuthBinding

    @Inject
    lateinit var accountManager: DropboxAccountManager

    private var authStarted = false

    companion object {
        const val EXTRA_ACCOUNT_NAME = "account_name"
        const val EXTRA_ACCOUNT_ID = "account_id"

        @Volatile
        private var sharedOkHttpClient: OkHttpClient? = null

        private fun getOkHttpClient(): OkHttpClient {
            return sharedOkHttpClient ?: synchronized(this) {
                sharedOkHttpClient ?: OkHttpClient().also { sharedOkHttpClient = it }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityDropboxAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            authStarted = savedInstanceState.getBoolean("authStarted", false)
        }

        setupUI()
    }

    private fun setupUI() {
        // Check if we're re-authenticating an existing account
        val existingAccountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)
        if (existingAccountId != null) {
            lifecycleScope.launch {
                val account = SettingsManager.getAccount(this@DropboxAuthActivity, SourceType.DROPBOX, existingAccountId)
                binding.btnSignIn.text = "Re-authenticate${account?.accountEmail?.let { " as $it" } ?: ""}"
            }
        }

        if (isTvDevice()) {
            binding.btnSignIn.isEnabled = false
            binding.btnSignIn.text = "Not Supported on TV"
            Toast.makeText(this, "Dropbox authentication requires a web browser and is not supported on Android TV.", Toast.LENGTH_LONG).show()
        } else {
            binding.btnSignIn.filterTouchesWhenObscured = true
            binding.btnSignIn.setOnClickListener {
                startDropboxAuth()
            }
        }
    }

    private fun isTvDevice(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("authStarted", authStarted)
    }

    private fun startDropboxAuth() {
        if (authStarted) return
        binding.btnSignIn.isEnabled = false

        if (BuildConfig.DROPBOX_APP_KEY == "default_key" || BuildConfig.DROPBOX_APP_KEY.isBlank()) {
            Toast.makeText(this, "Error: DROPBOX_APP_KEY not set in local.properties", Toast.LENGTH_LONG).show()
            authStarted = false
            binding.btnSignIn.isEnabled = true
            return
        }

        val client = getOkHttpClient()

        try {
            // Launches the Dropbox OAuth flow in the system browser using PKCE (Proof Key for Code Exchange)
            val requestConfig = DbxRequestConfig.newBuilder("AndroidScreensaver")
                .withHttpRequestor(OkHttp3Requestor(client))
                .build()
            // Scopes MUST be explicitly provided to force the modern OAuth endpoint.
            // Without scopes, the Dropbox SDK falls back to a legacy endpoint that triggers 
            // the "malicious app" security alert even when using PKCE.
            val scopes = listOf("files.content.read", "files.metadata.read", "account_info.read")
            Auth.startOAuth2PKCE(this, BuildConfig.DROPBOX_APP_KEY, requestConfig, scopes)
            authStarted = true
        } catch (e: Exception) {
            authStarted = false
            binding.btnSignIn.isEnabled = true
            Toast.makeText(this, "Failed to start authentication.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        
        // If we started the auth flow and returned to this screen, handle the result
        if (authStarted) {
            authStarted = false
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSignIn.isEnabled = false

            lifecycleScope.launch {
                try {
                    val returnedAccountId = accountManager.handleSignInResult()
                    
                    if (returnedAccountId == null) {
                        Toast.makeText(this@DropboxAuthActivity, "Failed to authenticate with Dropbox", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val accountEmail = accountManager.getAccountEmail(returnedAccountId) ?: "Unknown"
                    
                    val existingAccountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)
                    val accountId = returnedAccountId

                    // Save/update the account in SettingsManager, migrating existing selections if the ID changed
                    var existingAccount = SettingsManager.getAccount(this@DropboxAuthActivity, SourceType.DROPBOX, accountId)
                    if (existingAccount == null && existingAccountId != null) {
                        existingAccount = SettingsManager.getAccount(this@DropboxAuthActivity, SourceType.DROPBOX, existingAccountId)
                    }

                    val updatedAccount = AccountConfig(
                        accountId = accountId,
                        sourceType = SourceType.DROPBOX,
                        accountEmail = accountEmail,
                        enabled = existingAccount?.enabled ?: true,
                        selectedFolders = existingAccount?.selectedFolders ?: emptyList(),
                        deselectedFolders = existingAccount?.deselectedFolders ?: emptySet(),
                        isAuthenticated = true,
                        lastAuthTime = System.currentTimeMillis(),
                        lastSyncTime = existingAccount?.lastSyncTime,
                        photoCount = existingAccount?.photoCount ?: 0
                    )
                    SettingsManager.saveAccount(this@DropboxAuthActivity, updatedAccount)

                    // Cleanup old account reference if the ID changed during re-auth
                    if (existingAccountId != null && existingAccountId != accountId && existingAccount != null) {
                        SettingsManager.removeAccount(this@DropboxAuthActivity, SourceType.DROPBOX, existingAccountId)
                    }

                    Toast.makeText(this@DropboxAuthActivity, "Successfully signed in as $accountEmail", Toast.LENGTH_LONG).show()

                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_ACCOUNT_NAME, accountEmail)
                        putExtra(EXTRA_ACCOUNT_ID, accountId)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    android.util.Log.e("DropboxAuth", "Error during Dropbox authentication: ${e.javaClass.simpleName}")
                    Toast.makeText(this@DropboxAuthActivity, "Authentication error occurred.", Toast.LENGTH_LONG).show()
                } finally {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSignIn.isEnabled = true
                }
            }
        }
    }
}