package com.vincentwetzel.androidscreensaver.ui.slideshow

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.vincentwetzel.androidscreensaver.R

/**
 * View that displays a message when no photo sources are configured
 */
class NoSourcesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    init {
        setupView()
    }
    
    private fun setupView() {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.parseColor("#1a237e")) // Deep blue
        setPadding(48, 48, 48, 48)
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        
        val titleText = TextView(context).apply {
            text = context.getString(R.string.no_sources_title)
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        val messageText = TextView(context).apply {
            text = context.getString(R.string.no_sources_message)
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(48, 0, 48, 48)
        }
        
        val goButton = Button(context).apply {
            text = context.getString(R.string.go_to_settings)
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#3949ab")) // Lighter blue
            setPadding(48, 16, 48, 16)
            setOnClickListener {
                // Find the parent activity and finish it
                var parent = context
                while (parent is android.content.ContextWrapper) {
                    if (parent is android.app.Activity) {
                        parent.finish()
                        break
                    }
                    parent = parent.baseContext
                }
            }
        }
        
        addView(titleText)
        addView(messageText)
        addView(goButton)
    }
}
