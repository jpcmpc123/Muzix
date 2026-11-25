package com.maxrave.simpmusic.expect

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.maxrave.domain.data.model.update.ReleaseAsset
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.utils.isAllowedUpdateDownloadUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.mp.KoinPlatform.getKoin
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

private const val TAG = "InAppUpdater"
private const val UPDATES_SUBDIR = "updates"
private const val STREAM_BUFFER_BYTES = 32 * 1024
private const val PROGRESS_EMIT_INTERVAL_MS = 150L

private fun appContext(): Context = getKoin().get()

private fun activityOrAppContext(): Context =
    runCatching { getKoin().get<AppCompatActivity>() }.getOrElse { appContext() }

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
            Logger.e(TAG, "Rejected unsupported update URL for ${asset.name}")
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
                Logger.i(TAG, "Reusing cached verified APK: $safeName (${targetFile.length()} bytes)")
                return@withContext Result.success(targetFile.absolutePath)
            }
            targetFile.delete()
        }

        if (tempFile.exists()) {
            tempFile.delete()
        }

        // Dedicated OkHttpClient with callTimeout = 0 (unlimited total stream duration) so large
        // APKs (~44 MB) never hit Ktor CIO's 15-second default request timeout on slower links.
        val client =
            OkHttpClient
                .Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(0, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        val request =
            Request
                .Builder()
                .url(asset.downloadUrl)
                .header("User-Agent", "Muzix")
                .header("Accept", "application/octet-stream")
                .build()
        val call = client.newCall(request)
        try {
            Logger.i(TAG, "Starting APK download: ${asset.name} (${asset.sizeBytes} bytes)")
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }
                val body = response.body ?: throw IOException("Empty response body")
                val totalBytes =
                    body.contentLength().takeIf { it > 0L }
                        ?: asset.sizeBytes.takeIf { it > 0L }
                        ?: 0L
                val digest = MessageDigest.getInstance("SHA-256")
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

                body.byteStream().use { input ->
                    tempFile.outputStream().buffered(STREAM_BUFFER_BYTES).use { out ->
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
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
                Logger.i(TAG, "APK verified and saved: $safeName ($downloadedBytes bytes)")
            }
            Result.success(targetFile.absolutePath)
        } catch (e: CancellationException) {
            call.cancel()
            tempFile.delete()
            Logger.i(TAG, "APK download cancelled: ${asset.name}")
            throw e
        } catch (e: Exception) {
            call.cancel()
            tempFile.delete()
            val detail = e.message?.takeIf { it.isNotBlank() } ?: e::class.simpleName ?: "Unknown error"
            Logger.e(TAG, "APK download failed for ${asset.name}: ${e::class.simpleName}: $detail")
            Result.failure(IOException(detail, e))
        } finally {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }

actual fun canInstallApkUpdates(): Boolean {
    val context = appContext()
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
        runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
}

actual fun openUnknownSourcesSettings(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val context = activityOrAppContext()
    val intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri(),
        ).apply {
            if (context !is AppCompatActivity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    return runCatching {
        context.startActivity(intent)
        true
    }.onFailure { e ->
        Logger.e(TAG, "Failed to open unknown sources settings: ${e.message}")
    }.getOrDefault(false)
}

actual fun launchApkInstaller(filePath: String): Boolean {
    val context = activityOrAppContext()
    val file = File(filePath)
    if (!file.isFile) {
        Logger.e(TAG, "APK file missing at $filePath")
        return false
    }
    val uri =
        runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                file,
            )
        }.onFailure { e ->
            Logger.e(TAG, "FileProvider URI creation failed: ${e.message}")
        }.getOrNull() ?: return false
    val intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is AppCompatActivity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    return runCatching {
        context.startActivity(intent)
        Logger.i(TAG, "Launched APK installer for ${file.name}")
        true
    }.onFailure { e ->
        Logger.e(TAG, "Failed to launch APK installer: ${e.message}")
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
