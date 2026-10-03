// 顶层构建文件：仅声明插件版本，不在此处写实现。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
