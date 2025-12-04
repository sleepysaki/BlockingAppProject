package com.example.appblocking

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class BoxOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var faceRect: Rect? = null
    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    fun setFaceRect(rect: Rect?) {
        this.faceRect = rect
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        faceRect?.let { rect ->
            canvas.drawRect(rect, paint)
        }
    }
}