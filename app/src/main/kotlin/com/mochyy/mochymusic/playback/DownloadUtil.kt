// DownloadUtil.kt

package com.mochyy.mochymusic.playback

import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.mochyy.mochymusic.utils.followAppProxy
import com.music.innertube.YouTube
import com.mochyy.mochymusic.constants.AudioQuality
import com.mochyy.mochymusic.constants.DownloadQuality
import com.mochyy.mochymusic.constants.DownloadQualityKey
import com.mochyy.mochymusic.constants.DownloadProviderKey
import com.mochyy.mochymusic.constants.DownloadFolderKey
import com.mochyy.mochymusic.constants.IpVersionKey
import com.mochyy.mochymusic.constants.OfflineModeKey
import com.mochyy.mochymusic.extensions.toEnum
import com.mochyy.mochymusic.module.TrackQuery
import com.mochyy.mochymusic.modules.SourceModules
import com.mochyy.mochymusic.utils.dataStore
import com.music.innertube.models.IpVersion
import okhttp3.Dns
import java.net.InetAddress
import java.net.Inet4Address
import java.net.Inet6Address
import com.mochyy.mochymusic.db.MusicDatabase
import com.mochyy.mochymusic.db.entities.FormatEntity
import com.mochyy.mochymusic.db.entities.LyricsEntity
import com.mochyy.mochymusic.db.entities.Song
import com.mochyy.mochymusic.db.entities.SongEntity
import com.mochyy.mochymusic.lyrics.LyricsHelper
import com.mochyy.mochymusic.models.TransferBatch
import com.mochyy.mochymusic.models.TransferItem
import com.mochyy.mochymusic.models.TransferState
import com.mochyy.mochymusic.models.toMediaMetadata
import com.mochyy.mochymusic.utils.OfflineMode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.mochyy.mochymusic.di.DownloadCache
import com.mochyy.mochymusic.ui.utils.resize
import com.mochyy.mochymusic.utils.YTPlayerUtils
import com.mochyy.mochymusic.utils.enumPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.time.LocalDateTime
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext private val context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: SimpleCache,
    private val sourceModules: SourceModules,
    private val lyricsHelper: LyricsHelper,
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val ipVersion by enumPreference(context, IpVersionKey, IpVersion.AUTO)
    private data class DownloadSelection(val quality: DownloadQuality, val providerId: String?)
    private val selections = java.util.concurrent.ConcurrentHashMap<String, DownloadSelection>()
    private data class YouTubeDownload(
        val url: String,
        val expiresAt: Long,
        val headers: Map<String, String>,
        val quality: DownloadQuality,
        val itag: Int,
    )

    private val songUrlCache = java.util.concurrent.ConcurrentHashMap<String, YouTubeDownload>()

    // The providers each song's download already failed from, so a retry can use another source.
    private val failedProviders = java.util.concurrent.ConcurrentHashMap<String, Set<String>>()

    /** a module's file for each song being downloaded from one, looked up once since the download opens it more than once */
    private class ModuleDownload(
        val providerId: String,
        val url: String,
        val headers: Map<String, String>,
    )

    private val moduleDownloads = java.util.concurrent.ConcurrentHashMap<String, ModuleDownload>()

    // Stored with the cached bytes, so the format guard survives process restarts.
    private val streamIdentityKey = "custom_download_stream_identity"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private class NoLosslessDownload : java.io.IOException("No downloadable FLAC or ALAC stream")
    private class SelectedDownloadUnavailable(val providerId: String, val quality: DownloadQuality) :
        java.io.IOException("The selected download provider has no compatible file for this song")
    private val repairLock = Mutex()
    private val fileOperationLock = Mutex()
    private var locationChangePending = false
    private var downloadPauseHolds = 0
    private var resumeAfterHolds = false
    // DownloadManager commands and their callbacks are serialized on the application thread.
    private val pendingDownloadRequests = mutableSetOf<String>()
    private val unavailableLossless = MutableStateFlow<Set<String>>(emptySet())
    internal val losslessFailures = unavailableLossless.asStateFlow()
    private val _providerFailures = MutableStateFlow<Map<String, Pair<String, DownloadQuality>>>(emptyMap())
    internal val providerFailures = _providerFailures.asStateFlow()

    private val _batch = MutableStateFlow(TransferBatch())

    /**
     * The songs downloaded since the last time nothing was downloading, with how far each has got,
     * for the Library's progress card and sheet. A finished batch stays until the next download
     * starts, so the sheet can still save it as a playlist.
     */
    val batch: StateFlow<TransferBatch> = _batch.asStateFlow()

    // Straight from the network, never through the player's cache. The player caches the stream it
    // played, which is often another format than the download asks for (a lower quality, another
    // client's stream). A download that took the cached start from there and the rest from its own
    // stream was two files glued together: the song played for a few seconds, then went silent.
    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            OkHttpDataSource.Factory(
                OkHttpClient.Builder()
                    .dns(object : Dns {
                        override fun lookup(hostname: String): List<InetAddress> {
                            val addresses = Dns.SYSTEM.lookup(hostname)
                            return when (this@DownloadUtil.ipVersion) {
                                IpVersion.IPV4 -> addresses.filter { it is Inet4Address }.ifEmpty { addresses }
                                IpVersion.IPV6 -> addresses.filter { it is Inet6Address }.ifEmpty { addresses }
                                IpVersion.AUTO -> addresses
                            }
                        }
                    })
                    .followAppProxy()
                    .build(),
            ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val selection = selections.getOrPut(mediaId) {
                val prefs = runBlocking(Dispatchers.IO) { context.dataStore.data.first() }
                DownloadSelection(
                    prefs[DownloadQualityKey].toEnum(DownloadQuality.HIGH),
                    prefs[DownloadProviderKey]?.takeIf(String::isNotBlank),
                )
            }
            val quality = selection.quality
            val lossless = quality == DownloadQuality.LOSSLESS

            // a tv copying a song its linked phone already has takes the phone's file, the very same song and much quicker
            if (!lossless && selection.providerId == null) com.mochyy.mochymusic.link.PhoneFiles.streamUrl(mediaId)?.let {
                rememberDownloadStream(mediaId, "phone:$it", dataSpec.position)
                return@Factory dataSpec.withUri(it.toUri())
            }

            moduleDownload(mediaId, selection)?.let { found ->
                rememberDownloadStream(mediaId, "module:${found.providerId}:${found.url}", dataSpec.position)
                return@Factory dataSpec.buildUpon()
                    .setUri(found.url.toUri())
                    .setHttpRequestHeaders(dataSpec.httpRequestHeaders + found.headers)
                    .build()
            }

            if (selection.providerId != null && (lossless || selection.providerId != SourceModules.YouTubeId)) {
                throw SelectedDownloadUnavailable(selection.providerId, quality)
            }
            if (lossless) throw NoLosslessDownload()

            // only while it still works. This used to keep the url's lifetime instead of when it ends,
            // and compare the wrong way round, so a dead url was reused for every retry of a download
            songUrlCache[mediaId]?.takeIf { it.quality == quality && it.expiresAt > System.currentTimeMillis() }?.let {
                rememberDownloadStream(mediaId, "youtube:${it.itag}", dataSpec.position)
                return@Factory dataSpec
                    .withUri(it.url.toUri())
                    .withRequestHeaders(dataSpec.httpRequestHeaders + it.headers)
            }

            val playbackData = runBlocking(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = quality.youTubeQuality,
                    connectivityManager = connectivityManager,
                )
            }.getOrThrow()
            val format = playbackData.format

            // Picking up part way with a fresh url that gives another format would glue two files
            // together. Fail instead: the failed download is wiped (see forgetStream), so trying
            // again starts the whole song over in one format.
            rememberDownloadStream(mediaId, "youtube:${format.itag}", dataSpec.position)

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.substringAfter("codecs=", "").removeSurrounding("\""),
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        // some formats don't say; the real size is saved when the download finishes.
                        // Asserting it here failed those songs' downloads every time they were tried
                        contentLength = format.contentLength ?: 0L,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb,
                        playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    ),
                )

                val updatedSong = getSongByIdBlocking(mediaId)?.song ?: SongEntity(
                    id = mediaId,
                    title = playbackData.videoDetails?.title ?: "Unknown",
                    duration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0,
                    thumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url?.resize(1200, 1200),
                ).also { insert(it) }

                updatedSong.thumbnailUrl?.let { url ->
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                    SingletonImageLoader.get(context).enqueue(request)
                }
            }

            val streamUrl = playbackData.streamUrl.let {
                "${it}&range=0-${format.contentLength ?: 10000000}"
            }

            songUrlCache[mediaId] = YouTubeDownload(
                url = streamUrl,
                expiresAt = System.currentTimeMillis() + playbackData.streamExpiresInSeconds * 1000L,
                headers = playbackData.streamHeaders,
                quality = quality,
                itag = format.itag,
            )
            dataSpec
                .withUri(streamUrl.toUri())
                .withRequestHeaders(dataSpec.httpRequestHeaders + playbackData.streamHeaders)
        }

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    @OptIn(DelicateCoroutinesApi::class)
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run)
        ).apply {
            maxParallelDownloads = 3
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        val id = download.request.id
                        if (download.state != Download.STATE_REMOVING) pendingDownloadRequests.remove(id)
                        downloads.update { it + (id to download) }
                        val causes = generateSequence<Throwable>(finalException) { it.cause }
                        val providerFailure = causes.filterIsInstance<SelectedDownloadUnavailable>().firstOrNull()
                            ?.takeIf { download.state == Download.STATE_FAILED }
                        val losslessFailed = download.state == Download.STATE_FAILED &&
                            (providerFailure?.quality == DownloadQuality.LOSSLESS || causes.any { it is NoLosslessDownload })
                        unavailableLossless.update { if (losslessFailed) it + id else it - id }
                        _providerFailures.update {
                            if (providerFailure == null) it - id else it + (id to (providerFailure.providerId to providerFailure.quality))
                        }

                        scope.launch {
                            val completed = repairLock.withLock {
                                if (downloads.value[id] !== download) return@withLock false
                                when (download.state) {
                                    Download.STATE_COMPLETED -> {
                                        recordDownloadedLength(download)
                                        database.updateDownloadedInfo(id, true, LocalDateTime.now())
                                        songUrlCache.remove(id)
                                        moduleDownloads.remove(id)
                                        failedProviders.remove(id)
                                        selections.remove(id)
                                    }
                                    Download.STATE_FAILED -> {
                                        database.updateDownloadedInfo(id, false, null)
                                        forgetStream(id)
                                        selections.remove(id)
                                        // Release builds keep this log so a failure has a diagnostic reason.
                                        android.util.Log.w(LOG_TAG, "Download of $id failed", finalException)
                                    }
                                    Download.STATE_STOPPED,
                                    Download.STATE_REMOVING,
                                    Download.STATE_RESTARTING -> database.updateDownloadedInfo(id, false, null)
                                }
                                download.state == Download.STATE_COMPLETED
                            }
                            if (completed) {
                                runCatching {
                                    context.dataStore.data.first()[DownloadFolderKey]?.let { folder ->
                                        DownloadFolderWorker.enqueue(context, id, folder)
                                    }
                                }.onFailure { android.util.Log.w(LOG_TAG, "Couldn't schedule folder copy of $id", it) }
                                saveLyrics(id)
                            }
                        }
                    }

                    override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                        val id = download.request.id
                        pendingDownloadRequests.remove(id)
                        downloads.update { it - id }
                        unavailableLossless.update { it - id }
                        _providerFailures.update { it - id }
                        songUrlCache.remove(id)
                        moduleDownloads.remove(id)
                        failedProviders.remove(id)
                        selections.remove(id)
                        scope.launch {
                            repairLock.withLock {
                                if (downloads.value[id] == null) database.updateDownloadedInfo(id, false, null)
                            }
                        }
                    }
                }
            )
        }

    init {
        val result = mutableMapOf<String, Download>()
        downloadManager.downloadIndex.getDownloads().use { cursor ->
            while (cursor.moveToNext()) {
                result[cursor.download.request.id] = cursor.download
            }
        }
        downloads.value = result

        scope.launch { followBatches() }
        scope.launch { followAutoDownloads(context) }

        // Offline mode and file operations hold the same pause. Turning Offline Mode off
        // cannot restart the writer during a copy, and an existing pause stays in place.
        scope.launch {
            var offlineHold = false
            context.dataStore.data
                .map { it[OfflineModeKey] ?: false }
                .distinctUntilChanged()
                .collect { offline ->
                    withContext(Dispatchers.Main) {
                        if (offline && !offlineHold) holdDownloads()
                        if (!offline && offlineHold) releaseDownloads()
                        offlineHold = offline
                    }
                }
        }
    }

    private fun holdDownloads() {
        if (downloadPauseHolds == 0) {
            resumeAfterHolds = !downloadManager.downloadsPaused
            if (resumeAfterHolds) downloadManager.pauseDownloads()
        }
        downloadPauseHolds++
    }

    private fun releaseDownloads() {
        downloadPauseHolds--
        if (downloadPauseHolds == 0 && resumeAfterHolds) downloadManager.resumeDownloads()
    }

    internal suspend fun <T> withDownloadFilesPaused(
        keepPausedUntilRestart: Boolean = false,
        action: suspend () -> T,
    ): T = fileOperationLock.withLock {
        check(!locationChangePending) { "Downloads are waiting for the app to restart" }
        var held = false
        var complete = false
        try {
            withContext(Dispatchers.Main) {
                holdDownloads()
                held = true
            }
            check(withTimeoutOrNull(10_000L) {
                while (!withContext(Dispatchers.Main) { downloadManager.isIdle }) delay(100L)
                true
            } == true) { "Downloads did not stop writing" }
            action().also {
                complete = true
                if (keepPausedUntilRestart) locationChangePending = true
            }
        } finally {
            if (held && (!complete || !keepPausedUntilRestart)) {
                withContext(Dispatchers.Main + NonCancellable) { releaseDownloads() }
            }
        }
    }

    private fun rememberDownloadStream(mediaId: String, identity: String, position: Long) {
        val previous = downloadCache.getContentMetadata(mediaId).get(streamIdentityKey, "")
        val hasPartialBytes = position > 0L || downloadCache.getCachedSpans(mediaId).any { it.isCached }
        if (hasPartialBytes && previous != identity) {
            // Old downloads without an identity also need a clean retry; their bytes cannot be
            // safely attributed to the newly resolved stream. The failed-download path clears them.
            throw java.io.IOException("Stream changed while resuming $mediaId; retry the download from the start")
        }
        if (previous != identity) {
            downloadCache.applyContentMetadataMutations(
                mediaId,
                ContentMetadataMutations().set(streamIdentityKey, identity),
            )
        }
    }

    // a failed download retries with a fresh stream from the next provider down, and drops its bytes so two formats never get glued together
    private fun forgetStream(mediaId: String) {
        songUrlCache.remove(mediaId)
        moduleDownloads.remove(mediaId)?.let { failed -> failedProviders.merge(mediaId, setOf(failed.providerId)) { a, b -> a + b } }
        runCatching { downloadCache.removeResource(mediaId) }
    }

    /** the first module in the download order with a whole file of [mediaId]; null when youtube comes first or nothing else has it */
    private fun moduleDownload(mediaId: String, selection: DownloadSelection): ModuleDownload? {
        if (selection.providerId == SourceModules.YouTubeId) return null
        moduleDownloads[mediaId]?.let { return it }
        val song = runBlocking(Dispatchers.IO) { database.song(mediaId).first() } ?: return null
        val track = TrackQuery(
            title = song.song.title,
            artists = song.artists.map { it.name },
            album = song.song.albumName ?: song.album?.title,
            durationMs = song.song.duration.takeIf { it > 0 }?.times(1000L),
        )
        val failed = if (selection.providerId == null) failedProviders[mediaId].orEmpty() else emptySet()
        val route = runBlocking(Dispatchers.IO) {
            sourceModules.downloadRoute(track, failed, selection.quality, selection.providerId)
        }
        val (module, stream) = route as? SourceModules.Route.Module ?: return null
        val (mimeType, codecs) = savedFormatOf(stream.codec)

        database.query {
            // the length is filled in when the download finishes: see recordDownloadedLength
            upsert(
                FormatEntity(
                    id = mediaId,
                    itag = -1,
                    mimeType = mimeType,
                    codecs = codecs,
                    bitrate = 0,
                    sampleRate = stream.sampleRateHz,
                    contentLength = 0L,
                    loudnessDb = null,
                    perceptualLoudnessDb = null,
                    playbackUrl = null,
                ),
            )
        }
        return ModuleDownload(module.info.id, stream.url, stream.headers).also { moduleDownloads[mediaId] = it }
    }

    // the saved file as the format badges read it, a mime type and a codec, from the codec the module named
    private fun savedFormatOf(codec: String?): Pair<String, String> = when (codec?.trim()?.uppercase()) {
        "FLAC" -> "audio/flac" to "flac"
        "ALAC" -> "audio/mp4" to "alac"
        "AAC", "HE-AAC" -> "audio/mp4" to "aac"
        "MP3" -> "audio/mpeg" to "mp3"
        "OPUS" -> "audio/ogg" to "opus"
        "E-AC-3", "DOLBY ATMOS" -> "audio/mp4" to "ec-3"
        else -> "audio/*" to codec?.lowercase().orEmpty()
    }

    /** One lyrics lookup at a time: an album finishing downloading would otherwise ask every provider at once per song. */
    private val lyricsLock = Mutex()

    /**
     * A downloaded song's lyrics, saved with it so they're there offline. Lyrics were only ever
     * fetched when a song played with lyrics on, so a download played offline had none. A song
     * that already has lyrics keeps them; one saved as "not found" is asked again.
     */
    private suspend fun saveLyrics(songId: String) {
        if (OfflineMode.isOn) return
        lyricsLock.withLock {
            val stored = database.lyrics(songId).first()
            if (stored != null && stored.lyrics != LyricsEntity.LYRICS_NOT_FOUND) return
            val song = database.song(songId).first() ?: return
            val found = runCatching { lyricsHelper.getLyrics(song.toMediaMetadata()) }.getOrNull() ?: return
            if (found.provider == LyricsHelper.NoConnectionProvider) return
            database.query { upsert(LyricsEntity(id = songId, lyrics = found.lyrics, provider = found.provider)) }
        }
    }

    /**
     * The downloaded file's real size into the song's format record. Playback checks the download
     * holds the whole song against that size, and a FLAC is several times the size YouTube's
     * stream was recorded at.
     */
    private suspend fun recordDownloadedLength(download: Download) {
        val key = download.request.customCacheKey ?: download.request.uri.toString()
        val metadataLength = ContentMetadata.getContentLength(downloadCache.getContentMetadata(key))
        val length = download.contentLength.takeIf { it > 0 }
            ?: metadataLength.takeIf { it > 0 }
            ?: download.bytesDownloaded.takeIf { it > 0 }
            ?: return
        if (metadataLength <= 0 && downloadCache.isCached(key, 0, length)) {
            runCatching {
                val metadata = ContentMetadataMutations.setContentLength(ContentMetadataMutations(), length)
                downloadCache.applyContentMetadataMutations(key, metadata)
            }.onFailure { android.util.Log.w(LOG_TAG, "Couldn't record downloaded length", it) }
        }
        val format = database.format(download.request.id).first() ?: return
        if (format.contentLength != length) database.query { upsert(format.copy(contentLength = length)) }
    }

    private val DownloadQuality.youTubeQuality: AudioQuality
        get() = when (this) {
            DownloadQuality.LOSSLESS -> error("YouTube cannot provide lossless downloads")
            DownloadQuality.HIGH -> AudioQuality.HIGH
            DownloadQuality.MEDIUM -> AudioQuality.MEDIUM
            DownloadQuality.LOW -> AudioQuality.LOW
        }

    /**
     * Keeps [batch] up to date. The manager only reports a download starting, finishing or
     * failing, never how far along it is, so while anything downloads this asks twice a second.
     * Nothing is polled while idle: it waits for [downloads] to show a download under way.
     */
    private suspend fun followBatches() {
        while (true) {
            downloads.first { map -> map.values.any { it.isUnderWay } }
            val ids = LinkedHashSet<String>()
            val songs = HashMap<String, com.mochyy.mochymusic.db.entities.Song?>()
            while (true) {
                // the manager may only be read on the thread it was made on
                val running = kotlinx.coroutines.withContext(Dispatchers.Main) {
                    downloadManager.currentDownloads.filter { it.isUnderWay }
                }
                running.forEach { ids += it.request.id }
                val known = downloads.value
                // cancelled or deleted part way: it leaves the batch rather than showing as failed
                ids.retainAll { id -> known[id]?.state.let { it != null && it != Download.STATE_REMOVING } }
                ids.forEach { id -> if (id !in songs) songs[id] = database.song(id).first() }
                _batch.value = TransferBatch(
                    ids.map { id -> transferItem(id, running.firstOrNull { it.request.id == id } ?: known[id], songs[id]) },
                )
                if (running.isEmpty()) break
                delay(500)
            }
            // a batch that ended on an out-of-date entry in downloads can't spin this loop
            delay(1_000)
        }
    }

    // a song added to a playlist with "download new songs" on, or liked with it on for liked songs, starts downloading as it comes in
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private suspend fun followAutoDownloads(context: Context) {
        context.dataStore.data
            .map { AutoDownloads.marks(it) }
            .distinctUntilChanged()
            .collectLatest { marks ->
                if (marks.isEmpty()) return@collectLatest
                val sources = marks.map { (id, mark) ->
                    if (id == AutoDownloads.LikedId) database.likedSongIdsSince(mark) else database.playlistSongIdsAfter(id, mark)
                }
                combine(
                    combine(sources) { lists -> lists.flatMap { it }.toSet() }.distinctUntilChanged(),
                    context.dataStore.data.map { it[OfflineModeKey] ?: false }.distinctUntilChanged(),
                ) { ids, offline -> ids to offline }
                    .collect { (ids, offline) ->
                        if (!offline) ids.forEach { startAutoDownload(context, it) }
                    }
            }
    }

    private suspend fun startAutoDownload(context: Context, songId: String) {
        if (context.dataStore.data.first()[OfflineModeKey] == true) return
        // already saved, on its way, or stopped by hand: asking again would start a finished one over
        val known = downloads.value[songId]
        if (known != null && known.state != Download.STATE_FAILED) return
        val song = database.song(songId).first() ?: return
        // an imported file has no youtube id to download from
        if (song.song.isLocal) return
        val request = androidx.media3.exoplayer.offline.DownloadRequest.Builder(songId, songId.toUri())
            .setCustomCacheKey(songId)
            .setData(song.song.title.toByteArray())
            .build()
        kotlinx.coroutines.withContext(Dispatchers.Main) {
            val latest = downloads.value[songId]
            if (OfflineMode.isOn || (latest != null && latest.state != Download.STATE_FAILED) ||
                !pendingDownloadRequests.add(songId)
            ) return@withContext

            try {
                downloadManager.addDownload(request)
            } catch (e: Exception) {
                pendingDownloadRequests.remove(songId)
                android.util.Log.w(LOG_TAG, "Couldn't queue automatic download of $songId", e)
                return@withContext
            }
            // The manager owns the request even when Android bars a background service start.
            runCatching { DownloadService.start(context, ExoDownloadService::class.java) }
        }
    }

    private val Download.isUnderWay: Boolean
        get() = state == Download.STATE_QUEUED || state == Download.STATE_DOWNLOADING || state == Download.STATE_RESTARTING

    private fun transferItem(id: String, download: Download?, song: com.mochyy.mochymusic.db.entities.Song?): TransferItem =
        TransferItem(
            key = id,
            title = song?.song?.title
                ?: download?.request?.data?.takeIf { it.isNotEmpty() }?.decodeToString()
                ?: "Song",
            subtitle = song?.artists?.joinToString { it.name }?.ifBlank { null },
            artworkUrl = song?.song?.thumbnailUrl,
            state = when (download?.state) {
                Download.STATE_COMPLETED -> TransferState.Done
                Download.STATE_DOWNLOADING -> TransferState.Running
                Download.STATE_QUEUED, Download.STATE_RESTARTING -> TransferState.Waiting
                else -> TransferState.Failed
            },
            // unknown until the server says how big the file is
            progress = ((download?.percentDownloaded ?: 0f) / 100f).coerceIn(0f, 1f),
            songId = id,
        )

    internal fun needsPlaylistRepair(download: Download?): Boolean = when (download?.state) {
        null, Download.STATE_FAILED -> true
        Download.STATE_COMPLETED -> {
            val cacheKey = download.request.customCacheKey ?: download.request.uri.toString()
            val length = download.contentLength.takeIf { it > 0 }
                ?: ContentMetadata.getContentLength(downloadCache.getContentMetadata(cacheKey)).takeIf { it > 0 }
                ?: download.bytesDownloaded
            length <= 0 || !downloadCache.isCached(cacheKey, 0, length)
        }
        else -> false
    }

    internal suspend fun repairPlaylist(songs: List<Song>): Int =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            repairLock.withLock {
                var submitted = 0
                for (song in songs.distinctBy { it.id }) {
                    if (OfflineMode.isOn) break
                    if (song.song.isLocal) continue
                    val known = downloads.value[song.id]
                    if (!needsPlaylistRepair(known)) continue
                    val canSubmit = kotlinx.coroutines.withContext(Dispatchers.Main) {
                        !OfflineMode.isOn && downloads.value[song.id] === known && song.id !in pendingDownloadRequests
                    }
                    if (!canSubmit) continue
                    if (known?.state == Download.STATE_FAILED) forgetStream(song.id)
                    failedProviders.remove(song.id)
                    val request = known?.request ?: DownloadRequest.Builder(song.id, song.id.toUri())
                        .setCustomCacheKey(song.id)
                        .setData(song.song.title.toByteArray())
                        .build()
                    kotlinx.coroutines.withContext(Dispatchers.Main) submit@{
                        if (OfflineMode.isOn || downloads.value[song.id] !== known ||
                            !pendingDownloadRequests.add(song.id)
                        ) return@submit
                        try {
                            if (known?.state == Download.STATE_COMPLETED) downloadManager.removeDownload(song.id)
                            // Keep remove and add in order on the manager's command queue.
                            downloadManager.addDownload(request)
                            submitted++
                        } catch (e: Exception) {
                            pendingDownloadRequests.remove(song.id)
                            throw e
                        }
                        if (submitted == 1) {
                            runCatching { DownloadService.start(context, ExoDownloadService::class.java) }
                        }
                    }
                }
                submitted
            }
        }

    fun getDownload(songId: String): Flow<Download?> =
        downloads.map { it[songId] }.distinctUntilChanged()

    fun release() {
        scope.cancel()
    }

    private companion object {
        const val LOG_TAG = "MochyDownload"
    }
}
