package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.maxrave.simpmusic.ui.icon.PushPin
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.ArtworkShape
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.IMAGE_CROSSFADE_FAST_MS
import com.maxrave.simpmusic.utils.THUMB_CARD_PX
import com.maxrave.simpmusic.utils.findExistingArtworkBitmap
import com.maxrave.simpmusic.utils.rememberOptimizedImageRequest
import com.maxrave.simpmusic.utils.rememberRamFirstTransform

/**
 * Two [LibraryPlaylistGridItem]s side by side — one line of the grid layout. A lone last playlist
 * leaves its neighbour empty so it keeps the width of the others.
 */
@Composable
fun LibraryPlaylistGridRow(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.weight(1f)) { left() }
        Box(Modifier.weight(1f)) { right() }
    }
}

/**
 * One playlist as a full-width square cover with its title and subtitle underneath, as in the grid
 * layout of YouTube Music. Covers reuse any version already in RAM (`rememberRamFirstTransform`), so
 * the grid opens with its covers drawn like the row layout does.
 */
@Composable
fun LibraryPlaylistGridItem(
    title: String,
    subtitle: String,
    thumbnailUrl: String,
    pinned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val platformContext = LocalPlatformContext.current
    val hasRamBitmap =
        remember(thumbnailUrl) {
            thumbnailUrl.isNotBlank() && findExistingArtworkBitmap(platformContext, thumbnailUrl) != null
        }
    val imageRequest =
        rememberOptimizedImageRequest(
            rawUrl = thumbnailUrl,
            targetPx = THUMB_CARD_PX,
            crossfadeMs = if (hasRamBitmap) 0 else IMAGE_CROSSFADE_FAST_MS,
        )
    val holderPainter = rememberHolderPainter()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(ArtworkShape)
                .clickable(onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
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
                        .fillMaxSize()
                        .clip(ArtworkShape),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = typo().titleSmall,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2000),
        )
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (pinned) {
                Image(
                    imageVector = SimpIcons.PushPin,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier =
                        Modifier
                            .padding(top = 2.dp)
                            .rotate(30f)
                            .size(16.dp),
                )
            }
            Text(
                text = subtitle,
                style = typo().bodySmall,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
