package com.maxrave.simpmusic.ui.component

import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.simpmusic.extension.parseRichSyncWords
import com.maxrave.simpmusic.ui.screen.player.content.stripRichSyncTimestamps
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LyricsSanitizationAndPrepTest {
    @AfterTest
    fun tearDown() {
        LyricsUiPrep.clearCachesForTest()
    }

    @Test
    fun stripRichSyncTimestampsHandlesClosingTagsVoiceHtmlHyphensAndCjk() {
        assertEquals(
            "Há muito tempo",
            "<00:19.99>Há<00:20.00> <00:20.02>muito<00:20.03> <00:20.05>tempo<00:23.92>".stripRichSyncTimestamps(),
        )
        assertEquals("One digit min", "<0:05.1>One <100:00.00>digit [01:02.34]min".stripRichSyncTimestamps())
        assertEquals("mestre-sala", "<00:44.04>mestre-<00:44.70>sala".stripRichSyncTimestamps())
        assertEquals("三浦", "<00:07.22>三<00:08.12>浦".stripRichSyncTimestamps())
        assertEquals("Hello, world", "v1: <i>Hello</i><00:02.00>, world".stripRichSyncTimestamps())
        assertEquals("Simple line", "Simple line".stripRichSyncTimestamps())
    }

    @Test
    fun richSyncParserCapturesClosingEndTimesAndPreTagText() {
        val parsed =
            assertNotNull(
                parseRichSyncWords(
                    words = "Intro <00:19.99>Há<00:20.00> <00:20.50><i>muito</i>",
                    lineStartTimeMs = "19000",
                    lineEndTimeMs = "22000",
                ),
            )
        assertEquals(listOf("Intro", "Há", "muito"), parsed.words.map { it.text })
        assertEquals(listOf(19000L, 19990L, 20500L), parsed.words.map { it.startTimeMs })
        assertEquals(listOf(19990L, 20000L, 22000L), parsed.words.map { it.endTimeMs })
    }

    @Test
    fun warmUpPopulatesRichAndLineSyncedDisplayLinesWithCleanPlainWords() {
        val richLines =
            listOf(
                Line(startTimeMs = "1000", endTimeMs = "3000", syllables = emptyList(), words = "<00:01.00>Há<00:01.20> <00:01.40>tempo<00:02.00>"),
                Line(startTimeMs = "9000", endTimeMs = "11000", syllables = emptyList(), words = "<00:09.00>Depois<00:10.00>"),
            )
        val lyrics = Lyrics(error = false, lines = richLines, syncType = "RICH_SYNCED")
        LyricsUiPrep.warmUp(lyrics)

        val richDisplay = assertNotNull(LyricsUiPrep.peekDisplayLines(richLines, "RICH_SYNCED"))
        assertTrue(richDisplay.lines.size == 3, "Expected 1 inserted interlude dots line")
        assertTrue(richDisplay.plainWords.none { '<' in it || '[' in it })
        assertEquals("Há tempo", richDisplay.plainWords.first())

        val lineDisplay = assertNotNull(LyricsUiPrep.peekDisplayLines(richLines, "LINE_SYNCED"))
        assertEquals(listOf("Há tempo", "Depois"), lineDisplay.plainWords)
    }

    @Test
    fun resolveTranslatedLineAtAlignsMisalignedTimestampsAndSanitizesTags() {
        val orig =
            listOf(
                Line(startTimeMs = "1000", endTimeMs = "2000", syllables = emptyList(), words = "First"),
                Line(startTimeMs = "5000", endTimeMs = "6000", syllables = emptyList(), words = "Second"),
            )
        val trans =
            listOf(
                Line(startTimeMs = "950", endTimeMs = "1800", syllables = emptyList(), words = "<i>Primeira</i>"),
                Line(startTimeMs = "3200", endTimeMs = "3500", syllables = emptyList(), words = "Extra"),
                Line(startTimeMs = "5100", endTimeMs = "6000", syllables = emptyList(), words = "<00:05.10>Segunda"),
            )
        assertEquals("Primeira", LyricsUiPrep.resolveTranslatedLineAt(orig, trans, 0, isSynced = true))
        assertEquals("Segunda", LyricsUiPrep.resolveTranslatedLineAt(orig, trans, 1, isSynced = true))
    }
}
