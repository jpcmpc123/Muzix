package com.maxrave.simpmusic.utils

import androidx.compose.ui.graphics.ImageBitmap
import com.maxrave.simpmusic.expect.ui.imageBitmapFromArgb
import com.maxrave.simpmusic.expect.ui.toReadableBitmap
import kotlin.math.abs

/**
 * Downsamples [source] to `[targetWidth] x [targetHeight]` and runs an in-place $O(1)$-per-pixel
 * StackBlur so full-screen artwork/mesh backdrops and API < 31 lyric caches never need live GPU
 * `.blur()` passes. Call off the main thread (`Dispatchers.Default`).
 */
internal fun blurImageBitmap(
    source: ImageBitmap,
    targetWidth: Int,
    targetHeight: Int,
    radius: Int,
): ImageBitmap {
    val w = targetWidth.coerceAtLeast(1)
    val h = targetHeight.coerceAtLeast(1)
    val readable = source.toReadableBitmap(w, h)
    val pixels = IntArray(w * h)
    readable.readPixels(pixels)
    if (radius >= 1) {
        stackBlurArgb(pixels, w, h, radius)
    }
    return imageBitmapFromArgb(pixels, w, h)
}

/**
 * In-place fast $O(1)$-per-pixel StackBlur over packed `ARGB_8888` pixels (based on Mario
 * Klingemann's StackBlur algorithm). Zero allocations in the inner loops and preserves/blurs alpha
 * cleanly for both opaque backdrops and transparent-background text bitmaps.
 */
internal fun stackBlurArgb(
    pixels: IntArray,
    width: Int,
    height: Int,
    radius: Int,
) {
    if (width <= 0 || height <= 0 || radius < 1 || pixels.size < width * height) return

    val rCap = radius.coerceAtMost(254)
    val wm = width - 1
    val hm = height - 1
    val wh = width * height
    val div = rCap + rCap + 1

    val a = IntArray(wh)
    val r = IntArray(wh)
    val g = IntArray(wh)
    val b = IntArray(wh)

    val vmin = IntArray(maxOf(width, height))

    var divSum = (div + 1) shr 1
    divSum *= divSum
    val dv = IntArray(256 * divSum)
    for (i in dv.indices) {
        dv[i] = i / divSum
    }

    val stack = Array(div) { IntArray(4) }
    val r1 = rCap + 1

    var yw = 0
    var yi = 0

    // Horizontal pass
    for (y in 0 until height) {
        var asum = 0
        var rsum = 0
        var gsum = 0
        var bsum = 0
        var aoutsum = 0
        var routsum = 0
        var goutsum = 0
        var boutsum = 0
        var ainsum = 0
        var rinsum = 0
        var ginsum = 0
        var binsum = 0

        for (i in -rCap..rCap) {
            val p = pixels[yi + minOf(wm, maxOf(i, 0))]
            val sir = stack[i + rCap]
            sir[0] = (p ushr 24) and 0xFF
            sir[1] = (p shr 16) and 0xFF
            sir[2] = (p shr 8) and 0xFF
            sir[3] = p and 0xFF
            val rbs = r1 - abs(i)
            asum += sir[0] * rbs
            rsum += sir[1] * rbs
            gsum += sir[2] * rbs
            bsum += sir[3] * rbs
            if (i > 0) {
                ainsum += sir[0]
                rinsum += sir[1]
                ginsum += sir[2]
                binsum += sir[3]
            } else {
                aoutsum += sir[0]
                routsum += sir[1]
                goutsum += sir[2]
                boutsum += sir[3]
            }
        }

        var stackPointer = rCap
        for (x in 0 until width) {
            a[yi] = dv[asum]
            r[yi] = dv[rsum]
            g[yi] = dv[gsum]
            b[yi] = dv[bsum]

            asum -= aoutsum
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum

            var stackStart = stackPointer - rCap + div
            var sir = stack[stackStart % div]

            aoutsum -= sir[0]
            routsum -= sir[1]
            goutsum -= sir[2]
            boutsum -= sir[3]

            if (y == 0) {
                vmin[x] = minOf(x + rCap + 1, wm)
            }
            val p = pixels[yw + vmin[x]]

            sir[0] = (p ushr 24) and 0xFF
            sir[1] = (p shr 16) and 0xFF
            sir[2] = (p shr 8) and 0xFF
            sir[3] = p and 0xFF

            ainsum += sir[0]
            rinsum += sir[1]
            ginsum += sir[2]
            binsum += sir[3]

            asum += ainsum
            rsum += rinsum
            gsum += ginsum
            bsum += binsum

            stackPointer = (stackPointer + 1) % div
            sir = stack[stackPointer % div]

            aoutsum += sir[0]
            routsum += sir[1]
            goutsum += sir[2]
            boutsum += sir[3]

            ainsum -= sir[0]
            rinsum -= sir[1]
            ginsum -= sir[2]
            binsum -= sir[3]

            yi++
        }
        yw += width
    }

    // Vertical pass
    for (x in 0 until width) {
        var asum = 0
        var rsum = 0
        var gsum = 0
        var bsum = 0
        var aoutsum = 0
        var routsum = 0
        var goutsum = 0
        var boutsum = 0
        var ainsum = 0
        var rinsum = 0
        var ginsum = 0
        var binsum = 0

        var yp = -rCap * width
        for (i in -rCap..rCap) {
            yi = maxOf(0, yp) + x
            val sir = stack[i + rCap]
            sir[0] = a[yi]
            sir[1] = r[yi]
            sir[2] = g[yi]
            sir[3] = b[yi]
            val rbs = r1 - abs(i)
            asum += a[yi] * rbs
            rsum += r[yi] * rbs
            gsum += g[yi] * rbs
            bsum += b[yi] * rbs
            if (i > 0) {
                ainsum += sir[0]
                rinsum += sir[1]
                ginsum += sir[2]
                binsum += sir[3]
            } else {
                aoutsum += sir[0]
                routsum += sir[1]
                goutsum += sir[2]
                boutsum += sir[3]
            }
            if (i < hm) {
                yp += width
            }
        }

        yi = x
        var stackPointer = rCap
        for (y in 0 until height) {
            pixels[yi] =
                (dv[asum] shl 24) or
                    (dv[rsum] shl 16) or
                    (dv[gsum] shl 8) or
                    dv[bsum]

            asum -= aoutsum
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum

            val stackStart = stackPointer - rCap + div
            var sir = stack[stackStart % div]

            aoutsum -= sir[0]
            routsum -= sir[1]
            goutsum -= sir[2]
            boutsum -= sir[3]

            if (x == 0) {
                vmin[y] = minOf(y + r1, hm) * width
            }
            val p = x + vmin[y]

            sir[0] = a[p]
            sir[1] = r[p]
            sir[2] = g[p]
            sir[3] = b[p]

            ainsum += sir[0]
            rinsum += sir[1]
            ginsum += sir[2]
            binsum += sir[3]

            asum += ainsum
            rsum += rinsum
            gsum += ginsum
            bsum += binsum

            stackPointer = (stackPointer + 1) % div
            sir = stack[stackPointer]

            aoutsum += sir[0]
            routsum += sir[1]
            goutsum += sir[2]
            boutsum += sir[3]

            ainsum -= sir[0]
            rinsum -= sir[1]
            ginsum -= sir[2]
            binsum -= sir[3]

            yi += width
        }
    }
}
