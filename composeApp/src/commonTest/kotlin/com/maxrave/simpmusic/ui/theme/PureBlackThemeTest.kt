package com.maxrave.simpmusic.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PureBlackThemeTest {
    @Test
    fun `withPureBlackSurfaces applies YouTube Music pure black palette and preserves accents`() {
        val accentPrimary = Color(0xFF8ECAE6)
        val accentSecondary = Color(0xFFAAD7EE)
        val accentTertiary = Color(0xFFD0C4F8)
        val base =
            darkColorScheme(
                primary = accentPrimary,
                secondary = accentSecondary,
                tertiary = accentTertiary,
                background = Color(0xFF111318),
                surface = Color(0xFF111318),
            )

        val pureBlack = base.withPureBlackSurfaces()

        // Pure OLED #000000 grounds
        assertEquals(Color.Black, pureBlack.background)
        assertEquals(Color.Black, pureBlack.surface)
        assertEquals(Color.Black, pureBlack.surfaceDim)
        assertEquals(Color.Black, pureBlack.surfaceContainerLowest)

        // Elevated neutral charcoal tiers (R=G=B) and transparent surfaceTint
        assertEquals(Color(0xFF0E0E0E), pureBlack.surfaceContainerLow)
        assertEquals(Color(0xFF141414), pureBlack.surfaceContainer)
        assertEquals(Color(0xFF1C1C1C), pureBlack.surfaceContainerHigh)
        assertEquals(Color(0xFF262626), pureBlack.surfaceContainerHighest)
        assertEquals(Color(0xFF262626), pureBlack.outlineVariant)
        assertEquals(Color.Transparent, pureBlack.surfaceTint)

        // High-contrast readable text
        assertEquals(Color.White, pureBlack.onBackground)
        assertEquals(Color.White, pureBlack.onSurface)
        assertEquals(Color(0xFFAAAAAA), pureBlack.onSurfaceVariant)

        // Theme color accents remain intact
        assertEquals(accentPrimary, pureBlack.primary)
        assertEquals(accentSecondary, pureBlack.secondary)
        assertEquals(accentTertiary, pureBlack.tertiary)
        assertNotEquals(Color.Black, pureBlack.surfaceContainer)
    }

    @Test
    fun `isPureBlackTheme activates only when dark theme is active and enabled`() {
        assertFalse(isPureBlackTheme(isDark = true, pureBlackEnabled = false))
        assertFalse(isPureBlackTheme(isDark = false, pureBlackEnabled = false))
        assertFalse(isPureBlackTheme(isDark = false, pureBlackEnabled = true))
        assertTrue(isPureBlackTheme(isDark = true, pureBlackEnabled = true))
    }
}
