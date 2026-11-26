package com.maxrave.simpmusic.ui.component

import com.maxrave.simpmusic.expect.ui.isLiquidGlassSupported
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlassLuminanceTest {

    @Test
    fun captureGateRecordsOnlyOncePerRequestAndSkipsIdleReads() {
        val gate = GlassCaptureGate()

        // First frame captures once and marks a fresh frame ready.
        assertTrue(gate.shouldRecordFrame())
        // Subsequent frames in the same 2.5s window skip recording completely.
        assertFalse(gate.shouldRecordFrame())
        assertFalse(gate.shouldRecordFrame())

        // Sampler consumes the fresh frame once; idle ticks see nothing new.
        assertTrue(gate.takeFreshFrame())
        assertFalse(gate.takeFreshFrame())

        // Next interval asks for a fresh frame, allowing exactly one draw capture.
        gate.requestCapture()
        assertFalse(gate.takeFreshFrame())
        assertTrue(gate.shouldRecordFrame())
        assertFalse(gate.shouldRecordFrame())
        assertTrue(gate.takeFreshFrame())
    }

    @Test
    fun quantizedGlassBlurDpStabilizesWithinRangeAndCapsAtMax() {
        // Mid luminance (0.5) stays at 8dp.
        assertEquals(8f, quantizedGlassBlurDp(luminance = 0.5f))

        // Bright luminance (0.8) quantizes to integer dp instead of noisy sub-pixel floats.
        val bright = quantizedGlassBlurDp(luminance = 0.8f)
        assertEquals(11f, bright)

        // Dark luminance (0.3) quantizes cleanly.
        val dark = quantizedGlassBlurDp(luminance = 0.3f)
        assertEquals(7f, dark)

        // Blob extra blur is capped at 24dp to stay inside Android's fast Gaussian blur tier.
        val blobBright = quantizedGlassBlurDp(luminance = 0.8f, extraDp = 16f, maxDp = 24f)
        assertEquals(24f, blobBright)
    }

    @Test
    fun desktopJvmSupportsLiquidGlass() {
        assertTrue(isLiquidGlassSupported())
    }
}
