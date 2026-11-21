package com.mochyy.mochymusic.ui.player.info

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.playback.PlayingFrom
import com.mochyy.mochymusic.ui.utils.AppMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.map

@Composable
internal fun rememberTrackProvider(mediaId: String): PlayingFrom? {
    val service = LocalPlayerConnection.current?.service ?: return null
    return key(service, mediaId) {
        val provider = remember { service.playingFrom.map { it[mediaId] }.distinctUntilChanged() }
        provider.collectAsState(initial = service.playingFrom.value[mediaId]).value
    }
}

@Composable
internal fun PlayerProviderMark(
    mediaId: String,
    animate: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val service = LocalPlayerConnection.current?.service ?: return
    val from = rememberTrackProvider(mediaId)
    val facts = key(service, mediaId) {
        val stream = remember { service.streamFacts.map { it[mediaId] }.distinctUntilChanged() }
        stream.collectAsStateWithLifecycle(initialValue = service.streamFacts.value[mediaId]).value
    }
    val codec = facts?.codec?.trim()?.uppercase()
    // Only describe the current stream. The saved format may be from a previous provider.
    val quality = when (codec) {
        "FLAC", "ALAC" -> stringResource(R.string.aq_quality_lossless)
        else -> codec
    }?.takeIf { from != null }
    val name = from?.name ?: stringResource(R.string.pc_force_title)
    val label = if (quality == null) name else stringResource(R.string.player_provider_quality, name, quality)
    val action = onClick?.takeIf { mediaId.isNotBlank() && !mediaId.startsWith("LOCAL_") }
    val clickModifier = if (action == null) Modifier else Modifier.clickable(
        interactionSource = null,
        indication = null,
        role = Role.Button,
        onClickLabel = stringResource(R.string.pc_force_title),
        onClick = action,
    )
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val sweeping = from != null && animate && lifecycle.isAtLeast(Lifecycle.State.RESUMED) &&
        AppMotion.durationScale.scaleFactor > 0f
    val sweep = remember { Animatable(1f) }
    val glintVisible by remember { derivedStateOf { sweep.value < 1f } }
    LaunchedEffect(mediaId, from?.providerId, quality, sweeping) {
        sweep.snapTo(1f)
        if (!sweeping) return@LaunchedEffect
        while (isActive) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(1_250, easing = LinearEasing))
            delay(3_200L)
        }
    }

    Box(
        modifier = modifier
            .heightIn(min = 28.dp)
            .then(clickModifier)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            modifier = Modifier
                .graphicsLayer {
                    compositingStrategy = if (sweeping && glintVisible) {
                        CompositingStrategy.Offscreen
                    } else {
                        CompositingStrategy.Auto
                    }
                }
                .drawWithCache {
                    val band = size.width * .55f
                    val highlight = Brush.linearGradient(
                        listOf(Color.Transparent, Color(0xFFDCE5EF), Color.White, Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(band, size.height * .45f),
                    )
                    onDrawWithContent {
                        drawContent()
                        if (sweeping && glintVisible) {
                            translate(left = (size.width + band) * sweep.value - band) {
                                // Use the existing text's alpha as the mask for the moving highlight.
                                drawRect(highlight, size = Size(band, size.height), blendMode = BlendMode.SrcAtop)
                            }
                        }
                    }
                },
            color = Color(0xFFB8BEC8),
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
