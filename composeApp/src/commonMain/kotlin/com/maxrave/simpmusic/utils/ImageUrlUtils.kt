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
const val THUMB_HEADER_HERO_PX = 600
const val THUMB_HERO_PX = 800
const val THUMB_BACKDROP_PX = 360
const val THUMB_BLUR_BACKDROP_PX = 128

const val IMAGE_CROSSFADE_FAST_MS = 120
const val IMAGE_CROSSFADE_HERO_MS = 150
const val HERO_ARTWORK_PREP_TIMEOUT_MS = 350L

private val GOOGLE_WH_REGEX = Regex("(^|-)w(\\d+)-h(\\d+)(?=-|$)")
private val GOOGLE_S_REGEX = Regex("(^|-)s(\\d+)(?=-|$)")
private val YTIMG_VIDEO_ID_REGEX = Regex("/vi(?:_webp)?/([^/]+)/")
private val YTIMG_DOWNGRADE_REGEX = Regex("^(https?://i\\.ytimg\\.com/vi(?:_webp)?/[^/]+/)(?:maxresdefault|sddefault|hqdefault)\\.jpg$")
private val GSTATIC_SIZE_REGEX = Regex("-@\\d+(?=\\.[a-zA-Z0-9]+(?:$|\\?))")
private val SIZE_SUFFIX_REGEX = Regex("=[^/]*$")

internal object ArtworkHintCache {
    private const val MAX_ENTRIES = 64
    private val map =
        object : LinkedHashMap<String, String>(MAX_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > MAX_ENTRIES
        }

    internal fun normalizeId(collectionId: String): String = collectionId.trim().removePrefix("VL")

    fun put(
        collectionId: String?,
        rawThumbUrl: String?,
    ) {
        val cleanId = collectionId?.let(::normalizeId)?.takeIf { it.isNotBlank() } ?: return
        val cleanUrl = rawThumbUrl?.takeIf { it.isNotBlank() } ?: return
        synchronized(map) {
            map[cleanId] = cleanUrl
        }
    }

    fun get(collectionId: String?): String? {
        val cleanId = collectionId?.let(::normalizeId)?.takeIf { it.isNotBlank() } ?: return null
        return synchronized(map) { map[cleanId] }
    }

    internal fun clear() {
        synchronized(map) { map.clear() }
    }
}

internal fun primeCollectionArtworkNavigation(
    platformContext: PlatformContext,
    collectionId: String?,
    rawThumbUrl: String?,
) {
    val cleanUrl = rawThumbUrl?.takeIf { it.isNotBlank() } ?: return
    ArtworkHintCache.put(collectionId, cleanUrl)
    enqueueArtworkPrefetch(
        platformContext = platformContext,
        rawUrl = cleanUrl,
        targetPx = THUMB_HEADER_HERO_PX,
    )
}

internal fun isResizableGoogleImageUrl(url: String): Boolean =
    url.contains("googleusercontent.com", ignoreCase = true) ||
        url.contains("ggpht.com", ignoreCase = true)

/**
 * Rewrites Google/YouTube Music artwork URLs (`googleusercontent.com` / `ggpht.com`) to request
 * only [targetPx] pixels from the CDN instead of downloading full-size 544px–2880px bitmaps for
 * small cards and list rows, and shrinks unsigned `i.ytimg.com` track rows to `mqdefault.jpg`.
 */
