package com.mochyy.mochymusic.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
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
import com.mochyy.mochymusic.constants.SilverSongTitleSpeedKey
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

internal val LocalSilverTitleVisible = compositionLocalOf { true }

@Composable
internal fun Modifier.songTitleSilver(title: String, visible: Boolean = true): Modifier {
    if (!visible || title.isBlank() || !LocalSilverTitleVisible.current) return this
    val (enabled) = rememberPreference(SilverSongTitleKey, defaultValue = false)
    if (!enabled) return this
    val (speed) = rememberPreference(SilverSongTitleSpeedKey, defaultValue = 1f)
    return silverSweep(title, repeat = true, speed = speed)
}

/** Runs the silver pass while visible, with an offscreen layer only during the sweep. */
@Composable
internal fun Modifier.silverSweep(key: String, repeat: Boolean = false, speed: Float = 1f): Modifier {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    if (!lifecycle.isAtLeast(Lifecycle.State.RESUMED) || AppMotion.durationScale.scaleFactor <= 0f) return this

    val sweep = remember(key) { Animatable(1f) }
    val drawing by remember(sweep) { derivedStateOf { sweep.value < 1f } }
    val multiplier = speed.takeIf { it.isFinite() }?.coerceIn(0.5f, 3f) ?: 1f
    LaunchedEffect(sweep, repeat, multiplier) {
        sweep.snapTo(1f)
        delay(160L)
        do {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween((1_250 / multiplier).roundToInt(), easing = LinearEasing))
            if (repeat) delay((3_200 / multiplier).roundToInt().toLong())
        } while (repeat && isActive)
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
