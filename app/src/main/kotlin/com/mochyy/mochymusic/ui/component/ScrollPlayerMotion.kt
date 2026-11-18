package com.mochyy.mochymusic.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavBackStackEntry
import com.mochyy.mochymusic.R
import kotlin.math.abs

internal enum class ScrollPlayerMotion(
    @StringRes val label: Int,
    private val duration: Int,
    private val horizontal: Float = 0f,
    private val vertical: Float = 0f,
    private val width: Float = 1f,
    private val height: Float = 1f,
    private val fadeStart: Float = 0f,
    private val settle: Float = 0f,
) {
    FLOW(R.string.scroll_player_flow, 280, vertical = .8f, width = .94f),
    GLIDE(R.string.scroll_player_glide, 340, horizontal = .16f, vertical = .12f),
    LIFT(R.string.scroll_player_lift, 300, vertical = -.42f),
    SOFT_FADE(R.string.scroll_player_soft_fade, 260),
    ELASTIC(R.string.scroll_player_elastic, 360, width = .84f, height = .88f, settle = .06f),
    ZEPHYR(R.string.scroll_player_zephyr, 420, horizontal = -.065f, vertical = -.13f),
    CASCADE(R.string.scroll_player_cascade, 360, vertical = 1.36f, fadeStart = .24f),
    FOCUS(R.string.scroll_player_focus, 290, width = 1.055f, height = 1.055f),
    SNAP(R.string.scroll_player_snap, 100),
    BLOOM(R.string.scroll_player_bloom, 320, width = .86f, height = .86f, fadeStart = .14f),
    SWEEP(R.string.scroll_player_sweep, 310, horizontal = -.28f, vertical = .18f, fadeStart = .18f),
    FLOAT(R.string.scroll_player_float, 380, vertical = -.28f, width = .96f, height = .96f),
    COMPRESS(R.string.scroll_player_compress, 240, width = .90f, height = .50f, fadeStart = .21f),
    UNFOLD(R.string.scroll_player_unfold, 320, horizontal = .11f, vertical = .46f, height = .74f),
    SETTLE(R.string.scroll_player_settle, 400, vertical = .23f, width = .96f, height = .96f, settle = .032f);

    fun animation(entering: Boolean) = tween<Float>(
        durationMillis = duration,
        easing = if (entering) ScrollRevealEasing else FastOutSlowInEasing,
    )

    /** Add the scroll effect on top of the existing open/close transforms. */
    fun applyTo(layer: GraphicsLayerScope, foldedFraction: Float, travel: Float, rtl: Boolean = false) {
        val folded = foldedFraction.coerceIn(0f, 1f)
        val bounce = settle * (1f - folded) * folded * 4f
        val direction = if (rtl) -1f else 1f
        with(layer) {
            translationX = travel * horizontal * 3f * folded * direction
            // Keep fully hidden buttons outside the touchable window, even for the opacity presets.
            translationY += if (folded == 1f) travel + 1f else travel * vertical * folded
            scaleX *= 1f + (width - 1f) * folded + bounce
            scaleY *= 1f + (height - 1f) * folded + bounce
            alpha *= (1f - (folded - fadeStart) / (1f - fadeStart)).coerceIn(0f, 1f)
        }
    }
}

private val ScrollRevealEasing = CubicBezierEasing(.16f, 1f, .30f, 1f)
internal val ScrollPlayerEnabledKey = booleanPreferencesKey("scrollPlayerEnabled")
internal val ScrollPlayerMotionKey = stringPreferencesKey("scrollPlayerMotion")

internal class ScrollPlayerChrome(
    private val enabled: Boolean,
    private val threshold: Float,
) : NestedScrollConnection {
    private val folded = mutableStateOf(false)
    private var distance = 0f

    val isFolded: Boolean get() = folded.value

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (!enabled || source != NestedScrollSource.UserInput) return Offset.Zero

        // Only fold on real content movement. A pull at the top still restores the tabs.
        val dy = if (isFolded && consumed.y >= 0f) consumed.y + available.y.coerceAtLeast(0f) else consumed.y
        if (dy == 0f || abs(consumed.x) > abs(dy)) return Offset.Zero
        if ((dy > 0f) != isFolded) {
            distance = 0f
        } else {
            distance += abs(dy)
            if (distance >= threshold) {
                folded.value = !isFolded
                distance = 0f
            }
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        distance = 0f
        return Velocity.Zero
    }
}

@Composable
internal fun rememberScrollPlayerChrome(entry: NavBackStackEntry?, enabled: Boolean): ScrollPlayerChrome {
    val threshold = with(LocalDensity.current) { 28.dp.toPx() }
    return remember(entry, enabled, threshold) { ScrollPlayerChrome(enabled, threshold) }
}

@Composable
internal fun scrollPlayerFraction(
    entry: NavBackStackEntry?,
    chrome: ScrollPlayerChrome,
    motion: ScrollPlayerMotion,
): State<Float> = key(entry) {
    val folded = chrome.isFolded
    val animation = remember(motion, folded) { motion.animation(entering = !folded) }
    animateFloatAsState(
        targetValue = if (folded) 1f else 0f,
        animationSpec = animation,
        label = "scrollPlayerFold",
    )
}
