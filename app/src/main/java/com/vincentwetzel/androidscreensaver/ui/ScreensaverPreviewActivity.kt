package com.vincentwetzel.androidscreensaver.ui

import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vincentwetzel.androidscreensaver.dream.SlideshowManager
import com.vincentwetzel.androidscreensaver.ui.slideshow.SlideshowView
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity that simulates the screensaver for testing purposes
 * Since apps cannot directly trigger DreamService (system-controlled),
 * this Activity provides the same visual experience for testing
 */
@AndroidEntryPoint
class ScreensaverPreviewActivity : AppCompatActivity() {

    @Inject
    lateinit var slideshowManager: SlideshowManager
    
    private var slideshowView: SlideshowView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enter immersive fullscreen mode (hide status bar, nav bar)
        enableImmersiveMode()
        
        // Check if any sources are configured
        val hasSources = SettingsManager.hasAnySourceConfigured(this)
        
        if (!hasSources) {
            // Show "no sources configured" message (same as DreamService)
            val noSourcesLayout = com.vincentwetzel.androidscreensaver.ui.slideshow.NoSourcesView(this)
            setContentView(noSourcesLayout)
            android.util.Log.d("PreviewActivity", "No sources configured - showing setup message")
        } else {
            // Create and show the slideshow
            slideshowView = SlideshowView(this).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                onSlideshowStarted = { photos ->
                    val label = when (slideshowManager.config.mediaTypeFilter) {
                        com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY -> "videos"
                        com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY -> "photos"
                        else -> "items"
                    }
                    Toast.makeText(this@ScreensaverPreviewActivity, "Slideshow started: ${photos.size} $label", Toast.LENGTH_SHORT).show()
                    android.util.Log.d("PreviewActivity", "Slideshow started with ${photos.size} $label")
                }

                onError = { error ->
                    Toast.makeText(this@ScreensaverPreviewActivity, "Error: $error", Toast.LENGTH_LONG).show()
                    android.util.Log.e("PreviewActivity", "Slideshow error: $error")
                }

                // Initialize and start
                slideshowManager.loadConfig()
                initialize(slideshowManager)
            }
            
            setContentView(slideshowView)
        }
    }
    
    override fun onResume() {
        super.onResume()
        slideshowView?.resume()
    }
    
    override fun onPause() {
        super.onPause()
        slideshowView?.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        slideshowView?.stop()
    }
    
    /**
     * Enable immersive fullscreen mode (hide system UI)
     */
    private fun enableImmersiveMode() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
        
        // Android 11+ API
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Re-enable immersive mode if focus is regained
        if (hasFocus) {
            enableImmersiveMode()
        }
    }
}
