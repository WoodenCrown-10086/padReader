package com.translens.app.source

import android.graphics.Rect
import com.translens.app.core.model.TextSpan
import com.translens.app.ocr.OcrEngine

/**
 * 截图 OCR 兜底取字源（Phase 1 实现）。
 *
 * 仅在无障碍取不到文本时启用；只裁取透镜框对应区域、限制分辨率，用完即弃。
 * 截图仅作为 OCR 输入，不显示、不落盘。
 *
 * 实现要点：
 * - MediaProjectionManager.createScreenCaptureIntent() 授权；
 * - MediaProjection.createVirtualDisplay() + ImageReader 取帧；
 * - 按 box 裁剪 + 限制最长边（如 ≤ 1280px）→ [OcrEngine]；
 * - Android 14+ 需对应前台服务声明 foregroundServiceType="mediaProjection"。
 */
class ScreenCaptureOcrSource(
    private val ocr: OcrEngine,
) : TextSource {
    override suspend fun read(box: Rect): Result<List<TextSpan>> {
        return Result.failure(UnsupportedOperationException("截图 OCR 兜底将在 Phase 1 实现"))
    }
}
