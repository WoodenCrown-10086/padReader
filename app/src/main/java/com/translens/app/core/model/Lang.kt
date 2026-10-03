package com.translens.app.core.model

/**
 * 目标语言（ISO 639-1 代码）。源语言默认自动检测，不在此枚举中表达。
 */
enum class Lang(val code: String, val displayName: String) {
    EN("en", "英语"),
    ZH("zh", "中文"),
    JA("ja", "日语"),
    KO("ko", "韩语"),
    FR("fr", "法语"),
    DE("de", "德语"),
    RU("ru", "俄语");

    companion object {
        fun fromCode(code: String?): Lang? = entries.firstOrNull { it.code == code }
    }
}
