package com.mochyy.mochymusic.ui.player.styles

import kotlin.math.sqrt

/** Pure geometry shared by live cassette rendering and its still selector preview. */
internal object DeckKinematics {
    fun fraction(positionMs: Long, durationMs: Long): Float =
        if (durationMs <= 0L) 0f else (positionMs.coerceAtLeast(0L).toDouble() / durationMs).coerceIn(0.0, 1.0).toFloat()

    /** Tape transfers between hubs without creating tape: the sum of squared radii is constant. */
    fun tapeRadii(fraction: Float): Pair<Float, Float> {
        val progress = if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else 0f
        val coreArea = 0.28f * 0.28f
        val tapeArea = 0.83f * 0.83f - coreArea
        return sqrt(coreArea + tapeArea * (1f - progress)) to sqrt(coreArea + tapeArea * progress)
    }

    /** A resumed app never advances its decorative mechanics by the time spent in the background. */
    fun advance(degrees: Float, elapsedNanos: Long): Float =
        (degrees + elapsedNanos.coerceIn(0L, 50_000_000L) / 1_000_000_000f * 24f) % 360f
}
