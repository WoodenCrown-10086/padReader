package com.translens.app.pipeline

import com.translens.app.core.layout.RenderPlan

/**
 * 渲染结果消费者：由 [com.translens.app.overlay.LensOverlayService] 实现，
 * 把排版结果刷到透镜窗口，或在出错时降级处理。
 */
interface RenderSink {
    /** 渲染结果；null 表示框内无内容（仅显示边框）。 */
    fun onRender(plan: RenderPlan?)

    /** 取字 / 翻译失败时回调，用于降级与日志。 */
    fun onError(error: Throwable)
}
