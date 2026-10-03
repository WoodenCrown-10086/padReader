package com.translens.app.core.layout

import android.text.StaticLayout

/**
 * 一次渲染结果：背景色、内边距、以及按框宽重排好的译文布局。
 *
 * 由 [LayoutEngine] 产出、[com.translens.app.overlay.LensView] 消费，
 * 二者只通过该结构交互，保证渲染层与排版算法解耦。
 */
data class RenderPlan(
    val background: Int,
    val layout: StaticLayout,
    val paddingLeft: Float,
    val paddingTop: Float,
)
