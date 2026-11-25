package com.maxrave.simpmusic.viewModel

import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryHistoryHelpersTest {
    @Test
    fun deduplicateYouTubePlaylistsRemovesLikedAliasesAndKeepsOthersInOrder() {
        val playlists =
            listOf(
                playlist("LM", "Liked Music"),
                playlist("PL1", "top music"),
                playlist("VLLM", "Músicas que Gostei"),
                playlist("PL2", "Elis Regina"),
            )

        val result = deduplicateYouTubePlaylistsWithoutLiked(playlists)

        assertEquals(listOf("PL1", "PL2"), result.map { it.browseId })
    }

    @Test
    fun groupRecentlyAddedByDateSeparatesTodayYesterdayWeekMonthAndOlder() {
        val today = LocalDate(2026, 11, 25)
        val todaySong = song("s1", LocalDateTime(2026, 11, 25, 16, 50))
        val yesterdayPlaylist = PlaylistEntity(id = "PL1", title = "top music", author = "Algoritmo", thumbnails = "", inLibrary = LocalDateTime(2026, 11, 24, 21, 0))
        val thisWeekArtist = ArtistEntity(channelId = "UC1", name = "Steve Lacy", thumbnails = null, inLibrary = LocalDateTime(2026, 11, 20, 12, 0))
        val thisMonthSong = song("s2", LocalDateTime(2026, 11, 3, 10, 0))
        val olderSong = song("s3", LocalDateTime(2026, 10, 29, 9, 0))

        val sections =
            groupRecentlyAddedByDate(
                items = listOf(todaySong, yesterdayPlaylist, thisWeekArtist, thisMonthSong, olderSong),
                today = today,
            )

        assertEquals(
            listOf(
                HistoryDateBucket.TODAY,
                HistoryDateBucket.YESTERDAY,
                HistoryDateBucket.THIS_WEEK,
                HistoryDateBucket.THIS_MONTH,
                HistoryDateBucket.OLDER,
            ),
            sections.map { it.bucket },
        )
        assertEquals(listOf(todaySong), sections[0].items)
        assertEquals(listOf(yesterdayPlaylist), sections[1].items)
        assertEquals(listOf(thisWeekArtist), sections[2].items)
        assertEquals(listOf(thisMonthSong), sections[3].items)
        assertEquals(listOf(olderSong), sections[4].items)
    }

    private fun song(
        id: String,
        inLibrary: LocalDateTime,
    ) = SongEntity(
        videoId = id,
        albumId = null,
        albumName = null,
        artistId = null,
        artistName = listOf("Artist"),
        duration = "3:00",
        durationSeconds = 180,
        isAvailable = true,
        isExplicit = false,
        likeStatus = "INDIFFERENT",
        thumbnails = null,
        title = "Title $id",
        videoType = "SONG",
        category = null,
        resultType = null,
        inLibrary = inLibrary,
    )

    private fun playlist(
        id: String,
        title: String,
    ) = PlaylistsResult(
        author = "User",
        browseId = id,
        category = "",
        itemCount = "10 itens",
        resultType = "",
        thumbnails = emptyList(),
        title = title,
    )
}
