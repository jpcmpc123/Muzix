package com.maxrave.simpmusic.ui.screen.home

import com.maxrave.domain.data.player.EqualizerConfig
import com.maxrave.domain.data.player.EqualizerEnhancements
import com.maxrave.domain.data.player.PreampMode
import com.maxrave.domain.data.player.computePeakResponseDb
import org.jetbrains.compose.resources.StringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.equalizer_preset_acoustic
import simpmusic.composeapp.generated.resources.equalizer_preset_bass_booster
import simpmusic.composeapp.generated.resources.equalizer_preset_bass_reducer
import simpmusic.composeapp.generated.resources.equalizer_preset_car
import simpmusic.composeapp.generated.resources.equalizer_preset_classical
import simpmusic.composeapp.generated.resources.equalizer_preset_clean_bass
import simpmusic.composeapp.generated.resources.equalizer_preset_dance
import simpmusic.composeapp.generated.resources.equalizer_preset_deep
import simpmusic.composeapp.generated.resources.equalizer_preset_electronic
import simpmusic.composeapp.generated.resources.equalizer_preset_flat
import simpmusic.composeapp.generated.resources.equalizer_preset_hifi_harman
import simpmusic.composeapp.generated.resources.equalizer_preset_hiphop
import simpmusic.composeapp.generated.resources.equalizer_preset_jazz
import simpmusic.composeapp.generated.resources.equalizer_preset_latin
import simpmusic.composeapp.generated.resources.equalizer_preset_loudness
import simpmusic.composeapp.generated.resources.equalizer_preset_lounge
import simpmusic.composeapp.generated.resources.equalizer_preset_open_stage
import simpmusic.composeapp.generated.resources.equalizer_preset_piano
import simpmusic.composeapp.generated.resources.equalizer_preset_pop
import simpmusic.composeapp.generated.resources.equalizer_preset_rnb
import simpmusic.composeapp.generated.resources.equalizer_preset_rock
import simpmusic.composeapp.generated.resources.equalizer_preset_small_speakers
import simpmusic.composeapp.generated.resources.equalizer_preset_spoken_word
import simpmusic.composeapp.generated.resources.equalizer_preset_studio
import simpmusic.composeapp.generated.resources.equalizer_preset_treble_booster
import simpmusic.composeapp.generated.resources.equalizer_preset_treble_reducer
import simpmusic.composeapp.generated.resources.equalizer_preset_vocal_booster
import kotlin.math.abs

/**
 * Acoustically calibrated presets over 31, 62, 125, 250, 500 Hz, 1, 2, 4, 8, 16 kHz.
 *
 * Preamp values are computed from the true cascaded frequency response peak ([computePeakResponseDb])
 * so manual-preamp mode also remains headroom-safe without relying solely on max single-band gain.
 */
data class EqualizerPreset(
    val name: String,
    val labelRes: StringResource,
    val bandsDb: List<Float>,
    val bassBoostDb: Float = 0f,
    val trebleBoostDb: Float = 0f,
    val stereoWidth: Float = 1f,
    val spatialStage: Float = 0f,
    val loudnessAmount: Float = 0f,
) {
    val preampDb: Float
        get() {
            val peak =
                computePeakResponseDb(
                    EqualizerConfig(
                        enabled = true,
                        bandsDb = bandsDb,
                        enhancements =
                            EqualizerEnhancements(
                                preampMode = PreampMode.AUTO_STRICT,
                                bassBoostDb = bassBoostDb,
                                trebleBoostDb = trebleBoostDb,
                                stereoWidth = stereoWidth,
                                spatialStage = spatialStage,
                                loudnessAmount = loudnessAmount,
                            ),
                    ),
                )
            return if (peak > 0.2f) -(peak + 0.5f).coerceAtMost(12f) else 0f
        }
}

