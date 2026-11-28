package com.maxrave.simpmusic.expect

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.localaudio.DeviceAudioLibraryEngine
import com.maxrave.simpmusic.localaudio.DeviceAudioTechnicalDetails
import com.maxrave.simpmusic.localaudio.DeviceAudioTrack
import com.maxrave.simpmusic.localaudio.resolveCodecLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin
import java.io.File

private const val TAG = "DeviceAudioPlatform"

private fun deviceAppContext(): Context = getKoin().get()

private fun requiredAudioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

actual fun isDeviceAudioSupported(): Boolean = true

actual fun hasDeviceAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        deviceAppContext(),
        requiredAudioPermission(),
    ) == PackageManager.PERMISSION_GRANTED

@Composable
actual fun rememberDeviceAudioPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): DeviceAudioPermissionRequester {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            currentOnResult(granted)
        }
    return remember(context, launcher) {
        object : DeviceAudioPermissionRequester {
            override fun request() {
                if (hasDeviceAudioPermission()) {
                    currentOnResult(true)
                } else {
                    launcher.launch(requiredAudioPermission())
                }
            }

            override fun openAppSettings() {
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                runCatching { context.startActivity(intent) }
            }
        }
    }
}

actual suspend fun scanDeviceAudioTracks(): List<DeviceAudioTrack> =
    withContext(Dispatchers.IO) {
        if (!hasDeviceAudioPermission()) return@withContext emptyList()
        val context = deviceAppContext()
        val tracks = mutableListOf<DeviceAudioTrack>()
        val isQOrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        @Suppress("DEPRECATION")
        val pathColumn =
            if (isQOrAbove) {
                MediaStore.Audio.Media.RELATIVE_PATH
            } else {
                MediaStore.Audio.Media.DATA
            }

        val projection =
            buildList {
                add(MediaStore.Audio.Media._ID)
                add(MediaStore.Audio.Media.TITLE)
                add(MediaStore.Audio.Media.ARTIST)
                add(MediaStore.Audio.Media.ALBUM)
                add(MediaStore.Audio.Media.ALBUM_ID)
                add(MediaStore.Audio.Media.DURATION)
                add(MediaStore.Audio.Media.SIZE)
                add(MediaStore.Audio.Media.DATE_ADDED)
                add(MediaStore.Audio.Media.DATE_MODIFIED)
                add(MediaStore.Audio.Media.TRACK)
                add(MediaStore.Audio.Media.DISPLAY_NAME)
                add(MediaStore.Audio.Media.MIME_TYPE)
                add(pathColumn)
            }.toTypedArray()

        val selection =
            if (isQOrAbove) {
                "${MediaStore.Audio.Media.IS_PENDING} = 0"
            } else {
                null
            }

        @Suppress("DEPRECATION")
        val externalRoot = Environment.getExternalStorageDirectory()?.absolutePath?.trimEnd('/') ?: ""

        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val displayNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val mimeTypeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val pathCol = cursor.getColumnIndex(pathColumn)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val displayName =
                        if (displayNameCol >= 0) {
                            cursor.getString(displayNameCol).orEmpty()
                        } else {
                            ""
                        }
                    val rawTitle =
                        if (titleCol >= 0) {
                            cursor.getString(titleCol).orEmpty().trim()
                        } else {
                            ""
                        }
                    val title =
                        rawTitle.ifBlank {
                            displayName.substringBeforeLast('.').ifBlank { "Track $id" }
                        }
                    val rawArtist =
                        if (artistCol >= 0) {
                            cursor.getString(artistCol).orEmpty().trim()
                        } else {
                            ""
                        }
                    val artist =
                        if (rawArtist.isBlank() || rawArtist == MediaStore.UNKNOWN_STRING) {
                            ""
                        } else {
                            rawArtist
                        }
                    val rawAlbum =
                        if (albumCol >= 0) {
                            cursor.getString(albumCol).orEmpty().trim()
                        } else {
                            ""
                        }
                    val album =
                        if (rawAlbum.isBlank() || rawAlbum == MediaStore.UNKNOWN_STRING) {
                            ""
                        } else {
                            rawAlbum
                        }
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else 0L
                    val durationMs = if (durationCol >= 0) cursor.getLong(durationCol).coerceAtLeast(0L) else 0L
                    val sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
                    val dateAdded = if (dateAddedCol >= 0) cursor.getLong(dateAddedCol).coerceAtLeast(0L) else 0L
                    val dateModified = if (dateModifiedCol >= 0) cursor.getLong(dateModifiedCol).coerceAtLeast(0L) else 0L
                    val trackNum = if (trackCol >= 0) cursor.getInt(trackCol).coerceAtLeast(0) else 0
                    val mimeType = if (mimeTypeCol >= 0) cursor.getString(mimeTypeCol) else null
                    val rawPathValue = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""

                    val normalizedFolderPath =
                        if (isQOrAbove) {
                            DeviceAudioLibraryEngine.normalizeFolderPath(rawPathValue)
                        } else {
                            val parentPath = File(rawPathValue).parent.orEmpty()
                            val relative =
                                if (externalRoot.isNotEmpty() && parentPath.startsWith(externalRoot)) {
                                    parentPath.removePrefix(externalRoot)
                                } else {
                                    parentPath
                                }
                            DeviceAudioLibraryEngine.normalizeFolderPath(relative)
                        }
                    val relativePath =
                        if (normalizedFolderPath == "/") {
                            displayName
                        } else {
                            "$normalizedFolderPath/$displayName"
                        }
                    val folderName = DeviceAudioLibraryEngine.folderDisplayName(normalizedFolderPath)
                    val contentUri =
                        ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id,
                        ).toString()

                    tracks.add(
                        DeviceAudioTrack(
                            mediaStoreId = id,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            durationMs = durationMs,
                            sizeBytes = sizeBytes,
                            dateAddedSeconds = dateAdded,
                            dateModifiedSeconds = dateModified,
                            trackNumber = trackNum,
                            relativePath = relativePath,
                            folderPath = normalizedFolderPath,
                            folderName = folderName,
                            displayName = displayName.ifBlank { title },
                            mimeType = mimeType,
                            contentUri = contentUri,
                        ),
                    )
                }
            }
        }.onFailure { e ->
            Logger.e(TAG, "Failed to scan MediaStore audio: ${e.message}")
        }
        tracks
    }

