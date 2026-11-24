package com.maxrave.simpmusic.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import coil3.Canvas
import coil3.Image
import coil3.compose.AsyncImagePainter
import coil3.memory.MemoryCache
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.NullRequestDataException
import com.maxrave.simpmusic.viewModel.resolveNowPlayingThumbnailUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ImageUrlUtilsTest {
    @Test
    fun sizedArtworkUrlResizesGoogleSquareAndUpgradesSmallThumbs() {
        val src544 = "https://lh3.googleusercontent.com/abc123=w544-h544-l90-rj"
        assertEquals(
            "https://lh3.googleusercontent.com/abc123=w240-h240-l90-rj",
            src544.sizedArtworkUrl(THUMB_SMALL_PX),
        )

        val src120 = "https://lh3.googleusercontent.com/abc123=w120-h120-l90-rj"
        assertEquals(
            "https://lh3.googleusercontent.com/abc123=w400-h400-l90-rj",
            src120.sizedArtworkUrl(THUMB_CARD_PX),
        )
    }

    @Test
    fun sizedArtworkUrlSupportsSingleSizeParamAndWidescreenAspectRatio() {
        val avatarUrl = "https://yt3.ggpht.com/ytc/user123=s64-c-k-c0x00ffffff-no-rj"
        assertEquals(
            "https://yt3.ggpht.com/ytc/user123=s240-c-k-c0x00ffffff-no-rj",
            avatarUrl.sizedArtworkUrl(THUMB_SMALL_PX),
        )

        val bannerUrl = "https://lh3.googleusercontent.com/banner=w2880-h1200-p-l90-rj"
        assertEquals(
            "https://lh3.googleusercontent.com/banner=w800-h333-p-l90-rj",
            bannerUrl.sizedArtworkUrl(THUMB_HERO_PX, preserveAspectRatio = true),
        )
        assertEquals(
            "https://lh3.googleusercontent.com/banner=w400-h400-p-l90-rj",
            bannerUrl.sizedArtworkUrl(THUMB_CARD_PX, preserveAspectRatio = false),
        )
    }

    @Test
    fun sizedArtworkUrlLeavesExternalAndLargeYtimgUrlsUntouched() {
        val ytimg = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
        assertEquals(ytimg, ytimg.sizedArtworkUrl(THUMB_CARD_PX))

        val external = "https://i1.sndcdn.com/artworks-123-t500x500.jpg?w=500"
        assertEquals(external, external.sizedArtworkUrl(THUMB_SMALL_PX))
    }

    @Test
    fun sizedArtworkUrlShrinksUnsignedYtimgTrackThumbsAndLeavesSignedOnes() {
        val hq = "https://i.ytimg.com/vi/abc123/hqdefault.jpg"
        assertEquals("https://i.ytimg.com/vi/abc123/mqdefault.jpg", hq.sizedArtworkUrl(THUMB_SMALL_PX))

        val maxres = "https://i.ytimg.com/vi/abc123/maxresdefault.jpg"
        assertEquals("https://i.ytimg.com/vi/abc123/mqdefault.jpg", maxres.sizedArtworkUrl(THUMB_SMALL_PX))
        // Hero requests keep full resolution
        assertEquals(maxres, maxres.sizedArtworkUrl(THUMB_HEADER_HERO_PX))

        // Signed URLs remain completely untouched
        val signed = "https://i.ytimg.com/vi/abc123/hqdefault.jpg?sqp=-oaymwEcCNACELwBSFXyq4qpAw4IARUAAIhCGAFwAcABBg=="
        assertEquals(signed, signed.sizedArtworkUrl(THUMB_SMALL_PX))
    }

    @Test
    fun artworkIdentityKeyAndMemoryPlaceholderFindSmallerCachedVariant() {
        val smallUrl = "https://lh3.googleusercontent.com/sleeve999=w120-h120-l90-rj"
        val heroUrl = "https://lh3.googleusercontent.com/sleeve999=w1200-h1200-l90-rj"

        assertEquals(artworkIdentityKey(smallUrl), artworkIdentityKey(heroUrl))
        assertEquals(
            artworkIdentityKey("https://i.ytimg.com/vi/abcXYZ/maxresdefault.jpg"),
            artworkIdentityKey("https://i.ytimg.com/vi/abcXYZ/hqdefault.jpg"),
        )
        assertEquals(
            artworkIdentityKey("https://www.gstatic.com/youtube/media/lk/liked-music-@576.png"),
            artworkIdentityKey("https://www.gstatic.com/youtube/media/lk/liked-music-@1200.png"),
        )

        val memoryCache = MemoryCache.Builder().maxSizeBytes(1024L * 1024L).build()
        val smallKey = artworkMemoryCacheKey(smallUrl, THUMB_SMALL_PX)
        memoryCache[MemoryCache.Key(smallKey)] = MemoryCache.Value(DummyImage())

        assertEquals(smallKey, findExistingArtworkMemoryKey(memoryCache, heroUrl, THUMB_HEADER_HERO_PX))
        assertNull(findExistingArtworkMemoryKey(memoryCache, smallUrl, THUMB_SMALL_PX))
    }

    @Test
    fun artworkHintCacheNormalizesVlPrefixAndRespectsLruLookup() {
        ArtworkHintCache.clear()
        ArtworkHintCache.put("VLPLxyz123", "https://lh3.googleusercontent.com/cover=w400-h400")
        assertEquals(
            "https://lh3.googleusercontent.com/cover=w400-h400",
            ArtworkHintCache.get("PLxyz123"),
        )
        assertEquals(
            "https://lh3.googleusercontent.com/cover=w400-h400",
            ArtworkHintCache.get("VLPLxyz123"),
        )
        ArtworkHintCache.clear()
    }

    @Test
    fun ramFirstTransformPreservesRamPainterOnLoadingAndUsesFallbackOnlyWhenNull() {
        val ramPainter = ColorPainter(Color.Red)
        val fallbackPainter = ColorPainter(Color.Blue)
        val errorPainter = ColorPainter(Color.Green)

        val loadingWithRam = AsyncImagePainter.State.Loading(painter = ramPainter)
        val resultWithRam =
            ramFirstTransform(
                state = loadingWithRam,
                fallbackPlaceholder = fallbackPainter,
                error = errorPainter,
            )
        assertSame(ramPainter, resultWithRam.painter)

        val loadingWithoutRam = AsyncImagePainter.State.Loading(painter = null)
        val resultWithoutRam =
            ramFirstTransform(
                state = loadingWithoutRam,
                fallbackPlaceholder = fallbackPainter,
                error = errorPainter,
            )
        assertSame(fallbackPainter, resultWithoutRam.painter)

        val errorState =
            AsyncImagePainter.State.Error(
                painter = null,
                result =
                    ErrorResult(
                        image = null,
                        request = ImageRequest.Builder(coil3.PlatformContext.INSTANCE).data("https://example.com/a.jpg").build(),
                        throwable = NullRequestDataException(),
                    ),
            )
        val resultError =
            ramFirstTransform(
                state = errorState,
                fallbackPlaceholder = fallbackPainter,
                error = errorPainter,
            )
        assertSame(errorPainter, resultError.painter)
    }

    @Test
    fun resolveNowPlayingThumbnailUrlFallsBackToSongThumbnails() {
        assertEquals(
            "https://lh3.googleusercontent.com/media=w544-h544",
            resolveNowPlayingThumbnailUrl(
                mediaItemArtworkUri = "https://lh3.googleusercontent.com/media=w544-h544",
                songThumbnails = "https://lh3.googleusercontent.com/song=w544-h544",
            ),
        )
        assertEquals(
            "https://lh3.googleusercontent.com/song=w544-h544",
            resolveNowPlayingThumbnailUrl(
                mediaItemArtworkUri = null,
                songThumbnails = "https://lh3.googleusercontent.com/song=w544-h544",
            ),
        )
        assertEquals(
            "https://lh3.googleusercontent.com/song=w544-h544",
            resolveNowPlayingThumbnailUrl(
                mediaItemArtworkUri = "   ",
                songThumbnails = "https://lh3.googleusercontent.com/song=w544-h544",
            ),
        )
        assertNull(resolveNowPlayingThumbnailUrl(mediaItemArtworkUri = "", songThumbnails = null))
    }

    private class DummyImage : Image {
        override val size = 4L
        override val width = 1
        override val height = 1
        override val shareable = true

        override fun draw(canvas: Canvas) = Unit
    }
}
