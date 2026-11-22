// PassRouterSink.kt

package com.mochyy.mochymusic.playback.audio.pass

import android.media.AudioDeviceInfo
import android.media.AudioTrack
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.audio.AudioSink
import com.mochyy.mochymusic.playback.finishOutputDrain
import timber.log.Timber
import java.nio.ByteBuffer

/** sends each song either the normal way or to audio pass's exclusive output, and hands over between them without a song starting under another */
@UnstableApi
class PassRouterSink(
    private val normal: AudioSink,
    private val exclusive: PassSink,
    private val pass: AudioPass,
) : AudioSink {

    /** the file the playing song was decoded from, set by the audio renderer just before its decoder starts */
    @Volatile var sourceFormat: Format? = null

    private var active: AudioSink = normal

    // the sink that played the last song and is playing out what it holds before the next one writes
    private var retiring: AudioSink? = null
    private var retiringAtMs = 0L
    private var pendingConfigure = false
    private var configuredSource: Format? = null
    private var clock: Clock = Clock.DEFAULT
    private var lastFormat: Format? = null
    private var lastBufferSize = 0
    private var lastChannels: IntArray? = null
    private var decidedAt = -1
    private var playing = false

    override fun getFormatSupport(format: Format): Int {
        // a hi-res song is asked for as float while it could go exclusive: 24 bits fit a float whole
        if (format.sampleMimeType == MimeTypes.AUDIO_RAW && format.pcmEncoding == C.ENCODING_PCM_FLOAT && pass.wantsFloat(sourceFormat)) {
            return AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY
        }
        return normal.getFormatSupport(format)
    }

    override fun supportsFormat(format: Format): Boolean = getFormatSupport(format) != AudioSink.SINK_FORMAT_UNSUPPORTED

    override fun configure(inputFormat: Format, specifiedBufferSize: Int, outputChannels: IntArray?) {
        lastFormat = inputFormat
        lastBufferSize = specifiedBufferSize
        lastChannels = outputChannels
        configuredSource = sourceFormat
        route(inputFormat, fresh = true)
    }

    /** decides where this format goes now; a new song always configures its sink, a mid-song check only when the answer changed */
    private fun route(format: Format, fresh: Boolean) {
        decidedAt = pass.generation
        val target = selectSink(format)
        if (pendingConfigure) {
            active = target
        } else if (target !== active || (target === exclusive && exclusive.requiresReopen(format, configuredSource))) {
            // The same exclusive sink can also need a handover when its hardware format changes.
            retiring = active
            retiringAtMs = clock.elapsedRealtime()
            active = target
            pendingConfigure = true
        } else if (fresh) {
            if (active === normal && pass.holdsNormal()) pendingConfigure = true
            else configureSink(target, format)
        }
    }

    private fun selectSink(format: Format): AudioSink {
        val identity = lastChannels?.let { map -> map.indices.all { map[it] == it } } ?: true
        return if (identity && exclusive.canTake(format, configuredSource)) exclusive else normal
    }

    private fun configureSink(target: AudioSink, format: Format) {
        if (target === exclusive) exclusive.configure(format, configuredSource) else normal.configure(format, lastBufferSize, lastChannels)
    }

    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean {
        // an effect, the switch or the dac changed mid-song: the song carries on wherever it may go now
        if (!prepareActive()) return false
        if (active !== exclusive) {
            return active.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        }
        return try {
            exclusive.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        } catch (failure: PassFailure) {
            fallBack(failure)
            false
        }
    }

    /** the exclusive output failed: the song goes on the normal way from this buffer, with no error shown */
    private fun fallBack(failure: PassFailure) {
        val format = lastFormat ?: return
        Timber.tag("AudioPass").i("exclusive output stopped: ${failure.message}")
        exclusive.reset()
        pass.failed(format, configuredSource, failure.message.orEmpty())
        active = normal
        pendingConfigure = true
        decidedAt = pass.generation
    }

    override fun playToEndOfStream() {
        if (!prepareActive()) return
        try {
            active.playToEndOfStream()
        } catch (failure: PassFailure) {
            fallBack(failure)
        }
    }

    private fun prepareActive(): Boolean {
        if (decidedAt != pass.generation && retiring == null) lastFormat?.let { route(it, fresh = false) }
        if (!drainRetiringSink()) return false
        if (!pendingConfigure) return true
        val format = checkNotNull(lastFormat)
        // Releasing an exclusive owner can change which route is eligible.
        if (decidedAt != pass.generation) route(format, fresh = false)
        if (active === normal && pass.holdsNormal()) return false
        pendingConfigure = false
        try {
            configureSink(active, format)
        } catch (failure: AudioSink.ConfigurationException) {
            // Deferred configuration runs from both the buffer write and end-of-stream paths.
            throw AudioSink.WriteException(AudioTrack.ERROR_BAD_VALUE, failure.format, false)
                .apply { initCause(failure) }
        }
        if (!playing) return true
        return try {
            active.play()
            true
        } catch (failure: PassFailure) {
            fallBack(failure)
            false
        }
    }

    private fun drainRetiringSink(): Boolean {
        val old = retiring ?: return true
        val ended = runCatching {
            old.playToEndOfStream()
            old.isEnded
        }.onFailure { Timber.tag("AudioPass").w(it, "Previous audio output failed while draining") }.getOrDefault(true)
        if (!playing && !ended) return false
        if (!finishOutputDrain(ended, clock.elapsedRealtime() - retiringAtMs)) return false
        runCatching { old.reset() }
            .onFailure { Timber.tag("AudioPass").w(it, "Could not reset previous audio output") }
        retiring = null
        return true
    }

    override fun isEnded(): Boolean = retiring == null && !pendingConfigure && active.isEnded

    override fun hasPendingData(): Boolean = pendingConfigure || active.hasPendingData()

    override fun getCurrentPositionUs(sourceEnded: Boolean): Long =
        retiring?.getCurrentPositionUs(sourceEnded)
            ?: if (pendingConfigure) AudioSink.CURRENT_POSITION_NOT_SET else active.getCurrentPositionUs(sourceEnded)

    override fun play() {
        playing = true
        val old = retiring
        if (old != null) {
            retiringAtMs = clock.elapsedRealtime()
            runCatching { old.play() }
        } else if (!pendingConfigure) {
            try {
                active.play()
            } catch (failure: PassFailure) {
                fallBack(failure)
            }
        }
    }

    override fun pause() {
        playing = false
        retiring?.takeIf { it !== active }?.pause()
        active.pause()
    }

    override fun flush() {
        // a seek: whatever was handing over is dropped with the rest
        val old = retiring
        retiring = null
        old?.reset()
        if (old !== active) active.flush()
    }

    override fun reset() {
        retiring = null
        pendingConfigure = false
        configuredSource = null
        lastFormat = null
        normal.reset()
        exclusive.reset()
        active = normal
    }

    override fun release() {
        normal.release()
        exclusive.release()
    }

    override fun handleDiscontinuity() = active.handleDiscontinuity()

    override fun getAudioTrackBufferSizeUs(): Long = active.audioTrackBufferSizeUs

    // the rest reaches both, so whichever plays next is already set up
    override fun setListener(listener: AudioSink.Listener) {
        normal.setListener(listener)
        exclusive.setListener(listener)
    }

    override fun setPlayerId(playerId: PlayerId?) = normal.setPlayerId(playerId)
    override fun setClock(clock: Clock) {
        this.clock = clock
        normal.setClock(clock)
    }
    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) = normal.setPlaybackParameters(playbackParameters)
    override fun getPlaybackParameters(): PlaybackParameters = normal.playbackParameters
    override fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean) {
        normal.skipSilenceEnabled = skipSilenceEnabled
        exclusive.skipSilenceEnabled = skipSilenceEnabled
    }
    override fun getSkipSilenceEnabled(): Boolean = normal.skipSilenceEnabled
    override fun setAudioAttributes(audioAttributes: AudioAttributes) {
        normal.setAudioAttributes(audioAttributes)
        exclusive.setAudioAttributes(audioAttributes)
    }
    override fun getAudioAttributes(): AudioAttributes? = normal.audioAttributes
    override fun setAudioSessionId(audioSessionId: Int) = normal.setAudioSessionId(audioSessionId)
    override fun setAuxEffectInfo(auxEffectInfo: AuxEffectInfo) = normal.setAuxEffectInfo(auxEffectInfo)
    override fun setPreferredDevice(audioDeviceInfo: AudioDeviceInfo?) = normal.setPreferredDevice(audioDeviceInfo)
    override fun setOutputStreamOffsetUs(outputStreamOffsetUs: Long) = normal.setOutputStreamOffsetUs(outputStreamOffsetUs)
    override fun enableTunnelingV21() = normal.enableTunnelingV21()
    override fun disableTunneling() = normal.disableTunneling()
    override fun setOffloadMode(offloadMode: Int) = normal.setOffloadMode(offloadMode)
    override fun setOffloadDelayPadding(delayInFrames: Int, paddingInFrames: Int) = normal.setOffloadDelayPadding(delayInFrames, paddingInFrames)
    override fun getFormatOffloadSupport(format: Format) = normal.getFormatOffloadSupport(format)
    override fun setVolume(volume: Float) {
        normal.setVolume(volume)
        exclusive.setVolume(volume)
    }
}
