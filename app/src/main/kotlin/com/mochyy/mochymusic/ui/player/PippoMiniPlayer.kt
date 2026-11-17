package com.mochyy.mochymusic.ui.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.LocalArtworkColor
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.*
import com.mochyy.mochymusic.ui.component.LocalMiniPillFrame
import com.mochyy.mochymusic.ui.component.pressBounce
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.mochyy.mochymusic.ui.customization.UiCustomizationState
import com.mochyy.mochymusic.ui.player.models.TrackInfo
import com.mochyy.mochymusic.ui.player.models.TransportState
import com.mochyy.mochymusic.ui.theme.ArtworkPalette
import com.mochyy.mochymusic.ui.utils.AppMotion
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

private enum class MiniAction { NEXT, PREVIOUS, FAVORITE, QUEUE }

/** One selected layout over the existing sheet. Audio state, swipes and the expansion belong to that sheet. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PippoMiniPlayer(
    track: TrackInfo,
    transport: TransportState,
    progressProvider: () -> Float,
    onExpand: () -> Unit,
    onQueue: () -> Unit,
) {
    // Drop controls and flow subscriptions once the opening player has taken over.
    val shown by remember(progressProvider) { derivedStateOf { progressProvider() < .18f } }
    if (!shown) return
    val connection = LocalPlayerConnection.current ?: return
    val ui = UiCustomizationState.options
    val options = ui.mini
    val frame = LocalMiniPillFrame.current
    val error by connection.error.collectAsStateWithLifecycle()
    val waiting by connection.waitingForNetworkConnection.collectAsStateWithLifecycle()
    val visible by connection.isUiVisible.collectAsStateWithLifecycle()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val status = when {
        waiting -> stringResource(R.string.mini_offline)
        error != null -> stringResource(R.string.mini_error)
        transport.playbackState == Player.STATE_BUFFERING -> stringResource(R.string.mini_buffering)
        transport.playbackState == Player.STATE_ENDED -> stringResource(R.string.mini_finished)
        !transport.isPlaying -> stringResource(R.string.mini_paused)
        else -> null
    }
    val surface = miniPlayerSurface()
    val gap = when (options.spacing) { UiSpacing.COMPACT -> 4.dp; UiSpacing.COMFORTABLE -> 8.dp; UiSpacing.ROOMY -> 12.dp }
    val coverSize = when (options.style) { MiniStyle.COMPACT_CARD -> 52.dp; MiniStyle.MINIMAL -> 36.dp; MiniStyle.CLASSIC -> 48.dp; else -> 44.dp }
    val coverRadius = when (options.corners) { MiniCorners.SQUARE -> 0.dp; MiniCorners.SOFT -> 10.dp; MiniCorners.ROUND -> coverSize / 2 }
    val expand = stringResource(R.string.mini_expand)
    BoxWithConstraints(
        Modifier.padding(start = frame.start, end = frame.end).fillMaxWidth().height(frame.height)
            .graphicsLayer { alpha = (1f - progressProvider() / .18f).coerceIn(0f, 1f) }
            .background(surface)
            .clickable(role = Role.Button, onClickLabel = expand, onClick = onExpand),
    ) {
        // Reserve artwork, readable title space, and 48dp action targets. Extras go into a menu.
        val narrow = maxWidth < 280.dp
        val artworkSize = if (narrow) 36.dp else coverSize.coerceAtMost(frame.height - 8.dp)
        val actionSlots = ((maxWidth - artworkSize - gap * 3 - 16.dp - 72.dp).value / 48f).toInt().coerceIn(2, 5)
        val requested = buildList {
            if (options.next) add(MiniAction.NEXT)
            if (options.previous) add(MiniAction.PREVIOUS)
            if (options.favorite) add(MiniAction.FAVORITE)
            if (options.queue) add(MiniAction.QUEUE)
        }
        val needsMenu = requested.size + 1 > actionSlots
        val direct = requested.take((actionSlots - 1 - if (needsMenu) 1 else 0).coerceAtLeast(0))
        val overflow = requested.drop(direct.size)
        fun perform(action: MiniAction) = when (action) {
            MiniAction.NEXT -> connection.seekToNext()
            MiniAction.PREVIOUS -> connection.seekToPrevious()
            MiniAction.FAVORITE -> connection.toggleLike()
            MiniAction.QUEUE -> onQueue()
        }
        fun enabled(action: MiniAction) = when (action) {
            MiniAction.NEXT -> transport.canSkipNext
            MiniAction.PREVIOUS -> transport.canSkipPrevious
            else -> true
        }
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(track.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(artworkSize).clip(RoundedCornerShape(coverRadius)).background(MaterialTheme.colorScheme.surfaceVariant))
            Spacer(Modifier.width(gap))
            Column(Modifier.weight(1f).padding(end = 4.dp), verticalArrangement = Arrangement.Center) {
                val marquee = options.marquee && visible && lifecycle.isAtLeast(Lifecycle.State.STARTED) && transport.isPlaying && AppMotion.enabled && AppMotion.durationScale.scaleFactor > 0f
                Text(track.title, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                    style = if (options.style == MiniStyle.TWO_LINE) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = if (marquee) Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2000) else Modifier)
                val detail = listOfNotNull(status.takeIf { options.status }, track.artist.takeIf { it.isNotBlank() && options.style != MiniStyle.MINIMAL }).joinToString(" · ")
                key(track.mediaId) {
                    MiniDetails(track.mediaId, detail, options.elapsed, options.quality && !ui.hideQuality,
                        if (options.status && (error != null || waiting)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            direct.filter { it == MiniAction.PREVIOUS }.forEach { action ->
                MiniControl(action.icon(track.liked), stringResource(action.label(track.liked)), enabled(action)) { perform(action) }
            }
            MiniControl(
                icon = if (transport.playbackState == Player.STATE_ENDED) R.drawable.replay else if (transport.isPlaying) R.drawable.pause else R.drawable.play,
                label = stringResource(if (transport.playbackState == Player.STATE_ENDED) R.string.mini_replay else if (transport.isPlaying) R.string.pause else R.string.play),
                enabled = true,
            ) { connection.togglePlayPause() }
            direct.filter { it != MiniAction.PREVIOUS }.forEach { action -> MiniControl(action.icon(track.liked), stringResource(action.label(track.liked)), enabled(action)) { perform(action) } }
            if (overflow.isNotEmpty()) {
                var menu by remember(track.mediaId) { mutableStateOf(false) }
                Box {
                    MiniControl(R.drawable.more_horiz, stringResource(R.string.mini_more), true) { menu = true }
                    DropdownMenu(menu, { menu = false }) {
                        overflow.forEach { action -> DropdownMenuItem(
                            text = { Text(stringResource(action.label(track.liked))) },
                            leadingIcon = { Icon(painterResource(action.icon(track.liked)), null) },
                            enabled = enabled(action),
                            onClick = { menu = false; perform(action) },
                        ) }
                    }
                }
            }
        }
        if (options.progress != MiniProgress.NONE) MiniProgressLine(
            Modifier.align(if (options.progress == MiniProgress.TOP) Alignment.TopCenter else Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp).height(2.dp),
        )
    }
}

@Composable
private fun MiniControl(icon: Int, label: String, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    IconButton(onClick, enabled = enabled, interactionSource = interaction, modifier = Modifier.size(48.dp).pressBounce(interaction)) {
        Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(22.dp))
    }
}

private fun MiniAction.icon(liked: Boolean): Int = when (this) {
    MiniAction.NEXT -> R.drawable.skip_next
    MiniAction.PREVIOUS -> R.drawable.skip_previous
    MiniAction.FAVORITE -> if (liked) R.drawable.favorite else R.drawable.favorite_border
    MiniAction.QUEUE -> R.drawable.queue_music
}
private fun MiniAction.label(liked: Boolean): Int = when (this) {
    MiniAction.NEXT -> R.string.mini_next
    MiniAction.PREVIOUS -> R.string.mini_previous
    MiniAction.FAVORITE -> if (liked) R.string.mini_unfavorite else R.string.mini_favorite
    MiniAction.QUEUE -> R.string.mini_queue
}

/** Subscribe at the leaf: position updates redraw the line, never the sheet or title. */
@Composable
private fun MiniProgressLine(modifier: Modifier) {
    val connection = LocalPlayerConnection.current ?: return
    val progress = connection.progressState.collectAsStateWithLifecycle()
    val color = MaterialTheme.colorScheme.primary
    Box(modifier.drawBehind {
        drawRect(color.copy(alpha = .15f))
        val current = progress.value
        if (current.duration > 0L) drawRect(color, size = size.copy(width = size.width * current.percentage.coerceIn(0f, 1f)))
    })
}

