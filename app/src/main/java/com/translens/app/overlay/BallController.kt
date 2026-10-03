package com.translens.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager

/**
 * 悬浮球 + 扇区菜单的窗口管理。
 *
 * - [show]/[hide] 控制悬浮球生命周期；
 * - 球停靠在屏幕右侧边缘，单击展开扇形菜单；
 * - 菜单动作通过构造参数 [actionHandler] 回调给宿主（服务）。
 */
class BallController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val actionHandler: (BallAction) -> Unit,
) {
    private var ballView: BallView? = null
    private var ballLp: WindowManager.LayoutParams? = null
    private var menuView: SectorMenuView? = null

    private val ballSize = (BALL_SIZE_DP * density).toInt()
    private val edgeMargin = (EDGE_MARGIN_DP * density).toInt()
    private var ballY = 0

    fun show() {
        if (ballView != null) return
        ballY = (screenHeight - ballSize) / 2
        Log.d(TAG, "show: size=$ballSize x=${ballX()} y=$ballY screen=${screenWidth}x${screenHeight}")

        val view = BallView(context).apply {
            onBallClick = { toggleMenu() }
            onBallDrag = { dy ->
                ballY = (ballY + dy.toInt()).coerceIn(0, (screenHeight - ballSize).coerceAtLeast(0))
                applyBallParams()
            }
        }
        val lp = WindowManager.LayoutParams(
            ballSize,
            ballSize,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = ballX()
            y = ballY
        }

        windowManager.addView(view, lp)
        ballView = view
        ballLp = lp
    }

    fun hide() {
        closeMenu()
        ballView?.let { windowManager.removeView(it) }
        ballView = null
        ballLp = null
    }

    private fun toggleMenu() {
        if (menuView != null) closeMenu() else openMenu()
    }

    private fun openMenu() {
        if (menuView != null) return
        val ball = ballView ?: return

        val view = SectorMenuView(context).apply {
            centerX = ballX() + ballSize / 2f
            centerY = ballY + ballSize / 2f
            actions = BallAction.entries
            onAction = { selected ->
                closeMenu()
                actionHandler(selected)
            }
            onDismiss = { closeMenu() }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        windowManager.addView(view, lp)
        menuView = view
    }

    private fun closeMenu() {
        menuView?.let { windowManager.removeView(it) }
        menuView = null
    }

    private fun ballX(): Int = screenWidth - ballSize - edgeMargin

    private fun applyBallParams() {
        val view = ballView ?: return
        val lp = ballLp ?: return
        lp.x = ballX()
        lp.y = ballY
        windowManager.updateViewLayout(view, lp)
    }

    private val density: Float get() = context.resources.displayMetrics.density
    private val screenWidth: Int get() = context.resources.displayMetrics.widthPixels
    private val screenHeight: Int get() = context.resources.displayMetrics.heightPixels

    private val overlayType: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

    companion object {
        private const val TAG = "Translens"
        private const val BALL_SIZE_DP = 48f
        private const val EDGE_MARGIN_DP = 8f
    }
}
