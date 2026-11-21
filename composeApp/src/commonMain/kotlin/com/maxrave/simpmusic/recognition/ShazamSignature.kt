package com.maxrave.simpmusic.recognition

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin

/**
 * Shazam-compatible audio fingerprint, ported from Metrolist (GPL-3.0), whose pure-Kotlin version
 * ports the vibra algorithm of SongRec (https://github.com/marin-m/SongRec). Metrolist's song
 * recognition is based on Aleksey Saenko's MusicRecognizer
 * (https://github.com/aleksey-saenko/MusicRecognizer).
 *
 * The numbers below are the algorithm's own, not tuning: change one and Shazam stops matching.
 */
internal object ShazamSignature {
    const val SAMPLE_RATE = 16_000

    private const val FFT_SIZE = 2048
    private const val FFT_LOG2 = 11
    private const val FFT_OUTPUT_SIZE = FFT_SIZE / 2 + 1
    private const val CHUNK_STEP = 128
    private const val MAX_PEAKS = 255
    private const val MAX_TIME_SECONDS = 12.0
    private const val RING_BUF_SIZE = 256

    // Bands 250-520, 520-1450, 1450-3500 and 3500-5500 Hz, in the order the signature lists them.
    private const val BAND_250_520 = 0
    private const val BAND_520_1450 = 1
    private const val BAND_1450_3500 = 2
    private const val BAND_3500_5500 = 3

    private val TIME_SPREAD_OFFSETS = intArrayOf(-1, -3, -6)
    private val NEIGHBOR_OFFSETS = intArrayOf(-10, -7, -4, -3, 1, 2, 5, 8)
    private val OTHER_OFFSETS = intArrayOf(-53, -45, 165, 172, 179, 186, 193, 200, 214, 221, 228, 235, 242, 249)

    // vibra's precomputed Hanning window: 0.5 * (1 - cos(2π(i+1)/2049)).
    private val HANNING =
        DoubleArray(FFT_SIZE) { i ->
            0.5 * (1.0 - cos(2.0 * PI * (i + 1).toDouble() / 2049.0))
        }

    // Precomputed bit-reversal permutation for 2048-point iterative Cooley-Tukey FFT.
    private val BIT_REVERSE =
        IntArray(FFT_SIZE) { i ->
            var x = i
            var rev = 0
            repeat(FFT_LOG2) {
                rev = (rev shl 1) or (x and 1)
                x = x ushr 1
            }
            rev
        }

    // Precomputed twiddle factors per stage (halfLen = 1 shl stage) to keep trigonometry out of the hot loop.
    private val TWIDDLE_RE =
        Array(FFT_LOG2) { stage ->
            val halfLen = 1 shl stage
            DoubleArray(halfLen) { k -> cos(-PI * k / halfLen) }
        }
    private val TWIDDLE_IM =
        Array(FFT_LOG2) { stage ->
            val halfLen = 1 shl stage
            DoubleArray(halfLen) { k -> sin(-PI * k / halfLen) }
        }

    // Standard IEEE 802.3 CRC-32 lookup table so ShazamSignature works in commonMain without java.util.zip.
    private val CRC32_TABLE =
        IntArray(256) { entry ->
            var c = entry
            repeat(8) {
                c = if ((c and 1) != 0) (c ushr 1) xor 0xEDB88320.toInt() else (c ushr 1)
            }
            c
        }

    /** [pcm] is mono 16-bit audio at [sampleRate]; returns the `data:audio/vnd.shazam.sig` URI. */
    fun fromPcm(
        pcm: ShortArray,
        sampleRate: Int,
    ): String {
        val samples =
            if (sampleRate == SAMPLE_RATE) {
                pcm
            } else {
                val resampler = LowPassResampler(sampleRate, SAMPLE_RATE)
                resampler.resample(pcm, pcm.size)
            }
        require(samples.isNotEmpty()) { "No audio to fingerprint" }
        val generator = IncrementalSignatureGenerator()
        generator.feedPcm16k(samples, samples.size)
        return generator.snapshotUri()
    }

