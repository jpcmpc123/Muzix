package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.imageBitmapFromArgb
import com.maxrave.simpmusic.expect.ui.isHardwareRenderEffectBlurSupported
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.stackBlurArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Bidi
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Apple Music renders EVERY lyric line at the same size — what separates the line being sung from
 * the rest is focus, not scale: the active line is opaque and sharp, its neighbours dim and
 * progressively blurred with distance, as if the page had a shallow depth of field.
 */
internal const val MAX_EFFECTIVE_DISTANCE = 4
internal const val BLUR_QUANTIZE_STEP_DP = 0.5f
internal const val BLUR_MAX_DP = 6.0f
internal const val PRE_ROLL_LINE_ALPHA = 0.6f

// Smooth calibrated curve: subtle depth of field that matches real Apple Music without heavy GPU taps,
// and freezes at effective distance >= 4 so distant lines keep the exact same pooled BlurEffect.
private val SMOOTH_BLUR_LEVELS_DP = floatArrayOf(0f, 1.4f, 2.8f, 4.4f, 5.8f)
private val SMOOTH_ALPHA_LEVELS = floatArrayOf(1.0f, 0.68f, 0.48f, 0.34f, 0.24f)

// The unsung REMAINDER of the line currently being sung — "ce" in "dan|ce". It has to be clearly
// grey against the white of what has already been sung, or the wipe carries no contrast and the
// line never reads as lighting up. Classic's DimRichPendingColor is LightGray at 60%, far too
// close to white for that.
internal val AppleMusicPendingWordColor = Color(0xFF8E8E8E)

/**
 * Apple sets lyrics far larger than body copy — a line fills most of the width and wraps after a
 * few words, which is what makes the page readable at arm's length while the phone sits on a desk.
 * The app's own headlineLarge is 23sp, sized for list headers rather than for this, so the size is
 * overridden here while the weight and font family still come from the theme.
 */
internal val AppleMusicLyricFontSize = 28.sp
internal val AppleMusicLyricLineHeight = 34.sp
internal val AppleMusicLyricGap = 20.dp

internal val AppleMusicSubLineFontSize = 14.sp
internal val AppleMusicSubLineHeight = 21.sp
internal val AppleMusicMainToSubGap = 12.dp

internal val AppleMusicLyricPaddingX = 20.dp

internal val AppleMusicLyricCornerRadius = 8.dp
internal val AppleMusicLyricPressedBackground = Color.White.copy(alpha = 0.067f)

internal val AppleMusicWrappedLineSpacing = 2.dp

internal val AppleMusicTranslatedColor = Color.White.copy(alpha = 0.78f)
internal val AppleMusicRomanizedColor = Color.White.copy(alpha = 0.62f)
internal val AppleMusicInactiveLineColor = Color(0xFF9B9B9B)

internal val AppleMusicActiveLineGlow = Shadow(color = Color.White, offset = Offset.Zero, blurRadius = 0f)

internal fun resolveEffectiveDistance(distanceFromCurrent: Int): Int {
    if (distanceFromCurrent == 0) return 0
    val raw = if (distanceFromCurrent < 0) abs(distanceFromCurrent) + 1 else distanceFromCurrent
    return raw.coerceIn(0, MAX_EFFECTIVE_DISTANCE)
}

internal fun resolveTargetBlurDp(
    distanceFromCurrent: Int,
    blurEnabled: Boolean,
    hasActiveLine: Boolean = true,
    allLinesCurrent: Boolean = false,
): Dp {
    if (!blurEnabled || allLinesCurrent || !hasActiveLine || distanceFromCurrent == 0) {
        return 0.dp
    }
    val eff = resolveEffectiveDistance(distanceFromCurrent)
    return SMOOTH_BLUR_LEVELS_DP[eff].dp
}

internal fun resolveTargetAlpha(
    distanceFromCurrent: Int,
    hasActiveLine: Boolean = true,
    allLinesCurrent: Boolean = false,
): Float =
    when {
        allLinesCurrent -> SMOOTH_ALPHA_LEVELS[0]
        !hasActiveLine -> PRE_ROLL_LINE_ALPHA
        distanceFromCurrent == 0 -> SMOOTH_ALPHA_LEVELS[0]
        else -> SMOOTH_ALPHA_LEVELS[resolveEffectiveDistance(distanceFromCurrent)]
    }

