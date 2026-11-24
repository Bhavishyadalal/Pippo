package com.mochyy.mochymusic.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.SimpleCache
import coil3.SingletonImageLoader
import coil3.decode.BlackholeDecoder
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.mochyy.mochymusic.ui.utils.resize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@UnstableApi
internal class PlaybackReadAhead(
    private val cache: SimpleCache,
    private val dataSources: () -> DataSource.Factory,
) {
    data class File(
        val mediaId: String,
        val key: String,
        val uri: Uri,
        val headers: Map<String, String>,
    )

    // Keep separate renditions separate. Permanent downloads live in a different cache.
    private val written = ConcurrentHashMap<String, MutableSet<String>>()

    fun markWritten(mediaId: String, key: String) {
        written.getOrPut(mediaId) { ConcurrentHashMap.newKeySet<String>() }.add(key)
    }

    suspend fun fill(file: File, limit: Long, readToEnd: Boolean = false, allowRead: () -> Boolean) {
        if (limit <= 0 || !allowRead()) return
        val context = currentCoroutineContext()
        runInterruptible(Dispatchers.IO) {
            val size = ContentMetadata.getContentLength(cache.getContentMetadata(file.key))
            val target = if (size > 0) minOf(limit, size) else limit
            val start = cache.getCachedLength(file.key, 0L, target).coerceAtLeast(0)
            if (start >= target || !allowRead()) return@runInterruptible

            val source = dataSources().createDataSource()
            try {
                // An unbounded request for the active track lets Media3 record the real EOF.
                // The read loop still enforces the byte budget.
                val request = DataSpec.Builder().setUri(file.uri).setKey(file.key)
                    .setPosition(start).setHttpRequestHeaders(file.headers)
                    .setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)
                if (!readToEnd) request.setLength(target - start)
                source.open(request.build())
                val buffer = ByteArray(32 * 1024)
                var position = start
                var nextNetworkCheck = start
                var wrote = false
                while (position < target) {
                    context.ensureActive()
                    if (position >= nextNetworkCheck) {
                        if (!allowRead()) break
                        nextNetworkCheck = position + 256 * 1024
                    }
                    val read = source.read(buffer, 0, minOf(buffer.size.toLong(), target - position).toInt())
                    if (read <= 0) break
                    if (!wrote) {
                        markWritten(file.mediaId, file.key)
                        wrote = true
                    }
                    position += read
                }
            } catch (error: IOException) {
                context.ensureActive()
                Timber.d(error, "Couldn't read ahead for %s", file.mediaId)
            } finally {
                runCatching { source.close() }
            }
        }
    }

    suspend fun evict(mediaId: String, allowed: () -> Boolean) {
        runInterruptible(Dispatchers.IO) {
            val keys = written[mediaId] ?: return@runInterruptible
            for (key in keys) {
                if (!allowed()) return@runInterruptible
                try {
                    cache.removeResource(key)
                } catch (error: IOException) {
                    Timber.d(error, "Couldn't remove old read-ahead audio for %s", mediaId)
                }
                keys.remove(key)
            }
            if (keys.isEmpty()) written.remove(mediaId, keys)
        }
    }

    companion object {
        const val UpcomingBytes = 512L * 1024
        const val CurrentBytes = 64L * 1024 * 1024
        const val ArtworkPixels = 1200

        fun artworkUrl(url: String): String {
            // Many videos have an HQ thumbnail without the optional maxres image.
            val size = if (Uri.parse(url).host == "i.ytimg.com" && !url.contains("maxresdefault.jpg")) 480 else ArtworkPixels
            return url.resize(size, size)
        }

        suspend fun warmArtwork(context: Context, url: String) {
            val loader = SingletonImageLoader.get(context)
            val disk = loader.diskCache ?: return
            disk.openSnapshot(url)?.use { return }
            loader.execute(
                ImageRequest.Builder(context)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .decoderFactory(BlackholeDecoder.Factory())
                    .build(),
            )
        }
    }
}
