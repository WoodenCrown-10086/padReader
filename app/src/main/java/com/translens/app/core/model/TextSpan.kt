package com.translens.app.core.model

import android.graphics.Rect

/**
 * 一段原文及其屏幕坐标（像素，基于整个屏幕坐标系）。
 *
 * 所有取字源（无障碍 / OCR）统一产出该结构，下游翻译与排版只依赖它，
 * 因此与具体取字方式解耦。
 */
data class TextSpan(
    val text: String,
    val rect: Rect,
)
