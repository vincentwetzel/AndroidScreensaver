package com.vincentwetzel.androidscreensaver.dream

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.dreams.DreamService
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.WindowManager
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.vincentwetzel.androidscreensaver.data.model.ScreensaverExitTrigger
import com.vincentwetzel.androidscreensaver.data.model.TimeoutMinutes
import com.vincentwetzel.androidscreensaver.data.model.TimeoutUnit
import com.vincentwetzel.androidscreensaver.ui.slideshow.NoSourcesView
import com.vincentwetzel.androidscreensaver.ui.slideshow.SlideshowView
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
    private var timeoutHandler: Handler? = null
    private var timeoutRunnable: Runnable? = null
    private var serviceJob: Job? = null
    
    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.vincentwetzel.androidscreensaver.STOP_DREAM") {
                finish()
            }
        }
    }
    
    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                // Ensure config is loaded and slideshow is running to prevent UninitializedPropertyAccessException
                if (slideshowView != null && slideshowManager.config.respectBatterySaver) {
                    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                    if (powerManager.isPowerSaveMode) {
                        slideshowView?.pause()
                    } else {
                        slideshowView?.resume()
                    }
                }
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                // Ensure config is loaded and slideshow is running to prevent UninitializedPropertyAccessException
                if (slideshowView != null && slideshowManager.config.stopOnLowBattery) {
                    val status = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                                     status == android.os.BatteryManager.BATTERY_STATUS_FULL
                    
                    // Only exit if not charging, otherwise it could immediately restart and loop
                    if (!isCharging) {
                        val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                        if (level != -1 && scale != -1) {
                            val batteryPct = level * 100 / scale.toFloat()
                            if (batteryPct <= slideshowManager.config.lowBatteryThreshold) {
                                android.util.Log.d(TAG, "Battery level $batteryPct% is below threshold ${slideshowManager.config.lowBatteryThreshold}% and not charging, finishing screensaver.")
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        android.util.Log.d("PhotoScreensaver", "onAttachedToWindow - screensaver started!")
        
        ContextCompat.registerReceiver(
            this, 
            stopReceiver, 
            IntentFilter("com.vincentwetzel.androidscreensaver.STOP_DREAM"), 
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this, 
            powerSaveReceiver, 
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), 
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this, 
            batteryReceiver, 
            IntentFilter(Intent.ACTION_BATTERY_CHANGED), 
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        isInteractive = false
        isScreenBright = false

        // Ensure a fresh job is created for this attachment lifecycle to prevent silent cancellation drops
        serviceJob?.cancel()
        serviceJob = Job()
        
        CoroutineScope(Dispatchers.Main + serviceJob!!).launch {
            // Check if any sources configured
            val hasSources = SettingsManager.hasAnySourceConfigured(this@PhotoScreensaverService)
            
            if (!hasSources) {
                // Show "no sources configured" message
                setContentView(NoSourcesView(this@PhotoScreensaverService).apply {
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
    }
    
    /**
     * Start the photo slideshow
     */
    private suspend fun startSlideshow() {
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

        // Immediately apply battery saver check if applicable
        if (slideshowManager.config.respectBatterySaver) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (powerManager.isPowerSaveMode) {
                slideshowView?.post { slideshowView?.pause() }
            }
        }

        // Setup exit trigger handling
        setupExitTrigger()

        // Setup timeout if enabled
        setupTimeout()
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

    /**
     * Setup screensaver timeout if timer is enabled
     */
    private fun setupTimeout() {
        val timerConfig = slideshowManager.config.timerConfig
        
        // Cancel existing timeout if method is called multiple times
        timeoutHandler?.removeCallbacksAndMessages(null)

        android.util.Log.d(TAG, "setupTimeout called - timeout=${timerConfig.timeoutMinutes}")
        
        // Only setup timeout if not disabled
        if (timerConfig.timeoutMinutes == TimeoutMinutes.DISABLED) {
            android.util.Log.d(TAG, "Screensaver timeout disabled")
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

        android.util.Log.d(TAG, "Screensaver timeout: $timeoutMillis ms (${timeoutMillis / 1000} seconds)")

        // Create handler and runnable for timeout
        timeoutHandler = Handler(Looper.getMainLooper())
        timeoutRunnable = Runnable {
            android.util.Log.d(TAG, "Screensaver timeout reached - finishing screensaver!")
            finish()
        }

        // Post the delayed runnable
        timeoutHandler?.postDelayed(timeoutRunnable!!, timeoutMillis)
        android.util.Log.d(TAG, "Timeout scheduled successfully")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        android.util.Log.d(TAG, "onDetachedFromWindow")
        
        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "stopReceiver not registered")
        }
        try {
            unregisterReceiver(powerSaveReceiver)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "powerSaveReceiver not registered")
        }
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "batteryReceiver not registered")
        }
        serviceJob?.cancel()
        serviceJob = null
        
        // Clean up timeout handler
        timeoutHandler?.removeCallbacksAndMessages(null)
        timeoutHandler = null
        timeoutRunnable = null
        
        slideshowView?.stop()
        slideshowView = null
        gestureDetector = null
    }


    companion object {
        private const val TAG = "PhotoScreensaver"
    }
}
