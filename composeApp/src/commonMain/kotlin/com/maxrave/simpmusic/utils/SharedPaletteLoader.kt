package com.maxrave.simpmusic.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.kmpalette.loader.NetworkLoader
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.expect.ui.toReadableBitmap
import io.ktor.http.Url

/**
 * Shared [NetworkLoader] for `kmpalette` thumbnail colour extraction backed by Coil's
 * [SingletonImageLoader]. Reuses Coil's disk/RAM cache and connection pool instead of opening a
 * separate `HttpClient(CIO)` and decoding full-size bitmaps for palette sampling.
 */
private class CoilPaletteNetworkLoader(
    private val context: PlatformContext,
) : NetworkLoader {
    override suspend fun load(url: Url): ImageBitmap {
        val imageLoader = SingletonImageLoader.get(context)
        val request =
            ImageRequest
                .Builder(context)
                .data(url.toString())
                .size(128, 128)
                .build()
        return when (val result = imageLoader.execute(request)) {
            is SuccessResult -> result.image.toImageBitmap().toReadableBitmap(64, 64)
            is ErrorResult -> throw result.throwable
        }
    }
}

@Composable
fun rememberSharedPaletteNetworkLoader(): NetworkLoader {
    val context = LocalPlatformContext.current
    return remember(context) { CoilPaletteNetworkLoader(context) }
}
