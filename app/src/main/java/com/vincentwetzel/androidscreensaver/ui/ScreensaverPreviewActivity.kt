package com.vincentwetzel.androidscreensaver.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vincentwetzel.androidscreensaver.dream.SlideshowManager
import com.vincentwetzel.androidscreensaver.data.model.TimeoutMinutes
import com.vincentwetzel.androidscreensaver.data.model.TimeoutUnit
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
    private var timeoutHandler: Handler? = null
    private var timeoutRunnable: Runnable? = null

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

        // Setup timeout if enabled
        setupTimeout()
    }

    /**
     * Setup screensaver timeout if timer is enabled
     */
    private fun setupTimeout() {
        val timerConfig = slideshowManager.config.timerConfig
        
        android.util.Log.d("PreviewActivity", "setupTimeout called - timeout=${timerConfig.timeoutMinutes}")
        
        // Only setup timeout if not disabled
        if (timerConfig.timeoutMinutes == TimeoutMinutes.DISABLED) {
            android.util.Log.d("PreviewActivity", "Screensaver timeout disabled")
            return
        }

        // Calculate timeout duration in milliseconds
        val timeoutMillis = when (timerConfig.timeoutMinutes) {
            TimeoutMinutes.DISABLED -> 0L // Should never reach here due to early return
            TimeoutMinutes.SECONDS_30 -> 30 * 1000L
            TimeoutMinutes.MINUTES_5 -> 5 * 60 * 1000L
            TimeoutMinutes.MINUTES_15 -> 15 * 60 * 1000L
            TimeoutMinutes.MINUTES_30 -> 30 * 60 * 1000L
            TimeoutMinutes.MINUTES_45 -> 45 * 60 * 1000L
            TimeoutMinutes.MINUTES_60 -> 60 * 60 * 1000L
            TimeoutMinutes.MINUTES_90 -> 90 * 60 * 1000L
            TimeoutMinutes.MINUTES_120 -> 120 * 60 * 1000L
            TimeoutMinutes.CUSTOM -> {
                // Use custom value
                val multiplier = if (timerConfig.customTimeoutUnit == TimeoutUnit.HOURS) 60 else 1
                timerConfig.customTimeoutValue * multiplier * 60 * 1000L
            }
        }

        android.util.Log.d("PreviewActivity", "Screensaver timeout: $timeoutMillis ms (${timeoutMillis / 1000} seconds)")

        // Create handler and runnable for timeout
        timeoutHandler = Handler(Looper.getMainLooper())
        timeoutRunnable = Runnable {
            android.util.Log.d("PreviewActivity", "Screensaver timeout reached - finishing!")
            finish()
        }

        // Post the delayed runnable
        timeoutHandler?.postDelayed(timeoutRunnable!!, timeoutMillis)
        android.util.Log.d("PreviewActivity", "Timeout scheduled successfully")
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
        
        // Clean up timeout handler
        timeoutHandler?.let { handler ->
            timeoutRunnable?.let { runnable ->
                handler.removeCallbacks(runnable)
            }
        }
        timeoutHandler = null
        timeoutRunnable = null
        
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
