package com.kiko.kikoplay.data.repository

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 会话级"弹幕来源隐藏状态"记忆。
 *
 * 按弹幕池 key（danmuPool，PC 串流时多集共享同一池）隔离来源隐藏集合。
 *
 * sourceId 仅在单个弹幕池内有效，不跨池唯一，因此不做全局持久化（避免跨池错配与无界增长）；
 * 仅在进程内存中保留，覆盖"同一番剧跨集保持隐藏设置"这一核心体验，退出 App 即清空。
 *
 * 使用 [ConcurrentHashMap] 以保证 ViewModel 协程读取与主线程 toggle 写入之间的线程安全：
 * 单个 pool 对应的 MutableSet 仍在调用方保证原子性，但 map 层面的读写不会破坏结构。
 */
@Singleton
class HiddenDanmakuSourceStore @Inject constructor() {
    private val hiddenByPool = ConcurrentHashMap<String, MutableSet<Int>>()

    fun getHiddenSourceIds(pool: String?): Set<Int> {
        if (pool.isNullOrBlank()) return emptySet()
        return hiddenByPool[pool]?.toSet() ?: emptySet()
    }

    fun setHidden(pool: String?, sourceId: Int, hidden: Boolean) {
        if (pool.isNullOrBlank()) return
        val set = hiddenByPool.computeIfAbsent(pool) { java.util.Collections.synchronizedSet(mutableSetOf()) }
        if (hidden) set.add(sourceId) else set.remove(sourceId)
    }

    fun replaceAll(pool: String?, hiddenSourceIds: Set<Int>) {
        if (pool.isNullOrBlank()) return
        if (hiddenSourceIds.isEmpty()) {
            hiddenByPool.remove(pool)
        } else {
            hiddenByPool[pool] = java.util.Collections.synchronizedSet(hiddenSourceIds.toMutableSet())
        }
    }
}