internal fun quantizeBlurStep(blurDp: Float): Int {
    if (blurDp <= 0.25f) return 0
    val maxStep = (BLUR_MAX_DP / BLUR_QUANTIZE_STEP_DP).roundToInt()
    return (blurDp / BLUR_QUANTIZE_STEP_DP).roundToInt().coerceIn(0, maxStep)
}

internal fun quantizeBlurDp(blurDp: Float): Dp = (quantizeBlurStep(blurDp) * BLUR_QUANTIZE_STEP_DP).dp

/**
 * Process-wide pool of quantized [BlurEffect] instances keyed by `(step, densityBucket, tileMode)`.
 * Reusing the exact same [BlurEffect] reference across frames prevents Compose/HWUI from
 * invalidating RenderNode caches during 60/120fps animations and keeps distant rows (`>= 4`)
 * completely static on the GPU.
 */
internal object AppleMusicBlurEffectPool {
    private val cache = HashMap<Long, BlurEffect>(64)

    fun getOrCreate(
        blurDp: Float,
        density: Float,
        tileMode: TileMode,
    ): BlurEffect? {
        val step = quantizeBlurStep(blurDp)
        if (step <= 0) return null
        val radiusDp = step * BLUR_QUANTIZE_STEP_DP
        val radiusPx = radiusDp * density
        if (radiusPx <= 0.5f * density) return null
        val densityBucket = (density * 20f).roundToInt().coerceAtLeast(1)
        val tileTag = if (tileMode == TileMode.Clamp) 1 else 0
        val key = (step.toLong() shl 32) or (densityBucket.toLong() shl 1) or tileTag.toLong()
        return synchronized(cache) {
            cache.getOrPut(key) {
                val bucketRadiusPx = radiusDp * (densityBucket / 20f)
                BlurEffect(bucketRadiusPx, bucketRadiusPx, tileMode)
            }
        }
    }

    internal fun clearForTest() = synchronized(cache) { cache.clear() }

    internal fun sizeForTest(): Int = synchronized(cache) { cache.size }
}

/**
 * Bounded LRU cache of pre-blurred 0.5x lyric line bitmaps used only on Android < 12 where
 * hardware [BlurEffect] is unavailable. Each entry is ~40-60 KB, ~1.5 MB total worst case.
 */
internal object AppleMusicSoftwareBlurCache {
    private const val MAX_ENTRIES = 24
    private val cache =
        object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean =
                size > MAX_ENTRIES
        }

    fun get(key: String): ImageBitmap? = synchronized(cache) { cache[key] }

    fun put(
        key: String,
        bitmap: ImageBitmap,
    ) {
        synchronized(cache) {
            cache[key] = bitmap
        }
    }
}

/**
 * Depth-of-field treatment for one lyric line in **a single unified `graphicsLayer`**:
 * translation stagger, opacity dimming, and (on Android 12+ / Desktop) pooled quantized `BlurEffect`.
 */
@Composable
fun Modifier.appleMusicLyricFocus(
    distanceFromCurrent: Int,
    blurEnabled: Boolean,
    hasActiveLine: Boolean = true,
    allLinesCurrent: Boolean = false,
    translationYProvider: (() -> Float)? = null,
): Modifier {
    val targetBlur = resolveTargetBlurDp(distanceFromCurrent, blurEnabled, hasActiveLine, allLinesCurrent)
    val targetAlpha = resolveTargetAlpha(distanceFromCurrent, hasActiveLine, allLinesCurrent)

    // Fast sharpening (180ms) while dragging for instant legibility; smooth 320ms settle back into focus.
    val blurAnimMs = if (blurEnabled) 320 else 180
    val blurRadiusState = animateDpAsState(targetValue = targetBlur, animationSpec = tween(blurAnimMs), label = "appleMusicLyricBlur")
    val lineAlphaState = animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(blurAnimMs), label = "appleMusicLyricAlpha")
    val hardwareBlur = isHardwareRenderEffectBlurSupported()
    val isDesktop = getPlatform() == Platform.Desktop
    val edgeTreatment = if (isDesktop) TileMode.Clamp else TileMode.Decal

    return this.graphicsLayer {
        translationY = translationYProvider?.invoke() ?: 0f
        alpha = lineAlphaState.value
        if (hardwareBlur) {
            val effect =
                AppleMusicBlurEffectPool.getOrCreate(
                    blurDp = blurRadiusState.value.value,
                    density = density,
                    tileMode = edgeTreatment,
                )
            renderEffect = effect
            clip = isDesktop && effect != null
        } else {
            renderEffect = null
            clip = false
        }
    }
}

