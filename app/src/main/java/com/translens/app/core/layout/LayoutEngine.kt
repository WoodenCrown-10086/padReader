package com.translens.app.core.layout

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

/**
 * 排版引擎：把译文按透镜框宽度重排，产出可绘制的 [RenderPlan]。
 *
 * 只依赖 Android 文本排版 API，不依赖任何厂商服务，便于未来移植到其他平台
 * （如 HarmonyOS NEXT 的 ArkTS 排版）。
 */
class LayoutEngine {

    /**
     * @param textSizePx  字号（像素）。Phase 2 将改为根据原文字号估算。
     * @param background  覆盖原文的背景色。
     * @param paddingPx   文字内边距（像素）。
     */
    fun build(
        text: String,
        boxWidthPx: Int,
        boxHeightPx: Int,
        textSizePx: Float,
        textColor: Int = Color.BLACK,
        background: Int = DEFAULT_BACKGROUND,
        paddingPx: Float = DEFAULT_PADDING_PX,
    ): RenderPlan {
        val paint = TextPaint().apply {
            this.textSize = textSizePx
            this.color = textColor
            isAntiAlias = true
        }

        val maxWidth = (boxWidthPx - paddingPx * 2).toInt().coerceAtLeast(1)
        val lineHeight = textSizePx * LINE_HEIGHT_FACTOR
        val maxLines = if (lineHeight > 0) {
            (boxHeightPx / lineHeight).toInt().coerceAtLeast(1)
        } else {
            1
        }

        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(maxLines)
            .build()

        return RenderPlan(
            background = background,
            layout = layout,
            paddingLeft = paddingPx,
            paddingTop = paddingPx,
        )
    }

    /**
     * Phase 2：从原文区域采样估算背景色（用于原位覆盖的"盖原文"）。
     * 骨架阶段先返回半透明白色；后续用 OCR 截图区域做颜色统计。
     */
    fun estimateBackgroundColor(bitmap: Bitmap?, rect: Rect): Int = DEFAULT_BACKGROUND

    companion object {
        private const val LINE_HEIGHT_FACTOR = 1.4f
        private const val DEFAULT_PADDING_PX = 8f
        private val DEFAULT_BACKGROUND: Int = Color.parseColor("#F2FFFFFF")
    }
}
