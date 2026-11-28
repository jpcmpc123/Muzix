package com.maxrave.simpmusic.localaudio

object DeviceAudioLibraryEngine {
    private val ignoredPathMarkers =
        listOf(
            "whatsapp/media/whatsapp audio",
            "whatsapp/media/whatsapp voice notes",
            "whatsapp audio",
            "whatsapp voice notes",
            "whatsapp/media/.statuses",
            "android/media/com.whatsapp",
            "telegram/telegram audio",
            "telegram/telegram documents",
            "android/data",
            "android/obb",
            "notifications",
            "ringtones",
            "alarms",
            "callrecordings",
            "call recordings",
            "call_recordings",
            "voice recorder",
            "voicerecorder",
            "sound_recorder",
            "recordings",
        )

    fun normalizeFolderPath(rawPath: String): String {
        val cleaned = rawPath.replace('\\', '/').trim().trimEnd('/')
        return cleaned.ifEmpty { "/" }
    }

    fun folderDisplayName(normalizedFolderPath: String): String {
        if (normalizedFolderPath == "/" || normalizedFolderPath.isBlank()) return "Internal Storage"
        return normalizedFolderPath.substringAfterLast('/').ifBlank { normalizedFolderPath }
    }

    fun isSystemOrJunkFolder(path: String): Boolean {
        val normalized = normalizeFolderPath(path).lowercase()
        val segments = normalized.split('/').filter { it.isNotEmpty() }
        if (segments.any { it.startsWith(".") }) return true
        return ignoredPathMarkers.any { marker ->
            normalized.contains(marker)
        }
    }

    fun applyFilters(
        allTracks: List<DeviceAudioTrack>,
        config: DeviceAudioFilterConfig,
    ): List<DeviceAudioTrack> {
        val minDurationMs = config.minDurationSeconds.coerceAtLeast(0).toLong() * 1000L
        val hiddenFolders = config.hiddenFolderPaths.map(::normalizeFolderPath).toSet()
        return allTracks.filter { track ->
            if (track.mediaStoreId in config.hiddenMediaStoreIds) return@filter false
            val normalizedFolder = normalizeFolderPath(track.folderPath)
            if (normalizedFolder in hiddenFolders) return@filter false
            if (config.ignoreSystemAndMessagingFolders &&
                (isSystemOrJunkFolder(normalizedFolder) || isSystemOrJunkFolder(track.relativePath))
            ) {
                return@filter false
            }
            if (minDurationMs > 0L && track.durationMs < minDurationMs) return@filter false
            if (config.minSizeBytes > 0L && track.sizeBytes < config.minSizeBytes) return@filter false
            true
        }
    }

    fun searchTracks(
        tracks: List<DeviceAudioTrack>,
        query: String,
    ): List<DeviceAudioTrack> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return tracks
        return tracks.filter { track ->
            track.title.lowercase().contains(q) ||
                track.artist.lowercase().contains(q) ||
                track.album.lowercase().contains(q) ||
                track.folderName.lowercase().contains(q) ||
                track.displayName.lowercase().contains(q)
        }
    }

    fun sortTracks(
        tracks: List<DeviceAudioTrack>,
        order: DeviceAudioSortOrder,
    ): List<DeviceAudioTrack> =
        when (order) {
            DeviceAudioSortOrder.TITLE_ASC ->
                tracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

            DeviceAudioSortOrder.DATE_ADDED_DESC ->
                tracks.sortedWith(
                    compareByDescending<DeviceAudioTrack> { it.dateAddedSeconds }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
                )

            DeviceAudioSortOrder.ARTIST_ASC ->
                tracks.sortedWith(
                    compareBy<DeviceAudioTrack, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.album }
                        .thenBy { normalizeTrackNumber(it.trackNumber) }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
                )

            DeviceAudioSortOrder.ALBUM_ASC ->
                tracks.sortedWith(
                    compareBy<DeviceAudioTrack, String>(String.CASE_INSENSITIVE_ORDER) { it.album }
                        .thenBy { normalizeTrackNumber(it.trackNumber) }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
                )

            DeviceAudioSortOrder.DURATION_DESC ->
                tracks.sortedWith(
                    compareByDescending<DeviceAudioTrack> { it.durationMs }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
                )
        }

    fun groupByFolder(
        tracks: List<DeviceAudioTrack>,
        sortOrder: DeviceAudioSortOrder = DeviceAudioSortOrder.TITLE_ASC,
    ): List<DeviceAudioFolder> =
        tracks
            .groupBy { normalizeFolderPath(it.folderPath) }
            .map { (path, items) ->
                val sorted = sortTracks(items, sortOrder)
                DeviceAudioFolder(
                    folderPath = path,
                    folderName = sorted.firstOrNull()?.folderName?.ifBlank { null } ?: folderDisplayName(path),
                    tracks = sorted,
                    artworkUri = sorted.firstNotNullOfOrNull { it.albumArtUri },
                )
            }.sortedBy { it.folderName.lowercase() }

    fun groupByAlbum(tracks: List<DeviceAudioTrack>): List<DeviceAudioAlbum> =
        tracks
            .groupBy { track ->
                if (track.albumId > 0L) {
                    "id:${track.albumId}"
                } else {
                    "name:${track.album.trim().lowercase()}"
                }
            }.map { (key, items) ->
                val ordered =
                    items.sortedWith(
                        compareBy<DeviceAudioTrack> { normalizeTrackNumber(it.trackNumber) }
                            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
                    )
                val first = ordered.first()
                DeviceAudioAlbum(
                    albumKey = key,
                    albumName = first.album,
                    artistName = first.artist,
                    artworkUri = ordered.firstNotNullOfOrNull { it.albumArtUri },
                    tracks = ordered,
                )
            }.sortedBy { it.albumName.lowercase() }

    fun groupByArtist(
        tracks: List<DeviceAudioTrack>,
        sortOrder: DeviceAudioSortOrder = DeviceAudioSortOrder.TITLE_ASC,
    ): List<DeviceAudioArtist> =
        tracks
            .groupBy { it.artist.trim().lowercase() }
            .map { (key, items) ->
                val sorted = sortTracks(items, sortOrder)
                val albumCount =
                    sorted
                        .map {
                            if (it.albumId > 0L) "id:${it.albumId}" else "name:${it.album.trim().lowercase()}"
                        }.distinct()
                        .size
                DeviceAudioArtist(
                    artistKey = key,
                    artistName = sorted.first().artist,
                    albumCount = albumCount,
                    artworkUri = sorted.firstNotNullOfOrNull { it.albumArtUri },
                    tracks = sorted,
                )
            }.sortedBy { it.artistName.lowercase() }

    private fun normalizeTrackNumber(raw: Int): Int {
        if (raw <= 0) return Int.MAX_VALUE
        // MediaStore encodes disc*1000 + track in TRACK (e.g., 1002 -> disc 1 track 2).
        return raw
    }
}

