package com.maxrave.simpmusic.expect

import androidx.compose.runtime.Composable
import com.maxrave.simpmusic.localaudio.DeviceAudioTechnicalDetails
import com.maxrave.simpmusic.localaudio.DeviceAudioTrack

expect fun isDeviceAudioSupported(): Boolean

expect fun hasDeviceAudioPermission(): Boolean

interface DeviceAudioPermissionRequester {
    fun request()

    fun openAppSettings()
}

@Composable
expect fun rememberDeviceAudioPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): DeviceAudioPermissionRequester

expect suspend fun scanDeviceAudioTracks(): List<DeviceAudioTrack>

expect suspend fun readDeviceAudioTechnicalDetails(track: DeviceAudioTrack): DeviceAudioTechnicalDetails

expect fun shareDeviceAudioTrack(
    track: DeviceAudioTrack,
    chooserTitle: String,
)

interface DeviceAudioDeleteRequester {
    fun requestDelete(track: DeviceAudioTrack)
}

@Composable
expect fun rememberDeviceAudioDeleteRequester(
    onDeleted: (mediaStoreId: Long) -> Unit,
    onCancelled: () -> Unit,
    onLegacyConfirmationRequired: (DeviceAudioTrack) -> Unit,
): DeviceAudioDeleteRequester

expect suspend fun deleteLegacyDeviceAudioTrack(track: DeviceAudioTrack): Boolean
