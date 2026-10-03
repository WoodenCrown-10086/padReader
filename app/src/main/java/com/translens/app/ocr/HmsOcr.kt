package com.translens.app.ocr

import android.graphics.Bitmap

/**
 * HMS ML Kit 文本识别实现（Phase 1 接入，步骤见 README「接入 HMS ML Kit」）。
 */
class HmsOcr : OcrEngine {
    override suspend fun recognize(bitmap: Bitmap): Result<List<OcrLine>> {
        return Result.failure(UnsupportedOperationException("HMS OCR 尚未接入"))
    }
}