/**
 * Background 0.5x StackBlur generator for Android < 12 (`!isHardwareRenderEffectBlurSupported()`).
 * Returns null immediately on Android 12+ / Desktop, active lines, or while dragging.
 */
@Composable
internal fun rememberSoftwareBlurredLyricBitmap(
    originalWords: String,
    romanizedWords: String?,
    translatedWords: String?,
    distanceFromCurrent: Int,
    blurEnabled: Boolean,
    hasActiveLine: Boolean,
    allLinesCurrent: Boolean,
    fullWidthPx: Int,
    paddingXPx: Int,
    mainStyle: TextStyle,
    subStyle: TextStyle?,
    textMeasurer: TextMeasurer?,
): ImageBitmap? {
    if (isHardwareRenderEffectBlurSupported() || textMeasurer == null) return null
    val targetBlur = resolveTargetBlurDp(distanceFromCurrent, blurEnabled, hasActiveLine, allLinesCurrent)
    val step = quantizeBlurStep(targetBlur.value)
    if (step <= 0 || fullWidthPx <= paddingXPx * 2 + 2) return null

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val contentWidthPx = (fullWidthPx - paddingXPx * 2).coerceAtLeast(1)
    val cacheKey = "$originalWords|${romanizedWords.orEmpty()}|${translatedWords.orEmpty()}|$fullWidthPx|$step|$layoutDirection"
    val cachedImmediate = AppleMusicSoftwareBlurCache.get(cacheKey)
    var bitmap by remember(cacheKey) { mutableStateOf(cachedImmediate) }

    LaunchedEffect(cacheKey) {
        AppleMusicSoftwareBlurCache.get(cacheKey)?.let {
            bitmap = it
            return@LaunchedEffect
        }
        val blurDp = step * BLUR_QUANTIZE_STEP_DP
        val gapPx = with(density) { AppleMusicLyricGap.toPx() }
        val subGapPx = with(density) { AppleMusicMainToSubGap.toPx() }
        val origAlign =
            if (Bidi(originalWords, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).isRightToLeft) {
                TextAlign.End
            } else {
                TextAlign.Start
            }
        val origLayout =
            textMeasurer.measure(
                text = originalWords,
                style =
                    mainStyle.copy(
                        color = AppleMusicInactiveLineColor,
                        textAlign = origAlign,
                        textDirection = TextDirection.Content,
                    ),
                constraints = Constraints(maxWidth = contentWidthPx),
            )
        val effectiveSubStyle =
            (subStyle ?: mainStyle).copy(
                fontSize = AppleMusicSubLineFontSize,
                lineHeight = AppleMusicSubLineHeight,
                color = AppleMusicInactiveLineColor,
                textAlign = origAlign,
                textDirection = TextDirection.Content,
            )
        val romanLayout =
            romanizedWords?.let {
                textMeasurer.measure(
                    text = it,
                    style = effectiveSubStyle,
                    constraints = Constraints(maxWidth = contentWidthPx),
                )
            }
        val transLayout =
            translatedWords?.let {
                textMeasurer.measure(
                    text = it,
                    style = effectiveSubStyle,
                    constraints = Constraints(maxWidth = contentWidthPx),
                )
            }
        var totalHeightF = gapPx + origLayout.size.height
        if (romanLayout != null) totalHeightF += subGapPx + romanLayout.size.height
        if (transLayout != null) totalHeightF += subGapPx + transLayout.size.height
        totalHeightF += gapPx

        val fullH = totalHeightF.roundToInt().coerceIn(4, 2048)
        val halfW = (fullWidthPx / 2).coerceAtLeast(2)
        val halfH = (fullH / 2).coerceAtLeast(2)

        val rawBitmap = ImageBitmap(halfW, halfH)
        val canvas = Canvas(rawBitmap)
        val drawScope = CanvasDrawScope()
        drawScope.draw(density, layoutDirection, canvas, Size(halfW.toFloat(), halfH.toFloat())) {
            scale(0.5f, 0.5f, Offset.Zero) {
                var y = gapPx
                val x = paddingXPx.toFloat()
                drawText(origLayout, topLeft = Offset(x, y))
                y += origLayout.size.height
                if (romanLayout != null) {
                    y += subGapPx
                    drawText(romanLayout, topLeft = Offset(x, y))
                    y += romanLayout.size.height
                }
                if (transLayout != null) {
                    y += subGapPx
                    drawText(transLayout, topLeft = Offset(x, y))
                }
            }
        }
        val pixels = IntArray(halfW * halfH)
        rawBitmap.readPixels(pixels)
        val stackRadius = with(density) { (blurDp.dp.toPx() * 0.5f).roundToInt().coerceIn(1, 12) }
        val blurred =
            withContext(Dispatchers.Default) {
                stackBlurArgb(pixels, halfW, halfH, stackRadius)
                imageBitmapFromArgb(pixels, halfW, halfH)
            }
        AppleMusicSoftwareBlurCache.put(cacheKey, blurred)
        bitmap = blurred
    }
    return bitmap
}

