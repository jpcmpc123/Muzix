package com.maxrave.simpmusic.expect.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import coil3.toBitmap
import com.maxrave.simpmusic.extension.toResizedBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

actual fun ImageBitmap.toByteArray(): ByteArray? {
    val image = Image.makeFromBitmap(this.asSkiaBitmap())
    val bytesArray =
        image.encodeToData(EncodedImageFormat.JPEG, 100)?.bytes
    return bytesArray
}

actual fun ImageBitmap.toPngByteArray(): ByteArray? {
    val image = Image.makeFromBitmap(this.asSkiaBitmap())
    return image.encodeToData(EncodedImageFormat.PNG)?.bytes
}

actual fun coil3.Image.toImageBitmap(): ImageBitmap =
    this.toBitmap().asComposeImageBitmap()

// Skia bitmaps live in memory and can always be read, so a plain resize is enough here.
actual fun ImageBitmap.toReadableBitmap(
    width: Int,
    height: Int,
): ImageBitmap = toResizedBitmap(width, height)

actual fun imageBitmapFromArgb(
    pixels: IntArray,
    width: Int,
    height: Int,
): ImageBitmap {
    val safeW = width.coerceAtLeast(1)
    val safeH = height.coerceAtLeast(1)
    val bytes = ByteArray(safeW * safeH * 4)
    val count = minOf(pixels.size, safeW * safeH)
    for (i in 0 until count) {
        val argb = pixels[i]
        val base = i shl 2
        bytes[base] = (argb and 0xFF).toByte()
        bytes[base + 1] = ((argb shr 8) and 0xFF).toByte()
        bytes[base + 2] = ((argb shr 16) and 0xFF).toByte()
        bytes[base + 3] = ((argb ushr 24) and 0xFF).toByte()
    }
    val skiaBitmap = org.jetbrains.skia.Bitmap()
    val info =
        org.jetbrains.skia.ImageInfo(
            safeW,
            safeH,
            org.jetbrains.skia.ColorType.BGRA_8888,
            org.jetbrains.skia.ColorAlphaType.UNPREMUL,
        )
    skiaBitmap.allocPixels(info)
    skiaBitmap.installPixels(bytes)
    return skiaBitmap.asComposeImageBitmap()
}
