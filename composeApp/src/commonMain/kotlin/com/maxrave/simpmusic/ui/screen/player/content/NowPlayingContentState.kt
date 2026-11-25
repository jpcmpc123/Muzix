package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.data.player.GenericCastState
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.simpmusic.extension.GradientOffset
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.LyricsTranslationUiState
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

private val EmptyLyricsTranslationStateFlow: StateFlow<LyricsTranslationUiState> =
    MutableStateFlow(LyricsTranslationUiState())

/**
 * Whether the lyrics currently on screen can be rated.
 *
 * SimpMusic Lyrics is the only provider with a vote endpoint, and the vote is cast against the
 * SimpMusic record itself — so the provider tag alone is NOT the condition: `simpMusicLyrics`
 * must actually be there. Either half qualifying is enough, because the dialog rates whichever
 * of the two came from SimpMusic.
 *
 * Lives on the contract the three styles share. The Apple Music style shipped its floating vote
 * button ungated — offering a rating on YouTube, LRCLIB and Spotify lyrics alike — precisely
 * because this rule existed only as an expression copy-pasted inside the other two styles, where
 * a new style had no reason to go looking for it.
 */
internal fun NowPlayingScreenData.LyricsData?.canVote(): Boolean {
    val data = this ?: return false
    val votableLyrics =
        data.lyricsProvider == LyricsProvider.SIMPMUSIC && data.lyrics.simpMusicLyrics != null
    val votableTranslation =
        data.translatedLyrics?.second == LyricsProvider.SIMPMUSIC &&
            data.translatedLyrics?.first?.simpMusicLyrics != null
    return votableLyrics || votableTranslation
}

// Apple Music's animated covers arrive as HLS (.m3u8) from Apple's video CDN; Spotify's canvases
// are MP4. Only the former is album art, shaped like album art (3:4 or 1:1), so every style plays it
// at the top of the page in a frame of its own shape, under controls that never hide.
internal fun NowPlayingScreenData.CanvasData.isAnimatedArtwork(): Boolean = url.contains(".m3u8")

/**
 * The canvas that takes the whole page and hides the controls — a Spotify canvas (9:16, made to fill
 * a phone) — or null when there is none or it is Apple Music's animated artwork.
 */
internal fun NowPlayingScreenData.fullscreenCanvas(): NowPlayingScreenData.CanvasData? = canvasData?.takeUnless { it.isAnimatedArtwork() }

/**
 * Colour of the secondary text in the track and time rows. Over animated artwork the page is a mesh
 * of the sleeve's own colours and can be bright, which turns the body grey into a smudge, so the text
 * becomes a veil of white instead. Unspecified elsewhere: the style's own text style decides.
 */
internal fun NowPlayingContentState.secondaryTextColor(): Color =
    if (screenData.canvasData?.isAnimatedArtwork() == true) Color.White.copy(alpha = 0.7f) else Color.Unspecified

// Backdrop behind the player. A dark surface rather than pure black: #000000 reads as a hole
// next to the artwork-tinted gradient and cards, which is why Spotify sits its player on a
// near-black surface instead. Used for the gradient's end colour, the fade-to target and the
// area below the gradient so all three match exactly and leave no seam.
internal val PlayerBackdropColor = Color(0xFF121212)

private val RICH_SYNC_TIMESTAMP_REGEX = Regex("""<\d{2}:\d{2}\.\d{2,3}>\s*""")
private val WHITESPACE_REGEX = Regex("""\s+""")

// Word-by-word lyrics carry a timestamp per word; replace each with a space
// (not ""), then collapse — otherwise the words run together.
// Shared by every Now Playing content style (Spotify + M3 Expressive).
internal fun String.stripRichSyncTimestamps(): String =
    replace(RICH_SYNC_TIMESTAMP_REGEX, " ")
        .replace(WHITESPACE_REGEX, " ")
        .trim()

