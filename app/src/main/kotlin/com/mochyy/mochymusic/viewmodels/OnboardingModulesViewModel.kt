package com.mochyy.mochymusic.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.module.AccountState
import com.mochyy.mochymusic.module.ModuleFeature
import com.mochyy.mochymusic.module.ResolveResult
import com.mochyy.mochymusic.module.TrackMatcher
import com.mochyy.mochymusic.module.TrackQuery
import com.mochyy.mochymusic.modules.DeviceDecoders
import com.mochyy.mochymusic.modules.ModuleEntry
import com.mochyy.mochymusic.modules.SourceModules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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

    fun setEnabled(id: String, enabled: Boolean) = modules.setEnabled(id, enabled)

    internal suspend fun fetch(id: String, track: TrackQuery): ModuleFetchResult = lookupSlots.withPermit {
        if (!modules.isEnabled(id)) return@withPermit ModuleFetchResult.Unavailable
        val module = modules.module(id) ?: return@withPermit ModuleFetchResult.Unavailable
        // The module host has a blocking HTTP API. Bound the workers and the UI's wait independently.
        val result = viewModelScope.async(lookupDispatcher) {
            try {
                when (val resolved = module.resolve(track, modules.playbackPicks(id, module))) {
                    is ResolveResult.Found -> when {
                        !TrackMatcher.isCompatible(track, resolved.stream.matched) -> ModuleFetchResult.NoMatch
                        !DeviceDecoders.canPlay(resolved.stream) -> ModuleFetchResult.Unsupported
                        else -> ModuleFetchResult.Found(resolved.stream.qualityLabel)
                    }
                    is ResolveResult.NotFound -> ModuleFetchResult.NoMatch
                    is ResolveResult.Unavailable -> ModuleFetchResult.Unavailable
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ModuleFetchResult.Unavailable
            }
        }
        try {
            withTimeoutOrNull(18_000L) { result.await() } ?: ModuleFetchResult.Timeout
        } finally {
            result.cancel()
        }
    }
}
