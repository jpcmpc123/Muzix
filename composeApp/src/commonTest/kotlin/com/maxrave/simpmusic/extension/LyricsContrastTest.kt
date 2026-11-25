package com.maxrave.simpmusic.extension

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

class LyricsContrastTest {
    @Test
    fun clampsBrightAndPastelAlbumColorsToReadableBackdropCeiling() {
        val brightColors =
            listOf(
                Color(0xFF00B59E), // Bright teal (Flor De Lis)
                Color(0xFFFFFFFF), // Pure white
                Color(0xFFFFEE58), // Bright yellow
                Color(0xFF80DEEA), // Pastel cyan
                Color(0xFFFFB3C6), // Pastel pink
            )

        for (color in brightColors) {
            val clampedBackdrop = color.toReadableLyricsBackdrop()
            val clampedCard = color.toReadableLyricsCardColor()
            assertTrue(
                clampedBackdrop.luminance() <= 0.141f,
                "Backdrop luminance ${clampedBackdrop.luminance()} exceeded 0.14 for $color",
            )
            assertTrue(
                clampedCard.luminance() <= 0.161f,
                "Card luminance ${clampedCard.luminance()} exceeded 0.16 for $color",
            )
            assertTrue(clampedBackdrop.alpha == 1f)
            assertTrue(clampedCard.alpha == 1f)
        }
    }

    @Test
    fun keepsDarkColorsDarkWithoutWashingOutContrast() {
        val darkMoody = Color(0xFF1A0E24)
        val result = darkMoody.toReadableLyricsBackdrop()
        assertTrue(result.luminance() <= 0.14f)
        assertTrue(result.blue >= result.green)
    }
}
