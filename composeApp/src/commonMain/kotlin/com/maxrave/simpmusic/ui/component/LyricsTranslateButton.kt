package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Translate
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LyricsTranslationUiState
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.hide_translation
import simpmusic.composeapp.generated.resources.translate_lyrics
import simpmusic.composeapp.generated.resources.translation_source_ai
import simpmusic.composeapp.generated.resources.translation_source_google
import simpmusic.composeapp.generated.resources.translation_source_letras
import simpmusic.composeapp.generated.resources.translation_source_microsoft
import simpmusic.composeapp.generated.resources.translation_source_simpmusic
import simpmusic.composeapp.generated.resources.translation_source_vagalume
import simpmusic.composeapp.generated.resources.translation_source_youtube

private val InactiveCircleColor = Color.White.copy(alpha = 0.16f)
private val LoadingCircleColor = Color.White.copy(alpha = 0.22f)
private val DarkFallbackGlyphColor = Color(0xFF1A1A1A)

/**
 * Fullscreen circular translate toggle button modeled after Spotify/YouTube Music:
 * - Inactive: translucent circle with white `文A` icon.
 * - Loading: translucent circle with a small white progress spinner.
 * - Active: solid white circle with dark/background-tinted `文A` icon and a top-right `×` badge.
 *
 * Collects [translationStateFlow] locally so toggling or animating the spinner never recomposes
 * the surrounding fullscreen lyrics screen or blur backdrop. Long-pressing forces retranslation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullscreenLyricsTranslateButton(
    translationStateFlow: StateFlow<LyricsTranslationUiState>,
    accentBackgroundColor: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by translationStateFlow.collectAsStateWithLifecycle()
    if (uiState.isSameLanguage) return
    val glyphColor =
        if (accentBackgroundColor.luminance() < 0.55f) {
            accentBackgroundColor.copy(alpha = 1f)
        } else {
            DarkFallbackGlyphColor
        }
    val description =
        if (uiState.isVisible) {
            stringResource(Res.string.hide_translation)
        } else {
            stringResource(Res.string.translate_lyrics)
        }

    Box(
        modifier =
            modifier
                .size(48.dp)
                .semantics { contentDescription = description }
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            uiState.isVisible -> Color.White
                            uiState.isLoading -> LoadingCircleColor
                            else -> InactiveCircleColor
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = SimpIcons.Translate,
                    contentDescription = null,
                    tint = if (uiState.isVisible) glyphColor else Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        if (uiState.isVisible && !uiState.isLoading) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-1).dp, y = 1.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(width = 2.dp, color = glyphColor, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = SimpIcons.Close,
                    contentDescription = null,
                    tint = glyphColor,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
    }
}

/**
 * Compact translate toggle button for the Classic player lyrics card header.
 * Collects [translationStateFlow] locally to avoid recomposing the player screen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactLyricsTranslateButton(
    translationStateFlow: StateFlow<LyricsTranslationUiState>,
    cardColor: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailingSpacing: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val uiState by translationStateFlow.collectAsStateWithLifecycle()
    if (uiState.isSameLanguage) return
    val glyphColor =
        if (cardColor.luminance() < 0.55f) {
            cardColor.copy(alpha = 1f)
        } else {
            DarkFallbackGlyphColor
        }
    val description =
        if (uiState.isVisible) {
            stringResource(Res.string.hide_translation)
        } else {
            stringResource(Res.string.translate_lyrics)
        }

    Box(
        modifier =
            modifier
                .size(28.dp)
                .semantics { contentDescription = description }
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color.White,
                    strokeWidth = 1.8.dp,
                )
            }

            uiState.isVisible -> {
                Box(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SimpIcons.Translate,
                        contentDescription = null,
                        tint = glyphColor,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(width = 1.5.dp, color = glyphColor, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SimpIcons.Close,
                        contentDescription = null,
                        tint = glyphColor,
                        modifier = Modifier.size(7.dp),
                    )
                }
            }

            else -> {
                Icon(
                    imageVector = SimpIcons.Translate,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
    if (trailingSpacing > 0.dp) {
        Spacer(modifier = Modifier.width(trailingSpacing))
    }
}

@Composable
fun LyricsTranslationSourceText(
    translationStateFlow: StateFlow<LyricsTranslationUiState>,
    modifier: Modifier = Modifier,
) {
    val uiState by translationStateFlow.collectAsStateWithLifecycle()
    if (!uiState.isVisible) return
    val label =
        when (uiState.sourceEngine) {
            "letras" -> stringResource(Res.string.translation_source_letras)
            "vagalume" -> stringResource(Res.string.translation_source_vagalume)
            "simpmusic" -> stringResource(Res.string.translation_source_simpmusic)
            "youtube" -> stringResource(Res.string.translation_source_youtube)
            "microsoft" -> stringResource(Res.string.translation_source_microsoft)
            "google" -> stringResource(Res.string.translation_source_google)
            "ai" -> stringResource(Res.string.translation_source_ai)
            else -> return
        }
    Text(
        text = label,
        style = typo().bodySmall,
        color = Color.White.copy(alpha = 0.65f),
        textAlign = TextAlign.End,
        modifier = modifier.fillMaxWidth(),
    )
}
