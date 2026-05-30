package com.kiko.kikoplay.ui.player.subtitle

import java.util.Locale

object RemoteSubtitleNormalizer {
    private val srtTimingLineRegex = Regex("""(?m)^.*-->\s*.*$""")
    private val srtTimeCodeRegex = Regex("""(\d{1,2}:\d{2}:\d{2})([,.])(\d{1,9})""")

    fun normalize(format: String?, content: String): String {
        return when (format?.trim()?.lowercase(Locale.ROOT)) {
            "srt" -> normalizeSrt(content)
            else -> content
        }
    }

    private fun normalizeSrt(content: String): String {
        val withoutBom = content.removePrefix("\uFEFF")
        return srtTimingLineRegex.replace(withoutBom) { timingLine ->
            srtTimeCodeRegex.replace(timingLine.value) { timeCode ->
                val timestamp = timeCode.groupValues[1]
                val millis = normalizeMillis(timeCode.groupValues[3])
                "$timestamp,$millis"
            }
        }
    }

    private fun normalizeMillis(value: String): String {
        return value
            .take(3)
            .padEnd(3, '0')
    }
}
