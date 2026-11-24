package com.maxrave.simpmusic.ui.component

import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.utils.stackBlurArgb
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppleMusicLyricsBlurTest {
    @AfterTest
    fun tearDown() {
        AppleMusicBlurEffectPool.clearForTest()
    }

    @Test
    fun `resolveEffectiveDistance clamps negative and distant indices at max depth`() {
        assertEquals(0, resolveEffectiveDistance(0))
        // Past lines receive +1 depth step so already-sung lyrics recede faster like Apple Music
        assertEquals(2, resolveEffectiveDistance(-1))
        assertEquals(3, resolveEffectiveDistance(-2))
        assertEquals(1, resolveEffectiveDistance(1))
        assertEquals(MAX_EFFECTIVE_DISTANCE, resolveEffectiveDistance(-3))
        assertEquals(MAX_EFFECTIVE_DISTANCE, resolveEffectiveDistance(4))
        assertEquals(MAX_EFFECTIVE_DISTANCE, resolveEffectiveDistance(12))
        assertEquals(MAX_EFFECTIVE_DISTANCE, resolveEffectiveDistance(-9))
    }

    @Test
    fun `resolveTargetBlurDp applies smooth graduated curve and zeroes when disabled or unsynced`() {
        assertEquals(0.dp, resolveTargetBlurDp(distanceFromCurrent = 0, blurEnabled = true))
        assertEquals(1.4.dp, resolveTargetBlurDp(distanceFromCurrent = 1, blurEnabled = true))
        assertEquals(2.8.dp, resolveTargetBlurDp(distanceFromCurrent = 2, blurEnabled = true))
        assertEquals(4.4.dp, resolveTargetBlurDp(distanceFromCurrent = -2, blurEnabled = true))
        assertEquals(4.4.dp, resolveTargetBlurDp(distanceFromCurrent = 3, blurEnabled = true))
        assertEquals(5.8.dp, resolveTargetBlurDp(distanceFromCurrent = 4, blurEnabled = true))
        assertEquals(5.8.dp, resolveTargetBlurDp(distanceFromCurrent = 25, blurEnabled = true))

        assertEquals(0.dp, resolveTargetBlurDp(distanceFromCurrent = 3, blurEnabled = false))
        assertEquals(0.dp, resolveTargetBlurDp(distanceFromCurrent = 2, blurEnabled = true, hasActiveLine = false))
        assertEquals(0.dp, resolveTargetBlurDp(distanceFromCurrent = 2, blurEnabled = true, allLinesCurrent = true))
    }

    @Test
    fun `resolveTargetAlpha matches focus gradient and pre-roll state`() {
        assertEquals(1.0f, resolveTargetAlpha(distanceFromCurrent = 0))
        assertEquals(0.68f, resolveTargetAlpha(distanceFromCurrent = 1))
        assertEquals(0.48f, resolveTargetAlpha(distanceFromCurrent = 2))
        assertEquals(0.34f, resolveTargetAlpha(distanceFromCurrent = -2))
        assertEquals(0.34f, resolveTargetAlpha(distanceFromCurrent = 3))
        assertEquals(0.24f, resolveTargetAlpha(distanceFromCurrent = 4))
        assertEquals(0.24f, resolveTargetAlpha(distanceFromCurrent = 15))
        assertEquals(PRE_ROLL_LINE_ALPHA, resolveTargetAlpha(distanceFromCurrent = 1, hasActiveLine = false))
        assertEquals(1.0f, resolveTargetAlpha(distanceFromCurrent = 3, allLinesCurrent = true))
    }

    @Test
    fun `quantizeBlurStep and quantizeBlurDp bucket in 0_5dp steps up to cap`() {
        assertEquals(0, quantizeBlurStep(0f))
        assertEquals(0, quantizeBlurStep(0.12f))
        assertEquals(3, quantizeBlurStep(1.4f))
        assertEquals(12, quantizeBlurStep(5.8f))
        assertEquals((BLUR_MAX_DP / BLUR_QUANTIZE_STEP_DP).toInt(), quantizeBlurStep(20f))

        assertEquals(0.dp, quantizeBlurDp(0f))
        assertEquals(1.5.dp, quantizeBlurDp(1.4f))
        assertEquals(6.0.dp, quantizeBlurDp(5.8f))
        assertEquals(BLUR_MAX_DP.dp, quantizeBlurDp(18f))
    }

    @Test
    fun `AppleMusicBlurEffectPool reuses quantized BlurEffect instances`() {
        AppleMusicBlurEffectPool.clearForTest()

        assertNull(AppleMusicBlurEffectPool.getOrCreate(0.1f, density = 3.0f, tileMode = TileMode.Decal))

        val first = AppleMusicBlurEffectPool.getOrCreate(1.4f, density = 3.0f, tileMode = TileMode.Decal)
        val sameBucket = AppleMusicBlurEffectPool.getOrCreate(1.52f, density = 3.0f, tileMode = TileMode.Decal)
        assertSame(first, sameBucket)

        val dist4 = AppleMusicBlurEffectPool.getOrCreate(5.8f, density = 3.0f, tileMode = TileMode.Decal)
        val dist20 = AppleMusicBlurEffectPool.getOrCreate(5.8f, density = 3.0f, tileMode = TileMode.Decal)
        assertSame(dist4, dist20)

        assertTrue(AppleMusicBlurEffectPool.sizeForTest() <= 2)
    }

    @Test
    fun `stackBlurArgb smooths sharp impulse safely within bounds`() {
        val width = 16
        val height = 16
        val pixels = IntArray(width * height) { 0xFF000000.toInt() }
        pixels[8 * width + 8] = 0xFFFFFFFF.toInt()

        stackBlurArgb(pixels, width, height, radius = 3)

        val centerAlpha = (pixels[8 * width + 8] ushr 24) and 0xFF
        val centerRed = (pixels[8 * width + 8] ushr 16) and 0xFF
        val neighborRed = (pixels[8 * width + 9] ushr 16) and 0xFF

        assertEquals(255, centerAlpha)
        assertTrue(centerRed in 1..254, "Expected center peak to diffuse, got $centerRed")
        assertTrue(neighborRed in 1 until centerRed, "Expected neighbor to receive energy, got $neighborRed")

        // No-op safety on tiny buffers or zero radius
        val tiny = intArrayOf(0xFF123456.toInt())
        stackBlurArgb(tiny, width = 1, height = 1, radius = 0)
        assertEquals(0xFF123456.toInt(), tiny[0])
    }
}
