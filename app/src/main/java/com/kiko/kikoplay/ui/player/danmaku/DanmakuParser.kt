package com.kiko.kikoplay.ui.player.danmaku

import android.graphics.Color
import com.kiko.kikoplay.data.remote.model.DanmakuSource
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.IDanmakus
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import master.flame.danmaku.danmaku.util.DanmakuUtils
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToLong

data class DanmakuItem(
    val time: Float,    // seconds
    val type: Int,      // 0=scroll, 1=top, 2=bottom
    val color: Int,
    val text: String,
    val sender: String = ""
)

data class FullDanmakuComment(
    val rawTimeMs: Long,
    val type: Int,
    val color: Int,
    val sourceId: Int,
    val text: String,
    val sender: String = ""
)

data class DanmakuSourceSummary(
    val id: Int,
    val name: String,
    val commentCount: Int,
    val newCommentCount: Int,
    val scrollCount: Int,
    val topCount: Int,
    val bottomCount: Int,
    val senderCount: Int,
    val delayMs: Long,
    val durationSeconds: Double?,
    val scriptName: String?,
    val scriptId: String?,
    val timeline: String?,
    val timelineSegmentCount: Int
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

    fun parseFull(data: List<JsonArray>, sources: List<DanmakuSource> = emptyList()): List<DanmakuItem> {
        return toDisplayItems(parseFullComments(data), sources)
    }

    fun parseFullComments(data: List<JsonArray>): List<FullDanmakuComment> {
        return data.mapNotNull { arr ->
            if (arr.size < 6) return@mapNotNull null
            try {
                FullDanmakuComment(
                    rawTimeMs = (arr[0].jsonPrimitive.double * 1000.0).roundToLong(),
                    type = arr[1].jsonPrimitive.int,
                    color = arr[2].jsonPrimitive.int,
                    sourceId = arr[3].jsonPrimitive.int,
                    text = arr[4].jsonPrimitive.content,
                    sender = arr[5].jsonPrimitive.content
                )
            } catch (_: Exception) { null }
        }
    }

    fun toDisplayItems(
        comments: List<FullDanmakuComment>,
        sources: List<DanmakuSource>,
        hiddenSourceIds: Set<Int> = emptySet()
    ): List<DanmakuItem> {
        val timingBySourceId = sources.associate { source ->
            source.id to SourceTiming(
                delayMs = source.delay,
                timelineEntries = parseTimeline(source.timeline),
                clip = parseClip(source.clip)
            )
        }

        return comments
            .mapNotNull { comment ->
                // 来源被隐藏则不参与渲染（统计仍由 buildSourceSummaries 体现）
                if (comment.sourceId in hiddenSourceIds) return@mapNotNull null
                val adjustedTimeMs = applySourceTiming(
                    rawTimeMs = comment.rawTimeMs,
                    timing = timingBySourceId[comment.sourceId]
                ) ?: return@mapNotNull null
                adjustedTimeMs to DanmakuItem(
                    time = adjustedTimeMs / 1000f,
                    type = comment.type,
                    color = comment.color,
                    text = comment.text,
                    sender = comment.sender
                )
            }
            .sortedBy { it.first }
            .map { it.second }
    }

    fun buildSourceSummaries(
        comments: List<FullDanmakuComment>,
        sources: List<DanmakuSource>,
        newCommentCountBySource: Map<Int, Int> = emptyMap()
    ): List<DanmakuSourceSummary> {
        val commentsBySource = comments.groupBy { it.sourceId }
        val sourceById = sources.associateBy { it.id }
        val orderedSourceIds = buildList {
            addAll(sources.map { it.id })
            addAll(commentsBySource.keys.filterNot(sourceById::containsKey).sorted())
        }

        return orderedSourceIds.map { sourceId ->
            val source = sourceById[sourceId]
            val sourceComments = commentsBySource[sourceId].orEmpty()
            val timelineEntries = parseTimeline(source?.timeline)
            DanmakuSourceSummary(
                id = sourceId,
                name = source?.name?.takeIf { it.isNotBlank() } ?: "来源 #$sourceId",
                commentCount = sourceComments.size,
                newCommentCount = newCommentCountBySource[sourceId] ?: 0,
                scrollCount = sourceComments.count { it.type == 0 },
                topCount = sourceComments.count { it.type == 1 },
                bottomCount = sourceComments.count { it.type == 2 },
                senderCount = sourceComments
                    .asSequence()
                    .map { it.sender.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .count(),
                delayMs = source?.delay ?: 0L,
                durationSeconds = source?.duration,
                scriptName = source?.scriptName?.takeIf { it.isNotBlank() },
                scriptId = source?.scriptId?.takeIf { it.isNotBlank() },
                timeline = source?.timeline,
                timelineSegmentCount = timelineEntries.size
            )
        }
    }

    fun createParser(items: List<DanmakuItem>): BaseDanmakuParser {
        return KikoPlayDanmakuParser(items)
    }

    private data class SourceTiming(
        val delayMs: Long,
        val timelineEntries: List<TimelineEntry>,
        val clip: ClipRange?
    )

    private data class TimelineEntry(
        val timePointMs: Long,
        val offsetMs: Long
    )

    private data class ClipRange(
        val startMs: Long,
        val durationMs: Long
    )

    private fun parseTimeline(timeline: String?): List<TimelineEntry> {
        if (timeline.isNullOrBlank()) return emptyList()

        return timeline
            .split(';')
            .mapNotNull { segment ->
                val parts = segment
                    .trim()
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
                if (parts.size < 2) return@mapNotNull null
                val timePointMs = parts[0].toLongOrNull() ?: return@mapNotNull null
                val offsetMs = parts[1].toLongOrNull() ?: return@mapNotNull null
                TimelineEntry(
                    timePointMs = timePointMs,
                    offsetMs = offsetMs
                )
            }
            .sortedBy { it.timePointMs }
    }

    private fun parseClip(clip: String?): ClipRange? {
        if (clip.isNullOrBlank()) return null

        val parts = clip
            .trim()
            .split(':')
            .map { it.trim() }
        if (parts.size < 2) return null

        val startMs = parts[0].toLongOrNull() ?: return null
        val durationMs = parts[1].toLongOrNull() ?: return null
        if (durationMs < 0L) return null

        return ClipRange(
            startMs = startMs,
            durationMs = durationMs
        )
    }

    private fun applySourceTiming(
        rawTimeMs: Long,
        timing: SourceTiming?
    ): Long? {
        if (timing == null) return rawTimeMs.takeIf { it >= 0L }

        var originTimeMs = rawTimeMs
        timing.clip?.let { clip ->
            if (originTimeMs < clip.startMs || originTimeMs > clip.startMs + clip.durationMs) {
                return null
            }
            originTimeMs -= clip.startMs
        }

        val timelineOffsetMs = timing.timelineEntries
            .takeWhile { originTimeMs > it.timePointMs }
            .sumOf { it.offsetMs }
        val adjustedTimeMs = originTimeMs + timelineOffsetMs + timing.delayMs
        return adjustedTimeMs.takeIf { it >= 0L }
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
                danmaku.setTimer(mTimer)
                danmaku.setTime((item.time * 1000).toLong())
                danmaku.index = addedCount
                danmaku.textSize = 22f * (mContext.displayer?.scaledDensity ?: mContext.displayer?.density ?: 1f)
                danmaku.textColor = item.color or (0xFF shl 24)
                danmaku.textShadowColor = if (isLightColor(item.color)) Color.BLACK else Color.WHITE
                danmaku.padding = 2
                danmaku.priority = 0
                DanmakuUtils.fillText(danmaku, item.text)
                danmakus.addItem(danmaku)
                addedCount++
                if (danmaku.time < firstTime) firstTime = danmaku.time
                if (danmaku.time > lastTime) lastTime = danmaku.time
            }
            android.util.Log.d("DanmakuDebug", "parse(): added=$addedCount, firstTime=${firstTime}ms, lastTime=${lastTime}ms, density=${mContext.displayer?.density}, scaledDensity=${mContext.displayer?.scaledDensity}")
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
