package com.translens.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.translens.app.R
import com.translens.app.TranslensApp
import com.translens.app.core.layout.RenderPlan
import com.translens.app.core.model.Lang
import com.translens.app.core.model.TextSpan
import com.translens.app.detect.ChangeDetector
import com.translens.app.pipeline.RenderSink
import com.translens.app.pipeline.TranslationPipeline
import com.translens.app.source.AccessibilityTextSource
import com.translens.app.source.TextSource
import com.translens.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 透镜前台服务：持有透镜窗口，编排「取字 → 翻译 → 渲染」流水线。
 *
 * 前台服务用于保活；悬浮窗窗口本身即框（见 [LensView]）。
 */
class LensOverlayService : Service(), RenderSink {

    private lateinit var windowManager: WindowManager
    private var lensView: LensView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var ballController: BallController? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pipeline: TranslationPipeline? = null
    private var refreshJob: Job? = null

    @Volatile
    private var currentLang = Lang.EN

    // 当前框的屏幕坐标（像素）
    private var boxLeft = DEFAULT_BOX_LEFT
    private var boxTop = DEFAULT_BOX_TOP
    private var boxWidth = 0
    private var boxHeight = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startAsForeground()
        setupLens()
        setupPipeline()
        setupBall()
        Log.d(TAG, "onCreate done")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_MIN,
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.translens.app.ui.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("框选区域即可翻译")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupLens() {
        val density = resources.displayMetrics.density
        boxWidth = (DEFAULT_BOX_WIDTH_DP * density).toInt()
        boxHeight = (DEFAULT_BOX_HEIGHT_DP * density).toInt()

        val view = LensView(this).apply {
            onDrag = { dx, dy ->
                boxLeft = (boxLeft + dx.toInt()).coerceIn(0, (screenWidth - boxWidth).coerceAtLeast(0))
                boxTop = (boxTop + dy.toInt()).coerceIn(0, (screenHeight - boxHeight).coerceAtLeast(0))
                applyLayoutParams()
            }
            onResize = { dl, dt, dr, db ->
                val newLeft = (boxLeft + dl.toInt()).coerceAtLeast(0)
                val newTop = (boxTop + dt.toInt()).coerceAtLeast(0)
                val newRight = (boxLeft + boxWidth + dr.toInt()).coerceAtMost(screenWidth)
                val newBottom = (boxTop + boxHeight + db.toInt()).coerceAtMost(screenHeight)
                val minW = (MIN_BOX_WIDTH_DP * density).toInt()
                val minH = (MIN_BOX_HEIGHT_DP * density).toInt()
                if (newRight - newLeft >= minW) {
                    boxLeft = newLeft
                    boxWidth = newRight - newLeft
                }
                if (newBottom - newTop >= minH) {
                    boxTop = newTop
                    boxHeight = newBottom - newTop
                }
                applyLayoutParams()
            }
            onGestureEnd = { scheduleRefresh() }
        }

        val lp = WindowManager.LayoutParams(
            boxWidth,
            boxHeight,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = boxLeft
            y = boxTop
        }

        windowManager.addView(view, lp)
        lensView = view
        layoutParams = lp
    }

    private fun setupPipeline() {
        val container = (application as TranslensApp).container

        // 订阅目标语言变化
        scope.launch {
            container.settings.targetLang.collect { currentLang = it }
        }

        pipeline = TranslationPipeline(
            // 动态解析取字源：无障碍可用则用无障碍，否则降级为空实现（Phase 1 换 OCR 兜底）
            sourceProvider = { AccessibilityTextSource.get() ?: EmptyTextSource },
            translator = container.translator,
            layoutEngine = container.layoutEngine,
            cache = container.cache,
            targetLang = { currentLang },
            detector = ChangeDetector(),
            sink = this,
        )

        // 无障碍内容变化 → 防抖后触发刷新
        AccessibilityTextSource.get()?.contentChanged
            ?.debounce(DEBOUNCE_MS)
            ?.onEach { scheduleRefresh() }
            ?.launchIn(scope)

        scheduleRefresh()
    }

    private fun setupBall() {
        Log.d(TAG, "setupBall start")
        ballController = BallController(this, windowManager) { action -> handleBallAction(action) }
        ballController?.show()
        Log.d(TAG, "setupBall done")
    }

    private fun handleBallAction(action: BallAction) {
        when (action) {
            BallAction.SETTINGS -> {
                val intent = Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

            BallAction.STOP -> stopSelf()
        }
    }

    private fun applyLayoutParams() {
        val view = lensView ?: return
        val lp = layoutParams ?: return
        lp.x = boxLeft
        lp.y = boxTop
        lp.width = boxWidth
        lp.height = boxHeight
        windowManager.updateViewLayout(view, lp)
    }

    private val screenWidth: Int get() = resources.displayMetrics.widthPixels
    private val screenHeight: Int get() = resources.displayMetrics.heightPixels

    private fun currentBox(): Rect = Rect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight)

    /** 防抖调度刷新：拖动过程中多次触发只会执行最后一次。 */
    private fun scheduleRefresh() {
        refreshJob?.cancel()
        val p = pipeline ?: return
        refreshJob = scope.launch {
            delay(REFRESH_DELAY_MS)
            p.refresh(currentBox(), boxWidth, boxHeight)
        }
    }

    // ---- RenderSink ----

    override fun onRender(plan: RenderPlan?) {
        lensView?.renderPlan = plan
    }

    override fun onError(error: Throwable) {
        // 骨架阶段：静默降级（不翻译、不弹窗），避免反复打扰。
        // 后续接入统一日志（如 Timber）与可选错误提示。
    }

    override fun onDestroy() {
        scope.cancel()
        ballController?.hide()
        ballController = null
        lensView?.let { windowManager.removeView(it) }
        lensView = null
        layoutParams = null
        super.onDestroy()
    }

    private val overlayType: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

    companion object {
        private const val TAG = "Translens"
        private const val CHANNEL_ID = "lens_overlay"
        private const val NOTIFICATION_ID = 1
        private const val DEBOUNCE_MS = 300L
        private const val REFRESH_DELAY_MS = 200L
        private const val DEFAULT_BOX_LEFT = 100
        private const val DEFAULT_BOX_TOP = 200
        private const val DEFAULT_BOX_WIDTH_DP = 220f
        private const val DEFAULT_BOX_HEIGHT_DP = 140f
        private const val MIN_BOX_WIDTH_DP = 80f
        private const val MIN_BOX_HEIGHT_DP = 48f

        fun start(context: Context) {
            val intent = Intent(context, LensOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LensOverlayService::class.java))
        }
    }
}

/** 无障碍未开启时的空取字源：返回空结果，透镜只显示边框。 */
private object EmptyTextSource : TextSource {
    override suspend fun read(box: Rect): Result<List<TextSpan>> = Result.success(emptyList())
}
