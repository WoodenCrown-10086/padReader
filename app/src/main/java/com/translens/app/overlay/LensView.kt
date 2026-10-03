package com.translens.app.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.translens.app.core.layout.RenderPlan

/**
 * 透镜窗口视图：窗口本身即「框」。
 *
 * 负责绘制「背景 + 译文 + 1px 边框」，并把拖动/缩放手势回调给服务层。
 * 框外没有窗口，因此屏幕原样、触摸自然穿透，零额外 surface。
 */
class LensView(context: Context) : View(context) {

    /** 当前渲染结果；null 表示框内无内容（仅显示边框）。 */
    var renderPlan: RenderPlan? = null
        set(value) {
            field = value
            invalidate()
        }

    /** 拖动回调：dx/dy 为本次手势位移（像素）。 */
    var onDrag: ((dx: Float, dy: Float) -> Unit)? = null

    /** 缩放回调：dl/dt/dr/db 为左/上/右/下四边的增量（像素）。 */
    var onResize: ((dl: Float, dt: Float, dr: Float, db: Float) -> Unit)? = null

    /** 手势结束回调：服务层在此触发重新取字（松手才翻译）。 */
    var onGestureEnd: (() -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val edgeSlop = EDGE_SLOP_DP * density

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = BORDER_COLOR
    }

    private val bgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#F2FFFFFF")
    }

    private enum class DragMode { NONE, MOVE, RESIZE }

    private var mode = DragMode.NONE
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var resizeEdges = 0

    private val borderRect = RectF()

    override fun onDraw(canvas: Canvas) {
        renderPlan?.let { plan ->
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            canvas.save()
            canvas.translate(plan.paddingLeft, plan.paddingTop)
            plan.layout.draw(canvas)
            canvas.restore()
        }
        // 1px 边框（内缩半个描边宽度，确保完整可见）
        val inset = borderPaint.strokeWidth / 2f
        borderRect.set(inset, inset, width - inset, height - inset)
        canvas.drawRect(borderRect, borderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX
                lastRawY = event.rawY
                resizeEdges = hitEdges(event.x, event.y)
                mode = if (resizeEdges != 0) DragMode.RESIZE else DragMode.MOVE
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastRawX
                val dy = event.rawY - lastRawY
                lastRawX = event.rawX
                lastRawY = event.rawY
                when (mode) {
                    DragMode.MOVE -> onDrag?.invoke(dx, dy)
                    DragMode.RESIZE -> {
                        val dl = if (resizeEdges and EDGE_LEFT != 0) dx else 0f
                        val dt = if (resizeEdges and EDGE_TOP != 0) dy else 0f
                        val dr = if (resizeEdges and EDGE_RIGHT != 0) dx else 0f
                        val db = if (resizeEdges and EDGE_BOTTOM != 0) dy else 0f
                        onResize?.invoke(dl, dt, dr, db)
                    }

                    DragMode.NONE -> Unit
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasGesture = mode != DragMode.NONE
                mode = DragMode.NONE
                if (wasGesture) {
                    performClick()
                    onGestureEnd?.invoke()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /** 命中检测：触点是否落在边缘缩放手柄热区内。 */
    private fun hitEdges(x: Float, y: Float): Int {
        var edges = 0
        if (x <= edgeSlop) edges = edges or EDGE_LEFT
        if (y <= edgeSlop) edges = edges or EDGE_TOP
        if (x >= width - edgeSlop) edges = edges or EDGE_RIGHT
        if (y >= height - edgeSlop) edges = edges or EDGE_BOTTOM
        return edges
    }

    companion object {
        private const val EDGE_SLOP_DP = 24f
        private const val EDGE_LEFT = 1
        private const val EDGE_TOP = 2
        private const val EDGE_RIGHT = 4
        private const val EDGE_BOTTOM = 8
        private val BORDER_COLOR: Int = Color.parseColor("#FF3B82F6")
    }
}
