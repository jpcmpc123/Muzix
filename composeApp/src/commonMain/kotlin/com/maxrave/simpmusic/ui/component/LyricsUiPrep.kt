package com.maxrave.simpmusic.ui.component

import com.maxrave.domain.data.model.lyrics.RomanizationLanguage
import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.repository.LyricsRomanizerRepository
import com.maxrave.simpmusic.extension.ParsedRichSyncLine
import com.maxrave.simpmusic.extension.parseRichSyncWords
import com.maxrave.simpmusic.ui.screen.player.content.stripRichSyncTimestamps
import kotlin.math.abs

private const val INTERLUDE_MIN_GAP_MS = 3_000L
private const val INTERLUDE_DOT_COUNT = 3
private const val INTERLUDE_DOT = "\u2022"
private const val RICH_SYNC_CACHE_MAX_ENTRIES = 512
private const val DISPLAY_LINES_CACHE_MAX_ENTRIES = 16
private const val ROMANIZATION_CACHE_MAX_ENTRIES = 16
private const val TRANSLATION_CACHE_MAX_ENTRIES = 16

internal data class DisplayLines(
    val lines: List<Line>,
    val interludeIndices: Set<Int>,
)

private data class RichSyncCacheKey(
    val words: String,
    val lineStartTimeMs: String,
    val lineEndTimeMs: String,
)

private class CachedRichSync(
    val parsed: ParsedRichSyncLine?,
)

private data class DisplayLinesCacheKey(
    val lines: List<Line>,
    val syncType: String?,
)

private data class RomanizationCacheKey(
    val lines: List<Line>,
    val interludeIndices: Set<Int>,
    val syncType: String?,
    val languages: Set<RomanizationLanguage>,
    val japaneseReady: Boolean,
)

private data class TranslationCacheKey(
    val originalLines: List<Line>,
    val translatedLines: List<Line>,
    val thresholdMs: Long,
)

