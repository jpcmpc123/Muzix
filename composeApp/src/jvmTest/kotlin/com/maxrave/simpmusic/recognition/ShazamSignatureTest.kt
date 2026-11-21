package com.maxrave.simpmusic.recognition

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.zip.CRC32
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShazamSignatureTest {
    @Test
    fun batchSignatureHasValidShazamHeaderAndCrc32() {
        val signal = multiToneSignal(sampleRate = 16_000, seconds = 5)
        val uri = ShazamSignature.fromPcm(signal, sampleRate = 16_000)
        assertTrue(uri.startsWith("data:audio/vnd.shazam.sig;base64,"))

        val raw = Base64.getDecoder().decode(uri.removePrefix("data:audio/vnd.shazam.sig;base64,"))
        assertTrue(raw.size >= 56)

        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0xcafe2580.toInt(), buf.getInt(0))
        val embeddedCrc = buf.getInt(4)
        assertEquals(0x94119c00.toInt(), buf.getInt(12))
        assertEquals(3 shl 27, buf.getInt(28))

        val expectedCrc =
            CRC32().run {
                update(raw, 8, raw.size - 8)
                value.toInt()
            }
        assertEquals(expectedCrc, embeddedCrc)
        assertEquals(expectedCrc, ShazamSignature.computeCrc32(raw, start = 8, end = raw.size))
    }

    @Test
    fun incrementalStreamingMatchesBatchByteForByte() {
        val signal = multiToneSignal(sampleRate = 16_000, seconds = 6)
        val batchUri = ShazamSignature.fromPcm(signal, sampleRate = 16_000)

        val incremental = ShazamSignature.IncrementalSignatureGenerator()
        val chunkSizes = intArrayOf(320, 512, 77, 1000, 128, 999)
        var offset = 0
        var chunkIdx = 0
        while (offset < signal.size) {
            val len = minOf(chunkSizes[chunkIdx % chunkSizes.size], signal.size - offset)
            val slice = signal.copyOfRange(offset, offset + len)
            incremental.feedPcm16k(slice, slice.size)
            offset += len
            chunkIdx++
        }

        assertEquals(batchUri, incremental.snapshotUri())
        assertTrue(incremental.peakCount > 0)
    }

    @Test
    fun handlesSilenceAndClippedInputWithoutExceptions() {
        val silence = ShortArray(16_000 * 4)
        val silenceGen = ShazamSignature.IncrementalSignatureGenerator()
        silenceGen.feedPcm16k(silence, silence.size)
        assertEquals(0, silenceGen.peakCount)
        assertTrue(silenceGen.snapshotUri().startsWith("data:audio/vnd.shazam.sig;base64,"))

        val clipped = ShortArray(16_000 * 4) { i -> if ((i / 6) % 2 == 0) Short.MAX_VALUE else Short.MIN_VALUE }
        val clippedUri = ShazamSignature.fromPcm(clipped, sampleRate = 16_000)
        assertTrue(clippedUri.startsWith("data:audio/vnd.shazam.sig;base64,"))
    }

    @Test
    fun lowPassResamplerAttenuatesHighFrequenciesAboveNyquist() {
        val rate = 44_100
        val samples1k =
            ShortArray(rate * 2) { i ->
                (sin(2.0 * PI * 1_000.0 * i / rate) * 18_000.0).toInt().toShort()
            }
        val samples14k =
            ShortArray(rate * 2) { i ->
                (sin(2.0 * PI * 14_000.0 * i / rate) * 18_000.0).toInt().toShort()
            }

        val out1k = ShazamSignature.LowPassResampler(rate).resample(samples1k)
        val out14k = ShazamSignature.LowPassResampler(rate).resample(samples14k)

        val rms1k = rms(out1k)
        val rms14k = rms(out14k)
        assertTrue(rms1k > 5_000.0, "Expected 1 kHz tone to pass cleanly, got rms=$rms1k")
        assertTrue(rms14k < rms1k * 0.35, "Expected 14 kHz tone to be attenuated, rms14k=$rms14k vs rms1k=$rms1k")
    }

    private fun multiToneSignal(
        sampleRate: Int,
        seconds: Int,
    ): ShortArray =
        ShortArray(sampleRate * seconds) { i ->
            val t = i.toDouble() / sampleRate
            // Modulated chords across Shazam bands (440 Hz, 880 Hz, 2100 Hz, 4100 Hz)
            val env = 0.6 + 0.4 * sin(2.0 * PI * 1.7 * t)
            val v =
                (
                    sin(2.0 * PI * 440.0 * t) +
                        0.8 * sin(2.0 * PI * 880.0 * t) +
                        0.7 * sin(2.0 * PI * 2_100.0 * t) +
                        0.5 * sin(2.0 * PI * 4_100.0 * t)
                ) * env * 4_000.0
            v.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

    private fun rms(samples: ShortArray): Double {
        var sumSq = 0.0
        for (s in samples) {
            val d = s.toDouble()
            sumSq += d * d
        }
        return sqrt(sumSq / samples.size.coerceAtLeast(1))
    }
}
