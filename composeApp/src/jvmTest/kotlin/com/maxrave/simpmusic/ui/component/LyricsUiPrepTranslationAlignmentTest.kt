package com.maxrave.simpmusic.ui.component

import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.data.model.metadata.Lyrics
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class LyricsUiPrepTranslationAlignmentTest {
    @BeforeTest
    fun setUp() {
        LyricsUiPrep.clearCachesForTest()
    }

    @AfterTest
    fun tearDown() {
        LyricsUiPrep.clearCachesForTest()
    }

    @Test
    fun fastPathMapsAlignedOneToOneLinesAndSkipsBlankRows() {
        val original =
            listOf(
                line("1000", "And for a second there we'd"),
                line("2500", "won"),
                line("4000", "♫"),
            )
        val translated =
            listOf(
                line("1000", "E por um segundo lá nós"),
                line("2500", "ganhamos"),
                line("4000", "   "),
            )

        assertNull(LyricsUiPrep.peekTranslatedLineMap(original, translated))
        val mapped = LyricsUiPrep.getOrBuildTranslatedLineMap(original, translated)
        assertEquals(
            mapOf(
                0 to "E por um segundo lá nós",
                1 to "ganhamos",
            ),
            mapped,
        )
        // Second lookup hits the LRU cache directly with the exact same instance.
        assertSame(mapped, LyricsUiPrep.peekTranslatedLineMap(original, translated))
    }

    @Test
    fun fallbackTwoPointerMatchesCommunityTranslationsWithDifferentCountsAndOffsets() {
        val original =
            listOf(
                line("1000", "Line one"),
                line("5000", "Line two"),
                line("12000", "Line three"),
            )
        val translated =
            listOf(
                line("1250", "Linha um"),
                line("11800", "Linha três"),
            )

        val mapped = LyricsUiPrep.getOrBuildTranslatedLineMap(original, translated, thresholdMs = 1000L)
        assertEquals(
            mapOf(
                0 to "Linha um",
                2 to "Linha três",
            ),
            mapped,
        )
    }

    @Test
    fun warmUpTranslationPrePopulatesCacheForImmediateMainThreadPeek() {
        val originalLyrics =
            Lyrics(
                error = false,
                syncType = "LINE_SYNCED",
                lines = listOf(line("1000", "Hello"), line("3000", "World")),
            )
        val translatedLyrics =
            Lyrics(
                error = false,
                syncType = "LINE_SYNCED",
                lines = listOf(line("1000", "Olá"), line("3000", "Mundo")),
            )

        LyricsUiPrep.warmUpTranslation(originalLyrics, translatedLyrics)
        val peeked = LyricsUiPrep.peekTranslatedLineMap(originalLyrics.lines!!, translatedLyrics.lines!!)
        assertNotNull(peeked)
        assertEquals(mapOf(0 to "Olá", 1 to "Mundo"), peeked)
    }

    private fun line(
        startMs: String,
        words: String,
    ) = Line(
        startTimeMs = startMs,
        endTimeMs = (startMs.toLong() + 1000L).toString(),
        syllables = null,
        words = words,
    )
}
