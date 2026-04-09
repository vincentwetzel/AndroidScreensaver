package com.vincentwetzel.androidscreensaver.dream

import android.service.dreams.DreamService
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.WindowManager
import android.view.ViewGroup
import com.vincentwetzel.androidscreensaver.data.model.ScreensaverExitTrigger
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
    private var gestureDetector: GestureDetector? = null

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
        // Reload config to ensure we have the latest settings
        slideshowManager.loadConfig()

        // Apply keep screen on setting
        if (slideshowManager.config.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Apply screen rotation setting via WindowManager
        val orientation = when (slideshowManager.config.screenOrientation) {
            com.vincentwetzel.androidscreensaver.data.model.ScreenOrientation.PORTRAIT ->
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            com.vincentwetzel.androidscreensaver.data.model.ScreenOrientation.LANDSCAPE ->
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        window?.attributes = window?.attributes?.apply {
            screenOrientation = orientation
        }

        slideshowView = SlideshowView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            onSlideshowStarted = { photos ->
                val label = when (slideshowManager.config.mediaTypeFilter) {
                    com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY -> "videos"
                    com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY -> "photos"
                    else -> "items"
                }
                android.util.Log.d("PhotoScreensaver", "Slideshow started with ${photos.size} $label")
            }

            onError = { error ->
                android.util.Log.e("PhotoScreensaver", "Slideshow error: $error")
            }

            // Initialize and start
            initialize(slideshowManager)
        }

        setContentView(slideshowView)

        // Setup exit trigger handling
        setupExitTrigger()
    }

    /**
     * Setup exit trigger based on configuration
     */
    private fun setupExitTrigger() {
        val exitTrigger = slideshowManager.config.exitOnTrigger

        if (exitTrigger == ScreensaverExitTrigger.TOUCH) {
            gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    android.util.Log.d(TAG, "Touch exit trigger — finishing dream")
                    finish()
                    return true
                }

                override fun onDown(e: MotionEvent): Boolean = true
            })
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && gestureDetector?.onTouchEvent(ev) == true) return true
        return super.dispatchTouchEvent(ev)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        android.util.Log.d(TAG, "onDetachedFromWindow")
        slideshowView?.stop()
        slideshowView = null
        gestureDetector = null
    }

    companion object {
        private const val TAG = "PhotoScreensaver"
    }
}
