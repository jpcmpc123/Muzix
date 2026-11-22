package com.maxrave.simpmusic.ui.screen.home

import org.jetbrains.compose.resources.StringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.equalizer_preset_acoustic
import simpmusic.composeapp.generated.resources.equalizer_preset_bass_booster
import simpmusic.composeapp.generated.resources.equalizer_preset_bass_reducer
import simpmusic.composeapp.generated.resources.equalizer_preset_classical
import simpmusic.composeapp.generated.resources.equalizer_preset_dance
import simpmusic.composeapp.generated.resources.equalizer_preset_deep
import simpmusic.composeapp.generated.resources.equalizer_preset_electronic
import simpmusic.composeapp.generated.resources.equalizer_preset_flat
import simpmusic.composeapp.generated.resources.equalizer_preset_hiphop
import simpmusic.composeapp.generated.resources.equalizer_preset_jazz
import simpmusic.composeapp.generated.resources.equalizer_preset_latin
import simpmusic.composeapp.generated.resources.equalizer_preset_loudness
import simpmusic.composeapp.generated.resources.equalizer_preset_lounge
import simpmusic.composeapp.generated.resources.equalizer_preset_piano
import simpmusic.composeapp.generated.resources.equalizer_preset_pop
import simpmusic.composeapp.generated.resources.equalizer_preset_rnb
import simpmusic.composeapp.generated.resources.equalizer_preset_rock
import simpmusic.composeapp.generated.resources.equalizer_preset_small_speakers
import simpmusic.composeapp.generated.resources.equalizer_preset_spoken_word
import simpmusic.composeapp.generated.resources.equalizer_preset_treble_booster
import simpmusic.composeapp.generated.resources.equalizer_preset_treble_reducer
import simpmusic.composeapp.generated.resources.equalizer_preset_vocal_booster
import kotlin.math.abs

/**
 * A named curve for the ten ISO band centres the equalizer runs on, in dB per band.
 *
 * Names and order follow the list Spotify shows, because that is the set most listeners already
 * recognise. The gains are ours: neither Spotify nor Apple — whose preset list Spotify took — has
 * ever published the numbers behind those names, and both run a six-band equalizer, so there is
 * nothing to copy across to ten bands even in principle. Each curve here is written to the sense of
 * its own name and can be re-tuned freely without breaking anything that reads it.
 */
data class EqualizerPreset(
    val name: String,
    val labelRes: StringResource,
    val bandsDb: List<Float>,
) {
    /**
     * The preamp this curve wants, in dB.
     *
     * Derived instead of written out per preset: it is always the tallest boost pulled back down to
     * make room, so listing it twenty-two times would only be twenty-two chances to get it wrong.
     * A preset that only cuts asks for no headroom and comes out at zero — spelled out rather than
     * negating a max, which would hand storage a negative zero for every such preset.
     */
    val preampDb: Float
        get() {
            val loudest = bandsDb.maxOrNull() ?: 0f
            return if (loudest > 0f) -loudest else 0f
        }
}

/**
 * The preset list, in the order it is shown.
 *
 * Gains run left to right over 31, 62, 125, 250, 500 Hz, 1, 2, 4, 8, 16 kHz — the centres named in
 * `EQUALIZER_BAND_LABELS`.
 */
