package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

/**
 * Whether Apple Music lyrics blur can be rendered on this platform. Always true now that
 * Android < 12 uses an automatic background StackBlur fallback while Android 12+ / Desktop
 * use quantized hardware `RenderEffect` pooling.
 */
expect fun isLyricsBlurSupported(): Boolean

/**
 * Whether Compose `graphicsLayer { renderEffect = BlurEffect(...) }` is supported natively by
 * the platform (Android 12+ API 31 and Desktop Skiko).
 */
expect fun isHardwareRenderEffectBlurSupported(): Boolean

/**
 * Whether AGSL/Skiko runtime-shader Liquid Glass is supported natively on this device
 * (Android 13+ API 33 or Desktop Skiko).
 */
expect fun isLiquidGlassSupported(): Boolean

/**
 * Observes whether system power-save / battery-saver mode is active so heavy continuous
 * runtime shaders can automatically fall back to flat surfaces until battery saver is off.
 */
@Composable
expect fun rememberIsPowerSaveMode(): Boolean
