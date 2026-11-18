package com.mochyy.mochymusic.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.utils.AppMotion

enum class BottomNavigationStyle(@StringRes val label: Int) {
    GLASS(R.string.nav_style_glass),
    CAPSULE(R.string.nav_style_capsule),
    DOCKED(R.string.nav_style_docked),
    RIBBON(R.string.nav_style_ribbon);

    internal val horizontalMargin get() = when (this) {
        GLASS -> 24.dp
        CAPSULE, RIBBON -> 16.dp
        DOCKED -> 0.dp
    }
    internal val barHeight get() = when (this) {
        GLASS, DOCKED -> NavBarHeight
        CAPSULE, RIBBON -> 56.dp
    }
    internal val barShape get() = when (this) {
        GLASS, CAPSULE -> NavBarShape
        DOCKED -> RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
        RIBBON -> RoundedCornerShape(16.dp)
    }
}

enum class BottomNavigationMotion(@StringRes val label: Int) {
    SLIDE(R.string.nav_motion_slide),
    FOLD(R.string.nav_motion_fold),
    FADE(R.string.nav_motion_fade),
}

val BottomNavigationStyleKey = stringPreferencesKey("bottomNavigationStyle")
val BottomNavigationMotionKey = stringPreferencesKey("bottomNavigationMotion")

/** The bar follows the sheet's existing progress. Its layout and insets stay fixed during a gesture. */
internal fun GraphicsLayerScope.applyNavigationMotion(
    motion: BottomNavigationMotion,
    progress: Float,
    travel: Float,
    routeOffset: Float = 0f,
    scrim: Boolean = false,
) {
    val p = progress.coerceIn(0f, 1f)
    transformOrigin = TransformOrigin(.5f, 1f)
    val opacity = (1f - p / .6f).coerceIn(0f, 1f)
    val animate = AppMotion.durationScale.scaleFactor > 0f
    val visible = if (animate) opacity else if (p < .5f) 1f else 0f
    val showSlide = motion == BottomNavigationMotion.SLIDE && animate
    alpha = if (scrim && showSlide) 1f else visible
    translationY = routeOffset + when {
        showSlide -> travel * p
        visible == 0f -> travel
        motion == BottomNavigationMotion.FOLD -> travel * .34f * (1f - visible)
        else -> 0f
    }
    scaleX = if (!scrim && motion == BottomNavigationMotion.FOLD) .78f + .22f * visible else 1f
    scaleY = if (!scrim && motion == BottomNavigationMotion.FOLD) .68f + .32f * visible else 1f
}
