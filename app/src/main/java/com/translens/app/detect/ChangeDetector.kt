package com.translens.app.detect

import android.graphics.Bitmap
import com.translens.app.core.model.TextSpan

/**
 * 内容变化检测：内容不变则不触发翻译，静止时 CPU≈0。
 *
 * - 文本路径：对拼接后的文本做哈希对比（无障碍取字）；
 * - 帧路径：8x8 灰度感知哈希（pHash）+ 汉明距离（截图 OCR 取字）。
 */
class ChangeDetector(private val hammingThreshold: Int = 8) {

    private var lastTextHash: Int? = null
    private var lastPhash: Long? = null

    /** 文本是否变化。返回 true 表示需要重新翻译。 */
    fun textChanged(spans: List<TextSpan>): Boolean {
        val text = spans.joinToString(separator = "\u0000") { it.text }
        val hash = text.hashCode()
        if (hash == lastTextHash) return false
        lastTextHash = hash
        return true
    }

    /** 帧是否变化（截图 OCR 路径）。 */
    fun frameChanged(frame: Bitmap): Boolean {
        val hash = pHash(frame)
        val last = lastPhash
        if (last != null && hamming(hash, last) <= hammingThreshold) return false
        lastPhash = hash
        return true
    }

    /** 清空历史状态（如重新启动透镜时）。 */
    fun reset() {
        lastTextHash = null
        lastPhash = null
    }

    /** 8x8 灰度感知哈希 → 64 位。 */
    private fun pHash(bitmap: Bitmap): Long {
        val small = Bitmap.createScaledBitmap(bitmap, 8, 8, false)
        val pixels = IntArray(64)
        small.getPixels(pixels, 0, 8, 0, 0, 8, 8)
        small.recycle()

        val grays = IntArray(64)
        var sum = 0L
        for (i in 0 until 64) {
            val c = pixels[i]
            val g = (((c shr 16) and 0xFF) * 30 + ((c shr 8) and 0xFF) * 59 + (c and 0xFF) * 11) / 100
            grays[i] = g
            sum += g
        }
        val avg = (sum / 64).toInt()

        var hash = 0L
        for (i in 0 until 64) {
            if (grays[i] >= avg) hash = hash or (1L shl i)
        }
        return hash
    }

    private fun hamming(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
}
