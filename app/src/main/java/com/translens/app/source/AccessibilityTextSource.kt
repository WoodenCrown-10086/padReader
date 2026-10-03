package com.translens.app.source

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.translens.app.core.model.TextSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

/**
 * 无障碍取字源：直读其他 App 暴露的文本节点，零 OCR、零截图。
 *
 * 本类由系统实例化（见 AndroidManifest 中的 service 声明），
 * 应用内通过 [get] 获取当前实例。
 */
class AccessibilityTextSource : AccessibilityService(), TextSource {

    private val _contentChanged = MutableSharedFlow<Unit>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * 内容变化事件：typeWindowContentChanged / viewScrolled / windowStateChanged 时发射。
     * 悬浮窗服务订阅它，用于事件驱动地触发重新取字（静止时零开销）。
     */
    val contentChanged: SharedFlow<Unit> = _contentChanged.asSharedFlow()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            -> _contentChanged.tryEmit(Unit)
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    /** 读取 box（屏幕坐标）内所有文本节点。节点树必须在主线程访问。 */
    override suspend fun read(box: Rect): Result<List<TextSpan>> =
        withContext(Dispatchers.Main) {
            runCatching {
                val root = rootInActiveWindow ?: return@runCatching emptyList()
                val spans = mutableListOf<TextSpan>()
                collect(root, box, spans)
                // 按阅读顺序排序（自上而下、同一行自左而右），保证翻译顺序稳定
                spans.sortWith(compareBy({ it.rect.top }, { it.rect.left }))
                spans
            }
        }

    private fun collect(node: AccessibilityNodeInfo, box: Rect, out: MutableList<TextSpan>) {
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty() && isTextLeaf(node)) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            if (overlapsSignificantly(bounds, box)) {
                out += TextSpan(text = text, rect = Rect(bounds))
            }
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collect(it, box, out) }
        }
    }

    /**
     * 是否为文本叶子节点：自身有文本且所有子节点都无文本。
     * 跳过"容器节点"——其文本常为整页聚合内容（如文章标题），并非用户框选的部分。
     */
    private fun isTextLeaf(node: AccessibilityNodeInfo): Boolean {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (!child.text.isNullOrEmpty()) return false
        }
        return true
    }

    /**
     * 要求节点与框"显著重叠"（重叠面积占节点面积 ≥ [MIN_OVERLAP_PERCENT]），
     * 而非简单相交，避免把仅边缘擦过、或整页级大容器误收入框选结果。
     */
    private fun overlapsSignificantly(bounds: Rect, box: Rect): Boolean {
        val iw = minOf(bounds.right, box.right) - maxOf(bounds.left, box.left)
        val ih = minOf(bounds.bottom, box.bottom) - maxOf(bounds.top, box.top)
        if (iw <= 0 || ih <= 0) return false
        val intersectArea = iw.toLong() * ih.toLong()
        val nodeArea = bounds.width().toLong() * bounds.height().toLong()
        if (nodeArea <= 0) return false
        return intersectArea * 100 >= nodeArea * MIN_OVERLAP_PERCENT
    }

    companion object {
        /** 节点与框的"显著重叠"阈值：重叠面积占节点面积的比例（百分比）。 */
        private const val MIN_OVERLAP_PERCENT = 50

        @Volatile
        private var instance: AccessibilityTextSource? = null

        /** 当前无障碍服务实例；用户未开启时为 null。 */
        fun get(): AccessibilityTextSource? = instance

        /** 无障碍服务是否已开启。 */
        fun isEnabled(): Boolean = instance != null
    }
}
