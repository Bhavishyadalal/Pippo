package com.mochyy.mochymusic.playback

internal data class AudioRouteIdentity(val id: Int, val bluetooth: Boolean, val usb: Boolean = false) {
    val external: Boolean get() = bluetooth || usb
}

/** Only a real Bluetooth handover needs this workaround, not startup or a repeated device notification. */
internal fun needsBluetoothRouteRefresh(previous: AudioRouteIdentity?, current: AudioRouteIdentity?): Boolean =
    previous != null && current != null && previous != current && (previous.bluetooth || current.bluetooth)

internal fun needsUsbRouteRefresh(previous: AudioRouteIdentity?, current: AudioRouteIdentity): Boolean =
    previous != null && previous != current && (previous.usb || current.usb)

/** Delayed recovery belongs to one song and must never restart a paused song or local audio during casting. */
internal fun mayResumeRecovery(expectedId: String?, currentId: String?, wantsPlayback: Boolean, casting: Boolean,
    expectedPause: Long = 0, currentPause: Long = 0): Boolean =
    !expectedId.isNullOrEmpty() && expectedId == currentId && wantsPlayback && !casting && expectedPause == currentPause

/** A disappearing DAC must not leave a handover waiting for its playback head forever. */
internal fun finishOutputDrain(ended: Boolean, elapsedMs: Long): Boolean = ended || elapsedMs >= 3_000L

/** Bluetooth uses the normal PCM path; avoid offload handovers on wireless outputs. */
internal fun shouldUseAudioOffload(enabled: Boolean, transitions: Boolean, effects: Boolean, bluetooth: Boolean): Boolean =
    enabled && !transitions && !effects && !bluetooth

internal fun needsOutputRecovery(stalled: Int, wrongRoute: Int, missing: Int): Boolean =
    stalled >= 3 || wrongRoute >= 3 || missing >= 4

/** Use PCM16 on unverified wireless routes and phone speakers; leave USB/wired policy intact. */
internal fun permitsRouteFloat(phone: Boolean, bluetooth: Boolean, advertisesFloat: Boolean, known: Boolean): Boolean =
    known && !phone && (!bluetooth || advertisesFloat)
