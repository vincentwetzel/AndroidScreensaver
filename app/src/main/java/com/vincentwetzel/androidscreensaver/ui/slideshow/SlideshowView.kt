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
import coil.ImageLoader
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import coil.request.ErrorResult
import coil.request.SuccessResult
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.ClockPosition
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.PhotoInfoConfig
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
    private var photoInfoTextView: TextView? = null

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
    private var burnInProtectionJob: Job? = null // Cancelable job for pixel shifting

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
            repeatMode = Player.REPEAT_MODE_OFF // Set dynamically in showPhoto()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        // Check duration and apply loop dynamically for short videos
                        val durationMs = duration
                        val isShort = durationMs in 1..15000L // 15 seconds threshold
                        val shouldLoop = slideshowManager.config.videoLoopShort && isShort
                        
                        repeatMode = if (shouldLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                        
                        // Set a reasonable cap for looping short videos so they don't play forever
                        if (shouldLoop) {
                            val slideDurationMs = slideshowManager.config.slideDurationSeconds * 1000L
                            val loopTimeoutMs = maxOf(slideDurationMs, durationMs * 2).coerceAtMost(30000L)
                            videoDurationJob?.cancel()
                            videoDurationJob = slideshowScope.launch {
                                delay(loopTimeoutMs)
                                if (isPlayingVideo) {
                                    android.util.Log.d(TAG, "Short video loop timeout reached ($loopTimeoutMs ms), advancing")
                                    advanceToNext()
                                }
                            }
                        }
                    } else if (playbackState == Player.STATE_ENDED) {
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
        setupPhotoInfoOverlay()
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
    
    private fun setupPhotoInfoOverlay() {
        photoInfoTextView = TextView(context).apply {
            id = View.generateViewId()
            visibility = GONE
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        addView(photoInfoTextView, LayoutParams(
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
        startBurnInProtection()
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

                android.util.Log.d(TAG, "Loading photos from sources...")

                // Load photos
                val loadedPhotos = withContext(Dispatchers.IO) {
                    slideshowManager.loadPhotos()
                }

                android.util.Log.d(TAG, "Loaded ${loadedPhotos.size} photos from sources")

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
                    // No media - hide loading and notify caller
                    val label = when (slideshowManager.config.mediaTypeFilter) {
                        com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.VIDEOS_ONLY -> "videos"
                        com.vincentwetzel.androidscreensaver.data.model.MediaTypeFilter.IMAGES_ONLY -> "photos"
                        else -> "items"
                    }
                    loadingLayout.visibility = GONE
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
                loadingLayout.visibility = GONE
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
        val config = slideshowManager.config

        // Stop any playing video first
        stopVideoPlayer()

        if (isVideo(photo.uri)) {
            // Handle video playback
            android.util.Log.d(TAG, "Playing video: uri=$uri, title=${photo.title}")
            isPlayingVideo = true

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

            // Loop setting is deferred until video is prepared (STATE_READY) 
            // so we can dynamically check its duration
            videoPlayer?.repeatMode = Player.REPEAT_MODE_OFF

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

            // Cancel any running animation
            targetView.clearAnimation()
            targetView.translationX = 0f
            targetView.translationY = 0f
            targetView.scaleX = 1f
            targetView.scaleY = 1f

            // Apply display effect
            applyDisplayEffect(targetView, config)

            if (photo.uri.startsWith("http") && photo.sourceType == com.vincentwetzel.androidscreensaver.data.model.SourceType.GOOGLE_DRIVE) {
                slideshowScope.launch {
                    val localUri = slideshowManager.downloadPhotoToLocalCache(photo)
                    if (localUri != null) {
                        val newPhoto = photo.copy(uri = localUri)
                        loadImage(newPhoto, targetView, currentView, animate)
                    } else {
                        // Handle download error
                        advanceToNext()
                    }
                }
            } else {
                loadImage(photo, targetView, currentView, animate)
            }
        }
    }
    
    private fun loadImage(photo: Photo, targetView: ImageView, currentView: ImageView, animate: Boolean) {
        val uri = Uri.parse(photo.uri)
        val config = slideshowManager.config
        
        // Use explicit ImageRequest for better error handling
        val request = ImageRequest.Builder(context)
            .data(uri)
            .crossfade(false)
            .placeholder(android.R.color.black)
            .error(android.R.color.black)
            .allowHardware(false)
            .target(
                onStart = {
                    android.util.Log.d(TAG, "Started loading: $uri")
                },
                onSuccess = { drawable ->
                    targetView.setImageDrawable(drawable)
                    android.util.Log.d(TAG, "Successfully loaded: $uri, size=${drawable.intrinsicWidth}x${drawable.intrinsicHeight}")
                    // Now that image is loaded, either transition or show immediately
                    if (animate) {
                        applyTransition(currentView, targetView)
                    } else {
                        targetView.alpha = 1f
                        targetView.visibility = VISIBLE
                        currentView.visibility = GONE
                        activeView = !activeView
                        // Start display effect animation after image is shown
                        startDisplayEffectAnimation(targetView, config)
                    }
                    updatePhotoInfoOverlay(photo)
                },
                onError = { errorDrawable ->
                    android.util.Log.e(TAG, "Failed to load: $uri, drawable=$errorDrawable")
                    // Skip this photo and advance to next
                    slideshowScope.launch {
                        delay(1000) // Wait 1s before advancing
                        advanceToNext()
                    }
                }
            )
            .build()

        // Execute the request using the singleton ImageLoader
        // This is CRITICAL for connection pooling and memory/disk caching to work
        context.imageLoader.enqueue(request)
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
        
        if (photos.isNotEmpty()) {
            updatePhotoInfoOverlay(photos[currentIndex])
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
    
    private fun updatePhotoInfoOverlay(photo: Photo) {
        val config = slideshowManager.config.photoInfoConfig
        val photoInfoTextView = photoInfoTextView ?: return

        if (!config.enabled) {
            photoInfoTextView.visibility = GONE
            return
        }

        val info = mutableListOf<String>()

        if (config.showFileName) {
            photo.title?.let {
                var name = it
                if (!config.showFileNameWithExtension) {
                    name = name.substringBeforeLast('.')
                }
                info.add(name)
            }
        }
        if (config.showDateTaken) {
            photo.dateTaken?.let {
                val format = when (config.dateFormat) {
                    com.vincentwetzel.androidscreensaver.data.model.PhotoInfoDateFormat.FULL_DATE -> "EEEE, MMMM d, yyyy"
                    com.vincentwetzel.androidscreensaver.data.model.PhotoInfoDateFormat.SHORT_DATE -> "MMM d, yyyy"
                    com.vincentwetzel.androidscreensaver.data.model.PhotoInfoDateFormat.NUMERIC -> "MM/dd/yyyy"
                    com.vincentwetzel.androidscreensaver.data.model.PhotoInfoDateFormat.RELATIVE -> {
                        // Not implemented yet, fall back to short
                        "MMM d, yyyy"
                    }
                }
                info.add(SimpleDateFormat(format, Locale.getDefault()).format(Date(it)))
            }
        }
        if (config.showSourceName) {
            info.add(photo.sourceType.name)
        }
        if (config.showDescription) {
            photo.description?.let { info.add(it) }
        }
        if (config.showDimensions) {
            photo.width?.let { w ->
                photo.height?.let { h ->
                    info.add("${w}x${h}")
                }
            }
        }
        if (config.showFileSize) {
            photo.fileSize?.let {
                val sizeInMb = it / 1024.0 / 1024.0
                info.add(String.format("%.2f MB", sizeInMb))
            }
        }


        val separator = when (config.separator) {
            com.vincentwetzel.androidscreensaver.data.model.PhotoInfoSeparator.BULLET -> " • "
            com.vincentwetzel.androidscreensaver.data.model.PhotoInfoSeparator.PIPE -> " | "
            com.vincentwetzel.androidscreensaver.data.model.PhotoInfoSeparator.DASH -> " — "
            com.vincentwetzel.androidscreensaver.data.model.PhotoInfoSeparator.SLASH -> " / "
            com.vincentwetzel.androidscreensaver.data.model.PhotoInfoSeparator.COMMA -> ", "
        }

        photoInfoTextView.text = info.joinToString(separator)

        applyDecorationStyle(
            photoInfoTextView,
            config.position,
            16f,
            config.textOpacity,
            config.background.toDecorationBackground()
        )
        
        photoInfoTextView.visibility = VISIBLE
    }

    // ==================== DISPLAY EFFECTS ====================

    /**
     * Apply the configured display effect to the target image view.
     * Sets scaleType immediately (before image load). Animations are started separately
     * via startDisplayEffectAnimation() after the image finishes loading.
     */
    private fun applyDisplayEffect(targetView: ImageView, config: com.vincentwetzel.androidscreensaver.data.model.SlideshowConfig) {
        android.util.Log.d(TAG, "=== Applying display effect: ${config.displayEffect}, panDirection=${config.panDirection}, matchOrientation=${config.matchDeviceOrientation}")

        when (config.displayEffect) {
            com.vincentwetzel.androidscreensaver.data.model.DisplayEffect.CROP_TO_FIT -> {
                targetView.scaleType = ImageView.ScaleType.CENTER_CROP
                android.util.Log.d(TAG, "  -> CROP_TO_FIT (scaleType set, no animation)")
            }
            com.vincentwetzel.androidscreensaver.data.model.DisplayEffect.SCALE_TO_FIT -> {
                targetView.scaleType = ImageView.ScaleType.FIT_CENTER
                android.util.Log.d(TAG, "  -> SCALE_TO_FIT (scaleType set, no animation)")
            }
            com.vincentwetzel.androidscreensaver.data.model.DisplayEffect.ZOOM -> {
                targetView.scaleType = ImageView.ScaleType.CENTER_CROP
                android.util.Log.d(TAG, "  -> ZOOM (scaleType set, animation starts after load)")
            }
            com.vincentwetzel.androidscreensaver.data.model.DisplayEffect.PAN -> {
                targetView.scaleType = ImageView.ScaleType.CENTER_CROP
                android.util.Log.d(TAG, "  -> PAN (scaleType set, animation starts after load)")
            }
            com.vincentwetzel.androidscreensaver.data.model.DisplayEffect.FOCUS -> {
                targetView.scaleType = ImageView.ScaleType.CENTER_CROP
                android.util.Log.d(TAG, "  -> FOCUS (scaleType set, animation starts after load)")
            }
        }
    }

    /**
     * Start display effect animations. Called AFTER the image has been loaded by Coil.
     */
    private fun startDisplayEffectAnimation(targetView: ImageView, config: com.vincentwetzel.androidscreensaver.data.model.SlideshowConfig) {
        when (config.displayEffect) {
            com.vincentwetzel.androidscreensaver.data.model.DisplayEffect.ZOOM -> {
                // Reset any previous animation state
                targetView.scaleX = 1f
                targetView.scaleY = 1f
                // Animate scale from 1.0 to 1.5 over slide duration
                ObjectAnimator.ofFloat(targetView, View.SCALE_X, 1.0f, 1.5f).apply {
                    duration = config.slideDurationSeconds * 1000L
                    interpolator = DecelerateInterpolator()
                    start()
                }
                ObjectAnimator.ofFloat(targetView, View.SCALE_Y, 1.0f, 1.5f).apply {
                    duration = config.slideDurationSeconds * 1000L
                    interpolator = DecelerateInterpolator()
                    start()
                }
            }
            com.vincentwetzel.androidscreensaver.data.model.DisplayEffect.PAN -> {
                // Reset any previous animation state
                targetView.translationX = 0f
                targetView.translationY = 0f
                applyPanEffect(targetView, config)
            }
            com.vincentwetzel.androidscreensaver.data.model.DisplayEffect.FOCUS -> {
                // Reset alpha
                targetView.alpha = 0f
                ObjectAnimator.ofFloat(targetView, View.ALPHA, 0f, 1f).apply {
                    duration = (config.slideDurationSeconds * 1000L / 3).coerceAtMost(2000L)
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }
            else -> {
                // CROP_TO_FIT and SCALE_TO_FIT have no animations
            }
        }
    }

    /**
     * Apply Ken Burns pan effect with configurable direction
     */
    private fun applyPanEffect(targetView: ImageView, config: com.vincentwetzel.androidscreensaver.data.model.SlideshowConfig) {
        val direction = config.panDirection
        val actualDirection = if (direction == com.vincentwetzel.androidscreensaver.data.model.PanDirection.RANDOM) {
            com.vincentwetzel.androidscreensaver.data.model.PanDirection.values().random()
        } else {
            direction
        }

        val panDistance = targetView.width * 0.15f
        val duration = config.slideDurationSeconds * 1000L

        val (startX, endX, startY, endY) = when (actualDirection) {
            com.vincentwetzel.androidscreensaver.data.model.PanDirection.LEFT_TO_RIGHT ->
                listOf(-panDistance, panDistance, 0f, 0f)
            com.vincentwetzel.androidscreensaver.data.model.PanDirection.RIGHT_TO_LEFT ->
                listOf(panDistance, -panDistance, 0f, 0f)
            com.vincentwetzel.androidscreensaver.data.model.PanDirection.TOP_TO_BOTTOM ->
                listOf(0f, 0f, -panDistance, panDistance)
            com.vincentwetzel.androidscreensaver.data.model.PanDirection.BOTTOM_TO_TOP ->
                listOf(0f, 0f, panDistance, -panDistance)
            else -> listOf(-panDistance, panDistance, 0f, 0f)
        }

        ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X, startX, endX).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(targetView, View.TRANSLATION_Y, startY, endY).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /**
     * Stop any running display effect animations
     */
    private fun stopDisplayEffects() {
        imageViewA.clearAnimation()
        imageViewB.clearAnimation()
        imageViewA.translationX = 0f
        imageViewA.translationY = 0f
        imageViewA.scaleX = 1f
        imageViewA.scaleY = 1f
        imageViewA.alpha = 1f
        imageViewB.translationX = 0f
        imageViewB.translationY = 0f
        imageViewB.scaleX = 1f
        imageViewB.scaleY = 1f
        imageViewB.alpha = 1f
    }

    // ==================== TRANSITIONS ====================

    /**
     * Apply the configured transition effect between two image views.
     * Called AFTER the target image is loaded and ready.
     */
    private fun applyTransition(currentView: ImageView, targetView: ImageView) {
        val config = slideshowManager.config
        val durationMs = config.transitionDurationMs.toLong()
        val effect = config.transitionEffect
        val easing = when (config.transitionEasing) {
            com.vincentwetzel.androidscreensaver.data.model.TransitionEasing.EASE_IN_OUT -> AccelerateDecelerateInterpolator()
            com.vincentwetzel.androidscreensaver.data.model.TransitionEasing.EASE_IN -> android.view.animation.AccelerateInterpolator()
            com.vincentwetzel.androidscreensaver.data.model.TransitionEasing.EASE_OUT -> DecelerateInterpolator()
            com.vincentwetzel.androidscreensaver.data.model.TransitionEasing.LINEAR -> android.view.animation.LinearInterpolator()
        }

        // Get view dimensions for slide/wipe transitions (use measured width/height)
        val viewWidth = if (currentView.width > 0) currentView.width.toFloat()
            else context.resources.displayMetrics.widthPixels.toFloat()
        val viewHeight = if (currentView.height > 0) currentView.height.toFloat()
            else context.resources.displayMetrics.heightPixels.toFloat()

        // Clear any running animations on both views
        currentView.clearAnimation()
        targetView.clearAnimation()

        when (effect) {
            com.vincentwetzel.androidscreensaver.data.model.TransitionEffect.FADE -> {
                currentView.visibility = VISIBLE
                currentView.alpha = 1f
                targetView.visibility = VISIBLE
                targetView.alpha = 0f
                ObjectAnimator.ofFloat(targetView, View.ALPHA, 0f, 1f).apply {
                    this.duration = durationMs
                    interpolator = easing
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            currentView.alpha = 1f
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                    start()
                }
            }
            com.vincentwetzel.androidscreensaver.data.model.TransitionEffect.CROSS_FADE -> {
                currentView.visibility = VISIBLE
                currentView.alpha = 1f
                targetView.visibility = VISIBLE
                targetView.alpha = 0f
                val fadeIn = ObjectAnimator.ofFloat(targetView, View.ALPHA, 0f, 1f).apply {
                    this.duration = durationMs
                    interpolator = easing
                }
                val fadeOut = ObjectAnimator.ofFloat(currentView, View.ALPHA, 1f, 0f).apply {
                    this.duration = durationMs
                    interpolator = easing
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            currentView.alpha = 1f
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                }
                fadeIn.start()
                fadeOut.start()
            }
            com.vincentwetzel.androidscreensaver.data.model.TransitionEffect.SLIDE -> {
                currentView.visibility = VISIBLE
                currentView.alpha = 1f
                currentView.translationX = 0f
                targetView.visibility = VISIBLE
                targetView.alpha = 1f
                targetView.translationX = viewWidth
                val slideIn = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X, viewWidth, 0f).apply {
                    this.duration = durationMs
                    interpolator = easing
                }
                val slideOut = ObjectAnimator.ofFloat(currentView, View.TRANSLATION_X, 0f, -viewWidth).apply {
                    this.duration = durationMs
                    interpolator = easing
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            currentView.translationX = 0f
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                }
                slideIn.start()
                slideOut.start()
            }
            com.vincentwetzel.androidscreensaver.data.model.TransitionEffect.WIPE -> {
                currentView.visibility = VISIBLE
                currentView.alpha = 1f
                targetView.visibility = VISIBLE
                targetView.alpha = 1f
                targetView.translationY = -viewHeight
                val wipeIn = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_Y, -viewHeight, 0f).apply {
                    this.duration = durationMs
                    interpolator = easing
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            targetView.translationY = 0f
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                }
                wipeIn.start()
            }
            com.vincentwetzel.androidscreensaver.data.model.TransitionEffect.SWAP -> {
                currentView.visibility = VISIBLE
                currentView.alpha = 1f
                currentView.translationX = 0f
                targetView.visibility = VISIBLE
                targetView.alpha = 1f
                targetView.translationX = viewWidth
                val slideIn = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X, viewWidth, 0f).apply {
                    this.duration = durationMs
                    interpolator = easing
                }
                val slideOut = ObjectAnimator.ofFloat(currentView, View.TRANSLATION_X, 0f, -viewWidth).apply {
                    this.duration = durationMs
                    interpolator = easing
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            currentView.translationX = 0f
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                }
                slideIn.start()
                slideOut.start()
            }
            com.vincentwetzel.androidscreensaver.data.model.TransitionEffect.ZOOM -> {
                currentView.visibility = VISIBLE
                currentView.alpha = 1f
                targetView.visibility = VISIBLE
                targetView.scaleX = 0.1f
                targetView.scaleY = 0.1f
                targetView.alpha = 0f
                val scaleX = ObjectAnimator.ofFloat(targetView, View.SCALE_X, 0.1f, 1f).apply {
                    this.duration = durationMs
                    interpolator = easing
                }
                val scaleY = ObjectAnimator.ofFloat(targetView, View.SCALE_Y, 0.1f, 1f).apply {
                    this.duration = durationMs
                    interpolator = easing
                }
                val fadeIn = ObjectAnimator.ofFloat(targetView, View.ALPHA, 0f, 1f).apply {
                    this.duration = durationMs
                    interpolator = easing
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            targetView.scaleX = 1f
                            targetView.scaleY = 1f
                            targetView.alpha = 1f
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                }
                scaleX.start()
                scaleY.start()
                fadeIn.start()
            }
            com.vincentwetzel.androidscreensaver.data.model.TransitionEffect.FLASH -> {
                currentView.visibility = VISIBLE
                targetView.visibility = GONE
                val flashView = ImageView(context).apply {
                    setBackgroundColor(Color.WHITE)
                    alpha = 0f
                }
                addView(flashView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
                val fadeIn = ObjectAnimator.ofFloat(flashView, View.ALPHA, 0f, 1f).apply {
                    this.duration = durationMs / 3
                    interpolator = android.view.animation.AccelerateInterpolator()
                }
                val fadeOut = ObjectAnimator.ofFloat(flashView, View.ALPHA, 1f, 0f).apply {
                    this.duration = durationMs * 2 / 3
                    interpolator = DecelerateInterpolator()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            targetView.visibility = VISIBLE
                            targetView.alpha = 1f
                            removeView(flashView)
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                }
                fadeIn.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        targetView.visibility = VISIBLE
                        targetView.alpha = 1f
                        fadeOut.start()
                    }
                })
                fadeIn.start()
            }
            com.vincentwetzel.androidscreensaver.data.model.TransitionEffect.RIPPLE -> {
                currentView.visibility = VISIBLE
                currentView.alpha = 1f
                targetView.visibility = VISIBLE
                targetView.alpha = 0f
                targetView.scaleX = 0.5f
                targetView.scaleY = 0.5f
                val rippleInterpolator = android.view.animation.OvershootInterpolator(2f)
                val scaleX = ObjectAnimator.ofFloat(targetView, View.SCALE_X, 0.5f, 1f).apply {
                    this.duration = durationMs
                    interpolator = rippleInterpolator
                }
                val scaleY = ObjectAnimator.ofFloat(targetView, View.SCALE_Y, 0.5f, 1f).apply {
                    this.duration = durationMs
                    interpolator = rippleInterpolator
                }
                val fadeIn = ObjectAnimator.ofFloat(targetView, View.ALPHA, 0f, 1f).apply {
                    this.duration = durationMs
                    interpolator = easing
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            targetView.scaleX = 1f
                            targetView.scaleY = 1f
                            targetView.alpha = 1f
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                }
                scaleX.start()
                scaleY.start()
                fadeIn.start()
            }
            com.vincentwetzel.androidscreensaver.data.model.TransitionEffect.DOORWAY -> {
                currentView.visibility = VISIBLE
                currentView.alpha = 1f
                targetView.visibility = VISIBLE
                targetView.alpha = 0f
                val slideOut = ObjectAnimator.ofFloat(currentView, View.TRANSLATION_X, 0f, -viewWidth / 2).apply {
                    this.duration = durationMs
                    interpolator = easing
                }
                val fadeOutCurrent = ObjectAnimator.ofFloat(currentView, View.ALPHA, 1f, 0f).apply {
                    this.duration = durationMs
                    interpolator = easing
                }
                val fadeIn = ObjectAnimator.ofFloat(targetView, View.ALPHA, 0f, 1f).apply {
                    this.duration = durationMs
                    interpolator = easing
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            currentView.translationX = 0f
                            currentView.alpha = 1f
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                }
                slideOut.start()
                fadeOutCurrent.start()
                fadeIn.start()
            }
            com.vincentwetzel.androidscreensaver.data.model.TransitionEffect.RADIAL -> {
                currentView.visibility = VISIBLE
                currentView.alpha = 1f
                targetView.visibility = VISIBLE
                targetView.alpha = 1f
                
                // Calculate the center point and max radius for the circular reveal
                val centerX = targetView.width / 2f
                val centerY = targetView.height / 2f
                val startRadius = 0f
                val endRadius = kotlin.math.sqrt(
                    (targetView.width * targetView.width + targetView.height * targetView.height).toDouble()
                ).toFloat()
                
                // Use ViewAnimationUtils for a true circular reveal
                val revealAnimator = android.view.ViewAnimationUtils.createCircularReveal(
                    targetView,
                    centerX.toInt(),
                    centerY.toInt(),
                    startRadius,
                    endRadius
                ).apply {
                    this.duration = durationMs
                    this.interpolator = easing
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            currentView.alpha = 1f
                            targetView.visibility = VISIBLE
                            targetView.alpha = 1f
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                }
                
                revealAnimator.start()
            }
            com.vincentwetzel.androidscreensaver.data.model.TransitionEffect.STAR -> {
                currentView.visibility = VISIBLE
                currentView.alpha = 1f
                targetView.visibility = VISIBLE
                targetView.alpha = 0f
                targetView.scaleX = 0.1f
                targetView.scaleY = 0.1f
                targetView.rotation = -30f
                val scaleX = ObjectAnimator.ofFloat(targetView, View.SCALE_X, 0.1f, 1f).apply {
                    this.duration = durationMs
                    interpolator = easing
                }
                val scaleY = ObjectAnimator.ofFloat(targetView, View.SCALE_Y, 0.1f, 1f).apply {
                    this.duration = durationMs
                    interpolator = easing
                }
                val rotate = ObjectAnimator.ofFloat(targetView, View.ROTATION, -30f, 0f).apply {
                    this.duration = durationMs
                    interpolator = easing
                }
                val fadeIn = ObjectAnimator.ofFloat(targetView, View.ALPHA, 0f, 1f).apply {
                    this.duration = durationMs
                    interpolator = easing
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            targetView.scaleX = 1f
                            targetView.scaleY = 1f
                            targetView.rotation = 0f
                            targetView.alpha = 1f
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                }
                scaleX.start()
                scaleY.start()
                rotate.start()
                fadeIn.start()
            }
            com.vincentwetzel.androidscreensaver.data.model.TransitionEffect.WIND -> {
                currentView.visibility = VISIBLE
                currentView.alpha = 1f
                targetView.visibility = VISIBLE
                targetView.alpha = 0f
                targetView.translationX = -viewWidth
                targetView.rotation = -5f
                val slideIn = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X, -viewWidth, 0f).apply {
                    this.duration = durationMs
                    interpolator = easing
                }
                val rotate = ObjectAnimator.ofFloat(targetView, View.ROTATION, -5f, 0f).apply {
                    this.duration = durationMs
                    interpolator = easing
                }
                val fadeIn = ObjectAnimator.ofFloat(targetView, View.ALPHA, 0f, 1f).apply {
                    this.duration = durationMs
                    interpolator = easing
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            targetView.rotation = 0f
                            targetView.translationX = 0f
                            targetView.alpha = 1f
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                }
                slideIn.start()
                rotate.start()
                fadeIn.start()
            }
            com.vincentwetzel.androidscreensaver.data.model.TransitionEffect.CIRCLE -> {
                currentView.visibility = VISIBLE
                currentView.alpha = 1f
                targetView.visibility = VISIBLE
                targetView.alpha = 1f
                targetView.scaleX = 0f
                targetView.scaleY = 0f
                val bounceInterpolator = android.view.animation.BounceInterpolator()
                val scaleX = ObjectAnimator.ofFloat(targetView, View.SCALE_X, 0f, 1f).apply {
                    this.duration = durationMs
                    interpolator = bounceInterpolator
                }
                val scaleY = ObjectAnimator.ofFloat(targetView, View.SCALE_Y, 0f, 1f).apply {
                    this.duration = durationMs
                    interpolator = bounceInterpolator
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            targetView.scaleX = 1f
                            targetView.scaleY = 1f
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                }
                scaleX.start()
                scaleY.start()
            }
            // CUBE, MEMORY, ILLUSION fall back to crossfade
            else -> {
                currentView.visibility = VISIBLE
                currentView.alpha = 1f
                targetView.visibility = VISIBLE
                targetView.alpha = 0f
                val fadeIn = ObjectAnimator.ofFloat(targetView, View.ALPHA, 0f, 1f).apply {
                    this.duration = durationMs
                    interpolator = easing
                }
                val fadeOut = ObjectAnimator.ofFloat(currentView, View.ALPHA, 1f, 0f).apply {
                    this.duration = durationMs
                    interpolator = easing
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentView.visibility = GONE
                            currentView.alpha = 1f
                            activeView = !activeView
                            startDisplayEffectAnimation(targetView, config)
                        }
                    })
                }
                fadeIn.start()
                fadeOut.start()
            }
        }
    }

    /**
     * Apply common decoration styling (position, font size, opacity, background)
     */
    private fun applyDecorationStyle(
        textView: TextView,
        position: ClockPosition,
        fontSizeSp: Float,
        opacity: Int,
        background: com.vincentwetzel.androidscreensaver.data.model.DecorationBackground
    ) {
        // Font size (direct SP value)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)

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

    private fun com.vincentwetzel.androidscreensaver.data.model.PhotoInfoBackground.toDecorationBackground():
        com.vincentwetzel.androidscreensaver.data.model.DecorationBackground {
        return when (this) {
            com.vincentwetzel.androidscreensaver.data.model.PhotoInfoBackground.NONE ->
                com.vincentwetzel.androidscreensaver.data.model.DecorationBackground.NONE
            com.vincentwetzel.androidscreensaver.data.model.PhotoInfoBackground.SOLID ->
                com.vincentwetzel.androidscreensaver.data.model.DecorationBackground.SOLID
            com.vincentwetzel.androidscreensaver.data.model.PhotoInfoBackground.SEMI_TRANSPARENT,
            com.vincentwetzel.androidscreensaver.data.model.PhotoInfoBackground.GRADIENT_FADE ->
                com.vincentwetzel.androidscreensaver.data.model.DecorationBackground.SEMI_TRANSPARENT
        }
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
     * Protects against OLED burn-in by slightly shifting overlay positions every minute.
     */
    private fun startBurnInProtection() {
        burnInProtectionJob?.cancel()
        burnInProtectionJob = slideshowScope.launch {
            while (true) {
                delay(60_000L) // Shift every 1 minute
                
                if (isPlaying) {
                    val maxShift = 24f // Maximum shift in pixels (~24px)
                    val shiftViews = listOf(dateTextView, clockTextView, weatherTextView, photoInfoTextView)
                    
                    for (view in shiftViews) {
                        if (view != null && view.visibility == VISIBLE) {
                            val shiftX = (-maxShift.toInt()..maxShift.toInt()).random().toFloat()
                            val shiftY = (-maxShift.toInt()..maxShift.toInt()).random().toFloat()
                            
                            view.animate()
                                .translationX(shiftX)
                                .translationY(shiftY)
                                .setDuration(3000L) // Slow 3-second glide
                                .setInterpolator(android.view.animation.LinearInterpolator())
                                .start()
                        }
                    }
                }
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
        burnInProtectionJob?.cancel()
        burnInProtectionJob = null
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
