package com.maxrave.simpmusic.recognition

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import com.maxrave.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.koin.mp.KoinPlatform.getKoin
import java.util.Locale
import java.util.UUID
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Listens to the microphone using progressive incremental fingerprinting, then asks Shazam's
 * unofficial API what is playing. Ported from Metrolist (GPL-3.0), whose recognition is based on
 * Aleksey Saenko's MusicRecognizer (https://github.com/aleksey-saenko/MusicRecognizer).
 */
internal object SongRecognizer {
    private const val TAG = "SongRecognition"

    private const val RECORD_SECONDS = 12
    private const val MIN_PEAKS = 12
    private const val LOW_AUDIO_THRESHOLD = 0.008f
    private const val LOW_AUDIO_CHECK_MS = 2_500L

    private val PROGRESSIVE_WINDOWS_MS = longArrayOf(3_500L, 7_000L, 11_500L)

    private val userAgents =
        listOf(
            "Dalvik/2.1.0 (Linux; U; Android 14; Pixel 8 Build/AP1A.240505.004)",
            "Dalvik/2.1.0 (Linux; U; Android 14; SM-S928B Build/UP1A.231005.007)",
            "Dalvik/2.1.0 (Linux; U; Android 13; SM-A546B Build/TP1A.220624.014)",
            "Dalvik/2.1.0 (Linux; U; Android 13; Pixel 7a Build/TQ3A.230901.001)",
            "Dalvik/2.1.0 (Linux; U; Android 12; SM-G991B Build/SP1A.210812.016)",
        )

    private val timezones =
        listOf("Europe/Paris", "Europe/London", "America/New_York", "America/Los_Angeles", "America/Sao_Paulo", "Asia/Tokyo")

