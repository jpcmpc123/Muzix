package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.model.update.ReleaseAsset

private val ALLOWED_UPDATE_HOSTS =
    setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
    )

/**
 * Picks the smallest matching `.apk` release asset for the device's ABI list, falling back to
 * a universal APK if no ABI-specific build is present.
 */
internal fun selectBestApkAsset(
    assets: List<ReleaseAsset>,
    supportedAbis: List<String>,
): ReleaseAsset? {
    val apkAssets =
        assets.filter {
            it.name.endsWith(".apk", ignoreCase = true) &&
                isAllowedUpdateDownloadUrl(it.downloadUrl)
        }
    if (apkAssets.isEmpty()) return null

    for (abi in supportedAbis) {
        val normalizedAbi = abi.trim().lowercase()
        if (normalizedAbi.isEmpty()) continue
        val exactMatch =
            apkAssets.firstOrNull { asset ->
                val lower = asset.name.lowercase()
                "-$normalizedAbi-" in lower || "-$normalizedAbi." in lower
            }
        if (exactMatch != null) return exactMatch
    }

    return apkAssets.firstOrNull { "universal" in it.name.lowercase() }
        ?: apkAssets.singleOrNull()
}

/**
 * Ensures in-app update downloads only fetch over HTTPS from official GitHub hosts.
 */
internal fun isAllowedUpdateDownloadUrl(url: String): Boolean {
    val trimmed = url.trim()
    if (!trimmed.startsWith("https://", ignoreCase = true)) return false
    val host =
        trimmed
            .substringAfter("://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore(':')
            .lowercase()
    return host in ALLOWED_UPDATE_HOSTS
}

/**
 * Formats byte counts cleanly across all locales without relying on platform `String.format`.
 */
internal fun formatBytesLabel(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val tenthsMb = (bytes * 10L) / (1024L * 1024L)
    val whole = tenthsMb / 10L
    val frac = tenthsMb % 10L
    return "$whole.$frac MB"
}
