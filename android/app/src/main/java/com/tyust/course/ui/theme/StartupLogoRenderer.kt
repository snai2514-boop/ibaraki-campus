package com.tyust.course.ui.theme

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import androidx.core.content.ContextCompat
import com.tyust.course.R

/** Keep the system splash and its exit animation on the same campus artwork. */
class StartupLogoRenderer(context: Context) {
    private val logo = requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_campus_mark)).mutate()
    fun draw(canvas: Canvas, bounds: Rect, milliseconds: Float, opacity: Float = 1f) {
        logo.bounds = bounds
        logo.alpha = ((1f - StartupChoreography.content(milliseconds)) * opacity * 255).toInt().coerceIn(0,255)
        logo.draw(canvas)
    }
}
