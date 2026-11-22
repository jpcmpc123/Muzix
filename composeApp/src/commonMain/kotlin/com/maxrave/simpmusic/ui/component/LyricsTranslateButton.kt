package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Translate
import com.maxrave.simpmusic.viewModel.LyricsTranslationUiState
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.hide_translation
import simpmusic.composeapp.generated.resources.translate_lyrics

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
 * the surrounding fullscreen lyrics screen or blur backdrop.
 */
@Composable
fun FullscreenLyricsTranslateButton(
    translationStateFlow: StateFlow<LyricsTranslationUiState>,
    accentBackgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by translationStateFlow.collectAsStateWithLifecycle()
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
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
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
@Composable
fun CompactLyricsTranslateButton(
    translationStateFlow: StateFlow<LyricsTranslationUiState>,
    cardColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by translationStateFlow.collectAsStateWithLifecycle()
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
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
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
}
