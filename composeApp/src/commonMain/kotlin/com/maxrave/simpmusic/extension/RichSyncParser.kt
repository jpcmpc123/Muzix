package com.maxrave.simpmusic.extension

import com.maxrave.domain.extension.decodeHtmlEntities

/**
 * Represents a single word with its timing information for rich sync lyrics
 */
data class WordTiming(
    val text: String,
    val startTimeMs: Long,
)

/**
 * Represents a parsed rich sync line with all word timings
 */
data class ParsedRichSyncLine(
    val words: List<WordTiming>,
    val lineStartTimeMs: Long,
    val lineEndTimeMs: Long,
)

private val RICH_WORD_TIMESTAMP_REGEX = Regex("""<(\d{1,3}):(\d{2})[.:](\d{1,3})>""")

private fun parseRichTimestampMs(match: MatchResult): Long {
    val (minutes, seconds, fraction) = match.destructured
    val fractionMs = fraction.toLongOrNull() ?: 0L
    val scaledMs =
        when (fraction.length) {
            1 -> fractionMs * 100L
            2 -> fractionMs * 10L
            else -> fractionMs
        }
    return (minutes.toLongOrNull() ?: 0L) * 60000L +
        (seconds.toLongOrNull() ?: 0L) * 1000L +
        scaledMs
}

/**
 * Parses rich sync lyrics format into word-level timing data
 *
 * Expected format: <MM:SS.mm> word <MM:SS.mm> word ...
 * Example: <00:16.62> Và <00:16.64> em <00:16.68> nói
 *
 * @param words The rich sync string containing timestamps and words
 * @param lineStartTimeMs The start time of the line as a string
 * @param lineEndTimeMs The end time of the line as a string
 * @return ParsedRichSyncLine if parsing succeeds, null if parsing fails or input is invalid
 */
fun parseRichSyncWords(
    words: String,
    lineStartTimeMs: String,
    lineEndTimeMs: String,
): ParsedRichSyncLine? {
    // Handle edge cases
    if (words.isBlank() || '<' !in words) {
        return null
    }

    val wordTimings = mutableListOf<WordTiming>()
    var currentMatch: MatchResult? = null
    for (nextMatch in RICH_WORD_TIMESTAMP_REGEX.findAll(words)) {
        val prev = currentMatch
        if (prev != null) {
            val textBetween = words.substring(prev.range.last + 1, nextMatch.range.first).trim()
            if (textBetween.isNotEmpty()) {
                wordTimings.add(
                    WordTiming(
                        text = decodeHtmlEntities(textBetween),
                        startTimeMs = parseRichTimestampMs(prev),
                    ),
                )
            }
        }
        currentMatch = nextMatch
    }

    val lastMatch = currentMatch ?: return null
    val trailingText = words.substring(lastMatch.range.last + 1, words.length).trim()
    if (trailingText.isNotEmpty()) {
        wordTimings.add(
            WordTiming(
                text = decodeHtmlEntities(trailingText),
                startTimeMs = parseRichTimestampMs(lastMatch),
            ),
        )
    }

    // If no valid words were parsed, return null (fallback to LINE_SYNCED)
    if (wordTimings.isEmpty()) {
        return null
    }

    // Parse line timing
    val lineStart = lineStartTimeMs.toLongOrNull() ?: 0L
    val lineEnd = lineEndTimeMs.toLongOrNull() ?: Long.MAX_VALUE

    return ParsedRichSyncLine(
        words = wordTimings,
        lineStartTimeMs = lineStart,
        lineEndTimeMs = lineEnd,
    )
}