@Composable
private fun MiniDetails(mediaId: String, detail: String, elapsed: Boolean, quality: Boolean, color: Color) {
    val connection = LocalPlayerConnection.current ?: return
    val time = if (elapsed) {
        val secondsFlow = remember(connection, mediaId) {
            connection.progressState.map { it.position.coerceAtLeast(0L) / 1000 }.distinctUntilChanged()
        }
        // Reuse shared progress, but update text only when its displayed second changes.
        val seconds by secondsFlow.collectAsStateWithLifecycle(initialValue = 0L)
        "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
    } else null
    val codec = if (quality) {
        val flow = remember(connection, mediaId) { connection.streamFacts(mediaId) }
        val facts by flow.collectAsStateWithLifecycle(initialValue = null)
        streamBadgeText(facts, false)
    } else null
    val text = listOfNotNull(time, codec, detail.takeIf { it.isNotBlank() }).joinToString(" · ")
    if (text.isNotBlank()) Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
internal fun miniPlayerSurface(): Color {
    val ui = UiCustomizationState.options
    val options = ui.mini
    return when {
        ui.miniBlack -> Color.Black
        options.style == MiniStyle.ARTWORK_TINT -> {
            val seed = LocalArtworkColor.current
            remember(seed) { ArtworkPalette.from(seed).surface }
        }
        options.style == MiniStyle.MINIMAL -> MaterialTheme.colorScheme.background
        options.style == MiniStyle.COMPACT_CARD || options.style == MiniStyle.TWO_LINE || options.style == MiniStyle.CONNECTED_DOCK -> MaterialTheme.colorScheme.surfaceContainer
        else -> Color.Transparent // keep the existing shared glass beneath Classic / Floating pill
    }
}

/** Flat shared backing joins the phone's two rows; drawing never intercepts player gestures. */
@Composable
internal fun ConnectedMiniDockBackground(
    playerProgress: () -> Float,
    presence: () -> Float,
    bottomInset: Dp,
    navHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val color = if (UiCustomizationState.options.miniBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    Box(modifier.drawBehind {
        val alpha = ((1f - playerProgress() / .18f).coerceIn(0f, 1f) * presence()).coerceIn(0f, 1f)
        if (alpha <= 0f) return@drawBehind
        val top = size.height - bottomInset.toPx() - navHeight.toPx() - MiniPlayerHeight.toPx()
        val bottom = size.height - bottomInset.toPx() - 8.dp.toPx()
        drawRoundRect(color.copy(alpha = alpha),
            topLeft = Offset(8.dp.toPx(), top),
            size = Size((size.width - 16.dp.toPx()).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(12.dp.toPx()))
    })
}
