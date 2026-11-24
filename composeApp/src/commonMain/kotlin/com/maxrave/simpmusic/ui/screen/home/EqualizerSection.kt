package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.data.player.AudioOutputKind
import com.maxrave.domain.data.player.EQUALIZER_BASS_MAX_DB
import com.maxrave.domain.data.player.EQUALIZER_PREAMP_MAX_DB
import com.maxrave.domain.data.player.EQUALIZER_PREAMP_MIN_DB
import com.maxrave.domain.data.player.EQUALIZER_STEREO_WIDTH_MAX
import com.maxrave.domain.data.player.EQUALIZER_STEREO_WIDTH_MIN
import com.maxrave.domain.data.player.EQUALIZER_TREBLE_MAX_DB
import com.maxrave.domain.data.player.EQUALIZER_TREBLE_MIN_DB
import com.maxrave.domain.data.player.EqualizerConfig
import com.maxrave.domain.data.player.EqualizerEnhancements
import com.maxrave.domain.data.player.PreampMode
import com.maxrave.domain.data.player.SavedEqualizerProfile
import com.maxrave.simpmusic.extension.LeftToRight
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.EQUALIZER_BAND_LABELS
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.equalizer_autoeq
import simpmusic.composeapp.generated.resources.equalizer_bass_boost
import simpmusic.composeapp.generated.resources.equalizer_bass_mono
import simpmusic.composeapp.generated.resources.equalizer_bypass_active
import simpmusic.composeapp.generated.resources.equalizer_bypass_off
import simpmusic.composeapp.generated.resources.equalizer_delete
import simpmusic.composeapp.generated.resources.equalizer_delete_confirm
import simpmusic.composeapp.generated.resources.equalizer_duplicate
import simpmusic.composeapp.generated.resources.equalizer_effective_preamp
import simpmusic.composeapp.generated.resources.equalizer_include_autoeq
import simpmusic.composeapp.generated.resources.equalizer_limiter
import simpmusic.composeapp.generated.resources.equalizer_limiter_desc
import simpmusic.composeapp.generated.resources.equalizer_loudness
import simpmusic.composeapp.generated.resources.equalizer_my_profiles
import simpmusic.composeapp.generated.resources.equalizer_preamp
import simpmusic.composeapp.generated.resources.equalizer_preamp_auto_balanced
import simpmusic.composeapp.generated.resources.equalizer_preamp_auto_strict
import simpmusic.composeapp.generated.resources.equalizer_preamp_manual
import simpmusic.composeapp.generated.resources.equalizer_preamp_mode
import simpmusic.composeapp.generated.resources.equalizer_preset_custom
import simpmusic.composeapp.generated.resources.equalizer_presets
import simpmusic.composeapp.generated.resources.equalizer_profile_default_name
import simpmusic.composeapp.generated.resources.equalizer_profile_modified
import simpmusic.composeapp.generated.resources.equalizer_profile_name
import simpmusic.composeapp.generated.resources.equalizer_protection_section
import simpmusic.composeapp.generated.resources.equalizer_rename
import simpmusic.composeapp.generated.resources.equalizer_reset
import simpmusic.composeapp.generated.resources.equalizer_save_as_new
import simpmusic.composeapp.generated.resources.equalizer_save_profile
import simpmusic.composeapp.generated.resources.equalizer_save_profile_title
import simpmusic.composeapp.generated.resources.equalizer_stereo_width
import simpmusic.composeapp.generated.resources.equalizer_subsonic
import simpmusic.composeapp.generated.resources.equalizer_subsonic_desc
import simpmusic.composeapp.generated.resources.equalizer_tone_section
import simpmusic.composeapp.generated.resources.equalizer_treble_boost
import simpmusic.composeapp.generated.resources.equalizer_update_profile
import simpmusic.composeapp.generated.resources.equalizer_view_curve
import simpmusic.composeapp.generated.resources.equalizer_view_sliders
import kotlin.math.abs
import kotlin.math.roundToInt

private const val BAND_RANGE_DB = 12f
private val CURVE_HEIGHT = 220.dp
private val LABEL_COLUMN_WIDTH = 78.dp

