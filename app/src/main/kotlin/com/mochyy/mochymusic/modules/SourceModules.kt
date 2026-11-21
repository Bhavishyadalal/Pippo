// where songs play and download from: the provider order and the download order over youtube and the modules that add audio, and the walk down each

package com.mochyy.mochymusic.modules

import android.content.Context
import androidx.core.content.edit
import com.mochyy.mochymusic.module.AccountState
import com.mochyy.mochymusic.module.ModuleFeature
import com.mochyy.mochymusic.module.ResolveResult
import com.mochyy.mochymusic.module.SourceModule
import com.mochyy.mochymusic.module.SourceStream
import com.mochyy.mochymusic.module.StreamFormat
import com.mochyy.mochymusic.module.TrackMatcher
import com.mochyy.mochymusic.module.TrackQuery
import com.mochyy.mochymusic.module.applemusic.AppleMusicModule
import com.mochyy.mochymusic.module.monochrome.MonochromeModule
import com.mochyy.mochymusic.utils.DataSaver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceModules @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: ModuleRegistry,
) {
    private val prefs = context.getSharedPreferences("source_modules", Context.MODE_PRIVATE)

    // where modules look songs up, apart from whoever asked, so a call that won't stop never keeps a song waiting
    private val resolveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---- provider order -----------------------------------------------------------------------
    //
    // The order songs are looked for in: the modules and YouTube Music. Each song plays from the
    // first switched-on provider that has it. YouTube is on unless the user turns it off, and then
    // a song none of the modules have is skipped rather than quietly played from YouTube.

    private val _order = MutableStateFlow(readOrder())

    val order: StateFlow<List<String>> = _order.asStateFlow()

    private fun readOrder(): List<String> {
        var saved = prefs.getString("provider_order", null)?.split(',')?.filter { it.isNotBlank() }.orEmpty()
        // Monochrome is preferred when it has the song (lossless): first by default, and moved to
        // the top once for orders saved before it was
        if (!prefs.getBoolean("order_monochrome_first", false)) {
            saved = listOf(MonochromeModule.Id) + saved.filter { it != MonochromeModule.Id }
            prefs.edit {
                putString("provider_order", (saved + KnownOrder).distinct().joinToString(","))
                putBoolean("order_monochrome_first", true)
            }
        }
        return (saved + KnownOrder).distinct()
    }

    fun setOrder(ids: List<String>) {
        val full = (ids + _order.value).distinct()
        prefs.edit { putString("provider_order", full.joinToString(",")) }
        _order.value = full
    }

    /** a new order for the providers a page shows; the ones it hides (modules switched off) keep their places */
    fun setVisibleOrder(visible: List<String>) = setOrder(reorderVisible(_order.value, visible))

    // downloads walk their own order, started as a copy of the playback one, since a kept file may want another source than what plays
    private val _downloadOrder = MutableStateFlow(readDownloadOrder())

    val downloadOrder: StateFlow<List<String>> = _downloadOrder.asStateFlow()

    private fun readDownloadOrder(): List<String> {
        val saved = prefs.getString("download_order", null)?.split(',')?.filter { it.isNotBlank() }
        if (saved != null) return (saved + KnownOrder).distinct()
        val copy = _order.value
        prefs.edit { putString("download_order", copy.joinToString(",")) }
        return copy
    }

    fun setDownloadOrder(ids: List<String>) {
        val full = (ids + _downloadOrder.value).distinct()
        prefs.edit { putString("download_order", full.joinToString(",")) }
        _downloadOrder.value = full
    }

    fun setVisibleDownloadOrder(visible: List<String>) = setDownloadOrder(reorderVisible(_downloadOrder.value, visible))

    private fun reorderVisible(current: List<String>, visible: List<String>): List<String> {
        val shown = visible.toSet()
        val reordered = ArrayDeque(visible)
        return current.map { if (it in shown && reordered.isNotEmpty()) reordered.removeFirst() else it } + reordered
    }

    /** Health reports from account-free providers are advisory. Only sign-in can block a lookup. */
    fun isUsable(id: String): Boolean {
        if (!isEnabled(id)) return false
        if (id == YouTubeId) return true
        val source = module(id) ?: return false
        return source.signIn == null || runCatching { source.accountState() }.getOrNull() is AccountState.SignedIn
    }

    /** whether [id] can give a download: youtube always, since downloads have always fallen back to it, a module when on and signed in */
    fun isUsableForDownload(id: String): Boolean = id == YouTubeId || isUsable(id)

    fun nameOf(id: String): String = if (id == YouTubeId) "YouTube Music" else module(id)?.info?.name ?: id

    /** where a song plays from */
    sealed interface Route {
        data class Module(val module: SourceModule, val stream: SourceStream, val selectedQuality: String?) : Route
        /** hand it to YouTube, by its video id */
        data object YouTube : Route
        /** No provider could open the song. A timed-out or unavailable provider may work on retry. */
        data class Exhausted(val temporaryFailure: Boolean) : Route
    }

    /**
     * Walks the provider order for [track], passing over the providers in [skip] (the ones that
     * already failed on this song). YouTube in the order means "play it from YouTube now".
     * [onUndecodable] hears of each stream passed over because this phone has no decoder for it.
     */
    suspend fun route(
        track: TrackQuery?,
        skip: Set<String>,
        onUndecodable: (id: String, stream: SourceStream) -> Unit = { _, _ -> },
    ): Route = walk(_order.value, track, skip, ::isUsable, { id, module -> playbackPicks(id, module) }) { id, stream ->
        // a stream this phone has no decoder for would play in silence: the next provider has it
        DeviceDecoders.canPlay(stream).also { playable ->
            if (!playable) {
                android.util.Log.i(Tag, "$id: ${track?.title}: this phone can't decode ${stream.codec} at ${stream.sampleRateHz} Hz, passing over")
                onUndecodable(id, stream)
            }
        }
    }

    /** The first compatible whole file from the download order. */
    suspend fun downloadRoute(track: TrackQuery?, skip: Set<String>, lossless: Boolean = false): Route =
        walk(
            _downloadOrder.value,
            track,
            if (lossless) skip + YouTubeId else skip,
            ::isUsableForDownload,
            { id, module ->
                val selected = picks(id)
                if (!lossless) {
                    selected
                } else {
                    val quality = module.choices.firstOrNull { it.id.contains("quality", ignoreCase = true) }
                    val option = quality?.options?.firstOrNull { option ->
                        val name = "${option.id} ${option.label}".lowercase()
                        ("lossless" in name || "flac" in name || "alac" in name) && "lossy" !in name
                    }
                    if (quality != null && option != null) selected + (quality.id to option.id) else selected
                }
            },
        ) { id, stream ->
            (stream.format == StreamFormat.Progressive && stream.drm == null &&
                (!lossless || stream.isLosslessDownload) && DeviceDecoders.canPlay(stream)).also { savable ->
                if (!savable) android.util.Log.i(Tag, "$id: ${track?.title}: can't download a ${stream.format} ${stream.codec} stream at the requested quality")
            }
        }

    private suspend fun walk(
        order: List<String>,
        track: TrackQuery?,
        skip: Set<String>,
        usable: (String) -> Boolean,
        picksFor: (id: String, module: SourceModule) -> Map<String, String>,
        accept: (id: String, stream: SourceStream) -> Boolean,
    ): Route = withContext(Dispatchers.IO) {
        // the providers worth asking, in order, up to youtube, which needs no asking
        val asked = mutableListOf<String>()
        for (id in order) {
            if (id in skip || !usable(id)) continue
            asked += id
            if (id == YouTubeId) break
        }
        // every module ahead of youtube is asked at once, so a slow one up front no longer holds up the rest; answers are still taken in order
        val startedAt = android.os.SystemClock.elapsedRealtime()
        val answers = if (track == null) emptyMap() else asked.mapNotNull { id ->
            val module = module(id) ?: return@mapNotNull null
            val response = resolveScope.async {
                runCatching {
                    val picks = picksFor(id, module)
                    val quality = streamQuality(module, picks)
                    module.resolve(track, picks) to quality
                }
                    .onFailure { if (it !is CancellationException) android.util.Log.w(Tag, "$id failed for ${track.title}", it) }
                    .getOrNull()
            }
            id to (module to response)
        }.toMap()
        var temporaryFailure = false
        try {
            for (id in asked) {
                if (!usable(id)) continue
                if (id == YouTubeId) return@withContext Route.YouTube
                val (module, answer) = answers[id] ?: continue
                // timed on the waiting side, so a module stuck in a call that ignores cancelling can't hold the song either
                val left = ModuleTimeoutMs - (android.os.SystemClock.elapsedRealtime() - startedAt)
                val response = try {
                    withTimeoutOrNull(left.coerceAtLeast(1L)) { answer.await() }
                } catch (_: CancellationException) {
                    currentCoroutineContext().ensureActive()
                    null
                }
                // A provider can be disabled or replaced while its lookup is in flight.
                if (!usable(id) || module(id) !== module) continue
                if (response == null) {
                    temporaryFailure = true
                    if (answer.isActive) android.util.Log.i(Tag, "$id took longer than ${ModuleTimeoutMs / 1000} s for ${track?.title}, moving on")
                    continue
                }
                val (result, quality) = response
                when (result) {
                    is ResolveResult.Found -> {
                        val matched = result.stream.matched
                        if (track != null && !TrackMatcher.isCompatible(track, matched)) {
                            android.util.Log.i(Tag, "$id: ${track.title} did not match ${matched.title}")
                            continue
                        }
                        if (!accept(id, result.stream)) continue
                        android.util.Log.i(Tag, "$id: ${track?.title} -> ${result.stream.matched.title} (${result.stream.qualityLabel})")
                        return@withContext Route.Module(module, result.stream, quality)
                    }
                    is ResolveResult.NotFound -> android.util.Log.i(Tag, "$id: ${track?.title}: ${result.reason}")
                    is ResolveResult.Unavailable -> {
                        temporaryFailure = true
                        android.util.Log.i(Tag, "$id unavailable for ${track?.title}: ${result.reason}")
                    }
                }
            }
            Route.Exhausted(temporaryFailure)
        } finally {
            // the ones further down the order aren't needed once one has the song
            answers.values.forEach { (_, answer) -> answer.cancel() }
        }
    }

    /** every module, whatever it adds; playback only looks at the ones that add audio */
    val entries: StateFlow<List<ModuleEntry>> = registry.entries

    /** a module songs can play from; null for youtube and for modules that only add lyrics or a look */
    fun module(id: String): SourceModule? =
        (registry.module(id) as? SourceModule)?.takeIf { ModuleFeature.AudioSource in it.adds }

    fun isEnabled(id: String): Boolean = registry.isEnabled(id)

    private val _youTubeEnabled = MutableStateFlow(isEnabled(YouTubeId))

    /** youtube music isn't a module, so it has no entry: the settings watch its switch here */
    val youTubeEnabled: StateFlow<Boolean> = _youTubeEnabled.asStateFlow()

    fun setEnabled(id: String, enabled: Boolean) {
        registry.setEnabled(id, enabled)
        if (id == YouTubeId) _youTubeEnabled.value = enabled
        refresh()
    }

    fun picks(id: String): Map<String, String> = registry.picks(id)

    /**
     * the picks a song plays with: with data saver on, a module's stream quality steps down to its
     * "high" option (or one below its best when it has none), and a lighter pick stays as it is
     */
    fun qualityChoice(module: SourceModule) = module.choices.firstOrNull { it.id == QualityChoice }
        ?: module.choices.firstOrNull { it.id.contains(QualityChoice, ignoreCase = true) }

    fun streamQuality(module: SourceModule, picks: Map<String, String>): String? =
        qualityChoice(module)?.let { picks[it.id] ?: it.options.firstOrNull()?.id }

    fun playbackPicks(id: String, module: SourceModule, requestedOption: Pair<String, String>? = null): Map<String, String> {
        val saved = picks(id)
        val picks = if (requestedOption == null) saved else saved + requestedOption
        if (!DataSaver.isActive(context)) return picks
        val choice = qualityChoice(module) ?: return picks
        val quality = choice.options
        val cap = quality.indexOfFirst { it.id == SaverOption }.takeIf { it >= 0 } ?: 1
        val picked = quality.indexOfFirst { it.id == picks[choice.id] }.takeIf { it >= 0 } ?: 0
        if (picked >= cap || cap >= quality.size) return picks
        return picks + (choice.id to quality[cap].id)
    }

    fun setPick(id: String, choiceId: String, optionId: String) = registry.setPick(id, choiceId, optionId)

    fun refresh() {
        registry.refresh()
        val installed = registry.all().filter { ModuleFeature.AudioSource in it.adds }.map { it.info.id }
        slotIn(installed, _order.value, ::setOrder)
        slotIn(installed, _downloadOrder.value, ::setDownloadOrder)
    }

    // a module installed since goes into an order just ahead of youtube
    private fun slotIn(installed: List<String>, current: List<String>, save: (List<String>) -> Unit) {
        val missing = installed.filter { it !in current }
        if (missing.isEmpty()) return
        val at = current.indexOf(YouTubeId).let { if (it < 0) current.size else it }
        save(current.take(at) + missing + current.drop(at))
    }

    companion object {
        private const val Tag = "SourceModules"
        // how long a module may take to find a song before the next provider in the order plays it
        private const val ModuleTimeoutMs = 12_000L
        // the choice every module names its stream quality with, and the option data saver steps down to
        const val QualityChoice = "quality"
        private const val SaverOption = "high"
        /** youtube music in the provider order: not a module, and needs no account */
        const val YouTubeId = "youtube"
        // modules added later (jiosaavn, link modules) aren't listed: refresh slots them in just ahead of youtube, where they get asked
        private val KnownOrder = listOf(MonochromeModule.Id, AppleMusicModule.Id, YouTubeId)
    }
}

internal val SourceStream.isLosslessDownload: Boolean
    get() = when (codec?.trim()?.uppercase()) {
        "FLAC", "ALAC" -> true
        else -> false
    }
