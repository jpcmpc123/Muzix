package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import com.kyant.backdrop.backdrops.layerBackdrop as nativeBackdrop

/**
 * The backdrop a glass surface samples from.
 *
 * This used to be an `expect class` with a real Android actual and a no-op JVM one,
 * because desktop was assumed to have no Kyant backend. It does: `io.github.kyant0:backdrop`
 * is a KMP artifact declared in `commonMain`, and its desktop variant ships the same
 * API backed by `SkikoRuntimeShader`. So there is nothing left to abstract over — the
 * whole effect is plain common code, and the alias only survives so the ~20 call sites
 * keep reading `PlatformBackdrop`.
 */
typealias PlatformBackdrop = LayerBackdrop

/**
 * Root phone backdrop owned by `App.kt` and sampled by the floating bottom bar and MiniPlayer.
 * Screens with their own floating top bar / chips (Home, Library, Search) record their scrolling
 * content directly into this backdrop so the entire screen is captured ONCE per frame instead of
 * nesting two full-screen [LayerBackdrop] recordings.
 */
val LocalRootBackdrop = compositionLocalOf<PlatformBackdrop?> { null }

/**
 * Marks a composable as the source layer that sibling glass surfaces refract.
 * Automatically no-ops when [enabled] is false (defaulting to [LocalLiquidGlassEnabled]) so
 * screens never pay the offscreen recording cost while Liquid Glass is disabled.
 */
@Composable
fun Modifier.layerBackdrop(
    backdrop: PlatformBackdrop,
    enabled: Boolean = LocalLiquidGlassEnabled.current,
): Modifier = if (enabled) this.nativeBackdrop(backdrop) else this

/**
 * Wraps a destination route that does not manage its own scrolling backdrop so the root
 * bottom bar and MiniPlayer can still refract its content without wrapping sibling routes
 * (Home, Library, Search) in an outer nested [layerBackdrop].
 */
@Composable
fun RootBackdropSurface(content: @Composable () -> Unit) {
    val rootBackdrop = LocalRootBackdrop.current
    if (rootBackdrop != null) {
        Box(Modifier.fillMaxSize().layerBackdrop(rootBackdrop)) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun rememberBackdrop(color: Color): PlatformBackdrop =
    rememberLayerBackdrop {
        drawRect(color)
        drawContent()
    }
