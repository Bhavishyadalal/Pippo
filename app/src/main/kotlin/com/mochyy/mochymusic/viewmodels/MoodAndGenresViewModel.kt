package com.mochyy.mochymusic.viewmodels

import android.content.Context
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun coverKey(browseId: String, params: String?): String = "$browseId|${params.orEmpty()}"

internal fun searchTileCoverUrl(url: String): String = url.resize(128, 128)

@Singleton
class SearchBrowseArtwork @Inject constructor(@ApplicationContext private val context: Context) {
    private val disk by lazy { context.getSharedPreferences("search_browse_artwork", Context.MODE_PRIVATE) }
    private val sectionsLock = Mutex()
    private var loadedLocale: String? = null
    private val coverSlots = Semaphore(3)
    private val activeKeys = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val sections = MutableStateFlow<List<MoodAndGenres>?>(null)
    private val covers = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    val moodAndGenres = sections.asStateFlow()
    val tileCovers = covers.asStateFlow()

    suspend fun loadSections() = sectionsLock.withLock {
        val locale = YouTube.locale.let { "${it.gl}|${it.hl}" }
        if (loadedLocale == locale && sections.value != null) return@withLock
        val prefs = context.dataStore.data.first()
        val saved = if (prefs[MaxImageCacheSizeKey] != 0 && disk.getString("locale", null) == locale) {
            disk.getString("sections", null)?.let {
                runCatching { Json.decodeFromString<List<MoodAndGenres>>(it) }.getOrNull()
            }
        } else null
        sections.value = saved
        loadedLocale = locale
        val age = System.currentTimeMillis() - disk.getLong("savedAt", 0L)
        if ((saved != null && age in 0L until 7L * 24 * 60 * 60 * 1_000) || prefs[OfflineModeKey] == true) {
            return@withLock
        }
        val fetched = withTimeoutOrNull(12_000) { YouTube.moodAndGenres().getOrNull() }
        currentCoroutineContext().ensureActive()
        if (!fetched.isNullOrEmpty()) {
            sections.value = fetched
            if (prefs[MaxImageCacheSizeKey] != 0) {
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
        covers.value[key]?.let { return it }
        if (!activeKeys.add(key)) return emptyList()
        try {
            val prefs = context.dataStore.data.first()
            val cacheEnabled = prefs[MaxImageCacheSizeKey] != 0
            val saved = if (cacheEnabled) disk.getString("cover:$key", null)?.let {
                runCatching { Json.decodeFromString<List<String>>(it) }.getOrNull()
            }?.takeIf { it.isNotEmpty() } else null
            val urls = when {
                saved != null -> saved
                prefs[OfflineModeKey] == true -> emptyList()
                else -> coverSlots.withPermit {
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
            return urls
        } finally {
            activeKeys.remove(key)
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
        val warmed = mutableSetOf<String>()
        for (item in items) {
            val current = context.dataStore.data.first()
            if (current[OfflineModeKey] == true || current[MaxImageCacheSizeKey] == 0) return
            for (cover in loadCovers(item.endpoint.browseId, item.endpoint.params)) {
                val url = searchTileCoverUrl(cover)
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
    val covers = artwork.tileCovers

    init {
        viewModelScope.launch(Dispatchers.IO) { artwork.loadSections() }
    }

    fun requestCovers(browseId: String, params: String?) {
        if (coverKey(browseId, params) in covers.value) return
        viewModelScope.launch(Dispatchers.IO) { artwork.loadCovers(browseId, params) }
    }
}
