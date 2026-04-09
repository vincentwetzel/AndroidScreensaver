package com.vincentwetzel.androidscreensaver.ui.slideshow

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.AttributeSet
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
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.dream.SlideshowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // State
    private var photos: List<Photo> = emptyList()
    private var currentIndex = 0
    private var isPlaying = false
    private var slideJob: Job? = null
    private var isAdvancing = false // Prevent double-advance (video end + auto-advance racing)
    private var videoDurationJob: Job? = null // Cancelable job for video duration timers

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

            // Apply audio mode and volume
            when (config.videoAudioMode) {
                com.vincentwetzel.androidscreensaver.data.model.VideoAudioMode.MUTE ->
                    videoPlayer?.volume = 0f
                com.vincentwetzel.androidscreensaver.data.model.VideoAudioMode.SYSTEM_VOLUME ->
                    videoPlayer?.volume = 1f // System volume controls the output
                com.vincentwetzel.androidscreensaver.data.model.VideoAudioMode.CUSTOM_VOLUME ->
                    videoPlayer?.volume = config.videoCustomVolume / 100f
            }

            // Apply loop setting for short videos
            videoPlayer?.repeatMode = if (config.videoLoopShort) {
                Player.REPEAT_MODE_ONE
            } else {
                Player.REPEAT_MODE_OFF
            }

            // Apply auto play setting
            videoPlayer?.playWhenReady = config.videoAutoPlay

            // Handle EXTRACT_STILL: seek to timestamp and pause
            if (config.videoDisplayMode == com.vincentwetzel.androidscreensaver.data.model.VideoDisplayMode.EXTRACT_STILL) {
                val timestampMs = when (config.videoStillTimestamp) {
                    com.vincentwetzel.androidscreensaver.data.model.VideoStillTimestamp.BEGINNING -> 0L
                    com.vincentwetzel.androidscreensaver.data.model.VideoStillTimestamp.MIDDLE -> {
                        // Estimate middle — will be refined once we know duration
                        0L
                    }
                    com.vincentwetzel.androidscreensaver.data.model.VideoStillTimestamp.END -> {
                        // Seek near end — use a default near-max value
                        300_000L
                    }
                    com.vincentwetzel.androidscreensaver.data.model.VideoStillTimestamp.CUSTOM -> 0L
                }
                videoPlayer?.seekTo(timestampMs)
                videoPlayer?.playWhenReady = false // Pause for still
                isPlayingVideo = false // Treat as still for advance purposes
            }

            // Handle PLAY_FIXED: set a timer to stop after fixed seconds
            videoDurationJob?.cancel()
            if (config.videoDisplayMode == com.vincentwetzel.androidscreensaver.data.model.VideoDisplayMode.PLAY_FIXED) {
                val fixedMs = config.videoFixedPlaySeconds * 1000L
                videoDurationJob = slideshowScope.launch {
                    delay(fixedMs)
                    if (isPlayingVideo) {
                        android.util.Log.d(TAG, "Fixed duration reached ($fixedMs ms), advancing")
                        advanceToNext()
                    }
                }
            }

            // Handle videoMaxDurationSeconds: set a hard cap
            if (config.videoMaxDurationSeconds > 0 && config.videoDisplayMode != com.vincentwetzel.androidscreensaver.data.model.VideoDisplayMode.PLAY_FIXED) {
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
     * Stop the video player and cancel any pending duration timers
     */
    private fun stopVideoPlayer() {
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
