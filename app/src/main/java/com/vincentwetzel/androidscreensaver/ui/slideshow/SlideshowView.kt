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
    
    // Loading overlay
    private lateinit var loadingLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView
    
    // State
    private var photos: List<Photo> = emptyList()
    private var currentIndex = 0
    private var isPlaying = false
    private var slideJob: Job? = null
    
    // Callbacks
    var onSlideshowStarted: ((List<Photo>) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    init {
        setupViews()
    }
    
    private fun setupViews() {
        // Set dark background
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
        addView(loadingLayout)
    }
    
    /**
     * Initialize and start the slideshow
     * @param slideshowManager The slideshow manager instance
     */
    fun initialize(slideshowManager: SlideshowManager) {
        this.slideshowManager = slideshowManager
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
                    // No photos - caller should handle this case
                    withContext(Dispatchers.Main) {
                        onError?.invoke("No photos found in selected sources")
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
                
                android.util.Log.d("SlideshowView", "Started slideshow with ${photos.size} photos")
                
            } catch (e: Exception) {
                android.util.Log.e("SlideshowView", "Error loading photos", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke("Error loading photos: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Show a single photo with optional crossfade animation
     */
    private fun showPhoto(photo: Photo, animate: Boolean = true) {
        val targetView = if (activeView) imageViewB else imageViewA
        val currentView = if (activeView) imageViewA else imageViewB
        
        // Load the photo
        val uri = Uri.parse(photo.uri)
        targetView.load(uri) {
            crossfade(false) // We handle crossfade manually
            placeholder(android.R.color.black)
            error(android.R.color.black)
            allowHardware(false) // Better for long-lived views like screensavers
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
    
    /**
     * Start automatic photo advancement
     */
    private fun startAutoAdvance() {
        slideJob?.cancel()
        slideJob = slideshowScope.launch {
            val durationMs = slideshowManager.config.slideDurationSeconds * 1000L
            
            while (isPlaying && photos.isNotEmpty()) {
                delay(durationMs)
                
                if (!isPlaying) break
                
                currentIndex = (currentIndex + 1) % photos.size
                showPhoto(photos[currentIndex], animate = true)
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
    }
    
    /**
     * Resume the slideshow
     */
    fun resume() {
        if (photos.isNotEmpty() && !isPlaying) {
            isPlaying = true
            startAutoAdvance()
        }
    }
    
    /**
     * Stop the slideshow and clean up resources
     */
    fun stop() {
        pause()
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
    }
    
    companion object {
        private const val TAG = "SlideshowView"
    }
}
