package com.maxrave.simpmusic.viewModel

import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.PodcastsEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.type.RecentlyType
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val YOUTUBE_LIKED_MUSIC_THUMBNAIL_URL =
    "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-songs-delhi-1200.png"

private val LIBRARY_CACHE_JSON =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

@Serializable
internal data class CachedYouTubePlaylistEntry(
    val browseId: String,
    val title: String,
    val author: String = "",
    val itemCount: String = "",
    val thumbnailUrl: String = "",
    val category: String = "",
    val resultType: String = "",
)

@Serializable
internal data class CachedYouTubePlaylistsSnapshot(
    val accountHash: Int,
    val items: List<CachedYouTubePlaylistEntry> = emptyList(),
)

internal fun encodeYouTubePlaylistsCache(
    accountHash: Int,
    playlists: List<PlaylistsResult>,
): String {
    val entries =
        playlists.map { item ->
            CachedYouTubePlaylistEntry(
                browseId = item.browseId,
                title = item.title,
                author = item.author,
                itemCount = item.itemCount,
                thumbnailUrl = item.thumbnails.lastOrNull()?.url ?: "",
                category = item.category,
                resultType = item.resultType,
            )
        }
    return LIBRARY_CACHE_JSON.encodeToString(
        CachedYouTubePlaylistsSnapshot.serializer(),
        CachedYouTubePlaylistsSnapshot(accountHash = accountHash, items = entries),
    )
}

internal fun decodeYouTubePlaylistsCache(
    raw: String?,
    expectedAccountHash: Int,
): List<PlaylistsResult>? {
    if (raw.isNullOrBlank()) return null
    val snapshot =
        runCatching {
            LIBRARY_CACHE_JSON.decodeFromString(CachedYouTubePlaylistsSnapshot.serializer(), raw)
        }.getOrNull() ?: return null
    if (snapshot.accountHash != expectedAccountHash) return null
    return snapshot.items.map { entry ->
        PlaylistsResult(
            author = entry.author,
            browseId = entry.browseId,
            category = entry.category,
            itemCount = entry.itemCount,
            resultType = entry.resultType,
            thumbnails =
                if (entry.thumbnailUrl.isNotBlank()) {
                    listOf(Thumbnail(height = 240, url = entry.thumbnailUrl, width = 240))
                } else {
                    emptyList()
                },
            title = entry.title,
        )
    }
}

enum class HistoryDateBucket {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    OLDER,
}

data class HistorySection(
    val bucket: HistoryDateBucket,
    val items: List<RecentlyType>,
)

internal fun deduplicateYouTubePlaylistsWithoutLiked(playlists: List<PlaylistsResult>): List<PlaylistsResult> =
    playlists.filterNot { item ->
        item.browseId == "LM" || item.browseId == "VLLM"
    }

internal fun RecentlyType.inLibraryDateOrNull(): LocalDate? =
    when (this) {
        is SongEntity -> inLibrary.date
        is ArtistEntity -> inLibrary.date
        is AlbumEntity -> inLibrary.date
        is PlaylistEntity -> inLibrary.date
        is PodcastsEntity -> inLibrary.date
        else -> null
    }

internal fun classifyHistoryBucket(
    itemDate: LocalDate?,
    today: LocalDate,
): HistoryDateBucket {
    if (itemDate == null) return HistoryDateBucket.OLDER
    val yesterday = today.minus(DatePeriod(days = 1))
    val weekStart = today.minus(DatePeriod(days = 6))
    return when {
        itemDate >= today -> HistoryDateBucket.TODAY
        itemDate == yesterday -> HistoryDateBucket.YESTERDAY
        itemDate >= weekStart -> HistoryDateBucket.THIS_WEEK
        itemDate.year == today.year && itemDate.month == today.month -> HistoryDateBucket.THIS_MONTH
        else -> HistoryDateBucket.OLDER
    }
}

internal fun groupRecentlyAddedByDate(
    items: List<RecentlyType>,
    today: LocalDate,
): List<HistorySection> {
    if (items.isEmpty()) return emptyList()
    val grouped = LinkedHashMap<HistoryDateBucket, MutableList<RecentlyType>>(HistoryDateBucket.entries.size)
    for (item in items) {
        val bucket = classifyHistoryBucket(item.inLibraryDateOrNull(), today)
        grouped.getOrPut(bucket) { ArrayList() }.add(item)
    }
    return HistoryDateBucket.entries.mapNotNull { bucket ->
        val entries = grouped[bucket]
        if (entries.isNullOrEmpty()) null else HistorySection(bucket, entries)
    }
}

/** How Your library draws the YouTube playlists: one row each, or two covers per row. */
enum class LibraryLayoutMode(
    val storedValue: String,
) {
    LIST("list"),
    GRID("grid"),
    ;

    fun toggled(): LibraryLayoutMode = if (this == LIST) GRID else LIST
}

/** The saved layout, falling back to the list for anything missing, unknown or corrupt. */
internal fun parseLibraryLayoutMode(raw: String?): LibraryLayoutMode = LibraryLayoutMode.entries.firstOrNull { it.storedValue == raw } ?: LibraryLayoutMode.LIST
