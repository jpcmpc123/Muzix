package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toTrack
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toListName
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.ArtworkShape
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.IMAGE_CROSSFADE_FAST_MS
import com.maxrave.simpmusic.utils.THUMB_SMALL_PX
import com.maxrave.simpmusic.utils.rememberOptimizedImageRequest
import com.maxrave.simpmusic.utils.rememberRamFirstTransform
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.add_a_song
import simpmusic.composeapp.generated.resources.no_songs_found
import simpmusic.composeapp.generated.resources.search_songs_to_add

/**
 * Quick-search bottom sheet opened by "+ Add a song" inside editable playlists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongToPlaylistSheet(
    existingVideoIds: Set<String>,
    onAddTrack: suspend (Track) -> Boolean,
    onDismiss: () -> Unit,
    searchRepository: SearchRepository = koinInject(),
) {
    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SongsResult>>(emptyList()) }
    var addingVideoIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var addedInSession by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        val clean = query.trim()
        if (clean.length < 2) {
            isSearching = false
            results = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        isSearching = true
        searchRepository.getSearchDataSong(clean).collectLatest { res ->
            when (res) {
                is Resource.Success -> {
                    results = res.data ?: emptyList()
                    isSearching = false
                }
                is Resource.Error -> {
                    results = emptyList()
                    isSearching = false
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.ime)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 12.dp),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier =
                            Modifier
                                .padding(vertical = 8.dp)
                                .width(60.dp)
                                .height(4.dp),
                        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(50),
                    ) {}
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.add_a_song),
                        style = typo().titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    RippleIconButton(
                        imageVector = SimpIcons.Close,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(44.dp),
                        onClick = onDismiss,
                    )
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = SimpIcons.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    placeholder = {
                        Text(
                            text = stringResource(Res.string.search_songs_to_add),
                            style = typo().bodyMedium,
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                when {
                    isSearching -> {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CenterLoadingBox(Modifier.size(36.dp))
                        }
                    }

                    query.trim().length >= 2 && results.isEmpty() -> {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.no_songs_found),
                                style = typo().bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 380.dp),
                        ) {
                            itemsIndexed(
                                items = results,
                                key = { index, item -> "addsong_${item.videoId}_$index" },
                            ) { _, song ->
                                val alreadyIn =
                                    song.videoId in existingVideoIds || song.videoId in addedInSession
                                val isAdding = song.videoId in addingVideoIds
                                val holderPainter = rememberHolderPainter()
                                val request =
                                    rememberOptimizedImageRequest(
                                        rawUrl = song.thumbnails?.lastOrNull()?.url,
                                        targetPx = THUMB_SMALL_PX,
                                        crossfadeMs = IMAGE_CROSSFADE_FAST_MS,
                                    )
                                val triggerAdd = {
                                    if (!alreadyIn && !isAdding) {
                                        scope.launch {
                                            addingVideoIds = addingVideoIds + song.videoId
                                            try {
                                                if (onAddTrack(song.toTrack())) {
                                                    addedInSession = addedInSession + song.videoId
                                                }
                                            } finally {
                                                addingVideoIds = addingVideoIds - song.videoId
                                            }
                                        }
                                    }
                                }
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { triggerAdd() }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    AsyncImage(
                                        model = request,
                                        transform =
                                            rememberRamFirstTransform(
                                                fallbackPlaceholder = holderPainter,
                                                error = holderPainter,
                                            ),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier =
                                            Modifier
                                                .size(48.dp)
                                                .clip(ArtworkShape),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = song.title.orEmpty(),
                                            style = typo().titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = song.artists?.toListName()?.connectArtists().orEmpty(),
                                            style = typo().bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    when {
                                        isAdding ->
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        alreadyIn ->
                                            Icon(
                                                imageVector = SimpIcons.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        else ->
                                            RippleIconButton(
                                                imageVector = SimpIcons.Add,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(40.dp),
                                                onClick = { triggerAdd() },
                                            )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
