package com.maxrave.simpmusic.viewModel

import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchViewModelHelpersTest {
    @Test
    fun shouldFetchTabForQueryRespectsLoadedCachePerCategory() {
        val cache = mapOf(SearchType.SONGS to "queen", SearchType.ALL to "queen")
        assertFalse(shouldFetchTabForQuery("queen", SearchType.SONGS, cache))
        assertTrue(shouldFetchTabForQuery("queen", SearchType.ARTISTS, cache))
        assertTrue(shouldFetchTabForQuery("beatles", SearchType.SONGS, cache))
        assertFalse(shouldFetchTabForQuery("   ", SearchType.ARTISTS, cache))
    }

    @Test
    fun mergePagedResultsDeduplicatesIncomingEntriesWhilePreservingOrder() {
        val page1 = listOf(song("v1"), song("v2"))
        val page2 = listOf(song("v2"), song("v3"), song("v1"), song("v4"))
        val merged = mergePagedResults(page1, page2) { it.videoId }
        assertEquals(listOf("v1", "v2", "v3", "v4"), merged.map { it.videoId })
    }

    private fun song(id: String): SongsResult =
        SongsResult(
            album = null,
            artists = emptyList(),
            category = "Song",
            duration = null,
            durationSeconds = null,
            feedbackTokens = null,
            isExplicit = false,
            resultType = "Song",
            thumbnails = emptyList(),
            title = "Song $id",
            videoId = id,
            videoType = null,
            year = "",
        )
}
