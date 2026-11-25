package com.mochyy.mochymusic.playback.audio

import android.media.AudioTrack
import android.media.AudioRouting
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference

/** The active Media3 platform sink, sampled on the player's application thread. */
internal class PlatformOutputMonitor(onRouteChanged: (PlatformOutputMonitor) -> Unit) {
    @Volatile private var current = WeakReference<AudioTrack>(null)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var released = false
    private val routeListener = AudioRouting.OnRoutingChangedListener { output ->
        if (output === current.get()) onRouteChanged(this)
    }

    data class Sample(val track: AudioTrack, val frames: Int, val deviceId: Int?, val playState: Int)

    @Synchronized
    fun attach(track: AudioTrack) {
        if (released) return
        val old = current.get()
        if (old === track) return
        runCatching { old?.removeOnRoutingChangedListener(routeListener) }
        current = WeakReference(track)
        runCatching { track.addOnRoutingChangedListener(routeListener, mainHandler) }
    }

    @Synchronized
    fun release() {
        released = true
        val old = current.get()
        current = WeakReference<AudioTrack>(null)
        runCatching { old?.removeOnRoutingChangedListener(routeListener) }
    }

    fun sample(): Sample? {
        val track = current.get() ?: return null
        return runCatching {
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                Sample(track, track.playbackHeadPosition, track.routedDevice?.id, track.playState)
            } else null
        }.getOrNull()
    }
}
