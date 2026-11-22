package com.maxrave.simpmusic.viewModel

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import com.maxrave.common.AppIdentity
import com.maxrave.common.Config
import com.maxrave.common.Config.ALBUM_CLICK
import com.maxrave.common.Config.DOWNLOAD_CACHE
import com.maxrave.common.Config.PLAYLIST_CLICK
import com.maxrave.common.Config.RECOVER_TRACK_QUEUE
import com.maxrave.common.Config.SHARE
import com.maxrave.common.Config.SONG_CLICK
import com.maxrave.common.Config.VIDEO_CLICK
import com.maxrave.common.LibraryChipType
import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.common.STATUS_DONE
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.LyricsEntity
import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.SongInfoEntity
import com.maxrave.domain.data.entities.TranslatedLyricsEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.canvas.CanvasResult
import com.maxrave.domain.data.model.download.DownloadProgress
import com.maxrave.domain.data.model.intent.GenericIntent
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.model.promo.Promo
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.data.model.update.UpdateData
import com.maxrave.domain.data.player.GenericCastState
import com.maxrave.domain.data.player.LiveStreamRegistry
import com.maxrave.domain.extension.decodeHtmlEntities
import com.maxrave.domain.extension.isSong
import com.maxrave.domain.extension.isVideo
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.lyrics.LyricsContentQualityGuard
import com.maxrave.domain.lyrics.LyricsHeaderSanitizer
import com.maxrave.domain.lyrics.LyricsLookupRequest
import com.maxrave.domain.lyrics.LyricsLanguageDetector
import com.maxrave.domain.lyrics.LyricsQuality
import com.maxrave.domain.lyrics.LyricsResolution
import com.maxrave.domain.lyrics.LyricsResolutionEvent
import com.maxrave.domain.lyrics.LyricsSource
import com.maxrave.domain.lyrics.LyricsTranslationResult
import com.maxrave.domain.lyrics.LyricsTranslationSyncValidator
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.data.model.lyrics.RomanizationLanguage
import com.maxrave.domain.manager.DataStoreManager.Values.FALSE
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.NowPlayingTrackState
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.domain.mediaservice.handler.SleepTimerState
import com.maxrave.domain.mediaservice.handler.addToQueueBlock
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.DiscordRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.LyricsCanvasRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.domain.repository.UpdateRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.YouTubeLikeStatus
import com.maxrave.domain.utils.isEndlessQueueId
import com.maxrave.domain.utils.toListName
import com.maxrave.domain.utils.toLyrics
import com.maxrave.domain.utils.toLyricsEntity
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toSyncedLyrics
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.LogLevel
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.getDownloadFolderPath
import com.maxrave.simpmusic.expect.isOnUnmeteredNetwork
import com.maxrave.simpmusic.expect.ui.toByteArray
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.LyricsUiPrep
import com.maxrave.simpmusic.utils.LyricsPromo
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.utils.decodeLyricsPromos
import com.maxrave.simpmusic.utils.decodePromos
import com.maxrave.simpmusic.utils.shownPromoIds
import com.maxrave.simpmusic.utils.withShownPromo
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject
import org.simpmusic.lastfm.completeLogin
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.added_local_playlist
import simpmusic.composeapp.generated.resources.added_to_queue
import simpmusic.composeapp.generated.resources.added_to_youtube_liked
import simpmusic.composeapp.generated.resources.disliked
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.discord_login_failed
import simpmusic.composeapp.generated.resources.lastfm_login_failed
import simpmusic.composeapp.generated.resources.login_success
import simpmusic.composeapp.generated.resources.lyrics_already_in_language
import simpmusic.composeapp.generated.resources.play_next
import simpmusic.composeapp.generated.resources.removed_from_youtube_liked
import simpmusic.composeapp.generated.resources.retranslating_lyrics
import simpmusic.composeapp.generated.resources.shared
import simpmusic.composeapp.generated.resources.translation_failed
import simpmusic.composeapp.generated.resources.updated
import simpmusic.composeapp.generated.resources.vote_submitted
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

// DataStore key for the kotlin-footguns star prompt. Cleared on every version bump,
// so the ask comes back after an update - same policy as OPEN_APP_TIME.
const val FOOTGUNS_STAR_KEY = "footguns_starred"
private const val USER_PROTECTED_SIGNATURES_KEY = "user_protected_lyrics_signatures"
private const val MAX_PERSISTED_PROTECTED_SIGNATURES = 500