actual suspend fun readDeviceAudioTechnicalDetails(track: DeviceAudioTrack): DeviceAudioTechnicalDetails =
    withContext(Dispatchers.IO) {
        val context = deviceAppContext()
        val uri = track.contentUri.toUri()
        var bitrateKbps: Int? = track.bitrateKbps
        var sampleRateHz: Int? = track.sampleRateHz
        var mimeType: String? = track.mimeType

        val retriever = MediaMetadataRetriever()
        runCatching {
            retriever.setDataSource(context, uri)
            val rawBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            if (rawBitrate != null && rawBitrate > 0L) {
                bitrateKbps = (rawBitrate / 1000L).toInt().coerceAtLeast(1)
            }
            if (mimeType.isNullOrBlank()) {
                mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            }
        }
        runCatching { retriever.release() }

        if (sampleRateHz == null || bitrateKbps == null) {
            val extractor = MediaExtractor()
            runCatching {
                extractor.setDataSource(context, uri, null)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val trackMime = format.getString(MediaFormat.KEY_MIME)
                    if (trackMime?.startsWith("audio/") == true) {
                        if (mimeType.isNullOrBlank()) mimeType = trackMime
                        if (sampleRateHz == null && format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRateHz = format.getInteger(MediaFormat.KEY_SAMPLE_RATE).takeIf { it > 0 }
                        }
                        if (bitrateKbps == null && format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                            val bps = format.getInteger(MediaFormat.KEY_BIT_RATE)
                            if (bps > 0) bitrateKbps = (bps / 1000).coerceAtLeast(1)
                        }
                        break
                    }
                }
            }
            runCatching { extractor.release() }
        }

        DeviceAudioTechnicalDetails(
            displayName = track.displayName,
            fullPathOrRelative = track.relativePath,
            mimeType = mimeType,
            codecLabel = resolveCodecLabel(mimeType, track.displayName),
            bitrateKbps = bitrateKbps,
            sampleRateHz = sampleRateHz,
            sizeBytes = track.sizeBytes,
            durationMs = track.durationMs,
            dateAddedSeconds = track.dateAddedSeconds,
        )
    }

actual fun shareDeviceAudioTrack(
    track: DeviceAudioTrack,
    chooserTitle: String,
) {
    val context = deviceAppContext()
    val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = track.mimeType?.takeIf { it.startsWith("audio/") } ?: "audio/*"
            putExtra(Intent.EXTRA_STREAM, track.contentUri.toUri())
            putExtra(Intent.EXTRA_TITLE, track.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    val chooser =
        Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    runCatching { context.startActivity(chooser) }
        .onFailure { Logger.e(TAG, "Failed to share audio track: ${it.message}") }
}

@Composable
actual fun rememberDeviceAudioDeleteRequester(
    onDeleted: (mediaStoreId: Long) -> Unit,
    onCancelled: () -> Unit,
    onLegacyConfirmationRequired: (DeviceAudioTrack) -> Unit,
): DeviceAudioDeleteRequester {
    val context = LocalContext.current
    val currentOnDeleted by rememberUpdatedState(onDeleted)
    val currentOnCancelled by rememberUpdatedState(onCancelled)
    val currentOnLegacyConfirmation by rememberUpdatedState(onLegacyConfirmationRequired)
    var pendingDeleteTrackId by remember { mutableStateOf<Long?>(null) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val trackId = pendingDeleteTrackId
            pendingDeleteTrackId = null
            if (result.resultCode == Activity.RESULT_OK && trackId != null) {
                currentOnDeleted(trackId)
            } else {
                currentOnCancelled()
            }
        }

    return remember(context, launcher) {
        object : DeviceAudioDeleteRequester {
            override fun requestDelete(track: DeviceAudioTrack) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    runCatching {
                        val pendingIntent =
                            MediaStore.createDeleteRequest(
                                context.contentResolver,
                                listOf(track.contentUri.toUri()),
                            )
                        pendingDeleteTrackId = track.mediaStoreId
                        launcher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    }.onFailure { e ->
                        pendingDeleteTrackId = null
                        Logger.e(TAG, "createDeleteRequest failed: ${e.message}")
                        currentOnCancelled()
                    }
                } else {
                    currentOnLegacyConfirmation(track)
                }
            }
        }
    }
}

actual suspend fun deleteLegacyDeviceAudioTrack(track: DeviceAudioTrack): Boolean =
    withContext(Dispatchers.IO) {
        val context = deviceAppContext()
        runCatching {
            val rows = context.contentResolver.delete(track.contentUri.toUri(), null, null)
            rows > 0
        }.getOrElse { e ->
            Logger.e(TAG, "Legacy delete failed: ${e.message}")
            false
        }
    }
