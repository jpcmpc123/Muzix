package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.PodcastsEntity
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.type.ChartItem
import com.maxrave.domain.data.type.MonthlyRecapItem
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.utils.LocalResource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.ui.DesktopMiniPlayerClearance
import com.maxrave.simpmusic.expect.ui.VerticalScrollBar
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.LocalPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.create_playlist_action
import kotlin.math.roundToInt

/**
 * Columns for every grid built on [GridLibraryPlaylist] — the Library tabs, Wrapped and Mix for you.
 *
 * The count is whichever brings the artwork closest to 150dp, so every cell on a page is the same
 * size and a wider window adds columns instead of stretching them. `Adaptive(minSize = 120.dp)`
 * treated 120 as a floor instead, which on a 1404dp window meant eleven columns of ~108dp artwork.
 *
 * Never fewer than three: the column count must follow the window, not the device's density, and a
 * 360–412dp phone would otherwise round down to two. Below 480dp this gives exactly the three columns
 * `Adaptive(120.dp)` did, so phones keep the grid they have.
 */
internal object LibraryGridCells : GridCells {
    // The artwork plus the 10dp start + 10dp end padding every item in these grids carries.
    private val TargetCellWidth = 150.dp + 20.dp

    override fun Density.calculateCrossAxisCellSizes(
        availableSize: Int,
        spacing: Int,
    ): List<Int> {
        val target = TargetCellWidth.roundToPx() + spacing
        val count = ((availableSize + spacing).toFloat() / target).roundToInt().coerceAtLeast(3)
        // The same split GridCells.Fixed makes: leftover pixels go one each to the first columns.
        val usable = availableSize - spacing * (count - 1)
        return List(count) { usable / count + if (it < usable % count) 1 else 0 }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal inline fun <reified T> GridLibraryPlaylist(
    navController: NavController,
    contentPadding: PaddingValues,
    data: LocalResource<List<T>>,
    emptyText: StringResource,
    // Hoisted so a caller can read the scroll position itself — Mix for you derives its
    // top-bar frost from `index == 0 && offset == 0`, which the coarse onScrolling below
    // cannot say.
    state: LazyGridState = rememberLazyGridState(),
    noinline onScrolling: (onTop: Boolean) -> Unit = { _ -> },
    // A full-width block above the tiles, as a real grid item spanning every column — the same
    // mechanism the create tile and the chart button below already use. A caller drawing it in a
    // Box over the grid instead has to reserve its height in contentPadding and translate it by
    // the scroll offset by hand, which is what left a screen-tall hole above the Wrapped tab.
    noinline header: (@Composable () -> Unit)? = null,
    noinline createNewPlaylist: (() -> Unit)? = null,
    noinline onReload: () -> Unit,
) {
    val isScrollingUpState = state.isScrollingUp()

    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex <= 1 || isScrollingUpState.value }
            .distinctUntilChanged()
            .collect { onScrolling.invoke(it) }
    }
    val pullToRefreshState = rememberPullToRefreshState()
    // Initial loading and a pull-to-refresh both arrive as LocalResource.Loading, so the type alone
    // cannot tell them apart. The top spinner belongs to the gesture only; the first load is drawn
    // by the centered indicator below. [userRefreshing] is raised by the gesture and dropped as soon
    // as data leaves Loading (Success AND Error), so the spinner can never stay stuck after a failure.
    var userRefreshing by remember { mutableStateOf(false) }
    // Last list that loaded successfully: kept on screen while a manual refresh runs (the ViewModel
    // replaces the data with Loading, which would otherwise blank the grid under the spinner).
    var lastList by remember { mutableStateOf<List<T>>(emptyList()) }
    LaunchedEffect(data) {
        when (data) {
            is LocalResource.Loading -> Unit
            is LocalResource.Success -> {
                userRefreshing = false
                lastList = data.data ?: emptyList()
            }
            is LocalResource.Error -> {
                userRefreshing = false
                lastList = emptyList()
            }
        }
    }
    val showTopIndicator = userRefreshing && data is LocalResource.Loading
    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        onRefresh = {
            userRefreshing = true
            onReload()
        },
        isRefreshing = showTopIndicator,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = showTopIndicator,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(
                            top = contentPadding.calculateTopPadding(),
                        ),
                containerColor = PullToRefreshDefaults.indicatorContainerColor,
                color = PullToRefreshDefaults.indicatorColor,
                maxDistance = PullToRefreshDefaults.PositionalThreshold,
            )
        },
    ) {
        Crossfade(targetState = data) { data ->
            // While a manual refresh loads, the previous list stays visible under the top spinner.
            val list =
                (data as? LocalResource.Success)?.data
                    ?: if (data is LocalResource.Loading && userRefreshing) lastList else emptyList()
            // A header counts as content: a tab whose list is empty but whose header is the
            // point of the tab must not fall through to the empty text and hide it.
            if (((data is LocalResource.Success || data is LocalResource.Loading) && list.isNotEmpty()) ||
                createNewPlaylist != null ||
                header != null
            ) {
                LazyVerticalGrid(
                    columns = LibraryGridCells,
                    contentPadding = contentPadding,
                    state = state,
                ) {
                    if (header != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            header()
                        }
                    }
                    if (createNewPlaylist != null) {
                        item {
                            Box(
                                modifier =
                                    Modifier.clickable {
                                        createNewPlaylist()
                                    },
                            ) {
                                Column(
                                    modifier =
                                        Modifier
                                            .padding(10.dp),
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        Alignment.Center,
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(48.dp),
                                            imageVector = SimpIcons.Add,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            contentDescription = stringResource(Res.string.create_playlist_action),
                                        )
                                    }
                                    Text(
                                        text = stringResource(Res.string.create_playlist_action),
                                        style = typo().titleSmall,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight(align = Alignment.CenterVertically)
                                                .padding(top = 8.dp)
                                                .basicMarquee(
                                                    iterations = Int.MAX_VALUE,
                                                    animationMode = MarqueeAnimationMode.Immediately,
                                                ).focusable(),
                                    )
                                }
                            }
                        }
                    }
                    itemsIndexed(
                        items = list,
                        key = { idx, item ->
                            when (item) {
                                is ChartItem -> "chart_${item.ytPlaylistId}_$idx"
                                is LocalPlaylistEntity -> "local_${item.id}"
                                is PlaylistsResult -> "ytpl_${item.browseId}_$idx"
                                is AlbumEntity -> "album_${item.browseId}_$idx"
                                is PlaylistEntity -> "pl_${item.id}_$idx"
                                is PodcastsEntity -> "podcast_${item.podcastId}_$idx"
                                is MonthlyRecapItem -> "recap_${item.year}_${item.month}"
                                else -> "grid_${item.hashCode()}_$idx"
                            }
                        },
                    ) { _, item ->
                        if (item !is PlaylistType) {
                            return@itemsIndexed
                        }
                        HomeItemContentPlaylist(
                            onClick = {
                                when (item) {
                                    is ChartItem -> {
                                        navController.navigate(
                                            PlaylistDestination(
                                                playlistId = item.ytPlaylistId,
                                                isYourYouTubePlaylist = false,
                                            ),
                                        )
                                    }

                                    is LocalPlaylistEntity -> {
                                        navController.navigate(
                                            LocalPlaylistDestination(
                                                item.id,
                                            ),
                                        )
                                    }

                                    is PlaylistsResult -> {
                                        navController.navigate(
                                            PlaylistDestination(
                                                item.browseId,
                                                isYourYouTubePlaylist = true,
                                            ),
                                        )
                                    }

                                    is AlbumEntity -> {
                                        navController.navigate(
                                            AlbumDestination(
                                                item.browseId,
                                            ),
                                        )
                                    }

                                    is PlaylistEntity -> {
                                        navController.navigate(
                                            PlaylistDestination(
                                                item.id,
                                            ),
                                        )
                                    }

                                    is PodcastsEntity -> {
                                        navController.navigate(
                                            PodcastDestination(
                                                podcastId = item.podcastId,
                                            ),
                                        )
                                    }

                                    // The recap is not a stored playlist — it is a query over
                                    // `playback_event` — so it opens the dynamic playlist screen,
                                    // which rebuilds it from the year and month carried here.
                                    is MonthlyRecapItem -> {
                                        navController.navigate(
                                            LibraryDynamicPlaylistDestination(
                                                type =
                                                    LibraryDynamicPlaylistType
                                                        .MonthlyRecap(
                                                            year = item.year,
                                                            month = item.month,
                                                        ).toStringParams(),
                                            ),
                                        )
                                    }
                                }
                            },
                            data = item,
                            thumbSize = 132.dp,
                            // The cell decides the width now, so the artwork follows it instead of
                            // being pinned to thumbSize and cropped by a narrower cell.
                            fillMaxWidth = true,
                        )
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EndOfPage()
                    }
                }
                VerticalScrollBar(
                    scrollState = state,
                    modifier = Modifier.padding(top = contentPadding.calculateTopPadding(), bottom = DesktopMiniPlayerClearance),
                )
            } else if (data is LocalResource.Loading) {
                CenterLoadingBox(
                    Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(emptyText),
                        style = typo().bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}