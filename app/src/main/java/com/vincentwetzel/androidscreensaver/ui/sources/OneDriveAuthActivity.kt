package com.vincentwetzel.androidscreensaver.ui.sources

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.vincentwetzel.androidscreensaver.auth.OneDriveAuthManager
import com.vincentwetzel.androidscreensaver.data.model.AccountConfig
import com.vincentwetzel.androidscreensaver.data.model.SourceType
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OneDriveAuthActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: OneDriveAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Microsoft MSAL SDK handles the UI via a BrowserTabActivity overlay, 
        // so this activity remains transparent and acts as a launcher.
        
        lifecycleScope.launch {
            try {
                val authenticatedAccount = authManager.authenticate(this@OneDriveAuthActivity) { userCode, verificationUri ->
                    // TV Device Flow prompt
                    Toast.makeText(this@OneDriveAuthActivity, "TV Auth: Go to $verificationUri and enter code $userCode", Toast.LENGTH_LONG).show()
                }
                
                val newAccount = SettingsManager.getAccount(
                    this@OneDriveAuthActivity,
                    com.vincentwetzel.androidscreensaver.dream.SourceType.ONEDRIVE,
                    authenticatedAccount.accountId
                )?.copy(
                    accountEmail = authenticatedAccount.email,
                    isAuthenticated = true,
                    lastAuthTime = System.currentTimeMillis()
                ) ?: AccountConfig(
                    accountId = authenticatedAccount.accountId,
                    sourceType = SourceType.ONEDRIVE,
                    accountEmail = authenticatedAccount.email,
                    enabled = true,
                    isAuthenticated = true,
                    lastAuthTime = System.currentTimeMillis()
                )
                
                SettingsManager.saveAccount(this@OneDriveAuthActivity, newAccount)
                
                Toast.makeText(
                    this@OneDriveAuthActivity,
                    "Successfully signed in as ${authenticatedAccount.email}",
                    Toast.LENGTH_SHORT
                ).show()
                
                val resultIntent = Intent().apply {
                    putExtra(FolderBrowserActivity.EXTRA_ACCOUNT_ID, authenticatedAccount.accountId)
                }
                setResult(RESULT_OK, resultIntent)
            } catch (e: Exception) {
                android.util.Log.e("OneDriveAuthActivity", "Authentication failed", e)
                Toast.makeText(this@OneDriveAuthActivity, "Authentication failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                setResult(RESULT_CANCELED)
            } finally {
                finish()
            }
        }
    }
}
