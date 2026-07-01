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
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class OneDriveAuthActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: OneDriveAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Microsoft MSAL SDK handles the UI via a BrowserTabActivity overlay, 
        // so this activity remains transparent and acts as a launcher.
        
        val existingAccountId = intent.getStringExtra(FolderBrowserActivity.EXTRA_ACCOUNT_ID)

        lifecycleScope.launch {
            try {
                authManager.authenticate(this@OneDriveAuthActivity) { userCode, verificationUri ->
                    // TV Device Flow prompt
                    Toast.makeText(this@OneDriveAuthActivity, "TV Auth: Go to $verificationUri and enter code $userCode", Toast.LENGTH_LONG).show()
                }
                
                // Authentication successful
                val newAccountId = existingAccountId ?: UUID.randomUUID().toString()
                
                val newAccount = AccountConfig(
                    accountId = newAccountId,
                    sourceType = SourceType.ONEDRIVE,
                    accountEmail = "OneDrive Account", // Placeholder; Graph API user fetch could be added here later
                    enabled = true,
                    selectedFolders = emptyList(),
                    deselectedFolders = emptySet(),
                    isAuthenticated = true,
                    lastAuthTime = System.currentTimeMillis(),
                    lastSyncTime = null,
                    photoCount = 0
                )
                
                SettingsManager.saveAccount(this@OneDriveAuthActivity, newAccount)
                
                Toast.makeText(this@OneDriveAuthActivity, "Successfully signed in to OneDrive", Toast.LENGTH_SHORT).show()
                
                val resultIntent = Intent().apply {
                    putExtra(FolderBrowserActivity.EXTRA_ACCOUNT_ID, newAccountId)
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