val EQUALIZER_PRESETS: List<EqualizerPreset> =
    listOf(
        preset("Flat", Res.string.equalizer_preset_flat, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        preset(
            "Hi-Fi / Harman",
            Res.string.equalizer_preset_hifi_harman,
            2.5f, 2f, 0.5f, -1f, 0f, 0.5f, 1.5f, 2f, 2f, 2f,
            stereoWidth = 1.20f,
            spatialStage = 0.15f,
        ),
        preset(
            "Open Stage",
            Res.string.equalizer_preset_open_stage,
            2f, 1.5f, 0f, -1.5f, -0.5f, 0.5f, 1.5f, 2f, 2.5f, 2.5f,
            trebleBoostDb = 1f,
            stereoWidth = 1.30f,
            spatialStage = 0.30f,
        ),
        preset(
            "Clean Bass",
            Res.string.equalizer_preset_clean_bass,
            4.5f, 4f, 1.5f, -1f, 0f, 0.5f, 1f, 1.5f, 1.5f, 1f,
            bassBoostDb = 2f,
        ),
        preset("Acoustic", Res.string.equalizer_preset_acoustic, 3.5f, 3.5f, 2f, 0.5f, 1.5f, 1.5f, 2.5f, 3f, 2.5f, 1.5f),
        preset("Bass booster", Res.string.equalizer_preset_bass_booster, 5.5f, 5f, 2.5f, 0.5f, -0.5f, 0f, 0.5f, 1f, 0.5f, 0f),
        preset("Bass reducer", Res.string.equalizer_preset_bass_reducer, -6f, -5.5f, -4f, -2f, -0.5f, 0f, 0f, 0f, 0f, 0f),
        preset("Car Audio", Res.string.equalizer_preset_car, 4f, 3.5f, 1f, -1.5f, -0.5f, 0.5f, 1.5f, 2.5f, 3f, 2.5f),
        preset("Classical", Res.string.equalizer_preset_classical, 4f, 3f, 2.5f, 1.5f, -1f, -1f, 0.5f, 2f, 3f, 3.5f),
        preset("Dance", Res.string.equalizer_preset_dance, 4.5f, 5.5f, 2.5f, 0f, 1.5f, 2.5f, 3.5f, 3.5f, 2.5f, 1f),
        preset("Deep", Res.string.equalizer_preset_deep, 5f, 4.5f, 2.5f, 1f, 2f, 1.5f, 0.5f, -2f, -3f, -4f),
        preset("Electronic", Res.string.equalizer_preset_electronic, 4.5f, 4f, 1.5f, 0f, -1f, 1.5f, 1f, 1.5f, 4f, 4.5f),
        preset("HipHop", Res.string.equalizer_preset_hiphop, 5.5f, 4.5f, 1.5f, 2f, -1f, -0.5f, 1.5f, -0.5f, 2f, 3f),
        preset("Jazz", Res.string.equalizer_preset_jazz, 3.5f, 2.5f, 1.5f, 1.5f, -1f, -1f, 0.5f, 1.5f, 2.5f, 3.5f),
        preset("Latin", Res.string.equalizer_preset_latin, 4.5f, 3f, 0.5f, 0f, -1f, -1f, -1f, 0.5f, 3f, 4.5f),
        preset("Loudness", Res.string.equalizer_preset_loudness, 4.5f, 3.5f, 1.5f, 0f, -1f, -0.5f, 0.5f, 2f, 3.5f, 3f),
        preset("Lounge", Res.string.equalizer_preset_lounge, -2.5f, -1f, 0f, 1.5f, 3.5f, 2f, 0.5f, -1f, 1.5f, 1f),
        preset("Piano", Res.string.equalizer_preset_piano, 2.5f, 2f, 0f, 2f, 2.5f, 1f, 2f, 3.5f, 2.5f, 2.5f),
        preset("Pop", Res.string.equalizer_preset_pop, 2f, 2.5f, 1f, 0f, -0.5f, 0.5f, 1.5f, 2f, 2.5f, 2f),
        preset("RnB", Res.string.equalizer_preset_rnb, 5f, 5f, 3f, 0.5f, -1.5f, -0.5f, 2f, 2.5f, 3f, 3.5f),
        preset("Rock", Res.string.equalizer_preset_rock, 4.5f, 3.5f, 2f, 1f, -0.5f, -0.5f, 1f, 3f, 3.5f, 4f),
        preset("Small speakers", Res.string.equalizer_preset_small_speakers, -4f, -1.5f, 3f, 2.5f, 0.5f, 1f, 2.5f, 3f, 2f, 1f),
        preset("Spoken word", Res.string.equalizer_preset_spoken_word, -4f, -3.5f, -2f, 0f, 3f, 4.5f, 5f, 4f, 2f, -1f),
        preset("Studio", Res.string.equalizer_preset_studio, 0.5f, 0f, -0.5f, 0f, 0f, 0f, 0.5f, 1f, 1f, 0.5f),
        preset("Treble booster", Res.string.equalizer_preset_treble_booster, 0f, 0f, 0f, 0f, 0f, 0f, 2f, 4f, 5f, 5.5f),
        preset("Treble reducer", Res.string.equalizer_preset_treble_reducer, 0f, 0f, 0f, 0f, 0f, 0f, -2.5f, -4.5f, -5.5f, -6f),
        preset("Vocal booster", Res.string.equalizer_preset_vocal_booster, -2.5f, -3f, -2f, 1.5f, 4f, 4f, 3.5f, 2f, 0f, -1.5f),
    )

private fun preset(
    name: String,
    labelRes: StringResource,
    vararg bandsDb: Float,
    bassBoostDb: Float = 0f,
    trebleBoostDb: Float = 0f,
    stereoWidth: Float = 1f,
    spatialStage: Float = 0f,
    loudnessAmount: Float = 0f,
) = EqualizerPreset(
    name = name,
    labelRes = labelRes,
    bandsDb = bandsDb.toList(),
    bassBoostDb = bassBoostDb,
    trebleBoostDb = trebleBoostDb,
    stereoWidth = stereoWidth,
    spatialStage = spatialStage,
    loudnessAmount = loudnessAmount,
)

fun equalizerPresetFor(bandsDb: List<Float>): EqualizerPreset? =
    EQUALIZER_PRESETS.firstOrNull { preset ->
        preset.bandsDb.indices.all { i -> abs(preset.bandsDb[i] - bandsDb.getOrElse(i) { 0f }) < 0.05f }
    }

fun autoEqLabelFor(
    stored: String,
    bandsDb: List<Float>,
): String? {
    if (stored.isBlank()) return null
    val label = stored.substringBefore('\n').takeIf { it.isNotBlank() } ?: return null
    val saved =
        stored
            .substringAfter('\n', "")
            .split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
    if (saved.isEmpty()) return null
    val matches = bandsDb.indices.all { i -> abs(saved.getOrElse(i) { 0f } - bandsDb[i]) < 0.05f }
    return label.takeIf { matches }
}
