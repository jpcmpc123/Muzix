package com.maxrave.simpmusic.viewModel

import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun encodeAndDecodeYouTubePlaylistsCacheRoundTripsAndRejectsOtherAccountsOrCorruptJson() {
        val accountHash = 123456
        val playlists =
            listOf(
                PlaylistsResult(
                    author = "Algoritmo da Zueira",
                    browseId = "VLPL123",
                    category = "",
                    itemCount = "62 itens",
                    resultType = "",
                    thumbnails = listOf(Thumbnail(height = 240, url = "https://example.com/thumb.jpg", width = 240)),
                    title = "top music",
                ),
            )

        val encoded = encodeYouTubePlaylistsCache(accountHash, playlists)
        val decoded = decodeYouTubePlaylistsCache(encoded, expectedAccountHash = accountHash)

        assertEquals(1, decoded?.size)
        val first = decoded!!.first()
        assertEquals("VLPL123", first.browseId)
        assertEquals("top music", first.title)
        assertEquals("Algoritmo da Zueira", first.author)
        assertEquals("62 itens", first.itemCount)
        assertEquals("https://example.com/thumb.jpg", first.thumbnails.firstOrNull()?.url)

        assertNull(decodeYouTubePlaylistsCache(encoded, expectedAccountHash = 999999))
        assertNull(decodeYouTubePlaylistsCache("{not-valid-json", expectedAccountHash = accountHash))
        assertNull(decodeYouTubePlaylistsCache(null, expectedAccountHash = accountHash))
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

    @Test
    fun parseLibraryLayoutModeFallsBackToListAndToggles() {
        assertEquals(LibraryLayoutMode.GRID, parseLibraryLayoutMode("grid"))
        assertEquals(LibraryLayoutMode.LIST, parseLibraryLayoutMode("list"))
        assertEquals(LibraryLayoutMode.LIST, parseLibraryLayoutMode(null))
        assertEquals(LibraryLayoutMode.LIST, parseLibraryLayoutMode("???"))
        assertEquals(LibraryLayoutMode.GRID, LibraryLayoutMode.LIST.toggled())
        assertEquals(LibraryLayoutMode.LIST, LibraryLayoutMode.GRID.toggled())
        assertEquals(LibraryLayoutMode.GRID, parseLibraryLayoutMode(LibraryLayoutMode.GRID.storedValue))
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
