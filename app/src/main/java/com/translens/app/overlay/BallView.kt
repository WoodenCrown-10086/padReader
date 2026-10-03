package com.translens.app.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * 悬浮球视图：停靠在屏幕右侧边缘的小圆球。
 *
 * - 单击 → 打开扇区菜单；
 * - 上下拖动 → 沿右边缘移动（横向位移忽略，球始终贴右边缘）。
 */
class BallView(context: Context) : View(context) {

    var onBallClick: (() -> Unit)? = null

    /** 拖动回调：dy 为垂直位移（像素）。 */
    var onBallDrag: ((dy: Float) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#E63B82F6")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 16f * density
    }

    private var lastRawY = 0f
    private var dragging = false

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, width / 2f, fillPaint)
        val baseline = cy - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText("译", cx, baseline, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastRawY = event.rawY
                dragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = event.rawY - lastRawY
                if (!dragging && abs(dy) > touchSlop) dragging = true
                if (dragging) {
                    onBallDrag?.invoke(dy)
                    lastRawY = event.rawY
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val wasDragging = dragging
                dragging = false
                performClick()
                if (!wasDragging) onBallClick?.invoke()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