private fun padTwoDigits(value: Long): String = if (value < 10L) "0$value" else value.toString()

private fun formatDecimal(value: Double, decimals: Int): String {
    var factor = 1L
    repeat(decimals) { factor *= 10L }
    val scaled = kotlin.math.round(value * factor).toLong()
    val intPart = scaled / factor
    if (decimals == 0) return intPart.toString()
    val fracPart = kotlin.math.abs(scaled % factor).toString().padStart(decimals, '0')
    return "$intPart.$fracPart"
}

fun formatDurationMs(durationMs: Long): String {
    val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSec / 3600L
    val minutes = (totalSec % 3600L) / 60L
    val seconds = totalSec % 60L
    return if (hours > 0L) {
        "$hours:${padTwoDigits(minutes)}:${padTwoDigits(seconds)}"
    } else {
        "$minutes:${padTwoDigits(seconds)}"
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "${formatDecimal(kb, 0)} KB"
    val mb = kb / 1024.0
    if (mb < 1024.0) return "${formatDecimal(mb, 1)} MB"
    val gb = mb / 1024.0
    return "${formatDecimal(gb, 2)} GB"
}

fun formatAudioTechnicalLabel(
    mimeType: String?,
    displayName: String,
    bitrateKbps: Int? = null,
    sampleRateHz: Int? = null,
): String {
    val codec =
        resolveCodecLabel(mimeType = mimeType, displayName = displayName)
    val parts = buildList {
        if (!codec.isNullOrBlank()) add(codec)
        if (bitrateKbps != null && bitrateKbps > 0) add("$bitrateKbps kbps")
        if (sampleRateHz != null && sampleRateHz > 0) {
            val formattedKhz =
                if (sampleRateHz % 1000 == 0) {
                    "${sampleRateHz / 1000} kHz"
                } else {
                    "${formatDecimal(sampleRateHz / 1000.0, 1)} kHz"
                }
            add(formattedKhz)
        }
    }
    return parts.joinToString(" · ")
}

fun resolveCodecLabel(
    mimeType: String?,
    displayName: String,
): String? {
    val ext = displayName.substringAfterLast('.', "").lowercase()
    return when {
        mimeType?.contains("flac", ignoreCase = true) == true || ext == "flac" -> "FLAC"
        mimeType?.contains("opus", ignoreCase = true) == true || ext == "opus" -> "OPUS"
        mimeType?.contains("wav", ignoreCase = true) == true || mimeType?.contains("x-wav", ignoreCase = true) == true || ext == "wav" -> "WAV"
        mimeType?.contains("aac", ignoreCase = true) == true || ext == "aac" -> "AAC"
        mimeType?.contains("mp4", ignoreCase = true) == true || mimeType?.contains("m4a", ignoreCase = true) == true || ext == "m4a" -> "M4A"
        mimeType?.contains("ogg", ignoreCase = true) == true || mimeType?.contains("vorbis", ignoreCase = true) == true || ext == "ogg" -> "OGG"
        mimeType?.contains("mpeg", ignoreCase = true) == true || mimeType?.contains("mp3", ignoreCase = true) == true || ext == "mp3" -> "MP3"
        mimeType?.contains("amr", ignoreCase = true) == true -> "AMR"
        ext.isNotBlank() && ext.length <= 5 -> ext.uppercase()
        else -> null
    }
}
