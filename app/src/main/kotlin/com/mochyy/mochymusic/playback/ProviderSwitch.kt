// ProviderSwitch.kt

package com.mochyy.mochymusic.playback

import com.mochyy.mochymusic.module.SourceStream
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
) {
    companion object {
        const val Device = "device"
        const val Downloads = "downloads"
    }
}

/** a provider picked by hand for one song, and (once) the stream it already found */
class ForcedRoute(val providerId: String, @Volatile var freshStream: SourceStream?)

/** what "force change" in stream info is doing */
sealed interface ProviderSwitchState {
    data object Idle : ProviderSwitchState
    data class Working(val providerId: String, val attempt: Int, val of: Int) : ProviderSwitchState
    data class Failed(val providerId: String, val attempts: Int) : ProviderSwitchState
    data class Done(val providerId: String) : ProviderSwitchState
}

/** moves the playing song to a picked provider: stop, ask up to 20 times in a minute, reload at the same spot or carry on */
// the service's scope is read on every switch: it's replaced when the service comes back after being stopped
class ProviderSwitch(private val scope: () -> CoroutineScope, private val host: Host) {

    interface Host {
        /** the song playing, null when there's nothing to move */
        fun currentMediaId(): String?

        /** stops playback and says whether it was playing, so a failed switch can carry on */
        fun holdPlayback(): Boolean

        fun resumePlayback()

        /** one try at the provider; null when it didn't have the song or didn't answer */
        suspend fun resolve(providerId: String, mediaId: String): ForcedRoute?

        /** the provider answered: remember it for the song and reload it from there */
        fun switchTo(mediaId: String, route: ForcedRoute, play: Boolean)
    }

    private val _state = MutableStateFlow<ProviderSwitchState>(ProviderSwitchState.Idle)
    val state: StateFlow<ProviderSwitchState> = _state.asStateFlow()

    private var job: Job? = null
    private var heldMediaId: String? = null
    private var resumeAfterSwitch = false

    fun force(providerId: String) {
        val mediaId = host.currentMediaId() ?: return
        val wasPlaying = host.holdPlayback() || (heldMediaId == mediaId && resumeAfterSwitch)
        job?.cancel()
        heldMediaId = mediaId
        resumeAfterSwitch = wasPlaying
        _state.value = ProviderSwitchState.Working(providerId, 1, MaxTries)

        val serviceScope = scope()
        job = serviceScope.launch {
            var attempts = 0
            val route = withTimeoutOrNull(DeadlineMs) {
                for (attempt in 1..MaxTries) {
                    attempts = attempt
                    _state.value = ProviderSwitchState.Working(providerId, attempt, MaxTries)
                    // Wait separately from the module call. A blocking module cannot delay a
                    // replacement tap, cancellation, or the deadline.
                    val fetch = serviceScope.async(Dispatchers.IO) {
                        try {
                            host.resolve(providerId, mediaId)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.tag(Tag).w(e, "$providerId try $attempt failed for $mediaId")
                            null
                        }
                    }
                    val found = try {
                        withTimeoutOrNull(AttemptMs) { fetch.await() }
                    } catch (_: CancellationException) {
                        currentCoroutineContext().ensureActive()
                        null
                    } finally {
                        fetch.cancel()
                    }
                    if (found != null) return@withTimeoutOrNull found
                    if (attempt < MaxTries) delay((attempt * 400L).coerceAtMost(3_000L))
                }
                null
            }
            currentCoroutineContext().ensureActive()
            heldMediaId = null
            resumeAfterSwitch = false
            if (host.currentMediaId() != mediaId) {
                _state.value = ProviderSwitchState.Idle
            } else if (route == null) {
                _state.value = ProviderSwitchState.Failed(providerId, attempts)
                if (wasPlaying) host.resumePlayback()
            } else {
                host.switchTo(mediaId, route, wasPlaying)
                _state.value = ProviderSwitchState.Done(providerId)
            }
        }
    }

    fun cancel() {
        val mediaId = heldMediaId ?: return
        job?.cancel()
        job = null
        heldMediaId = null
        val resume = resumeAfterSwitch
        resumeAfterSwitch = false
        _state.value = ProviderSwitchState.Idle
        if (resume && host.currentMediaId() == mediaId) host.resumePlayback()
    }

    fun onMediaChanged(mediaId: String?) {
        if (heldMediaId != null && heldMediaId != mediaId) cancel()
    }

    /** the sheet closed: a finished or failed switch has said what it had to */
    fun acknowledge() {
        if (_state.value !is ProviderSwitchState.Working) _state.value = ProviderSwitchState.Idle
    }

    companion object {
        private const val Tag = "ProviderSwitch"
        const val MaxTries = 20
        const val DeadlineMs = 60_000L
        private const val AttemptMs = 9_000L
    }
}
