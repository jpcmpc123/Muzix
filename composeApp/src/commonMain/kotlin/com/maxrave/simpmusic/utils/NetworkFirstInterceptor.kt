package com.maxrave.simpmusic.utils

import coil3.ImageLoader
import coil3.annotation.InternalCoilApi
import coil3.disk.DiskCache
import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.network.CacheNetworkResponse
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.maxrave.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.coroutineContext

/**
 * HTTP(S) images use cache for 24 hours (7 days for immutable Google/YouTube artwork hashes),
 * then try the network with stale-cache fallback. Expiry never deletes an image, and a failed
 * refresh never renews its TTL.
 *
 * When a cached entry already exists on disk/RAM, refreshes stage under a temporary disk key and
 * promote only after decoding succeeds so a 404 or corrupt 200 never overwrites offline fallback.
 * Pure cache misses (no existing fallback) write directly to [diskKey] to avoid a double disk copy
 * and remove [diskKey] if network/decode fails.
 *
 * When [revalidateScope] is provided, expired cache hits return immediately (stale-while-revalidate)
 * while a deduplicated background refresh updates disk & RAM without blocking UI rendering.
 */
class NetworkFirstInterceptor(
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val revalidateScope: CoroutineScope? = null,
    private val imageLoader: () -> ImageLoader,
) : Interceptor {
    init {
        require(ttlMillis >= 0) { "Image cache TTL must not be negative" }
    }

    private class RequestLock {
        val mutex = Mutex()
        var users = 0
    }

    private val requestLocks = mutableMapOf<String, RequestLock>()
    private val revalidatingKeys = mutableSetOf<String>()
    private val registryMutex = Mutex()

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val url = request.data.toString()
        if ((!url.startsWith("https://", ignoreCase = true) && !url.startsWith("http://", ignoreCase = true)) ||
            !request.networkCachePolicy.readEnabled
        ) {
            return chain.proceed()
        }

        val diskKey = request.diskCacheKey ?: url

        // Fast-path for in-memory bitmaps: avoid registryMutex + per-key Mutex + disk I/O
        // during fast scrolling and recompositions.
        if (request.memoryCachePolicy.readEnabled) {
            val ramRequest =
                request
                    .newBuilder()
                    .memoryCachePolicy(
                        if (request.memoryCachePolicy.writeEnabled) CachePolicy.ENABLED else CachePolicy.READ_ONLY,
                    ).diskCachePolicy(CachePolicy.DISABLED)
                    .networkCachePolicy(CachePolicy.DISABLED)
                    .build()
            val ramResult = chain.withRequest(ramRequest).proceed()
            if (ramResult is SuccessResult) {
                val loader = imageLoader()
                val cachedValue = ramResult.memoryCacheKey?.let { loader.memoryCache?.get(it) }
                val memoryCachedAt =
                    cachedValue?.takeIf { it.image === ramResult.image }?.extras?.get(CACHED_AT_KEY) as? Long
                if (isFresh(memoryCachedAt, url)) {
                    return ramResult.copy(request = request)
                }
                if (revalidateScope != null) {
                    scheduleBackgroundRevalidate(chain, request, url, diskKey)
                    return ramResult.copy(request = request)
                }
            }
        }

        return withPerKeyLock(diskKey) {
            loadWithTtl(chain, request, url, diskKey)
        }
    }

    private suspend fun <T> withPerKeyLock(
        diskKey: String,
        block: suspend () -> T,
    ): T {
        // Serialize only requests for the same disk entry so promotion and RAM updates agree.
        // Remove the lock after its last user; browsing new URLs must not grow a permanent map.
        val requestLock =
            registryMutex.withLock {
                requestLocks.getOrPut(diskKey) { RequestLock() }.also { it.users++ }
            }
        try {
            return requestLock.mutex.withLock {
                block()
            }
        } finally {
            if (coroutineContext.isActive) {
                registryMutex.withLock {
                    if (--requestLock.users == 0) requestLocks.remove(diskKey)
                }
            } else {
                withContext(NonCancellable) {
                    registryMutex.withLock {
                        if (--requestLock.users == 0) requestLocks.remove(diskKey)
                    }
                }
            }
        }
    }

    private suspend fun loadWithTtl(
        chain: Interceptor.Chain,
        request: ImageRequest,
        url: String,
        diskKey: String,
    ): ImageResult {
        val loader = imageLoader()
        val diskCache = loader.diskCache
        val cacheRequest =
            request
                .newBuilder()
                .memoryCachePolicy(
                    when {
                        !request.memoryCachePolicy.readEnabled -> CachePolicy.DISABLED
                        request.memoryCachePolicy.writeEnabled -> CachePolicy.ENABLED
                        else -> CachePolicy.READ_ONLY
                    },
                ).diskCachePolicy(if (request.diskCachePolicy.readEnabled) CachePolicy.READ_ONLY else CachePolicy.DISABLED)
                .networkCachePolicy(CachePolicy.DISABLED)
                .build()
        val cachedResult = chain.withRequest(cacheRequest).proceed()
        if (cachedResult is SuccessResult) {
            val cachedValue = cachedResult.memoryCacheKey?.let { loader.memoryCache?.get(it) }
            val memoryCachedAt =
                cachedValue?.takeIf { it.image === cachedResult.image }?.extras?.get(CACHED_AT_KEY) as? Long
            val cachedAtMillis =
                memoryCachedAt
                    ?: readCachedAt(diskCache, cachedResult.diskCacheKey ?: diskKey)
            if (cachedAtMillis != null && memoryCachedAt != cachedAtMillis) {
                // Warming RAM from disk must carry the original age, not start a new TTL.
                updateMemoryEntry(loader.memoryCache, cachedResult, cachedAtMillis = cachedAtMillis)
            }
            if (isFresh(cachedAtMillis, url)) return cachedResult.copy(request = request)
            if (revalidateScope != null) {
                scheduleBackgroundRevalidate(chain, request, url, diskKey)
                return cachedResult.copy(request = request)
            }
        }

        val hasExistingFallback =
            cachedResult is SuccessResult ||
                (!request.diskCachePolicy.readEnabled && hasDiskEntry(diskCache, diskKey))

        if (!hasExistingFallback) {
            return fetchDirectOnMiss(chain, request, loader, diskCache, diskKey)
        }

        return refreshProtectingFallback(
            chain = chain,
            request = request,
            loader = loader,
            diskCache = diskCache,
            diskKey = diskKey,
            cachedResult = cachedResult as? SuccessResult,
            cacheRequest = cacheRequest,
        )
    }

    /**
     * Pure cache miss: there is no existing offline fallback on disk to protect, so download and
     * cache straight under [diskKey] without staging + byte-copy promotion. If the request fails
     * or cannot be decoded, purge [diskKey] so HTTP error payloads are never retained.
     */
    private suspend fun fetchDirectOnMiss(
        chain: Interceptor.Chain,
        request: ImageRequest,
        loader: ImageLoader,
        diskCache: DiskCache?,
        diskKey: String,
    ): ImageResult {
        val networkRequest =
            request
                .newBuilder()
                .memoryCachePolicy(if (request.memoryCachePolicy.writeEnabled) CachePolicy.WRITE_ONLY else CachePolicy.DISABLED)
                .diskCachePolicy(if (request.diskCachePolicy.writeEnabled) CachePolicy.WRITE_ONLY else CachePolicy.DISABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(diskKey)
                .build()

        var succeeded = false
        try {
            val networkResult = chain.withRequest(networkRequest).proceed()
            if (networkResult is SuccessResult) {
                succeeded = true
                updateMemoryEntry(
                    memoryCache = loader.memoryCache,
                    result = networkResult,
                    cachedAtMillis = nowMillis(),
                )
                return networkResult.copy(request = request)
            }

            if (request.diskCachePolicy.writeEnabled) {
                removeDiskEntrySafely(diskCache, diskKey)
            }
            return (networkResult as ErrorResult).copy(request = request)
        } catch (cancelled: CancellationException) {
            if (!succeeded && request.diskCachePolicy.writeEnabled) {
                removeDiskEntrySafely(diskCache, diskKey)
            }
            throw cancelled
        }
    }

    /**
     * Refresh when an existing entry is present: stage under a unique temporary key and promote to
     * [diskKey] only after decoding succeeds so a 404 or corrupt 200 never clobbers the fallback.
     */
    private suspend fun refreshProtectingFallback(
        chain: Interceptor.Chain,
        request: ImageRequest,
        loader: ImageLoader,
        diskCache: DiskCache?,
        diskKey: String,
        cachedResult: SuccessResult?,
        cacheRequest: ImageRequest,
    ): ImageResult {
        val temporaryKey = "coil-network-first:${UUID.randomUUID()}"
        val networkRequest =
            request
                .newBuilder()
                .memoryCachePolicy(if (request.memoryCachePolicy.writeEnabled) CachePolicy.WRITE_ONLY else CachePolicy.DISABLED)
                .diskCachePolicy(if (request.diskCachePolicy.writeEnabled) CachePolicy.WRITE_ONLY else CachePolicy.DISABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(temporaryKey)
                .build()

        try {
            val networkResult = chain.withRequest(networkRequest).proceed()
            if (networkResult is SuccessResult) {
                val cachedAtMillis = nowMillis()
                val savedDiskKey =
                    if (networkResult.diskCacheKey == temporaryKey) {
                        if (diskCache != null && promote(diskCache, temporaryKey, diskKey)) diskKey else null
                    } else {
                        networkResult.diskCacheKey
                    }
                updateMemoryEntry(loader.memoryCache, networkResult, temporaryKey, savedDiskKey, cachedAtMillis)
                return networkResult.copy(request = request, diskCacheKey = savedDiskKey)
            }

            // Force the stale cache on failure without changing its age. Retry cache-only if it
            // was initially missing, in case another producer filled it during the network attempt.
            val fallbackResult = cachedResult ?: chain.withRequest(cacheRequest).proceed()
            return if (fallbackResult is SuccessResult) {
                fallbackResult.copy(request = request)
            } else {
                // Keep the real network error, not the cache-only miss (usually HTTP 504).
                (networkResult as ErrorResult).copy(request = request)
            }
        } catch (cancelled: CancellationException) {
            // The engine may have written RAM just before cancellation prevented it returning a
            // result. Drop only entries still pointing at this request's soon-to-be-deleted file.
            val memoryCache = loader.memoryCache
            if (memoryCache != null) {
                memoryCache.keys.forEach { key ->
                    if (memoryCache[key]?.extras?.values?.any { it == temporaryKey } == true) {
                        memoryCache.remove(key)
                    }
                }
            }
            throw cancelled
        } finally {
            // Cancellation must propagate, but it must not leave completed staging entries behind.
            removeDiskEntrySafely(diskCache, temporaryKey)
        }
    }

    private fun scheduleBackgroundRevalidate(
        chain: Interceptor.Chain,
        request: ImageRequest,
        url: String,
        diskKey: String,
    ) {
        val scope = revalidateScope ?: return
        scope.launch(Dispatchers.IO) {
            val acquired =
                registryMutex.withLock {
                    revalidatingKeys.add(diskKey)
                }
            if (!acquired) return@launch
            try {
                withPerKeyLock(diskKey) {
                    val loader = imageLoader()
                    val diskCache = loader.diskCache
                    val latestCachedAt = readCachedAt(diskCache, diskKey)
                    if (isFresh(latestCachedAt, url)) return@withPerKeyLock
                    val temporaryKey = "coil-network-first:${UUID.randomUUID()}"
                    val refreshRequest =
                        request
                            .newBuilder()
                            .memoryCachePolicy(if (request.memoryCachePolicy.writeEnabled) CachePolicy.WRITE_ONLY else CachePolicy.DISABLED)
                            .diskCachePolicy(if (request.diskCachePolicy.writeEnabled) CachePolicy.WRITE_ONLY else CachePolicy.DISABLED)
                            .networkCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(temporaryKey)
                            .build()
                    try {
                        val result = chain.withRequest(refreshRequest).proceed()
                        if (result is SuccessResult) {
                            val savedDiskKey =
                                if (result.diskCacheKey == temporaryKey) {
                                    if (diskCache != null && promote(diskCache, temporaryKey, diskKey)) diskKey else null
                                } else {
                                    result.diskCacheKey
                                }
                            updateMemoryEntry(loader.memoryCache, result, temporaryKey, savedDiskKey, nowMillis())
                        }
                    } finally {
                        removeDiskEntrySafely(diskCache, temporaryKey)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Logger.w("Coil", "Background image revalidation failed (${error::class.simpleName})")
            } finally {
                withContext(NonCancellable) {
                    registryMutex.withLock {
                        revalidatingKeys.remove(diskKey)
                    }
                }
            }
        }
    }

    private fun effectiveTtlMillis(url: String): Long {
        if (ttlMillis != DEFAULT_TTL_MILLIS) return ttlMillis
        return if (isImmutableArtworkHost(url)) IMMUTABLE_ARTWORK_TTL_MILLIS else ttlMillis
    }

    private fun isImmutableArtworkHost(url: String): Boolean {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        val host = afterScheme.substringBefore('/').substringBefore('?').lowercase()
        return host.endsWith(".googleusercontent.com") ||
            host == "googleusercontent.com" ||
            host.endsWith(".ggpht.com") ||
            host == "ggpht.com" ||
            host.endsWith(".ytimg.com") ||
            host == "ytimg.com"
    }

    private fun isFresh(
        cachedAtMillis: Long?,
        url: String,
    ): Boolean {
        if (cachedAtMillis == null || cachedAtMillis <= 0) return false
        val now = nowMillis()
        val effectiveTtl = effectiveTtlMillis(url)
        return now >= cachedAtMillis && now - cachedAtMillis < effectiveTtl
    }

    private suspend fun hasDiskEntry(
        diskCache: DiskCache?,
        key: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (diskCache == null) return@withContext false
            try {
                diskCache.openSnapshot(key)?.use { true } ?: false
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                false
            }
        }

    private suspend fun removeDiskEntrySafely(
        diskCache: DiskCache?,
        key: String,
    ) {
        withContext(NonCancellable + Dispatchers.IO) {
            try {
                diskCache?.remove(key)
            } catch (error: Exception) {
                Logger.w("Coil", "Could not remove image cache entry (${error::class.simpleName})")
            }
        }
    }

    @OptIn(InternalCoilApi::class)
    private suspend fun readCachedAt(
        diskCache: DiskCache?,
        key: String,
    ): Long? =
        withContext(Dispatchers.IO) {
            if (diskCache == null) return@withContext null
            try {
                diskCache.openSnapshot(key)?.use { snapshot ->
                    diskCache.fileSystem.read(snapshot.metadata) { CacheNetworkResponse.readFrom(this).responseMillis }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                // Unknown/older metadata is stale, not grounds to delete its still-usable image.
                null
            }
        }

    private suspend fun promote(
        diskCache: DiskCache,
        temporaryKey: String,
        diskKey: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val snapshot = diskCache.openSnapshot(temporaryKey) ?: return@withContext false
                snapshot.use {
                    val editor = diskCache.openEditor(diskKey) ?: return@withContext false
                    try {
                        // Copy encoded bytes, not a recompressed bitmap: format and quality stay intact.
                        diskCache.fileSystem.copy(snapshot.metadata, editor.metadata)
                        diskCache.fileSystem.copy(snapshot.data, editor.data)
                        coroutineContext.ensureActive()
                        editor.commit()
                        true
                    } catch (error: Exception) {
                        runCatching { editor.abort() }
                        throw error
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                // Cache write failure must not hide an image that was successfully downloaded.
                Logger.w("Coil", "Could not update image cache (${error::class.simpleName})")
                false
            }
        }

    private fun updateMemoryEntry(
        memoryCache: MemoryCache?,
        result: SuccessResult,
        temporaryKey: String? = null,
        savedDiskKey: String? = result.diskCacheKey,
        cachedAtMillis: Long? = null,
    ) {
        val memoryKey = result.memoryCacheKey ?: return
        val cache = memoryCache ?: return
        val value = cache[memoryKey] ?: return
        if (value.image !== result.image) return

        // Coil keeps the source disk key in value extras. Replace only our unique temporary value,
        // without depending on Coil's private extra names or altering sampling/size information.
        val extras = value.extras.toMutableMap()
        if (temporaryKey != null) {
            value.extras.forEach { (key, extra) ->
                if (extra == temporaryKey) {
                    if (savedDiskKey != null) extras[key] = savedDiskKey else extras.remove(key)
                }
            }
        }
        if (cachedAtMillis != null) extras[CACHED_AT_KEY] = cachedAtMillis
        cache[memoryKey] = MemoryCache.Value(value.image, extras)
    }

    private companion object {
        const val DEFAULT_TTL_MILLIS = 24L * 60 * 60 * 1000
        const val IMMUTABLE_ARTWORK_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000
        const val CACHED_AT_KEY = "simpmusic#image_response_millis"
    }
}
