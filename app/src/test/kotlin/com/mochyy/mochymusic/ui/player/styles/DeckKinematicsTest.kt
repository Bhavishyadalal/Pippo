package com.mochyy.mochymusic.ui.player.styles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckKinematicsTest {
    @Test fun `unknown duration and invalid player positions never corrupt tape geometry`() {
        assertEquals(0f, DeckKinematics.fraction(10_000L, 0L), 0f)
        assertEquals(0f, DeckKinematics.fraction(10_000L, -9223372036854775807L), 0f)
        assertEquals(0f, DeckKinematics.fraction(-20L, 100L), 0f)
        assertEquals(1f, DeckKinematics.fraction(Long.MAX_VALUE, 100L), 0f)
        assertEquals(.5f, DeckKinematics.fraction(90_000L, 180_000L), .00001f)
    }

    @Test fun `transferring tape conserves area while supply shrinks and takeup grows`() {
        var previousSupply = 1f
        var previousTakeup = 0f
        val expectedArea = .28f * .28f + .83f * .83f
        for (step in 0..1000) {
            val (supply, takeup) = DeckKinematics.tapeRadii(step / 1000f)
            assertEquals(expectedArea, supply * supply + takeup * takeup, .000001f)
            assertTrue(supply <= previousSupply && takeup >= previousTakeup)
            assertTrue(supply >= .28f && takeup >= .28f && supply <= .83f && takeup <= .83f)
            previousSupply = supply
            previousTakeup = takeup
        }
    }

    @Test fun `seeking between beginning and end reverses the tape packs`() {
        val start = DeckKinematics.tapeRadii(0f)
        val end = DeckKinematics.tapeRadii(1f)
        assertEquals(start.first, end.second, 0f)
        assertEquals(start.second, end.first, 0f)
        val middle = DeckKinematics.tapeRadii(.5f)
        assertEquals(middle.first, middle.second, 0f)
    }

    @Test fun `out of range and nonfinite fractions have safe bounded packs`() {
        assertEquals(DeckKinematics.tapeRadii(0f), DeckKinematics.tapeRadii(-1f))
        assertEquals(DeckKinematics.tapeRadii(1f), DeckKinematics.tapeRadii(2f))
        assertEquals(DeckKinematics.tapeRadii(0f), DeckKinematics.tapeRadii(Float.NaN))
        assertEquals(DeckKinematics.tapeRadii(0f), DeckKinematics.tapeRadii(Float.POSITIVE_INFINITY))
    }

    @Test fun `a background gap cannot jump decorative rotation forward`() {
        assertEquals(DeckKinematics.advance(45f, 50_000_000L), DeckKinematics.advance(45f, 600_000_000_000L), 0f)
        assertEquals(45f, DeckKinematics.advance(45f, -1L), 0f)
        assertTrue(DeckKinematics.advance(359.9f, 50_000_000L) in 0f..360f)
        assertEquals(.4f, DeckKinematics.advance(0f, 16_666_667L), .00001f)
    }
}
