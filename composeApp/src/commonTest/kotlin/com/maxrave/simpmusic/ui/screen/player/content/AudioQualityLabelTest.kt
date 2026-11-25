package com.maxrave.simpmusic.ui.screen.player.content

import com.maxrave.domain.data.entities.NewFormatEntity
import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertNull((null as NewFormatEntity?).toAudioQualityLabel())
    }
}
