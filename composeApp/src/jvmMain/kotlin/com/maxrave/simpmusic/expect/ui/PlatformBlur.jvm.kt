package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

// skiko implements Modifier.blur on every desktop target, with no version gate to respect. This is
// plain Compose blur, NOT haze's progressive path — that one throws NoSuchMethodError against the
// pinned Compose (see CLAUDE.md), and nothing here goes near it.
actual fun isLyricsBlurSupported(): Boolean = true

actual fun isHardwareRenderEffectBlurSupported(): Boolean = true

actual fun isLiquidGlassSupported(): Boolean = true

@Composable
actual fun rememberIsPowerSaveMode(): Boolean = false
