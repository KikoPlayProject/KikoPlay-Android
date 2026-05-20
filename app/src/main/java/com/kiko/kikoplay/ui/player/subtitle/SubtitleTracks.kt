package com.kiko.kikoplay.ui.player.subtitle

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import java.util.Locale

const val SUBTITLE_TRACK_NONE_ID = "subtitle:none"
const val REMOTE_SUBTITLE_TRACK_ID = "kikoplay:remote-subtitle"

enum class SubtitleTrackSource {
    NONE,
    EMBEDDED,
    REMOTE
}

data class SubtitleTrackUiItem(
    val id: String,
    val title: String,
    val source: SubtitleTrackSource,
    val trackGroup: TrackGroup? = null,
    val trackIndex: Int = C.INDEX_UNSET
)

object SubtitleTrackSelector {
    val NoneTrack = SubtitleTrackUiItem(
        id = SUBTITLE_TRACK_NONE_ID,
        title = "无",
        source = SubtitleTrackSource.NONE
    )

    fun withNone(tracks: List<SubtitleTrackUiItem>): List<SubtitleTrackUiItem> {
        return listOf(NoneTrack) + tracks
            .filterNot { it.source == SubtitleTrackSource.NONE || it.id == SUBTITLE_TRACK_NONE_ID }
            .distinctBy { it.id }
    }

    fun defaultTrackId(tracks: List<SubtitleTrackUiItem>): String {
        return tracks.firstOrNull { it.source == SubtitleTrackSource.EMBEDDED }?.id
            ?: tracks.firstOrNull { it.source == SubtitleTrackSource.REMOTE }?.id
            ?: SUBTITLE_TRACK_NONE_ID
    }

    fun fromPlayerTracks(tracks: Tracks): List<SubtitleTrackUiItem> {
        val embeddedTracks = mutableListOf<SubtitleTrackUiItem>()
        val remoteTracks = mutableListOf<SubtitleTrackUiItem>()
        var embeddedOrdinal = 0
        var remoteOrdinal = 0

        tracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed
            val trackGroup = group.mediaTrackGroup

            for (trackIndex in 0 until group.length) {
                if (!group.isTrackSupported(trackIndex)) continue

                val format = group.getTrackFormat(trackIndex)
                val source = if (
                    format.id == REMOTE_SUBTITLE_TRACK_ID ||
                    format.label?.startsWith("Web 字幕") == true
                ) {
                    SubtitleTrackSource.REMOTE
                } else {
                    SubtitleTrackSource.EMBEDDED
                }
                val ordinal = when (source) {
                    SubtitleTrackSource.REMOTE -> ++remoteOrdinal
                    SubtitleTrackSource.EMBEDDED -> ++embeddedOrdinal
                    SubtitleTrackSource.NONE -> 0
                }

                val item = SubtitleTrackUiItem(
                    id = trackId(source, groupIndex, trackIndex, format),
                    title = format.displayTitle(source, ordinal),
                    source = source,
                    trackGroup = trackGroup,
                    trackIndex = trackIndex
                )
                when (source) {
                    SubtitleTrackSource.EMBEDDED -> embeddedTracks += item
                    SubtitleTrackSource.REMOTE -> remoteTracks += item
                    SubtitleTrackSource.NONE -> Unit
                }
            }
        }

        return withNone(embeddedTracks + remoteTracks)
    }

    fun remoteSubtitleLabel(format: String?): String {
        val normalized = format?.trim()?.takeIf { it.isNotEmpty() }?.uppercase(Locale.ROOT)
        return if (normalized == null) "Web 字幕" else "Web 字幕 ($normalized)"
    }

    fun remoteSubtitleMimeType(format: String?): String {
        return when (format?.lowercase(Locale.ROOT)) {
            "srt" -> "application/x-subrip"
            "ass", "ssa" -> "text/x-ssa"
            else -> "text/x-ssa"
        }
    }

    private fun trackId(
        source: SubtitleTrackSource,
        groupIndex: Int,
        trackIndex: Int,
        format: Format
    ): String {
        if (source == SubtitleTrackSource.REMOTE) return "subtitle:remote"

        val stableFormatKey = listOf(
            format.id.orEmpty(),
            format.label.orEmpty(),
            format.language.orEmpty(),
            format.sampleMimeType.orEmpty()
        ).joinToString("|")
        return "subtitle:embedded:$groupIndex:$trackIndex:${stableFormatKey.hashCode()}"
    }

    private fun Format.displayTitle(source: SubtitleTrackSource, ordinal: Int): String {
        val explicitLabel = label?.trim()?.takeIf { it.isNotEmpty() }
        val languageLabel = language
            ?.trim()
            ?.takeIf { it.isNotEmpty() && it != C.LANGUAGE_UNDETERMINED }
            ?.uppercase(Locale.ROOT)

        return when (source) {
            SubtitleTrackSource.REMOTE -> explicitLabel
                ?: languageLabel?.let { "Web 字幕 ($it)" }
                ?: "Web 字幕 ${ordinal.takeIf { it > 1 } ?: ""}".trim()
            SubtitleTrackSource.EMBEDDED -> explicitLabel
                ?: languageLabel?.let { "内嵌字幕 $ordinal ($it)" }
                ?: "内嵌字幕 $ordinal"
            SubtitleTrackSource.NONE -> "无"
        }
    }
}