@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModel(
    private val dataStoreManager: DataStoreManager,
    private val streamRepository: StreamRepository,
    private val updateRepository: UpdateRepository,
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val lyricsCanvasRepository: LyricsCanvasRepository,
    private val cacheRepository: CacheRepository,
    private val discordRepository: DiscordRepository,
) : BaseViewModel() {
    var isFirstLiked: Boolean = false
    var isFirstMiniplayer: Boolean = false
    var isFirstSuggestions: Boolean = false
    var showedUpdateDialog: Boolean = false

    private val appIdentity: AppIdentity by inject()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate

    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val liked: SharedFlow<Boolean> = _liked.asSharedFlow()

    var isServiceRunning: Boolean = false

    private var _sleepTimerState = MutableStateFlow(SleepTimerState(false, 0))
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState

    private var regionCode: String? = null
    private var language: String? = null

    private var _format: MutableStateFlow<NewFormatEntity?> = MutableStateFlow(null)
    val format: SharedFlow<NewFormatEntity?> = _format.asSharedFlow()

    /**
     * Which extractor and cipher decoder produced the current track's URLs, shown in the info sheet.
     *
     * Read alongside the format rather than stored with it: the format row is cached and reused,
     * while this describes the extraction that happened in this run of the app.
     */
    private val _extractSource: MutableStateFlow<String?> = MutableStateFlow(null)
    val extractSource: StateFlow<String?> = _extractSource.asStateFlow()

    private var _canvas: MutableStateFlow<CanvasResult?> = MutableStateFlow(null)
    val canvas: StateFlow<CanvasResult?> = _canvas

    private var canvasJob: Job? = null
    private var lyricsJob: Job? = null
    private var translationJob: Job? = null

    /**
     * Isolated translation visibility/loading state so toggling the translate button or animating
     * its spinner only recomposes the button and [LyricsView], never the full player or blur.
     */
    private val _lyricsTranslationUiState = MutableStateFlow(LyricsTranslationUiState())
    val lyricsTranslationUiState: StateFlow<LyricsTranslationUiState> = _lyricsTranslationUiState.asStateFlow()
    private var pendingYoutubeTranslatedLyrics: Pair<String, Lyrics>? = null

    // Track ids whose canvas / lyrics were already requested. ExoPlayer refines a track's duration
    // after the container is parsed (131000 -> 130981 ms), which re-ran the timeline job and
    // fetched the same canvas/lyrics again while the first request was still in flight.
    private var canvasRequestedFor: String? = null
    private var lyricsRequestedFor: String? = null

    private val upgradedVideoIds = mutableSetOf<String>()
    private val userProtectedSignatures = LinkedHashSet<Int>()
    private var userProtectedSignaturesLoaded = false
    private var pendingLyricsRetryVideoId: String? = null

    private val _intent: MutableStateFlow<GenericIntent?> = MutableStateFlow(null)
    val intent: StateFlow<GenericIntent?> = _intent

    private val _showNotificationPermissionDialog = MutableStateFlow(false)
    val showNotificationPermissionDialog: StateFlow<Boolean> = _showNotificationPermissionDialog

    private val _isOfficialBuild = MutableStateFlow(true)
    val isOfficialBuild: StateFlow<Boolean> = _isOfficialBuild

    // One-shot: the Desktop capsule asks the Now Playing panel, which hosts the page, to open
    // full-screen lyrics. The panel consumes it once shown.
    private val _fullscreenLyricsRequest = MutableStateFlow(false)
    val fullscreenLyricsRequest: StateFlow<Boolean> = _fullscreenLyricsRequest

    fun requestFullscreenLyrics() {
        _fullscreenLyricsRequest.value = true
    }

    fun consumeFullscreenLyricsRequest() {
        _fullscreenLyricsRequest.value = false
    }

    private var getFormatFlowJob: Job? = null

    var playlistId: MutableStateFlow<String?> = MutableStateFlow(null)

    var isFullScreen: Boolean = false

    private var _nowPlayingState = MutableStateFlow<NowPlayingTrackState?>(null)
    val nowPlayingState: StateFlow<NowPlayingTrackState?> = _nowPlayingState

    fun getQueueDataState() = mediaPlayerHandler.queueData

    val castState: StateFlow<GenericCastState> get() = mediaPlayerHandler.castState

    private var _controllerState =
        MutableStateFlow<ControlState>(
            ControlState(
                isPlaying = false,
                isShuffle = false,
                repeatState = RepeatState.None,
                isLiked = false,
                isNextAvailable = false,
                isPreviousAvailable = false,
                isCrossfading = false,
                volume = 1f,
            ),
        )
    val controllerState: StateFlow<ControlState> = _controllerState
    private val _getVideo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val getVideo: StateFlow<Boolean> = _getVideo

    private var _timeline =
        MutableStateFlow<TimeLine>(
            TimeLine(
                current = -1L,
                total = -1L,
                bufferedPercent = 0,
                loading = true,
            ),
        )
    val timeline: StateFlow<TimeLine> = _timeline

    // Last progress value actually published to the timeline; the Progress branch below
    // coalesces position-only updates so the UI does not recompose 20x per second.
    private var lastPublishedProgressMs = -1L

    private var _nowPlayingScreenData =
        MutableStateFlow<NowPlayingScreenData>(
            NowPlayingScreenData.initial(),
        )
    val nowPlayingScreenData: StateFlow<NowPlayingScreenData> = _nowPlayingScreenData

    private var _likeStatus = MutableStateFlow(YouTubeLikeStatus.INDIFFERENT)
    val likeStatus: StateFlow<YouTubeLikeStatus> = _likeStatus
    private var likeStatusJob: Job? = null

    // The track whose dislike request is still waiting for YouTube. An id rather than a flag, so the
    // pending state belongs to that track only and the next one starts out dislikeable.
    private val _dislikingVideoId = MutableStateFlow<String?>(null)
    val isDislikePending: StateFlow<Boolean> =
        combine(_dislikingVideoId, mediaPlayerHandler.nowPlaying) { pending, now ->
            pending != null && pending == now?.mediaId
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Which body of the Apple Music player was open last — held by ENUM NAME so this class stays
     * ignorant of the UI enum, which is internal to the player package.
     *
     * It cannot live in the composable: that player is inside a ModalBottomSheet, so dismissing
     * the sheet disposes the whole tree and takes any rememberSaveable with it — the tab snapped
     * back to the artwork every single time it was reopened. This class is a Koin `single`, so it
     * outlives the sheet while still resetting on app restart, which is the right lifetime for
     * "where I was a moment ago".
     */
    private val _lastPlayerViewTab = MutableStateFlow<String?>(null)
    val lastPlayerViewTab: StateFlow<String?> = _lastPlayerViewTab

    fun setLastPlayerViewTab(tabName: String) {
        _lastPlayerViewTab.value = tabName
    }

    val openAppTime: StateFlow<Int> = dataStoreManager.openAppTime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)
    private val _shareSavedLyrics: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val shareSavedLyrics: StateFlow<Boolean> get() = _shareSavedLyrics

    private var songInfoJob: Job? = null

    // Vote state for translated lyrics
    private val _translatedVoteState = MutableStateFlow<VoteData?>(null)
    val translatedVoteState: StateFlow<VoteData?> = _translatedVoteState.asStateFlow()

    // Vote state for original lyrics
    private val _lyricsVoteState = MutableStateFlow<VoteData?>(null)
    val lyricsVoteState: StateFlow<VoteData?> = _lyricsVoteState.asStateFlow()

    init {
        viewModelScope.launch {
            log("SharedViewModel init")
            if (dataStoreManager.appVersion.first() != VersionManager.getVersionName()) {
                dataStoreManager.resetOpenAppTime()
                dataStoreManager.putString(FOOTGUNS_STAR_KEY, "false")
                dataStoreManager.setAppVersion(
                    VersionManager.getVersionName(),
                )
            }
            dataStoreManager.openApp()
            launch { ensureUserProtectedSignaturesLoaded() }
            val timeLineJob =
                launch {
                    nowPlayingState
                        .filterNotNull()
                        .flatMapLatest { nowPlayingState ->
                            timeline.map { timeLine ->
                                Pair(timeLine, nowPlayingState)
                            }
                        }.distinctUntilChanged { old, new ->
                            // A live stream's "total" is the seek window mpv reports, and it grows with
                            // every new segment — not a new length, so it must not re-run what a new
                            // length triggers (this fired every few seconds for as long as one played).
                            val sameLiveStream = old.first.isLive && new.first.isLive
                            val sameVideo = old.second.songEntity?.videoId == new.second.songEntity?.videoId
                            (sameVideo && old.first.total == new.first.total) ||
                                (sameLiveStream && sameVideo)
                        }.collectLatest {
                            val nowPlaying = it.second
                            val timeline = it.first
                            // Lyrics and canvas both key off the track's length, which a live stream has none of.
                            if (timeline.isLive) return@collectLatest
                            log("Timeline job ${timeline.total} ${nowPlaying.songEntity?.videoId}")
                            if (timeline.total > 0 && nowPlaying.songEntity != null) {
                                val currentMediaId = nowPlaying.mediaItem.mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
                                // A songEntity that does not belong to the mediaItem is a transition
                                // caught half-way: acting on it fetched the OLD song's lyrics.
                                if (nowPlaying.songEntity?.videoId != currentMediaId) {
                                    Logger.w(tag, "Skipping canvas/lyrics: songEntity ${nowPlaying.songEntity?.videoId} != media $currentMediaId")
                                    return@collectLatest
                                }
                                if (nowPlaying.mediaItem.isSong() &&
                                    nowPlayingScreenData.value.canvasData == null &&
                                    canvasRequestedFor != currentMediaId
                                ) {
                                    canvasRequestedFor = currentMediaId
                                    Logger.w(tag, "Duration is ${timeline.total}")
                                    Logger.w(tag, "MediaId is ${nowPlaying.mediaItem.mediaId}")
                                    getCanvas(nowPlaying.mediaItem.mediaId, (timeline.total / 1000).toInt())
                                }
                                nowPlaying.songEntity?.let { song ->
                                    if (nowPlayingScreenData.value.lyricsData == null &&
                                        lyricsRequestedFor != song.videoId &&
                                        !LiveStreamRegistry.isLive(song.videoId)
                                    ) {
                                        lyricsRequestedFor = song.videoId
                                        Logger.w(tag, "Get lyrics from format")
                                        startLyricsResolution(
                                            song = song,
                                            artistHint = nowPlaying.mediaItem.metadata.artist,
                                            durationSec = (timeline.total / 1000).toInt(),
                                            preferCache = true,
                                            forceRefresh = false,
                                        )
                                    }
                                }
                            }
                        }
                }
            val checkGetVideoJob =
                launch {
                    dataStoreManager.watchVideoInsteadOfPlayingAudio.collectLatest {
                        _getVideo.value = it == TRUE
                    }
                }
            val lyricsProviderJob =
                launch {
                    dataStoreManager.lyricsProvider
                        .distinctUntilChanged()
                        .drop(1)
                        .collectLatest {
                            setLyricsProvider()
                        }
                }
            val shareSavedLyricsJob =
                launch {
                    dataStoreManager.helpBuildLyricsDatabase.distinctUntilChanged().collectLatest {
                        _shareSavedLyrics.value = it == TRUE
                    }
                }
            launch {
                mediaPlayerHandler.networkRestoredEvents.collect {
                    val state = _nowPlayingState.value ?: return@collect
                    val song = state.songEntity ?: return@collect
                    val retryVideoId = pendingLyricsRetryVideoId ?: return@collect
                    if (retryVideoId == song.videoId &&
                        _nowPlayingScreenData.value.lyricsData == null &&
                        !LiveStreamRegistry.isLive(song.videoId)
                    ) {
                        val durationSec =
                            (timeline.value.total / 1000L).toInt().takeIf { it > 0 }
                                ?: song.durationSeconds
                        lyricsRequestedFor = song.videoId
                        pendingLyricsRetryVideoId = null
                        startLyricsResolution(
                            song = song,
                            artistHint = state.mediaItem.metadata.artist,
                            durationSec = durationSec,
                            preferCache = false,
                            forceRefresh = false,
                        )
                    }
                }
            }
//            val controllerStateJob =
//                launch {
//                    controllerState.map { it.isLiked }.distinctUntilChanged().collectLatest {
//                        if (dataStoreManager.combineLocalAndYouTubeLiked.first() == TRUE) {
//                            nowPlayingState.value?.mediaItem?.mediaId?.let {
//                                getLikeStatus(it)
//                            }
//                        }
//                    }
//                }
            timeLineJob.join()
            checkGetVideoJob.join()
            lyricsProviderJob.join()
            shareSavedLyricsJob.join()
//            controllerStateJob.join()
        }

        viewModelScope.launch {
            dataStoreManager.getString("miniplayer_guide").first().let {
                isFirstMiniplayer = it != STATUS_DONE
            }
            dataStoreManager.getString("suggest_guide").first().let {
                isFirstSuggestions = it != STATUS_DONE
            }
            dataStoreManager.getString("liked_guide").first().let {
                isFirstLiked = it != STATUS_DONE
            }
        }
        viewModelScope.launch {
            combine(
                dataStoreManager.translationLanguage,
                dataStoreManager.language,
            ) { translationLang, appLang ->
                translationLang.ifBlank { appLang }.ifBlank { "pt" }
            }.distinctUntilChanged().collect { targetLang ->
                val activeLyrics = _nowPlayingScreenData.value.lyricsData?.lyrics ?: return@collect
                val sameLang = LyricsLanguageDetector.isSameLanguage(activeLyrics, targetLang)
                _lyricsTranslationUiState.update { state ->
                    if (sameLang) {
                        state.copy(isSameLanguage = true, isVisible = false, isLoading = false)
                    } else {
                        state.copy(isSameLanguage = false)
                    }
                }
            }
        }
        viewModelScope.launch {
            mediaPlayerHandler.nowPlayingState
                .distinctUntilChangedBy {
                    it.songEntity?.videoId
                }.collectLatest { state ->
                    canvasJob?.cancel()
                    lyricsJob?.cancel()
                    lyricsJob = null
                    translationJob?.cancel()
                    translationJob = null
                    pendingYoutubeTranslatedLyrics = null
                    _lyricsTranslationUiState.value = LyricsTranslationUiState()
                    canvasRequestedFor = null
                    lyricsRequestedFor = null
                    pendingLyricsRetryVideoId = null
                    resetLyricsVoteState()
                    _nowPlayingState.value = state

                    // Seed the timeline from METADATA as soon as the track is known, instead of
                    // waiting for the player. SimpleMediaState.Ready carries a duration and only
                    // fires once the container is parsed; after a queue restore nothing plays, the
                    // position poll never starts (it runs only while isPlaying), and so nothing
                    // ever reports a length — which is why a restored queue showed no times at all.
                    // SongEntity has known the length since the song was first seen.
                    //
                    // Written on EVERY track change, not just when missing: leaving the old value
                    // in place would show the previous track's length over the new one.
                    // Order matters: metadata first because it is available immediately, then the
                    // player's own duration, and only -1 ("not known yet") when neither has one.
                    //
                    // Writing -1 whenever metadata is missing — which this did at first — makes
                    // every radio track and every first-time track flash NA:NA on the clock until
                    // the container is parsed, because those rows have no durationSeconds stored.
                    // Asking the player before giving up covers exactly that case: on a normal
                    // track change it usually already knows.
                    val metadataDurationMs = (state.songEntity?.durationSeconds ?: 0).toLong() * 1000L
                    val seededTotal =
                        metadataDurationMs.takeIf { it > 0L }
                            ?: mediaPlayerHandler.getPlayerDuration().takeIf { it > 0L }
                            ?: -1L
                    _timeline.update { it.copy(total = seededTotal) }
                    state.songEntity?.let { track ->
                        _nowPlayingScreenData.value =
                            NowPlayingScreenData(
                                nowPlayingTitle = track.title,
                                artistName =
                                    track
                                        .artistName
                                        ?.joinToString(", ") ?: "",
                                isVideo = false,
                                thumbnailURL = null,
                                canvasData = null,
                                lyricsData = null,
                                songInfoData = null,
                                playlistName =
                                    mediaPlayerHandler.queueData.value
                                        ?.data
                                        ?.playlistName ?: "",
                                isRadioQueue =
                                    mediaPlayerHandler.queueData.value
                                        ?.data
                                        ?.playlistId
                                        ?.isEndlessQueueId() == true,
                            )
                    }
                    state.mediaItem.let { now ->
                        _canvas.value = null
                        getLikeStatus(now.mediaId)
                        getSongInfo(now.mediaId)
                        getFormat(now.mediaId)
                        _nowPlayingScreenData.update {
                            it.copy(
                                thumbnailURL = now.metadata.artworkUri,
                                isVideo = now.isVideo(),
                            )
                        }
                    }
                    state.songEntity?.let { song ->
                        _liked.value = song.liked == true
                        _nowPlayingScreenData.update {
                            it.copy(
                                isExplicit = song.isExplicit,
                            )
                        }
                        val currentMediaId = state.mediaItem.mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
                        val knownDurationSec = (seededTotal / 1000L).toInt()
                        if (song.videoId == currentMediaId &&
                            !LiveStreamRegistry.isLive(song.videoId) &&
                            knownDurationSec > 0
                        ) {
                            lyricsRequestedFor = song.videoId
                            startLyricsResolution(
                                song = song,
                                artistHint = state.mediaItem.metadata.artist,
                                durationSec = knownDurationSec,
                                preferCache = true,
                                forceRefresh = false,
                            )
                        }
                    }
                }
        }
        viewModelScope.launch {
            val job1 =
                launch {
                    mediaPlayerHandler.simpleMediaState.collect { mediaState ->
                        when (mediaState) {
                            is SimpleMediaState.Buffering -> {
                                _timeline.update {
                                    it.copy(
                                        loading = true,
                                    )
                                }
                            }

                            SimpleMediaState.Initial -> {
                                _timeline.update { it.copy(loading = true) }
                            }

                            SimpleMediaState.Ended -> {
                                // Park at the end of the track rather than at -1. The only formatter
                                // for these fields renders any negative as "NA:NA", and nothing here
                                // is guaranteed to follow: at the end of the queue the player simply
                                // stays ended, so a -1 written here stays on screen. Worse, the
                                // Progress branch below ignores negative values and the Loading
                                // branch restores `total` without touching `current`, which is how
                                // the player ends up showing a correct duration next to "NA:NA".
                                _timeline.update {
                                    it.copy(
                                        current = it.total.coerceAtLeast(0L),
                                        bufferedPercent = 0,
                                        loading = false,
                                    )
                                }
                            }

                            is SimpleMediaState.Progress -> {
                                if (mediaState.progress >= 0L && mediaState.progress != _timeline.value.current) {
                                    // A live stream never reports a length (ExoPlayer: C.TIME_UNSET), so
                                    // a missing one means "loading" for everything except a live stream.
                                    val currentTotal = _timeline.value.total
                                    val hasLength = currentTotal > 0L || _timeline.value.isLive
                                    val progressDelta = mediaState.progress - lastPublishedProgressMs
                                    val dueByTime =
                                        lastPublishedProgressMs < 0L || progressDelta < 0L || progressDelta >= 200L
                                    if (!dueByTime && currentTotal > 0L && hasLength != _timeline.value.loading) {
                                        return@collect
                                    }
                                    val newTotal =
                                        mediaPlayerHandler.getPlayerDuration().takeIf { d -> d > 0L } ?: currentTotal
                                    // Publish at most ~5x per second while only the position moves:
                                    // this branch fires on a 50 ms ticker and every publish recomposes
                                    // the player and mini-player screens. A total/loading change, a
                                    // seek (backwards jump) and the first publish still go through
                                    // immediately, so nothing the UI shows lags behind the player.
                                    val slowStateChanged =
                                        newTotal != currentTotal || hasLength == _timeline.value.loading
                                    if (slowStateChanged || dueByTime) {
                                        lastPublishedProgressMs = mediaState.progress
                                        _timeline.update {
                                            it.copy(
                                                total = newTotal,
                                                current = mediaState.progress,
                                                // A Progress publish at position 0 is not
                                                // evidence that anything loaded: during a
                                                // load that cannot proceed (no network) the
                                                // position ticker keeps publishing 0, and
                                                // clearing the flag here froze the bar at
                                                // 0:00 over a track with zero bytes
                                                // buffered. Only a playhead that actually
                                                // moved past 0 (or a live stream) may clear
                                                // it; at 0 the flag is left as the
                                                // Buffering/Loading events set it.
                                                loading =
                                                    if (mediaState.progress > 0L || it.isLive) {
                                                        !hasLength
                                                    } else {
                                                        it.loading
                                                    },
                                            )
                                        }
                                    }
                                }
                                // When progress hasn't changed (same value polled again) or is negative,
                                // don't modify loading state. The loading flag is already managed by
                                // Buffering/Ready/Loading state events. Setting loading=true here would
                                // cause rapid flickering whenever the same value arrives twice,
                                // which it can: the handler's ticker and the adapter's position
                                // poll both run at 50ms and are not in step, so a poll is sometimes
                                // read twice. A repeat is not evidence of a stall, and the loading
                                // flag belongs to the Buffering/Ready events rather than to a
                                // guess made here.
                            }

                            is SimpleMediaState.Loading -> {
                                _timeline.update {
                                    it.copy(
                                        bufferedPercent = mediaState.bufferedPercentage,
                                        total = mediaState.duration,
                                        loading = true,
                                    )
                                }
                            }

                            is SimpleMediaState.Ready -> {
                                _timeline.update {
                                    // A Ready carrying neither a duration nor any progress
                                    // is a track that never loaded a single byte: the
                                    // player adapter reports READY for a dead load too
                                    // (its READY and PAUSED states both surface as
                                    // STATE_READY, with duration still TIME_UNSET).
                                    // Clearing the loading flag for it left a static bar
                                    // at 0:00 over a track still waiting on the network,
                                    // so such a Ready keeps the loading state instead.
                                    val readyProgress = mediaPlayerHandler.getProgress()
                                    // Duration alone is not proof either: if the network
                                    // dies after the container was parsed, the duration is
                                    // already known while not a single byte is playable.
                                    // What proves readiness is progress, or buffered media
                                    // ahead of the playhead.
                                    val actuallyReady =
                                        readyProgress > 0L ||
                                            (mediaState.duration > 0L && mediaPlayerHandler.player.bufferedPosition > readyProgress)
                                    it.copy(
                                        current = readyProgress,
                                        loading = !actuallyReady,
                                        // The player's own duration wins, but ONLY when it has one.
                                        // ExoPlayer answers C.TIME_UNSET (a large negative, not
                                        // null) until it has parsed the container, and Ready is
                                        // also published from onIsLoadingChanged — which fires
                                        // before STATE_READY. Writing that would throw away the
                                        // metadata duration seeded on the track change above.
                                        total = mediaState.duration.takeIf { d -> d > 0L } ?: it.total,
                                    )
                                }
                            }
                        }
                    }
                }
            val controllerJob =
                launch {
                    Logger.w(tag, "ControllerJob is running")
                    mediaPlayerHandler.controlState.collectLatest {
                        Logger.w(tag, "ControlState is $it")
                        _controllerState.value = it
                        // Propagate crossfade state to timeline so UI can react
                        _timeline.update { timeline ->
                            timeline.copy(isCrossfading = it.isCrossfading)
                        }
                    }
                }
            val sleepTimerJob =
                launch {
                    mediaPlayerHandler.sleepTimerState.collectLatest {
                        _sleepTimerState.value = it
                    }
                }
            val playlistNameJob =
                launch {
                    mediaPlayerHandler.queueData.collectLatest { queue ->
                        _nowPlayingScreenData.update {
                            it.copy(
                                playlistName = queue?.data?.playlistName ?: "",
                                isRadioQueue = queue?.data?.playlistId?.isEndlessQueueId() == true,
                            )
                        }
                    }
                }
            val liveStreamJob =
                launch {
                    // A track is only known to be live once its stream has been resolved, so this
                    // follows the registry as well as the track itself.
                    combine(mediaPlayerHandler.nowPlayingState, LiveStreamRegistry.liveVideoIds) { state, liveVideoIds ->
                        state.mediaItem.mediaId in liveVideoIds
                    }.distinctUntilChanged()
                        .collect { isLive -> _timeline.update { it.copy(isLive = isLive) } }
                }
            job1.join()
            controllerJob.join()
            sleepTimerJob.join()
            playlistNameJob.join()
            liveStreamJob.join()
        }
        // Reset downloading songs & playlists to not downloaded
        checkAllDownloadingSongs()
        checkAllDownloadingPlaylists()
        checkAllDownloadingLocalPlaylists()
    }

    fun setIntent(intent: GenericIntent?) {
        _intent.value = intent
    }

    /**
     * Finishes a Last.fm login from the `wordbyword://lastfm-auth` callback.
     *
     * It lands here, and not on the login screen, because the callback arrives at the app rather
     * than at any one screen: the user left for their browser, and the screen they left from may
     * not even exist any more if the process was killed. Routing the token onwards through
     * navigation would push a second copy of the login screen on top of the one already open.
     *
     * The screen learns it succeeded by watching the stored session key, not by being told.
     */
    fun completeLastfmLogin(token: String) {
        if (token.isEmpty()) return
        viewModelScope.launch {
            val session = completeLogin(token)
            if (session != null) {
                dataStoreManager.setLastfmSession(
                    sessionKey = session.sessionKey,
                    username = session.username,
                )
                makeToast(getString(Res.string.login_success))
            } else {
                makeToast(getString(Res.string.lastfm_login_failed))
            }
        }
    }

    /**
     * Finishes a Discord login from the `simpmusic://discord-auth` redirect (Full builds). Like the
     * Last.fm callback, the login screen is already open and closes itself once a login is stored.
     */
    fun completeDiscordLogin(callbackUrl: String) {
        viewModelScope.launch {
            if (discordRepository.completeLogin(callbackUrl)) {
                makeToast(getString(Res.string.login_success))
            } else {
                makeToast(getString(Res.string.discord_login_failed))
            }
        }
    }

    fun showNotificationPermissionDialog() {
        _showNotificationPermissionDialog.value = true
    }

    fun dismissNotificationPermissionDialog(doNotShowAgain: Boolean) {
        _showNotificationPermissionDialog.value = false
        if (doNotShowAgain) {
            putString("notification_permission_do_not_ask", "true")
        }
    }

    private fun getLikeStatus(videoId: String?) {
        // Only the latest track may write: a slower reply for the previous track would otherwise
        // land on this one.
        likeStatusJob?.cancel()
        likeStatusJob =
            viewModelScope.launch {
                if (videoId != null) {
                    _likeStatus.value = YouTubeLikeStatus.INDIFFERENT
                    songRepository.getLikeStatus(videoId).collectLatest { status ->
                        _likeStatus.value = status
                    }
                }
            }
    }

    private fun getCanvas(
        videoId: String,
        duration: Int,
    ) {
        Logger.w(tag, "Start getCanvas: $videoId $duration")
        canvasJob?.cancel()
        canvasJob = viewModelScope.launch {
            // Both sources fill the same slot, so they are tried in order rather than raced:
            // animated artwork first, and a Spotify canvas only if that found nothing AND the user
            // has that switch on too. Neither switch touches the other — a track with no animated
            // artwork still gets its canvas, and turning Spotify off still means no canvas at all.
            val sources =
                buildList {
                    if (dataStoreManager.amAnimatedArtwork.first() == TRUE) {
                        add(lyricsCanvasRepository.getAMAnimatedArtwork(videoId))
                    }
                    if (dataStoreManager.spotifyCanvas.first() == TRUE) {
                        add(lyricsCanvasRepository.getCanvas(dataStoreManager, videoId, duration))
                    }
                }
            if (sources.isEmpty()) return@launch

            var resolved = false
            for (source in sources) {
                if (resolved) break
                source.cancellable().collect { response ->
                    val data = response.data
                    if (response is Resource.Success && data != null && nowPlayingState.value?.mediaItem?.mediaId == videoId) {
                        resolved = true
                        _canvas.value = data
                        _nowPlayingScreenData.update {
                            it.copy(
                                canvasData =
                                    NowPlayingScreenData.CanvasData(
                                        isVideo = data.isVideo,
                                        url = data.canvasUrl,
                                        thumbUrl = data.canvasThumbUrl,
                                    ),
                            )
                        }
                        // Save canvas video url
                        if (data.isVideo) lyricsCanvasRepository.updateCanvasUrl(videoId, data.canvasUrl)
                        // Save canvas thumb url
                        data.canvasThumbUrl?.let { lyricsCanvasRepository.updateCanvasThumbUrl(videoId, it) }
                    } else {
                        log("Get canvas miss from a source: ${response.message}")
                    }
                }
            }

            if (!resolved) {
                nowPlayingState.value?.songEntity?.canvasUrl?.let { url ->
                    _nowPlayingScreenData.update {
                        it.copy(
                            canvasData =
                                NowPlayingScreenData.CanvasData(
                                    isVideo = url.isCanvasVideoUrl(),
                                    url = url,
                                    thumbUrl = nowPlayingState.value?.songEntity?.canvasThumbUrl,
                                ),
                        )
                    }
                }
            }
        }
    }

    fun getString(key: String): String? = runBlocking { dataStoreManager.getString(key).first() }

    /**
     * The `promos` block the last config fetch saved, as it changes. Every DataStore write re-emits
     * the whole settings file (the playback position is saved every few seconds), so only a change
     * to this block's own text gets through.
     */
    fun cachedPromos(): Flow<List<Promo>> = flowOf(emptyList())

    /** The same block's entries placed under the lyrics, as it changes. */
    fun cachedLyricsPromos(): Flow<List<LyricsPromo>> = flowOf(emptyList())

    /** The ids of the launch banners this app version has already shown. */
    suspend fun shownPromos(): Set<String> = shownPromosFlow().first()

    /** The same ids as they change: the lyrics line stays on screen, and on two pages at once. */
    fun shownPromosFlow(): Flow<Set<String>> =
        dataStoreManager
            .getString(Promo.shownKey(appIdentity.isDevBuild))
            .distinctUntilChanged()
            .map { shownPromoIds(stored = it, appVersion = VersionManager.getVersionName()) }

    /** Each banner once per app version, however it was closed. */
    fun markPromoShown(id: String) {
        viewModelScope.launch {
            val key = Promo.shownKey(appIdentity.isDevBuild)
            dataStoreManager.putString(
                key,
                withShownPromo(dataStoreManager.getString(key).first(), VersionManager.getVersionName(), id),
            )
        }
    }

    // A Library tab asked for from outside the Library screen (a deep link); the screen selects it
    // the next time it composes and clears the request.
    private val _libraryTabRequest = MutableStateFlow<LibraryChipType?>(null)
    val libraryTabRequest: StateFlow<LibraryChipType?> = _libraryTabRequest.asStateFlow()

    fun requestLibraryTab(tab: LibraryChipType?) {
        _libraryTabRequest.value = tab
    }

    // The song recognition sheet asked for from outside Search (simpmusic://recognize, the Quick
    // Settings tile); Search opens it the next time it composes and clears the request.
    private val _songRecognitionRequest = MutableStateFlow(false)
    val songRecognitionRequest: StateFlow<Boolean> = _songRecognitionRequest.asStateFlow()

    fun requestSongRecognition(requested: Boolean) {
        _songRecognitionRequest.value = requested
    }

    fun putString(
        key: String,
        value: String,
    ) {
        viewModelScope.launch { dataStoreManager.putString(key, value) }
    }

    fun setSleepTimer(minutes: Int) {
        mediaPlayerHandler.sleepStart(minutes)
    }

    fun stopSleepTimer() {
        mediaPlayerHandler.sleepStop()
    }

    private var _downloadState: MutableStateFlow<DownloadHandler.Download?> = MutableStateFlow(null)
    var downloadState: StateFlow<DownloadHandler.Download?> = _downloadState.asStateFlow()

    fun checkIsRestoring() {
        viewModelScope.launch {
            val downloadedCacheKeys = cacheRepository.getAllCacheKeys(DOWNLOAD_CACHE)
            songRepository.getDownloadedSongs().first().let { songs ->
                songs?.forEach { song ->
                    if (!downloadedCacheKeys.contains(song.videoId)) {
                        songRepository.updateDownloadState(
                            song.videoId,
                            DownloadState.STATE_NOT_DOWNLOADED,
                        )
                    }
                }
            }
            playlistRepository.getAllDownloadedPlaylist().first().let { list ->
                for (data in list) {
                    when (data) {
                        is AlbumEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                albumRepository.updateAlbumDownloadState(
                                    data.browseId,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is PlaylistEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                playlistRepository.updatePlaylistDownloadState(
                                    data.id,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is LocalPlaylistEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                localPlaylistRepository.updateLocalPlaylistDownloadState(
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                    data.id,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun insertLyrics(lyrics: LyricsEntity) {
        viewModelScope.launch {
            lyricsCanvasRepository.insertLyrics(lyrics)
        }
    }

    /**
     * Lyrics typed into Add lyrics take the offline lyrics' path: [updateLyrics] caches and shows
     * them, and contributes them when Help build lyrics database is on. The cache write of its own
     * keeps them for a song that ended while they were being typed, which [updateLyrics] skips.
     */
    fun saveUserLyrics(
        videoId: String,
        lyrics: Lyrics,
    ) {
        val durationSec =
            (timeline.value.total / 1000L).toInt().takeIf { it > 0 }
                ?: _nowPlayingState.value?.songEntity?.takeIf { it.videoId == videoId }?.durationSeconds
                ?: 0
        val currentVideoId = _nowPlayingState.value?.songEntity?.videoId
        if (currentVideoId == videoId) {
            lyricsJob?.cancel()
            lyricsJob = null
            translationJob?.cancel()
            translationJob = null
            pendingYoutubeTranslatedLyrics = null
            _lyricsTranslationUiState.value = LyricsTranslationUiState()
        }
        viewModelScope.launch {
            val decoded =
                withContext(Dispatchers.Default) {
                    decodeLyricsEntities(lyrics)
                }
            recordUserProtectedSignatures(lyrics, decoded)
            insertLyrics(decoded.toLyricsEntity(videoId))
            updateLyrics(
                videoId = videoId,
                duration = durationSec,
                inputLyrics = decoded,
                isTranslatedLyrics = false,
                lyricsProvider = LyricsProvider.OFFLINE,
                persistToCache = false,
                canContributeToSimpMusic = true,
                alreadyDecoded = true,
            )
            if (_nowPlayingState.value?.songEntity?.videoId == videoId) {
                val sameLanguage = isLyricsInTargetLanguage(decoded)
                _lyricsTranslationUiState.value = LyricsTranslationUiState(isSameLanguage = sameLanguage)
                if (!sameLanguage) {
                    scheduleAutoTranslationIfEnabled(videoId, decoded)
                }
            }
        }
    }

    fun loadSharedMediaItem(videoId: String) {
        viewModelScope.launch {
            val localSong = songRepository.getSongById(videoId).firstOrNull()
            if (localSong != null) {
                val track = localSong.toTrack()
                mediaPlayerHandler.setQueueData(
                    QueueData.Data(
                        listTracks = arrayListOf(track),
                        firstPlayedTrack = track,
                        playlistId = "RDAMVM$videoId",
                        playlistName = getString(Res.string.shared),
                        playlistType = PlaylistType.RADIO,
                        continuation = null,
                    ),
                )
                loadMediaItemFromTrack(track, SONG_CLICK)
            } else {
                streamRepository.getFullMetadata(videoId).collectLatest { response ->
                    val track = response.data
                    when (response) {
                        is Resource.Success if (track != null) -> {
                            mediaPlayerHandler.setQueueData(
                                QueueData.Data(
                                    listTracks = arrayListOf(track),
                                    firstPlayedTrack = track,
                                    playlistId = "RDAMVM$videoId",
                                    playlistName = getString(Res.string.shared),
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            loadMediaItemFromTrack(track, SONG_CLICK)
                        }

                        else -> {
                            log("Load shared media item error: ${response.message}", LogLevel.WARN)
                            makeToast("${getString(Res.string.error)}: ${response.message}")
                        }
                    }
                }
            }
        }
    }

    fun loadMediaItemFromTrack(
        track: Track,
        type: String,
        index: Int? = null,
    ) {
        viewModelScope.launch {
            mediaPlayerHandler.clearMediaItems()
            songRepository.insertSong(track.toSongEntity()).lastOrNull()?.let {
                println("insertSong: $it")
                songRepository
                    .getSongById(track.videoId)
                    .collect { songEntity ->
                        if (songEntity != null) {
                            Logger.w("Check like", "loadMediaItemFromTrack ${songEntity.liked}")
                            _liked.value = songEntity.liked
                        }
                    }
            }
            track.durationSeconds?.let {
                songRepository.updateDurationSeconds(
                    it,
                    track.videoId,
                )
            }
            withContext(Dispatchers.Main) {
                mediaPlayerHandler.addMediaItem(track.toGenericMediaItem(), playWhenReady = type != RECOVER_TRACK_QUEUE)
            }

            when (type) {
                SONG_CLICK -> {
                    mediaPlayerHandler.getRelated(track.videoId)
                }

                VIDEO_CLICK -> {
                    mediaPlayerHandler.getRelated(track.videoId)
                }

                SHARE -> {
                    mediaPlayerHandler.getRelated(track.videoId)
                }

                PLAYLIST_CLICK -> {
                    if (index == null) {
//                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = 0)
                    } else {
//                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = index)
                    }
                }

                ALBUM_CLICK -> {
                    if (index == null) {
//                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = 0)
                    } else {
//                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = index)
                    }
                }
            }
        }
    }

    fun onUIEvent(uiEvent: UIEvent) =
        viewModelScope.launch {
            when (uiEvent) {
                UIEvent.Backward -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.Backward,
                    )
                }

                UIEvent.Forward -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Forward)
                }

                UIEvent.PlayPause -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.PlayPause,
                    )
                }

                UIEvent.Next -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next)
                }

                UIEvent.Previous -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.Previous,
                    )
                }

                UIEvent.SkipToPrevious -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.SkipToPrevious,
                    )
                }

                UIEvent.Stop -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Stop)
                }

                is UIEvent.UpdateProgress -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.UpdateProgress(
                            uiEvent.newProgress,
                        ),
                    )
                }

                UIEvent.Repeat -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Repeat)
                }

                UIEvent.Shuffle -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Shuffle)
                }

                UIEvent.ToggleLike -> {
                    Logger.w(tag, "ToggleLike")
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.ToggleLike)
                }

                is UIEvent.UpdateVolume -> {
                    val newVolume = uiEvent.newVolume
                    // Apply to the player first: persisting to DataStore is a suspending disk write
                    // and must not sit between the user's gesture and the audible change.
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.UpdateVolume(newVolume))
                    dataStoreManager.setPlayerVolume(newVolume)
                }
            }
        }

    override fun onCleared() {
        Logger.w("Check onCleared", "onCleared")
    }

    fun getLocation() {
        viewModelScope.launch {
            regionCode = dataStoreManager.location.first()
            language = dataStoreManager.getString(SELECTED_LANGUAGE).first()
        }
    }

    private fun checkAllDownloadingLocalPlaylists() {
        viewModelScope.launch {
            localPlaylistRepository.getAllDownloadingLocalPlaylists().firstOrNull()?.forEach { playlist ->
                localPlaylistRepository.updateDownloadState(playlist.id, 0, successMessage = getString(Res.string.updated)).lastOrNull()
            }
        }
    }

    private fun checkAllDownloadingPlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllDownloadingPlaylist().firstOrNull()?.forEach { data ->
                when (data) {
                    is AlbumEntity -> {
                        albumRepository.updateAlbumDownloadState(data.browseId, 0)
                    }

                    is PlaylistEntity -> {
                        playlistRepository.updatePlaylistDownloadState(data.id, 0)
                    }

                    else -> {
                        // Skip
                    }
                }
            }
        }
    }

    private fun checkAllDownloadingSongs() {
        viewModelScope.launch {
            songRepository.getDownloadingSongs().firstOrNull()?.forEach { song ->
                songRepository.updateDownloadState(
                    song.videoId,
                    DownloadState.STATE_NOT_DOWNLOADED,
                )
            }
            songRepository.getPreparingSongs().firstOrNull()?.forEach { song ->
                songRepository.updateDownloadState(
                    song.videoId,
                    DownloadState.STATE_NOT_DOWNLOADED,
                )
            }
        }
    }

    private var smartDownloadsJob: Job? = null

    /**
     * Smart downloads, asked for by MainActivity.onStart: every time the app comes onto the screen,
     * the only moment Android lets it start the download service. Not from init: the Glance widgets
     * create this ViewModel with the app in the background. Opening the app many times a day stays
     * cheap: three settings are read, the pass itself runs at most once a day (the run is stamped
     * before it starts), and never twice at once.
     */
    fun runSmartDownloadsIfDue() {
        if (getPlatform() != Platform.Android) return
        if (smartDownloadsJob?.isActive == true) return
        smartDownloadsJob =
            viewModelScope.launch {
                if (dataStoreManager.smartDownloadsEnabled.first() != TRUE) return@launch
                if (dataStoreManager.localTrackingEnabled.first() != TRUE) return@launch
                val nowMillis = Clock.System.now().toEpochMilliseconds()
                val sinceLastRun = nowMillis - dataStoreManager.smartDownloadsLastRun.first()
                // A last run stamped in the future (the clock was moved back) does not block the pass.
                if (sinceLastRun in 0 until 1.days.inWholeMilliseconds) return@launch
                if (!isOnUnmeteredNetwork()) return@launch
                // Stamped first: a pass that fails is not retried at every launch, and a call arriving
                // while this one runs finds the day already done.
                dataStoreManager.setSmartDownloadsLastRun(nowMillis)
                songRepository.runSmartDownloads(dataStoreManager.smartDownloadsCount.first())
            }
    }

    private fun getFormat(mediaId: String?) {
        if (mediaId != _format.value?.videoId && !mediaId.isNullOrEmpty()) {
            _format.value = null
            _extractSource.value = streamRepository.getExtractSource(mediaId)
            getFormatFlowJob?.cancel()
            getFormatFlowJob =
                viewModelScope.launch {
                    streamRepository.getFormatFlow(mediaId).cancellable().collectLatest { f ->
                        Logger.d(tag, "Get format for $mediaId: itag ${f?.itag}, expires ${f?.expiredTime}")
                        if (f != null) {
                            _format.emit(f)
                        } else {
                            _format.emit(null)
                        }
                        // Re-read on every emission: the first one usually lands before the
                        // extractor has finished, so the source is only known on a later pass.
                        _extractSource.value = streamRepository.getExtractSource(mediaId)
                    }
                }
        }
    }

    fun getSongInfo(mediaId: String?) {
        songInfoJob?.cancel()
        songInfoJob =
            viewModelScope.launch {
                if (mediaId != null) {
                    songRepository.getSongInfo(mediaId).collect { song ->
                        _nowPlayingScreenData.update {
                            it.copy(
                                songInfoData = song,
                            )
                        }
                    }
                }
            }
    }

    private var _updateResponse = MutableStateFlow<UpdateData?>(null)
    val updateResponse: StateFlow<UpdateData?> = _updateResponse

    fun checkForUpdate() {
        // Raised before the coroutine starts, so Home's launch banner, which reads it right after
        // startup, can never miss a check that is already under way but not yet marked.
        _isCheckingUpdate.value = true
        viewModelScope.launch {
            dataStoreManager.putString(
                "CheckForUpdateAt",
                System.currentTimeMillis().toString(),
            )
            try {
                val update =
                    withContext(Dispatchers.IO) {
                        HttpClient(CIO).use { client ->
                            val httpResponse =
                                client.get("https://api.github.com/repos/jpcmpc123/Muzix/releases/latest") {
                                    header(HttpHeaders.Accept, "application/vnd.github+json")
                                    header(HttpHeaders.UserAgent, "Muzix")
                                }
                            if (!httpResponse.status.isSuccess()) {
                                error("GitHub release check failed: HTTP ${httpResponse.status.value}")
                            }
                            val obj = Json.parseToJsonElement(httpResponse.bodyAsText()).jsonObject
                            val tagName = obj["tag_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@use null
                            UpdateData(
                                tagName = tagName,
                                releaseTime = obj["published_at"]?.jsonPrimitive?.contentOrNull,
                                body = obj["body"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            )
                        }
                    }
                if (update != null) {
                    _updateResponse.value = update
                    showedUpdateDialog = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log("Check for update error: ${e.message}", LogLevel.WARN)
            } finally {
                _isCheckingUpdate.value = false
            }
        }.invokeOnCompletion {
            // Also for a check that cancels or throws: the launch banner waits for this flag, so it
            // must never stay raised.
            _isCheckingUpdate.value = false
        }
    }

    /**
     * [signingCerts]: SHA-256 hex of each certificate this APK is signed with. A failed fetch leaves
     * the app usable — it plays offline, and an unknown answer must not lock out our own users.
     */
    fun checkOfficialBuild(
        packageName: String,
        signingCerts: List<String>,
    ) {
        if (packageName !in Config.OFFICIAL_PACKAGE_NAMES) {
            _isOfficialBuild.value = false
            return
        }
        viewModelScope.launch {
            updateRepository.getFdroidSigningKeys().collect { response ->
                val keys = response.data
                // No certificate read at all is an unknown answer, and unknown never blocks.
                if (response is Resource.Success && keys != null && signingCerts.isNotEmpty() && keys.none { it in signingCerts }) {
                    _isOfficialBuild.value = false
                }
            }
        }
    }

    fun stopPlayer() {
        lyricsJob?.cancel()
        lyricsJob = null
        translationJob?.cancel()
        translationJob = null
        pendingYoutubeTranslatedLyrics = null
        _lyricsTranslationUiState.value = LyricsTranslationUiState()
        lyricsRequestedFor = null
        pendingLyricsRetryVideoId = null
        _nowPlayingScreenData.value = NowPlayingScreenData.initial()
        _nowPlayingState.value = null
        mediaPlayerHandler.resetSongAndQueue()
        onUIEvent(UIEvent.Stop)
    }

    fun createLocalPlaylist(title: String) {
        viewModelScope.launch {
            localPlaylistRepository
                .insertLocalPlaylist(
                    LocalPlaylistEntity(title = title),
                    getString(Res.string.added_local_playlist),
                ).lastOrNull()
            makeToast(getString(Res.string.added_local_playlist))
        }
    }

    private fun loadPlaylistOrAlbum(index: Int? = null) {
        mediaPlayerHandler.loadPlaylistOrAlbum(index)
    }

    private suspend fun updateLyrics(
        videoId: String,
        duration: Int, // 0 if translated lyrics
        inputLyrics: Lyrics?,
        isTranslatedLyrics: Boolean,
        lyricsProvider: LyricsProvider = LyricsProvider.SIMPMUSIC,
        persistToCache: Boolean = true,
        canContributeToSimpMusic: Boolean = false,
        expectedOriginalLyrics: Lyrics? = null,
        alreadyDecoded: Boolean = false,
    ): Lyrics? {
        if (_nowPlayingState.value?.songEntity?.videoId != videoId) return null

        if (inputLyrics == null) {
            _lyricsTranslationUiState.value = LyricsTranslationUiState()
            _nowPlayingScreenData.update {
                it.copy(
                    lyricsData = null,
                )
            }
            return null
        }

        val trackState = _nowPlayingState.value
        val trackTitle = trackState?.songEntity?.title ?: trackState?.mediaItem?.metadata?.title
        val trackArtists =
            trackState?.songEntity?.artistName?.joinToString(", ")
                ?: trackState?.mediaItem?.metadata?.artist

        val baselineForWarmup =
            if (isTranslatedLyrics) {
                expectedOriginalLyrics ?: _nowPlayingScreenData.value.lyricsData?.lyrics
            } else {
                null
            }

        val lyrics =
            withContext(Dispatchers.Default) {
                val decoded = if (alreadyDecoded) inputLyrics else decodeLyricsEntities(inputLyrics)
                if (!isTranslatedLyrics) {
                    val sanitized = LyricsHeaderSanitizer.sanitize(decoded, trackTitle, trackArtists)
                    if (!LyricsContentQualityGuard.isAcceptableContent(sanitized)) {
                        return@withContext null
                    }
                    val baseCleaned = LyricsContentQualityGuard.stripAsrSoundCueLines(sanitized)
                    val cleaned =
                        if (baseCleaned.syncType == "UNSYNCED") {
                            LyricsHeaderSanitizer.sanitizeTranslatedLyrics(baseCleaned)
                        } else {
                            baseCleaned
                        }
                    LyricsUiPrep.warmUp(cleaned)
                    cleaned
                } else {
                    val cleanedTranslated = LyricsHeaderSanitizer.sanitizeTranslatedLyrics(decoded)
                    if (baselineForWarmup != null) {
                        LyricsUiPrep.warmUpTranslation(baselineForWarmup, cleanedTranslated)
                    }
                    cleanedTranslated
                }
            } ?: return null

        // Re-check active track after switching dispatchers in case playback moved on mid-decode.
        if (_nowPlayingState.value?.songEntity?.videoId != videoId) return null

        if (isTranslatedLyrics) {
            val activeOriginal = _nowPlayingScreenData.value.lyricsData?.lyrics ?: return null
            if (expectedOriginalLyrics != null &&
                LyricsQuality.contentSignature(activeOriginal) != LyricsQuality.contentSignature(expectedOriginalLyrics)
            ) {
                Logger.d(tag, "Skipping stale translated lyrics for $videoId because original lyrics changed")
                return null
            }

            if (lyricsProvider != LyricsProvider.AI && lyricsProvider != LyricsProvider.TRANSLATOR) {
                // Skip sync validation for machine/AI translations — timestamps are copied programmatically 1-to-1.
                val syncBaseline = expectedOriginalLyrics ?: activeOriginal
                if (LyricsTranslationSyncValidator.isOutOfSync(syncBaseline, lyrics)) {
                    Logger.w(tag, "Translated lyrics out of sync for $videoId; discarding")
                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                it.lyricsData?.copy(
                                    translatedLyrics = null,
                                ),
                        )
                    }

                    viewModelScope.launch {
                        lyricsCanvasRepository.removeTranslatedLyrics(
                            videoId,
                            dataStoreManager.translationLanguage.first(),
                        )
                        log("Removed out-of-sync translated lyrics for $videoId")
                        val simpMusicLyricsId = lyrics.simpMusicLyrics?.id
                        if (lyricsProvider == LyricsProvider.SIMPMUSIC && !simpMusicLyricsId.isNullOrEmpty()) {
                            viewModelScope.launch {
                                lyricsCanvasRepository
                                    .voteSimpMusicTranslatedLyrics(
                                        translatedLyricsId = simpMusicLyricsId,
                                        false,
                                    ).collectLatest {
                                        when (it) {
                                            is Resource.Error -> {
                                                Logger.w(tag, "Vote SimpMusic Translated Lyrics Error ${it.message}")
                                            }

                                            is Resource.Success -> {
                                                Logger.d(tag, "Vote SimpMusic Translated Lyrics Success")
                                            }
                                        }
                                    }
                            }
                        }
                    }
                    return null
                }
            }
        }

        val shouldSendLyricsToSimpMusic =
            canContributeToSimpMusic &&
                lyricsProvider != LyricsProvider.SIMPMUSIC &&
                lyricsProvider != LyricsProvider.SPOTIFY &&
                lyricsProvider != LyricsProvider.LETRAS &&
                lyricsProvider != LyricsProvider.TRANSLATOR &&
                dataStoreManager.helpBuildLyricsDatabase.first() == TRUE
        if (_nowPlayingState.value?.songEntity?.videoId != videoId) return null

        val track = _nowPlayingState.value?.track
        when (isTranslatedLyrics) {
            true -> {
                if (lyricsProvider == LyricsProvider.SIMPMUSIC) {
                    _translatedVoteState.value =
                        VoteData(
                            id = lyrics.simpMusicLyrics?.id ?: "",
                            vote = lyrics.simpMusicLyrics?.vote ?: 0,
                            state = VoteState.Idle,
                        )
                }
                _nowPlayingScreenData.update {
                    it.copy(
                        lyricsData =
                            it.lyricsData?.copy(
                                translatedLyrics = lyrics to lyricsProvider,
                            ),
                    )
                }
                if (shouldSendLyricsToSimpMusic && track != null) {
                    viewModelScope.launch {
                        lyricsCanvasRepository
                            .insertSimpMusicTranslatedLyrics(
                                dataStoreManager,
                                track,
                                lyrics,
                                dataStoreManager.translationLanguage.first(),
                            ).collect {
                                when (it) {
                                    is Resource.Error -> {
                                        log("Insert SimpMusic Translated Lyrics Error ${it.message}", LogLevel.WARN)
                                    }

                                    is Resource.Success -> {
                                        log("Insert SimpMusic Translated Lyrics Success")
                                    }
                                }
                            }
                    }
                }
            }

            false -> {
                if (lyricsProvider == LyricsProvider.SIMPMUSIC) {
                    _lyricsVoteState.value =
                        VoteData(
                            id = lyrics.simpMusicLyrics?.id ?: "",
                            vote = lyrics.simpMusicLyrics?.vote ?: 0,
                            state = VoteState.Idle,
                        )
                } else {
                    resetLyricsVoteState()
                }
                _nowPlayingScreenData.update {
                    it.copy(
                        lyricsNotFound = false,
                        lyricsData =
                            NowPlayingScreenData.LyricsData(
                                lyrics = lyrics,
                                lyricsProvider = lyricsProvider,
                            ),
                    )
                }
                if (persistToCache) {
                    // Save fresh non-cache lyrics to database once.
                    viewModelScope.launch {
                        lyricsCanvasRepository.insertLyrics(
                            LyricsEntity(
                                videoId = videoId,
                                error = false,
                                lines = lyrics.lines,
                                syncType = lyrics.syncType,
                            ),
                        )
                    }
                }
                if (shouldSendLyricsToSimpMusic && track != null) {
                    viewModelScope.launch {
                        lyricsCanvasRepository
                            .insertSimpMusicLyrics(
                                dataStoreManager,
                                track,
                                duration,
                                lyrics,
                            ).collect {
                                when (it) {
                                    is Resource.Error -> {
                                        Logger.w(tag, "Insert SimpMusic Lyrics Error ${it.message}")
                                    }

                                    is Resource.Success -> {
                                        Logger.d(tag, "Insert SimpMusic Lyrics Success")
                                    }
                                }
                            }
                    }
                }
            }
        }
        return lyrics
    }

    private fun startLyricsResolution(
        song: SongEntity,
        artistHint: String?,
        durationSec: Int,
        preferCache: Boolean,
        forceRefresh: Boolean,
    ) {
        // A live broadcast has nothing to sync lyrics to — the "duration" mpv reports for it is only
        // its seek window — so no provider is asked. Every caller comes through here.
        if (LiveStreamRegistry.isLive(song.videoId)) return

        lyricsJob?.cancel()
        translationJob?.cancel()
        translationJob = null
        // A fetch in flight is not "no lyrics": switching the provider re-fetches the same song, and
        // Add lyrics offered meanwhile could be saved over the lyrics the new provider finds.
        _nowPlayingScreenData.update { it.copy(lyricsNotFound = false) }
        resetLyricsVoteState()

        lyricsJob =
            viewModelScope.launch {
                try {
                    if (_nowPlayingState.value?.songEntity?.videoId != song.videoId) return@launch
                    ensureUserProtectedSignaturesLoaded()

                    log("Resolve lyrics for ${song.videoId} (preferCache=$preferCache, forceRefresh=$forceRefresh)", LogLevel.WARN)
                    val validArtists =
                        song.artistName
                            .orEmpty()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() && !it.contains("Various Artists", ignoreCase = true) }
                    val artist =
                        if (validArtists.isNotEmpty()) {
                            validArtists.joinToString(", ")
                        } else {
                            artistHint
                                ?: mediaPlayerHandler.nowPlaying.first()?.metadata?.artist
                                ?: ""
                        }

                    val preferredSource = dataStoreManager.lyricsProvider.first().toLyricsSource()
                    val allowSpotify = dataStoreManager.spotifyLyrics.first() == TRUE
                    val allowUpgradeScan = forceRefresh || song.videoId !in upgradedVideoIds
                    val request =
                        LyricsLookupRequest(
                            videoId = song.videoId,
                            title = song.title,
                            artist = artist,
                            durationSeconds = durationSec.takeIf { it > 0 },
                            preferredSource = preferredSource,
                            allowSpotify = allowSpotify,
                            preferCache = preferCache,
                            allowUpgradeScan = allowUpgradeScan,
                            userProtectedSignatures = userProtectedSignatures.toSet(),
                        )

                    var hadTransientExhaustion = false
                    lyricsCanvasRepository.resolveLyrics(request).collect { event ->
                        when (event) {
                            is LyricsResolutionEvent.Found -> {
                                val resolution = event.resolution
                                if (_nowPlayingState.value?.songEntity?.videoId != resolution.videoId) {
                                    return@collect
                                }
                                val currentUiLyrics = _nowPlayingScreenData.value.lyricsData?.lyrics
                                if (!forceRefresh &&
                                    currentUiLyrics != null &&
                                    LyricsQuality.contentSignature(currentUiLyrics) in userProtectedSignatures
                                ) {
                                    return@collect
                                }

                                val uiProvider = resolution.source.toUiProvider()
                                val appliedLyrics =
                                    updateLyrics(
                                        videoId = resolution.videoId,
                                        duration = durationSec,
                                        inputLyrics = resolution.lyrics,
                                        isTranslatedLyrics = false,
                                        lyricsProvider = uiProvider,
                                        persistToCache = !resolution.fromCache,
                                        canContributeToSimpMusic =
                                            !resolution.fromCache &&
                                                resolution.simpMusicNotFoundObserved &&
                                                resolution.source != LyricsSource.SIMPMUSIC &&
                                                resolution.source != LyricsSource.SPOTIFY &&
                                                resolution.source != LyricsSource.LETRAS &&
                                                resolution.source != LyricsSource.OFFLINE,
                                    )
                                if (appliedLyrics != null) {
                                    pendingLyricsRetryVideoId = null
                                    pendingYoutubeTranslatedLyrics =
                                        resolution.youtubeTranslatedLyrics?.let { resolution.videoId to it }
                                    val sameLanguage = isLyricsInTargetLanguage(appliedLyrics)
                                    _lyricsTranslationUiState.value =
                                        LyricsTranslationUiState(isSameLanguage = sameLanguage)
                                    if (!sameLanguage) {
                                        scheduleAutoTranslationIfEnabled(
                                            videoId = resolution.videoId,
                                            originalLyrics = appliedLyrics,
                                        )
                                    }
                                }
                            }

                            is LyricsResolutionEvent.Exhausted -> {
                                if (event.confirmedInstrumental || event.cacheInvalidated) {
                                    if (event.confirmedInstrumental) {
                                        pendingLyricsRetryVideoId = null
                                    } else if (!event.definitiveNotFound) {
                                        hadTransientExhaustion = true
                                        pendingLyricsRetryVideoId = event.videoId
                                    } else {
                                        pendingLyricsRetryVideoId = null
                                    }
                                    val currentLyrics = _nowPlayingScreenData.value.lyricsData?.lyrics
                                    val isCurrentProtected =
                                        currentLyrics != null &&
                                            LyricsQuality.contentSignature(currentLyrics) in userProtectedSignatures
                                    if (!isCurrentProtected && _nowPlayingState.value?.songEntity?.videoId == event.videoId) {
                                        pendingYoutubeTranslatedLyrics = null
                                        translationJob?.cancel()
                                        translationJob = null
                                        _lyricsTranslationUiState.value = LyricsTranslationUiState()
                                        resetLyricsVoteState()
                                        _nowPlayingScreenData.update {
                                            it.copy(
                                                lyricsData = null,
                                                lyricsNotFound = true,
                                            )
                                        }
                                        viewModelScope.launch {
                                            lyricsCanvasRepository.removeLyrics(event.videoId)
                                        }
                                    }
                                } else {
                                    if (!event.definitiveNotFound) {
                                        hadTransientExhaustion = true
                                        pendingLyricsRetryVideoId = event.videoId
                                    } else {
                                        pendingLyricsRetryVideoId = null
                                    }
                                    // The end of every provider and of the offline cache: the one place the app
                                    // knows this song has no lyrics, which is what offers Add lyrics.
                                    if (_nowPlayingState.value?.songEntity?.videoId == event.videoId &&
                                        _nowPlayingScreenData.value.lyricsData == null
                                    ) {
                                        _nowPlayingScreenData.update { it.copy(lyricsNotFound = true) }
                                    }
                                }
                            }
                        }
                    }

                    if (allowUpgradeScan && !hadTransientExhaustion) {
                        upgradedVideoIds.add(song.videoId)
                    }
                } catch (e: CancellationException) {
                    throw e
                }
            }
    }

    /**
     * Toggles translated lyrics on/off for the currently playing song.
     * - If currently loading: cancels the in-flight translation immediately.
     * - If currently visible: hides in 0 ms while keeping the translated lyrics cached in memory.
     * - If already loaded in memory: reveals in 0 ms without network or disk I/O.
     * - Otherwise: runs the 4-layer quality cascade (Room cache -> human community/YouTube ->
     *   AI if configured -> free Microsoft Edge neural translator in pt-BR -> Google fallback).
     */
    fun toggleLyricsTranslation() {
        val currentState = _lyricsTranslationUiState.value
        if (currentState.isLoading) {
            translationJob?.cancel()
            translationJob = null
            _lyricsTranslationUiState.update { it.copy(isLoading = false) }
            return
        }
        if (currentState.isVisible) {
            _lyricsTranslationUiState.update { it.copy(isVisible = false) }
            return
        }
        val videoId = _nowPlayingState.value?.songEntity?.videoId ?: return
        val lyricsData = _nowPlayingScreenData.value.lyricsData ?: return
        if (lyricsData.translatedLyrics != null) {
            _lyricsTranslationUiState.update { it.copy(isVisible = true, isLoading = false) }
            return
        }
        translationJob?.cancel()
        translationJob =
            viewModelScope.launch {
                loadLyricsTranslation(
                    videoId = videoId,
                    originalLyrics = lyricsData.lyrics,
                    showToasts = true,
                    bypassCache = false,
                )
            }
    }

    /**
     * Forces a fresh translation pass for the current track, clearing the cached Room v2 entry
     * (useful on long-press of the translate button).
     */
    fun retranslateLyrics() {
        val videoId = _nowPlayingState.value?.songEntity?.videoId ?: return
        val lyricsData = _nowPlayingScreenData.value.lyricsData ?: return
        translationJob?.cancel()
        _nowPlayingScreenData.update {
            it.copy(lyricsData = it.lyricsData?.copy(translatedLyrics = null))
        }
        _lyricsTranslationUiState.value = LyricsTranslationUiState(isVisible = false, isLoading = true)
        translationJob =
            viewModelScope.launch {
                makeToast(getString(Res.string.retranslating_lyrics))
                loadLyricsTranslation(
                    videoId = videoId,
                    originalLyrics = lyricsData.lyrics,
                    showToasts = true,
                    bypassCache = true,
                )
            }
    }

    private fun scheduleAutoTranslationIfEnabled(
        videoId: String,
        originalLyrics: Lyrics,
    ) {
        translationJob?.cancel()
        translationJob =
            viewModelScope.launch {
                if (dataStoreManager.autoTranslateLyrics.first() != TRUE) {
                    return@launch
                }
                // Short debounce avoids firing translation requests while skipping rapidly through tracks.
                delay(350L)
                if (_nowPlayingState.value?.songEntity?.videoId != videoId) {
                    return@launch
                }
                loadLyricsTranslation(
                    videoId = videoId,
                    originalLyrics = originalLyrics,
                    showToasts = false,
                    bypassCache = false,
                )
            }
    }

    private fun decodeLyricsEntities(inputLyrics: Lyrics): Lyrics {
        val lines = inputLyrics.lines ?: return inputLyrics
        var changed = false
        val decodedLines =
            lines.map { line ->
                val decodedWords = decodeHtmlEntities(line.words)
                if (decodedWords !== line.words) {
                    changed = true
                    line.copy(words = decodedWords)
                } else {
                    line
                }
            }
        return if (changed) inputLyrics.copy(lines = decodedLines) else inputLyrics
    }

    private suspend fun ensureUserProtectedSignaturesLoaded() {
        if (userProtectedSignaturesLoaded) return
        val stored = dataStoreManager.getString(USER_PROTECTED_SIGNATURES_KEY).firstOrNull()
        stored
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.forEach { userProtectedSignatures.add(it) }
        userProtectedSignaturesLoaded = true
    }

    private suspend fun recordUserProtectedSignatures(vararg lyricsVariants: Lyrics) {
        ensureUserProtectedSignaturesLoaded()
        var changed = false
        lyricsVariants.forEach { variant ->
            if (LyricsQuality.level(variant) > LyricsQuality.LEVEL_INVALID) {
                val signature = LyricsQuality.contentSignature(variant)
                if (userProtectedSignatures.add(signature)) {
                    changed = true
                }
            }
        }
        if (!changed) return
        while (userProtectedSignatures.size > MAX_PERSISTED_PROTECTED_SIGNATURES) {
            val oldest = userProtectedSignatures.firstOrNull() ?: break
            userProtectedSignatures.remove(oldest)
        }
        dataStoreManager.putString(
            USER_PROTECTED_SIGNATURES_KEY,
            userProtectedSignatures.joinToString(","),
        )
    }

    private suspend fun isLyricsInTargetLanguage(lyrics: Lyrics): Boolean {
        val targetLang =
            dataStoreManager.translationLanguage
                .first()
                .ifBlank { dataStoreManager.language.first() }
                .ifBlank { "pt" }
        return LyricsLanguageDetector.isSameLanguage(lyrics, targetLang)
    }

    private fun String.toLyricsSource(): LyricsSource =
        when (this) {
            DataStoreManager.YOUTUBE -> LyricsSource.YOUTUBE
            DataStoreManager.LRCLIB -> LyricsSource.LRCLIB
            DataStoreManager.BETTER_LYRICS -> LyricsSource.BETTER_LYRICS
            else -> LyricsSource.SIMPMUSIC
        }

    private fun LyricsSource.toUiProvider(): LyricsProvider =
        when (this) {
            LyricsSource.SIMPMUSIC -> LyricsProvider.SIMPMUSIC
            LyricsSource.YOUTUBE -> LyricsProvider.YOUTUBE
            LyricsSource.SPOTIFY -> LyricsProvider.SPOTIFY
            LyricsSource.LRCLIB -> LyricsProvider.LRCLIB
            LyricsSource.BETTER_LYRICS -> LyricsProvider.BETTER_LYRICS
            LyricsSource.LETRAS -> LyricsProvider.LETRAS
            LyricsSource.OFFLINE -> LyricsProvider.OFFLINE
        }

    private suspend fun loadLyricsTranslation(
        videoId: String,
        originalLyrics: Lyrics,
        showToasts: Boolean,
        bypassCache: Boolean = false,
    ) {
        _lyricsTranslationUiState.update { it.copy(isLoading = true) }
        try {
            val targetLanguage = dataStoreManager.translationLanguage.first().ifBlank { "pt" }
            val cacheLanguageKey = "${targetLanguage}#v6"

            if (bypassCache) {
                lyricsCanvasRepository.removeTranslatedLyrics(videoId, targetLanguage)
                lyricsCanvasRepository.removeTranslatedLyrics(videoId, "${targetLanguage}#v2")
                lyricsCanvasRepository.removeTranslatedLyrics(videoId, "${targetLanguage}#v3")
                lyricsCanvasRepository.removeTranslatedLyrics(videoId, "${targetLanguage}#v4")
                lyricsCanvasRepository.removeTranslatedLyrics(videoId, "${targetLanguage}#v5")
                lyricsCanvasRepository.removeTranslatedLyrics(videoId, cacheLanguageKey)
            } else {
                // 0. Local Room cache (v6 — auto-upgrades 1-to-N Letras verse merges like Liz).
                val saved =
                    lyricsCanvasRepository
                        .getSavedTranslatedLyrics(videoId, cacheLanguageKey)
                        .firstOrNull()
                        ?.toLyrics()
                if (saved != null && !saved.lines.isNullOrEmpty()) {
                    if (!LyricsTranslationSyncValidator.isOutOfSync(originalLyrics, saved)) {
                        val applied =
                            updateLyrics(
                                videoId = videoId,
                                duration = 0,
                                inputLyrics = saved,
                                isTranslatedLyrics = true,
                                lyricsProvider = LyricsProvider.TRANSLATOR,
                                persistToCache = false,
                                canContributeToSimpMusic = false,
                                expectedOriginalLyrics = originalLyrics,
                            )
                        if (applied != null) {
                            _lyricsTranslationUiState.update { it.copy(isVisible = true) }
                            return
                        }
                    } else {
                        lyricsCanvasRepository.removeTranslatedLyrics(videoId, cacheLanguageKey)
                    }
                }
            }

            // 1A. Pre-fetched YouTube human caption track (if already resolved with YouTube lyrics).
            val ytTranslated =
                pendingYoutubeTranslatedLyrics
                    ?.takeIf { it.first == videoId }
                    ?.second
            if (ytTranslated != null && !LyricsTranslationSyncValidator.isOutOfSync(originalLyrics, ytTranslated)) {
                val applied =
                    updateLyrics(
                        videoId = videoId,
                        duration = 0,
                        inputLyrics = ytTranslated,
                        isTranslatedLyrics = true,
                        lyricsProvider = LyricsProvider.YOUTUBE,
                        persistToCache = false,
                        canContributeToSimpMusic = false,
                        expectedOriginalLyrics = originalLyrics,
                    )
                if (applied != null) {
                    _lyricsTranslationUiState.update { it.copy(isVisible = true, sourceEngine = "youtube") }
                    return
                }
            }

            // 1B. Community human translation from SimpMusic Lyrics DB (for ANY lyrics provider).
            val simpCode = targetLanguage.substringBefore('-').lowercase()
            if (simpCode.length == 2 && trySimpMusicTranslatedLyrics(videoId, originalLyrics, simpCode, cacheLanguageKey)) {
                _lyricsTranslationUiState.update { it.copy(isVisible = true, sourceEngine = "simpmusic") }
                return
            }

            if (_nowPlayingState.value?.songEntity?.videoId != videoId) return

            // 1C. Human Brazilian Portuguese translations from Letras.mus.br -> Vagalume fallback.
            val trackState = _nowPlayingState.value
            val songTitle = trackState?.songEntity?.title ?: trackState?.mediaItem?.metadata?.title.orEmpty()
            val artistName =
                trackState?.songEntity?.artistName?.firstOrNull()
                    ?: trackState?.mediaItem?.metadata?.artist.orEmpty()
            if (simpCode == "pt" && songTitle.isNotBlank() && artistName.isNotBlank()) {
                val letrasRes =
                    lyricsCanvasRepository
                        .getLetrasTranslatedLyrics(songTitle, artistName, originalLyrics, targetLanguage)
                        .firstOrNull()
                        ?.data
                if (letrasRes != null &&
                    applyHumanPortugueseTranslationResult(
                        videoId = videoId,
                        originalLyrics = originalLyrics,
                        translated = letrasRes.lyrics,
                        cacheLanguageKey = cacheLanguageKey,
                        sourceEngine = letrasRes.sourceEngine ?: "letras",
                    )
                ) {
                    return
                }

                if (_nowPlayingState.value?.songEntity?.videoId != videoId) return

                val vagalumeRes =
                    lyricsCanvasRepository
                        .getVagalumeTranslatedLyrics(songTitle, artistName, originalLyrics, targetLanguage)
                        .firstOrNull()
                        ?.data
                if (vagalumeRes != null &&
                    applyHumanPortugueseTranslationResult(
                        videoId = videoId,
                        originalLyrics = originalLyrics,
                        translated = vagalumeRes.lyrics,
                        cacheLanguageKey = cacheLanguageKey,
                        sourceEngine = vagalumeRes.sourceEngine ?: "vagalume",
                    )
                ) {
                    return
                }
            }

            if (_nowPlayingState.value?.songEntity?.videoId != videoId) return

            // 2. Optional AI translation if user explicitly enabled AI mode and provided an API key.
            val translationMode = dataStoreManager.lyricsTranslationMode.first()
            val aiEnabled =
                (translationMode == DataStoreManager.LYRICS_TRANSLATION_MODE_AI || dataStoreManager.useAITranslation.first() == TRUE) &&
                    dataStoreManager.aiApiKey.first().isNotEmpty()
            if (aiEnabled && tryAiTranslatedLyrics(videoId, originalLyrics, cacheLanguageKey)) {
                _lyricsTranslationUiState.update { it.copy(isVisible = true, sourceEngine = "ai") }
                return
            }

            if (_nowPlayingState.value?.songEntity?.videoId != videoId) return

            // 3. Built-in free neural translator (Microsoft Edge public pt-BR -> Google stanza HTML fallback).
            val freeResponse =
                lyricsCanvasRepository
                    .getFreeTranslatedLyrics(originalLyrics, targetLanguage)
                    .firstOrNull()
            when (freeResponse) {
                is Resource.Success -> {
                    when (val result = freeResponse.data) {
                        is LyricsTranslationResult.AlreadyInTargetLanguage -> {
                            _lyricsTranslationUiState.update {
                                it.copy(isSameLanguage = true, isVisible = false)
                            }
                            if (showToasts) {
                                makeToast(getString(Res.string.lyrics_already_in_language))
                            }
                        }

                        is LyricsTranslationResult.Translated -> {
                            val translated = result.lyrics
                            lyricsCanvasRepository.insertTranslatedLyrics(
                                TranslatedLyricsEntity(
                                    videoId = videoId,
                                    language = cacheLanguageKey,
                                    error = false,
                                    lines = translated.lines,
                                    syncType = translated.syncType,
                                ),
                            )
                            val applied =
                                updateLyrics(
                                    videoId = videoId,
                                    duration = 0,
                                    inputLyrics = translated,
                                    isTranslatedLyrics = true,
                                    lyricsProvider = LyricsProvider.TRANSLATOR,
                                    persistToCache = false,
                                    canContributeToSimpMusic = false,
                                    expectedOriginalLyrics = originalLyrics,
                                )
                            if (applied != null) {
                                _lyricsTranslationUiState.update {
                                    it.copy(isVisible = true, sourceEngine = result.sourceEngine ?: "microsoft")
                                }
                            }
                        }

                        null -> {
                            if (showToasts) {
                                makeToast(getString(Res.string.translation_failed))
                            }
                        }
                    }
                }

                else -> {
                    Logger.w(tag, "Free translation error for $videoId: ${freeResponse?.message}")
                    if (showToasts) {
                        makeToast(getString(Res.string.translation_failed))
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Logger.w(tag, "Translation failed for $videoId: ${t.message}")
            if (showToasts) {
                makeToast(getString(Res.string.translation_failed))
            }
        } finally {
            _lyricsTranslationUiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun applyHumanPortugueseTranslationResult(
        videoId: String,
        originalLyrics: Lyrics,
        translated: Lyrics,
        cacheLanguageKey: String,
        sourceEngine: String,
    ): Boolean {
        if (_nowPlayingState.value?.songEntity?.videoId != videoId) return false
        lyricsCanvasRepository.insertTranslatedLyrics(
            TranslatedLyricsEntity(
                videoId = videoId,
                language = cacheLanguageKey,
                error = false,
                lines = translated.lines,
                syncType = translated.syncType,
            ),
        )
        val applied =
            updateLyrics(
                videoId = videoId,
                duration = 0,
                inputLyrics = translated,
                isTranslatedLyrics = true,
                lyricsProvider = LyricsProvider.TRANSLATOR,
                persistToCache = false,
                canContributeToSimpMusic = false,
                expectedOriginalLyrics = originalLyrics,
            ) ?: return false
        _lyricsTranslationUiState.update {
            it.copy(isVisible = true, sourceEngine = sourceEngine)
        }
        return true
    }

    private suspend fun trySimpMusicTranslatedLyrics(
        videoId: String,
        originalLyrics: Lyrics,
        simpLanguageCode: String,
        cacheLanguageKey: String,
    ): Boolean {
        val response =
            lyricsCanvasRepository
                .getSimpMusicTranslatedLyrics(videoId, simpLanguageCode)
                .firstOrNull()
        val data = response?.data
        if (response !is Resource.Success || data == null) {
            return false
        }
        if (data.syncType == "RICH_SYNCED") {
            Logger.w(tag, "SimpMusic translated lyrics are RICH_SYNCED; skipping")
            val simpMusicLyricsId = data.simpMusicLyrics?.id
            if (!simpMusicLyricsId.isNullOrEmpty()) {
                viewModelScope.launch {
                    lyricsCanvasRepository
                        .voteSimpMusicTranslatedLyrics(simpMusicLyricsId, false)
                        .collectLatest {}
                }
            }
            return false
        }
        if (LyricsTranslationSyncValidator.isOutOfSync(originalLyrics, data)) {
            Logger.w(tag, "SimpMusic translated lyrics out of sync for $videoId; falling through")
            val simpMusicLyricsId = data.simpMusicLyrics?.id
            if (!simpMusicLyricsId.isNullOrEmpty()) {
                viewModelScope.launch {
                    lyricsCanvasRepository
                        .voteSimpMusicTranslatedLyrics(simpMusicLyricsId, false)
                        .collectLatest {}
                }
            }
            return false
        }
        lyricsCanvasRepository.insertTranslatedLyrics(
            TranslatedLyricsEntity(
                videoId = videoId,
                language = cacheLanguageKey,
                error = false,
                lines = data.lines,
                syncType = data.syncType,
            ),
        )
        return updateLyrics(
            videoId = videoId,
            duration = 0,
            inputLyrics = data,
            isTranslatedLyrics = true,
            lyricsProvider = LyricsProvider.SIMPMUSIC,
            persistToCache = false,
            canContributeToSimpMusic = false,
            expectedOriginalLyrics = originalLyrics,
        ) != null
    }

    private suspend fun tryAiTranslatedLyrics(
        videoId: String,
        lyrics: Lyrics,
        targetLanguage: String,
    ): Boolean {
        Logger.d(tag, "Get AI Translation Lyrics for $videoId")
        val lyricsForAi =
            if (lyrics.syncType == "RICH_SYNCED") {
                lyrics.toSyncedLyrics()
            } else {
                lyrics
            }
        val response =
            lyricsCanvasRepository
                .getAITranslationLyrics(lyricsForAi, targetLanguage)
                .firstOrNull()
        val data = response?.data
        if (response !is Resource.Success || data == null) {
            Logger.w(tag, "Get AI Translate Lyrics Error: ${response?.message}")
            return false
        }
        Logger.d(tag, "Get AI Translate Lyrics Success")
        lyricsCanvasRepository.insertTranslatedLyrics(
            TranslatedLyricsEntity(
                videoId = videoId,
                language = targetLanguage,
                error = false,
                lines = data.lines,
                syncType = data.syncType,
            ),
        )
        return updateLyrics(
            videoId = videoId,
            duration = 0,
            inputLyrics = data,
            isTranslatedLyrics = true,
            lyricsProvider = LyricsProvider.AI,
            persistToCache = false,
            canContributeToSimpMusic = true,
            expectedOriginalLyrics = lyrics,
        ) != null
    }

    private fun setLyricsProvider() {
        val state = nowPlayingState.value ?: return
        val songEntity = state.songEntity ?: return
        if (LiveStreamRegistry.isLive(songEntity.videoId)) return
        val durationSec =
            (timeline.value.total / 1000L).toInt().takeIf { it > 0 }
                ?: songEntity.durationSeconds
        lyricsRequestedFor = songEntity.videoId
        startLyricsResolution(
            song = songEntity,
            artistHint = state.mediaItem.metadata.artist,
            durationSec = durationSec,
            preferCache = false,
            forceRefresh = true,
        )
    }

    private var _recreateActivity: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recreateActivity: StateFlow<Boolean> = _recreateActivity

    fun activityRecreate() {
        _recreateActivity.value = true
    }

    fun activityRecreateDone() {
        _recreateActivity.value = false
    }

    fun addListToQueue(listTrack: ArrayList<Track>) {
        viewModelScope.launch {
            // A radio, a mix or an Endless queue has no end to append to, so a single song (the swipe
            // on a song row) goes right after the current one instead.
            val endlessQueue = dataStoreManager.endlessQueue.first() == TRUE
            if (listTrack.size == 1 && mediaPlayerHandler.queueData.value.addToQueueBlock(endlessQueue) != null) {
                mediaPlayerHandler.playNext(listTrack.first())
                makeToast(getString(Res.string.play_next))
            } else {
                mediaPlayerHandler.loadMoreCatalog(listTrack)
                makeToast(getString(Res.string.added_to_queue))
            }
        }
    }

    fun addToYouTubeLiked() {
        viewModelScope.launch {
            val videoId = mediaPlayerHandler.nowPlaying.first()?.mediaId
            if (videoId != null) {
                val like = likeStatus.value == YouTubeLikeStatus.LIKE
                if (!like) {
                    songRepository
                        .addToYouTubeLiked(
                            mediaPlayerHandler.nowPlaying.first()?.mediaId,
                        ).collect { response ->
                            if (response == 200) {
                                makeToast(getString(Res.string.added_to_youtube_liked))
                                getLikeStatus(videoId)
                            } else {
                                makeToast(getString(Res.string.error))
                            }
                        }
                } else {
                    songRepository
                        .removeFromYouTubeLiked(
                            mediaPlayerHandler.nowPlaying.first()?.mediaId,
                        ).collect {
                            if (it == 200) {
                                makeToast(getString(Res.string.removed_from_youtube_liked))
                                getLikeStatus(videoId)
                            } else {
                                makeToast(getString(Res.string.error))
                            }
                        }
                }
            }
        }
    }

    /**
     * Dislikes [videoId], the track the sheet was opened for. While the request is out the action
     * reads "Disliking…" and cannot be pressed again; a failure puts it back as it was.
     */
    fun addToYouTubeDisliked(videoId: String) {
        if (videoId.isEmpty() || _dislikingVideoId.value == videoId) return
        _dislikingVideoId.value = videoId
        viewModelScope.launch {
            try {
                songRepository.addToYouTubeDisliked(videoId).collect { response ->
                    if (response == 200) {
                        // The reply can arrive after the player moved on; it belongs to the
                        // track it was sent for, so it only shows if that track is still playing.
                        if (mediaPlayerHandler.nowPlaying.value?.mediaId == videoId) {
                            _likeStatus.value = YouTubeLikeStatus.DISLIKE
                        }
                        makeToast(getString(Res.string.disliked))
                    } else {
                        makeToast(getString(Res.string.error))
                    }
                }
            } finally {
                _dislikingVideoId.compareAndSet(videoId, null)
            }
        }
    }

    fun getEnableLiquidGlass() = dataStoreManager.enableLiquidGlass

    fun getLocalTrackingEnabled() = dataStoreManager.localTrackingEnabled

    // Drives the Mix for you tab: YouTube hands an anonymous session no mixes at all.
    fun getYouTubeLoggedIn() = dataStoreManager.loggedIn

    fun getThemeMode() = dataStoreManager.themeMode

    fun getThemeColorSource() = dataStoreManager.themeColorSource

    fun getCustomThemeColor() = dataStoreManager.customThemeColor

    fun getNowPlayingStyle() = dataStoreManager.nowPlayingStyle

    fun getLyricsStyle() = dataStoreManager.lyricsStyle

    fun getLyricsOffsetMs() = dataStoreManager.lyricsOffsetMs

    fun setLyricsOffsetMs(offsetMs: Int) {
        viewModelScope.launch {
            dataStoreManager.setLyricsOffsetMs(offsetMs)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            dataStoreManager.setThemeMode(mode)
        }
    }

    fun setThemeColorSource(source: String) {
        viewModelScope.launch {
            dataStoreManager.setThemeColorSource(source)
        }
    }

    fun setCustomThemeColor(argbHex: String) {
        viewModelScope.launch {
            dataStoreManager.setCustomThemeColor(argbHex)
        }
    }

    fun setNowPlayingStyle(style: String) {
        viewModelScope.launch {
            dataStoreManager.setNowPlayingStyle(style)
        }
    }

    fun setLyricsStyle(style: String) {
        viewModelScope.launch {
            dataStoreManager.setLyricsStyle(style)
        }
    }

    fun getRomanizationLanguages() = dataStoreManager.romanizationLanguages

    fun setRomanizationLanguages(languages: Set<RomanizationLanguage>) {
        viewModelScope.launch {
            // Sorted by name so the stored string is stable: an unsorted Set writes a different
            // value for the same selection depending on iteration order, which makes the DataStore
            // flow emit on a change that did not happen.
            dataStoreManager.setRomanizationLanguages(languages.map { it.name }.sorted().joinToString(","))
        }
    }

    private val _reloadDestination: MutableStateFlow<KClass<*>?> = MutableStateFlow(null)
    val reloadDestination: StateFlow<KClass<*>?> = _reloadDestination.asStateFlow()

    fun reloadDestination(destination: KClass<*>) {
        _reloadDestination.value = destination
    }

    fun reloadDestinationDone() {
        _reloadDestination.value = null
    }

    fun shouldCheckForUpdate(): Boolean = runBlocking { dataStoreManager.autoCheckForUpdates.first() == TRUE }

    fun checkForUpdateIfEnabled() {
        viewModelScope.launch {
            if (dataStoreManager.autoCheckForUpdates.first() == TRUE) {
                checkForUpdate()
            }
        }
    }

    private var _downloadFileProgress = MutableStateFlow<DownloadProgress>(DownloadProgress.INIT)
    val downloadFileProgress: StateFlow<DownloadProgress> get() = _downloadFileProgress

    fun downloadFile(bitmap: ImageBitmap) {
        val fileName =
            "${nowPlayingScreenData.value.nowPlayingTitle} - ${nowPlayingScreenData.value.artistName}"
                .replace(Regex("""[|\\?*<":>]"""), "")
                .replace(" ", "_")
        val path =
            "${getDownloadFolderPath()}/$fileName"
        viewModelScope.launch {
            nowPlayingState.value?.track?.let { track ->
                val bytesArray = bitmap.toByteArray()
                try {
                    val fileOutputStream = FileOutputStream("$path.jpg")
                    fileOutputStream.write(bytesArray)
                    fileOutputStream.close()
                    Logger.d(tag, "Thumbnail saved to $path.jpg")
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
                songRepository
                    .downloadToFile(
                        track = track,
                        videoId = track.videoId,
                        path = path,
                        isVideo = nowPlayingScreenData.value.isVideo && getVideo.value,
                    ).collectLatest {
                        _downloadFileProgress.value = it
                    }
            }
        }
    }

    fun downloadFileDone() {
        _downloadFileProgress.value = DownloadProgress.INIT
    }

    fun onDoneReview(isDismissOnly: Boolean = true) {
        viewModelScope.launch {
            if (!isDismissOnly) {
                dataStoreManager.doneOpenAppTime()
            } else {
                dataStoreManager.openApp()
            }
        }
    }

    fun onDoneRequestingShareLyrics(contributor: Pair<String, String>? = null) {
        viewModelScope.launch {
            dataStoreManager.setHelpBuildLyricsDatabase(true)
            dataStoreManager.setContributorLyricsDatabase(
                contributor,
            )
        }
    }

    fun setBitmap(bitmap: ImageBitmap?) {
        _nowPlayingScreenData.update {
            it.copy(bitmap = bitmap)
        }
    }

    /**
     * Vote for SimpMusic original lyrics (upvote or downvote)
     * @param upvote true for upvote, false for downvote
     */
    fun voteLyrics(upvote: Boolean) {
        val lyricsData = _nowPlayingScreenData.value.lyricsData
        val lyricsProvider = lyricsData?.lyricsProvider
        val simpMusicLyricsId = lyricsData?.lyrics?.simpMusicLyrics?.id ?: return

        if (lyricsProvider != LyricsProvider.SIMPMUSIC || simpMusicLyricsId.isEmpty()) {
            Logger.w(tag, "Cannot vote: not a SimpMusic lyrics or missing ID")
            return
        }

        viewModelScope.launch {
            _lyricsVoteState.update {
                it?.copy(
                    state = VoteState.Loading,
                )
            }
            lyricsCanvasRepository
                .voteSimpMusicLyrics(
                    lyricsId = simpMusicLyricsId,
                    upvote = upvote,
                ).collectLatest { result ->
                    when (result) {
                        is Resource.Error -> {
                            Logger.w(tag, "Vote SimpMusic Lyrics Error ${result.message}")
                            _lyricsVoteState.update {
                                it?.copy(
                                    state = VoteState.Error(result.message ?: "Unknown error"),
                                )
                            }
                        }

                        is Resource.Success -> {
                            Logger.d(tag, "Vote SimpMusic Lyrics Success")
                            _lyricsVoteState.update {
                                it?.copy(
                                    state = VoteState.Success(upvote),
                                    vote = it.vote + if (upvote) 1 else -1,
                                )
                            }
                            makeToast(getString(Res.string.vote_submitted))
                        }
                    }
                }
        }
    }

    private fun resetLyricsVoteState() {
        _lyricsVoteState.value = null
        _translatedVoteState.value = null
    }

    /**
     * Vote for SimpMusic translated lyrics (upvote or downvote)
     * @param upvote true for upvote, false for downvote
     */
    fun voteTranslatedLyrics(upvote: Boolean) {
        val translatedLyrics = _nowPlayingScreenData.value.lyricsData?.translatedLyrics
        val lyricsProvider = translatedLyrics?.second
        val simpMusicLyricsId = translatedLyrics?.first?.simpMusicLyrics?.id ?: return

        if (lyricsProvider != LyricsProvider.SIMPMUSIC || simpMusicLyricsId.isEmpty()) {
            Logger.w(tag, "Cannot vote: not a SimpMusic translated lyrics or missing ID")
            return
        }

        viewModelScope.launch {
            _translatedVoteState.update {
                it?.copy(
                    state = VoteState.Loading,
                )
            }
            lyricsCanvasRepository
                .voteSimpMusicTranslatedLyrics(
                    translatedLyricsId = simpMusicLyricsId,
                    upvote = upvote,
                ).collectLatest { result ->
                    when (result) {
                        is Resource.Error -> {
                            Logger.w(tag, "Vote SimpMusic Translated Lyrics Error ${result.message}")
                            _translatedVoteState.update {
                                it?.copy(
                                    state = VoteState.Error(result.message ?: "Unknown error"),
                                )
                            }
                        }

                        is Resource.Success -> {
                            Logger.d(tag, "Vote SimpMusic Translated Lyrics Success")
                            _translatedVoteState.update {
                                it?.copy(
                                    state = VoteState.Success(upvote),
                                    vote = it.vote + if (upvote) 1 else -1,
                                )
                            }
                            makeToast(getString(Res.string.vote_submitted))
                        }
                    }
                }
        }
    }

    fun shouldStopMusicService(): Boolean = runBlocking { dataStoreManager.killServiceOnExit.first() == TRUE }

    fun isUserLoggedIn(): Boolean = runBlocking { dataStoreManager.cookie.first().isNotEmpty() }

    // Flow-based variant of [isUserLoggedIn] so composables can collect login state once
    // instead of calling runBlocking inside composition (used by NowPlayingScreenContent).
    fun isUserLoggedInFlow(): Flow<Boolean> = dataStoreManager.cookie.map { it.isNotEmpty() }

    fun isCombineFavoriteAndYTLiked(): Boolean = runBlocking { dataStoreManager.combineLocalAndYouTubeLiked.first() == TRUE }
}

sealed class UIEvent {
    data object PlayPause : UIEvent()

    data object Backward : UIEvent()

    data object Forward : UIEvent()

    data object Next : UIEvent()

    data object Previous : UIEvent()

    /**
     * Always advances to the previous track — bypasses the 3-second
     * "seek to start of current track" rule used by [Previous]. Used by the
     * NowPlaying artwork pager swipe.
     */
    data object SkipToPrevious : UIEvent()

    data object Stop : UIEvent()

    data object Shuffle : UIEvent()

    data object Repeat : UIEvent()

    data class UpdateProgress(
        val newProgress: Float,
    ) : UIEvent()

    data class UpdateVolume(
        val newVolume: Float,
    ) : UIEvent()

    data object ToggleLike : UIEvent()
}

@Immutable
data class LyricsTranslationUiState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val sourceEngine: String? = null,
    val isSameLanguage: Boolean = false,
)

enum class LyricsProvider {
    SIMPMUSIC,
    YOUTUBE,
    SPOTIFY,
    LRCLIB,
    BETTER_LYRICS,
    LETRAS,
    AI,
    TRANSLATOR,
    OFFLINE,
}

data class NowPlayingScreenData(
    val playlistName: String,
    /** The queue plays from a radio or mix id, so the top bar says Radio/Mix instead of Now Playing. */
    val isRadioQueue: Boolean = false,
    val nowPlayingTitle: String,
    val artistName: String,
    val isVideo: Boolean,
    val isExplicit: Boolean = false,
    val thumbnailURL: String?,
    val canvasData: CanvasData? = null,
    val lyricsData: LyricsData? = null,
    /** Every provider and the offline cache came back empty for this song; offers Add lyrics. */
    val lyricsNotFound: Boolean = false,
    val songInfoData: SongInfoEntity? = null,
    val bitmap: ImageBitmap? = null,
) {
    data class CanvasData(
        val isVideo: Boolean,
        val url: String,
        // A still of the clip at the clip's own size — Apple Music's animated artwork sends one.
        // The Apple Music player shows it under the clip while that loads, and reads the clip's
        // proportions off it, so the frame is the right shape before the first video frame arrives.
        val thumbUrl: String? = null,
    )

    data class LyricsData(
        val lyrics: Lyrics,
        val translatedLyrics: Pair<Lyrics, LyricsProvider>? = null,
        val lyricsProvider: LyricsProvider,
    )

    companion object {
        fun initial(): NowPlayingScreenData =
            NowPlayingScreenData(
                nowPlayingTitle = "",
                artistName = "",
                isVideo = false,
                thumbnailURL = null,
                canvasData = null,
                lyricsData = null,
                songInfoData = null,
                playlistName = "",
            )
    }
}

data class VoteData(
    val id: String,
    val vote: Int,
    val state: VoteState,
)

sealed class VoteState {
    data object Idle : VoteState()

    data object Loading : VoteState()

    data class Success(
        val upvote: Boolean,
    ) : VoteState()

    data class Error(
        val message: String,
    ) : VoteState()
}

/**
 * Whether a stored canvas url points at something a player should open rather than an image.
 *
 * The column holds whatever the active source wrote: a Spotify canvas is an `.mp4`, while AM
 * animated artwork is an HLS `.m3u8` master playlist. Testing only for `.mp4` — as this did before
 * AM existed — sends every AM artwork down the still-image branch, and because the branch that
 * reads this is the one that restores a *cached* url, the failure only appears from the second play
 * of a track onwards.
 */
private fun String.isCanvasVideoUrl(): Boolean = contains(".mp4") || contains(".m3u8")