/**
 * Studio-grade 10-band equalizer control surface shared by Settings and the full-screen Player sheet.
 */
@Composable
fun EqualizerSection(
    viewModel: SettingsViewModel = koinViewModel(),
    embeddedInSheet: Boolean = false,
) {
    val eqEnabled by viewModel.equalizerEnabled.collectAsStateWithLifecycle()
    val bands by viewModel.equalizerBands.collectAsStateWithLifecycle()
    val preamp by viewModel.equalizerPreamp.collectAsStateWithLifecycle()
    val enhancements by viewModel.equalizerEnhancements.collectAsStateWithLifecycle()
    val savedProfiles by viewModel.equalizerSavedProfiles.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.equalizerActiveProfileId.collectAsStateWithLifecycle()
    val autoEqProfile by viewModel.equalizerAutoEqProfile.collectAsStateWithLifecycle()
    val autoEqLayer by viewModel.equalizerAutoEqLayer.collectAsStateWithLifecycle()
    val audioOutputs by viewModel.audioOutputs.collectAsStateWithLifecycle()

    val curveColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    LaunchedEffect(Unit) { viewModel.getEqualizer() }
    DisposableEffect(Unit) {
        onDispose { viewModel.clearEqualizerPreview() }
    }

    var draftEnh by remember { mutableStateOf<EqualizerEnhancements?>(null) }
    LaunchedEffect(enhancements) { draftEnh = null }
    val shownEnh = draftEnh ?: enhancements

    var draftManualPreamp by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(preamp) { draftManualPreamp = null }
    val shownManualPreamp = draftManualPreamp ?: preamp

    var presetMenuOpen by remember { mutableStateOf(false) }
    var useSlidersMode by rememberSaveable { mutableStateOf(false) }
    var protectionExpanded by rememberSaveable { mutableStateOf(false) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var renamingProfile by remember { mutableStateOf<SavedEqualizerProfile?>(null) }
    var deletingProfile by remember { mutableStateOf<SavedEqualizerProfile?>(null) }

    val activePreset = remember(bands) { equalizerPresetFor(bands) }
    val autoEqLabel =
        remember(bands, autoEqProfile, autoEqLayer) {
            autoEqLayer?.label ?: autoEqLabelFor(autoEqProfile, bands)
        }
    val activeSavedProfile =
        remember(savedProfiles, activeProfileId) {
            savedProfiles.firstOrNull { it.id == activeProfileId }
        }
    val activeProfileModified =
        remember(activeSavedProfile, bands, preamp, enhancements, autoEqLayer) {
            activeSavedProfile != null &&
                !activeSavedProfile.matches(bands, preamp, enhancements, autoEqLayer)
        }

    val effectiveHeadroom =
        remember(eqEnabled, bands, shownManualPreamp, shownEnh, autoEqLayer) {
            EqualizerConfig(
                enabled = eqEnabled,
                bandsDb = bands,
                manualPreampDb = shownManualPreamp,
                enhancements = shownEnh,
                autoEqLayer = autoEqLayer,
            ).effectivePreampDb(48_000.0)
        }

    val containerModifier =
        if (embeddedInSheet) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        }

    Surface(
        modifier = containerModifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // 1. Top A/B Bypass + Quick Save + Reset bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Chip(
                    isSelected = eqEnabled,
                    text =
                        stringResource(
                            if (eqEnabled) Res.string.equalizer_bypass_active else Res.string.equalizer_bypass_off,
                        ),
                    onClick = { viewModel.setEqualizerEnabled(!eqEnabled) },
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { showSaveDialog = true }) {
                        Icon(
                            imageVector = SimpIcons.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(Res.string.equalizer_save_profile), style = typo().labelMedium)
                    }
                    TextButton(onClick = { viewModel.resetEqualizer() }) {
                        Text(text = stringResource(Res.string.equalizer_reset), style = typo().labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. My Saved Profiles (1-tap switch + update/rename/duplicate/delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(Res.string.equalizer_my_profiles),
                    style = typo().labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (activeProfileModified && activeSavedProfile != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.equalizer_profile_modified),
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.saveEqualizerProfile(
                                            name = activeSavedProfile.name,
                                            includeAutoEq = activeSavedProfile.autoEqLayer != null,
                                            overwriteId = activeSavedProfile.id,
                                        )
                                    },
                        ) {
                            Text(
                                text = stringResource(Res.string.equalizer_update_profile),
                                style = typo().labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 6.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                savedProfiles.forEach { profile ->
                    val isSelected = profile.id == activeProfileId
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.applySavedProfile(profile) },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                            ) {
                                Text(
                                    text = profile.name,
                                    style = typo().labelMedium,
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                IconButton(
                                    onClick = { menuOpen = true },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = SimpIcons.MoreVert,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.equalizer_update_profile)) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.saveEqualizerProfile(
                                        name = profile.name,
                                        includeAutoEq = profile.autoEqLayer != null || autoEqLayer?.enabled == true,
                                        overwriteId = profile.id,
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.equalizer_rename)) },
                                onClick = {
                                    menuOpen = false
                                    renamingProfile = profile
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.equalizer_duplicate)) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.duplicateSavedProfile(profile)
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(Res.string.equalizer_delete),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    deletingProfile = profile
                                },
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showSaveDialog = true },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = SimpIcons.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(Res.string.equalizer_save_as_new),
                            style = typo().labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            HorizontalDivider(color = gridColor.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // 3. Factory Presets & AutoEq row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.equalizer_presets),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(LABEL_COLUMN_WIDTH),
                )
                Box {
                    Row(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { presetMenuOpen = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text =
                                activePreset?.let { stringResource(it.labelRes) }
                                    ?: stringResource(Res.string.equalizer_preset_custom),
                            style = typo().bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(
                            imageVector = SimpIcons.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(expanded = presetMenuOpen, onDismissRequest = { presetMenuOpen = false }) {
                        EQUALIZER_PRESETS.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(text = stringResource(preset.labelRes), style = typo().bodyMedium) },
                                onClick = {
                                    viewModel.applyEqualizerPreset(preset)
                                    presetMenuOpen = false
                                },
                                leadingIcon = {
                                    if (preset.name == activePreset?.name) {
                                        Icon(
                                            imageVector = SimpIcons.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    } else {
                                        Box(modifier = Modifier.size(18.dp))
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.equalizer_autoeq),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(LABEL_COLUMN_WIDTH),
                )
                AutoEqPicker(label = autoEqLabel)
            }

            // 4. Musical Enhancement Sliders (live preview while dragging)
            Text(
                text = stringResource(Res.string.equalizer_tone_section),
                style = typo().labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            LiveToneSlider(
                label = stringResource(Res.string.equalizer_bass_boost),
                value = shownEnh.bassBoostDb,
                valueRange = 0f..EQUALIZER_BASS_MAX_DB,
                snap = ::snapHalfDb,
                readout = { "+${formatHalfDb(it)} dB" },
                onPreview = { v ->
                    val next = shownEnh.copy(bassBoostDb = v)
                    draftEnh = next
                    viewModel.previewEqualizerDraft(enhancements = next)
                },
                onCommit = { v -> viewModel.setEqualizerEnhancements(shownEnh.copy(bassBoostDb = v)) },
            )

            LiveToneSlider(
                label = stringResource(Res.string.equalizer_treble_boost),
                value = shownEnh.trebleBoostDb,
                valueRange = EQUALIZER_TREBLE_MIN_DB..EQUALIZER_TREBLE_MAX_DB,
                snap = ::snapHalfDb,
                readout = {
                    val s = formatHalfDb(it)
                    if (it > 0f) "+$s dB" else "$s dB"
                },
                onPreview = { v ->
                    val next = shownEnh.copy(trebleBoostDb = v)
                    draftEnh = next
                    viewModel.previewEqualizerDraft(enhancements = next)
                },
                onCommit = { v -> viewModel.setEqualizerEnhancements(shownEnh.copy(trebleBoostDb = v)) },
            )

            LiveToneSlider(
                label = stringResource(Res.string.equalizer_stereo_width),
                value = shownEnh.stereoWidth,
                valueRange = EQUALIZER_STEREO_WIDTH_MIN..EQUALIZER_STEREO_WIDTH_MAX,
                snap = { ((it * 20f).roundToInt() / 20f).coerceIn(EQUALIZER_STEREO_WIDTH_MIN, EQUALIZER_STEREO_WIDTH_MAX) },
                readout = { "${(it * 100f).roundToInt()}%" },
                onPreview = { v ->
                    val next = shownEnh.copy(stereoWidth = v)
                    draftEnh = next
                    viewModel.previewEqualizerDraft(enhancements = next)
                },
                onCommit = { v -> viewModel.setEqualizerEnhancements(shownEnh.copy(stereoWidth = v)) },
            )

            AnimatedVisibility(visible = abs(shownEnh.stereoWidth - 1f) > 0.01f) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(Res.string.equalizer_bass_mono),
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = shownEnh.bassMonoEnabled,
                        onCheckedChange = { checked ->
                            viewModel.setEqualizerEnhancements(shownEnh.copy(bassMonoEnabled = checked))
                        },
                    )
                }
            }

            LiveToneSlider(
                label = stringResource(Res.string.equalizer_loudness),
                value = shownEnh.loudnessAmount,
                valueRange = 0f..1f,
                snap = { ((it * 20f).roundToInt() / 20f).coerceIn(0f, 1f) },
                readout = { "${(it * 100f).roundToInt()}%" },
                onPreview = { v ->
                    val next = shownEnh.copy(loudnessAmount = v)
                    draftEnh = next
                    viewModel.previewEqualizerDraft(enhancements = next)
                },
                onCommit = { v -> viewModel.setEqualizerEnhancements(shownEnh.copy(loudnessAmount = v)) },
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5. 10-Band EQ View Mode Toggle (Curve <-> Sliders)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "10 Bandas (0.5 dB)",
                    style = typo().labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(
                        isSelected = !useSlidersMode,
                        text = stringResource(Res.string.equalizer_view_curve),
                        onClick = { useSlidersMode = false },
                    )
                    Chip(
                        isSelected = useSlidersMode,
                        text = stringResource(Res.string.equalizer_view_sliders),
                        onClick = { useSlidersMode = true },
                    )
                }
            }

            if (!useSlidersMode) {
                LeftToRight {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.width(44.dp).height(CURVE_HEIGHT).padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                text = "+${BAND_RANGE_DB.roundToInt()}dB",
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "-${BAND_RANGE_DB.roundToInt()}dB",
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        EqualizerCurve(
                            bands = bands,
                            curveColor = curveColor,
                            gridColor = gridColor,
                            onBandsPreview = { working ->
                                viewModel.previewEqualizerDraft(bandsDb = working)
                            },
                            onBandsChange = { working ->
                                viewModel.setEqualizerBands(working)
                            },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(CURVE_HEIGHT)
                                    .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                        )
                    }
                }
                LeftToRight {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 44.dp + 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        EQUALIZER_BAND_LABELS.forEach { label ->
                            Text(
                                text = label,
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                LeftToRight {
                    VerticalBandSlidersRow(
                        bands = bands,
                        accentColor = curveColor,
                        gridColor = gridColor,
                        onBandsPreview = { working -> viewModel.previewEqualizerDraft(bandsDb = working) },
                        onBandsCommit = { working -> viewModel.setEqualizerBands(working) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = gridColor.copy(alpha = 0.4f))

            // 6. Headroom & Protection (Collapsible)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { protectionExpanded = !protectionExpanded }
                        .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.equalizer_protection_section),
                        style = typo().labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.equalizer_effective_preamp, formatHalfDb(effectiveHeadroom)),
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = SimpIcons.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            AnimatedVisibility(visible = protectionExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.equalizer_preamp_mode),
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Chip(
                            isSelected = shownEnh.preampMode == PreampMode.AUTO_BALANCED,
                            text = stringResource(Res.string.equalizer_preamp_auto_balanced),
                            onClick = { viewModel.setEqualizerEnhancements(shownEnh.copy(preampMode = PreampMode.AUTO_BALANCED)) },
                        )
                        Chip(
                            isSelected = shownEnh.preampMode == PreampMode.AUTO_STRICT,
                            text = stringResource(Res.string.equalizer_preamp_auto_strict),
                            onClick = { viewModel.setEqualizerEnhancements(shownEnh.copy(preampMode = PreampMode.AUTO_STRICT)) },
                        )
                        Chip(
                            isSelected = shownEnh.preampMode == PreampMode.MANUAL,
                            text = stringResource(Res.string.equalizer_preamp_manual),
                            onClick = { viewModel.setEqualizerEnhancements(shownEnh.copy(preampMode = PreampMode.MANUAL)) },
                        )
                    }

                    AnimatedVisibility(visible = shownEnh.preampMode == PreampMode.MANUAL) {
                        LiveToneSlider(
                            label = stringResource(Res.string.equalizer_preamp),
                            value = shownManualPreamp,
                            valueRange = EQUALIZER_PREAMP_MIN_DB..EQUALIZER_PREAMP_MAX_DB,
                            snap = ::snapHalfDb,
                            readout = {
                                val s = formatHalfDb(it)
                                if (it > 0f) "+$s dB" else "$s dB"
                            },
                            onPreview = { v ->
                                draftManualPreamp = v
                                viewModel.previewEqualizerDraft(manualPreampDb = v)
                            },
                            onCommit = { v -> viewModel.setEqualizerPreamp(v) },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(stringResource(Res.string.equalizer_limiter), style = typo().bodySmall)
                            Text(
                                stringResource(Res.string.equalizer_limiter_desc),
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = shownEnh.limiterEnabled,
                            onCheckedChange = { checked ->
                                viewModel.setEqualizerEnhancements(shownEnh.copy(limiterEnabled = checked))
                            },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(stringResource(Res.string.equalizer_subsonic), style = typo().bodySmall)
                            Text(
                                stringResource(Res.string.equalizer_subsonic_desc),
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = shownEnh.subsonicFilterEnabled,
                            onCheckedChange = { checked ->
                                viewModel.setEqualizerEnhancements(shownEnh.copy(subsonicFilterEnabled = checked))
                            },
                        )
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        val activeOutput = audioOutputs.firstOrNull { it.isActive }
        val fallbackName = stringResource(Res.string.equalizer_profile_default_name) + " ${savedProfiles.size + 1}"
        val suggestedName =
            remember(activeOutput, savedProfiles.size, fallbackName) {
                when (activeOutput?.kind) {
                    AudioOutputKind.BLUETOOTH, AudioOutputKind.USB ->
                        activeOutput.name.trim().ifEmpty { fallbackName }
                    else -> fallbackName
                }
            }
        SaveEqualizerProfileDialog(
            title = stringResource(Res.string.equalizer_save_profile_title),
            initialName = suggestedName,
            hasAutoEqLayer = autoEqLayer?.enabled == true,
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, includeAuto ->
                viewModel.saveEqualizerProfile(name = name, includeAutoEq = includeAuto)
                showSaveDialog = false
            },
        )
    }

    renamingProfile?.let { profile ->
        SaveEqualizerProfileDialog(
            title = stringResource(Res.string.equalizer_rename),
            initialName = profile.name,
            hasAutoEqLayer = false,
            onDismiss = { renamingProfile = null },
            onConfirm = { newName, _ ->
                viewModel.renameSavedProfile(profile.id, newName)
                renamingProfile = null
            },
        )
    }

    deletingProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { deletingProfile = null },
            title = { Text(stringResource(Res.string.equalizer_delete), style = typo().titleSmall) },
            text = { Text(stringResource(Res.string.equalizer_delete_confirm, profile.name), style = typo().bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSavedProfile(profile.id)
                        deletingProfile = null
                    },
                ) {
                    Text(stringResource(Res.string.equalizer_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProfile = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun LiveToneSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    snap: (Float) -> Float,
    readout: (Float) -> String,
    onPreview: (Float) -> Unit,
    onCommit: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = readout(value),
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = { onPreview(snap(it)) },
            onValueChangeFinished = { onCommit(value) },
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun VerticalBandSlidersRow(
    bands: List<Float>,
    accentColor: Color,
    gridColor: Color,
    onBandsPreview: (List<Float>) -> Unit,
    onBandsCommit: (List<Float>) -> Unit,
) {
    val count = EQUALIZER_BAND_LABELS.size
    var draft by remember { mutableStateOf<List<Float>?>(null) }
    val shown = draft ?: bands
    val currentBands by rememberUpdatedState(bands)
    LaunchedEffect(bands) { draft = null }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until count) {
            val gain = shown.getOrElse(i) { 0f }
            var trackHeightPx by remember { mutableStateOf(1) }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (gain > 0f) "+${formatCompactDb(gain)}" else formatCompactDb(gain),
                    style = typo().bodySmall.copy(fontSize = 10.sp),
                    color = if (abs(gain) > 0.1f) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Box(
                    modifier =
                        Modifier
                            .height(176.dp)
                            .fillMaxWidth()
                            .onSizeChanged { trackHeightPx = maxOf(1, it.height) }
                            .pointerInput(i) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    var g = yToHalfDbGain(down.position.y, h)
                                    var working = List(count) { idx -> if (idx == i) g else currentBands.getOrElse(idx) { 0f } }
                                    draft = working
                                    onBandsPreview(working)
                                    down.consume()
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        val ch = ev.changes.firstOrNull() ?: break
                                        if (!ch.pressed) break
                                        g = yToHalfDbGain(ch.position.y, h)
                                        working = working.toMutableList().also { it[i] = g }
                                        draft = working
                                        onBandsPreview(working)
                                        ch.consume()
                                    }
                                    onBandsCommit(working)
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    // Track rail + center 0 dB tick + active fill
                    Box(
                        modifier =
                            Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(gridColor.copy(alpha = 0.4f))
                                .drawBehind {
                                    val midY = size.height / 2f
                                    val targetY = midY - (gain / BAND_RANGE_DB) * midY
                                    drawLine(
                                        color = accentColor,
                                        start = Offset(size.width / 2f, midY),
                                        end = Offset(size.width / 2f, targetY),
                                        strokeWidth = size.width,
                                    )
                                },
                    )
                    // Thumb dot positioned dynamically at the band's current dB height
                    val usableH = (trackHeightPx - 24).coerceAtLeast(1)
                    val offsetY = (-(gain / (BAND_RANGE_DB * 2f)) * usableH).roundToInt()
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .offset { IntOffset(0, offsetY) }
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                    )
                }
                Text(
                    text = EQUALIZER_BAND_LABELS[i],
                    style = typo().bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EqualizerCurve(
    bands: List<Float>,
    curveColor: Color,
    gridColor: Color,
    onBandsPreview: (List<Float>) -> Unit,
    onBandsChange: (List<Float>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = EQUALIZER_BAND_LABELS.size

    var draft by remember { mutableStateOf<List<Float>?>(null) }
    var activeDragBand by remember { mutableStateOf<Int?>(null) }
    val shown = draft ?: bands

    val currentBands by rememberUpdatedState(bands)
    val preview by rememberUpdatedState(onBandsPreview)
    val commit by rememberUpdatedState(onBandsChange)

    LaunchedEffect(bands) {
        draft = null
        activeDragBand = null
    }

    Box(
        modifier =
            modifier.pointerInput(count) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val (idx0, working0) = currentBands.withBandAtPair(down.position, size.width, size.height, count)
                    var working = working0
                    draft = working
                    activeDragBand = idx0
                    preview(working)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        val (idx, next) = working.withBandAtPair(change.position, size.width, size.height, count)
                        working = next
                        draft = working
                        activeDragBand = idx
                        preview(working)
                        change.consume()
                    }
                    activeDragBand = null
                    commit(working)
                }
            }.drawBehind {
                val step = size.width / (count - 1)
                val midY = size.height / 2f

                drawLine(gridColor, Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1f)
                repeat(count) { i ->
                    val x = step * i
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                }

                val points = List(count) { i -> Offset(step * i, gainToY(shown.getOrElse(i) { 0f }, size.height)) }
                val line = smoothPath(points)

                val area =
                    Path().apply {
                        addPath(line)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                drawPath(
                    area,
                    Brush.verticalGradient(
                        listOf(curveColor.copy(alpha = 0.35f), curveColor.copy(alpha = 0.02f)),
                    ),
                )
                drawPath(line, curveColor, style = Stroke(width = 3f))
                points.forEachIndexed { index, pt ->
                    val r = if (index == activeDragBand) 10f else 7f
                    drawCircle(curveColor, radius = r, center = pt)
                }
            },
    ) {
        activeDragBand?.let { idx ->
            val gain = shown.getOrElse(idx) { 0f }
            val label = EQUALIZER_BAND_LABELS.getOrElse(idx) { "" }
            val sign = if (gain > 0f) "+" else ""
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            ) {
                Text(
                    text = "$label · $sign${formatHalfDb(gain)} dB",
                    style = typo().labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SaveEqualizerProfileDialog(
    title: String,
    initialName: String,
    hasAutoEqLayer: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, includeAutoEq: Boolean) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var includeAutoEq by remember { mutableStateOf(hasAutoEqLayer) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = typo().titleSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(Res.string.equalizer_profile_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (hasAutoEqLayer) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { includeAutoEq = !includeAutoEq },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = includeAutoEq, onCheckedChange = { includeAutoEq = it })
                        Text(
                            text = stringResource(Res.string.equalizer_include_autoeq),
                            style = typo().bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name, includeAutoEq) },
            ) {
                Text(stringResource(Res.string.equalizer_save_profile))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

private fun gainToY(
    gainDb: Float,
    height: Float,
): Float = height / 2f - (gainDb / BAND_RANGE_DB) * (height / 2f)

private fun yToHalfDbGain(
    y: Float,
    height: Float,
): Float = snapHalfDb(((height / 2f - y) / (height / 2f)) * BAND_RANGE_DB).coerceIn(-BAND_RANGE_DB, BAND_RANGE_DB)

private fun List<Float>.withBandAtPair(
    position: Offset,
    width: Int,
    height: Int,
    count: Int,
): Pair<Int, List<Float>> {
    if (width <= 0 || height <= 0) return 0 to this
    val step = width.toFloat() / (count - 1)
    val index = (position.x / step).roundToInt().coerceIn(0, count - 1)
    val gain = yToHalfDbGain(position.y, height.toFloat())
    return index to List(count) { i -> if (i == index) gain else getOrElse(i) { 0f } }
}

private fun snapHalfDb(value: Float): Float = ((value * 2f).roundToInt() / 2f)

private fun formatHalfDb(value: Float): String {
    val snapped = snapHalfDb(value)
    val whole = snapped.roundToInt()
    return if (abs(snapped - whole.toFloat()) < 0.05f) whole.toString() else "$snapped"
}

private fun formatCompactDb(value: Float): String {
    val snapped = snapHalfDb(value)
    val whole = snapped.roundToInt()
    return if (abs(snapped - whole.toFloat()) < 0.05f) whole.toString() else "$snapped"
}

private fun smoothPath(points: List<Offset>): Path =
    Path().apply {
        if (points.isEmpty()) return@apply
        moveTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            val p0 = points[(i - 1).coerceAtLeast(0)]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[(i + 2).coerceAtMost(points.size - 1)]
            val lo = minOf(p1.y, p2.y)
            val hi = maxOf(p1.y, p2.y)
            cubicTo(
                p1.x + (p2.x - p0.x) / 6f,
                (p1.y + (p2.y - p0.y) / 6f).coerceIn(lo, hi),
                p2.x - (p3.x - p1.x) / 6f,
                (p2.y - (p3.y - p1.y) / 6f).coerceIn(lo, hi),
                p2.x,
                p2.y,
            )
        }
    }
