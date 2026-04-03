package com.vincentwetzel.androidscreensaver.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.databinding.ActivityAboutBinding
import com.vincentwetzel.androidscreensaver.utils.SecureLinks
import com.vincentwetzel.androidscreensaver.utils.VersionUtils

/**
 * About Activity
 * Displays app information, links, and version
 */
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.about)

        setupUI()
    }

    private fun setupUI() {
        // Set version
        binding.tvVersion.text = VersionUtils.getFormattedVersion(this)

        // GitHub link
        binding.cardGithub.setOnClickListener {
            openUrl(SecureLinks.githubRepo)
        }

        // Discord link (obfuscated)
        binding.cardDiscord.setOnClickListener {
            openUrl(SecureLinks.discordInvite)
        }

        // License link
        binding.cardLicense.setOnClickListener {
            openUrl("https://opensource.org/licenses/MIT")
        }

        // Privacy Policy link
        binding.cardPrivacy.setOnClickListener {
            // TODO: Add your privacy policy URL
            openUrl("https://github.com/vincentwetzel/AndroidScreensaver/blob/main/PRIVACY.md")
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback if no browser is available
            e.printStackTrace()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
