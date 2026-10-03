package com.translens.app.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * 扇区菜单：围绕悬浮球展开一圈按钮（扇形，向左开口）。
 * 点击按钮执行对应动作，点击空白处关闭。
 *
 * 作为全屏透明悬浮窗使用，视图坐标系即屏幕坐标系。
 */
class SectorMenuView(context: Context) : View(context) {

    /** 悬浮球圆心（屏幕坐标）。 */
    var centerX = 0f
    var centerY = 0f

    var actions: List<BallAction> = emptyList()

    var onAction: ((BallAction) -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val buttonRadius = BUTTON_RADIUS_DP * density
    private val menuRadius = MENU_RADIUS_DP * density

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val buttonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = Color.parseColor("#3B82F6")
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3B82F6")
        textAlign = Paint.Align.CENTER
        textSize = 12f * density
    }

    override fun onDraw(canvas: Canvas) {
        if (actions.isEmpty()) return
        val centers = computeCenters()
        for (i in actions.indices) {
            val cx = centers[i][0]
            val cy = centers[i][1]
            canvas.drawCircle(cx, cy, buttonRadius, buttonPaint)
            canvas.drawCircle(cx, cy, buttonRadius, buttonBorderPaint)
            val baseline = cy - (labelPaint.ascent() + labelPaint.descent()) / 2f
            canvas.drawText(actions[i].label, cx, baseline, labelPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val index = hitTest(event.x, event.y)
            if (index >= 0) {
                performClick()
                onAction?.invoke(actions[index])
            } else {
                onDismiss?.invoke()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /** 计算各按钮圆心（屏幕坐标），围绕悬浮球展开成扇形（向左开口）。 */
    private fun computeCenters(): List<FloatArray> {
        val n = actions.size
        if (n == 0) return emptyList()
        val startAngle = 180.0 - FAN_HALF_DEG
        val endAngle = 180.0 + FAN_HALF_DEG
        val step = if (n > 1) (endAngle - startAngle) / (n - 1) else 0.0
        return List(n) { i ->
            val deg = if (n == 1) 180.0 else startAngle + step * i
            val rad = Math.toRadians(deg)
            floatArrayOf(
                centerX + menuRadius * cos(rad).toFloat(),
                centerY + menuRadius * sin(rad).toFloat(),
            )
        }
    }

    private fun hitTest(x: Float, y: Float): Int {
        val r2 = buttonRadius * buttonRadius
        computeCenters().forEachIndexed { index, c ->
            val dx = x - c[0]
            val dy = y - c[1]
            if (dx * dx + dy * dy <= r2) return index
        }
        return -1
    }

    companion object {
        private const val FAN_HALF_DEG = 70.0
        private const val BUTTON_RADIUS_DP = 21f
        private const val MENU_RADIUS_DP = 64f
    }
}
