package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
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
 * Marks a composable as the source layer that sibling glass surfaces refract.
 * Automatically no-ops when [enabled] is false (defaulting to [LocalLiquidGlassEnabled]) so
 * screens never pay the offscreen recording cost while Liquid Glass is disabled.
 */
@Composable
fun Modifier.layerBackdrop(
    backdrop: PlatformBackdrop,
    enabled: Boolean = LocalLiquidGlassEnabled.current,
): Modifier = if (enabled) this.nativeBackdrop(backdrop) else this

@Composable
fun rememberBackdrop(color: Color): PlatformBackdrop =
    rememberLayerBackdrop {
        drawRect(color)
        drawContent()
    }
