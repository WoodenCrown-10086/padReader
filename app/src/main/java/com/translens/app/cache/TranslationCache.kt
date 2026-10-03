package com.translens.app.cache

import android.util.LruCache
import com.translens.app.core.model.Lang

/**
 * 翻译结果 LRU 缓存：同一「原文 + 目标语言」不重复翻译。
 *
 * 翻页 / 来回滚动时命中率高，能显著减少翻译调用次数。
 * 线程安全（内部加锁），可被多协程并发访问。
 */
class TranslationCache(maxEntries: Int = 512) {

    private val cache = object : LruCache<String, String>(maxEntries) {
        override fun sizeOf(key: String, value: String): Int = key.length + value.length
    }

    fun get(key: String): String? = synchronized(cache) { cache.get(key) }

    fun put(key: String, value: String) {
        synchronized(cache) { cache.put(key, value) }
    }

    fun clear() = synchronized(cache) { cache.evictAll() }

    companion object {
        fun key(source: String, target: Lang): String = "${target.code}:$source"
    }
}
