package com.mochyy.mochymusic.ui.player.customize

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import com.mochyy.mochymusic.constants.PlayerArtworkBackgroundStrengthKey
import com.mochyy.mochymusic.utils.rememberPreference

internal const val DefaultPlayerArtworkBackgroundStrength = 0f

@Composable
internal fun playerArtworkBackgroundStrength(): Float =
    rememberPreference(PlayerArtworkBackgroundStrengthKey, DefaultPlayerArtworkBackgroundStrength)
        .value.coerceIn(0f, 1f)

/** Draw a black scrim on the backdrop alone so artwork and controls keep their original colors. */
internal fun Modifier.dimPlayerArtworkBackground(strength: Float): Modifier = when (strength) {
    1f -> this
    else -> drawWithContent {
        if (strength > 0f) drawContent()
        drawRect(Color.Black, alpha = 1f - strength)
    }
}

// Reflections and frost sit over the dimmed background, so fade them down faster than the base.
internal fun playerArtworkOverlayStrength(strength: Float): Float = strength * strength
