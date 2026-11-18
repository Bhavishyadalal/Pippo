package com.mochyy.mochymusic.playback

internal data class AudioRouteIdentity(val id: Int, val bluetooth: Boolean)

/** Only a real Bluetooth handover needs this workaround, not startup or a repeated device notification. */
internal fun needsBluetoothRouteRefresh(previous: AudioRouteIdentity?, current: AudioRouteIdentity?): Boolean =
    previous != null && current != null && previous != current && (previous.bluetooth || current.bluetooth)

/** Delayed recovery belongs to one song and must never restart a paused song or local audio during casting. */
internal fun mayResumeRecovery(expectedId: String?, currentId: String?, wantsPlayback: Boolean, casting: Boolean,
    expectedPause: Long = 0, currentPause: Long = 0): Boolean =
    !expectedId.isNullOrEmpty() && expectedId == currentId && wantsPlayback && !casting && expectedPause == currentPause

/** A disappearing DAC must not leave a handover waiting for its playback head forever. */
internal fun finishOutputDrain(ended: Boolean, elapsedMs: Long): Boolean = ended || elapsedMs >= 3_000L
