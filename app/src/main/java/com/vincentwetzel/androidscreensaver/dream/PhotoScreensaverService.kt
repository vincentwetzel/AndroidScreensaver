package com.vincentwetzel.androidscreensaver.dream

import android.service.dreams.DreamService
import android.view.ViewGroup
import com.vincentwetzel.androidscreensaver.ui.slideshow.NoSourcesView
import com.vincentwetzel.androidscreensaver.ui.slideshow.SlideshowView
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * DreamService that displays a photo slideshow as a screensaver
 */
@AndroidEntryPoint
class PhotoScreensaverService : DreamService() {

    @Inject
    lateinit var slideshowManager: SlideshowManager
    
    private var slideshowView: SlideshowView? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        android.util.Log.d("PhotoScreensaver", "onAttachedToWindow - screensaver started!")

        isInteractive = false
        isScreenBright = false

        // Check if any sources are configured
        val hasSources = SettingsManager.hasAnySourceConfigured(this)
        
        if (!hasSources) {
            // Show "no sources configured" message
            setContentView(NoSourcesView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            })
            android.util.Log.d("PhotoScreensaver", "No sources configured - showing setup message")
        } else {
            // Create and show the slideshow
            startSlideshow()
        }
    }
    
    /**
     * Start the photo slideshow
     */
    private fun startSlideshow() {
        slideshowView = SlideshowView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            onSlideshowStarted = { photos ->
                android.util.Log.d("PhotoScreensaver", "Slideshow started with ${photos.size} photos")
            }
            
            onError = { error ->
                android.util.Log.e("PhotoScreensaver", "Slideshow error: $error")
            }
            
            // Initialize and start
            initialize(slideshowManager)
        }
        
        setContentView(slideshowView)
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        android.util.Log.d("PhotoScreensaver", "onDetachedFromWindow")
        slideshowView?.stop()
    }

    companion object {
        private const val TAG = "PhotoScreensaver"
    }
}
