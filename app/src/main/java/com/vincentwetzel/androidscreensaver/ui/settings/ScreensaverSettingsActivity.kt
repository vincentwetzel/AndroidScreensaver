package com.vincentwetzel.androidscreensaver.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vincentwetzel.androidscreensaver.R

/**
 * DreamService settings activity
 * Shown when user taps the gear icon next to this app in system screensaver settings
 */
class ScreensaverSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Redirect to main activity - users configure sources there
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Open Android Screensaver app to configure sources", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
