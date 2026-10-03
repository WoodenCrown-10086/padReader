# 项目混淆规则
# 接入 HMS ML Kit / PaddleOCR 后，按对应 SDK 文档追加 keep 规则。
# 无障碍服务类由系统反射调用，如遇问题可显式 keep：
# -keep class com.translens.app.source.AccessibilityTextSource { *; }