    /**
     * Streaming low-pass FIR + linear resampler for devices that cannot capture at 16 kHz natively.
     * Attenuates energy above ~7.2 kHz before downsampling so high frequencies do not alias into
     * Shazam's 250-5500 Hz fingerprint bands.
     */
    internal class LowPassResampler(
        private val fromRate: Int,
        private val toRate: Int = SAMPLE_RATE,
    ) {
        private val ratio = toRate.toDouble() / fromRate
        private val taps = buildLowPassTaps(cutoffHz = 7_200.0, sampleRate = fromRate, tapCount = 11)
        private val halfTap = taps.size / 2
        private val history = ShortArray(taps.size - 1)
        private var historyFilled = 0

        fun resample(
            input: ShortArray,
            count: Int = input.size,
        ): ShortArray {
            if (count <= 0) return ShortArray(0)
            if (fromRate == toRate) return if (count == input.size) input else input.copyOf(count)

            val filtered = DoubleArray(count)
            val histLen = history.size
            for (i in 0 until count) {
                var acc = 0.0
                for (t in taps.indices) {
                    val srcIdx = i + t - halfTap
                    val sample =
                        when {
                            srcIdx < 0 -> {
                                val hIdx = historyFilled + srcIdx
                                if (hIdx >= 0 && hIdx < historyFilled) history[hIdx].toDouble() else 0.0
                            }
                            srcIdx < count -> input[srcIdx].toDouble()
                            else -> input[count - 1].toDouble()
                        }
                    acc += sample * taps[t]
                }
                filtered[i] = acc
            }

            // Preserve trailing samples so the next chunk filters seamlessly without boundary clicks.
            val keep = minOf(histLen, count)
            if (keep < histLen && historyFilled > 0) {
                val shift = minOf(historyFilled, histLen - keep)
                history.copyInto(history, destinationOffset = 0, startIndex = historyFilled - shift, endIndex = historyFilled)
                input.copyInto(history, destinationOffset = shift, startIndex = count - keep, endIndex = count)
                historyFilled = shift + keep
            } else {
                input.copyInto(history, destinationOffset = 0, startIndex = count - keep, endIndex = count)
                historyFilled = keep
            }

            val outLen = (count * ratio).toInt()
            if (outLen <= 0) return ShortArray(0)
            return ShortArray(outLen) { i ->
                val srcPos = i / ratio
                val srcIndex = srcPos.toInt()
                val fraction = srcPos - srcIndex
                val s0 = filtered[srcIndex]
                val s1 = if (srcIndex + 1 < count) filtered[srcIndex + 1] else s0
                (s0 * (1.0 - fraction) + s1 * fraction).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        private fun buildLowPassTaps(
            cutoffHz: Double,
            sampleRate: Int,
            tapCount: Int,
        ): DoubleArray {
            val fc = (cutoffHz / sampleRate).coerceIn(0.01, 0.49)
            val mid = (tapCount - 1) / 2.0
            var sum = 0.0
            val raw =
                DoubleArray(tapCount) { i ->
                    val n = i - mid
                    val sinc = if (n == 0.0) 2.0 * fc else sin(2.0 * PI * fc * n) / (PI * n)
                    val window = 0.5 - 0.5 * cos(2.0 * PI * i / (tapCount - 1))
                    val v = sinc * window
                    sum += v
                    v
                }
            for (i in raw.indices) raw[i] /= sum
            return raw
        }
    }

    /**
     * Stateful streaming fingerprint builder. Audio can be fed chunk-by-chunk as it arrives from
     * the microphone, and [snapshotUri] can be called at progressive checkpoints (e.g. 3.5s, 7s, 11.5s)
     * without recomputing earlier FFT frames.
     */
    internal class IncrementalSignatureGenerator {
        private val samplesRing = IntArray(FFT_SIZE)
        private var samplesPos = 0

        private val pending128 = ShortArray(CHUNK_STEP)
        private var pendingLen = 0

        private val fftOutputs = Array(RING_BUF_SIZE) { DoubleArray(FFT_OUTPUT_SIZE) }
        private var fftPos = 0

        private val spreadFfts = Array(RING_BUF_SIZE) { DoubleArray(FFT_OUTPUT_SIZE) }
        private var spreadPos = 0
        private var spreadNumWritten = 0

        // Scratch buffers reused across every FFT step to avoid GC pressure in the hot loop.
        private val fftRe = DoubleArray(FFT_SIZE)
        private val fftIm = DoubleArray(FFT_SIZE)
        private val spreadScratch = DoubleArray(FFT_OUTPUT_SIZE)

        private var numSamples = 0

        private val bandPeaks = Array(4) { mutableListOf<FrequencyPeak>() }
        private var totalPeaks = 0

        val elapsedMs: Long
            get() = numSamples * 1000L / SAMPLE_RATE

        val peakCount: Int
            get() = totalPeaks

        val isBudgetExhausted: Boolean
            get() = (numSamples.toDouble() / SAMPLE_RATE) >= MAX_TIME_SECONDS && totalPeaks >= MAX_PEAKS

        fun feedPcm16k(
            pcm: ShortArray,
            count: Int = pcm.size,
        ) {
            if (count <= 0 || isBudgetExhausted) return
            var idx = 0
            while (idx < count) {
                if (isBudgetExhausted) break
                val needed = CHUNK_STEP - pendingLen
                val toCopy = minOf(needed, count - idx)
                pcm.copyInto(pending128, destinationOffset = pendingLen, startIndex = idx, endIndex = idx + toCopy)
                pendingLen += toCopy
                idx += toCopy

                if (pendingLen == CHUNK_STEP) {
                    process128(pending128)
                    pendingLen = 0
                }
            }
        }

        private fun process128(block: ShortArray) {
            numSamples += CHUNK_STEP
            for (k in 0 until CHUNK_STEP) {
                samplesRing[samplesPos] = block[k].toInt()
                samplesPos = (samplesPos + 1) % FFT_SIZE
            }
            doFFT()
            doPeakSpreading()
            if (spreadNumWritten >= 47) doPeakRecognition()
        }

        private fun doFFT() {
            // Fill in bit-reversed order directly so Cooley-Tukey can run in-place with zero extra copies.
            for (i in 0 until FFT_SIZE) {
                val sample = samplesRing[(samplesPos + i) % FFT_SIZE].toDouble() * HANNING[i]
                fftRe[BIT_REVERSE[i]] = sample
            }
            fftIm.fill(0.0)

            for (stage in 0 until FFT_LOG2) {
                val halfLen = 1 shl stage
                val len = halfLen shl 1
                val twRe = TWIDDLE_RE[stage]
                val twIm = TWIDDLE_IM[stage]
                var i = 0
                while (i < FFT_SIZE) {
                    for (k in 0 until halfLen) {
                        val u = i + k
                        val v = u + halfLen
                        val wRe = twRe[k]
                        val wIm = twIm[k]
                        val evenRe = fftRe[u]
                        val evenIm = fftIm[u]
                        val oddRe = fftRe[v] * wRe - fftIm[v] * wIm
                        val oddIm = fftRe[v] * wIm + fftIm[v] * wRe
                        fftRe[u] = evenRe + oddRe
                        fftIm[u] = evenIm + oddIm
                        fftRe[v] = evenRe - oddRe
                        fftIm[v] = evenIm - oddIm
                    }
                    i += len
                }
            }

            val out = fftOutputs[fftPos]
            val scaleFactor = 1.0 / (1 shl 17)
            for (idx in 0 until FFT_OUTPUT_SIZE) {
                val mag = (fftRe[idx] * fftRe[idx] + fftIm[idx] * fftIm[idx]) * scaleFactor
                out[idx] = if (mag < 1e-10) 1e-10 else mag
            }
            fftPos = (fftPos + 1) % RING_BUF_SIZE
        }

        private fun doPeakSpreading() {
            val lastFftIdx = (fftPos - 1 + RING_BUF_SIZE) % RING_BUF_SIZE
            fftOutputs[lastFftIdx].copyInto(spreadScratch)

            // Frequency spreading: 3-point running max, forward pass.
            for (pos in 0 until FFT_OUTPUT_SIZE - 2) {
                spreadScratch[pos] = maxOf(spreadScratch[pos], spreadScratch[pos + 1], spreadScratch[pos + 2])
            }

            // Time spreading reaches only the older entries at -1, -3 and -6; the new entry keeps
            // frequency spreading alone, as in vibra.
            for (pos in 0 until FFT_OUTPUT_SIZE) {
                var maxVal = spreadScratch[pos]
                for (offset in TIME_SPREAD_OFFSETS) {
                    val idx = ((spreadPos + offset) % RING_BUF_SIZE + RING_BUF_SIZE) % RING_BUF_SIZE
                    val oldVal = spreadFfts[idx][pos]
                    if (oldVal > maxVal) maxVal = oldVal
                    spreadFfts[idx][pos] = maxVal
                }
            }

            spreadScratch.copyInto(spreadFfts[spreadPos])
            spreadPos = (spreadPos + 1) % RING_BUF_SIZE
            spreadNumWritten++
        }

        private fun doPeakRecognition() {
            val fftMinus46 = fftOutputs[(fftPos - 46 + RING_BUF_SIZE * 2) % RING_BUF_SIZE]
            val spreadMinus49 = spreadFfts[(spreadPos - 49 + RING_BUF_SIZE * 2) % RING_BUF_SIZE]

            for (binPos in 10 until FFT_OUTPUT_SIZE - 8) {
                val fftVal = fftMinus46[binPos]
                if (fftVal < 1.0 / 64.0 || fftVal < spreadMinus49[binPos]) continue

                var maxNeighborSpread49 = 0.0
                for (neighborOffset in NEIGHBOR_OFFSETS) {
                    val v = spreadMinus49[binPos + neighborOffset]
                    if (v > maxNeighborSpread49) maxNeighborSpread49 = v
                }
                if (fftVal <= maxNeighborSpread49) continue

                var maxNeighborOther = maxNeighborSpread49
                for (otherOffset in OTHER_OFFSETS) {
                    val spreadIdx = ((spreadPos + otherOffset) % RING_BUF_SIZE + RING_BUF_SIZE) % RING_BUF_SIZE
                    val v = spreadFfts[spreadIdx][binPos - 1]
                    if (v > maxNeighborOther) maxNeighborOther = v
                }
                if (fftVal <= maxNeighborOther) continue

                val fftNumber = spreadNumWritten - 46

                val peakMag = ln(max(1.0 / 64.0, fftVal)) * 1477.3 + 6144
                val peakMagBefore = ln(max(1.0 / 64.0, fftMinus46[binPos - 1])) * 1477.3 + 6144
                val peakMagAfter = ln(max(1.0 / 64.0, fftMinus46[binPos + 1])) * 1477.3 + 6144

                val peakVariation1 = peakMag * 2 - peakMagBefore - peakMagAfter
                if (peakVariation1 <= 0.0) continue
                val peakVariation2 = (peakMagAfter - peakMagBefore) * 32 / peakVariation1
                if (!peakVariation2.isFinite()) continue

                val correctedBin = binPos * 64.0 + peakVariation2
                val frequencyHz = correctedBin * (16000.0 / 2.0 / 1024.0 / 64.0)

                val band =
                    when {
                        frequencyHz < 250.0 -> continue
                        frequencyHz < 520.0 -> BAND_250_520
                        frequencyHz < 1450.0 -> BAND_520_1450
                        frequencyHz < 3500.0 -> BAND_1450_3500
                        frequencyHz <= 5500.0 -> BAND_3500_5500
                        else -> continue
                    }

                bandPeaks[band].add(
                    FrequencyPeak(
                        fftPassNumber = fftNumber,
                        peakMagnitude = peakMag.toInt(),
                        correctedPeakFrequencyBin = correctedBin.toInt(),
                    ),
                )
                totalPeaks++
            }
        }

        @OptIn(ExperimentalEncodingApi::class)
        fun snapshotUri(): String {
            val contentsStream = ByteBufferWriter()

            for (bandId in 0..3) {
                val peaks = bandPeaks[bandId]
                if (peaks.isEmpty()) continue

                val peakBuf = ByteBufferWriter()
                var prevFftPassNumber = 0

                for (peak in peaks) {
                    if (peak.fftPassNumber - prevFftPassNumber >= 255) {
                        // A jump too long for one byte is written as 0xFF plus the absolute position.
                        peakBuf.writeByte(0xFF)
                        peakBuf.writeLeInt(peak.fftPassNumber)
                        prevFftPassNumber = peak.fftPassNumber
                    }
                    peakBuf.writeByte(peak.fftPassNumber - prevFftPassNumber)
                    peakBuf.writeLeShort(peak.peakMagnitude)
                    peakBuf.writeLeShort(peak.correctedPeakFrequencyBin)
                    prevFftPassNumber = peak.fftPassNumber
                }

                val peakBytes = peakBuf.toByteArray()
                contentsStream.writeLeInt(0x60030040 + bandId)
                contentsStream.writeLeInt(peakBytes.size)
                contentsStream.writeBytes(peakBytes)
                repeat((4 - peakBytes.size % 4) % 4) { contentsStream.writeByte(0) }
            }

            val contents = contentsStream.toByteArray()
            val full = ByteBufferWriter(56 + contents.size)
            full.writeLeInt(0xcafe2580.toInt()) // magic1
            full.writeLeInt(0) // crc32, filled in below
            full.writeLeInt(contents.size + 8) // size_minus_header
            full.writeLeInt(0x94119c00.toInt()) // magic2
            full.writeLeInt(0)
            full.writeLeInt(0)
            full.writeLeInt(0)
            full.writeLeInt(3 shl 27) // shifted_sample_rate_id: 16 kHz
            full.writeLeInt(0)
            full.writeLeInt(0)
            full.writeLeInt((numSamples + SAMPLE_RATE * 0.24).toInt())
            full.writeLeInt((15 shl 19) + 0x40000) // fixed_value
            full.writeLeInt(0x40000000)
            full.writeLeInt(contents.size + 8)
            full.writeBytes(contents)

            val fullBytes = full.toByteArray()
            val crc32Value = computeCrc32(fullBytes, start = 8, end = fullBytes.size)
            fullBytes[4] = (crc32Value and 0xFF).toByte()
            fullBytes[5] = ((crc32Value ushr 8) and 0xFF).toByte()
            fullBytes[6] = ((crc32Value ushr 16) and 0xFF).toByte()
            fullBytes[7] = ((crc32Value ushr 24) and 0xFF).toByte()

            return "data:audio/vnd.shazam.sig;base64," + Base64.encode(fullBytes)
        }
    }

    internal fun computeCrc32(
        bytes: ByteArray,
        start: Int = 0,
        end: Int = bytes.size,
    ): Int {
        var crc = -1
        for (i in start until end) {
            val b = bytes[i].toInt() and 0xFF
            crc = (crc ushr 8) xor CRC32_TABLE[(crc xor b) and 0xFF]
        }
        return crc xor -1
    }

    private class FrequencyPeak(
        val fftPassNumber: Int,
        val peakMagnitude: Int,
        val correctedPeakFrequencyBin: Int,
    )

    private class ByteBufferWriter(
        initialCapacity: Int = 64,
    ) {
        private var buf = ByteArray(initialCapacity)
        private var size = 0

        fun writeByte(value: Int) {
            ensureCapacity(1)
            buf[size++] = (value and 0xFF).toByte()
        }

        fun writeLeShort(value: Int) {
            ensureCapacity(2)
            buf[size++] = (value and 0xFF).toByte()
            buf[size++] = ((value ushr 8) and 0xFF).toByte()
        }

        fun writeLeInt(value: Int) {
            ensureCapacity(4)
            buf[size++] = (value and 0xFF).toByte()
            buf[size++] = ((value ushr 8) and 0xFF).toByte()
            buf[size++] = ((value ushr 16) and 0xFF).toByte()
            buf[size++] = ((value ushr 24) and 0xFF).toByte()
        }

        fun writeBytes(bytes: ByteArray) {
            if (bytes.isEmpty()) return
            ensureCapacity(bytes.size)
            bytes.copyInto(buf, destinationOffset = size)
            size += bytes.size
        }

        fun toByteArray(): ByteArray = buf.copyOf(size)

        private fun ensureCapacity(extra: Int) {
            val needed = size + extra
            if (needed <= buf.size) return
            var next = buf.size.coerceAtLeast(16)
            while (next < needed) next = next shl 1
            buf = buf.copyOf(next)
        }
    }
}
