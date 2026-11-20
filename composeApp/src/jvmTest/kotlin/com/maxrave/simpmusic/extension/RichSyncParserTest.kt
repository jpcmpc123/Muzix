package com.maxrave.simpmusic.extension

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RichSyncParserTest {
    @Test
    fun returnsNullImmediatelyForBlankOrPlainLinesWithoutRichTags() {
        assertNull(parseRichSyncWords("", "0", "1000"))
        assertNull(parseRichSyncWords("   ", "0", "1000"))
        assertNull(parseRichSyncWords("Plain lyric line without word tags", "0", "1000"))
    }

    @Test
    fun parses1_2And3DigitFractionsAndDecodesHtmlEntities() {
        val parsed =
            parseRichSyncWords(
                words = "<0:01.5>Don&apos;t <00:02.34>stop <100:03:005>rocking",
                lineStartTimeMs = "1500",
                lineEndTimeMs = "6005000",
            )
        assertNotNull(parsed)
        assertEquals(3, parsed.words.size)
        assertEquals("Don't" to 1_500L, parsed.words[0].text to parsed.words[0].startTimeMs)
        assertEquals("stop" to 2_340L, parsed.words[1].text to parsed.words[1].startTimeMs)
        assertEquals("rocking" to 6_003_005L, parsed.words[2].text to parsed.words[2].startTimeMs)
        assertEquals(1_500L, parsed.lineStartTimeMs)
        assertEquals(6_005_000L, parsed.lineEndTimeMs)
    }

    @Test
    fun ignoresTrailingEndMarkerWithoutText() {
        val parsed =
            parseRichSyncWords(
                words = "<00:12.30>First <00:12.80>line<00:14.90>",
                lineStartTimeMs = "12300",
                lineEndTimeMs = "14900",
            )
        assertNotNull(parsed)
        assertEquals(listOf("First" to 12_300L, "line" to 12_800L), parsed.words.map { it.text to it.startTimeMs })
    }
}
