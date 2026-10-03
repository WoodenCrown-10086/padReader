package com.translens.app.core.translate

import com.translens.app.core.model.Lang

/**
 * HMS ML Kit 离线翻译实现（Phase 1 接入）。
 *
 * 接入步骤详见 README「接入 HMS ML Kit」小节，要点：
 * 1. 华为开发者账号 + AppGallery Connect 创建应用；
 * 2. 根 settings.gradle.kts 增加 huawei maven 仓库，build.gradle.kts 接入 agconnect 插件；
 * 3. 放置 agconnect-services.json；
 * 4. 依赖 com.huawei.hms:ml-computer-translate；
 * 5. 实现 MLTranslatorFactory + MLTranslateSetting。
 *
 * 未接入前本类会抛出 [UnsupportedOperationException]，由上层降级处理。
 */
class HmsTranslator : Translator {
    override suspend fun translate(text: String, target: Lang): String {
        // TODO(Phase 1): 接入 HMS ML Kit 离线翻译，示意：
        // val settings = MLTranslateSetting.Factory()
        //     .setSourceTextLanguage(MLTranslateLanguage.AUTO)
        //     .setTargetTextLanguage(target.code)
        //     .create()
        // val translator = MLTranslatorFactory.getInstance().getRemoteTranslator(settings)
        // return suspendCancellableCoroutine { cont ->
        //     translator.asyncTranslate(text)
        //         .addOnSuccessListener { cont.resume(it) }
        //         .addOnFailureListener { cont.resumeWithException(it) }
        // }
        throw UnsupportedOperationException("HMS 翻译尚未接入，请先在 AppContainer 中替换为已实现版本")
    }
}
