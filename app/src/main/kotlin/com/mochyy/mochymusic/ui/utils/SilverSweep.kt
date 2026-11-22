package com.mochyy.mochymusic.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.mochyy.mochymusic.constants.SilverSongTitleKey
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.delay

@Composable
internal fun Modifier.songTitleSilver(title: String, visible: Boolean = true): Modifier {
    if (!visible || title.isBlank()) return this
    val (enabled) = rememberPreference(SilverSongTitleKey, defaultValue = false)
    return if (enabled) silverSweep(title) else this
}

/** One pass on appearance or content change, with an offscreen layer only while the glint is moving. */
@Composable
internal fun Modifier.silverSweep(key: String): Modifier {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    if (!lifecycle.isAtLeast(Lifecycle.State.RESUMED) || AppMotion.durationScale.scaleFactor <= 0f) return this

    val sweep = remember(key) { Animatable(1f) }
    val drawing by remember(sweep) { derivedStateOf { sweep.value < 1f } }
    LaunchedEffect(sweep) {
        delay(160)
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(1_550, easing = LinearEasing))
    }
    return graphicsLayer {
        compositingStrategy = if (drawing) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }.drawWithCache {
        val band = size.width * 0.62f
        val highlight = Brush.linearGradient(
            0f to Color.Transparent,
            0.24f to Color(0x704B5C70),
            0.47f to Color(0xFF9DAFC1),
            0.62f to Color(0xFFF2F8FF),
            0.76f to Color(0xB8BFCFDB),
            1f to Color.Transparent,
            end = Offset(band * 0.86f, size.height * 0.54f),
        )
        onDrawWithContent {
            drawContent()
            if (drawing && band > 0f) {
                translate(left = (size.width + band) * sweep.value - band) {
                    drawRect(highlight, size = Size(band, size.height), blendMode = BlendMode.SrcAtop)
                }
            }
        }
    }
}
