package com.maxrave.simpmusic.localaudio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceAudioLibraryEngineTest {
    private fun sampleTrack(
        id: Long,
        title: String,
        artist: String = "Artist A",
        album: String = "Album 1",
        albumId: Long = 10L,
        durationMs: Long = 180_000L,
        sizeBytes: Long = 5_000_000L,
        dateAddedSeconds: Long = 1_000L,
        trackNumber: Int = 1,
        folderPath: String = "Music/Rock",
        displayName: String = "$title.flac",
        mimeType: String? = "audio/flac",
    ): DeviceAudioTrack =
        DeviceAudioTrack(
            mediaStoreId = id,
            title = title,
            artist = artist,
            album = album,
            albumId = albumId,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            dateAddedSeconds = dateAddedSeconds,
            trackNumber = trackNumber,
            relativePath = "$folderPath/$displayName",
            folderPath = folderPath,
            folderName = DeviceAudioLibraryEngine.folderDisplayName(folderPath),
            displayName = displayName,
            mimeType = mimeType,
        )

    @Test
    fun filters_duration_size_whatsapp_and_hidden_items() {
        val tracks =
            listOf(
                sampleTrack(1L, "Valid Song"),
                sampleTrack(2L, "Short Clip", durationMs = 15_000L),
                sampleTrack(3L, "Tiny File", sizeBytes = 40_000L),
                sampleTrack(
                    4L,
                    "WhatsApp Note",
                    folderPath = "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes",
                ),
                sampleTrack(5L, "Ringtone", folderPath = "Ringtones"),
                sampleTrack(6L, "Manually Hidden"),
                sampleTrack(7L, "Hidden Folder Track", folderPath = "Podcasts/Tmp"),
            )

        val filtered =
            DeviceAudioLibraryEngine.applyFilters(
                allTracks = tracks,
                config =
                    DeviceAudioFilterConfig(
                        minDurationSeconds = 30,
                        minSizeBytes = 100L * 1024L,
                        ignoreSystemAndMessagingFolders = true,
                        hiddenFolderPaths = setOf("Podcasts/Tmp"),
                        hiddenMediaStoreIds = setOf(6L),
                    ),
            )

        assertEquals(listOf(1L), filtered.map { it.mediaStoreId })
    }

    @Test
    fun disables_whatsapp_filter_when_toggled_off() {
        val tracks =
            listOf(
                sampleTrack(
                    1L,
                    "WhatsApp Audio",
                    folderPath = "WhatsApp/Media/WhatsApp Audio",
                    durationMs = 90_000L,
                ),
            )

        val hidden =
            DeviceAudioLibraryEngine.applyFilters(
                tracks,
                DeviceAudioFilterConfig(ignoreSystemAndMessagingFolders = true),
            )
        val shown =
            DeviceAudioLibraryEngine.applyFilters(
                tracks,
                DeviceAudioFilterConfig(ignoreSystemAndMessagingFolders = false),
            )

        assertTrue(hidden.isEmpty())
        assertEquals(listOf(1L), shown.map { it.mediaStoreId })
    }

    @Test
    fun detects_system_and_hidden_dot_folders() {
        assertTrue(DeviceAudioLibraryEngine.isSystemOrJunkFolder("Music/.thumbnails"))
        assertTrue(DeviceAudioLibraryEngine.isSystemOrJunkFolder("Recordings/Call Recordings"))
        assertFalse(DeviceAudioLibraryEngine.isSystemOrJunkFolder("Music/FLAC/Pink Floyd"))
    }

    @Test
    fun sorts_correctly_by_title_recent_artist_album_and_duration() {
        val tracks =
            listOf(
                sampleTrack(1L, "Zebra", artist = "Beta", album = "Zoo", durationMs = 120_000L, dateAddedSeconds = 100L, trackNumber = 10),
                sampleTrack(2L, "Alpha", artist = "Gamma", album = "First", durationMs = 300_000L, dateAddedSeconds = 300L, trackNumber = 2),
                sampleTrack(3L, "Middle", artist = "Alpha", album = "First", durationMs = 200_000L, dateAddedSeconds = 200L, trackNumber = 1),
            )

        assertEquals(
            listOf("Alpha", "Middle", "Zebra"),
            DeviceAudioLibraryEngine.sortTracks(tracks, DeviceAudioSortOrder.TITLE_ASC).map { it.title },
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            DeviceAudioLibraryEngine.sortTracks(tracks, DeviceAudioSortOrder.DATE_ADDED_DESC).map { it.mediaStoreId },
        )
        assertEquals(
            listOf("Alpha", "Beta", "Gamma"),
            DeviceAudioLibraryEngine.sortTracks(tracks, DeviceAudioSortOrder.ARTIST_ASC).map { it.artist },
        )
        assertEquals(
            listOf(3L, 2L, 1L),
            DeviceAudioLibraryEngine.sortTracks(tracks, DeviceAudioSortOrder.ALBUM_ASC).map { it.mediaStoreId },
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            DeviceAudioLibraryEngine.sortTracks(tracks, DeviceAudioSortOrder.DURATION_DESC).map { it.mediaStoreId },
        )
    }

    @Test
    fun searches_and_groups_folders_albums_and_artists() {
        val tracks =
            listOf(
                sampleTrack(1L, "Track 10", artist = "Band X", album = "LP 1", albumId = 10L, trackNumber = 10, folderPath = "Music/Rock"),
                sampleTrack(2L, "Track 01", artist = "Band X", album = "LP 1", albumId = 10L, trackNumber = 1, folderPath = "Music/Rock"),
                sampleTrack(3L, "Track 02", artist = "Band X", album = "LP 1", albumId = 10L, trackNumber = 2, folderPath = "Music/Jazz"),
            )

        assertEquals(
            listOf("Jazz", "Rock"),
            DeviceAudioLibraryEngine.groupByFolder(tracks).map { it.folderName },
        )

        val albums = DeviceAudioLibraryEngine.groupByAlbum(tracks)
        assertEquals(1, albums.size)
        assertEquals(listOf(2L, 3L, 1L), albums.first().tracks.map { it.mediaStoreId })

        val artists = DeviceAudioLibraryEngine.groupByArtist(tracks)
        assertEquals(1, artists.size)
        assertEquals(1, artists.first().albumCount)

        val searched = DeviceAudioLibraryEngine.searchTracks(tracks, "jazz")
        assertEquals(listOf(3L), searched.map { it.mediaStoreId })
    }

    @Test
    fun formats_technical_labels_sizes_and_durations() {
        assertEquals(
            "FLAC · 1411 kbps · 44.1 kHz",
            formatAudioTechnicalLabel("audio/flac", "test.flac", bitrateKbps = 1411, sampleRateHz = 44100),
        )
        assertEquals("4:05", formatDurationMs(245_000L))
        assertEquals("1:01:05", formatDurationMs(3_665_000L))
        assertEquals("500 B", formatFileSize(500L))
        assertEquals("120 KB", formatFileSize(120L * 1024L))
        assertEquals("1.5 MB", formatFileSize((1.5 * 1024 * 1024).toLong()))
    }
}
