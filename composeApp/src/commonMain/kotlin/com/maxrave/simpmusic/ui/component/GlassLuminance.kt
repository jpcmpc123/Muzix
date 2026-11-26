package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.maxrave.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sign

private const val SAMPLE_SIZE = 5
private const val SAMPLE_INTERVAL_MS = 2_500L
private const val CAPTURE_SETTLE_DELAY_MS = 100L
private const val LUMINANCE_CHANGE_THRESHOLD = 0.02f

/**
 * Coordinates on-demand offscreen capture for glass luminance sampling.
 * Instead of recording the backdrop into a scratch [GraphicsLayer] on every single frame
 * (150–300× per 2.5 s window), [shouldRecordFrame] returns true for only the single draw pass
 * right after [requestCapture] is called, and [takeFreshFrame] ensures idle screens skip
 * GPU→CPU `readPixels()` stalls completely.
 */
internal class GlassCaptureGate {
    private var captureRequested = true
    private var hasFreshFrame = false

    fun requestCapture() {
        captureRequested = true
    }

    fun shouldRecordFrame(): Boolean {
        if (!captureRequested) return false
        captureRequested = false
        hasFreshFrame = true
        return true
    }

    fun takeFreshFrame(): Boolean {
        if (!hasFreshFrame) return false
        hasFreshFrame = false
        return true
    }
}

internal class GlassLuminanceState(
    val luminance: State<Float>,
    private val gate: GlassCaptureGate,
) {
    fun shouldRecordFrame(): Boolean = gate.shouldRecordFrame()
}

/**
 * Computes the luminance-driven glass blur radius in dp, quantized to [stepDp] (default 1 dp)
 * so smooth luminance animations update the surface scrim at 60–120 FPS without forcing Android
 * to rebuild the hardware `RenderEffect` chain on every animation frame during `tween(500)`.
 */
internal fun quantizedGlassBlurDp(
    luminance: Float,
    blurScale: Float = 1f,
    extraDp: Float = 0f,
    maxDp: Float = 24f,
    stepDp: Float = 1f,
): Float {
    val l = (luminance * 2f - 1f).let { sign(it) * it * it }
    val baseDp =
        if (l > 0f) {
            lerp(8f, 16f, l)
        } else {
            lerp(8f, 2f, -l)
        }
    val raw = (baseDp * blurScale + extraDp).coerceIn(0f, maxDp)
    return if (stepDp > 0f) round(raw / stepDp) * stepDp else raw
}

/**
 * Samples a tiny 5×5 GPU-rendered copy of [source] on demand (at most once every 2.5 s,
 * and zero times while the screen is static). Callers pass [GlassLuminanceState.shouldRecordFrame]
 * into `drawInteractiveGlass` so `layer.record { drawBackdrop() }` executes on only 1 frame
 * per sample interval rather than every frame.
 */
@Composable
internal fun rememberGlassLuminanceState(
    source: GraphicsLayer,
    enabled: Boolean = true,
): GlassLuminanceState {
    val sample = rememberGraphicsLayer()
    val luminance = remember { Animatable(0f) }
    val gate = remember(source) { GlassCaptureGate() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    LaunchedEffect(source, sample, enabled, lifecycle, density, layoutDirection) {
        if (!enabled) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
            gate.requestCapture()
            delay(CAPTURE_SETTLE_DELAY_MS)
            while (isActive) {
                val sourceSize = source.size
                // A layer has no display list until its first draw, and idle screens produce no fresh frame.
                if (gate.takeFreshFrame() && sourceSize.width > 0 && sourceSize.height > 0) {
                    val target =
                        try {
                            sample.record(density, layoutDirection, IntSize(SAMPLE_SIZE, SAMPLE_SIZE)) {
                                scale(
                                    scaleX = SAMPLE_SIZE.toFloat() / sourceSize.width,
                                    scaleY = SAMPLE_SIZE.toFloat() / sourceSize.height,
                                    pivot = Offset.Zero,
                                ) {
                                    drawLayer(source)
                                }
                            }
                            sample.toImageBitmap().readPixels(pixels)
                            val average =
                                pixels.sumOf { color ->
                                    val r = (color shr 16 and 0xFF) / 255.0
                                    val g = (color shr 8 and 0xFF) / 255.0
                                    val b = (color and 0xFF) / 255.0
                                    0.2126 * r + 0.7152 * g + 0.0722 * b
                                } / pixels.size
                            average.toFloat().coerceIn(0.3f, 0.8f)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            // Keep the last tint and retry on the next lifecycle start, not in a loop.
                            Logger.e("GlassLuminance", "Unable to sample glass background", error)
                            return@repeatOnLifecycle
                        }
                    if (abs(target - luminance.targetValue) >= LUMINANCE_CHANGE_THRESHOLD) {
                        luminance.animateTo(target, tween(500))
                    }
                }
                delay(SAMPLE_INTERVAL_MS)
                gate.requestCapture()
                delay(CAPTURE_SETTLE_DELAY_MS)
            }
        }
    }
    return remember(luminance, gate) { GlassLuminanceState(luminance.asState(), gate) }
}

/** Backwards-compatible helper returning only the animated luminance [State]. */
@Composable
internal fun rememberGlassLuminance(
    source: GraphicsLayer,
    enabled: Boolean = true,
): State<Float> = rememberGlassLuminanceState(source, enabled).luminance
