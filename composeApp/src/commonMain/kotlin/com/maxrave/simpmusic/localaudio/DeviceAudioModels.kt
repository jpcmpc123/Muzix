package com.maxrave.simpmusic.localaudio

import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.common.localDeviceAudioContentUriString
import com.maxrave.common.localDeviceAudioId
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.songs.Album
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import kotlinx.serialization.Serializable

data class DeviceAudioTrack(
    val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    val dateModifiedSeconds: Long = 0L,
    val trackNumber: Int = 0,
    val relativePath: String,
    val folderPath: String,
    val folderName: String,
    val displayName: String,
    val mimeType: String? = null,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val contentUri: String = localDeviceAudioContentUriString(localDeviceAudioId(mediaStoreId))
        ?: "content://media/external/audio/media/$mediaStoreId",
    val albumArtUri: String? = if (albumId > 0L) "content://media/external/audio/albumart/$albumId" else null,
) {
    val mediaId: String
        get() = localDeviceAudioId(mediaStoreId)

    fun toTrack(unknownArtistFallback: String = "Unknown artist"): Track =
        Track(
            album =
                album.takeIf { it.isNotBlank() }?.let { albumName ->
                    Album(
                        id = if (albumId > 0L) "local_album_$albumId" else "",
                        name = albumName,
                    )
                },
            artists =
                listOf(
                    Artist(
                        id = null,
                        name = artist.ifBlank { unknownArtistFallback },
                    ),
                ),
            duration = formatDurationMs(durationMs),
            durationSeconds = (durationMs / 1000L).toInt().coerceAtLeast(1),
            isAvailable = true,
            isExplicit = false,
            likeStatus = null,
            thumbnails =
                albumArtUri?.let { uri ->
                    listOf(
                        Thumbnail(
                            height = 512,
                            url = uri,
                            width = 512,
                        ),
                    )
                },
            title = title.ifBlank { displayName.substringBeforeLast('.') },
            videoId = mediaId,
            videoType = MERGING_DATA_TYPE.SONG,
            category = "DEVICE_AUDIO",
            feedbackTokens = null,
            resultType = "SONG",
        )
}

enum class DeviceAudioSubTab {
    SONGS,
    FOLDERS,
    ALBUMS,
    ARTISTS,
}

enum class DeviceAudioSortOrder {
    TITLE_ASC,
    DATE_ADDED_DESC,
    ARTIST_ASC,
    ALBUM_ASC,
    DURATION_DESC,
    ;

    companion object {
        fun fromStored(value: String?): DeviceAudioSortOrder =
            entries.firstOrNull { it.name == value } ?: TITLE_ASC
    }
}

@Serializable
data class DeviceAudioFilterConfig(
    val minDurationSeconds: Int = 30,
    val minSizeBytes: Long = 100L * 1024L,
    val ignoreSystemAndMessagingFolders: Boolean = true,
    val hiddenFolderPaths: Set<String> = emptySet(),
    val hiddenMediaStoreIds: Set<Long> = emptySet(),
)

data class DeviceAudioTechnicalDetails(
    val displayName: String,
    val fullPathOrRelative: String,
    val mimeType: String?,
    val codecLabel: String?,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
    val sizeBytes: Long,
    val durationMs: Long,
    val dateAddedSeconds: Long,
)

data class DeviceAudioFolder(
    val folderPath: String,
    val folderName: String,
    val tracks: List<DeviceAudioTrack>,
    val artworkUri: String?,
)

data class DeviceAudioAlbum(
    val albumKey: String,
    val albumName: String,
    val artistName: String,
    val artworkUri: String?,
    val tracks: List<DeviceAudioTrack>,
)

data class DeviceAudioArtist(
    val artistKey: String,
    val artistName: String,
    val albumCount: Int,
    val artworkUri: String?,
    val tracks: List<DeviceAudioTrack>,
)
