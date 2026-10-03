package com.translens.app.source

import android.graphics.Rect
import com.translens.app.core.model.TextSpan

/**
 * 取字源抽象：读取屏幕指定矩形（屏幕像素坐标）内的文本。
 *
 * 实现：
 * - [AccessibilityTextSource] 无障碍直读（首选，零 OCR）；
 * - [ScreenCaptureOcrSource] 截图 OCR 兜底（Phase 1）。
 *
 * 返回 [Result]，失败时由上层统一降级。
 */
interface TextSource {
    suspend fun read(box: Rect): Result<List<TextSpan>>
}
