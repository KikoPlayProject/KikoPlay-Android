package com.kiko.kikoplay.ui.player.danmaku

import com.kiko.kikoplay.data.remote.model.DanmakuSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuParserTest {

    @Test
    fun parseFull_appliesClipThenCumulativeTimelineThenDelay() {
        val comments = listOf(
            fullComment(timeSeconds = 9.999, text = "before clip"),
            fullComment(timeSeconds = 10.0, text = "clip start"),
            fullComment(timeSeconds = 11.001, text = "after first timeline point"),
            fullComment(timeSeconds = 12.001, text = "after second timeline point"),
            fullComment(timeSeconds = 15.0, text = "clip end"),
            fullComment(timeSeconds = 15.001, text = "after clip")
        )
        val sources = listOf(
            DanmakuSource(
                id = 1,
                name = "source",
                delay = 100,
                timeline = "1000 200;2000 -50",
                clip = "10000:5000"
            )
        )

        val items = DanmakuParser.parseFull(comments, sources)

        assertEquals(
            listOf("clip start", "after first timeline point", "after second timeline point", "clip end"),
            items.map { it.text }
        )
        assertEquals(listOf(0.1f, 1.301f, 2.251f, 5.25f), items.map { it.time })
    }

    @Test
    fun parseFull_matchesPcTimelineBoundaryAndNegativeTimeClipping() {
        val comments = listOf(
            fullComment(timeSeconds = 1.0, text = "at boundary"),
            fullComment(timeSeconds = 1.001, text = "after boundary"),
            fullComment(timeSeconds = 2.0, text = "negative final time")
        )
        val sources = listOf(
            DanmakuSource(
                id = 1,
                name = "source",
                delay = -2500,
                timeline = "1000 2000"
            )
        )

        val items = DanmakuParser.parseFull(comments, sources)

        assertEquals(listOf("after boundary"), items.map { it.text })
        assertEquals(listOf(0.501f), items.map { it.time })
    }

    private fun fullComment(
        timeSeconds: Double,
        text: String,
        sourceId: Int = 1
    ): JsonArray {
        return Json.parseToJsonElement(
            """[$timeSeconds,0,16777215,$sourceId,"$text","sender"]"""
        ) as JsonArray
    }
}
