package com.maxrave.simpmusic.expect.ui

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
