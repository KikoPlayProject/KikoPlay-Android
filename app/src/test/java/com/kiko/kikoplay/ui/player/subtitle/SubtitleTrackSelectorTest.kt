package com.kiko.kikoplay.ui.player.subtitle

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleTrackSelectorTest {

    @Test
    fun defaultTrack_prefersFirstEmbeddedWhenRemoteAlsoExists() {
        val tracks = SubtitleTrackSelector.fromPlayerTracks(
            tracksOf(
                group(
                    format(id = "embedded-1", label = "简体中文"),
                    format(id = REMOTE_SUBTITLE_TRACK_ID, label = "Web 字幕 (SRT)")
                )
            )
        )

        assertEquals(
            listOf("无", "简体中文", "Web 字幕 (SRT)"),
            tracks.map { it.title }
        )
        assertEquals(SubtitleTrackSource.EMBEDDED, tracks[1].source)
        assertEquals(tracks[1].id, SubtitleTrackSelector.defaultTrackId(tracks))
    }

    @Test
    fun defaultTrack_usesRemoteWhenNoEmbeddedExists() {
        val tracks = SubtitleTrackSelector.fromPlayerTracks(
            tracksOf(
                group(format(id = REMOTE_SUBTITLE_TRACK_ID, label = "Web 字幕 (ASS)"))
            )
        )

        assertEquals(listOf("无", "Web 字幕 (ASS)"), tracks.map { it.title })
        assertEquals(tracks[1].id, SubtitleTrackSelector.defaultTrackId(tracks))
    }

    @Test
    fun defaultTrack_usesNoneWhenNoSubtitleExists() {
        val tracks = SubtitleTrackSelector.fromPlayerTracks(Tracks.EMPTY)

        assertEquals(listOf("无"), tracks.map { it.title })
        assertEquals(SUBTITLE_TRACK_NONE_ID, SubtitleTrackSelector.defaultTrackId(tracks))
    }

    private fun tracksOf(vararg groups: TrackGroup): Tracks {
        return Tracks(
            groups.map { trackGroup ->
                Tracks.Group(
                    trackGroup,
                    false,
                    IntArray(trackGroup.length) { C.FORMAT_HANDLED },
                    BooleanArray(trackGroup.length)
                )
            }
        )
    }

    private fun group(vararg formats: Format): TrackGroup = TrackGroup(*formats)

    private fun format(
        id: String,
        label: String? = null,
        language: String? = null
    ): Format {
        return Format.Builder()
            .setId(id)
            .setLabel(label)
            .setLanguage(language)
            .setSampleMimeType("text/x-ssa")
            .build()
    }
}
