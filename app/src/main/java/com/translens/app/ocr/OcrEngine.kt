package com.translens.app.ocr

import android.graphics.Bitmap
import android.graphics.Rect

/** 一行 OCR 识别结果：文本及其在输入位图中的坐标。 */
data class OcrLine(
    val text: String,
    val rect: Rect,
)

/**
 * OCR 引擎抽象。
 *
 * 实现：
 * - [HmsOcr]（华为 HMS ML Kit，本机首选）；
 * - [PaddleOcr]（纯开源本地，跨设备兜底）。
 */
interface OcrEngine {
    suspend fun recognize(bitmap: Bitmap): Result<List<OcrLine>>
}
