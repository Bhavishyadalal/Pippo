package com.mochyy.mochymusic.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.module.AccountState
import com.mochyy.mochymusic.module.ModuleFeature
import com.mochyy.mochymusic.module.SourceModule
import com.mochyy.mochymusic.module.TrackQuery
import com.mochyy.mochymusic.module.monochrome.MonochromeModule
import com.mochyy.mochymusic.modules.ModuleEntry
import com.mochyy.mochymusic.modules.SourceModules
import com.mochyy.mochymusic.modules.SourceModules.PlaybackLookup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

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
