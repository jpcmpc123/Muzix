package com.maxrave.simpmusic.ui.component

import com.maxrave.domain.data.model.lyrics.RomanizationDictionaryState
import com.maxrave.domain.data.model.lyrics.RomanizationLanguage
import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.repository.LyricsRomanizerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LyricsUiPrepTest {
    private class FakeRomanizer : LyricsRomanizerRepository {
        var calls = 0
        val seenInputs = mutableListOf<String>()

        override fun romanize(
            line: String,
            enabled: Set<RomanizationLanguage>,
        ): String {
            calls++
            seenInputs.add(line)
            return "rom($line)"
        }

        override val japaneseDictionaryState: StateFlow<RomanizationDictionaryState> =
            MutableStateFlow(RomanizationDictionaryState.READY)

        override suspend fun downloadJapaneseDictionary(): RomanizationDictionaryState =
            RomanizationDictionaryState.READY

        override fun ensureJapaneseDictionary() = Unit
    }

    @BeforeTest
    fun setUp() {
        LyricsUiPrep.clearCachesForTest()
    }

    @AfterTest
    fun tearDown() {
        LyricsUiPrep.clearCachesForTest()
    }

    @Test
    fun warmUpPopulatesDisplayLinesAndAllRichSyncLinesInCache() {
        val line1 = Line("1000", "2000", null, "<00:01.00>Hello <00:01.50>world")
        val line2 = Line("8000", "9000", null, "<00:08.00>After <00:08.50>silence")
        val line3 = Line("9500", "10500", null, "<00:09.50>Final")
        val lyrics =
            Lyrics(
                error = false,
                lines = listOf(line1, line2, line3),
                syncType = "RICH_SYNCED",
            )

        assertNull(LyricsUiPrep.peekDisplayLines(lyrics.lines, lyrics.syncType))

        LyricsUiPrep.warmUp(lyrics)

        val display = LyricsUiPrep.peekDisplayLines(lyrics.lines, lyrics.syncType)
        assertNotNull(display)
        assertEquals(4, display.lines.size)
        assertEquals(setOf(1), display.interludeIndices)

        // All display lines including last line & interlude dots hit rich sync cache with same instance
        display.lines.forEach { line ->
            val first = LyricsUiPrep.parseRichSyncLineCached(line.words, line.startTimeMs, line.endTimeMs)
            val second = LyricsUiPrep.parseRichSyncLineCached(line.words, line.startTimeMs, line.endTimeMs)
            assertNotNull(first)
            assertSame(first, second)
        }
    }

    @Test
    fun romanizationSkipsInterludesStripsRichTimestampsAndCachesResult() {
        val display =
            DisplayLines(
                lines =
                    listOf(
                        Line("1000", "2000", null, "<00:01.00>東京 <00:01.50>の夜"),
                        Line("5000", "9000", null, "<00:05.00>•<00:06.33>•<00:07.66>•"),
                        Line("9000", "10000", null, "<00:09.00>また"),
                    ),
                interludeIndices = setOf(1),
            )
        val romanizer = FakeRomanizer()
        val langs = setOf(RomanizationLanguage.JAPANESE)

        assertNull(
            LyricsUiPrep.peekRomanizedLines(
                lines = display.lines,
                interludeIndices = display.interludeIndices,
                syncType = "RICH_SYNCED",
                languages = langs,
                japaneseReady = true,
            ),
        )

        val firstMap =
            LyricsUiPrep.getOrComputeRomanizedLines(
                lines = display.lines,
                interludeIndices = display.interludeIndices,
                syncType = "RICH_SYNCED",
                languages = langs,
                japaneseReady = true,
                romanizer = romanizer,
            )
        assertEquals(2, romanizer.calls)
        assertEquals(setOf(0, 2), firstMap.keys)
        assertTrue(firstMap[0]!!.contains("東京"))
        assertTrue('<' !in romanizer.seenInputs[0])
        assertNull(firstMap[1])

        val secondMap =
            LyricsUiPrep.getOrComputeRomanizedLines(
                lines = display.lines,
                interludeIndices = display.interludeIndices,
                syncType = "RICH_SYNCED",
                languages = langs,
                japaneseReady = true,
                romanizer = romanizer,
            )
        assertEquals(2, romanizer.calls)
        assertSame(firstMap, secondMap)
    }
}
