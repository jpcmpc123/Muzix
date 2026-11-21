package com.maxrave.simpmusic.expect.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.extension.findActivity
import com.maxrave.simpmusic.recognition.Recognition
import com.maxrave.simpmusic.ui.component.SurfaceDarkColors
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.CloudOff
import com.maxrave.simpmusic.ui.icon.GraphicEq
import com.maxrave.simpmusic.ui.icon.MicOff
import com.maxrave.simpmusic.ui.icon.MusicNote
import com.maxrave.simpmusic.ui.icon.MusicOff
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.Refresh
import com.maxrave.simpmusic.ui.icon.Replay
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.ArtworkShape
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SongRecognitionViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.login_sync_open_settings
import simpmusic.composeapp.generated.resources.recognize_allow_mic
import simpmusic.composeapp.generated.resources.recognize_failed
import simpmusic.composeapp.generated.resources.recognize_failed_hint
import simpmusic.composeapp.generated.resources.recognize_find
import simpmusic.composeapp.generated.resources.recognize_identifying
import simpmusic.composeapp.generated.resources.recognize_identifying_more
import simpmusic.composeapp.generated.resources.recognize_listen_again
import simpmusic.composeapp.generated.resources.recognize_listening
import simpmusic.composeapp.generated.resources.recognize_listening_hint
import simpmusic.composeapp.generated.resources.recognize_low_audio_hint
import simpmusic.composeapp.generated.resources.recognize_mic_hint
import simpmusic.composeapp.generated.resources.recognize_mic_needed
import simpmusic.composeapp.generated.resources.recognize_no_match
import simpmusic.composeapp.generated.resources.recognize_no_match_hint
import simpmusic.composeapp.generated.resources.recognize_play_now
import simpmusic.composeapp.generated.resources.recognize_song
import simpmusic.composeapp.generated.resources.recognize_try_again

