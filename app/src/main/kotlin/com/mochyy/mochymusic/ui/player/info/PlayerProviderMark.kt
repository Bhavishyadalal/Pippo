package com.mochyy.mochymusic.ui.player.info

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
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
import com.mochyy.mochymusic.LocalArtworkColor
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.playback.PlayingFrom
import com.mochyy.mochymusic.ui.theme.ArtworkPalette
import com.mochyy.mochymusic.ui.utils.AppMotion
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal val ProviderMarkRoom = 40.dp

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
    onClick: () -> Unit,
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
    val artworkColor = LocalArtworkColor.current
    val palette = remember(artworkColor) { ArtworkPalette.from(artworkColor) }
    val canSwitch = mediaId.isNotBlank() && !mediaId.startsWith("LOCAL_")
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val sweeping = from != null && animate && lifecycle.isAtLeast(Lifecycle.State.RESUMED) &&
        AppMotion.durationScale.scaleFactor > 0f
    val sweep = remember { Animatable(1f) }
    LaunchedEffect(mediaId, from?.providerId, quality, sweeping) {
        if (sweeping) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(950, easing = LinearEasing))
        } else {
            sweep.snapTo(1f)
        }
    }
    ProviderCapsule(name, quality, palette, canSwitch, onClick, modifier, sweep = { sweep.value })
}

@Composable
private fun ProviderCapsule(
    provider: String,
    quality: String?,
    palette: ArtworkPalette,
    canSwitch: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    sweep: () -> Float,
) {
    val ink = palette.onContainer
    Row(
        modifier = modifier
            .widthIn(max = 294.dp)
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(palette.surface.copy(alpha = .90f))
            .border(1.dp, palette.accent.copy(alpha = .29f), CircleShape)
            .drawWithCache {
                val band = size.width * .48f
                val silver = Brush.linearGradient(
                    listOf(Color.Transparent, palette.accent.copy(alpha = .23f), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(band, size.height * .55f),
                )
                onDrawBehind {
                    val fraction = sweep()
                    if (fraction < 1f) translate(left = (size.width + band) * fraction - band) {
                        drawRect(silver, size = Size(band, size.height))
                    }
                }
            }
            .clickable(
                enabled = canSwitch,
                role = Role.Button,
                onClickLabel = stringResource(R.string.pc_force_title),
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.graphic_eq), null, Modifier.size(14.dp), tint = palette.accent)
        Text(
            provider,
            modifier = Modifier.weight(1f, fill = false),
            color = ink,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (quality != null) {
            Text(
                quality,
                modifier = Modifier.widthIn(max = 92.dp)
                    .background(palette.container, CircleShape)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                color = palette.onContainer,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (canSwitch) Icon(painterResource(R.drawable.navigate_next), null, Modifier.size(13.dp), tint = ink.copy(alpha = .62f))
    }
}
