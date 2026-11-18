package com.mochyy.mochymusic.utils

import org.junit.Assert.*
import org.junit.Test

class ValidatedNetworkStateTest {
    @Test fun availableCaptivePortalIsNotInternet() {
        val state = ValidatedNetworkState()
        assertFalse(state.available(1))
        assertFalse(state.capabilities(1, true, false))
        assertTrue(state.capabilities(1, true, true))
    }
    @Test fun oldNetworkLossCannotDisconnectReplacement() {
        val state = ValidatedNetworkState(1, true)
        assertFalse(state.available(2))
        assertTrue(state.capabilities(2, true, true))
        assertTrue(state.lost(1))
        assertTrue(state.capabilities(1, false, false))
        assertFalse(state.lost(2))
    }
    @Test fun capabilityLossAndRecoveryAreReportedWithoutReconnect() {
        val state = ValidatedNetworkState(1, true)
        assertFalse(state.capabilities(1, true, false))
        assertTrue(state.capabilities(1, true, true))
        assertFalse(state.capabilities(1, false, true))
    }
    @Test fun duplicateAvailablePreservesValidation() {
        val state = ValidatedNetworkState(1, true)
        assertTrue(state.available(1))
        assertTrue(state.lost(99))
    }
}
