package com.translens.app.overlay

/**
 * 悬浮球扇区菜单的动作项。
 *
 * 新增功能时：在这里加枚举 → 由 [SectorMenuView] 自动渲染 →
 * 在 [LensOverlayService.handleBallAction] 里处理，即可扩展菜单。
 */
enum class BallAction(val label: String) {
    SETTINGS("设置"),
    STOP("关闭"),

    // 预留：切换语言 / 截图翻译 / 朗读 / 复制 等后续功能在此扩展
    ;
}
