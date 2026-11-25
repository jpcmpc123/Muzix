package com.maxrave.simpmusic.ui.screen.player.content

import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.player.ActiveAudioTrackInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AudioQualityLabelTest {
    private fun entity(
        itag: Int = 251,
        mimeType: String? = "audio/webm",
        codecs: String? = "opus",
        bitrate: Int? = 142_310,
        sampleRate: Int? = 48_000,
    ) = NewFormatEntity(
        videoId = "testId",
        itag = itag,
        mimeType = mimeType,
        codecs = codecs,
        bitrate = bitrate,
        sampleRate = sampleRate,
        contentLength = null,
        loudnessDb = null,
        lengthSeconds = 200,
        playbackTrackingVideostatsPlaybackUrl = null,
        playbackTrackingAtrUrl = null,
        playbackTrackingVideostatsWatchtimeUrl = null,
        cpn = null,
    )

    @Test
    fun `toAudioQualityLabel formats codec bitrate and sample rate cleanly`() {
        assertEquals(
            "Opus · 142 kbps · 48 kHz",
            entity(itag = 251, mimeType = "audio/webm", codecs = "opus", bitrate = 142_310, sampleRate = 48_000).toAudioQualityLabel(),
        )
        assertEquals(
            "AAC · 128 kbps · 44.1 kHz",
            entity(itag = 140, mimeType = "audio/mp4", codecs = "mp4a.40.2", bitrate = 128_000, sampleRate = 44_100).toAudioQualityLabel(),
        )
        assertEquals(
            "Opus",
            entity(itag = 251, mimeType = null, codecs = null, bitrate = null, sampleRate = null).toAudioQualityLabel(),
        )
        assertEquals(
            "MP3 · 320 kbps · 44.1 kHz",
            entity(itag = 0, mimeType = "audio/mpeg", codecs = null, bitrate = 320_000, sampleRate = 44_100).toAudioQualityLabel(),
        )
        assertNull((null as NewFormatEntity?).toAudioQualityLabel())
    }

    @Test
    fun `mergeFormatWithActiveAudioTrack synthesizes fallback when db row is missing`() {
        val merged =
            mergeFormatWithActiveAudioTrack(
                dbFormat = null,
                activeTrack =
                    ActiveAudioTrackInfo(
                        mediaId = "GLyyjO0O79Y",
                        mimeType = "audio/opus",
                        codecs = "opus",
                        bitrate = 136_000,
                        sampleRate = 48_000,
                    ),
                currentMediaId = "GLyyjO0O79Y",
            )
        assertNotNull(merged)
        assertEquals(0, merged.itag)
        assertEquals("Opus · 136 kbps · 48 kHz", merged.toAudioQualityLabel())
    }

    @Test
    fun `mergeFormatWithActiveAudioTrack ignores mismatched mediaId from previous track`() {
        val merged =
            mergeFormatWithActiveAudioTrack(
                dbFormat = null,
                activeTrack =
                    ActiveAudioTrackInfo(
                        mediaId = "oldId",
                        mimeType = "audio/opus",
                        codecs = "opus",
                        bitrate = 136_000,
                        sampleRate = 48_000,
                    ),
                currentMediaId = "newId",
            )
        assertNull(merged)
    }

    @Test
    fun `mergeFormatWithActiveAudioTrack preserves DB metadata and fills missing fields from ExoPlayer`() {
        val stored =
            entity(
                itag = 251,
                mimeType = "audio/webm",
                codecs = "opus",
                bitrate = null,
                sampleRate = null,
            )
        val merged =
            mergeFormatWithActiveAudioTrack(
                dbFormat = stored,
                activeTrack =
                    ActiveAudioTrackInfo(
                        mediaId = "${com.maxrave.common.MERGING_DATA_TYPE.VIDEO}testId",
                        mimeType = "audio/opus",
                        codecs = "opus",
                        bitrate = 148_000,
                        sampleRate = 48_000,
                    ),
                currentMediaId = "testId",
            )
        assertNotNull(merged)
        assertEquals(251, merged.itag)
        assertEquals("Opus · 148 kbps · 48 kHz", merged.toAudioQualityLabel())
    }
}
