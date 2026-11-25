package com.maxrave.simpmusic.extension

import com.maxrave.domain.extension.decodeHtmlEntities

/**
 * Represents a single word with its timing information for rich sync lyrics
 */
data class WordTiming(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long? = null,
)

/**
 * Represents a parsed rich sync line with all word timings
 */
data class ParsedRichSyncLine(
    val words: List<WordTiming>,
    val lineStartTimeMs: Long,
    val lineEndTimeMs: Long,
)

private val RICH_WORD_TIMESTAMP_REGEX = Regex("""<(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?>""")
private val STRAY_BRACKET_TIME_REGEX = Regex("""\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?\]""")
private val STRAY_HTML_TAG_REGEX = Regex("""</?(?:i|b|u|em|strong|span|br|font|ruby|rt|rp)(?:\s+[^>]*)?>""", RegexOption.IGNORE_CASE)
private val STRAY_VOICE_PREFIX_REGEX = Regex("""^\s*(?:v\d+|bg)\s*:\s*""", RegexOption.IGNORE_CASE)

private fun cleanRichWordToken(raw: String): String =
    decodeHtmlEntities(
        raw
            .replace(STRAY_BRACKET_TIME_REGEX, "")
            .replace(STRAY_HTML_TAG_REGEX, "")
            .replace(STRAY_VOICE_PREFIX_REGEX, ""),
    ).trim()

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
 * Expected format: `<MM:SS.mm> word <MM:SS.mm> word ...` or `<MM:SS.mm>word<MM:SS.mm> <MM:SS.mm>word`
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
    if (words.isBlank() || '<' !in words) {
        return null
    }

    val lineStart = lineStartTimeMs.toLongOrNull() ?: 0L
    val lineEnd = lineEndTimeMs.toLongOrNull() ?: Long.MAX_VALUE

    val wordTimings = mutableListOf<WordTiming>()
    var currentMatch: MatchResult? = null
    for (nextMatch in RICH_WORD_TIMESTAMP_REGEX.findAll(words)) {
        val nextTs = parseRichTimestampMs(nextMatch)
        val prev = currentMatch
        if (prev == null) {
            if (nextMatch.range.first > 0) {
                val leading = cleanRichWordToken(words.substring(0, nextMatch.range.first))
                if (leading.isNotEmpty()) {
                    wordTimings.add(
                        WordTiming(
                            text = leading,
                            startTimeMs = lineStart,
                            endTimeMs = nextTs.takeIf { it > lineStart },
                        ),
                    )
                }
            }
        } else {
            val textBetween = cleanRichWordToken(words.substring(prev.range.last + 1, nextMatch.range.first))
            if (textBetween.isNotEmpty()) {
                val wordStart = parseRichTimestampMs(prev)
                wordTimings.add(
                    WordTiming(
                        text = textBetween,
                        startTimeMs = wordStart,
                        endTimeMs = nextTs.takeIf { it > wordStart },
                    ),
                )
            }
        }
        currentMatch = nextMatch
    }

    val lastMatch = currentMatch ?: return null
    val trailingText = cleanRichWordToken(words.substring(lastMatch.range.last + 1, words.length))
    if (trailingText.isNotEmpty()) {
        val wordStart = parseRichTimestampMs(lastMatch)
        wordTimings.add(
            WordTiming(
                text = trailingText,
                startTimeMs = wordStart,
                endTimeMs = lineEnd.takeIf { it != Long.MAX_VALUE && it > wordStart },
            ),
        )
    }

    if (wordTimings.isEmpty()) {
        return null
    }

    return ParsedRichSyncLine(
        words = wordTimings,
        lineStartTimeMs = lineStart,
        lineEndTimeMs = lineEnd,
    )
}