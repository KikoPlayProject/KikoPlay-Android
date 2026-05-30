package com.kiko.kikoplay.ui.player.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteSubtitleNormalizerTest {

    @Test
    fun normalizeSrt_convertsDotMillisecondsToCommaMilliseconds() {
        val raw = """
            1
            00:00:11.392 --> 00:00:13.392
            Hello

            2
            00:54:25.2 --> 00:54:26.82
            World
        """.trimIndent()

        val normalized = RemoteSubtitleNormalizer.normalize("srt", raw)

        assertTrue(normalized.contains("00:00:11,392 --> 00:00:13,392"))
        assertTrue(normalized.contains("00:54:25,200 --> 00:54:26,820"))
    }

    @Test
    fun normalizeSrt_keepsNonTimingTextUnchanged() {
        val raw = "1\n00:00:01,000 --> 00:00:02,000\nVersion 1.2.3\n"

        val normalized = RemoteSubtitleNormalizer.normalize("srt", raw)

        assertEquals(raw, normalized)
    }

    @Test
    fun normalize_ignoresOtherSubtitleFormats() {
        val raw = "Dialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,Hello"

        val normalized = RemoteSubtitleNormalizer.normalize("ass", raw)

        assertEquals(raw, normalized)
    }
}
