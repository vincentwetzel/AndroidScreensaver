package com.vincentwetzel.androidscreensaver.ui.sources

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.databinding.ActivityGoogleDriveAuthBinding
import com.vincentwetzel.androidscreensaver.viewmodel.AuthState
import com.vincentwetzel.androidscreensaver.viewmodel.GoogleDriveViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Google Drive Authentication Activity
 * Handles Google Sign-In flow
 */
@AndroidEntryPoint
class GoogleDriveAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoogleDriveAuthBinding
    private val viewModel: GoogleDriveViewModel by viewModels()

    // Activity result launcher for Google Sign-In
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleSignInResult(result.data)
    }

    companion object {
        private const val TAG = "GoogleDriveAuth"
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
        viewModel.authState.observe(this) { state ->
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
                    // Auth successful, go back to previous screen
                    Toast.makeText(this, "Successfully signed in!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
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

        viewModel.accountName.observe(this) { name ->
            if (name != null) {
                binding.btnSignIn.text = "Signed in as $name"
            }
        }
    }

    private fun startGoogleSignIn() {
        val signInIntent = viewModel.getSignInIntent()
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            viewModel.onAuthenticated(account)
        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                12500 -> "Sign-in failed. Please try again."
                10 -> "No Google account found on this device."
                else -> "Sign-in failed: ${e.statusMessage}"
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }
}
