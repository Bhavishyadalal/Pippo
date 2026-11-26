package com.mochyy.mochymusic.playback

/**
 * A DASH stream's init and media segments don't carry the song's cache key: Media3 keys each
 * segment request by the segment's own resolved URL (DashUtil.resolveCacheKey). Such a key is
 * an already-resolved address, not a song id, and must never be looked up on YouTube.
 *
 * YouTube song ids, `module|` keys and linked-phone keys never start with a URL scheme.
 */
internal fun isResolvedUrlCacheKey(key: String): Boolean =
    key.startsWith("https://", ignoreCase = true) ||
        key.startsWith("http://", ignoreCase = true) ||
        key.startsWith("data:", ignoreCase = true)


/** Written only for progressive, unlicensed module audio with an exact rendition key. */
internal const val MODULE_OFFLINE_MARKER = "pippo.module.offline.v1"

internal fun moduleCacheMediaId(key: String): String? =
    key.takeIf { it.startsWith("module|") && it.count { ch -> ch == '|' } == 2 }
        ?.substringAfterLast('|')?.takeIf { it.isNotBlank() }

internal fun isCompleteModuleCache(cache: androidx.media3.datasource.cache.SimpleCache, key: String): Boolean =
    runCatching {
        if (moduleCacheMediaId(key) == null) return@runCatching false
        val metadata = cache.getContentMetadata(key)
        val size = androidx.media3.datasource.cache.ContentMetadata.getContentLength(metadata)
        metadata.get(MODULE_OFFLINE_MARKER, 0L) == 1L && size > 0 && cache.isCached(key, 0L, size)
    }.getOrDefault(false)

internal fun completeModuleCacheKey(cache: androidx.media3.datasource.cache.SimpleCache, mediaId: String): String? =
    runCatching {
        cache.keys.asSequence().filter { moduleCacheMediaId(it) == mediaId && isCompleteModuleCache(cache, it) }
            .maxByOrNull { key -> cache.getCachedSpans(key).maxOfOrNull { it.lastTouchTimestamp } ?: 0L }
    }.getOrNull()