    private val json = Json { ignoreUnknownKeys = true }

    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                connectTimeoutMillis = 6_000
                socketTimeoutMillis = 10_000
                requestTimeoutMillis = 12_000
            }
        }
    }

    private sealed interface QueryOutcome {
        data class Found(
            val result: Recognition.Found,
        ) : QueryOutcome

        data class NoMatch(
            val retryMs: Long = 0L,
        ) : QueryOutcome

        data class RateLimited(
            val retryAfterMs: Long,
        ) : QueryOutcome

        data object Error : QueryOutcome
    }

    /**
     * Progressively listens for up to [RECORD_SECONDS] seconds, updating [onAmplitude], [onLowAudio],
     * and [onState] in real time, and stops the microphone the instant Shazam matches.
     */
    suspend fun recognize(
        onAmplitude: (Float) -> Unit = {},
        onLowAudio: (Boolean) -> Unit = {},
        onState: (Recognition) -> Unit = {},
    ): Recognition =
        try {
            recognizeProgressive(onAmplitude, onLowAudio, onState)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, "Recognition failed", e)
            Recognition.Failed
        }

    @SuppressLint("MissingPermission")
    private suspend fun recognizeProgressive(
        onAmplitude: (Float) -> Unit,
        onLowAudio: (Boolean) -> Unit,
        onState: (Recognition) -> Unit,
    ): Recognition =
        coroutineScope {
            val context = runCatching { getKoin().get<Context>() }.getOrNull()
            val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val captureConfig = openAudioRecord(audioManager) ?: error("Microphone unavailable")
            val (recorder, sampleRate, readChunkShorts) = captureConfig

            val focusRequest = requestExclusiveAudioFocus(audioManager)
            val audioChannel = Channel<ShortArray>(capacity = 32)

            val deviceUuid = UUID.randomUUID().toString().uppercase()
            val sessionUuid = UUID.randomUUID().toString()
            val resampler = if (sampleRate != ShazamSignature.SAMPLE_RATE) ShazamSignature.LowPassResampler(sampleRate) else null
            val generator = ShazamSignature.IncrementalSignatureGenerator()

            var inFlight: Deferred<QueryOutcome>? = null
            var windowIndex = 0
            var nextAllowedMs = 0L
            var lastQueryElapsedMs = 0L
            var anyHttpResponse = false
            var matched: Recognition.Found? = null

            val recordJob =
                launch(Dispatchers.IO) {
                    val totalTargetSamples = sampleRate * RECORD_SECONDS
                    var capturedSamples = 0
                    val readBuffer = ShortArray(readChunkShorts)
                    var peakAmplitudeSeen = 0f
                    var reportedLowAudio = false

                    try {
                        recorder.startRecording()
                        while (capturedSamples < totalTargetSamples && isActive) {
                            val remaining = totalTargetSamples - capturedSamples
                            val toRead = minOf(readBuffer.size, remaining)
                            val read = recorder.read(readBuffer, 0, toRead)
                            if (read < 0) error("AudioRecord.read failed: $read")
                            if (read == 0) continue

                            capturedSamples += read
                            val rms = computeNormalizedAmplitude(readBuffer, read)
                            onAmplitude(rms)
                            if (rms > peakAmplitudeSeen) peakAmplitudeSeen = rms
                            val elapsedCaptureMs = capturedSamples * 1000L / sampleRate
                            if (elapsedCaptureMs >= LOW_AUDIO_CHECK_MS && peakAmplitudeSeen < LOW_AUDIO_THRESHOLD && !reportedLowAudio) {
                                reportedLowAudio = true
                                onLowAudio(true)
                            } else if (reportedLowAudio && rms >= LOW_AUDIO_THRESHOLD * 2f) {
                                reportedLowAudio = false
                                onLowAudio(false)
                            }

                            audioChannel.send(readBuffer.copyOf(read))
                        }
                    } finally {
                        audioChannel.close()
                        onAmplitude(0f)
                        runCatching {
                            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                                recorder.stop()
                            }
                        }
                        recorder.release()
                        if (focusRequest != null) {
                            runCatching { audioManager?.abandonAudioFocusRequest(focusRequest) }
                        }
                    }
                }

            try {
                withContext(Dispatchers.Default) {
                    for (chunk in audioChannel) {
                        val samples16k = resampler?.resample(chunk, chunk.size) ?: chunk
                        generator.feedPcm16k(samples16k, samples16k.size)

                        val completed = inFlight?.takeIf { it.isCompleted }
                        if (completed != null) {
                            inFlight = null
                            when (val outcome = completed.await()) {
                                is QueryOutcome.Found -> {
                                    matched = outcome.result
                                    break
                                }
                                is QueryOutcome.NoMatch -> {
                                    anyHttpResponse = true
                                    if (outcome.retryMs > 0L) {
                                        nextAllowedMs = maxOf(nextAllowedMs, lastQueryElapsedMs + outcome.retryMs)
                                    }
                                }
                                is QueryOutcome.RateLimited -> {
                                    anyHttpResponse = true
                                    nextAllowedMs = maxOf(nextAllowedMs, lastQueryElapsedMs + outcome.retryAfterMs)
                                }
                                QueryOutcome.Error -> Unit
                            }
                        }

                        if (inFlight == null &&
                            windowIndex < PROGRESSIVE_WINDOWS_MS.size &&
                            generator.elapsedMs >= maxOf(PROGRESSIVE_WINDOWS_MS[windowIndex], nextAllowedMs) &&
                            generator.peakCount >= MIN_PEAKS
                        ) {
                            val attempt = windowIndex + 1
                            windowIndex++
                            lastQueryElapsedMs = generator.elapsedMs
                            val uri = generator.snapshotUri()
                            onState(Recognition.Identifying(attempt = attempt, maxAttempts = PROGRESSIVE_WINDOWS_MS.size))
                            inFlight =
                                async(Dispatchers.IO) {
                                    askOnce(deviceUuid, sessionUuid, uri, lastQueryElapsedMs)
                                }
                        }
                    }
                }

                if (matched != null) {
                    recordJob.cancel()
                    return@coroutineScope matched!!
                }

                // Wait for any query still running when the 12-second recording finished.
                inFlight?.let { pending ->
                    when (val outcome = pending.await()) {
                        is QueryOutcome.Found -> return@coroutineScope outcome.result
                        is QueryOutcome.NoMatch, is QueryOutcome.RateLimited -> anyHttpResponse = true
                        QueryOutcome.Error -> Unit
                    }
                }

                // Run one final full-sample query if significant new audio accumulated since the last send.
                if (generator.peakCount >= MIN_PEAKS &&
                    (windowIndex == 0 || generator.elapsedMs - lastQueryElapsedMs >= 1_500L)
                ) {
                    val finalAttempt = minOf(windowIndex + 1, PROGRESSIVE_WINDOWS_MS.size)
                    onState(Recognition.Identifying(attempt = finalAttempt, maxAttempts = PROGRESSIVE_WINDOWS_MS.size))
                    val uri = withContext(Dispatchers.Default) { generator.snapshotUri() }
                    when (val outcome = askOnce(deviceUuid, sessionUuid, uri, generator.elapsedMs)) {
                        is QueryOutcome.Found -> return@coroutineScope outcome.result
                        is QueryOutcome.NoMatch, is QueryOutcome.RateLimited -> anyHttpResponse = true
                        QueryOutcome.Error -> Unit
                    }
                }

                when {
                    generator.peakCount < MIN_PEAKS -> Recognition.NoMatch
                    anyHttpResponse -> Recognition.NoMatch
                    else -> Recognition.Failed
                }
            } finally {
                recordJob.cancel()
                inFlight?.cancel()
            }
        }

    private data class CaptureConfig(
        val recorder: AudioRecord,
        val sampleRate: Int,
        val readChunkShorts: Int,
    )

    @SuppressLint("MissingPermission")
    private fun openAudioRecord(audioManager: AudioManager?): CaptureConfig? {
        val supportsUnprocessed =
            audioManager?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val candidates =
            buildList {
                if (supportsUnprocessed) add(MediaRecorder.AudioSource.UNPROCESSED to 16_000)
                add(MediaRecorder.AudioSource.MIC to 16_000)
                if (supportsUnprocessed) add(MediaRecorder.AudioSource.UNPROCESSED to 44_100)
                add(MediaRecorder.AudioSource.MIC to 44_100)
            }

        for ((source, rate) in candidates) {
            val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minBuf <= 0) continue
            // At least 2x minBufferSize and >= 250ms of 16-bit mono audio (aligned to even bytes).
            val bufferBytes = maxOf(minBuf * 2, (rate / 2) and -2)
            val recorder =
                try {
                    AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferBytes)
                } catch (_: IllegalArgumentException) {
                    continue
                }
            if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                // ~100 ms read chunks give responsive RMS meter and prompt cancellation.
                val chunkShorts = minOf(bufferBytes / 2, rate / 10).coerceAtLeast(512)
                return CaptureConfig(recorder, rate, chunkShorts)
            }
            recorder.release()
        }
        return null
    }

    private fun requestExclusiveAudioFocus(audioManager: AudioManager?): AudioFocusRequest? {
        if (audioManager == null) return null
        val attrs =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        val request =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attrs)
                .build()
        return if (runCatching { audioManager.requestAudioFocus(request) }.getOrNull() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            request
        } else {
            null
        }
    }

    private fun computeNormalizedAmplitude(
        samples: ShortArray,
        count: Int,
    ): Float {
        if (count <= 0) return 0f
        var sumSq = 0.0
        for (i in 0 until count) {
            val s = samples[i].toDouble()
            sumSq += s * s
        }
        val rms = sqrt(sumSq / count) / 32768.0
        return (rms * 6.0).coerceIn(0.0, 1.0).toFloat()
    }

    private suspend fun askOnce(
        deviceUuid: String,
        sessionUuid: String,
        signature: String,
        sampleMs: Long,
    ): QueryOutcome {
        val locale = Locale.getDefault()
        val lang = locale.language.takeIf { it.length == 2 } ?: "en"
        val country = locale.country.takeIf { it.length == 2 } ?: "US"
        val contentLanguage = "${lang}_$country"
        return try {
            val response =
                client.post("https://amp.shazam.com/discovery/v5/$lang/$country/android/-/tag/$deviceUuid/$sessionUuid") {
                    parameter("sync", "true")
                    parameter("webv3", "true")
                    parameter("sampling", "true")
                    parameter("connected", "")
                    parameter("shazamapiversion", "v3")
                    parameter("sharehub", "true")
                    parameter("video", "v3")
                    header("User-Agent", userAgents.random())
                    header("Content-Language", contentLanguage)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody(signature, sampleMs))
                }
            when {
                response.status.isSuccess() -> parse(response.bodyAsText())
                response.status == HttpStatusCode.NotFound -> QueryOutcome.NoMatch()
                response.status == HttpStatusCode.TooManyRequests -> {
                    val retryAfterSec = response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()?.coerceIn(1L, 5L)
                    QueryOutcome.RateLimited(retryAfterMs = (retryAfterSec ?: 2L) * 1_000L)
                }
                else -> {
                    Logger.w(TAG, "Shazam answered HTTP ${response.status.value}")
                    QueryOutcome.Error
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG, "Shazam query failed: ${e.message}")
            QueryOutcome.Error
        }
    }

    private fun requestBody(
        signature: String,
        sampleMs: Long,
    ): String {
        val now = System.currentTimeMillis() / 1000
        return buildJsonObject {
            // Shazam asks where and when the tag was taken; it gets a random answer, not the user's.
            putJsonObject("geolocation") {
                put("altitude", Random.nextDouble() * 400 + 100)
                put("latitude", Random.nextDouble() * 180 - 90)
                put("longitude", Random.nextDouble() * 360 - 180)
            }
            putJsonObject("signature") {
                put("samplems", sampleMs)
                put("timestamp", now)
                put("uri", signature)
            }
            put("timestamp", now)
            put("timezone", timezones.random())
        }.toString()
    }

    private fun parse(body: String): QueryOutcome {
        val root = json.parseToJsonElement(body) as? JsonObject ?: return QueryOutcome.NoMatch()
        val retryMs = root["retryms"]?.jsonPrimitive?.longOrNull ?: 0L
        val track = root["track"] as? JsonObject
        val title = track?.get("title")?.jsonPrimitive?.contentOrNull
        if (track == null || title.isNullOrBlank()) {
            return QueryOutcome.NoMatch(retryMs = retryMs)
        }
        Logger.i(TAG, "Recognized ${track["key"]?.jsonPrimitive?.contentOrNull}")
        val images = track["images"] as? JsonObject
        val isrc = track["isrc"]?.jsonPrimitive?.contentOrNull
        var album: String? = null
        var year: String? = null
        var youtubeVideoId: String? = null

        val sections = track["sections"] as? JsonArray
        sections?.forEach { sectionEl ->
            val section = sectionEl as? JsonObject ?: return@forEach
            when (section["type"]?.jsonPrimitive?.contentOrNull) {
                "SONG" -> {
                    val metadata = section["metadata"] as? JsonArray
                    metadata?.forEach { itemEl ->
                        val item = itemEl as? JsonObject ?: return@forEach
                        val metaTitle = item["title"]?.jsonPrimitive?.contentOrNull
                        val metaText = item["text"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                        when (metaTitle) {
                            "Album" -> if (album == null) album = metaText
                            "Released" -> if (year == null) year = metaText
                        }
                    }
                }
                "VIDEO" -> {
                    if (youtubeVideoId == null) {
                        val ytNode = section["youtubeurl"]
                        val ytUrl =
                            when (ytNode) {
                                is kotlinx.serialization.json.JsonPrimitive -> ytNode.contentOrNull
                                is JsonObject ->
                                    (ytNode["actions"] as? JsonArray)
                                        ?.firstNotNullOfOrNull { actionEl ->
                                            ((actionEl as? JsonObject)?.get("uri") as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                                        }
                                else -> null
                            }
                        if (!ytUrl.isNullOrBlank()) {
                            youtubeVideoId = extractYouTubeVideoId(ytUrl)
                        }
                    }
                }
            }
        }

        return QueryOutcome.Found(
            Recognition.Found(
                title = title,
                artist = track["subtitle"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                coverArt = (images?.get("coverarthq") ?: images?.get("coverart"))?.jsonPrimitive?.contentOrNull,
                album = album,
                year = year,
                isrc = isrc,
                youtubeVideoId = youtubeVideoId,
            ),
        )
    }

    private fun extractYouTubeVideoId(url: String): String? {
        val vMatch = Regex("[?&]v=([A-Za-z0-9_-]{11})").find(url)
        if (vMatch != null) return vMatch.groupValues[1]
        val shortMatch = Regex("youtu\\.be/([A-Za-z0-9_-]{11})").find(url)
        return shortMatch?.groupValues?.get(1)
    }
}
