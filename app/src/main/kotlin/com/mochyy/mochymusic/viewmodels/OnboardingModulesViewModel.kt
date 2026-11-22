package com.mochyy.mochymusic.viewmodels

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.module.AccountState
import com.mochyy.mochymusic.module.CatalogTrack
import com.mochyy.mochymusic.module.ModuleFeature
import com.mochyy.mochymusic.module.SourceModule
import com.mochyy.mochymusic.module.TrackMatcher
import com.mochyy.mochymusic.module.TrackQuery
import com.mochyy.mochymusic.module.monochrome.MonochromeModule
import com.mochyy.mochymusic.modules.ModuleEntry
import com.mochyy.mochymusic.modules.SourceModules
import com.mochyy.mochymusic.modules.SourceModules.PlaybackLookup
import com.music.innertube.models.SongItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

internal val onboardingSampleTrack = TrackQuery("One Kiss", listOf("Calvin Harris", "Dua Lipa"), durationMs = 214_000L)

internal sealed interface ModuleFetchResult {
    data object Fetching : ModuleFetchResult
    data class Found(val quality: String) : ModuleFetchResult
    data object NoMatch : ModuleFetchResult
    data object Unavailable : ModuleFetchResult
    data object Timeout : ModuleFetchResult
    data object Unsupported : ModuleFetchResult
}

@HiltViewModel
class OnboardingModulesViewModel @Inject constructor(
    private val modules: SourceModules,
) : ViewModel() {
    val entries = modules.entries

    @OptIn(ExperimentalCoroutinesApi::class)
    private val lookupDispatcher = Dispatchers.IO.limitedParallelism(4)
    private val lookupSlots = Semaphore(4)
    private val suggestionLock = Mutex()
    private val _suggestedSong = MutableStateFlow<SongItem?>(null)
    internal val suggestedSong = _suggestedSong.asStateFlow()
    private var lastSuggestionAttemptMs: Long? = null

    internal suspend fun loadSuggestedSong(): SongItem? = suggestionLock.withLock {
        _suggestedSong.value?.let { return@withLock it }
        val now = SystemClock.elapsedRealtime()
        if (lastSuggestionAttemptMs?.let { now - it < 30_000L } == true) return@withLock null
        val songs = withTimeoutOrNull(6_000L) { searchModuleTestSongs("One Kiss Calvin Harris Dua Lipa") }.orEmpty()
        lastSuggestionAttemptMs = SystemClock.elapsedRealtime()
        val sampleTitle = TrackMatcher.core(onboardingSampleTrack.title)
        val candidates = songs.filter { song ->
            song.artists.any { artist -> onboardingSampleTrack.artists.any { it.equals(artist.name, ignoreCase = true) } } &&
                TrackMatcher.core(song.title) == sampleTitle
        }.associateBy { it.id }
        val match = TrackMatcher.best(onboardingSampleTrack, candidates.values.map { song ->
            CatalogTrack(song.id, song.title, song.artists.joinToString(", ") { it.name }, song.album?.name, song.duration?.times(1_000L))
        })
        match?.let { candidates[it.track.id] }?.also { _suggestedSong.value = it }
    }

    internal fun showInSetup(entry: ModuleEntry): Boolean =
        entry.adds(ModuleFeature.AudioSource) && (entry.module.signIn == null || entry.account is AccountState.SignedIn)

    internal fun canFetch(entry: ModuleEntry): Boolean = entry.enabled || entry.id == MonochromeModule.Id

    fun setEnabled(id: String, enabled: Boolean) = modules.setEnabled(id, enabled)

    internal suspend fun fetch(id: String, track: TrackQuery): ModuleFetchResult = lookupSlots.withPermit {
        val module = modules.module(id)?.takeIf { id == MonochromeModule.Id || modules.isUsable(id) }
            ?: return@withPermit ModuleFetchResult.Unavailable
        val first = fetchOnce(module, track)
        val result = if (first == ModuleFetchResult.Unavailable || first == ModuleFetchResult.Timeout) {
            delay(400)
            fetchOnce(module, track)
        } else first
        currentCoroutineContext().ensureActive()
        if (modules.module(id) !== module) return@withPermit ModuleFetchResult.Unavailable
        if (id == MonochromeModule.Id && result is ModuleFetchResult.Found && !modules.isEnabled(id)) {
            modules.setEnabled(id, true)
        }
        result
    }

    private suspend fun fetchOnce(module: SourceModule, track: TrackQuery): ModuleFetchResult {
        // The module host has a blocking HTTP API. Bound the workers and the UI's wait independently.
        val result = viewModelScope.async(lookupDispatcher) {
            try {
                when (val resolved = modules.lookupForPlayback(module, track)) {
                    is PlaybackLookup.Found -> ModuleFetchResult.Found(resolved.stream.qualityLabel)
                    is PlaybackLookup.NotFound, PlaybackLookup.WrongRecording -> ModuleFetchResult.NoMatch
                    PlaybackLookup.UnsupportedAudio -> ModuleFetchResult.Unsupported
                    is PlaybackLookup.Unavailable -> ModuleFetchResult.Unavailable
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ModuleFetchResult.Unavailable
            }
        }
        return try {
            withTimeoutOrNull(18_000L) { result.await() } ?: ModuleFetchResult.Timeout
        } catch (_: CancellationException) {
            currentCoroutineContext().ensureActive()
            ModuleFetchResult.Unavailable
        } finally {
            result.cancel()
        }
    }
}
