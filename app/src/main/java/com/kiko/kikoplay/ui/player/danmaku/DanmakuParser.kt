package com.kiko.kikoplay.ui.player.danmaku

import android.graphics.Color
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.Duration
import master.flame.danmaku.danmaku.model.FTDanmaku
import master.flame.danmaku.danmaku.model.FBDanmaku
import master.flame.danmaku.danmaku.model.R2LDanmaku
import master.flame.danmaku.danmaku.model.IDanmakus
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

data class DanmakuItem(
    val time: Float,    // seconds
    val type: Int,      // 0=scroll, 1=top, 2=bottom
    val color: Int,
    val text: String,
    val sender: String = ""
)

object DanmakuParser {

    fun parseV3(data: List<JsonArray>): List<DanmakuItem> {
        return data.mapNotNull { arr ->
            if (arr.size < 5) return@mapNotNull null
            try {
                DanmakuItem(
                    time = arr[0].jsonPrimitive.double.toFloat(),
                    type = arr[1].jsonPrimitive.int,
                    color = arr[2].jsonPrimitive.int,
                    text = arr[4].jsonPrimitive.content,
                    sender = arr[3].jsonPrimitive.content
                )
            } catch (_: Exception) { null }
        }
    }

    fun parseFull(data: List<JsonArray>): List<DanmakuItem> {
        return data.mapNotNull { arr ->
            if (arr.size < 6) return@mapNotNull null
            try {
                DanmakuItem(
                    time = arr[0].jsonPrimitive.double.toFloat(),
                    type = arr[1].jsonPrimitive.int,
                    color = arr[2].jsonPrimitive.int,
                    text = arr[4].jsonPrimitive.content,
                    sender = arr[5].jsonPrimitive.content
                )
            } catch (_: Exception) { null }
        }
    }

    fun createParser(items: List<DanmakuItem>): BaseDanmakuParser {
        return KikoPlayDanmakuParser(items)
    }

    private fun isLightColor(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000 > 128
    }

    /**
     * 自定义 parser，在 parse() 中通过 mContext（由 DanmakuView.prepare 注入）创建弹幕
     */
    private class KikoPlayDanmakuParser(
        private val items: List<DanmakuItem>
    ) : BaseDanmakuParser() {

        override fun parse(): IDanmakus {
            val danmakus = Danmakus()
            val globalFlags = mContext.mGlobalFlagValues
            var addedCount = 0
            var firstTime = Long.MAX_VALUE
            var lastTime = 0L
            items.forEach { item ->
                val type = when (item.type) {
                    1 -> BaseDanmaku.TYPE_FIX_TOP
                    2 -> BaseDanmaku.TYPE_FIX_BOTTOM
                    else -> BaseDanmaku.TYPE_SCROLL_RL
                }
                val danmaku = mContext.mDanmakuFactory.createDanmaku(type, mContext) ?: return@forEach
                danmaku.flags = globalFlags
                danmaku.time = (item.time * 1000).toLong()
                danmaku.textSize = 25f * (mContext.displayer?.density ?: 1f)
                danmaku.textColor = item.color or (0xFF shl 24)
                danmaku.textShadowColor = if (isLightColor(item.color)) Color.BLACK else Color.WHITE
                danmaku.text = item.text
                danmaku.priority = 0
                danmakus.addItem(danmaku)
                addedCount++
                if (danmaku.time < firstTime) firstTime = danmaku.time
                if (danmaku.time > lastTime) lastTime = danmaku.time
            }
            android.util.Log.d("DanmakuDebug", "parse(): added=$addedCount, firstTime=${firstTime}ms, lastTime=${lastTime}ms, density=${mContext.displayer?.density}, textSize=${25f * (mContext.displayer?.density ?: 1f)}")
            if (addedCount > 0) {
                // 打印前5条弹幕的详细信息
                items.take(5).forEachIndexed { i, item ->
                    android.util.Log.d("DanmakuDebug", "  sample[$i]: time=${item.time}s, type=${item.type}, color=${item.color}, text='${item.text}'")
                }
            }
            return danmakus
        }
    }
}
