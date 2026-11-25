package com.maxrave.simpmusic.expect

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.maxrave.domain.data.model.update.ReleaseAsset
import com.maxrave.simpmusic.utils.isAllowedUpdateDownloadUrl
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin
import java.io.File
import java.io.IOException
import java.security.MessageDigest

private const val UPDATES_SUBDIR = "updates"
private const val STREAM_BUFFER_BYTES = 32 * 1024
private const val PROGRESS_EMIT_INTERVAL_MS = 150L

private fun appContext(): Context = getKoin().get()

private fun updatesDirectory(context: Context): File =
    File(context.cacheDir, UPDATES_SUBDIR).apply {
        if (!exists()) mkdirs()
    }

private fun sanitizeApkFileName(rawName: String): String {
    val base = rawName.substringAfterLast('/').substringAfterLast('\\')
    return base.replace(Regex("[^A-Za-z0-9._-]"), "_")
}

private fun File.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(STREAM_BUFFER_BYTES)
    inputStream().use { input ->
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

actual fun supportsInAppApkUpdate(): Boolean = true

actual fun deviceSupportedAbis(): List<String> = Build.SUPPORTED_ABIS.toList()

actual suspend fun downloadApkUpdate(
    asset: ReleaseAsset,
    onState: (InAppUpdateState) -> Unit,
): Result<String> =
    withContext(Dispatchers.IO) {
        if (!isAllowedUpdateDownloadUrl(asset.downloadUrl)) {
            return@withContext Result.failure(IllegalArgumentException("Unsupported update URL"))
        }
        val context = appContext()
        val safeName = sanitizeApkFileName(asset.name)
        val dir = updatesDirectory(context)
        val targetFile = File(dir, safeName)
        val tempFile = File(dir, "$safeName.part")

        cleanupOldUpdateApks(keepFileName = safeName)

        // Reuse already verified APK if the user tapped download again or resumed after permission grant.
        if (targetFile.isFile && targetFile.length() > 0L) {
            val expectedSha = asset.sha256
            val reusable =
                if (!expectedSha.isNullOrBlank()) {
                    targetFile.sha256Hex().equals(expectedSha, ignoreCase = true)
                } else {
                    asset.sizeBytes <= 0L || targetFile.length() == asset.sizeBytes
                }
            if (reusable) {
                return@withContext Result.success(targetFile.absolutePath)
            }
            targetFile.delete()
        }

        if (tempFile.exists()) {
            tempFile.delete()
        }

        val client =
            HttpClient(CIO) {
                expectSuccess = false
                followRedirects = true
            }
        try {
            client.prepareGet(asset.downloadUrl) {
                header(HttpHeaders.UserAgent, "Muzix")
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    throw IOException("HTTP ${response.status.value}")
                }
                val totalBytes =
                    response.contentLength()?.takeIf { it > 0L }
                        ?: asset.sizeBytes.takeIf { it > 0L }
                        ?: 0L
                val digest = MessageDigest.getInstance("SHA-256")
                val channel = response.bodyAsChannel()
                val buffer = ByteArray(STREAM_BUFFER_BYTES)
                var downloadedBytes = 0L
                var lastEmitMs = 0L
                var lastPercent = -1

                onState(
                    InAppUpdateState.Downloading(
                        assetName = asset.name,
                        downloadedBytes = 0L,
                        totalBytes = totalBytes,
                        progress = if (totalBytes > 0L) 0f else null,
                    ),
                )

                tempFile.outputStream().buffered(STREAM_BUFFER_BYTES).use { out ->
                    while (true) {
                        val count = channel.readAvailable(buffer, 0, buffer.size)
                        if (count == -1) break
                        if (count == 0) continue
                        digest.update(buffer, 0, count)
                        out.write(buffer, 0, count)
                        downloadedBytes += count

                        val nowMs = System.currentTimeMillis()
                        val percent =
                            if (totalBytes > 0L) {
                                ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }
                        val progressedEnough = percent != lastPercent || (nowMs - lastEmitMs) >= PROGRESS_EMIT_INTERVAL_MS
                        if (progressedEnough) {
                            lastEmitMs = nowMs
                            lastPercent = percent
                            onState(
                                InAppUpdateState.Downloading(
                                    assetName = asset.name,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    progress =
                                        if (totalBytes > 0L) {
                                            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                        } else {
                                            null
                                        },
                                ),
                            )
                        }
                    }
                    out.flush()
                }

                if (downloadedBytes <= 0L) {
                    throw IOException("Empty APK file")
                }

                onState(InAppUpdateState.Verifying(assetName = asset.name))

                val expectedSha = asset.sha256
                if (!expectedSha.isNullOrBlank()) {
                    val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
                    if (!actualSha.equals(expectedSha, ignoreCase = true)) {
                        throw IOException("SHA-256 mismatch")
                    }
                } else if (totalBytes > 0L && downloadedBytes != totalBytes) {
                    throw IOException("Incomplete download ($downloadedBytes / $totalBytes)")
                }

                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
            }
            Result.success(targetFile.absolutePath)
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            Result.failure(e)
        } finally {
            client.close()
        }
    }

actual fun canInstallApkUpdates(): Boolean {
    val context = appContext()
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
        runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
}

actual fun openUnknownSourcesSettings(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val context = appContext()
    val intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri(),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    return runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

actual fun launchApkInstaller(filePath: String): Boolean {
    val context = appContext()
    val file = File(filePath)
    if (!file.isFile) return false
    val uri =
        runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                file,
            )
        }.getOrNull() ?: return false
    val intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    return runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

actual fun cleanupOldUpdateApks(keepFileName: String?) {
    val context = appContext()
    val dir = File(context.cacheDir, UPDATES_SUBDIR)
    if (!dir.isDirectory) return
    val keep = keepFileName?.let(::sanitizeApkFileName)
    dir.listFiles()?.forEach { entry ->
        if (keep == null || entry.name != keep) {
            entry.delete()
        }
    }
}