fun String?.sizedArtworkUrl(
    targetPx: Int,
    preserveAspectRatio: Boolean = false,
): String? {
    val url = this ?: return null
    if (url.isBlank() || targetPx <= 0) return url
    if (!isResizableGoogleImageUrl(url)) {
        if (targetPx <= THUMB_SMALL_PX && !url.contains('?')) {
            val ytMatch = YTIMG_DOWNGRADE_REGEX.find(url)
            if (ytMatch != null) {
                return "${ytMatch.groupValues[1]}mqdefault.jpg"
            }
        }
        return url
    }

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
    if (url.contains("gstatic.com", ignoreCase = true)) {
        return "gstatic:${GSTATIC_SIZE_REGEX.replace(url, "")}"
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
        listOf(THUMB_HERO_PX, THUMB_HEADER_HERO_PX, THUMB_CARD_PX, THUMB_SMALL_PX, THUMB_BACKDROP_PX)
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
    fallbackPlaceholderUrl: String? = null,
): ImageRequest? {
    val cleanUrl = rawUrl?.takeIf { it.isNotBlank() } ?: return null
    val dataUrl = cleanUrl.sizedArtworkUrl(targetPx, preserveAspectRatio = preserveAspectRatio) ?: return null
    val memKey = artworkMemoryCacheKey(cleanUrl, targetPx, preserveAspectRatio)
    val imageLoader = SingletonImageLoader.get(platformContext)
    val placeholderRamKey =
        when {
            selfAsPlaceholder -> memKey
            reuseExistingRamPlaceholder ->
                findExistingArtworkMemoryKey(imageLoader.memoryCache, cleanUrl, targetPx)
                    ?: fallbackPlaceholderUrl?.let { findExistingArtworkMemoryKey(imageLoader.memoryCache, it, -1) }
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
    targetPx: Int = THUMB_HEADER_HERO_PX,
    preserveAspectRatio: Boolean = false,
) {
    val cleanUrl = rawUrl?.takeIf { it.isNotBlank() } ?: return
    val memKey = artworkMemoryCacheKey(cleanUrl, targetPx, preserveAspectRatio)
    val cache = SingletonImageLoader.get(platformContext).memoryCache
    if (cache?.get(MemoryCache.Key(memKey)) != null) return
    val request =
        buildOptimizedImageRequest(
            platformContext = platformContext,
            rawUrl = cleanUrl,
            targetPx = targetPx,
            crossfadeMs = 0,
            preserveAspectRatio = preserveAspectRatio,
            reuseExistingRamPlaceholder = false,
        ) ?: return
    SingletonImageLoader.get(platformContext).enqueue(request)
}

internal fun findExistingArtworkBitmap(
    platformContext: PlatformContext,
    rawUrl: String?,
): ImageBitmap? {
    val cleanUrl = rawUrl?.takeIf { it.isNotBlank() } ?: return null
    val cache = SingletonImageLoader.get(platformContext).memoryCache ?: return null
    val baseId = artworkIdentityKey(cleanUrl)
    for (candidatePx in listOf(THUMB_HERO_PX, THUMB_HEADER_HERO_PX, THUMB_CARD_PX, THUMB_SMALL_PX, THUMB_BACKDROP_PX)) {
        val value = cache[MemoryCache.Key("art:$baseId:$candidatePx")]
        if (value != null) {
            return value.image.toImageBitmap()
        }
    }
    return null
}

internal fun enqueueTrackArtworkPrefetch(
    platformContext: PlatformContext,
    rawUrls: List<String?>,
    maxCount: Int = 8,
) {
    val seen = HashSet<String>(maxCount)
    var queued = 0
    for (raw in rawUrls) {
        if (queued >= maxCount) break
        val clean = raw?.takeIf { it.isNotBlank() } ?: continue
        if (!seen.add(artworkIdentityKey(clean))) continue
        enqueueArtworkPrefetch(
            platformContext = platformContext,
            rawUrl = clean,
            targetPx = THUMB_SMALL_PX,
        )
        queued++
    }
}

internal data class HeroArtworkResult(
    val bitmap: ImageBitmap?,
    val source: String,
)

/**
 * Non-cancelling hero artwork preparation for Album/Playlist headers:
 * 1. Reuses any RAM variant for [rawUrl] or [hintUrl] in 0 ms and enqueues a single [targetPx]
 *    (`600px`) upgrade only if not already cached.
 * 2. On cold miss, enqueues ONE `600px` request in [ImageLoader]'s own scope and awaits up to
 *    [timeoutMs] WITHOUT cancelling the HTTP request on timeout — so `AsyncImage` shares the
 *    already in-flight download cleanly via `NetworkFirstInterceptor` without throwing away bytes.
 */
internal suspend fun prepareHeroArtworkResult(
    platformContext: PlatformContext,
    rawUrl: String?,
    hintUrl: String? = null,
    targetPx: Int = THUMB_HEADER_HERO_PX,
    timeoutMs: Long = HERO_ARTWORK_PREP_TIMEOUT_MS,
): HeroArtworkResult {
    val cleanUrl = rawUrl?.takeIf { it.isNotBlank() } ?: return HeroArtworkResult(null, "empty")

    val cachedPrimary = findExistingArtworkBitmap(platformContext, cleanUrl)
    if (cachedPrimary != null) {
        enqueueArtworkPrefetch(
            platformContext = platformContext,
            rawUrl = cleanUrl,
            targetPx = targetPx,
        )
        return HeroArtworkResult(cachedPrimary, "ram")
    }

    val cachedHint =
        hintUrl
            ?.takeIf { it.isNotBlank() && it != cleanUrl }
            ?.let { findExistingArtworkBitmap(platformContext, it) }
    if (cachedHint != null) {
        enqueueArtworkPrefetch(
            platformContext = platformContext,
            rawUrl = cleanUrl,
            targetPx = targetPx,
        )
        return HeroArtworkResult(cachedHint, "hint")
    }

    val request =
        buildOptimizedImageRequest(
            platformContext = platformContext,
            rawUrl = cleanUrl,
            targetPx = targetPx,
            crossfadeMs = 0,
            preserveAspectRatio = false,
            reuseExistingRamPlaceholder = false,
        ) ?: return HeroArtworkResult(null, "empty")

    val disposable = SingletonImageLoader.get(platformContext).enqueue(request)
    val awaited = withTimeoutOrNull(timeoutMs) { disposable.job.await() }
    return if (awaited is SuccessResult) {
        HeroArtworkResult(awaited.image.toImageBitmap(), "network")
    } else {
        HeroArtworkResult(null, if (awaited is ErrorResult) "error" else "timeout")
    }
}

internal suspend fun prepareHeroArtworkFast(
    platformContext: PlatformContext,
    rawUrl: String?,
    timeoutMs: Long = HERO_ARTWORK_PREP_TIMEOUT_MS,
): ImageBitmap? = prepareHeroArtworkResult(platformContext, rawUrl, timeoutMs = timeoutMs).bitmap

@Composable
internal fun rememberOptimizedImageRequest(
    rawUrl: String?,
    targetPx: Int,
    crossfadeMs: Int = IMAGE_CROSSFADE_FAST_MS,
    preserveAspectRatio: Boolean = false,
    reuseExistingRamPlaceholder: Boolean = true,
    selfAsPlaceholder: Boolean = false,
    fallbackPlaceholderUrl: String? = null,
): ImageRequest? {
    val platformContext = LocalPlatformContext.current
    return remember(
        platformContext,
        rawUrl,
        targetPx,
        crossfadeMs,
        preserveAspectRatio,
        reuseExistingRamPlaceholder,
        selfAsPlaceholder,
        fallbackPlaceholderUrl,
    ) {
        buildOptimizedImageRequest(
            platformContext = platformContext,
            rawUrl = rawUrl,
            targetPx = targetPx,
            crossfadeMs = crossfadeMs,
            preserveAspectRatio = preserveAspectRatio,
            reuseExistingRamPlaceholder = reuseExistingRamPlaceholder,
            selfAsPlaceholder = selfAsPlaceholder,
            fallbackPlaceholderUrl = fallbackPlaceholderUrl,
        )
    }
}
