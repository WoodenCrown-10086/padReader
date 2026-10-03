package com.translens.app.core.model

/**
 * 译文呈现方式。
 */
enum class RenderMode {
    /** 原位覆盖：译文盖在框内原文上（主模式）。 */
    IN_PLACE,

    /** 卡片式：译文浮在框旁，不覆盖原文（后续版本实现，需额外悬浮窗）。 */
    CARD,
}
