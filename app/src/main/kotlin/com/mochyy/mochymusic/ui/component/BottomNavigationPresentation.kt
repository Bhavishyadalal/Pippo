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

// Keep the transition visible through most of the sheet's travel, and clear the buttons
// before the full player's controls reach the bottom of the window.
internal fun navigationPlayerHideFraction(progress: Float): Float =
    if (AppMotion.durationScale.scaleFactor > 0f) (progress / .78f).coerceIn(0f, 1f)
    else if (progress < .5f) 0f else 1f

/** The bar follows the sheet without changing the layout or insets during a gesture. */
internal fun GraphicsLayerScope.applyNavigationMotion(
    motion: BottomNavigationMotion,
    progress: Float,
    travel: Float,
    routeOffset: Float = 0f,
    scrim: Boolean = false,
) {
    val hidden = navigationPlayerHideFraction(progress)
    transformOrigin = TransformOrigin(.5f, 1f)
    if (hidden >= 1f) {
        alpha = 0f
        translationY = routeOffset + travel
        return
    }
    alpha = when (motion) {
        BottomNavigationMotion.SLIDE -> 1f - hidden * hidden
        BottomNavigationMotion.FOLD, BottomNavigationMotion.FADE -> 1f - hidden
    }
    translationY = routeOffset + travel * when (motion) {
        BottomNavigationMotion.SLIDE -> hidden
        BottomNavigationMotion.FOLD -> .5f * hidden
        BottomNavigationMotion.FADE -> 0f
    }
    scaleX = if (!scrim && motion == BottomNavigationMotion.FOLD) 1f - .26f * hidden else 1f
    scaleY = if (!scrim && motion == BottomNavigationMotion.FOLD) 1f - .42f * hidden else 1f
}
