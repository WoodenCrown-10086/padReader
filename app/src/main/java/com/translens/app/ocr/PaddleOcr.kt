package com.translens.app.ocr

import android.graphics.Bitmap

/**
 * PaddleOCR 纯开源本地实现（跨设备兜底，Phase 1+ 接入）。
 *
 * 不依赖任何厂商服务，适合无 GMS / 无 HMS 的其他安卓平板；
 * 代价是模型包体与 JNI 集成成本较高。
 */
class PaddleOcr : OcrEngine {
    override suspend fun recognize(bitmap: Bitmap): Result<List<OcrLine>> {
        return Result.failure(UnsupportedOperationException("PaddleOCR 尚未接入"))
    }
}
