package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Animatable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.data.model.lyrics.RomanizationDictionaryState
import com.maxrave.domain.data.model.lyrics.RomanizationLanguage
import com.maxrave.domain.repository.LyricsRomanizerRepository
import com.maxrave.simpmusic.expect.ui.isLyricsBlurSupported
import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentActions
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentState
import org.koin.compose.koinInject
import androidx.navigation.NavController
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.simpmusic.extension.LyricsStagger
import com.maxrave.simpmusic.extension.ParsedRichSyncLine
import com.maxrave.simpmusic.extension.animateScrollAndAnchorItemTop
import com.maxrave.simpmusic.extension.animateScrollAndCentralizeItem
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LyricsTranslationUiState
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import java.text.Bidi
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.min
import kotlin.math.max

private const val TAG = "LyricsView"

// The interval the player publishes its position on (MediaServiceHandlerImpl ticks on up to 100ms,
// JvmMediaPlayerHandlerImpl ticks on 50ms). It is the ceiling on how far the interpolated playhead
// below is allowed to run ahead of the last real tick, so a stalled tick — paused playback, a
// wedged player — can never drift further than the granularity we already live with.
private const val PLAYHEAD_TICK_MS = 100L
private const val PLAYHEAD_CEILING_MS = 180L
internal const val PLAYHEAD_SEEK_BACK_THRESHOLD_MS = 400L
internal const val PLAYHEAD_SEEK_FORWARD_THRESHOLD_MS = 600L
internal const val MIN_VISUAL_WORD_SWEEP_MS = 85L

// Emphasis maths ported from AMLL (applemusic-like-lyrics),
// packages/core/src/lyric-player/dom/lyric-line.ts -> initEmphasizeAnimation. Kept to its exact
// constants rather than re-derived: the shape of these curves IS the effect.
//
// The key part is the exponent switch. Below the reference duration the strength is CUBED, so a
// half-second word gets almost nothing; above it the strength is a SQUARE ROOT, so a very long
// note grows but flattens out. A linear ramp — which is what a first guess produces — makes every
// ordinary word shimmer and every held note underwhelming, i.e. exactly backwards.
private const val EMP_AMOUNT_REF_MS = 2000f
private const val EMP_BLUR_REF_MS = 3000f
private const val EMP_MIN_DURATION_MS = 1000f
private const val EMP_AMOUNT_GAIN = 0.6f
private const val EMP_BLUR_GAIN = 0.5f
private const val EMP_AMOUNT_CAP = 1.2f
private const val EMP_BLUR_CAP = 0.8f
// AMLL leans on the LAST word of a line — that is where a singer holds, so it earns extra.
private const val EMP_LAST_WORD_AMOUNT = 1.6f
private const val EMP_LAST_WORD_BLUR = 1.5f
// Both are fractions of the font size (AMLL expresses them in `em`), never fixed dp.
private const val EMP_SCALE_EM = 0.1f
private const val EMP_RISE_EM = 0.025f
private const val EMP_GLOW_RADIUS_EM = 0.3f

// The bloom every sung word carries; a held word replaces this with AMLL's larger emphasis glow.
// The flare on the word currently being sung. Bright, because it marks one word out of a line
// rather than washing over the whole sung half.
// The provider caption under the lyrics: quiet enough to sit beneath them, still readable on
// purpose — it is dimmed, never blurred.
// How far the travelling glow reaches, in characters. Below ~1 the light goes out between
// characters; far above it the whole word lifts at once.
private const val FLARE_REACH_CHARS = 3.5f

// How long a word's flare takes to fade in as it becomes the sung one, and to fade out again once
// the playhead has left it. Without this the flare was gated by a hard boolean, so the light died
// at every word boundary and was reborn on the next word — smooth WITHIN a word, broken BETWEEN
// them. Fading the gate makes the outgoing word's last characters still be glowing while the
// incoming word's first ones light up, and the two overlapping reads as one travelling light.
//
// Roughly one sung word long. Much shorter and the hand-over is not visible; much longer and half
// the line glows at once, which is a smear rather than a light.
private const val FLARE_FADE_MS = 2_500

// ...and how long it takes to come UP, which must be its own number. One tween served both
// directions until the release was lengthened for the wake, and that silently made the attack just
// as long: a word starting to be sung took two and a half seconds to reach full brightness, so the
// glow arrived long after the character had finished rising. Neighbouring words sat at different
// points on that slow climb, which is what read as flicker rather than one travelling light.
private const val FLARE_ATTACK_MS = 500

// Distance, in characters, over which the wake behind the playhead falls to about a third of full
// brightness. The decay is EXPONENTIAL, not linear, and that distinction is the whole point: a long
// linear ramp does not make a tail, it makes the whole word evenly bright, because the first
// character of a four-letter word still sits at ninety percent of it. An exponential keeps the
// light concentrated on the character being sung, drops away sharply behind it, and leaves a faint
// glow trailing for a long way — which is what a wake looks like.
private const val FLARE_TAIL_CHARS = 2.5f

// How far the character under the light lifts, as a fraction of the font size — Apple nudges the
// syllable being sung upward, and the eye reads the lift as the light having weight.
//
// Driven by the flare itself rather than by a second animation, so the rise travels with the light
// instead of chasing it. It is a graphicsLayer translation, which happens at DRAW time: the glyph
// moves without the line being measured again, so nothing reflows and no line ever changes height.
// No alpha goes through that layer — alpha below 1 would force an offscreen buffer the size of the
// glyph and slice off the very glow this is meant to accompany.
private const val CHAR_RISE_EM = 0.065f

// How long one character takes to travel from its resting line to the top, once the light has
// touched it. Deliberately longer than a character is sung for: the lift is NOT finished before the
// next character starts its own, so at any instant a stretch of the line is caught at different
// heights and the whole thing reads as a wave rolling behind the singing.
//
// This is why the lift cannot be computed from the word's progress. That number stops advancing the
// moment the word is done, so a word sung in less than this would freeze its characters half-risen.
// Each character needs a clock of its own, started when the light arrives and left to finish on its
// own time.
private const val CHAR_RISE_MS = 1_000

private const val FOOTER_ALPHA = 0.45f

private const val SUNG_BASE_GLOW_ALPHA = 0.95f
private const val SUNG_BASE_GLOW_EM = 0.26f


// makeEmpEasing(0.5): bezIn up to the midpoint, then 1 - bezOut back down — a bump that rises
// faster than it falls, which is why it reads as a voice pushing rather than a sine wobble.
private val EmpBezIn = CubicBezierEasing(0.2f, 0.4f, 0.58f, 1f)
private val EmpBezOut = CubicBezierEasing(0.3f, 0f, 0.58f, 1f)

