package com.translens.app.core.translate

import com.translens.app.core.model.Lang

/**
 * 云端翻译骨架：适用于有道 / 百度 / DeepL 等 HTTP API。
 *
 * 不同提供方的鉴权方式、请求/响应结构差异较大，接入时按提供方文档实现网络请求。
 * 注意：云翻译会把原文发送到第三方，开启前必须在 UI 上向用户明示。
 */
class CloudTranslator(
    private val endpoint: String,
    private val apiKey: String,
) : Translator {
    override suspend fun translate(text: String, target: Lang): String {
        // TODO: 按提供方实现请求与响应解析
        throw UnsupportedOperationException("云端翻译尚未配置 provider")
    }
}
