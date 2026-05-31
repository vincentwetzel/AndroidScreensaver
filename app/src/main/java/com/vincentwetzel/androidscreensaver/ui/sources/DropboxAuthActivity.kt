package com.vincentwetzel.androidscreensaver.ui.sources

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dropbox.core.android.Auth
import com.vincentwetzel.androidscreensaver.data.model.AccountConfig
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.databinding.ActivityDropboxAuthBinding
import com.vincentwetzel.androidscreensaver.utils.DropboxAccountManager
import com.vincentwetzel.androidscreensaver.utils.DropboxOAuthConfig
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import com.vincentwetzel.androidscreensaver.dream.SourceType as DreamSourceType
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityDropboxAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // Check if we're re-authenticating an existing account
        val existingAccountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)
        if (existingAccountId != null) {
            lifecycleScope.launch {
                val account = SettingsManager.getAccount(this@DropboxAuthActivity, DreamSourceType.DROPBOX, existingAccountId)
                binding.btnSignIn.text = "Re-authenticate${account?.accountEmail?.let { " as $it" } ?: ""}"
            }
        }

        binding.btnSignIn.setOnClickListener {
            startDropboxAuth()
        }
    }

    private fun startDropboxAuth() {
        // Launches the Dropbox OAuth flow in the system browser
        Auth.startOAuth2Authentication(this, DropboxOAuthConfig.APP_KEY)
        authStarted = true
    }

    override fun onResume() {
        super.onResume()
        
        // If we started the auth flow and returned to this screen, handle the result
        if (authStarted) {
            authStarted = false
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSignIn.isEnabled = false

            lifecycleScope.launch {
                val accountId = accountManager.handleSignInResult()
                
                if (accountId == null) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSignIn.isEnabled = true
                    Toast.makeText(this@DropboxAuthActivity, "Failed to authenticate with Dropbox", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val accountEmail = accountManager.getAccountEmail(accountId) ?: "Unknown"
                Toast.makeText(this@DropboxAuthActivity, "Successfully signed in as $accountEmail", Toast.LENGTH_LONG).show()

                val resultIntent = Intent().apply {
                    putExtra(EXTRA_ACCOUNT_NAME, accountEmail)
                    putExtra(EXTRA_ACCOUNT_ID, accountId)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }
}