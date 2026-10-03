package com.translens.app.core.translate

import com.translens.app.core.model.Lang

/**
 * 翻译引擎抽象。
 *
 * 实现约定：
 * - 可在后台协程调用（[translate] 为 suspend）；
 * - 并发安全（同一实例可能被多线程调用）；
 * - 失败时抛出异常，由上层（TranslationPipeline）统一捕获并降级处理。
 */
interface Translator {
    suspend fun translate(text: String, target: Lang): String
}
