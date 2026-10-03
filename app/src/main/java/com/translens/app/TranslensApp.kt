package com.translens.app

import android.app.Application
import android.content.Context
import com.translens.app.cache.TranslationCache
import com.translens.app.core.layout.LayoutEngine
import com.translens.app.core.translate.NoopTranslator
import com.translens.app.core.translate.Translator
import com.translens.app.settings.SettingsRepo

/**
 * 手动依赖注入容器：轻量、无反射、无代码生成，便于替换实现与单元测试。
 *
 * 跨设备时只需在这里替换 [translator]（HMS / 云 / 本地模型）等实现，
 * 其余代码无需改动。
 */
class AppContainer(context: Context) {
    val settings: SettingsRepo = SettingsRepo(context.applicationContext)
    val layoutEngine: LayoutEngine = LayoutEngine()
    val cache: TranslationCache = TranslationCache()

    /** 翻译引擎：Phase 0 占位实现；Phase 1 替换为 HmsTranslator。 */
    val translator: Translator = NoopTranslator()
}

class TranslensApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
