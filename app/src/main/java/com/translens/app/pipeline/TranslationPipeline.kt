package com.translens.app.pipeline

import android.graphics.Rect
import com.translens.app.cache.TranslationCache
import com.translens.app.core.layout.LayoutEngine
import com.translens.app.core.model.Lang
import com.translens.app.core.translate.Translator
import com.translens.app.detect.ChangeDetector
import com.translens.app.source.TextSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 流水线编排中心：取字 → 变化检测 → 翻译（含缓存）→ 排版 → 交给 [RenderSink]。
 *
 * 设计要点：
 * - 与具体取字/翻译实现解耦，便于测试与替换；
 * - 用 [Mutex] 串行化刷新，避免并发导致渲染乱序；
 * - 内容无变化时直接跳过，静止时零开销。
 */
class TranslationPipeline(
    private val sourceProvider: () -> TextSource,
    private val translator: Translator,
    private val layoutEngine: LayoutEngine,
    private val cache: TranslationCache,
    private val targetLang: () -> Lang,
    private val detector: ChangeDetector,
    private val sink: RenderSink,
) {
    private val mutex = Mutex()

    /** 读取 box 内文本；若内容有变化则翻译并渲染。 */
    suspend fun refresh(box: Rect, boxWidthPx: Int, boxHeightPx: Int) {
        mutex.withLock {
            val result = sourceProvider().read(box)
            if (result.isFailure) {
                sink.onError(result.exceptionOrNull() ?: IllegalStateException("取字失败"))
                return@withLock
            }

            val spans = result.getOrNull().orEmpty()
            val text = spans.joinToString(separator = " ") { it.text }
            if (text.isBlank()) {
                sink.onRender(null)
                return@withLock
            }
            if (!detector.textChanged(spans)) return@withLock // 内容未变化，跳过

            val lang = targetLang()
            val key = TranslationCache.key(text, lang)
            val translated = cache.get(key) ?: translate(text, lang, key) ?: return@withLock

            val plan = layoutEngine.build(
                text = translated,
                boxWidthPx = boxWidthPx,
                boxHeightPx = boxHeightPx,
                textSizePx = DEFAULT_TEXT_SIZE_PX, // TODO(Phase 2): 按原文字号估算
            )
            sink.onRender(plan)
        }
    }

    /** 翻译并写入缓存；失败时回调 [RenderSink.onError] 并返回 null（取消异常则原样抛出）。 */
    private suspend fun translate(text: String, lang: Lang, key: String): String? {
        return try {
            translator.translate(text, lang).also { cache.put(key, it) }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            sink.onError(t)
            null
        }
    }

    companion object {
        private const val DEFAULT_TEXT_SIZE_PX = 16f
    }
}
