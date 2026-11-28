package com.maxrave.simpmusic.expect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.maxrave.simpmusic.localaudio.DeviceAudioTechnicalDetails
import com.maxrave.simpmusic.localaudio.DeviceAudioTrack
import com.maxrave.simpmusic.localaudio.resolveCodecLabel

actual fun isDeviceAudioSupported(): Boolean = false

actual fun hasDeviceAudioPermission(): Boolean = false

@Composable
actual fun rememberDeviceAudioPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): DeviceAudioPermissionRequester =
    remember(onResult) {
        object : DeviceAudioPermissionRequester {
            override fun request() {
                onResult(false)
            }

            override fun openAppSettings() = Unit
        }
    }

actual suspend fun scanDeviceAudioTracks(): List<DeviceAudioTrack> = emptyList()

actual suspend fun readDeviceAudioTechnicalDetails(track: DeviceAudioTrack): DeviceAudioTechnicalDetails =
    DeviceAudioTechnicalDetails(
        displayName = track.displayName,
        fullPathOrRelative = track.relativePath,
        mimeType = track.mimeType,
        codecLabel = resolveCodecLabel(track.mimeType, track.displayName),
        bitrateKbps = track.bitrateKbps,
        sampleRateHz = track.sampleRateHz,
        sizeBytes = track.sizeBytes,
        durationMs = track.durationMs,
        dateAddedSeconds = track.dateAddedSeconds,
    )

actual fun shareDeviceAudioTrack(
    track: DeviceAudioTrack,
    chooserTitle: String,
) = Unit

@Composable
actual fun rememberDeviceAudioDeleteRequester(
    onDeleted: (mediaStoreId: Long) -> Unit,
    onCancelled: () -> Unit,
    onLegacyConfirmationRequired: (DeviceAudioTrack) -> Unit,
): DeviceAudioDeleteRequester =
    remember {
        object : DeviceAudioDeleteRequester {
            override fun requestDelete(track: DeviceAudioTrack) = onCancelled()
        }
    }

actual suspend fun deleteLegacyDeviceAudioTrack(track: DeviceAudioTrack): Boolean = false