internal fun mergeFormatWithActiveAudioTrack(
    dbFormat: com.maxrave.domain.data.entities.NewFormatEntity?,
    activeTrack: com.maxrave.domain.data.player.ActiveAudioTrackInfo?,
    currentMediaId: String?,
): com.maxrave.domain.data.entities.NewFormatEntity? {
    val matchingTrack =
        activeTrack?.takeIf { info ->
            val cleanCurrent = currentMediaId?.removePrefix(com.maxrave.common.MERGING_DATA_TYPE.VIDEO)
            val cleanInfo = info.mediaId.removePrefix(com.maxrave.common.MERGING_DATA_TYPE.VIDEO)
            cleanCurrent == null || cleanInfo == cleanCurrent
        }
    if (dbFormat != null) {
        if (matchingTrack == null) return dbFormat
        return dbFormat.copy(
            mimeType = dbFormat.mimeType?.takeIf { it.isNotBlank() } ?: matchingTrack.mimeType,
            codecs = dbFormat.codecs?.takeIf { it.isNotBlank() } ?: matchingTrack.codecs,
            bitrate = dbFormat.bitrate?.takeIf { it > 0 } ?: matchingTrack.bitrate,
            sampleRate = dbFormat.sampleRate?.takeIf { it > 0 } ?: matchingTrack.sampleRate,
        )
    }
    if (matchingTrack == null) return null
    val hasAnyMetadata =
        !matchingTrack.mimeType.isNullOrBlank() ||
            !matchingTrack.codecs.isNullOrBlank() ||
            (matchingTrack.bitrate ?: 0) > 0 ||
            (matchingTrack.sampleRate ?: 0) > 0
    if (!hasAnyMetadata) return null
    return com.maxrave.domain.data.entities.NewFormatEntity(
        videoId = currentMediaId ?: matchingTrack.mediaId,
        itag = 0,
        mimeType = matchingTrack.mimeType,
        codecs = matchingTrack.codecs,
        bitrate = matchingTrack.bitrate,
        sampleRate = matchingTrack.sampleRate,
        contentLength = null,
        loudnessDb = null,
        lengthSeconds = null,
        playbackTrackingVideostatsPlaybackUrl = null,
        playbackTrackingAtrUrl = null,
        playbackTrackingVideostatsWatchtimeUrl = null,
        cpn = null,
    )
}

/**
 * "Opus · 154 kbps · 48 kHz" — the Apple Music style's quality line, read off the format YouTube
 * served for the track now playing: its codec, `bitrate` (the figure the Info sheet prints in bps),
 * and `sampleRate`. Any field the format does not carry is omitted cleanly, and null comes back
 * when nothing is known.
 */
internal fun NewFormatEntity?.toAudioQualityLabel(): String? {
    val format = this ?: return null
    val codec = resolveFriendlyAudioCodec(format.codecs, format.mimeType, format.itag)
    val parts =
        buildList {
            codec?.let { add(it) }
            format.bitrate?.takeIf { it > 0 }?.let { add("${(it / 1000.0).roundToInt()} kbps") }
            format.sampleRate?.takeIf { it > 0 }?.let { add(it.toKhzLabel()) }
        }
    return parts.joinToString(" · ").takeIf { it.isNotEmpty() }
}

private fun resolveFriendlyAudioCodec(
    codecs: String?,
    mimeType: String?,
    itag: Int,
): String? {
    val combined = "${codecs.orEmpty()} ${mimeType.orEmpty()}".lowercase()
    return when {
        "flac" in combined -> "FLAC"
        "alac" in combined -> "ALAC"
        "opus" in combined || itag in setOf(249, 250, 251, 774) -> "Opus"
        "mp4a" in combined || "aac" in combined || itag in setOf(139, 140, 141) -> "AAC"
        "vorbis" in combined || itag in setOf(171, 172) -> "Vorbis"
        "mpeg" in combined || "mp3" in combined -> "MP3"
        else -> null
    }
}

private val XTAGS_PARAM_REGEX = Regex("[?&]xtags=([^&]+)")

internal fun isDrcAudioStreamUrl(url: String): Boolean {
    val xtags =
        XTAGS_PARAM_REGEX
            .find(url)
            ?.groupValues
            ?.get(1)
            ?.replace("%3D", "=", ignoreCase = true)
            ?.replace("%3A", ":", ignoreCase = true)
            ?: return false
    return xtags
        .split(':')
        .any { part ->
            val key = part.substringBefore('=')
            val value = part.substringAfter('=', "")
            key == "drc" && (value == "1" || value.equals("true", ignoreCase = true))
        }
}

// 48000 → "48 kHz", 44100 → "44.1 kHz": one decimal, and none when it would be ".0".
private fun Int.toKhzLabel(): String {
    val tenths = (this + 50) / 100
    return if (tenths % 10 == 0) "${tenths / 10} kHz" else "${tenths / 10}.${tenths % 10} kHz"
}

/**
 * Everything a Now Playing content layer reads. The shell ([com.maxrave.simpmusic.ui.screen.player.NowPlayingScreenContent])
 * owns the ViewModel collection, palette animation, sheets/dialogs and gesture state machines;
 * a content composable only renders from this snapshot.
 */
