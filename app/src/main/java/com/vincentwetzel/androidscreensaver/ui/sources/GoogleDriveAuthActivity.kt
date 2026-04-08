package com.vincentwetzel.androidscreensaver.ui.sources

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.repository.GoogleDriveRepository
import com.vincentwetzel.androidscreensaver.databinding.ActivityGoogleDriveAuthBinding
import com.vincentwetzel.androidscreensaver.viewmodel.AuthState
import com.vincentwetzel.androidscreensaver.viewmodel.GoogleDriveViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Google Drive Authentication Activity
 * Handles Google Sign-In flow
 */
@AndroidEntryPoint
class GoogleDriveAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoogleDriveAuthBinding
    private val viewModel: GoogleDriveViewModel by viewModels()

    @Inject
    lateinit var driveRepository: GoogleDriveRepository

    // Activity result launcher for Google Sign-In
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleSignInResult(result.data)
    }

    companion object {
        private const val TAG = "GoogleDriveAuth"
        const val EXTRA_ACCOUNT_NAME = "account_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoogleDriveAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnSignIn.setOnClickListener {
            startGoogleSignIn()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Unauthenticated -> {
                        binding.btnSignIn.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                    }
                    is AuthState.Authenticating -> {
                        binding.btnSignIn.isEnabled = false
                        binding.progressBar.visibility = View.VISIBLE
                        binding.errorMessage.visibility = View.GONE
                    }
                    is AuthState.Authenticated -> {
                        // Auth successful, show account email and return
                        val accountEmail = driveRepository.currentAccount.value?.email
                        val message = if (accountEmail != null) {
                            "Successfully signed in as $accountEmail"
                        } else {
                            "Successfully signed in!"
                        }
                        Toast.makeText(this@GoogleDriveAuthActivity, message, Toast.LENGTH_LONG).show()

                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_ACCOUNT_NAME, accountEmail)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                    is AuthState.Error -> {
                        binding.btnSignIn.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        binding.errorMessage.text = state.message
                        binding.errorMessage.visibility = View.VISIBLE
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.accountName.collect { name ->
                if (name != null) {
                    binding.btnSignIn.text = "Signed in as $name"
                }
            }
        }
    }

    private fun startGoogleSignIn() {
        val signInIntent = driveRepository.getSignInIntent()
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            viewModel.onAuthenticated(account)
        } catch (e: ApiException) {
            android.util.Log.e(TAG, "Google Sign-In failed: statusCode=${e.statusCode}", e)
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
}