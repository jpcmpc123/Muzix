package com.maxrave.simpmusic.expect

import com.maxrave.domain.data.model.update.ReleaseAsset

sealed interface InAppUpdateState {
    data object Idle : InAppUpdateState

    data class Downloading(
        val assetName: String,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val progress: Float?,
    ) : InAppUpdateState

    data class Verifying(
        val assetName: String,
    ) : InAppUpdateState

    data class ReadyToInstall(
        val filePath: String,
        val assetName: String,
        val needsPermission: Boolean,
    ) : InAppUpdateState

    data class Error(
        val message: String,
    ) : InAppUpdateState
}

expect fun supportsInAppApkUpdate(): Boolean

expect fun deviceSupportedAbis(): List<String>

expect suspend fun downloadApkUpdate(
    asset: ReleaseAsset,
    onState: (InAppUpdateState) -> Unit,
): Result<String>

expect fun canInstallApkUpdates(): Boolean

expect fun openUnknownSourcesSettings(): Boolean

expect fun launchApkInstaller(filePath: String): Boolean

expect fun cleanupOldUpdateApks(keepFileName: String? = null)
