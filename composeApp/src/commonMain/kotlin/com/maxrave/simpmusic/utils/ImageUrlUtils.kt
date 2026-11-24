package com.maxrave.simpmusic.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.size.Precision
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

const val THUMB_SMALL_PX = 240
const val THUMB_CARD_PX = 400
const val THUMB_HERO_PX = 800
const val THUMB_BACKDROP_PX = 360
const val THUMB_BLUR_BACKDROP_PX = 128

const val IMAGE_CROSSFADE_FAST_MS = 120
const val IMAGE_CROSSFADE_HERO_MS = 150
const val HERO_ARTWORK_PREP_TIMEOUT_MS = 1_500L

private val GOOGLE_WH_REGEX = Regex("(^|-)w(\\d+)-h(\\d+)(?=-|$)")
private val GOOGLE_S_REGEX = Regex("(^|-)s(\\d+)(?=-|$)")
private val YTIMG_VIDEO_ID_REGEX = Regex("/vi(?:_webp)?/([^/]+)/")
private val SIZE_SUFFIX_REGEX = Regex("=[^/]*$")

internal fun isResizableGoogleImageUrl(url: String): Boolean =
    url.contains("googleusercontent.com", ignoreCase = true) ||
        url.contains("ggpht.com", ignoreCase = true)

/**
 * Rewrites Google/YouTube Music artwork URLs (`googleusercontent.com` / `ggpht.com`) to request
 * only [targetPx] pixels from the CDN instead of downloading full-size 544px–2880px bitmaps for
 * small cards and list rows. Non-Google URLs pass through untouched.
 */
fun String?.sizedArtworkUrl(
    targetPx: Int,
    preserveAspectRatio: Boolean = false,
): String? {
    val url = this ?: return null
    if (url.isBlank() || targetPx <= 0 || !isResizableGoogleImageUrl(url)) return url

    val eqIndex = url.lastIndexOf('=')
    val slashIndex = url.lastIndexOf('/')
    if (eqIndex <= slashIndex || eqIndex == url.lastIndex) return url

    val base = url.substring(0, eqIndex + 1)
    val spec = url.substring(eqIndex + 1)

    val whMatch = GOOGLE_WH_REGEX.find(spec)
    if (whMatch != null) {
        val prefix = whMatch.groupValues[1]
        val srcW = whMatch.groupValues[2].toIntOrNull() ?: 0
        val srcH = whMatch.groupValues[3].toIntOrNull() ?: 0
        val (outW, outH) =
            if (preserveAspectRatio && srcW > 0 && srcH > 0 && srcW != srcH) {
                val maxDim = maxOf(srcW, srcH).toFloat()
                val scale = targetPx / maxDim
                (srcW * scale).roundToInt().coerceAtLeast(1) to (srcH * scale).roundToInt().coerceAtLeast(1)
            } else {
                targetPx to targetPx
            }
        val replaced =
            spec.replaceRange(whMatch.range, "${prefix}w$outW-h$outH")
        return base + replaced
    }

    val sMatch = GOOGLE_S_REGEX.find(spec)
    if (sMatch != null) {
        val prefix = sMatch.groupValues[1]
        val replaced = spec.replaceRange(sMatch.range, "${prefix}s$targetPx")
        return base + replaced
    }

    return url
}

internal fun artworkIdentityKey(url: String): String {
    YTIMG_VIDEO_ID_REGEX.find(url)?.groupValues?.getOrNull(1)?.let { videoId ->
        return "ytimg:$videoId"
    }
    if (isResizableGoogleImageUrl(url)) {
        return "gimg:${SIZE_SUFFIX_REGEX.replace(url, "")}"
    }
    return url
}

internal fun artworkMemoryCacheKey(
    url: String,
    targetPx: Int,
    preserveAspectRatio: Boolean = false,
): String {
    val suffix = if (preserveAspectRatio) ":aspect" else ""
    return "art:${artworkIdentityKey(url)}:$targetPx$suffix"
}

internal fun findExistingArtworkMemoryKey(
    memoryCache: MemoryCache?,
    rawUrl: String?,
    targetPx: Int,
): String? {
    val cache = memoryCache ?: return null
    val url = rawUrl?.takeIf { it.isNotBlank() } ?: return null
    val baseId = artworkIdentityKey(url)
    val candidates =
        listOf(THUMB_HERO_PX, THUMB_CARD_PX, THUMB_SMALL_PX, THUMB_BACKDROP_PX)
            .filter { it != targetPx }
    for (candidatePx in candidates) {
        val candidateKey = "art:$baseId:$candidatePx"
        if (cache[MemoryCache.Key(candidateKey)] != null) {
            return candidateKey
        }
    }
    return null
}

/**
 * Keeps any RAM placeholder painter supplied via `placeholderMemoryCacheKey` while loading
 * instead of overwriting it unconditionally with [fallbackPlaceholder] (which Coil 3's built-in
 * `placeholder` parameter does). Falls back to [fallbackPlaceholder] only when RAM had nothing.
 */