val EQUALIZER_PRESETS: List<EqualizerPreset> =
    listOf(
        preset("Flat", Res.string.equalizer_preset_flat, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        preset("Acoustic", Res.string.equalizer_preset_acoustic, 4f, 4f, 3f, 1f, 1.5f, 1.5f, 3f, 3.5f, 3f, 1.5f),
        preset("Bass booster", Res.string.equalizer_preset_bass_booster, 6f, 5.5f, 4.5f, 2.5f, 0.5f, 0f, 0f, 0f, 0f, 0f),
        preset("Bass reducer", Res.string.equalizer_preset_bass_reducer, -6f, -5.5f, -4.5f, -2.5f, -0.5f, 0f, 0f, 0f, 0f, 0f),
        preset("Classical", Res.string.equalizer_preset_classical, 4.5f, 3.5f, 3f, 2.5f, -1.5f, -1.5f, 0f, 2f, 3f, 3.5f),
        preset("Dance", Res.string.equalizer_preset_dance, 5f, 6f, 3.5f, 0f, 2f, 3f, 4f, 4f, 3f, 0f),
        preset("Deep", Res.string.equalizer_preset_deep, 6f, 5f, 3f, 1.5f, 3f, 2f, 0.5f, -2.5f, -4f, -5f),
        preset("Electronic", Res.string.equalizer_preset_electronic, 5f, 4.5f, 1.5f, 0f, -1.5f, 2f, 1f, 1.5f, 4.5f, 5f),
        preset("HipHop", Res.string.equalizer_preset_hiphop, 6f, 5f, 1.5f, 3f, -1f, -1f, 1.5f, -1f, 2f, 3f),
        preset("Jazz", Res.string.equalizer_preset_jazz, 4f, 3f, 1.5f, 2f, -1.5f, -1.5f, 0f, 1.5f, 3f, 4f),
        preset("Latin", Res.string.equalizer_preset_latin, 5f, 3f, 0f, 0f, -1.5f, -1.5f, -1.5f, 0f, 3f, 5f),
        preset("Loudness", Res.string.equalizer_preset_loudness, 6f, 5f, 0f, 0f, -2f, 0f, -1f, -5f, 5f, 1f),
        preset("Lounge", Res.string.equalizer_preset_lounge, -3f, -1.5f, -0.5f, 1.5f, 4f, 2.5f, 0f, -1.5f, 2f, 1f),
        preset("Piano", Res.string.equalizer_preset_piano, 3f, 2f, 0f, 2.5f, 3f, 1f, 2f, 4f, 3f, 3f),
        preset("Pop", Res.string.equalizer_preset_pop, -1.5f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f, -1.5f),
        preset("RnB", Res.string.equalizer_preset_rnb, 5.5f, 6f, 4.5f, 1f, -2.5f, -1f, 2.5f, 3f, 3.5f, 4f),
        preset("Rock", Res.string.equalizer_preset_rock, 5f, 4f, 3f, 1.5f, -0.5f, -1f, 0.5f, 3f, 4f, 4.5f),
        preset("Small speakers", Res.string.equalizer_preset_small_speakers, 6f, 5f, 3.5f, 2f, 0f, -0.5f, 0f, 1.5f, 2.5f, 2f),
        preset("Spoken word", Res.string.equalizer_preset_spoken_word, -4f, -3.5f, -2f, 0f, 3f, 4.5f, 5f, 4f, 2f, -1f),
        preset("Treble booster", Res.string.equalizer_preset_treble_booster, 0f, 0f, 0f, 0f, 0f, 0f, 2.5f, 4.5f, 5.5f, 6f),
        preset("Treble reducer", Res.string.equalizer_preset_treble_reducer, 0f, 0f, 0f, 0f, 0f, 0f, -2.5f, -4.5f, -5.5f, -6f),
        preset("Vocal booster", Res.string.equalizer_preset_vocal_booster, -2.5f, -3f, -2.5f, 1.5f, 4f, 4f, 3.5f, 2f, 0f, -1.5f),
    )

private fun preset(
    name: String,
    labelRes: StringResource,
    vararg bandsDb: Float,
) = EqualizerPreset(name, labelRes, bandsDb.toList())

/**
 * The preset [bandsDb] currently sits on, or null once the curve has been dragged off every one.
 *
 * Read back off the curve rather than stored alongside it, so there is only one thing to keep
 * right: dragging a band drops the label to Custom by itself, and a preset re-selects itself if the
 * curve is ever put back on it. The tolerance is there because the stored gains are floats that
 * have been through text.
 */
fun equalizerPresetFor(bandsDb: List<Float>): EqualizerPreset? =
    EQUALIZER_PRESETS.firstOrNull { preset ->
        preset.bandsDb.indices.all { i -> abs(preset.bandsDb[i] - bandsDb.getOrElse(i) { 0f }) < 0.01f }
    }

/**
 * The imported AutoEq profile's label for [bandsDb], or null once the curve has moved off it.
 *
 * [stored] is the `"<label>\n<gains>"` pair written at import. Read the same way a preset is —
 * by comparing against the curve actually in force — so dragging a band drops the headphone name
 * without anything having to notice and clear it.
 */
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
    val matches = bandsDb.indices.all { i -> abs(saved.getOrElse(i) { 0f } - bandsDb[i]) < 0.01f }
    return label.takeIf { matches }
}