internal object LyricsUiPrep {
    private val richSyncLock = Any()
    private val richSyncCache =
        object : LinkedHashMap<RichSyncCacheKey, CachedRichSync>(128, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<RichSyncCacheKey, CachedRichSync>?,
            ): Boolean = size > RICH_SYNC_CACHE_MAX_ENTRIES
        }

    private val displayLinesLock = Any()
    private val displayLinesCache =
        object : LinkedHashMap<DisplayLinesCacheKey, DisplayLines>(16, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<DisplayLinesCacheKey, DisplayLines>?,
            ): Boolean = size > DISPLAY_LINES_CACHE_MAX_ENTRIES
        }

    private val romanizationLock = Any()
    private val romanizationCache =
        object : LinkedHashMap<RomanizationCacheKey, Map<Int, String>>(16, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<RomanizationCacheKey, Map<Int, String>>?,
            ): Boolean = size > ROMANIZATION_CACHE_MAX_ENTRIES
        }

    private val translationLock = Any()
    private val translationCache =
        object : LinkedHashMap<TranslationCacheKey, Map<Int, String>>(16, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<TranslationCacheKey, Map<Int, String>>?,
            ): Boolean = size > TRANSLATION_CACHE_MAX_ENTRIES
        }

    fun parseRichSyncLineCached(
        words: String,
        lineStartTimeMs: String,
        lineEndTimeMs: String,
    ): ParsedRichSyncLine? {
        if (words.isBlank() || '<' !in words) return null
        val key = RichSyncCacheKey(words, lineStartTimeMs, lineEndTimeMs)
        synchronized(richSyncLock) {
            richSyncCache[key]?.let { return it.parsed }
        }
        val parsed = parseRichSyncWords(words, lineStartTimeMs, lineEndTimeMs)
        synchronized(richSyncLock) {
            richSyncCache[key] = CachedRichSync(parsed)
        }
        return parsed
    }

    fun peekDisplayLines(
        lines: List<Line>?,
        syncType: String?,
    ): DisplayLines? {
        val source = lines.orEmpty()
        if (syncType != "RICH_SYNCED" || source.size < 2) {
            return DisplayLines(source, emptySet())
        }
        val key = DisplayLinesCacheKey(source, syncType)
        synchronized(displayLinesLock) {
            return displayLinesCache[key]
        }
    }

    fun getOrBuildDisplayLines(
        lines: List<Line>?,
        syncType: String?,
    ): DisplayLines {
        val source = lines.orEmpty()
        if (syncType != "RICH_SYNCED" || source.size < 2) {
            return DisplayLines(source, emptySet())
        }
        val key = DisplayLinesCacheKey(source, syncType)
        synchronized(displayLinesLock) {
            displayLinesCache[key]?.let { return it }
        }
        val built = buildDisplayLines(source, syncType)
        synchronized(displayLinesLock) {
            displayLinesCache[key] = built
        }
        return built
    }

    fun peekRomanizedLines(
        lines: List<Line>,
        interludeIndices: Set<Int>,
        syncType: String?,
        languages: Set<RomanizationLanguage>,
        japaneseReady: Boolean,
    ): Map<Int, String>? {
        if (languages.isEmpty() || lines.isEmpty()) return emptyMap()
        val key = RomanizationCacheKey(lines, interludeIndices, syncType, languages, japaneseReady)
        synchronized(romanizationLock) {
            return romanizationCache[key]
        }
    }

    fun getOrComputeRomanizedLines(
        lines: List<Line>,
        interludeIndices: Set<Int>,
        syncType: String?,
        languages: Set<RomanizationLanguage>,
        japaneseReady: Boolean,
        romanizer: LyricsRomanizerRepository,
    ): Map<Int, String> {
        if (languages.isEmpty() || lines.isEmpty()) return emptyMap()
        val key = RomanizationCacheKey(lines, interludeIndices, syncType, languages, japaneseReady)
        synchronized(romanizationLock) {
            romanizationCache[key]?.let { return it }
        }

        val isRichSynced = syncType == "RICH_SYNCED"
        val computed = HashMap<Int, String>(lines.size)
        lines.forEachIndexed { index, line ->
            if (index !in interludeIndices && line.words.isNotBlank()) {
                val source = if (isRichSynced) line.words.stripRichSyncTimestamps() else line.words
                val romanized = romanizer.romanize(source, languages)
                if (!romanized.isNullOrBlank()) {
                    computed[index] = romanized
                }
            }
        }
        synchronized(romanizationLock) {
            romanizationCache[key] = computed
        }
        return computed
    }

    fun peekTranslatedLineMap(
        originalLines: List<Line>,
        translatedLines: List<Line>?,
        thresholdMs: Long = 1000L,
    ): Map<Int, String>? {
        val translated = translatedLines.orEmpty()
        if (originalLines.isEmpty() || translated.isEmpty()) return emptyMap()
        val key = TranslationCacheKey(originalLines, translated, thresholdMs)
        synchronized(translationLock) {
            return translationCache[key]
        }
    }

    fun getOrBuildTranslatedLineMap(
        originalLines: List<Line>,
        translatedLines: List<Line>?,
        thresholdMs: Long = 1000L,
    ): Map<Int, String> {
        val translated = translatedLines.orEmpty()
        if (originalLines.isEmpty() || translated.isEmpty()) return emptyMap()
        val key = TranslationCacheKey(originalLines, translated, thresholdMs)
        synchronized(translationLock) {
            translationCache[key]?.let { return it }
        }
        val built = computeSyncedTranslatedWordsByLineIndex(originalLines, translated, thresholdMs)
        synchronized(translationLock) {
            translationCache[key] = built
        }
        return built
    }

    fun warmUp(lyrics: Lyrics) {
        val lines = lyrics.lines ?: return
        if (lyrics.syncType != "RICH_SYNCED") return
        val display = getOrBuildDisplayLines(lines, lyrics.syncType)
        display.lines.forEach { line ->
            parseRichSyncLineCached(line.words, line.startTimeMs, line.endTimeMs)
        }
    }

    fun warmUpTranslation(
        originalLyrics: Lyrics,
        translatedLyrics: Lyrics,
        thresholdMs: Long = 1000L,
    ) {
        val origLines = originalLyrics.lines ?: return
        val transLines = translatedLyrics.lines ?: return
        val display = getOrBuildDisplayLines(origLines, originalLyrics.syncType)
        getOrBuildTranslatedLineMap(display.lines, transLines, thresholdMs)
    }

    fun clearCachesForTest() {
        synchronized(richSyncLock) { richSyncCache.clear() }
        synchronized(displayLinesLock) { displayLinesCache.clear() }
        synchronized(romanizationLock) { romanizationCache.clear() }
        synchronized(translationLock) { translationCache.clear() }
    }

    /**
     * Maps each original line index to its closest translated line within [thresholdMs].
     *
     * Fast path (`O(N)`, zero sorting): when both lists have the exact same size and every entry's
     * `startTimeMs` matches 1-to-1 (always true for Free/AI translations), maps directly in one
     * pass. Falls back to the deterministic two-pointer alignment for community translations whose
     * line counts or timestamps differ. Blank translated lines are omitted so Compose skips
     * composing empty subtitle rows.
     */
    internal fun computeSyncedTranslatedWordsByLineIndex(
        originalLines: List<Line>,
        translatedLines: List<Line>,
        thresholdMs: Long = 1000L,
    ): Map<Int, String> {
        if (originalLines.isEmpty() || translatedLines.isEmpty()) return emptyMap()

        if (originalLines.size == translatedLines.size && isAlignedOneToOne(originalLines, translatedLines, thresholdMs)) {
            val fastResult = HashMap<Int, String>(originalLines.size)
            for (i in originalLines.indices) {
                val words = translatedLines[i].words.trim()
                if (words.isNotEmpty()) {
                    fastResult[i] = words
                }
            }
            return fastResult
        }

        val sortedTranslated =
            translatedLines
                .mapNotNull { line ->
                    val words = line.words.trim()
                    if (words.isEmpty()) return@mapNotNull null
                    val ts = line.startTimeMs.toLongOrNull() ?: return@mapNotNull null
                    ts to words
                }.sortedBy { it.first }

        if (sortedTranslated.isEmpty()) return emptyMap()

        data class OriginalEntry(
            val index: Int,
            val ts: Long,
        )

        val sortedOriginal =
            originalLines
                .mapIndexedNotNull { index, line ->
                    val ts = line.startTimeMs.toLongOrNull() ?: return@mapIndexedNotNull null
                    OriginalEntry(index, ts)
                }.sortedBy { it.ts }

        if (sortedOriginal.isEmpty()) return emptyMap()

        val result = HashMap<Int, String>(sortedOriginal.size)
        var j = 0
        for (orig in sortedOriginal) {
            while (j + 1 < sortedTranslated.size && sortedTranslated[j + 1].first <= orig.ts) {
                j++
            }
            val candA = sortedTranslated[j]
            var bestWords = candA.second
            var bestDiff = abs(candA.first - orig.ts)
            if (j + 1 < sortedTranslated.size) {
                val candB = sortedTranslated[j + 1]
                val diffB = abs(candB.first - orig.ts)
                if (diffB < bestDiff) {
                    bestWords = candB.second
                    bestDiff = diffB
                }
            }
            if (bestDiff < thresholdMs) {
                result[orig.index] = bestWords
            }
        }
        return result
    }

    private fun isAlignedOneToOne(
        originalLines: List<Line>,
        translatedLines: List<Line>,
        thresholdMs: Long,
    ): Boolean {
        for (i in originalLines.indices) {
            val origStart = originalLines[i].startTimeMs
            val transStart = translatedLines[i].startTimeMs
            if (origStart == transStart) continue
            val origMs = origStart.toLongOrNull() ?: return false
            val transMs = transStart.toLongOrNull() ?: return false
            if (abs(origMs - transMs) >= thresholdMs) return false
        }
        return true
    }

    /**
     * Inserts a dots line into every silence of at least [INTERLUDE_MIN_GAP_MS] between two lines.
     *
     * The inserted line is an ordinary rich-synced [Line] carrying three dots, one timestamp each,
     * dividing the silence into equal thirds. Written as real markup rather than handled by a special
     * case in the renderer: it then goes through the same parser, the same wipe and the same layout as
     * a sung line, which is the only way its dots land on the same baseline and left edge as the words
     * above them.
     *
     * Rich sync only. It is the one format that records where a line STOPS, so it is the one format
     * that can tell a real pause from a line whose end was simply never written down.
     */
    private fun buildDisplayLines(
        source: List<Line>,
        syncType: String?,
    ): DisplayLines {
        if (syncType != "RICH_SYNCED" || source.size < 2) return DisplayLines(source, emptySet())

        val out = ArrayList<Line>(source.size)
        val inserted = mutableSetOf<Int>()
        source.forEachIndexed { index, line ->
            out += line
            val nextStartMs = source.getOrNull(index + 1)?.startTimeMs?.toLongOrNull() ?: return@forEachIndexed
            // The rule, literally: once the line's LAST WORD has been lit for INTERLUDE_MIN_GAP_MS and
            // the next line still has not begun, the dots take over from there.
            //
            // Measured from where that word STARTS, not where it ends, and the dots begin three seconds
            // later rather than at the end of the line. Both of those matter for the same reason: the
            // dots become the sung line the moment they start, so starting them any earlier drags the
            // sheet away from a word the listener is still watching light up. For those three seconds
            // the last lyric stays the sung line, which is what it still is.
            val lastWordStartMs =
                parseRichSyncLineCached(line.words, line.startTimeMs, line.endTimeMs)
                    ?.words
                    ?.lastOrNull()
                    ?.startTimeMs
                    ?: return@forEachIndexed
            val dotsStartMs = lastWordStartMs + INTERLUDE_MIN_GAP_MS
            if (nextStartMs <= dotsStartMs) return@forEachIndexed
            val interlude = interludeLine(dotsStartMs, nextStartMs) ?: return@forEachIndexed
            inserted += out.size
            out += interlude
        }
        return DisplayLines(out, inserted)
    }

    /** Null when a timestamp will not fit the `MM:SS.cc` shape the rich-sync parser reads. */
    private fun interludeLine(
        startMs: Long,
        endMs: Long,
    ): Line? {
        val stepMs = (endMs - startMs) / INTERLUDE_DOT_COUNT
        val words =
            buildString {
                repeat(INTERLUDE_DOT_COUNT) { dot ->
                    append(richSyncTimestamp(startMs + stepMs * dot) ?: return null)
                    append(INTERLUDE_DOT)
                }
            }
        return Line(
            startTimeMs = startMs.toString(),
            endTimeMs = endMs.toString(),
            syllables = null,
            words = words,
        )
    }

    private fun richSyncTimestamp(ms: Long): String? {
        val centis = ms / 10
        val minutes = centis / 6000
        // The parser's regex reads two digits of minutes for interludes.
        if (minutes > 99) return null
        return "<${twoDigits(minutes)}:${twoDigits((centis / 100) % 60)}.${twoDigits(centis % 100)}>"
    }

    private fun twoDigits(value: Long): String = if (value < 10) "0$value" else value.toString()
}