private fun empEasing(x: Float): Float =
    if (x < 0.5f) {
        EmpBezIn.transform((x / 0.5f).coerceIn(0f, 1f))
    } else {
        1f - EmpBezOut.transform(((x - 0.5f) / 0.5f).coerceIn(0f, 1f))
    }

// Repeated lyrics palette tokens hoisted to file scope: avoids re-allocating
// the same Color() objects on every recomposition of every line item.
private val DimOriginalColor = Color.LightGray.copy(alpha = 0.35f)
private val ClassicTranslatedColor = Color.White.copy(alpha = 0.86f)
private val ClassicDimTranslatedColor = Color.White.copy(alpha = 0.42f)

// A pronunciation guide sits BETWEEN the original and its translation in meaning, so it does the
// same visually: neutral grey rather than the translation's yellow, dimmer than the original it
// belongs to. Two values because the sung line lifts everything on it.
private val DimRomanizedCurrentColor = Color.White.copy(alpha = 0.7f)
private val DimRomanizedColor = Color.LightGray.copy(alpha = 0.3f)
private val DimRichPendingColor = Color.LightGray.copy(alpha = 0.6f)

internal data class TimedLineIndex(
    val index: Int,
    val startTimeMs: Long,
)

/**
 * Returns the original line index of the last [TimedLineIndex] whose [TimedLineIndex.startTimeMs]
 * is `<= nowMs`. Assumes the receiver is sorted ascending by [TimedLineIndex.startTimeMs].
 *
 * Rules:
 *  - empty list -> -1
 *  - nowMs strictly before the first start time -> -1
 *  - nowMs after the last start time -> the last entry's original index (sticky last line)
 */
