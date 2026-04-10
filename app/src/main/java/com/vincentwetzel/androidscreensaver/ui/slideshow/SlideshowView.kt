package com.vincentwetzel.androidscreensaver.ui.slideshow

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.load
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.ClockPosition
import com.vincentwetzel.androidscreensaver.data.model.ClockSize
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.dream.SlideshowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom view that displays a photo slideshow with transitions
 * Supports both images and videos (videos play with ExoPlayer)
 * Can be used in both DreamService and Activities
 */
class SlideshowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val slideshowScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var slideshowManager: SlideshowManager

    // Two image views for crossfade transitions
    private lateinit var imageViewA: ImageView
    private lateinit var imageViewB: ImageView
    private var activeView = true // true = A is visible, false = B is visible

    // Video player
    private var videoPlayer: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private var isPlayingVideo = false

    // Loading overlay
    private lateinit var loadingLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView

    // Decoration overlays
    private var dateTextView: TextView? = null
    private var clockTextView: TextView? = null
    private var weatherTextView: TextView? = null

    // Decoration timers
    private val decorationHandler = Handler(Looper.getMainLooper())
    private var clockUpdateJob: Job? = null

    // State
    private var photos: List<Photo> = emptyList()
    private var currentIndex = 0
    private var isPlaying = false
    private var slideJob: Job? = null
    private var isAdvancing = false // Prevent double-advance (video end + auto-advance racing)
    private var videoDurationJob: Job? = null // Cancelable job for video duration timers

    // Audio volume management
    private var savedSystemVolume: Int = -1 // Stores original system volume when overridden
    private var isVolumeOverridden: Boolean = false // Whether we've modified system volume

    // Callbacks
    var onSlideshowStarted: ((List<Photo>) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    init {
        setupViews()
    }

    private fun setupViews() {
        // Background color will be set in initialize() once config is available
        // Default to black until config is loaded
        setBackgroundColor(Color.parseColor("#000000"))

        // ImageView A
        imageViewA = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0f
            visibility = VISIBLE
        }

        // ImageView B
        imageViewB = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0f
            visibility = GONE
        }

        // Video player view (hidden by default)
        playerView = PlayerView(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            // useController is set dynamically in showPhoto() based on config
            visibility = GONE
        }

        // Initialize ExoPlayer — volume and repeat mode set dynamically in showVideo()
        videoPlayer = ExoPlayer.Builder(context).build().apply {
            // playWhenReady is set dynamically based on videoAutoPlay config
            repeatMode = Player.REPEAT_MODE_OFF // Set dynamically in showVideo()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        // Video finished, advance to next
                        android.util.Log.d(TAG, "Video ended, advancing to next")
                        advanceToNext()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e(TAG, "Video playback error: ${error.message}")
                    advanceToNext()
                }
            })
        }
        playerView.player = videoPlayer

        // Loading overlay
        loadingLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#000000"))
        }

        progressBar = ProgressBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        loadingText = TextView(context).apply {
            text = context.getString(R.string.loading_photos)
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 0)
        }

        loadingLayout.addView(progressBar)
        loadingLayout.addView(loadingText)

        addView(imageViewA)
        addView(imageViewB)
        addView(playerView)
        addView(loadingLayout)

        // Create decoration overlay views
        setupDecorationOverlays()
    }

    /**
     * Create decoration overlay TextViews for date, clock, and weather
     */
    private fun setupDecorationOverlays() {
        // Date decoration
        dateTextView = TextView(context).apply {
            id = View.generateViewId()
            visibility = GONE
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
        }

        // Clock decoration
        clockTextView = TextView(context).apply {
            id = View.generateViewId()
            visibility = GONE
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
        }

        // Weather decoration
        weatherTextView = TextView(context).apply {
            id = View.generateViewId()
            visibility = GONE
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
        }

        addView(dateTextView, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ))
        addView(clockTextView, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ))
        addView(weatherTextView, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ))
    }

    /**
     * Initialize and start the slideshow
     * @param slideshowManager The slideshow manager instance
     */
    fun initialize(slideshowManager: SlideshowManager) {
        this.slideshowManager = slideshowManager
        // Apply configured background color
        setBackgroundColor(slideshowManager.config.backgroundColor)
        loadingLayout.setBackgroundColor(slideshowManager.config.backgroundColor)
        // Setup decoration overlays
        updateDecorations()
        loadPhotosAndStart()
    }

    /**
     * Load photos and start the slideshow
     */
    private fun loadPhotosAndStart() {
        slideshowScope.launch {
            try {
                // Show loading
                loadingLayout.visibility = VISIBLE
                imageViewA.visibility = GONE
                imageViewB.visibility = GONE
                playerView.visibility = GONE

                // Load photos
                val loadedPhotos = withContext(Dispatchers.IO) {
                    slideshowManager.loadPhotos()
                }

                // Apply shuffle if enabled
                val finalPhotos = if (slideshowManager.config.shuffle) {
                    loadedPhotos.shuffled()
                } else {
                    // Sort by configured order
                    @Suppress("UNCHECKED_CAST")
                    loadedPhotos.sortedBy { slideshowManager.getSortKey(it) as Comparable<Any> }
                }

                photos = finalPhotos

                if (photos.isEmpty()) {
                    // No media - caller should handle this case
                    val label = when (slideshowManager.config.mediaTypeFilter) {
                        com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY -> "videos"
                        com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY -> "photos"
                        else -> "items"
                    }
                    withContext(Dispatchers.Main) {
                        onError?.invoke("No $label found in selected sources")
                    }
                    return@launch
                }

                // Notify that slideshow has started
                onSlideshowStarted?.invoke(photos)

                // Hide loading, show first photo
                currentIndex = 0
                showPhoto(photos[0], animate = false)
                loadingLayout.visibility = GONE

                // Start auto-advance
                startAutoAdvance()
                isPlaying = true

                android.util.Log.d(TAG, "Started slideshow with ${photos.size} items")

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading photos", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke("Error loading photos: ${e.message}")
                }
            }
        }
    }

    /**
     * Check if the URI points to a video file
     */
    private fun isVideo(uri: String): Boolean {
        val lower = uri.lowercase()
        // Check for video content URIs
        if (lower.contains("/video/media/")) return true
        // Check for video file extensions
        val videoExtensions = listOf(".mp4", ".avi", ".mov", ".mkv", ".webm", ".wmv", ".flv", ".m4v")
        return videoExtensions.any { lower.endsWith(it) }
    }

    /**
     * Show a single photo/video with optional crossfade animation
     */
    private fun showPhoto(photo: Photo, animate: Boolean = true) {
        val targetView = if (activeView) imageViewB else imageViewA
        val currentView = if (activeView) imageViewA else imageViewB
        val uri = Uri.parse(photo.uri)

        // Stop any playing video first
        stopVideoPlayer()

        if (isVideo(photo.uri)) {
            // Handle video playback
            android.util.Log.d(TAG, "Playing video: uri=$uri, title=${photo.title}")
            isPlayingVideo = true

            val config = slideshowManager.config

            // Apply display mode
            when (config.videoDisplayMode) {
                com.vincentwetzel.androidscreensaver.data.model.VideoDisplayMode.PLAY_FULL -> {
                    // Play full video normally
                }
                com.vincentwetzel.androidscreensaver.data.model.VideoDisplayMode.PLAY_FIXED -> {
                    // Will limit playback to fixed seconds — handled after prepare
                }
                com.vincentwetzel.androidscreensaver.data.model.VideoDisplayMode.EXTRACT_STILL -> {
                    // For still extraction, we seek to the timestamp and pause
                    // This is a simplified implementation — a full one would extract a frame
                }
            }

            // Apply show controls
            playerView.useController = config.videoShowControls

            // Hide image views, show video player
            currentView.visibility = GONE
            targetView.visibility = GONE
            playerView.visibility = VISIBLE

            // Set media source
            val mediaItem = MediaItem.fromUri(uri)
            videoPlayer?.setMediaItem(mediaItem)
            videoPlayer?.prepare()

            // Check minimum duration — skip video if it's too short
            if (config.videoMinDurationSeconds > 0) {
                val videoDurationMs = videoPlayer?.duration ?: 0L
                val minDurationMs = config.videoMinDurationSeconds * 1000L
                if (videoDurationMs < minDurationMs && videoDurationMs > 0) {
                    android.util.Log.d(TAG, "Skipping short video: duration=${videoDurationMs}ms < min=${minDurationMs}ms")
                    advanceToNext()
                    return
                }
            }

            // Apply audio mode and volume
            applyAudioMode(config)

            // Apply loop setting for short videos
            videoPlayer?.repeatMode = if (config.videoLoopShort) {
                Player.REPEAT_MODE_ONE
            } else {
                Player.REPEAT_MODE_OFF
            }

            // Apply auto play setting
            videoPlayer?.playWhenReady = config.videoAutoPlay

            // Handle videoMaxDurationSeconds: set a hard cap
            if (config.videoMaxDurationSeconds > 0) {
                val maxMs = config.videoMaxDurationSeconds * 1000L
                videoDurationJob?.cancel()
                videoDurationJob = slideshowScope.launch {
                    delay(maxMs)
                    if (isPlayingVideo) {
                        android.util.Log.d(TAG, "Max duration reached ($maxMs ms), advancing")
                        advanceToNext()
                    }
                }
            }
        } else {
            // Handle image display
            android.util.Log.d(TAG, "Loading image: uri=$uri, title=${photo.title}")
            isPlayingVideo = false

            // Hide video player, show image views
            playerView.visibility = GONE

            // Apply match_orientation: use FIT_CENTER if photo orientation doesn't match device
            if (slideshowManager.config.matchDeviceOrientation) {
                val photoIsPortrait = (photo.height ?: 0) > (photo.width ?: 0)
                val deviceIsPortrait = context.resources.configuration.orientation ==
                    android.content.res.Configuration.ORIENTATION_PORTRAIT
                targetView.scaleType = if (photoIsPortrait == deviceIsPortrait) {
                    ImageView.ScaleType.CENTER_CROP
                } else {
                    ImageView.ScaleType.FIT_CENTER
                }
            } else {
                targetView.scaleType = ImageView.ScaleType.CENTER_CROP
            }

            targetView.load(uri) {
                crossfade(false)
                placeholder(android.R.color.black)
                error(android.R.color.black)
                allowHardware(false)
                listener(
                    onStart = {
                        android.util.Log.d(TAG, "Started loading: $uri")
                    },
                    onSuccess = { _, _ ->
                        android.util.Log.d(TAG, "Successfully loaded: $uri")
                    },
                    onError = { _, result ->
                        android.util.Log.e(TAG, "Failed to load: $uri, error=${result.throwable?.message}")
                    }
                )
            }

            if (animate) {
                // Crossfade transition
                currentView.visibility = VISIBLE
                targetView.visibility = VISIBLE
                targetView.alpha = 0f

                val fadeIn = ObjectAnimator.ofFloat(targetView, "alpha", 0f, 1f).apply {
                    duration = slideshowManager.config.transitionDurationMs.toLong()
                    interpolator = DecelerateInterpolator()
                }

                val fadeOut = ObjectAnimator.ofFloat(currentView, "alpha", 1f, 0f).apply {
                    duration = slideshowManager.config.transitionDurationMs.toLong()
                    interpolator = DecelerateInterpolator()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            activeView = !activeView
                        }
                    })
                }

                fadeIn.start()
                fadeOut.start()
            } else {
                // No animation - just show immediately
                targetView.alpha = 1f
                targetView.visibility = VISIBLE
                currentView.visibility = GONE
                activeView = !activeView
            }
        }
    }

    /**
     * Advance to the next item in the slideshow
     */
    private fun advanceToNext() {
        if (!isPlaying || photos.isEmpty() || isAdvancing) return
        isAdvancing = true
        currentIndex = (currentIndex + 1) % photos.size
        showPhoto(photos[currentIndex], animate = true)
        // isAdvancing is reset in showPhoto when the next item is displayed
        // For images: reset after transition starts
        // For videos: reset after video starts playing (ExoPlayer handles its own end callback)
        if (!isPlayingVideo) {
            isAdvancing = false
        }
    }

    /**
     * Apply audio mode settings. For CUSTOM_VOLUME, temporarily overrides system volume.
     */
    private fun applyAudioMode(config: com.vincentwetzel.androidscreensaver.data.model.SlideshowConfig) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // First, restore any previously overridden system volume
        restoreSystemVolume(audioManager)

        when (config.videoAudioMode) {
            com.vincentwetzel.androidscreensaver.data.model.VideoAudioMode.MUTE -> {
                // Mute: set player volume to 0
                videoPlayer?.volume = 0f
                android.util.Log.d(TAG, "Audio: MUTED (player volume=0)")
            }
            com.vincentwetzel.androidscreensaver.data.model.VideoAudioMode.SYSTEM_VOLUME -> {
                // System volume: set player volume to 1.0 (full), let system control output
                videoPlayer?.volume = 1f
                android.util.Log.d(TAG, "Audio: SYSTEM VOLUME (player volume=1.0)")
            }
            com.vincentwetzel.androidscreensaver.data.model.VideoAudioMode.CUSTOM_VOLUME -> {
                // Custom volume: calculate target system volume level and override it
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVolume = (maxVolume * config.videoCustomVolume / 100f).toInt().coerceIn(0, maxVolume)

                // Save current system volume
                savedSystemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

                // Set system volume to custom level
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    targetVolume,
                    0 // No flags - don't show volume UI or play sound
                )
                isVolumeOverridden = true

                // Set player volume to full (we're controlling via system volume)
                videoPlayer?.volume = 1f

                android.util.Log.d(TAG, "Audio: CUSTOM VOLUME - system volume set to $targetVolume/$maxVolume (${config.videoCustomVolume}%), player volume=1.0, saved original=$savedSystemVolume")
            }
        }
    }

    /**
     * Restore system volume to the saved value if we had overridden it
     */
    private fun restoreSystemVolume(audioManager: AudioManager? = null) {
        if (!isVolumeOverridden || savedSystemVolume < 0) return

        val am = audioManager ?: context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            savedSystemVolume,
            0
        )
        android.util.Log.d(TAG, "Audio: Restored system volume to $savedSystemVolume")
        savedSystemVolume = -1
        isVolumeOverridden = false
    }

    // ==================== DECORATIONS ====================

    /**
     * Update all decoration overlays based on current config
     */
    private fun updateDecorations() {
        val config = slideshowManager.config

        // Date decoration
        if (config.dateDecoration != null) {
            updateDateDecoration(config)
            dateTextView?.visibility = VISIBLE
        } else {
            dateTextView?.visibility = GONE
        }

        // Clock decoration
        if (config.clockDecoration != null) {
            updateClockDecoration(config)
            clockTextView?.visibility = VISIBLE
            startClockUpdates()
        } else {
            clockTextView?.visibility = GONE
            stopClockUpdates()
        }

        // Weather decoration
        if (config.weatherDecoration != null) {
            updateWeatherDecoration(config)
            weatherTextView?.visibility = VISIBLE
        } else {
            weatherTextView?.visibility = GONE
        }
    }

    private fun updateDateDecoration(config: com.vincentwetzel.androidscreensaver.data.model.SlideshowConfig) {
        val dateConfig = config.dateDecoration ?: return
        val dateTextView = dateTextView ?: return

        val now = Date()
        val format = when (dateConfig.dateFormat) {
            com.vincentwetzel.androidscreensaver.data.model.DateFormat.FULL_DATE -> "EEEE, MMMM d, yyyy"
            com.vincentwetzel.androidscreensaver.data.model.DateFormat.SHORT_DATE -> "MMM d, yyyy"
            com.vincentwetzel.androidscreensaver.data.model.DateFormat.MONTH_DAY -> "MMMM d"
            com.vincentwetzel.androidscreensaver.data.model.DateFormat.NUMERIC_DATE -> "MM/dd/yyyy"
            com.vincentwetzel.androidscreensaver.data.model.DateFormat.ABBREVIATE_MONTH -> "MMM d, yyyy"
            com.vincentwetzel.androidscreensaver.data.model.DateFormat.WEEKDAY -> "EEEE"
            com.vincentwetzel.androidscreensaver.data.model.DateFormat.YEAR -> "yyyy"
            com.vincentwetzel.androidscreensaver.data.model.DateFormat.ABBREVIATE_WEEKDAY -> "EEE"
            com.vincentwetzel.androidscreensaver.data.model.DateFormat.CUSTOM -> dateConfig.customDateFormatPattern ?: "M/d/yyyy"
        }
        dateTextView.text = SimpleDateFormat(format, Locale.getDefault()).format(now)

        applyDecorationStyle(dateTextView, dateConfig.position, dateConfig.fontSize, dateConfig.opacity, dateConfig.background)
    }

    private fun updateClockDecoration(config: com.vincentwetzel.androidscreensaver.data.model.SlideshowConfig) {
        val clockConfig = config.clockDecoration ?: return
        val clockTextView = clockTextView ?: return

        val now = Date()
        val pattern = if (clockConfig.clockFormat == com.vincentwetzel.androidscreensaver.data.model.ClockFormat.HOUR_24) {
            if (clockConfig.showSeconds) "HH:mm:ss" else "HH:mm"
        } else {
            if (clockConfig.showSeconds) "h:mm:ss a" else "h:mm a"
        }
        clockTextView.text = SimpleDateFormat(pattern, Locale.getDefault()).format(now)

        applyDecorationStyle(clockTextView, clockConfig.position, clockConfig.fontSize, clockConfig.opacity, clockConfig.background)
    }

    private fun updateWeatherDecoration(config: com.vincentwetzel.androidscreensaver.data.model.SlideshowConfig) {
        val weatherConfig = config.weatherDecoration ?: return
        val weatherTextView = weatherTextView ?: return

        // Placeholder weather text — real implementation would fetch from Open-Meteo API
        val tempUnit = when (weatherConfig.temperatureUnit) {
            com.vincentwetzel.androidscreensaver.data.model.TemperatureUnit.CELSIUS -> "°C"
            else -> "°F"
        }
        weatherTextView.text = "72${tempUnit} ☀️"

        applyDecorationStyle(weatherTextView, weatherConfig.position, weatherConfig.fontSize, weatherConfig.opacity,
            if (weatherConfig.widgetBackground == com.vincentwetzel.androidscreensaver.data.model.WeatherWidgetBackground.SOLID)
                com.vincentwetzel.androidscreensaver.data.model.DecorationBackground.SOLID
            else if (weatherConfig.widgetBackground == com.vincentwetzel.androidscreensaver.data.model.WeatherWidgetBackground.FROSTED_GLASS)
                com.vincentwetzel.androidscreensaver.data.model.DecorationBackground.SEMI_TRANSPARENT
            else
                com.vincentwetzel.androidscreensaver.data.model.DecorationBackground.NONE)
    }

    /**
     * Apply common decoration styling (position, font size, opacity, background)
     */
    private fun applyDecorationStyle(
        textView: TextView,
        position: ClockPosition,
        size: ClockSize,
        opacity: Int,
        background: com.vincentwetzel.androidscreensaver.data.model.DecorationBackground
    ) {
        // Font size
        val fontSizeDp = when (size) {
            ClockSize.SMALL -> 16f
            ClockSize.MEDIUM -> 24f
            ClockSize.LARGE -> 36f
        }
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeDp)

        // Position
        val params = textView.layoutParams as LayoutParams
        params.width = LayoutParams.WRAP_CONTENT
        params.height = LayoutParams.WRAP_CONTENT
        params.gravity = when (position) {
            ClockPosition.TOP_LEFT -> Gravity.TOP or Gravity.START
            ClockPosition.TOP_RIGHT -> Gravity.TOP or Gravity.END
            ClockPosition.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
            ClockPosition.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
            ClockPosition.CENTER -> Gravity.CENTER
        }
        val padding = 32
        params.setMargins(padding, padding, padding, padding)
        textView.layoutParams = params

        // Background
        val bgDrawable = when (background) {
            com.vincentwetzel.androidscreensaver.data.model.DecorationBackground.NONE -> null
            com.vincentwetzel.androidscreensaver.data.model.DecorationBackground.SEMI_TRANSPARENT -> GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.argb(128, 0, 0, 0))
                cornerRadius = 16f
            }
            com.vincentwetzel.androidscreensaver.data.model.DecorationBackground.SOLID -> GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.argb(200, 0, 0, 0))
                cornerRadius = 16f
            }
        }
        textView.background = bgDrawable

        // Opacity
        textView.alpha = opacity / 100f
    }

    /**
     * Start periodic clock updates (every second)
     */
    private fun startClockUpdates() {
        val runnable = object : Runnable {
            override fun run() {
                val config = slideshowManager.config
                if (config.clockDecoration != null) {
                    updateClockDecoration(config)
                }
                decorationHandler.postDelayed(this, 1000)
            }
        }
        decorationHandler.postDelayed(runnable, 1000)
    }

    /**
     * Stop clock updates
     */
    private fun stopClockUpdates() {
        decorationHandler.removeCallbacksAndMessages(null)
    }

    /**
     * Stop the video player and cancel any pending duration timers
     */
    private fun stopVideoPlayer() {
        // Restore system volume if we had overridden it
        restoreSystemVolume()

        videoDurationJob?.cancel()
        videoDurationJob = null
        if (isPlayingVideo) {
            videoPlayer?.stop()
            videoPlayer?.clearMediaItems()
            isPlayingVideo = false
        }
    }

    /**
     * Start automatic photo advancement
     */
    private fun startAutoAdvance() {
        slideJob?.cancel()
        slideJob = slideshowScope.launch {
            while (isPlaying && photos.isNotEmpty()) {
                // Reload config each cycle so settings changes take effect immediately
                slideshowManager.loadConfig()
                updateDecorations()

                // For videos, wait for them to finish (handled by ExoPlayer listener)
                // The isPlayingVideo flag is set in showPhoto() when a video starts
                if (isPlayingVideo) {
                    delay(500) // Poll every 500ms
                    continue
                }

                // Reset advancing guard if we're showing a still image
                isAdvancing = false

                val durationMs = slideshowManager.config.slideDurationSeconds * 1000L
                delay(durationMs)

                if (!isPlaying) break

                advanceToNext()
            }
        }
    }

    /**
     * Pause the slideshow
     */
    fun pause() {
        isPlaying = false
        slideJob?.cancel()
        slideJob = null
        if (isPlayingVideo) {
            videoPlayer?.pause()
        }
    }

    /**
     * Resume the slideshow
     */
    fun resume() {
        if (photos.isNotEmpty() && !isPlaying) {
            isPlaying = true
            if (isPlayingVideo) {
                videoPlayer?.play()
            }
            startAutoAdvance()
        }
    }

    /**
     * Stop the slideshow and clean up resources
     */
    fun stop() {
        pause()
        stopVideoPlayer()
        stopClockUpdates()
        slideshowScope.launch {
            withContext(Dispatchers.Main) {
                imageViewA.setImageDrawable(null)
                imageViewB.setImageDrawable(null)
            }
        }
    }

    /**
     * Clean up when view is detached
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
        videoPlayer?.release()
        videoPlayer = null
    }

    companion object {
        private const val TAG = "SlideshowView"
    }
}
