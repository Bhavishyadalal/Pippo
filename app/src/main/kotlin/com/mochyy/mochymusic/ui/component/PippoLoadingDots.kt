package com.mochyy.mochymusic.ui.component

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import com.mochyy.mochymusic.R

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mochyy.mochymusic.LocalAppContentObscured
import com.mochyy.mochymusic.ui.theme.PippoMotion
import com.mochyy.mochymusic.ui.theme.PippoSpacing
import com.mochyy.mochymusic.ui.utils.AppMotion

/** Reusable progress decoration; hidden pages and reduced motion keep a still three-dot mark. */
@Composable
fun PippoLoadingDots(modifier: Modifier = Modifier) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val obscured = LocalAppContentObscured.current
    val animate = lifecycle.isAtLeast(Lifecycle.State.STARTED) && !obscured.value &&
        AppMotion.enabled && AppMotion.durationScale.scaleFactor > 0f &&
        com.mochyy.mochymusic.ui.customization.UiCustomizationState.options.loadingMotion
    val description = stringResource(R.string.please_wait)
    Row(
        modifier = modifier.semantics {
            contentDescription = description
            liveRegion = LiveRegionMode.Polite
        },
        horizontalArrangement = Arrangement.spacedBy(PippoSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (animate) {
            val transition = rememberInfiniteTransition(label = "pippoLoading")
            repeat(3) { index ->
                val phase = transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(PippoMotion.Loading, delayMillis = index * 120, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse,
                    ),
                    label = "pippoLoadingDot$index",
                )
                LoadingDot(phase)
            }
        } else {
            val still = rememberUpdatedState(.6f)
            repeat(3) { LoadingDot(still) }
        }
    }
}

@Composable
private fun LoadingDot(phase: State<Float>) {
    Box(
        Modifier.size(6.dp).graphicsLayer {
            alpha = .35f + .65f * phase.value
            scaleX = .85f + .15f * phase.value
            scaleY = scaleX
        }.clip(CircleShape).background(MaterialTheme.colorScheme.primary),
    )
}