/**
 * One line-synced lyric line, Apple Music style: same size for every line (the Classic renderer
 * swaps headlineLarge/headlineMedium instead), white, aligned to its text direction, with the translation underneath.
 * Focus is applied by the caller through [appleMusicLyricFocus] so the blur wraps the whole line
 * including its translation.
 */
@Composable
fun AppleMusicLyricsLineItem(
    originalWords: String,
    translatedWords: String?,
    isCurrent: Boolean,
    // Sits between the original and the translation. Shares the sub-line type scale with the
    // translation, since both are secondary to the lyric itself and a second size at this scale
    // would just look like a mistake.
    romanizedWords: String? = null,
    modifier: Modifier = Modifier,
    mainStyleOverride: TextStyle? = null,
    subStyleOverride: TextStyle? = null,
) {
    val resolvedMainStyle =
        mainStyleOverride
            ?: typo().headlineLarge.copy(
                fontSize = AppleMusicLyricFontSize,
                lineHeight = AppleMusicLyricLineHeight,
            )
    val resolvedSubStyle =
        if (romanizedWords != null || translatedWords != null) {
            subStyleOverride
                ?: typo().bodyMedium.copy(
                    fontSize = AppleMusicSubLineFontSize,
                    lineHeight = AppleMusicSubLineHeight,
                )
        } else {
            null
        }
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(AppleMusicLyricGap))
        LyricText(
            text = originalWords,
            modifier = Modifier.fillMaxWidth(),
            color = if (isCurrent) Color.White else AppleMusicInactiveLineColor,
            style = resolvedMainStyle,
        )
        if (romanizedWords != null && resolvedSubStyle != null) {
            Spacer(modifier = Modifier.height(AppleMusicMainToSubGap))
            LyricText(
                text = romanizedWords,
                alignmentText = originalWords,
                modifier = Modifier.fillMaxWidth(),
                style = resolvedSubStyle,
                // Follows the line's own state exactly as the translation does: readable white-ish
                // on the sung line, the same grey as the lyric everywhere else.
                color = if (isCurrent) AppleMusicRomanizedColor else AppleMusicInactiveLineColor,
            )
        }
        if (translatedWords != null && resolvedSubStyle != null) {
            Spacer(modifier = Modifier.height(AppleMusicMainToSubGap))
            LyricText(
                text = translatedWords,
                alignmentText = originalWords,
                modifier = Modifier.fillMaxWidth(),
                style = resolvedSubStyle,
                // The colour still follows the line's own state; AMLL carries the whole sub-line at
                // a flat 0.3 opacity on top of that, which is applied as a modifier so it composes
                // with appleMusicLyricFocus's own dimming instead of fighting it.
                color = if (isCurrent) AppleMusicTranslatedColor else AppleMusicInactiveLineColor,
            )
        }
        Spacer(modifier = Modifier.height(AppleMusicLyricGap))
    }
}
