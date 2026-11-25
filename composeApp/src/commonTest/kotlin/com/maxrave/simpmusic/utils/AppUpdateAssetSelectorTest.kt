package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.model.update.ReleaseAsset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUpdateAssetSelectorTest {
    private val sampleAssets =
        listOf(
            ReleaseAsset(
                name = "Muzix-full-armeabi-v7a-release.apk",
                downloadUrl = "https://github.com/jpcmpc123/Muzix/releases/download/v1.0.32/Muzix-full-armeabi-v7a-release.apk",
                sizeBytes = 42_211_491L,
                sha256 = "2463fff468efb1452f6d2377a9f3370bb5a66b284f2a0abe7116858dbc7e3494",
            ),
            ReleaseAsset(
                name = "Muzix-full-arm64-v8a-release.apk",
                downloadUrl = "https://github.com/jpcmpc123/Muzix/releases/download/v1.0.32/Muzix-full-arm64-v8a-release.apk",
                sizeBytes = 44_370_000L,
                sha256 = "03d8b35bdcd69162f4b7d9b7c62d7cd6d8247b65b5c35416cc8d0230364f5c9f",
            ),
            ReleaseAsset(
                name = "Muzix-full-x86_64-release.apk",
                downloadUrl = "https://github.com/jpcmpc123/Muzix/releases/download/v1.0.32/Muzix-full-x86_64-release.apk",
                sizeBytes = 50_231_322L,
            ),
            ReleaseAsset(
                name = "Muzix-full-universal-release.apk",
                downloadUrl = "https://github.com/jpcmpc123/Muzix/releases/download/v1.0.32/Muzix-full-universal-release.apk",
                sizeBytes = 105_550_691L,
            ),
        )

    @Test
    fun `selectBestApkAsset chooses device primary ABI first`() {
        val picked = selectBestApkAsset(sampleAssets, listOf("arm64-v8a", "armeabi-v7a"))
        assertNotNull(picked)
        assertEquals("Muzix-full-arm64-v8a-release.apk", picked.name)
    }

    @Test
    fun `selectBestApkAsset picks secondary ABI when primary has no exact APK`() {
        val picked = selectBestApkAsset(sampleAssets, listOf("riscv64", "armeabi-v7a"))
        assertNotNull(picked)
        assertEquals("Muzix-full-armeabi-v7a-release.apk", picked.name)
    }

    @Test
    fun `selectBestApkAsset falls back to universal APK when no ABI matches`() {
        val picked = selectBestApkAsset(sampleAssets, listOf("mips64"))
        assertNotNull(picked)
        assertEquals("Muzix-full-universal-release.apk", picked.name)
    }

    @Test
    fun `selectBestApkAsset rejects non-GitHub or non-HTTPS download URLs`() {
        val untrusted =
            listOf(
                ReleaseAsset(
                    name = "Muzix-full-arm64-v8a-release.apk",
                    downloadUrl = "http://github.com/jpcmpc123/Muzix/releases/download/v1.0.32/app.apk",
                ),
                ReleaseAsset(
                    name = "Muzix-full-universal-release.apk",
                    downloadUrl = "https://evil.example.com/app.apk",
                ),
            )
        assertNull(selectBestApkAsset(untrusted, listOf("arm64-v8a")))
        assertTrue(isAllowedUpdateDownloadUrl("https://objects.githubusercontent.com/github-production-release-asset/123"))
        assertTrue(isAllowedUpdateDownloadUrl("https://release-assets.githubusercontent.com/456"))
        assertFalse(isAllowedUpdateDownloadUrl("https://github.com.evil.test/app.apk"))
    }

    @Test
    fun `formatBytesLabel renders megabytes with one decimal place`() {
        assertEquals("42.3 MB", formatBytesLabel(44_370_000L))
        assertEquals("0 MB", formatBytesLabel(0L))
    }
}
