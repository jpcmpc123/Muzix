package com.maxrave.simpmusic.ui.screen.home

import com.maxrave.domain.data.player.EqualizerConfig
import com.maxrave.domain.data.player.EqualizerEnhancements
import com.maxrave.domain.data.player.PreampMode
import com.maxrave.domain.data.player.computePeakResponseDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EqualizerPresetsTest {
    @Test
    fun allFactoryPresetsHaveTenBandsSafeHeadroomAndUniqueNames() {
        val names = HashSet<String>()
        EQUALIZER_PRESETS.forEach { preset ->
            assertTrue(names.add(preset.name), "Duplicate preset name: ${preset.name}")
            assertEquals(10, preset.bandsDb.size, "Expected 10 bands on ${preset.name}")
            val config =
                EqualizerConfig(
                    enabled = true,
                    bandsDb = preset.bandsDb,
                    manualPreampDb = preset.preampDb,
                    enhancements =
                        EqualizerEnhancements(
                            preampMode = PreampMode.MANUAL,
                            bassBoostDb = preset.bassBoostDb,
                            trebleBoostDb = preset.trebleBoostDb,
                            stereoWidth = preset.stereoWidth,
                            loudnessAmount = preset.loudnessAmount,
                        ),
                )
            val summedPeak = computePeakResponseDb(config, 48_000.0)
            assertTrue(
                summedPeak + preset.preampDb <= 0.1f,
                "Preset ${preset.name} peak ($summedPeak dB) exceeds manual preamp (${preset.preampDb} dB)",
            )
            assertEquals(preset.name, equalizerPresetFor(preset.bandsDb)?.name)
        }
        assertNotNull(EQUALIZER_PRESETS.firstOrNull { it.name == "Hi-Fi / Harman" })
        assertNotNull(EQUALIZER_PRESETS.firstOrNull { it.name == "Clean Bass" })
    }
}
