package com.mochyy.mochymusic.viewmodels

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.SingletonImageLoader
import coil3.decode.BlackholeDecoder
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.music.innertube.YouTube
import com.music.innertube.models.YTItem
import com.music.innertube.pages.MoodAndGenres
import com.mochyy.mochymusic.constants.MaxImageCacheSizeKey
import com.mochyy.mochymusic.constants.OfflineModeKey
import com.mochyy.mochymusic.ui.utils.resize
import com.mochyy.mochymusic.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private fun coverKey(browseId: String, params: String?): String = "$browseId|${params.orEmpty()}"

private const val SectionCacheAgeMs = 7L * 24 * 60 * 60 * 1_000

internal fun searchTileCoverUrl(url: String): String = url.resize(128, 128)

@Singleton
class SearchBrowseArtwork @Inject constructor(@ApplicationContext private val context: Context) {
    private val disk by lazy { context.getSharedPreferences("search_browse_artwork", Context.MODE_PRIVATE) }
    private val sectionsLock = Mutex()
    private var loadedLocale: String? = null
    private var lastSectionFailureAt: Long? = null
    private val coverSlots = Semaphore(3)
    private val coverLocks = Array(32) { Mutex() }
    private val sections = MutableStateFlow<List<MoodAndGenres>?>(null)
    private val covers = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    val offlineMode = context.dataStore.data.map { it[OfflineModeKey] == true }.distinctUntilChanged()
    val moodAndGenres = sections.asStateFlow()
    val tileCovers = covers.asStateFlow()

    suspend fun loadSections(force: Boolean = false) = sectionsLock.withLock {
        val locale = YouTube.locale.let { "${it.gl}|${it.hl}" }
        val prefs = context.dataStore.data.first()
        val offline = prefs[OfflineModeKey] == true
        val cacheEnabled = prefs[MaxImageCacheSizeKey] != 0
        val sameLocale = loadedLocale == locale
        if (!sameLocale) lastSectionFailureAt = null
        val settled = sections.value?.takeIf { sameLocale && it.isNotEmpty() }
        val saved = if (cacheEnabled && disk.getString("locale", null) == locale) {
            settled ?: disk.getString("sections", null)?.let {
                runCatching { Json.decodeFromString<List<MoodAndGenres>>(it) }.getOrNull()
            }
        } else null
        if (!sameLocale || sections.value.isNullOrEmpty()) sections.value = saved
        loadedLocale = locale
        if (offline) {
            if (sections.value == null) sections.value = emptyList()
            return@withLock
        }
        val age = System.currentTimeMillis() - disk.getLong("savedAt", 0L)
        val recentlyFailed = lastSectionFailureAt?.let { SystemClock.elapsedRealtime() - it < 10_000L } == true
        if (!force && ((settled != null && !cacheEnabled) ||
                (saved != null && age in 0L until SectionCacheAgeMs) || recentlyFailed)
        ) {
            if (sections.value == null) sections.value = emptyList()
            return@withLock
        }
        val fetched = withTimeoutOrNull(12_000) { YouTube.moodAndGenres().getOrNull() }
        currentCoroutineContext().ensureActive()
        if (fetched.isNullOrEmpty()) {
            lastSectionFailureAt = SystemClock.elapsedRealtime()
            if (sections.value == null) sections.value = emptyList()
        } else {
            lastSectionFailureAt = null
            sections.value = fetched
            if (cacheEnabled) {
                disk.edit()
                    .putString("sections", Json.encodeToString(fetched))
                    .putString("locale", locale)
                    .putLong("savedAt", System.currentTimeMillis())
                    .apply()
            }
        }
    }

    suspend fun loadCovers(browseId: String, params: String?): List<String> {
        val key = coverKey(browseId, params)
        val lock = coverLocks[(key.hashCode() and Int.MAX_VALUE) % coverLocks.size]
        return lock.withLock {
            covers.value[key]?.let { return@withLock it }
            val prefs = context.dataStore.data.first()
            val cacheEnabled = prefs[MaxImageCacheSizeKey] != 0
            val saved = if (cacheEnabled) disk.getString("cover:$key", null)?.let {
                runCatching { Json.decodeFromString<List<String>>(it) }.getOrNull()
            }?.takeIf { it.isNotEmpty() } else null
            val urls = when {
                saved != null -> saved
                prefs[OfflineModeKey] == true -> emptyList()
                else -> coverSlots.withPermit {
                    if (context.dataStore.data.first()[OfflineModeKey] == true) return@withPermit emptyList()
                    val result = withTimeoutOrNull(10_000) { YouTube.browse(browseId, params).getOrNull() }
                    currentCoroutineContext().ensureActive()
                    result?.items.orEmpty()
                        .asSequence().flatMap { it.items.asSequence() }
                        .mapNotNull(YTItem::thumbnail).distinct().take(2).toList()
                }
            }
            if (urls.isNotEmpty()) {
                covers.update { it + (key to urls) }
                if (cacheEnabled && saved == null) {
                    disk.edit().putString("cover:$key", Json.encodeToString(urls)).apply()
                }
            }
            urls
        }
    }

    /** Warm tiles serially and share the image URLs and disk cache with the visible Search page. */
    @OptIn(coil3.annotation.ExperimentalCoilApi::class)
    suspend fun warmDiskCache() {
        val prefs = context.dataStore.data.first()
        if (prefs[OfflineModeKey] == true || prefs[MaxImageCacheSizeKey] == 0) return
        loadSections()
        val items = sections.value.orEmpty().flatMap { it.items }
            .distinctBy { coverKey(it.endpoint.browseId, it.endpoint.params) }
        if (items.isEmpty()) return
        val loader = SingletonImageLoader.get(context)
        if (loader.diskCache == null) return
        val warmed = mutableSetOf<String>()
        for (item in items) {
            val current = context.dataStore.data.first()
            if (current[OfflineModeKey] == true || current[MaxImageCacheSizeKey] == 0) return
            for (cover in loadCovers(item.endpoint.browseId, item.endpoint.params)) {
                val url = searchTileCoverUrl(cover)
                val latest = context.dataStore.data.first()
                if (latest[OfflineModeKey] == true || latest[MaxImageCacheSizeKey] == 0) return
                if (!warmed.add(url)) continue
                val cached = loader.diskCache?.openSnapshot(url)
                if (cached != null) {
                    cached.close()
                    continue
                }
                loader.execute(
                    ImageRequest.Builder(context)
                        .data(url)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .decoderFactory(BlackholeDecoder.Factory())
                        .build(),
                )
            }
            delay(150)
        }
    }
}

@HiltViewModel
class MoodAndGenresViewModel @Inject constructor(
    private val artwork: SearchBrowseArtwork,
) : ViewModel() {
    val moodAndGenres = artwork.moodAndGenres
    private var sectionRetry: Job? = null

    fun observeCovers(browseId: String, params: String?) =
        artwork.tileCovers.map { it[coverKey(browseId, params)].orEmpty() }.distinctUntilChanged()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            artwork.offlineMode.collectLatest { artwork.loadSections() }
        }
    }

    fun retrySections() {
        if (sectionRetry?.isActive == true) return
        sectionRetry = viewModelScope.launch(Dispatchers.IO) { artwork.loadSections(force = true) }
    }

    fun requestCovers(browseId: String, params: String?) {
        if (coverKey(browseId, params) in artwork.tileCovers.value) return
        viewModelScope.launch(Dispatchers.IO) { artwork.loadCovers(browseId, params) }
    }
}
