package com.maxrave.simpmusic.expect.ui

import android.os.Build

actual fun isLyricsBlurSupported(): Boolean = true

actual fun isHardwareRenderEffectBlurSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
