package com.maxrave.simpmusic.expect

import com.maxrave.domain.data.model.update.ReleaseAsset

actual fun supportsInAppApkUpdate(): Boolean = false

actual fun deviceSupportedAbis(): List<String> = emptyList()

actual suspend fun downloadApkUpdate(
    asset: ReleaseAsset,
    onState: (InAppUpdateState) -> Unit,
): Result<String> = Result.failure(UnsupportedOperationException("In-app APK update is Android-only"))

actual fun canInstallApkUpdates(): Boolean = false

actual fun openUnknownSourcesSettings(): Boolean = false

actual fun launchApkInstaller(filePath: String): Boolean = false

actual fun cleanupOldUpdateApks(keepFileName: String?) = Unit