internal fun List<TimedLineIndex>.activeIndexAt(nowMs: Long): Int {
    if (isEmpty()) return -1
    if (nowMs < first().startTimeMs) return -1
    // Binary search for the last item whose startTimeMs <= nowMs.
    var lo = 0
    var hi = size - 1
    var ans = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (this[mid].startTimeMs <= nowMs) {
            ans = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return if (ans >= 0) this[ans].index else -1
}

@Composable
fun LyricsView(
    lyricsData: NowPlayingScreenData.LyricsData,
    timeLine: StateFlow<TimeLine>,
    // Whether the player is actually playing. The rich-sync playhead's frame loop gates on it:
    // paused, the loop stops instead of spinning at display rate over a frozen position.
    // Default true keeps any caller that does not thread it behaving exactly as before.
    isPlaying: Boolean = true,
    onLineClick: (Float) -> Unit,
    modifier: Modifier = Modifier,
    showScrollShadows: Boolean = false,
    backgroundColor: Color = Color(0xFF242424),
    // Optional trailing slot rendered as the LAST list item, so a caller's caption scrolls with
    // the lyrics instead of sitting anchored below them. Null by default: every existing caller
    // renders exactly as before.
    footerContent: (@Composable () -> Unit)? = null,
    // Optional isolated translation visibility flow: when provided, toggling translation recomposes
    // only LyricsView and the translate button, and short-circuits translated rows when hidden.
    translationStateFlow: StateFlow<LyricsTranslationUiState>? = null,
    dataStoreManager: DataStoreManager = koinInject(),
    romanizer: LyricsRomanizerRepository = koinInject(),
) {
    val listState = rememberLazyListState()
    // AMLL drops blur to zero while the user is scrolling by hand (resolveBlurLevel:
    // `if (this.scrollState.isTouchScrolled || isFocused) return 0`) — you are reading ahead at
    // that moment, and out-of-focus text is not readable. Dragged, not isScrollInProgress: the
    // latter is also true for the player's own animated scroll, which must stay blurred.
    val isDragging by listState.interactionSource.collectIsDraggedAsState()
    val timeLineState = timeLine.collectAsStateWithLifecycle()
    val isTranslationVisible =
        if (translationStateFlow != null) {
            translationStateFlow.collectAsStateWithLifecycle().value.isVisible
        } else {
            true
        }

    // The listener's audio-delay correction, applied HERE rather than to the lyric rows: the rows
    // are cached in Room and shared with the community lyrics database, so a local correction must
    // never be written into them. Reading it at this end also means a change lands on the next
    // frame instead of the next track.
    //
    // SUBTRACTED. Bluetooth buffers, so at player position P the ear is hearing P - offset; that
    // earlier moment is the one the sheet must light up for. Early in a track this can go negative,
    // which the `now <= 0L` guard below already reads as "no line yet" — correct, since in heard
    // time the song has not reached its first line.
    val lyricsOffsetMs by dataStoreManager.lyricsOffsetMs.collectAsStateWithLifecycle(0)
    val currentTimeMsState =
        remember {
            derivedStateOf { timeLineState.value.current - lyricsOffsetMs }
        }

    // Read here rather than taken as a parameter: all four call sites (the fullscreen sheet and
    // the three player styles) want the user's one choice, so making them each thread it through
    // would be four copies of the same lookup. Re-checked against isLyricsBlurSupported() even
    // though Settings hides the option below Android 12 — a DataStore restored from a backup, or
    // carried to another device, can still hold APPLE_MUSIC on a phone that cannot draw it.
    val lyricsStyle by dataStoreManager.lyricsStyle.collectAsStateWithLifecycle(DataStoreManager.LYRICS_STYLE_CLASSIC)
    val appleStyle = lyricsStyle == DataStoreManager.LYRICS_STYLE_APPLE_MUSIC && isLyricsBlurSupported()

    // Read here for the same reason the style is: all four call sites want the user's one choice,
    // so threading it through each of them would be four copies of the same lookup. The stored
    // value is a comma-separated list of enum names; parsing it per RECOMPOSITION would rebuild the
    // set on every frame of the sung-line animation, hence the remember on the raw string.
    val romanizationStored by dataStoreManager.romanizationLanguages.collectAsStateWithLifecycle("")
    val romanizationLanguages =
        remember(romanizationStored) { RomanizationLanguage.parse(romanizationStored) }
    // Lyrics on screen with Japanese selected is when the dictionary pack is needed, so a missing
    // one is fetched from here: after a reinstall that restored a backup the selection is back but
    // the pack is not, and before this only the settings dialog ever started a download. Keyed on
    // the lyrics too, so a failed attempt is tried again on a later song (the repository spaces
    // those retries out).
    LaunchedEffect(romanizationLanguages, lyricsData.lyrics) {
        if (RomanizationLanguage.JAPANESE in romanizationLanguages) romanizer.ensureJapaneseDictionary()
    }
    // A Japanese line read before the pack is on disk comes back null, and the remember on each
    // line would keep that null for as long as the line stays on screen. Keyed on this, the
    // visible lines are read again the moment the pack lands — usually with these lyrics open,
    // since the effect above is what started the download. Derived, so the other state changes
    // (downloading, failed) do not touch the lines at all.
    val japaneseDictionaryState by romanizer.japaneseDictionaryState.collectAsStateWithLifecycle()
    val japaneseDictionaryReady by remember {
        derivedStateOf { japaneseDictionaryState == RomanizationDictionaryState.READY }
    }

    // One text row plus the padding that separates two lyric items — the exact amount of the
    // previous line that stays on screen above the sung one.
    val exposedRowPx =
        with(LocalDensity.current) {
            AppleMusicLyricLineHeight.toPx() + AppleMusicLyricGap.toPx()
        }

    // The lines this sheet actually renders: the sheet's own, plus a dots line inserted into every
    // long silence. Everything below counts indices against THIS list — the sung line, the blur
    // distance, the scroll target and the translation lookup — so the dots line is a line like any
    // other rather than something drawn on top of one.
    val displayLines by produceState(
        initialValue =
            LyricsUiPrep.peekDisplayLines(lyricsData.lyrics.lines, lyricsData.lyrics.syncType)
                ?: DisplayLines(lyricsData.lyrics.lines.orEmpty(), emptySet()),
        key1 = lyricsData.lyrics.lines,
        key2 = lyricsData.lyrics.syncType,
    ) {
        val cached = LyricsUiPrep.peekDisplayLines(lyricsData.lyrics.lines, lyricsData.lyrics.syncType)
        value =
            cached
                ?: withContext(Dispatchers.Default) {
                    LyricsUiPrep.getOrBuildDisplayLines(lyricsData.lyrics.lines, lyricsData.lyrics.syncType)
                }
    }

    val romanizedByLineIndex by produceState<Map<Int, String>>(
        initialValue =
            LyricsUiPrep.peekRomanizedLines(
                lines = displayLines.lines,
                interludeIndices = displayLines.interludeIndices,
                syncType = lyricsData.lyrics.syncType,
                languages = romanizationLanguages,
                japaneseReady = japaneseDictionaryReady,
            ) ?: emptyMap(),
        key1 = displayLines,
        key2 = romanizationLanguages to lyricsData.lyrics.syncType,
        key3 = japaneseDictionaryReady,
    ) {
        value =
            if (romanizationLanguages.isEmpty()) {
                emptyMap()
            } else {
                withContext(Dispatchers.Default) {
                    LyricsUiPrep.getOrComputeRomanizedLines(
                        lines = displayLines.lines,
                        interludeIndices = displayLines.interludeIndices,
                        syncType = lyricsData.lyrics.syncType,
                        languages = romanizationLanguages,
                        japaneseReady = japaneseDictionaryReady,
                        romanizer = romanizer,
                    )
                }
            }
    }

    val timedLineIndexes =
        remember(displayLines) {
            val timed =
                displayLines.lines
                    .mapIndexedNotNull { index, line ->
                        line.startTimeMs.toLongOrNull()?.let { TimedLineIndex(index, it) }
                    }
            // An unsynced sheet still carries a startTimeMs on every line — the literal "0", on all
            // 307 unsynced rows of the author's own library. Those parse perfectly well, so this
            // list came out full of zeros and [activeIndexAt], asked for "the last line at or before
            // now", answered with the LAST LINE OF THE SONG from the first second onward. Every
            // other line then sat at a huge distance from it and blurred to maximum: the whole
            // sheet unreadable, with the one sharp line parked off the bottom of the screen.
            //
            // Tested on the timestamps rather than on syncType because it is the timestamps the
            // search actually reads: one distinct value cannot order anything, whatever the sheet
            // calls itself. With the list empty, currentLineIndex stays -1, and the renderer's
            // no-active-line branch dims every line uniformly and blurs none of them.
            if (timed.distinctBy { it.startTimeMs }.size <= 1) {
                emptyList()
            } else {
                timed.sortedBy { it.startTimeMs }
            }
        }

    val currentLineIndex by remember(timedLineIndexes) {
        derivedStateOf {
            val now = timeLineState.value.current - lyricsOffsetMs
            if (now <= 0L) -1 else timedLineIndexes.activeIndexAt(now)
        }
    }

    // Read off the SAME list the blur fix built, not off syncType: it is the timestamps that decide
    // whether a line can ever be "the sung one", and [timedLineIndexes] is already empty exactly
    // when they cannot order anything. Every line then renders as the current line — white, fully
    // opaque, unblurred — because a sheet with no sung line has no line to contrast one against.
    // Distinct from a SYNCED sheet's pre-roll, where currentLineIndex is also -1 but a sung line is
    // on its way and PRE_ROLL_LINE_ALPHA deliberately keeps the page dimmer than it.
    val allLinesCurrent = timedLineIndexes.isEmpty()

    val visibleTranslatedLines = if (isTranslationVisible) lyricsData.translatedLyrics?.first?.lines else null
    val syncedTranslatedWordsByLineIndex by
        produceState(
            initialValue =
                if (visibleTranslatedLines.isNullOrEmpty()) {
                    emptyMap()
                } else {
                    LyricsUiPrep.peekTranslatedLineMap(displayLines.lines, visibleTranslatedLines) ?: emptyMap()
                },
            key1 = displayLines,
            key2 = visibleTranslatedLines,
        ) {
            if (visibleTranslatedLines.isNullOrEmpty()) {
                value = emptyMap()
                return@produceState
            }
            val cached = LyricsUiPrep.peekTranslatedLineMap(displayLines.lines, visibleTranslatedLines)
            if (cached != null) {
                value = cached
                return@produceState
            }
            value =
                withContext(Dispatchers.Default) {
                    LyricsUiPrep.getOrBuildTranslatedLineMap(displayLines.lines, visibleTranslatedLines)
                }
        }
    // The Apple Music handover: rows set off one behind another instead of as one block. Per sheet,
    // so a new song's first line counts as the next line rather than as a jump.
    val stagger = remember(displayLines) { LyricsStagger() }

    LaunchedEffect(currentLineIndex, lyricsData.lyrics.syncType, appleStyle, stagger) {
        if (!isDragging &&
            currentLineIndex > -1 &&
            (lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED")
        ) {
            if (appleStyle) {
                // NEAR the top, not against it: Apple leaves exactly ONE physical row of the
                // previous lyric visible above the line being sung. Scrolling to `index - 1`
                // instead — which is what this did first — anchors the whole previous ITEM, and a
                // lyric that wraps is one item spanning two or three rows, so the entire wrapped
                // block hung above the sung line. Anchoring the sung line itself and backing off by
                // one row's height is row-accurate no matter how the previous line wrapped.
                listState.animateScrollAndAnchorItemTop(currentLineIndex, -exposedRowPx, stagger)
            } else {
                listState.animateScrollAndCentralizeItem(currentLineIndex)
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        // Apple keeps the sung line at the TOP even when it is the last line of the song — which
        // is only possible if there is empty space below it to scroll into. Without this tail the
        // list simply runs out of content and the closing lines pile up against the bottom edge,
        // so the final third of every song reads bottom-anchored instead of top-anchored.
        val tailPadding = if (appleStyle) maxHeight * 0.72f else 0.dp
        val baseBodyMedium = typo().bodyMedium
        val appleSubLineStyle =
            remember(appleStyle, baseBodyMedium) {
                if (appleStyle) {
                    baseBodyMedium.copy(
                        fontSize = AppleMusicSubLineFontSize,
                        lineHeight = AppleMusicSubLineHeight,
                    )
                } else {
                    null
                }
            }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = tailPadding),
        ) {
            items(
                count = displayLines.lines.size,
                // A Line has no id, and its startTimeMs alone is not unique — an unsynced sheet
                // carries "0" on every row and the inserted interlude lines share their
                // neighbours' times — so the key pairs the line's own start time with its index.
                // The display list is built once per sheet and never reordered, so the pair is
                // stable for the list's whole life.
                key = { index -> "${displayLines.lines[index].startTimeMs}_$index" },
            ) { index ->
                val line = displayLines.lines.getOrNull(index)
                // A dots line stands for silence, so it has nothing to translate or romanize. Worth
                // saying out loud for the translation: that map matches by TIME, and the silence
                // begins close enough to the line before it that it would otherwise pick up that
                // line's translation and print it under the dots.
                val isInterlude = index in displayLines.interludeIndices
                // Translated lyrics: synced -> precomputed map by line index, unsynced -> by index.
                val translatedWords =
                    when {
                        !isTranslationVisible || isInterlude -> null
                        lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED" ->
                            syncedTranslatedWordsByLineIndex[index]
                        else ->
                            visibleTranslatedLines
                                ?.getOrNull(index)
                                ?.words
                                ?.takeIf { it.isNotBlank() }
                    }

                line?.words?.let { words ->
                    // Signed distance from the line being sung. The shell is the only place that
                    // knows it, and it is the ONLY extra input the Apple Music renderer needs —
                    // which is why the split lives here and not inside the line items.
                    val distanceFromCurrent = if (currentLineIndex < 0) 0 else index - currentLineIndex

                    // Romanization is its OWN row, between the original and the translation, so the
                    // reader can follow the original script and still know how to pronounce it.
                    //
                    // Being a separate row is also what makes rich sync painless: the original
                    // keeps its <mm:ss.xx> markers untouched and goes on lighting up word by word,
                    // while this row is plain static text. It only has to have the markers STRIPPED
                    // before romanizing, or the timestamps themselves would be transliterated.
                    val romanizedWords =
                        if (isInterlude || romanizationLanguages.isEmpty()) {
                            null
                        } else {
                            romanizedByLineIndex[index]
                        }

                    val renderLine: @Composable () -> Unit = {
                        when {
                            // Rich sync: parse and use RichSyncLyricsLineItem
                            lyricsData.lyrics.syncType == "RICH_SYNCED" -> {
                                val parsedLine =
                                    remember(words, line.startTimeMs, line.endTimeMs) {
                                        LyricsUiPrep.parseRichSyncLineCached(words, line.startTimeMs, line.endTimeMs)
                                    }

                                if (parsedLine != null) {
                                    // Reused verbatim by BOTH styles: word-by-word highlighting is
                                    // already what Apple does with a rich-synced line, so there is
                                    // nothing to re-implement — only the focus treatment differs,
                                    // and that is applied by the wrapper below.
                                    RichSyncLyricsLineItem(
                                        parsedLine = parsedLine,
                                        translatedWords = translatedWords,
                                        romanizedWords = romanizedWords,
                                        currentTimeMs = 0L,
                                        isCurrent = index == currentLineIndex,
                                        isPlaying = isPlaying,
                                        customFontSize = if (appleStyle) AppleMusicLyricFontSize else null,
                                        glow = if (appleStyle && index == currentLineIndex) AppleMusicActiveLineGlow else null,
                                        pendingColorOverride = if (appleStyle) AppleMusicPendingWordColor else null,
                                        translatedColorOverride = if (appleStyle) AppleMusicTranslatedColor else null,
                                        translatedStyleOverride = appleSubLineStyle,
                                        customPadding = if (appleStyle) AppleMusicLyricGap else 12.dp,
                                        wrappedLineSpacing = if (appleStyle) AppleMusicWrappedLineSpacing else 0.dp,
                                        // Apple takes its click from the wrapper Column below, which
                                        // covers the translation too; a click here as well would put
                                        // a second, smaller target on top of it.
                                        modifier =
                                            if (appleStyle) {
                                                Modifier
                                            } else {
                                                Modifier.clickable {
                                                    val totalMs = timeLine.value.total
                                                    val targetMs = line.startTimeMs.toLongOrNull()
                                                    if (totalMs > 0L && targetMs != null) {
                                                        onLineClick((targetMs.toFloat() * 100f / totalMs.toFloat()).coerceIn(0f, 100f))
                                                    }
                                                }
                                            },
                                        currentTimeMsState = currentTimeMsState,
                                    )
                                } else if (appleStyle) {
                                    // Parsing failed — fall back to a plain line, but still the
                                    // Apple-shaped one, or a single unparsable line would render
                                    // at a different size than every line around it.
                                    AppleMusicLyricsLineItem(
                                        originalWords = words,
                                        translatedWords = translatedWords,
                                        // Same clause as the line-synced branch below, and it has to
                                        // be here too: [allLinesCurrent] already lifts this line's
                                        // opacity to full through appleMusicLyricFocus, so without it
                                        // the line would come out fully opaque in grey ink.
                                        isCurrent = index == currentLineIndex || allLinesCurrent,
                                        romanizedWords = romanizedWords,
                                    )
                                } else {
                                    // Fallback to regular line item if parsing fails
                                    LyricsLineItem(
                                        originalWords = words,
                                        translatedWords = translatedWords,
                                        isBold = index <= currentLineIndex,
                                        isCurrent = index == currentLineIndex,
                                        romanizedWords = romanizedWords,
                                        modifier =
                                            Modifier
                                                .clickable {
                                                    val totalMs = timeLine.value.total
                                                    val targetMs = line.startTimeMs.toLongOrNull()
                                                    if (totalMs > 0L && targetMs != null) {
                                                        onLineClick((targetMs.toFloat() * 100f / totalMs.toFloat()).coerceIn(0f, 100f))
                                                    }
                                                },
                                    )
                                }
                            }

                            // Line sync or unsynced
                            appleStyle -> {
                                AppleMusicLyricsLineItem(
                                    originalWords = words,
                                    translatedWords = translatedWords,
                                    romanizedWords = romanizedWords,
                                    // Strictly the sung line — NOT Classic's `|| syncType != LINE_SYNCED`.
                                    // That clause keys off syncType, which lit every line of a
                                    // LINE_SYNCED sheet too and put two lit lines on screen at once.
                                    // [allLinesCurrent] is the narrow version: it fires only when the
                                    // timestamps genuinely cannot order anything, i.e. an unsynced
                                    // sheet, where every line being current is the wanted result.
                                    isCurrent = index == currentLineIndex || allLinesCurrent,
                                )
                            }

                            // Line sync or unsynced: use existing LyricsLineItem
                            else -> {
                                LyricsLineItem(
                                    originalWords = words,
                                    translatedWords = translatedWords,
                                    romanizedWords = romanizedWords,
                                    isBold = index <= currentLineIndex || lyricsData.lyrics.syncType != "LINE_SYNCED",
                                    isCurrent = index == currentLineIndex || lyricsData.lyrics.syncType != "LINE_SYNCED",
                                    modifier =
                                        Modifier
                                            .clickable(enabled = lyricsData.lyrics.syncType == "LINE_SYNCED") {
                                                val totalMs = timeLine.value.total
                                                val targetMs = line.startTimeMs.toLongOrNull()
                                                if (totalMs > 0L && targetMs != null) {
                                                    onLineClick((targetMs.toFloat() * 100f / totalMs.toFloat()).coerceIn(0f, 100f))
                                                }
                                            },
                                )
                            }
                        }
                    }

                    if (appleStyle) {
                        // The whole wrapper is the tap target — original line AND translation — the
                        // way AMLL's .lyricLineWrapper is, rather than each Text separately. The
                        // press shows as a tinted rounded panel; indication is null because a
                        // ripple centred on the finger reads as a button, and these are lyrics.
                        val lineInteraction = remember { MutableInteractionSource() }
                        val linePressed by lineInteraction.collectIsPressedAsState()
                        Column(
                            modifier =
                                Modifier
                                    // Trails the list while a handover runs (LyricsStagger). A
                                    // translation only — no clip — so the blur and the glow below
                                    // still spill past the line's own box.
                                    .graphicsLayer { translationY = stagger.lagPx(index) }
                                    // background(colour, shape), NOT clip(shape) + background().
                                    // clip() cuts everything that leaves this line's box — which is
                                    // exactly what blur and glow are supposed to do. It sliced the
                                    // blur off square at the left edge and erased the bloom around
                                    // the sung line entirely. background() with a shape paints the
                                    // rounded press panel without clipping any of the content.
                                    .background(
                                        color = if (linePressed) AppleMusicLyricPressedBackground else Color.Transparent,
                                        shape = RoundedCornerShape(AppleMusicLyricCornerRadius),
                                    ).clickable(
                                        interactionSource = lineInteraction,
                                        indication = null,
                                        enabled = lyricsData.lyrics.syncType == "LINE_SYNCED" ||
                                            lyricsData.lyrics.syncType == "RICH_SYNCED",
                                    ) {
                                        val totalMs = timeLine.value.total
                                        val targetMs = line.startTimeMs.toLongOrNull()
                                        if (totalMs > 0L && targetMs != null) {
                                            onLineClick((targetMs.toFloat() * 100f / totalMs.toFloat()).coerceIn(0f, 100f))
                                        }
                                    },
                        ) {
                            // Blur spans the FULL width; the gutter is applied inside it. Ordered
                            // the other way — gutter outside, blur around the text — the blurred
                            // box begins exactly where the glyphs begin, so there is no margin for
                            // the softened edge to spill into and it comes out sliced flat down the
                            // left. This is the same problem AMLL solves with `margin: -1em;
                            // padding: 1em`, which widens the painted area without moving the
                            // layout; Compose has no negative padding, so the equivalent is to blur
                            // the wide box and inset the content within it.
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .appleMusicLyricFocus(
                                            distanceFromCurrent,
                                            blurEnabled = !isDragging,
                                            hasActiveLine = currentLineIndex >= 0,
                                            allLinesCurrent = allLinesCurrent,
                                        ),
                            ) {
                                Box(modifier = Modifier.padding(horizontal = AppleMusicLyricPaddingX)) {
                                    renderLine()
                                }
                            }
                        }
                    } else {
                        renderLine()
                    }
                }
            }
            footerContent?.let { footer ->
                item {
                    if (appleStyle) {
                        // Same gutter as the lyrics, and dimmed — but NOT blurred. Blur means
                        // "further away in the song"; the caption is not part of the song at all,
                        // it is a note about where the words came from. Blurring it made it look
                        // like a lyric line the reader had lost, and it is meant to stay legible
                        // when someone actually goes looking for it.
                        Box(
                            modifier =
                                Modifier
                                    // The row after the last line: it trails with the lines, or
                                    // the lines would run into it while they catch up.
                                    .graphicsLayer { translationY = stagger.lagPx(displayLines.lines.size) }
                                    .padding(horizontal = AppleMusicLyricPaddingX)
                                    .alpha(FOOTER_ALPHA),
                        ) {
                            footer()
                        }
                    } else {
                        footer()
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsLineItem(
    originalWords: String,
    translatedWords: String?,
    isBold: Boolean,
    isCurrent: Boolean = false,
    // Between the original and the translation, never in place of either: the point is to read the
    // original script AND know how to say it. Null when romanization is off for this language.
    romanizedWords: String? = null,
    modifier: Modifier = Modifier,
) {
    // Both halves live INSIDE the Crossfade. They did not before: the large one was in here and the
    // small one sat outside behind a plain `if`, so the small one was removed the instant the state
    // flipped while the large one was still fading in — the line appeared to blink out and come
    // back. Overlapped, one fades down as the other fades up.
    //
    // What this does NOT fix, and cannot: the two halves are different FONT SIZES, so they wrap
    // differently, and a Crossfade composes both during the transition — the box takes the taller
    // of the two the moment the transition starts. Re-wrapping is a measurement, and a crossfade
    // only touches opacity.
    Crossfade(targetState = isBold) { bold ->
        Column(
            modifier = modifier,
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            LyricText(
                text = originalWords,
                modifier = Modifier.fillMaxWidth(),
                style = if (bold) typo().headlineLarge else typo().headlineMedium,
                color = if (bold && isCurrent) Color.White else DimOriginalColor,
            )
            if (romanizedWords != null) {
                LyricText(
                    text = romanizedWords,
                    alignmentText = originalWords,
                    modifier = Modifier.fillMaxWidth(),
                    style = typo().bodyMedium,
                    // Neither the original's white nor the translation's yellow: a reading is a
                    // third KIND of thing, and giving it the translation's colour would read as
                    // two translations stacked.
                    color = if (bold && isCurrent) DimRomanizedCurrentColor else DimRomanizedColor,
                )
            }
            if (!translatedWords.isNullOrBlank()) {
                LyricText(
                    text = translatedWords,
                    alignmentText = originalWords,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    style = typo().bodyMedium,
                    color = if (bold && isCurrent) ClassicTranslatedColor else ClassicDimTranslatedColor,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * The playback position, carried forward between the player's 100 ms position ticks so that it
 * advances once per FRAME instead of ten times a second.
 *
 * Rich sync needs this and a line-level sheet does not. Word timings routinely sit tens of
 * milliseconds apart — the parser's own doc example is `<00:16.62> Và <00:16.64> em`, a gap of 20 ms
 * — so several words can begin inside one tick. Picking the sung word straight off the tick jumps
 * over every one of them but the last, and a word that is jumped over goes from "not yet" to "past"
 * without ever being active: no wipe, and the travelling flare never touches it, because the flare
 * is off whenever a word is not active. That is the stutter this exists to remove.
 *
 * Returned as [State] rather than as a value on purpose. Reading it during composition would
 * recompose the caller, and every word in the line with it, sixty times a second; feeding a
 * `derivedStateOf` instead means the caller only recomposes when the sung word actually changes.
 *
 * Drift is bounded by [PLAYHEAD_TICK_MS]: while the ticks keep coming this never runs more than one
 * tick ahead, and when they stop — paused playback — it settles one tick ahead and stays there.
 */
internal fun computeMonotonicPlayheadMs(
    previousPlayheadMs: Long,
    candidateMs: Long,
    rawMs: Long,
): Long {
    if (previousPlayheadMs <= 0L) return candidateMs
    if (rawMs < previousPlayheadMs - PLAYHEAD_SEEK_BACK_THRESHOLD_MS) return candidateMs
    if (rawMs > previousPlayheadMs + PLAYHEAD_SEEK_FORWARD_THRESHOLD_MS) return candidateMs
    return max(previousPlayheadMs, candidateMs)
}

internal class WordProgressTracker(
    var maxProgress: Float = 0f,
    var lastNowMs: Long = Long.MIN_VALUE,
)

internal fun computeMonotonicWordProgress(
    nowMs: Long,
    wordStartTimeMs: Long,
    wordEndTimeMs: Long,
    tracker: WordProgressTracker,
): Float {
    if (tracker.lastNowMs != Long.MIN_VALUE && nowMs < tracker.lastNowMs - PLAYHEAD_SEEK_BACK_THRESHOLD_MS) {
        tracker.maxProgress = 0f
    }
    tracker.lastNowMs = nowMs

    val rawDurationMs = (wordEndTimeMs - wordStartTimeMs).coerceAtLeast(1L)
    val effectiveDurationMs = max(rawDurationMs, MIN_VISUAL_WORD_SWEEP_MS)
    val rawProgress =
        when {
            nowMs <= wordStartTimeMs -> 0f
            nowMs >= wordStartTimeMs + effectiveDurationMs -> 1f
            else -> ((nowMs - wordStartTimeMs).toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
        }
    val monotonic = max(tracker.maxProgress, rawProgress)
    tracker.maxProgress = monotonic
    return monotonic
}

@Composable
private fun rememberSmoothPlayhead(
    rawMs: Long,
    rawMsState: State<Long>? = null,
    enabled: Boolean,
    running: Boolean = true,
): State<Long> {
    val playhead = remember { mutableLongStateOf(rawMsState?.value ?: rawMs) }
    LaunchedEffect(rawMs, rawMsState, enabled, running) {
        if (enabled && running) {
            val initialRaw = rawMsState?.value ?: rawMs
            if (playhead.longValue == 0L || abs(initialRaw - playhead.longValue) > PLAYHEAD_SEEK_BACK_THRESHOLD_MS) {
                playhead.longValue = initialRaw
            }
        }
        suspend fun runTick(latestRawMs: Long) {
            if (!enabled) {
                playhead.longValue = latestRawMs
                return
            }
            if (!running) {
                // Paused: freeze within one ceiling window unless user seeked while paused.
                if (playhead.longValue < latestRawMs - PLAYHEAD_SEEK_BACK_THRESHOLD_MS ||
                    playhead.longValue > latestRawMs + PLAYHEAD_CEILING_MS
                ) {
                    playhead.longValue = latestRawMs
                }
                return
            }
            var baseNanos = -1L
            while (true) {
                var reachedCeiling = false
                withInfiniteAnimationFrameNanos { frameNanos ->
                    if (baseNanos < 0L) baseNanos = frameNanos
                    val elapsedMs = (frameNanos - baseNanos) / 1_000_000L
                    val candidateMs = latestRawMs + elapsedMs.coerceIn(0L, PLAYHEAD_CEILING_MS)
                    playhead.longValue =
                        computeMonotonicPlayheadMs(
                            previousPlayheadMs = playhead.longValue,
                            candidateMs = candidateMs,
                            rawMs = latestRawMs,
                        )
                    if (elapsedMs >= PLAYHEAD_CEILING_MS) {
                        reachedCeiling = true
                    }
                }
                if (reachedCeiling) break
            }
        }

        if (rawMsState != null && enabled) {
            snapshotFlow { rawMsState.value }.collectLatest { latestRawMs ->
                runTick(latestRawMs)
            }
        } else {
            runTick(rawMsState?.value ?: rawMs)
        }
    }
    return playhead
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RichSyncLyricsLineItem(
    parsedLine: ParsedRichSyncLine,
    translatedWords: String?,
    // Static text under a line that lights up word by word. Keeping it out of the FlowRow above is
    // deliberate: the wipe is driven by per-word timings this row does not have and must not fake.
    romanizedWords: String? = null,
    currentTimeMs: Long,
    isCurrent: Boolean,
    // False while paused: the smooth playhead freezes instead of looping frames. Default true
    // keeps callers that do not thread it (the desktop mini player) exactly as they were.
    isPlaying: Boolean = true,
    customFontSize: TextUnit? = null,
    customPadding: Dp = 12.dp,
    // Null for the Classic renderer, so nothing about it changes. The Apple Music style passes a
    // bloom here for the line being sung — applied per WORD rather than per line, because in a
    // rich-synced line only the words already sung are lit.
    glow: Shadow? = null,
    pendingColorOverride: Color? = null,
    // Same deal: null keeps Classic's yellow (and its dimmed variant) exactly as it was. Apple
    // renders translations in white, and this is a SHARED composable — hardcoding white here
    // would repaint the Classic style too.
    translatedColorOverride: Color? = null,
    // Null / 1f keep Classic's bodyMedium at full opacity. Apple Music passes AMLL's sub-line
    // ratios (0.5em, 1.5em leading, 0.3 opacity) — again as parameters, because this composable is
    // shared and hardcoding them would resize Classic's translations too.
    translatedStyleOverride: TextStyle? = null,
    // Zero keeps FlowRow's original Arrangement.Center, so Classic wraps exactly as it always has.
    // Apple Music passes real spacing because its lines are large enough to wrap often, and
    // FlowRow ignores the lineHeight that spaces the non-wrapped renderer's lines.
    wrappedLineSpacing: Dp = 0.dp,
    modifier: Modifier = Modifier,
    currentTimeMsState: State<Long>? = null,
) {
    val playhead =
        rememberSmoothPlayhead(
            rawMs = currentTimeMs,
            rawMsState = currentTimeMsState,
            enabled = isCurrent,
            running = isPlaying,
        )
    val originalWords = remember(parsedLine.words) { parsedLine.words.joinToString(" ") { it.text } }
    val lineDirection = rememberLyricLayoutDirection(originalWords)

    // Remembered on the LINE rather than on the clock. The previous `remember(currentTimeMs, …)`
    // rebuilt this derived state on every tick, which gave away the one thing derivedStateOf is
    // for: it notifies only when the RESULT changes. Keyed this way, the playhead can move sixty
    // times a second inside it while this composable recomposes only when the sung word changes.
    val currentWordIndex by remember(parsedLine.words, isCurrent) {
        derivedStateOf {
            if (!isCurrent) return@derivedStateOf -1
            parsedLine.words.indexOfLast { it.startTimeMs <= playhead.value }
        }
    }

    Column(
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(customPadding))

        // Word order follows the original; subtitles share its edge but keep their own reading direction.
        CompositionLocalProvider(LocalLayoutDirection provides lineDirection) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement =
                    if (wrappedLineSpacing > 0.dp) Arrangement.spacedBy(wrappedLineSpacing) else Arrangement.Center,
            ) {
                parsedLine.words.forEachIndexed { index, wordTiming ->
                    // Calculate word end time (start time of next word or line end time)
                    // If last word and lineEndTimeMs is invalid (Long.MAX_VALUE), estimate based on previous word duration
                    val wordEndTimeMs =
                        if (index < parsedLine.words.size - 1) {
                            parsedLine.words[index + 1].startTimeMs
                        } else if (parsedLine.lineEndTimeMs == Long.MAX_VALUE || parsedLine.lineEndTimeMs <= wordTiming.startTimeMs) {
                            // Estimate: use previous word duration or default 500ms
                            if (index > 0 && parsedLine.words[index - 1].startTimeMs < wordTiming.startTimeMs) {
                                val prevWordDuration = wordTiming.startTimeMs - parsedLine.words[index - 1].startTimeMs
                                wordTiming.startTimeMs + prevWordDuration
                            } else {
                                wordTiming.startTimeMs + 500L // Default 500ms if no reference
                            }
                        } else {
                            parsedLine.lineEndTimeMs
                        }
                    AnimatedWord(
                        word = wordTiming.text,
                        lineDirection = lineDirection,
                        wordIndex = index,
                        wordStartTimeMs = wordTiming.startTimeMs,
                        wordEndTimeMs = wordEndTimeMs,
                        currentTimeMs = currentTimeMs,
                        playheadMs = playhead,
                        isActive = isCurrent && index == currentWordIndex,
                        isPast = isCurrent && index < currentWordIndex,
                        isCurrent = isCurrent,
                        customFontSize = customFontSize,
                        glow = glow,
                        isLastWord = index == parsedLine.words.lastIndex,
                        pendingColorOverride = pendingColorOverride,
                    )
                }
            }
        }

        if (romanizedWords != null) {
            LyricText(
                text = romanizedWords,
                alignmentText = originalWords,
                modifier = Modifier.fillMaxWidth(),
                style = translatedStyleOverride ?: typo().bodyMedium,
                color = if (isCurrent) DimRomanizedCurrentColor else DimRomanizedColor,
            )
        }

        // Translated lyrics (line-level, no word sync)
        if (!translatedWords.isNullOrBlank()) {
            LyricText(
                text = translatedWords,
                alignmentText = originalWords,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                style = translatedStyleOverride ?: typo().bodyMedium,
                color = translatedColorOverride ?: if (isCurrent) ClassicTranslatedColor else ClassicDimTranslatedColor,
            )
        }

        Spacer(modifier = Modifier.height(customPadding))
    }
}

@Composable
private fun AnimatedWord(
    word: String,
    lineDirection: LayoutDirection,
    wordIndex: Int,
    wordStartTimeMs: Long,
    wordEndTimeMs: Long,
    currentTimeMs: Long,
    // The frame-interpolated playhead, taken as State and read ONLY inside the effect below. Taking
    // it as a plain Long would recompose every word in the line on every frame; a State read from a
    // suspend body is not a composition read at all.
    playheadMs: State<Long>,
    isActive: Boolean,
    isPast: Boolean,
    isCurrent: Boolean,
    customFontSize: TextUnit? = null,
    glow: Shadow? = null,
    // AMLL emphasises the closing word of a line more than the rest — that is where a singer holds.
    isLastWord: Boolean = false,
    // Null keeps Classic's DimRichPendingColor untouched.
    pendingColorOverride: Color? = null,
) {
    val wordBidi =
        remember(word, lineDirection) {
            val fallback =
                if (lineDirection == LayoutDirection.Rtl) Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT else Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT
            Bidi(word, fallback)
        }
    val wordDirection = if (wordBidi.baseIsLeftToRight()) TextDirection.Ltr else TextDirection.Rtl
    val baseHeadline = typo().headlineLarge
    val style =
        remember(customFontSize, wordDirection, baseHeadline) {
            baseHeadline.copy(
                fontSize = customFontSize ?: baseHeadline.fontSize,
                textDirection = wordDirection,
            )
        }

    if (!isCurrent) {
        Text(text = word, style = style, color = pendingColorOverride ?: DimOriginalColor)
        return
    }

    // Direct draw-phase monotonic wipe driven by the frame-interpolated playhead:
    // avoids per-word coroutine startup delay and backward snap jitter on fast vocals.
    val wordDurationMs = (wordEndTimeMs - wordStartTimeMs).coerceAtLeast(1L)
    val progressTracker =
        remember(wordStartTimeMs, wordEndTimeMs) {
            WordProgressTracker(maxProgress = if (isPast) 1f else 0f)
        }

    val readProgress: () -> Float =
        remember(progressTracker, playheadMs, wordStartTimeMs, wordEndTimeMs) {
            {
                computeMonotonicWordProgress(
                    nowMs = playheadMs.value,
                    wordStartTimeMs = wordStartTimeMs,
                    wordEndTimeMs = wordEndTimeMs,
                    tracker = progressTracker,
                )
            }
        }

    // AMLL's emphasis strengths, derived from the word's OWN duration.
    val emphasisDurationMs = max(EMP_MIN_DURATION_MS, wordDurationMs.toFloat())
    val amount =
        if (glow == null) {
            0f
        } else {
            val raw = emphasisDurationMs / EMP_AMOUNT_REF_MS
            val shaped = if (raw > 1f) sqrt(raw) else raw * raw * raw
            min(EMP_AMOUNT_CAP, shaped * EMP_AMOUNT_GAIN * if (isLastWord) EMP_LAST_WORD_AMOUNT else 1f)
        }
    val blurAmount =
        if (glow == null) {
            0f
        } else {
            val raw = emphasisDurationMs / EMP_BLUR_REF_MS
            val shaped = if (raw > 1f) sqrt(raw) else raw * raw * raw
            min(EMP_BLUR_CAP, shaped * EMP_BLUR_GAIN * if (isLastWord) EMP_LAST_WORD_BLUR else 1f)
        }
    // The flare's on/off, animated instead of switched. Held as State<Float> so only graphicsLayer reads it.
    val flareGateState =
        if (glow != null) {
            animateFloatAsState(
                targetValue = if (isActive) 1f else 0f,
                animationSpec =
                    tween(
                        durationMillis = if (isActive) FLARE_ATTACK_MS else FLARE_FADE_MS,
                        easing = LinearEasing,
                    ),
                label = "flareGate",
            )
        } else {
            null
        }

    val fontPx = with(LocalDensity.current) { style.fontSize.toPx() }
    val wordGlow =
        remember(glow, fontPx, blurAmount) {
            glow?.let { base ->
                val radius =
                    if (blurAmount > 0f) {
                        min(EMP_GLOW_RADIUS_EM, blurAmount * EMP_GLOW_RADIUS_EM) * fontPx
                    } else {
                        SUNG_BASE_GLOW_EM * fontPx
                    }
                base.copy(
                    color = base.color.copy(alpha = SUNG_BASE_GLOW_ALPHA),
                    blurRadius = radius,
                )
            }
        }
    val pendingColor = pendingColorOverride ?: DimRichPendingColor

    Box(
        modifier =
            Modifier.graphicsLayer {
                val eased =
                    if (amount <= 0f && blurAmount <= 0f) 0f else empEasing(readProgress())
                val scale = 1f + eased * EMP_SCALE_EM * amount
                scaleX = scale
                scaleY = scale
                // Rises slightly as it swells — AMLL's offsetY. Negative is upward.
                translationY = -eased * EMP_RISE_EM * amount * fontPx
            },
    ) {
        if (!wordBidi.isLeftToRight) {
            // Keep Arabic shaping and combining marks intact, including RTL words in an LTR line.
            ShapedLyricsWord(
                word = word,
                style = style,
                readProgress = readProgress,
                isActive = isActive,
                isPast = isPast,
                isRtl = wordDirection == TextDirection.Rtl,
                restingColor = pendingColor,
                glow = wordGlow,
                flareState = flareGateState,
                fontPx = fontPx,
            )
        } else {
            // A Latin word keeps its character order even inside an RTL line.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                ShapedLyricsWord(
                    word = word,
                    style = style,
                    readProgress = readProgress,
                    isActive = isActive,
                    isPast = isPast,
                    isRtl = false,
                    restingColor = pendingColor,
                    glow = wordGlow,
                    flareState = flareGateState,
                    fontPx = fontPx,
                )
            }
        }
    }
}

@Composable
private fun ShapedLyricsWord(
    word: String,
    style: TextStyle,
    readProgress: () -> Float,
    isActive: Boolean,
    isPast: Boolean,
    isRtl: Boolean,
    restingColor: Color,
    glow: Shadow?,
    flareState: State<Float>?,
    fontPx: Float,
) {
    val hasStartedRise by
        remember(isActive, isPast, readProgress) {
            derivedStateOf { isPast || (isActive && readProgress() > 0f) }
        }
    val riseState =
        if (glow != null) {
            animateFloatAsState(
                targetValue = if (hasStartedRise) 1f else 0f,
                animationSpec = tween(CHAR_RISE_MS, easing = FastOutSlowInEasing),
                label = "shapedWordRise",
            )
        } else {
            null
        }
    Box(Modifier.graphicsLayer { translationY = -(riseState?.value ?: 0f) * CHAR_RISE_EM * fontPx }) {
        Text(text = word, style = style, color = restingColor)
        if (glow != null) {
            val glowStyle = remember(style, glow) { style.copy(shadow = glow) }
            Text(
                text = word,
                modifier =
                    Modifier
                        .clearAndSetSemantics {}
                        .graphicsLayer { alpha = flareState?.value ?: 0f },
                style = glowStyle,
                color = Color.Transparent,
            )
        }
        Text(
            text = word,
            style = style,
            color = Color.White,
            modifier =
                Modifier
                    .clearAndSetSemantics {}
                    .drawWithContent {
                        val progress = readProgress()
                        if (progress >= 1f) {
                            drawContent()
                        } else if (progress > 0f) {
                            clipRect(
                                left = if (isRtl) size.width * (1f - progress) else 0f,
                                right = if (isRtl) size.width else size.width * progress,
                            ) { this@drawWithContent.drawContent() }
                        }
                    },
        )
    }
}

/**
 * Android's host for [FullscreenLyricsContent]: the page as a full-height bottom sheet. Desktop
 * hosts the same content in a full-window Popup instead (NowPlayingScreenContent).
 */
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun FullscreenLyricsSheet(
    sharedViewModel: SharedViewModel,
    navController: NavController,
    color: Color = Color(0xFF242424),
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    nowPlayingStyle: String,
    onDismiss: () -> Unit,
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val coroutineScope = rememberCoroutineScope()
    // ModalBottomSheet caps its width at 640dp by default, which would squeeze the two-column
    // landscape layout into a centred strip on a phone turned sideways. Portrait keeps the default.
    val screenInfo = getScreenSizeInfo()
    val isLandscape = screenInfo.wDP > screenInfo.hDP

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        containerColor = Color.Black,
        contentColor = Color.Transparent,
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = .5f),
        sheetMaxWidth = if (isLandscape) Dp.Unspecified else BottomSheetDefaults.SheetMaxWidth,
        sheetState = sheetState,
        modifier =
            Modifier
                .fillMaxHeight(),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        FullscreenLyricsContent(
            sharedViewModel = sharedViewModel,
            navController = navController,
            color = color,
            state = state,
            actions = actions,
            nowPlayingStyle = nowPlayingStyle,
            // A dismiss the page asks for itself slides the sheet away before it leaves
            // composition, instead of cutting it off mid-screen.
            onDismiss = {
                coroutineScope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            },
        )
    }
}
