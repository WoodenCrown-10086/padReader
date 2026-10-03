package com.translens.app.core.translate

import com.translens.app.core.model.Lang

/**
 * 占位翻译器：Phase 0 未接入真实引擎时使用，原样返回原文。
 *
 * 仅用于跑通「取字 → 变化检测 → 翻译 → 排版 → 渲染」整条流水线。
 * 接入 HMS 后，在 [com.translens.app.TranslensApp] 的 AppContainer 中替换为
 * [HmsTranslator] 即可，其余代码无需改动。
 */
class NoopTranslator : Translator {
    override suspend fun translate(text: String, target: Lang): String = text
}
