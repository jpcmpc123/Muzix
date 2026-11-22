package com.maxrave.simpmusic.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RichSyncPlayheadMonotonicTest {
    @Test
    fun playheadNeverRegressesUnderMinorTickJitterButSnapsOnGenuineSeek() {
        // Interpolated to 1096ms, then player tick arrives slightly behind at 1085ms -> hold 1096ms.
        assertEquals(
            1096L,
            computeMonotonicPlayheadMs(previousPlayheadMs = 1096L, candidateMs = 1085L, rawMs = 1085L),
        )
        // Next frame advances past 1096ms smoothly.
        assertEquals(
            1102L,
            computeMonotonicPlayheadMs(previousPlayheadMs = 1096L, candidateMs = 1102L, rawMs = 1085L),
        )
        // Genuine backward seek (> 400ms) snaps back immediately.
        assertEquals(
            500L,
            computeMonotonicPlayheadMs(previousPlayheadMs = 1102L, candidateMs = 500L, rawMs = 500L),
        )
        // Genuine forward seek (> 600ms) snaps forward immediately.
        assertEquals(
            4500L,
            computeMonotonicPlayheadMs(previousPlayheadMs = 1102L, candidateMs = 4500L, rawMs = 4500L),
        )
    }

    @Test
    fun wordProgressStaysStrictlyMonotonicAcrossRapidVocalsAndResetsOnSeek() {
        val tracker = WordProgressTracker()
        // Ultra-fast 40ms word still sweeps smoothly over MIN_VISUAL_WORD_SWEEP_MS (85ms).
        val p0 = computeMonotonicWordProgress(1000L, 1000L, 1040L, tracker)
        val p1 = computeMonotonicWordProgress(1020L, 1000L, 1040L, tracker)
        // Slight backward clock jitter must NOT make white wipe bounce backwards.
        val pJitter = computeMonotonicWordProgress(1010L, 1000L, 1040L, tracker)
        val p2 = computeMonotonicWordProgress(1060L, 1000L, 1040L, tracker)
        val pDone = computeMonotonicWordProgress(1090L, 1000L, 1040L, tracker)

        assertEquals(0f, p0)
        assertTrue(p1 > 0f && p1 < 1f)
        assertEquals(p1, pJitter)
        assertTrue(p2 > p1 && p2 < 1f)
        assertEquals(1f, pDone)

        // Genuine backward seek (> 400ms) resets the word tracker cleanly.
        val afterSeek = computeMonotonicWordProgress(200L, 1000L, 1040L, tracker)
        assertEquals(0f, afterSeek)
    }
}
