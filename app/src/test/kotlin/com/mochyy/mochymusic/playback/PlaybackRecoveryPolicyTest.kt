package com.mochyy.mochymusic.playback

import org.junit.Assert.*
import org.junit.Test

class PlaybackRecoveryPolicyTest {
    private val phone = AudioRouteIdentity(1, false)
    private val tws = AudioRouteIdentity(2, true)
    @Test fun onlyBluetoothHandoverRefreshesSink() {
        assertFalse(needsBluetoothRouteRefresh(null, tws))
        assertFalse(needsBluetoothRouteRefresh(tws, tws.copy()))
        assertFalse(needsBluetoothRouteRefresh(tws, null))
        assertFalse(needsBluetoothRouteRefresh(phone, AudioRouteIdentity(3, false)))
        assertTrue(needsBluetoothRouteRefresh(phone, tws))
        assertTrue(needsBluetoothRouteRefresh(tws, phone))
        assertTrue(needsBluetoothRouteRefresh(tws, AudioRouteIdentity(4, true)))
    }
    @Test fun disconnectedOutputCannotBlockHandoverForever() {
        assertFalse(finishOutputDrain(false, 0))
        assertFalse(finishOutputDrain(false, 2999))
        assertTrue(finishOutputDrain(false, 3000))
        assertTrue(finishOutputDrain(false, 60000))
        assertTrue(finishOutputDrain(true, 0))
    }
    @Test fun bluetoothOffloadAndEffectsAreIncompatible() {
        assertTrue(shouldUseAudioOffload(true, false, false, false))
        assertFalse(shouldUseAudioOffload(true, false, false, true))
        assertFalse(shouldUseAudioOffload(true, true, false, false))
        assertFalse(shouldUseAudioOffload(true, false, true, false))
        assertFalse(shouldUseAudioOffload(false, false, false, false))
    }
    @Test fun outputRecoveryRequiresRepeatedUnhealthySamples() {
        assertFalse(needsOutputRecovery(2, 2, 3))
        assertTrue(needsOutputRecovery(3, 0, 0))
        assertTrue(needsOutputRecovery(0, 3, 0))
        assertTrue(needsOutputRecovery(0, 0, 4))
    }
    @Test fun routeFloatRequiresVerifiedWirelessSupport() {
        assertFalse(permitsRouteFloat(true, false, true, true))
        assertFalse(permitsRouteFloat(false, true, false, true))
        assertTrue(permitsRouteFloat(false, true, true, true))
        assertTrue(permitsRouteFloat(false, false, false, true))
        assertFalse(permitsRouteFloat(false, false, true, false))
    }
    @Test fun stalePausedAndCastRecoveryCannotResume() {
        assertTrue(mayResumeRecovery("song", "song", true, false))
        assertFalse(mayResumeRecovery("old", "new", true, false))
        assertFalse(mayResumeRecovery("song", "song", false, false))
        assertFalse(mayResumeRecovery("song", "song", true, true))
        assertFalse(mayResumeRecovery(null, null, true, false))
        assertFalse(mayResumeRecovery("", "", true, false))
        assertFalse(mayResumeRecovery("song", "song", true, false, expectedPause = 1, currentPause = 2))
        assertTrue(mayResumeRecovery("song", "song", true, false, expectedPause = 2, currentPause = 2))
    }
}
