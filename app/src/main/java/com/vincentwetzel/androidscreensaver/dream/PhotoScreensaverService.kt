package com.vincentwetzel.androidscreensaver.dream

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import coil.load
import coil.request.ImageRequest
import coil.size.Size
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.DisplayEffect
import com.vincentwetzel.androidscreensaver.data.model.Photo
import com.vincentwetzel.androidscreensaver.data.model.TransitionEffect
import com.vincentwetzel.androidscreensaver.data.model.TransitionEasing
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * DreamService that displays a photo slideshow as a screensaver
 */
@AndroidEntryPoint
class PhotoScreensaverService : DreamService() {

    @Inject
    lateinit var slideshowManager: SlideshowManager

    private var imageView: ImageView? = null
    private var serviceScope: CoroutineScope? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Configure screensaver behavior
        isInteractive = false
        isScreenBright = false

        // Set up the view
        setupImageView()

        // Start slideshow
        startSlideshow()
    }

    private fun setupImageView() {
        imageView = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF000000.toInt())
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        setContentView(imageView)
    }

    private fun startSlideshow() {
        serviceScope?.launch {
            try {
                // Load photos from all enabled sources
                val photos = slideshowManager.loadPhotos()

                if (photos.isEmpty()) {
                    // Show placeholder
                    mainHandler.post {
                        imageView?.setImageResource(R.drawable.ic_no_photos)
                    }
                    return@launch
                }

                // Shuffle if configured
                val displayPhotos = if (slideshowManager.config.shuffle) {
                    photos.shuffled()
                } else {
                    photos.sortedBy { slideshowManager.getSortKey(it) }
                }

                // Display photos in loop
                var currentIndex = 0
                while (isActive) {
                    val photo = displayPhotos[currentIndex % displayPhotos.size]
                    displayPhoto(photo)

                    // Wait for configured duration
                    delay(slideshowManager.config.slideDurationSeconds * 1000L)

                    // Preload next photo
                    val nextIndex = (currentIndex + 1) % displayPhotos.size
                    slideshowManager.preloadPhoto(displayPhotos[nextIndex])

                    currentIndex = (currentIndex + 1) % displayPhotos.size
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun displayPhoto(photo: Photo) {
        withContext(Dispatchers.Main) {
            val view = imageView ?: return@withContext

            // Apply transition effect
            val config = slideshowManager.config
            
            when (config.transitionEffect) {
                TransitionEffect.FADE -> applyFadeTransition(view, photo)
                TransitionEffect.CROSS_FADE -> applyCrossFadeTransition(view, photo)
                TransitionEffect.SLIDE -> applySlideTransition(view, photo)
                else -> applyBasicTransition(view, photo)
            }
        }
    }

    private fun applyFadeTransition(view: ImageView, photo: Photo) {
        // Fade out
        ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).apply {
            duration = slideshowManager.config.transitionDurationMs.toLong()
            interpolator = getEasingInterpolator(slideshowManager.config.transitionEasing)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Load new photo
                    loadImage(view, photo)
                    
                    // Fade in
                    ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
                        duration = slideshowManager.config.transitionDurationMs.toLong()
                        interpolator = getEasingInterpolator(slideshowManager.config.transitionEasing)
                        start()
                    }
                }
            })
            start()
        }
    }

    private fun applyCrossFadeTransition(view: ImageView, photo: Photo) {
        // Create overlay for crossfade
        val overlay = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            alpha = 0f
            scaleType = view.scaleType
        }

        (view.parent as? ViewGroup)?.addView(overlay)

        // Load new photo on overlay
        loadImage(overlay, photo)

        // Crossfade
        ObjectAnimator.ofFloat(overlay, View.ALPHA, 0f, 1f).apply {
            duration = slideshowManager.config.transitionDurationMs.toLong()
            interpolator = getEasingInterpolator(slideshowManager.config.transitionEasing)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Update main view
                    view.setImageDrawable(overlay.drawable)
                    view.alpha = 1f
                    (overlay.parent as? ViewGroup)?.removeView(overlay)
                }
            })
            start()
        }
    }

    private fun applySlideTransition(view: ImageView, photo: Photo) {
        val direction = when (slideshowManager.config.transitionDirection) {
            com.vincentwetzel.androidscreensaver.data.model.TransitionDirection.LEFT -> -view.width.toFloat()
            com.vincentwetzel.androidscreensaver.data.model.TransitionDirection.RIGHT -> view.width.toFloat()
            com.vincentwetzel.androidscreensaver.data.model.TransitionDirection.UP -> -view.height.toFloat()
            com.vincentwetzel.androidscreensaver.data.model.TransitionDirection.DOWN -> view.height.toFloat()
            else -> view.width.toFloat() // Default: from right
        }

        // Create overlay for slide
        val overlay = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            translationX = direction
            scaleType = view.scaleType
        }

        (view.parent as? ViewGroup)?.addView(overlay)

        // Load new photo on overlay
        loadImage(overlay, photo)

        // Slide in
        ObjectAnimator.ofFloat(overlay, View.TRANSLATION_X, direction, 0f).apply {
            duration = slideshowManager.config.transitionDurationMs.toLong()
            interpolator = getEasingInterpolator(slideshowManager.config.transitionEasing)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.setImageDrawable(overlay.drawable)
                    (overlay.parent as? ViewGroup)?.removeView(overlay)
                }
            })
            start()
        }
    }

    private fun applyBasicTransition(view: ImageView, photo: Photo) {
        loadImage(view, photo)
    }

    private fun loadImage(view: ImageView, photo: Photo) {
        val config = slideshowManager.config

        // Apply display effect
        when (config.displayEffect) {
            DisplayEffect.CROP_TO_FIT -> view.scaleType = ImageView.ScaleType.CENTER_CROP
            DisplayEffect.SCALE_TO_FIT -> view.scaleType = ImageView.ScaleType.FIT_CENTER
            DisplayEffect.ZOOM -> view.scaleType = ImageView.ScaleType.CENTER_CROP
            DisplayEffect.PAN -> view.scaleType = ImageView.ScaleType.CENTER_CROP
            DisplayEffect.FOCUS -> view.scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // Load image with Coil
        view.load(photo.uri) {
            crossfade(false)
            size(Size.ORIGINAL)
            placeholder(R.drawable.ic_no_photos)
            error(R.drawable.ic_no_photos)
            listener(
                onSuccess = { _, _ ->
                    // Apply display effect animations
                    applyDisplayEffect(view, config.displayEffect)
                }
            )
        }
    }

    private fun applyDisplayEffect(view: ImageView, effect: DisplayEffect) {
        when (effect) {
            DisplayEffect.ZOOM -> {
                // Zoom in effect
                view.scaleX = 1.0f
                view.scaleY = 1.0f
                ObjectAnimator.ofPropertyValuesHolder(
                    view,
                    android.util.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.2f),
                    android.util.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.2f)
                ).apply {
                    duration = slideshowManager.config.slideDurationSeconds * 1000L
                    interpolator = DecelerateInterpolator()
                    start()
                }
            }
            DisplayEffect.PAN -> {
                // Pan effect
                view.translationX = 0f
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, 50f).apply {
                    duration = slideshowManager.config.slideDurationSeconds * 1000L
                    interpolator = DecelerateInterpolator()
                    start()
                }
            }
            DisplayEffect.FOCUS -> {
                // Focus (blur to sharp) - would need RenderScript or similar
                // For now, just fade in
                view.alpha = 0.5f
                ObjectAnimator.ofFloat(view, View.ALPHA, 0.5f, 1f).apply {
                    duration = 1000
                    start()
                }
            }
            DisplayEffect.CROP_TO_FIT, DisplayEffect.SCALE_TO_FIT -> {
                // No animation needed
            }
        }
    }

    private fun getEasingInterpolator(easing: TransitionEasing): android.animation.TimeInterpolator {
        return when (easing) {
            TransitionEasing.LINEAR -> android.animation.LinearInterpolator()
            TransitionEasing.EASE_IN -> android.view.animation.AccelerateInterpolator()
            TransitionEasing.EASE_OUT -> android.view.animation.DecelerateInterpolator()
            TransitionEasing.EASE_IN_OUT -> AccelerateDecelerateInterpolator()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        serviceScope?.cancel()
        imageView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope?.cancel()
        serviceScope = null
    }

    companion object {
        private const val TAG = "PhotoScreensaverService"
    }
}
