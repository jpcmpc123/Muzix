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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.extension.findActivity
import com.maxrave.simpmusic.recognition.Recognition
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.SurfaceDarkColors
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.CloudOff
import com.maxrave.simpmusic.ui.icon.GraphicEq
import com.maxrave.simpmusic.ui.icon.MicOff
import com.maxrave.simpmusic.ui.icon.MusicNote
import com.maxrave.simpmusic.ui.icon.MusicOff
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.Refresh
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.ThumbUp
import com.maxrave.simpmusic.ui.icon.ThumbUpOutline
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
import simpmusic.composeapp.generated.resources.recognize_identifying
import simpmusic.composeapp.generated.resources.recognize_identifying_more
import simpmusic.composeapp.generated.resources.recognize_like
import simpmusic.composeapp.generated.resources.recognize_listening
import simpmusic.composeapp.generated.resources.recognize_listening_hint
import simpmusic.composeapp.generated.resources.recognize_low_audio_hint
import simpmusic.composeapp.generated.resources.recognize_mic_hint
import simpmusic.composeapp.generated.resources.recognize_mic_needed
import simpmusic.composeapp.generated.resources.recognize_no_match
import simpmusic.composeapp.generated.resources.recognize_no_match_hint
import simpmusic.composeapp.generated.resources.recognize_search_another
import simpmusic.composeapp.generated.resources.recognize_song
import simpmusic.composeapp.generated.resources.recognize_try_again
import simpmusic.composeapp.generated.resources.save

private val OrbSize = 236.dp
private val CoreSize = 118.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun SongRecognitionSheet(
    onFind: (String) -> Unit,
    onOpenPlayer: () -> Unit,
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
    var showSavePlaylistSheet by remember { mutableStateOf(false) }

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
            then()
            viewModel.resetSession()
            onDismiss()
        }
    }

    if (showSavePlaylistSheet) {
        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist = uiState.listLocalPlaylist,
            listYouTubePlaylist = uiState.listYouTubePlaylist,
            videoId = uiState.matchedTrack?.videoId,
            onClick = { playlist ->
                viewModel.addToLocalPlaylist(playlist.id)
            },
            onYTPlaylistClick = { ytPlaylist ->
                viewModel.addToYouTubePlaylist(ytPlaylist.browseId)
            },
            onDismiss = { showSavePlaylistSheet = false },
        )
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
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors().copy(containerColor = colors.container),
            ) {
                val state = uiState.recognition
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
                    if (state !is Recognition.Found) {
                        Text(
                            text = stringResource(Res.string.recognize_song),
                            style = typo().titleMedium,
                            color = colors.content,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    IconButton(onClick = { close() }) {
                        Icon(imageVector = SimpIcons.Close, contentDescription = null, tint = colors.content)
                    }
                }

                when {
                    !micGranted -> {
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
                    }

                    state is Recognition.Found -> {
                        val matched = uiState.matchedTrack
                        val displayCover =
                            matched?.thumbnails?.lastOrNull()?.url?.let { url ->
                                if (url.contains("w120") || url.contains("h120")) {
                                    url.replace(Regex("([wh])120"), "$1544")
                                } else {
                                    url
                                }
                            } ?: state.coverArt
                        val displayTitle = matched?.title?.takeIf { it.isNotBlank() } ?: state.title
                        val displayArtist =
                            matched?.artists
                                ?.mapNotNull { it.name.takeIf(String::isNotBlank) }
                                ?.joinToString(", ")
                                ?.takeIf { it.isNotBlank() }
                                ?: state.artist
                        val displayAlbum = matched?.album?.name?.takeIf { it.isNotBlank() } ?: state.album
                        val displayYear = (matched?.year as? String)?.takeIf { it.isNotBlank() } ?: state.year

                        val openMatchedSong: () -> Unit = {
                            if (matched != null) {
                                viewModel.playTrack(matched)
                                close { onOpenPlayer() }
                            } else {
                                val query = "$displayTitle $displayArtist".trim()
                                close { onFind(query) }
                            }
                        }
                        val hasPlayableTrack = matched != null

                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(modifier = Modifier.weight(0.55f))

                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 360.dp),
                                horizontalAlignment = Alignment.Start,
                            ) {
                                AsyncImage(
                                    model = displayCover,
                                    contentDescription = displayTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.handle)
                                            .clickable(onClick = openMatchedSong),
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable(onClick = openMatchedSong),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        text = displayTitle,
                                        style = typo().titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                                        color = colors.content,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = displayArtist,
                                        style = typo().bodyMedium.copy(fontSize = 15.sp),
                                        color = colors.subtitle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val metaLine = listOfNotNull(displayAlbum, displayYear).joinToString(" • ")
                                    if (metaLine.isNotEmpty()) {
                                        Text(
                                            text = metaLine,
                                            style = typo().bodySmall.copy(fontSize = 14.sp),
                                            color = colors.subtitle.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Primary white pill: Gostei / Like
                                Button(
                                    onClick = { viewModel.toggleLike() },
                                    enabled = hasPlayableTrack,
                                    shape = RoundedCornerShape(24.dp),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF0F0F0F),
                                            disabledContainerColor = Color.White.copy(alpha = 0.45f),
                                            disabledContentColor = Color(0xFF0F0F0F).copy(alpha = 0.6f),
                                        ),
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isLiked) SimpIcons.ThumbUp else SimpIcons.ThumbUpOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(Res.string.recognize_like),
                                        style = typo().labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Secondary outlined dark pill: Salvar / Save
                                OutlinedButton(
                                    onClick = {
                                        viewModel.loadPlaylistsForSave()
                                        showSavePlaylistSheet = true
                                    },
                                    enabled = hasPlayableTrack,
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                                    colors =
                                        ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = colors.content,
                                            disabledContentColor = colors.content.copy(alpha = 0.45f),
                                        ),
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                ) {
                                    Icon(
                                        imageVector = SimpIcons.PlaylistAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(Res.string.save),
                                        style = typo().labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Bottom circular wave button + "Pesquisar outra música"
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier =
                                    Modifier
                                        .padding(bottom = 28.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.retry() }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = SimpIcons.GraphicEq,
                                        contentDescription = stringResource(Res.string.recognize_search_another),
                                        tint = colors.content,
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                                Text(
                                    text = stringResource(Res.string.recognize_search_another),
                                    style = typo().bodyMedium,
                                    color = colors.subtitle,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    state is Recognition.NoMatch || state is Recognition.Failed -> {
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
                            val noMatch = state is Recognition.NoMatch
                            RecognitionOrb(if (noMatch) SimpIcons.MusicOff else SimpIcons.CloudOff, muted = true)
                            Message(
                                title = stringResource(if (noMatch) Res.string.recognize_no_match else Res.string.recognize_failed),
                                hint = stringResource(if (noMatch) Res.string.recognize_no_match_hint else Res.string.recognize_failed_hint),
                                colors = colors,
                            )
                            SheetButton(stringResource(Res.string.recognize_try_again), SimpIcons.Refresh) { viewModel.retry() }
                        }
                    }

                    else -> {
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
        Text(text)
    }
}