private val OrbSize = 236.dp
private val CoreSize = 118.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun SongRecognitionSheet(
    onFind: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val localDensity = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val colors = rememberSurfaceDarkColors()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val viewModel: SongRecognitionViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    fun hasMic() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    var micGranted by remember { mutableStateOf(hasMic()) }
    var micBlocked by remember { mutableStateOf(false) }

    val micLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            micGranted = granted
            // Rationale is false after a denial only when the system will not ask again.
            val showRationale =
                runCatching {
                    context.findActivity().shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
                }.getOrDefault(false)
            micBlocked = !granted && !showRationale
        }

    // Back from the system settings page with the microphone now allowed.
    LifecycleResumeEffect(micGranted) {
        if (!micGranted && hasMic()) micGranted = true
        onPauseOrDispose { }
    }

    LaunchedEffect(micGranted) {
        if (micGranted) {
            viewModel.startIfNeeded()
        }
    }

    LaunchedEffect(uiState.recognition) {
        if (uiState.recognition is Recognition.Found) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun close(then: () -> Unit = {}) {
        viewModel.cancelListening()
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            viewModel.resetSession()
            then()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetSession()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        // Transparent above the card, so the sheet stops below the status bar.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() }),
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = colors.container),
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(4.dp),
                    colors = CardDefaults.cardColors().copy(containerColor = colors.handle),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.recognize_song),
                        style = typo().titleMedium,
                        color = colors.content,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { close() }) {
                        Icon(imageVector = SimpIcons.Close, contentDescription = null, tint = colors.content)
                    }
                }
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 28.dp, end = 28.dp, bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                ) {
                    val state = uiState.recognition
                    when {
                        !micGranted -> {
                            RecognitionOrb(SimpIcons.MicOff, muted = true)
                            Message(stringResource(Res.string.recognize_mic_needed), stringResource(Res.string.recognize_mic_hint), colors)
                            if (micBlocked) {
                                SheetButton(stringResource(Res.string.login_sync_open_settings)) { context.openAppSettings() }
                            } else {
                                SheetButton(stringResource(Res.string.recognize_allow_mic)) {
                                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }

                        state is Recognition.Found -> {
                            AsyncImage(
                                model = state.coverArt,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .size(OrbSize)
                                        .clip(ArtworkShape)
                                        .background(colors.handle),
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = state.title,
                                    style = typo().titleMedium,
                                    color = colors.content,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = state.artist,
                                    style = typo().bodyMedium,
                                    color = colors.subtitle,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val metaLine = listOfNotNull(state.album, state.year).joinToString(" · ")
                                if (metaLine.isNotEmpty()) {
                                    Text(
                                        text = metaLine,
                                        style = typo().bodySmall,
                                        color = colors.subtitle.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val hasPlayableTrack = uiState.matchedTrack != null
                                SheetButton(
                                    text = stringResource(if (hasPlayableTrack) Res.string.recognize_play_now else Res.string.recognize_find),
                                    icon = if (hasPlayableTrack) SimpIcons.PlayArrow else SimpIcons.Search,
                                ) {
                                    if (hasPlayableTrack) {
                                        close { viewModel.playMatchedTrack() }
                                    } else {
                                        val query = "${state.title} ${state.artist}".trim()
                                        close { onFind(query) }
                                    }
                                }
                                if (hasPlayableTrack) {
                                    OutlinedButton(
                                        onClick = {
                                            val query = "${state.title} ${state.artist}".trim()
                                            close { onFind(query) }
                                        },
                                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                    ) {
                                        Icon(imageVector = SimpIcons.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(text = stringResource(Res.string.recognize_find), color = colors.content)
                                    }
                                }
                                TextButton(
                                    onClick = { viewModel.retry() },
                                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                                ) {
                                    Icon(imageVector = SimpIcons.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = stringResource(Res.string.recognize_listen_again), color = LocalContentColor.current)
                                }
                            }
                        }

                        state is Recognition.NoMatch || state is Recognition.Failed -> {
                            val noMatch = state is Recognition.NoMatch
                            RecognitionOrb(if (noMatch) SimpIcons.MusicOff else SimpIcons.CloudOff, muted = true)
                            Message(
                                title = stringResource(if (noMatch) Res.string.recognize_no_match else Res.string.recognize_failed),
                                hint = stringResource(if (noMatch) Res.string.recognize_no_match_hint else Res.string.recognize_failed_hint),
                                colors = colors,
                            )
                            SheetButton(stringResource(Res.string.recognize_try_again), SimpIcons.Refresh) { viewModel.retry() }
                        }

                        else -> {
                            val listening = state is Recognition.Listening
                            val identifyingMore = state is Recognition.Identifying && state.attempt > 1
                            RecognitionOrb(
                                icon = if (listening || identifyingMore) SimpIcons.GraphicEq else SimpIcons.MusicNote,
                                listening = listening || state is Recognition.Identifying,
                                amplitude = uiState.amplitude,
                            )
                            val title =
                                when {
                                    listening -> stringResource(Res.string.recognize_listening)
                                    identifyingMore -> stringResource(Res.string.recognize_identifying_more)
                                    else -> stringResource(Res.string.recognize_identifying)
                                }
                            val hint =
                                when {
                                    uiState.lowAudio -> stringResource(Res.string.recognize_low_audio_hint)
                                    listening || identifyingMore -> stringResource(Res.string.recognize_listening_hint)
                                    else -> ""
                                }
                            Message(title = title, hint = hint, colors = colors)
                        }
                    }
                }
            }
        }
    }
}

/** The disc in the middle of the sheet; while listening, rings spread out and pulse with live audio amplitude. */
@Composable
private fun RecognitionOrb(
    icon: ImageVector,
    muted: Boolean = false,
    listening: Boolean = false,
    amplitude: Float = 0f,
) {
    val cs = MaterialTheme.colorScheme
    val smoothedAmplitude by animateFloatAsState(
        targetValue = if (listening) amplitude.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 110),
        label = "orbAmplitude",
    )
    Box(Modifier.size(if (muted) 140.dp else OrbSize), contentAlignment = Alignment.Center) {
        if (listening) {
            val ring = cs.primary
            val progress by rememberInfiniteTransition(label = "rings").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
                label = "ring",
            )
            Canvas(Modifier.size(OrbSize)) {
                val stroke = (2.dp + (smoothedAmplitude * 2.5f).dp).toPx()
                val from = (CoreSize.toPx() / 2) * (1f + smoothedAmplitude * 0.14f)
                val to = size.minDimension / 2 - stroke / 2
                // Two rings half a cycle apart, fading as they grow.
                for (k in 0..1) {
                    val p = (progress + k / 2f) % 1f
                    drawCircle(
                        color = ring,
                        radius = from + (to - from) * p,
                        alpha = (0.35f + smoothedAmplitude * 0.45f) * (1f - p),
                        style = Stroke(stroke),
                    )
                }
            }
        }
        val coreScale = if (listening) 1f + smoothedAmplitude * 0.14f else 1f
        Box(
            modifier =
                Modifier
                    .size(CoreSize)
                    .graphicsLayer {
                        scaleX = coreScale
                        scaleY = coreScale
                    }.clip(CircleShape)
                    .background(if (muted) cs.surfaceContainerHighest else cs.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (muted) cs.onSurfaceVariant else cs.onPrimary,
                modifier = Modifier.size(52.dp),
            )
        }
    }
}

@Composable
private fun Message(
    title: String,
    hint: String,
    colors: SurfaceDarkColors,
) {
    Text(text = title, style = typo().titleMedium, color = colors.content, textAlign = TextAlign.Center)
    Text(text = hint, style = typo().bodyMedium, color = colors.subtitle, textAlign = TextAlign.Center)
}

@Composable
private fun SheetButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        contentPadding = if (icon != null) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, color = LocalContentColor.current)
    }
}
