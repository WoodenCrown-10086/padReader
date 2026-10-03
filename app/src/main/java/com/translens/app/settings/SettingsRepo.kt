package com.translens.app.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.translens.app.core.model.Lang
import com.translens.app.core.model.RenderMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 全局 DataStore 实例（进程内唯一）。 */
private val Context.dataStore by preferencesDataStore(name = "translens_settings")

/**
 * 配置持久化：目标语言、渲染模式等。
 *
 * 使用 DataStore 而非 SharedPreferences：类型安全、线程安全、协程友好，
 * 且后续可平滑迁移到 Proto DataStore。
 */
class SettingsRepo(private val context: Context) {

    private val targetLangKey = stringPreferencesKey("target_lang")
    private val renderModeKey = stringPreferencesKey("render_mode")

    val targetLang: Flow<Lang> = context.dataStore.data.map { prefs ->
        Lang.fromCode(prefs[targetLangKey]) ?: Lang.EN
    }

    val renderMode: Flow<RenderMode> = context.dataStore.data.map { prefs ->
        prefs[renderModeKey]?.let { name ->
            RenderMode.entries.firstOrNull { it.name == name }
        } ?: RenderMode.IN_PLACE
    }

    suspend fun setTargetLang(lang: Lang) {
        context.dataStore.edit { it[targetLangKey] = lang.code }
    }

    suspend fun setRenderMode(mode: RenderMode) {
        context.dataStore.edit { it[renderModeKey] = mode.name }
    }
}
