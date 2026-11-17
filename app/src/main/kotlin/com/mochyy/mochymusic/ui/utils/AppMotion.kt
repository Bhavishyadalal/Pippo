// customize's animation and element blur settings, mirrored here by mainactivity so draw code and nav lambdas can read them

package com.mochyy.mochymusic.ui.utils

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.MotionDurationScale
import androidx.navigation.NavBackStackEntry

// the speeds the animation slider stops at, slowest to fastest
val AnimationSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

object AppMotion {
    /** false: nothing animates, every compose animation jumps straight to its end */
    var enabled by mutableStateOf(true)

    /** how much faster than android's own animation speed things move, 1 is just android's */
    var speed by mutableFloatStateOf(1f)

    /** false: lyric lines, title morphs, menus and page wipes stay sharp, glass blur isn't touched */
    var elementBlur by mutableStateOf(true)

    private var systemScale by mutableFloatStateOf(1f)

    /** handed to the window's recomposer, so every animation in the app reads it each frame */
    val durationScale: MotionDurationScale = object : MotionDurationScale {
        override val scaleFactor: Float
            get() = if (!enabled) 0f else systemScale / speed.coerceAtLeast(0.1f)
    }

    // keeps android's animator duration scale in step, the way compose's own scale did before we replaced it
    fun followSystemScale(context: Context): ContentObserver {
        val resolver = context.contentResolver
        fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        systemScale = read()
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                systemScale = read()
            }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        return observer
    }
}

/** how a speed reads on screen: 1x, 1.25x, 0.5x */
fun animationSpeedLabel(speed: Float): String {
    val text = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')
    return "${text}x"
}

/** [transition], or no transition at all while animations are switched off */
fun AnimatedContentTransitionScope<NavBackStackEntry>.motionEnter(
    transition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?,
): EnterTransition? = if (AppMotion.enabled) selectedPageEnter { transition() } else EnterTransition.None

/** [transition], or no transition at all while animations are switched off */
fun AnimatedContentTransitionScope<NavBackStackEntry>.motionExit(
    transition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?,
): ExitTransition? = if (AppMotion.enabled) selectedPageExit { transition() } else ExitTransition.None

/** The NavHost's own defaults, which can't be null: [transition], or none while animations are off */
fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultMotionEnter(
    transition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
): EnterTransition = if (AppMotion.enabled) selectedPageEnter { transition() } ?: EnterTransition.None else EnterTransition.None

/** The NavHost's own defaults, which can't be null: [transition], or none while animations are off */
fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultMotionExit(
    transition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition,
): ExitTransition = if (AppMotion.enabled) selectedPageExit { transition() } ?: ExitTransition.None else ExitTransition.None

// Only navigation-owned transitions are changed; draggable player/sheet choreography stays intact.
private fun selectedPageEnter(current: () -> EnterTransition?): EnterTransition? {
    val ui = com.mochyy.mochymusic.ui.customization.UiCustomizationState.options
    val spec = androidx.compose.animation.core.tween<Float>(ui.transitionDuration)
    return when (ui.transition) {
        com.mochyy.mochymusic.constants.UiTransition.DEFAULT -> current()
        com.mochyy.mochymusic.constants.UiTransition.FADE -> androidx.compose.animation.fadeIn(spec)
        com.mochyy.mochymusic.constants.UiTransition.SCALE -> androidx.compose.animation.fadeIn(spec) + androidx.compose.animation.scaleIn(spec, initialScale = .96f)
        com.mochyy.mochymusic.constants.UiTransition.NONE -> EnterTransition.None
    }
}

private fun selectedPageExit(current: () -> ExitTransition?): ExitTransition? {
    val ui = com.mochyy.mochymusic.ui.customization.UiCustomizationState.options
    val spec = androidx.compose.animation.core.tween<Float>(ui.transitionDuration)
    return when (ui.transition) {
        com.mochyy.mochymusic.constants.UiTransition.DEFAULT -> current()
        com.mochyy.mochymusic.constants.UiTransition.FADE -> androidx.compose.animation.fadeOut(spec)
        com.mochyy.mochymusic.constants.UiTransition.SCALE -> androidx.compose.animation.fadeOut(spec) + androidx.compose.animation.scaleOut(spec, targetScale = .98f)
        com.mochyy.mochymusic.constants.UiTransition.NONE -> ExitTransition.None
    }
}
