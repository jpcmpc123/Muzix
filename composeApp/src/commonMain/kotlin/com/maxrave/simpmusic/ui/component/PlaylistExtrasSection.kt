package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.isLive
import com.maxrave.domain.utils.toListName
import com.maxrave.simpmusic.extension.mouseDragScroll
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.ArtworkShape
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.IMAGE_CROSSFADE_FAST_MS
import com.maxrave.simpmusic.utils.THUMB_CARD_PX
import com.maxrave.simpmusic.utils.THUMB_SMALL_PX
import com.maxrave.simpmusic.utils.findExistingArtworkBitmap
import com.maxrave.simpmusic.utils.rememberOptimizedImageRequest
import com.maxrave.simpmusic.utils.rememberRamFirstTransform
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.add_a_song
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.refresh_suggestions
import simpmusic.composeapp.generated.resources.related_playlists
import simpmusic.composeapp.generated.resources.suggestions

/**
 * Full-width "+ Add a song" row shown in editable playlists, matching YouTube Music's layout
 * while adopting Muzix's translucent white tile over the dark blurred playlist backdrop.
 */
@Composable
fun AddSongToPlaylistRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SimpIcons.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(Res.string.add_a_song),
            style = typo().titleSmall,
            color = Color.White,
        )
    }
}

/**
 * Suggestions track list + centered pill "Refresh" button rendered at the bottom of a playlist.
 */
@Composable
fun PlaylistSuggestionsSection(
    suggestions: List<Track>,
    isLoading: Boolean,
    canReload: Boolean,
    addingVideoIds: Set<String>,
    playingVideoId: String?,
    isPlaybackActive: Boolean,
    onPlayTrack: (Track) -> Unit,
    onAddTrack: (Track) -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.suggestions),
            style = typo().headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        if (isLoading && suggestions.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                contentAlignment = Alignment.Center,
            ) {
                CenterLoadingBox(Modifier.size(32.dp))
            }
        } else {
            suggestions.forEach { track ->
                SuggestionTrackRow(
                    track = track,
                    isPlaying = playingVideoId == track.videoId,
                    isPlaybackActive = isPlaybackActive,
                    isAdding = track.videoId in addingVideoIds,
                    onClick = { onPlayTrack(track) },
                    onAddClick = { onAddTrack(track) },
                )
            }
            if (canReload) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    OutlinedButton(
                        onClick = onReload,
                        enabled = !isLoading,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = stringResource(Res.string.refresh_suggestions),
                            style = typo().bodyMedium,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionTrackRow(
    track: Track,
    isPlaying: Boolean,
    isPlaybackActive: Boolean,
    isAdding: Boolean,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
) {
    val platformContext = LocalPlatformContext.current
    val thumb = track.thumbnails?.lastOrNull()?.url.orEmpty()
    val hasRamBitmap =
        remember(thumb) {
            thumb.isNotBlank() && findExistingArtworkBitmap(platformContext, thumb) != null
        }
    val imageRequest =
        rememberOptimizedImageRequest(
            rawUrl = thumb,
            targetPx = THUMB_SMALL_PX,
            crossfadeMs = if (hasRamBitmap) 0 else IMAGE_CROSSFADE_FAST_MS,
        )
    val holderPainter = rememberHolderPainter()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            if (isPlaying) {
                AudioPlayingIndicator(
                    isAnimating = isPlaybackActive,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AsyncImage(
                    model = imageRequest,
                    transform =
                        rememberRamFirstTransform(
                            fallbackPlaceholder = holderPainter,
                            error = holderPainter,
                        ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(ArtworkShape),
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp)
                    .align(Alignment.CenterVertically),
        ) {
            Text(
                text = track.title,
                style = typo().titleSmall,
                color = Color.White,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2000),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.isLive) LiveBadge(modifier = Modifier.padding(end = 6.dp))
                Text(
                    text = track.artists?.toListName()?.connectArtists().orEmpty(),
                    style = typo().bodySmall,
                    color = Color(0xC4FFFFFF),
                    maxLines = 1,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2000),
                )
            }
        }
        if (isAdding) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            RippleIconButton(
                imageVector = SimpIcons.PlaylistAdd,
                tint = Color.White,
                modifier = Modifier.size(40.dp),
                onClick = onAddClick,
            )
        }
    }
}

/**
 * Horizontal carousel of "Related playlists" rendered at the bottom of a playlist screen.
 */
@Composable
fun RelatedPlaylistsSection(
    relatedPlaylists: List<PlaylistsResult>,
    onPlaylistClick: (PlaylistsResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (relatedPlaylists.isEmpty()) return
    val rowState = rememberLazyListState()
    val playlistLabel = stringResource(Res.string.playlist)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
    ) {
        Text(
            text = stringResource(Res.string.related_playlists),
            style = typo().headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .mouseDragScroll(rowState),
        ) {
            itemsIndexed(
                items = relatedPlaylists,
                key = { index, item -> "relpl_${item.browseId}_$index" },
            ) { _, playlist ->
                RelatedPlaylistCard(
                    playlist = playlist,
                    playlistLabel = playlistLabel,
                    onClick = { onPlaylistClick(playlist) },
                )
            }
        }
    }
}

@Composable
private fun RelatedPlaylistCard(
    playlist: PlaylistsResult,
    playlistLabel: String,
    onClick: () -> Unit,
) {
    val platformContext = LocalPlatformContext.current
    val thumb = playlist.thumbnails.lastOrNull()?.url.orEmpty()
    val hasRamBitmap =
        remember(thumb) {
            thumb.isNotBlank() && findExistingArtworkBitmap(platformContext, thumb) != null
        }
    val imageRequest =
        rememberOptimizedImageRequest(
            rawUrl = thumb,
            targetPx = THUMB_CARD_PX,
            crossfadeMs = if (hasRamBitmap) 0 else IMAGE_CROSSFADE_FAST_MS,
        )
    val holderPainter = rememberHolderPainter()
    val subtitle =
        remember(playlistLabel, playlist.author, playlist.itemCount) {
            listOf(playlistLabel, playlist.author, playlist.itemCount)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
        }
    Column(
        modifier =
            Modifier
                .width(156.dp)
                .clip(ArtworkShape)
                .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = imageRequest,
            transform =
                rememberRamFirstTransform(
                    fallbackPlaceholder = holderPainter,
                    error = holderPainter,
                ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(156.dp)
                    .clip(ArtworkShape),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = playlist.title,
            style = typo().titleSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = typo().bodySmall,
                color = Color(0xC4FFFFFF),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
