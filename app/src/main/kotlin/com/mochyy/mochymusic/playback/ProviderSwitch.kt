// ProviderSwitch.kt

package com.mochyy.mochymusic.playback

import com.mochyy.mochymusic.constants.AudioQuality
import com.mochyy.mochymusic.db.entities.FormatEntity
import com.mochyy.mochymusic.module.SourceStream
import com.mochyy.mochymusic.utils.YTPlayerUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/** where a song is playing from right now, for stream info's provider row */
data class PlayingFrom(
    /** a module id, "youtube", or one of [Device] and [Downloads] */
    val providerId: String,
    val name: String,
    /** the module's own words for the stream ("flac 24-bit"), when it said */
    val quality: String? = null,
    /** what the module matched the song to, when it's a module */
    val matchedId: String? = null,
    val selectedOption: String? = null,
    val manualYouTubeFormat: FormatEntity? = null,
) {
    companion object {
        const val Device = "device"
        const val Downloads = "downloads"
    }
}

/** A manual route for one song, with the answer from its preflight lookup if available. */
data class ForcedRoute(
    val providerId: String,
    val freshStream: SourceStream? = null,
    val option: Pair<String, String>? = null,
    val youtubeQuality: AudioQuality? = null,
    val freshYouTube: YTPlayerUtils.PlaybackData? = null,
    val expectedDurationMs: Long? = null,
    val selectedModuleQuality: String? = null,
) {
    val isPreflight: Boolean get() = freshStream != null || freshYouTube != null
}

/** what "force change" in stream info is doing */
sealed interface ProviderSwitchState {
    data object Idle : ProviderSwitchState
    data class Working(val providerId: String, val attempt: Int, val of: Int) : ProviderSwitchState
    data class Starting(val providerId: String) : ProviderSwitchState
    data class Failed(val providerId: String, val attempts: Int, val reason: String? = null) : ProviderSwitchState
    data class Done(val providerId: String) : ProviderSwitchState
}

/** Resolves the latest choice in the background before handing off the current queue entry. */
class ProviderSwitch(private val scope: () -> CoroutineScope, private val host: Host) {

    data class Target(val mediaId: String, val index: Int, val occurrence: Long)

    sealed interface Resolution {
        data class Found(val route: ForcedRoute) : Resolution
        data class Rejected(val reason: String? = null) : Resolution
        data object Retry : Resolution
    }

    interface Host {
        fun currentTarget(): Target?
        suspend fun resolve(target: Target, providerId: String, option: Pair<String, String>?): Resolution

        /** True once the replacement can play. A failed or cancelled handoff restores the old route. */
        suspend fun switchTo(target: Target, route: ForcedRoute): Boolean
    }

    private val _state = MutableStateFlow<ProviderSwitchState>(ProviderSwitchState.Idle)
    val state: StateFlow<ProviderSwitchState> = _state.asStateFlow()

    private var job: Job? = null
    private var target: Target? = null

    fun force(providerId: String, option: Pair<String, String>? = null) {
        val target = host.currentTarget() ?: return
        job?.cancel()
        this.target = target
        _state.value = ProviderSwitchState.Working(providerId, 1, MaxTries)

        val serviceScope = scope()
        job = serviceScope.launch {
            var attempts = 0
            val result = withTimeoutOrNull(DeadlineMs) {
                for (attempt in 1..MaxTries) {
                    currentCoroutineContext().ensureActive()
                    attempts = attempt
                    _state.value = ProviderSwitchState.Working(providerId, attempt, MaxTries)
                    // Some modules block even after cancellation. Wait on a separate job so
                    // the latest tap and the deadline can still take effect.
                    val fetch = serviceScope.async(Dispatchers.IO) {
                        try {
                            host.resolve(target, providerId, option)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.tag(Tag).w(e, "$providerId try $attempt failed for ${target.mediaId}")
                            Resolution.Retry
                        }
                    }
                    val answer = try {
                        withTimeoutOrNull(AttemptMs) { fetch.await() } ?: Resolution.Retry
                    } catch (_: CancellationException) {
                        currentCoroutineContext().ensureActive()
                        Resolution.Retry
                    } finally {
                        fetch.cancel()
                    }
                    if (answer !is Resolution.Retry) return@withTimeoutOrNull answer
                    if (attempt < MaxTries) delay((attempt * 400L).coerceAtMost(3_000L))
                }
                Resolution.Retry
            }

            currentCoroutineContext().ensureActive()
            _state.value = when {
                host.currentTarget() != target -> ProviderSwitchState.Idle
                result is Resolution.Found -> {
                    _state.value = ProviderSwitchState.Starting(providerId)
                    val switched = try {
                        host.switchTo(target, result.route)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.tag(Tag).w(e, "Provider handoff failed")
                        false
                    }
                    currentCoroutineContext().ensureActive()
                    if (host.currentTarget() != target) ProviderSwitchState.Idle
                    else if (switched) ProviderSwitchState.Done(providerId)
                    else ProviderSwitchState.Failed(providerId, attempts)
                }
                else -> ProviderSwitchState.Failed(providerId, attempts, (result as? Resolution.Rejected)?.reason)
            }
            this@ProviderSwitch.target = null
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        target = null
        _state.value = ProviderSwitchState.Idle
    }

    fun onMediaChanged() {
        if (target != null && host.currentTarget() != target) cancel()
    }

    fun acknowledge() {
        if (target == null) _state.value = ProviderSwitchState.Idle
    }

    companion object {
        private const val Tag = "ProviderSwitch"
        const val MaxTries = 20
        const val DeadlineMs = 60_000L
        private const val AttemptMs = 9_000L
    }
}
