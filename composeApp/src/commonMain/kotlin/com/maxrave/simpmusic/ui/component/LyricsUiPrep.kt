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
private const val DISPLAY_LINES_CACHE_MAX_ENTRIES = 24
private const val ROMANIZATION_CACHE_MAX_ENTRIES = 16
private const val TRANSLATION_CACHE_MAX_ENTRIES = 16

internal data class DisplayLines(
    val lines: List<Line>,
    val interludeIndices: Set<Int>,
    val plainWords: List<String> = lines.map { it.words.stripRichSyncTimestamps() },
)

private class IdentityListKey(
    val list: List<Line>,
) {
    override fun equals(other: Any?): Boolean = other is IdentityListKey && list === other.list

    override fun hashCode(): Int {
        val first = list.firstOrNull() ?: return 0
        return (list.size * 31 + first.startTimeMs.hashCode()) * 31 + first.words.hashCode()
    }
}

private data class RichSyncCacheKey(
    val words: String,
    val lineStartTimeMs: String,
    val lineEndTimeMs: String,
)

private class CachedRichSync(
    val parsed: ParsedRichSyncLine?,
)

private data class DisplayLinesCacheKey(
    val linesKey: IdentityListKey,
    val syncType: String?,
)

private data class RomanizationCacheKey(
    val linesKey: IdentityListKey,
    val interludeIndices: Set<Int>,
    val languages: Set<RomanizationLanguage>,
    val japaneseReady: Boolean,
)

private data class TranslationCacheKey(
    val originalLinesKey: IdentityListKey,
    val translatedLinesKey: IdentityListKey,
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
        object : LinkedHashMap<DisplayLinesCacheKey, DisplayLines>(24, 0.75f, true) {
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
        if (source.isEmpty()) return DisplayLines(emptyList(), emptySet(), emptyList())
        val key = DisplayLinesCacheKey(IdentityListKey(source), syncType)
        synchronized(displayLinesLock) {
            return displayLinesCache[key]
        }
    }

    fun buildImmediateFallbackDisplayLines(lines: List<Line>?): DisplayLines {
        val source = lines.orEmpty()
        if (source.isEmpty()) return DisplayLines(emptyList(), emptySet(), emptyList())
        return DisplayLines(
            lines = source,
            interludeIndices = emptySet(),
            plainWords = source.map { it.words.stripRichSyncTimestamps() },
        )
    }

    fun getOrBuildDisplayLines(
        lines: List<Line>?,
        syncType: String?,
    ): DisplayLines {
        val source = lines.orEmpty()
        if (source.isEmpty()) return DisplayLines(emptyList(), emptySet(), emptyList())
        val key = DisplayLinesCacheKey(IdentityListKey(source), syncType)
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
        val key = RomanizationCacheKey(IdentityListKey(lines), interludeIndices, languages, japaneseReady)
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
        val key = RomanizationCacheKey(IdentityListKey(lines), interludeIndices, languages, japaneseReady)
        synchronized(romanizationLock) {
            romanizationCache[key]?.let { return it }
        }

        val computed = HashMap<Int, String>(lines.size)
        lines.forEachIndexed { index, line ->
            if (index !in interludeIndices && line.words.isNotBlank()) {
                val source = line.words.stripRichSyncTimestamps()
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
        val key = TranslationCacheKey(IdentityListKey(originalLines), IdentityListKey(translated), thresholdMs)
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
        val key = TranslationCacheKey(IdentityListKey(originalLines), IdentityListKey(translated), thresholdMs)
        synchronized(translationLock) {
            translationCache[key]?.let { return it }
        }
        val built = computeSyncedTranslatedWordsByLineIndex(originalLines, translated, thresholdMs)
        synchronized(translationLock) {
            translationCache[key] = built
        }
        return built
    }

    fun resolveTranslatedLineAt(
        originalLines: List<Line>?,
        translatedLines: List<Line>?,
        lineIndex: Int,
        isSynced: Boolean = true,
    ): String? {
        if (lineIndex < 0 || originalLines.isNullOrEmpty() || translatedLines.isNullOrEmpty()) return null
        if (!isSynced) {
            return translatedLines
                .getOrNull(lineIndex)
                ?.words
                ?.stripRichSyncTimestamps()
                ?.takeIf { it.isNotBlank() }
        }
        return getOrBuildTranslatedLineMap(originalLines, translatedLines)[lineIndex]
    }

    fun warmUp(lyrics: Lyrics) {
        val lines = lyrics.lines ?: return
        val display = getOrBuildDisplayLines(lines, lyrics.syncType)
        if (lyrics.syncType == "RICH_SYNCED") {
            // Also warm LINE_SYNCED shape in case the user has word-by-word highlight disabled.
            getOrBuildDisplayLines(lines, "LINE_SYNCED")
            display.lines.forEach { line ->
                parseRichSyncLineCached(line.words, line.startTimeMs, line.endTimeMs)
            }
        }
    }

    fun warmUpTranslation(
        originalLyrics: Lyrics,
        translatedLyrics: Lyrics,
        thresholdMs: Long = 1000L,
    ) {
        val origLines = originalLyrics.lines ?: return
        val transLines = translatedLyrics.lines ?: return
        // Warm both raw lines (used by mini-previews) and display lines (with interludes for LyricsView).
        getOrBuildTranslatedLineMap(origLines, transLines, thresholdMs)
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
     * Maps each original line index to its closest sanitized translated line within [thresholdMs].
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
                val words = translatedLines[i].words.stripRichSyncTimestamps()
                if (words.isNotEmpty()) {
                    fastResult[i] = words
                }
            }
            return fastResult
        }

        val sortedTranslated =
            translatedLines
                .mapNotNull { line ->
                    val words = line.words.stripRichSyncTimestamps()
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

    private fun buildDisplayLines(
        source: List<Line>,
        syncType: String?,
    ): DisplayLines {
        if (syncType != "RICH_SYNCED" || source.size < 2) {
            return DisplayLines(
                lines = source,
                interludeIndices = emptySet(),
                plainWords = source.map { it.words.stripRichSyncTimestamps() },
            )
        }

        val out = ArrayList<Line>(source.size)
        val outPlain = ArrayList<String>(source.size)
        val inserted = mutableSetOf<Int>()
        source.forEachIndexed { index, line ->
            out += line
            outPlain += line.words.stripRichSyncTimestamps()
            val nextStartMs = source.getOrNull(index + 1)?.startTimeMs?.toLongOrNull() ?: return@forEachIndexed
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
            outPlain += "$INTERLUDE_DOT $INTERLUDE_DOT $INTERLUDE_DOT"
        }
        return DisplayLines(out, inserted, outPlain)
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
        if (minutes > 99) return null
        return "<${twoDigits(minutes)}:${twoDigits((centis / 100) % 60)}.${twoDigits(centis % 100)}>"
    }

    private fun twoDigits(value: Long): String = if (value < 10) "0$value" else value.toString()
}
