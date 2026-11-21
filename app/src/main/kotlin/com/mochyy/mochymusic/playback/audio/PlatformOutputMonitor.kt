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
    private val routeListener = AudioRouting.OnRoutingChangedListener { output ->
        if (output === current.get()) onRouteChanged(this)
    }

    data class Sample(val track: AudioTrack, val frames: Int, val deviceId: Int?)

    fun attach(track: AudioTrack) {
        current.get()?.removeOnRoutingChangedListener(routeListener)
        current = WeakReference(track)
        track.addOnRoutingChangedListener(routeListener, mainHandler)
    }

    fun sample(): Sample? {
        val track = current.get() ?: return null
        return runCatching {
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                Sample(track, track.playbackHeadPosition, track.routedDevice?.id)
            } else null
        }.getOrNull()
    }
}