@Stable
data class NowPlayingContentState(
    val screenData: NowPlayingScreenData,
    val controllerState: ControlState,
    val timelineState: TimeLine,
    val timelineFlow: StateFlow<TimeLine>,
    val likeStatus: Boolean,
    val castState: GenericCastState,
    val shouldShowVideo: Boolean,
    val isUserLoggedIn: Boolean,
    val artworkQueue: List<Track>,
    val currentOrderIndex: Int,
    val artworkPagerState: PagerState,
    val startColor: Animatable<Color, AnimationVector4D>,
    val endColor: Animatable<Color, AnimationVector4D>,
    val spotShadowColor: Color,
    val gradientOffset: GradientOffset,
    val sliderTrackColor: Color,
    val sliderValue: Float,
    val currentLyricLineIndex: Int,
    val showControlLayout: Boolean,
    val controlLayoutAlpha: Float,
    val showHideMiddleLayout: Boolean,
    val shouldShowToolbar: Boolean,
    val isInPipMode: Boolean,
    val mainScrollState: ScrollState,
    val isExpanded: Boolean,
    val dismissIcon: ImageVector,
    /** "154 kbps · 48 kHz" for the stream now playing, or null while unknown — see [toAudioQualityLabel]. */
    val audioQualityLabel: String? = null,
    /**
     * The user's lyrics delay, applied at read time. [currentLyricLineIndex] already has it baked in;
     * this is for anything that times WITHIN a line (the Apple Music lyric strip's word sweep).
     */
    val lyricsOffsetMs: Long = 0L,
    /**
     * Width / height of the video now playing, 16:9 until the player knows it. Every style sizes
     * its video frame from this one value, so a frame and the spacer that measures it cannot drift.
     */
    val videoAspectRatio: Float = 16f / 9,
    /**
     * The YouTube id of the track playing, "Video" prefix stripped, or null before anything plays.
     * Read off the media item rather than the queue's track, which is null while the queue restored
     * at startup is still being rebuilt: the same id the comments sheet uses.
     */
    val videoId: String? = null,
    /** Isolated lyrics translation visibility/loading state flow to avoid recomposing the player. */
    val lyricsTranslationStateFlow: StateFlow<LyricsTranslationUiState> = EmptyLyricsTranslationStateFlow,
)

/**
 * Everything a Now Playing content layer can do. All callbacks land in the shell, which owns
 * the ViewModel, the navController and the sheet/dialog visibility flags.
 */
@Stable
class NowPlayingContentActions(
    val onUIEvent: (UIEvent) -> Unit,
    val onSeekToQueueIndex: (Int) -> Unit,
    val onArtworkBitmap: (ImageBitmap) -> Unit,
    val onSliderChange: (Float) -> Unit,
    val onSliderChangeFinished: () -> Unit,
    val onToggleControls: () -> Unit,
    val onNavigateToArtist: () -> Unit,
    val onOpenListenTogether: () -> Unit,
    val onAddToYouTubeLiked: () -> Unit,
    val onShowMoreSheet: () -> Unit,
    val onShowQueue: () -> Unit,
    val onShowInfo: () -> Unit,
    val onShowAddToPlaylist: () -> Unit,
    /** Opens the comments sheet for the track playing. */
    val onShowComments: () -> Unit,
    /** Opens the comments sheet with [draft] already in its box (the SoundCloud style's emoji). */
    val onWriteComment: (draft: String) -> Unit,
    val onShowFullscreenLyrics: () -> Unit,
    val onShowVoteDialog: () -> Unit,
    /** Writes the lyrics timing offset — the same value Settings › Lyrics edits. */
    val onLyricsOffsetChange: (Int) -> Unit,
    /** Toggles on-demand lyrics translation visibility/fetching for the current song. */
    val onToggleLyricsTranslation: () -> Unit = {},
    /** Forces a fresh translation bypass of the local cache on long press. */
    val onRetranslateLyrics: () -> Unit = {},
    val onEnterFullscreenVideo: () -> Unit,
    val onDismiss: () -> Unit,
    val onToolbarVisibilityChange: (Boolean) -> Unit,
    /** Reorders the queue. `from`/`to` are absolute indices into [NowPlayingContentState.artworkQueue]. */
    val onMoveQueueItem: (from: Int, to: Int) -> Unit,
    /** Removes one queue entry. `index` is an absolute index into [NowPlayingContentState.artworkQueue]. */
    val onRemoveQueueItem: (index: Int) -> Unit,
    /** Opens the full song sheet for one queue entry; `index` is absolute, as above. */
    val onQueueItemMore: (index: Int, track: Track) -> Unit,
)