internal fun ramFirstTransform(
    state: AsyncImagePainter.State,
    fallbackPlaceholder: Painter? = null,
    error: Painter? = null,
): AsyncImagePainter.State =
    when (state) {
        is AsyncImagePainter.State.Loading ->
            if (state.painter == null && fallbackPlaceholder != null) {
                state.copy(painter = fallbackPlaceholder)
            } else {
                state
            }
        is AsyncImagePainter.State.Error ->
            if (error != null) state.copy(painter = error) else state
        else -> state
    }

@Composable
internal fun rememberRamFirstTransform(
    fallbackPlaceholder: Painter? = null,
    error: Painter? = null,
): (AsyncImagePainter.State) -> AsyncImagePainter.State =
    remember(fallbackPlaceholder, error) {
        { state -> ramFirstTransform(state, fallbackPlaceholder = fallbackPlaceholder, error = error) }
    }

internal fun buildOptimizedImageRequest(
    platformContext: PlatformContext,
    rawUrl: String?,
    targetPx: Int,
    crossfadeMs: Int = IMAGE_CROSSFADE_FAST_MS,
    preserveAspectRatio: Boolean = false,
    reuseExistingRamPlaceholder: Boolean = true,
    selfAsPlaceholder: Boolean = false,
): ImageRequest? {
    val cleanUrl = rawUrl?.takeIf { it.isNotBlank() } ?: return null
    val dataUrl = cleanUrl.sizedArtworkUrl(targetPx, preserveAspectRatio = preserveAspectRatio) ?: return null
    val memKey = artworkMemoryCacheKey(cleanUrl, targetPx, preserveAspectRatio)
    val imageLoader = SingletonImageLoader.get(platformContext)
    val placeholderRamKey =
        when {
            selfAsPlaceholder -> memKey
            reuseExistingRamPlaceholder -> findExistingArtworkMemoryKey(imageLoader.memoryCache, cleanUrl, targetPx)
            else -> null
        }
    return ImageRequest
        .Builder(platformContext)
        .data(dataUrl)
        .size(targetPx, targetPx)
        .precision(Precision.INEXACT)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCacheKey(dataUrl)
        .memoryCacheKey(memKey)
        .apply {
            if (placeholderRamKey != null) {
                placeholderMemoryCacheKey(placeholderRamKey)
            }
        }.crossfade(crossfadeMs)
        .build()
}

internal fun enqueueArtworkPrefetch(
    platformContext: PlatformContext,
    rawUrl: String?,
    targetPx: Int = THUMB_HERO_PX,
    preserveAspectRatio: Boolean = false,
) {
    val request =
        buildOptimizedImageRequest(
            platformContext = platformContext,
            rawUrl = rawUrl,
            targetPx = targetPx,
            crossfadeMs = 0,
            preserveAspectRatio = preserveAspectRatio,
            reuseExistingRamPlaceholder = false,
        ) ?: return
    SingletonImageLoader.get(platformContext).enqueue(request)
}

internal suspend fun loadHeroArtworkBitmap(
    platformContext: PlatformContext,
    rawUrl: String?,
    targetPx: Int = THUMB_HERO_PX,
    timeoutMs: Long = HERO_ARTWORK_PREP_TIMEOUT_MS,
): ImageBitmap? {
    val request =
        buildOptimizedImageRequest(
            platformContext = platformContext,
            rawUrl = rawUrl,
            targetPx = targetPx,
            crossfadeMs = 0,
            preserveAspectRatio = false,
            reuseExistingRamPlaceholder = false,
        ) ?: return null
    return withTimeoutOrNull(timeoutMs) {
        when (val result = SingletonImageLoader.get(platformContext).execute(request)) {
            is SuccessResult -> result.image.toImageBitmap()
            is ErrorResult -> null
        }
    }
}

@Composable
internal fun rememberOptimizedImageRequest(
    rawUrl: String?,
    targetPx: Int,
    crossfadeMs: Int = IMAGE_CROSSFADE_FAST_MS,
    preserveAspectRatio: Boolean = false,
    reuseExistingRamPlaceholder: Boolean = true,
    selfAsPlaceholder: Boolean = false,
): ImageRequest? {
    val platformContext = LocalPlatformContext.current
    return remember(platformContext, rawUrl, targetPx, crossfadeMs, preserveAspectRatio, reuseExistingRamPlaceholder, selfAsPlaceholder) {
        buildOptimizedImageRequest(
            platformContext = platformContext,
            rawUrl = rawUrl,
            targetPx = targetPx,
            crossfadeMs = crossfadeMs,
            preserveAspectRatio = preserveAspectRatio,
            reuseExistingRamPlaceholder = reuseExistingRamPlaceholder,
            selfAsPlaceholder = selfAsPlaceholder,
        )
    }
}
