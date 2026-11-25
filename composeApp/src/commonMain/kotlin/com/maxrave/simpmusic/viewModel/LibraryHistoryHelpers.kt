package com.maxrave.simpmusic.viewModel

import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.PodcastsEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.type.RecentlyType
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

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
