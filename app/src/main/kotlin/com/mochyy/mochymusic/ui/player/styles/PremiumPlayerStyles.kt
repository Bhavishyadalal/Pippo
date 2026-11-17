package com.mochyy.mochymusic.ui.player.styles

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mochyy.mochymusic.LocalAppContentObscured
import com.mochyy.mochymusic.LocalArtworkColor
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.PlayerStyle
import com.mochyy.mochymusic.extensions.toggleRepeatMode
import com.mochyy.mochymusic.ui.player.CarouselPictures
import com.mochyy.mochymusic.ui.player.PlayerProgressSlider
import com.mochyy.mochymusic.ui.player.models.TransportState
import com.mochyy.mochymusic.ui.utils.AppMotion
import com.mochyy.mochymusic.ui.utils.isTvLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.math.cos
import kotlin.math.sin

private val DeckInk = Color(0xFFF7EEE9)
private val DeckPeach = Color(0xFFEAB0A2)
private val DeckBlack = Color(0xFF151318)

/** All four looks use the existing session, progress slider, gestures and overlay pages. */
@Composable
internal fun PremiumPlayerStyle(style: PlayerStyle, actions: StyleActions, modifier: Modifier, preview: Boolean) {
    val tones = rememberStyleTones().copy(ink = DeckInk, inkSoft = DeckInk.copy(alpha = .65f), chip = Color.White.copy(alpha = .10f))
    val track = rememberStyleTrack()
    val carousel = rememberStyleCarousel(preview)
    val motion = rememberDeckMotion(preview)
    val position = rememberLivePosition(animated = false)
    val duration = rememberDurationMs()
    val seed = LocalArtworkColor.current
    StyleFrame(preview, modifier, tones, background = {
        Box(Modifier.fillMaxSize().background(DeckBlack).drawBehind {
            drawRect(Brush.radialGradient(
                listOf(seed.copy(alpha = .30f), Color.Transparent),
                center = Offset(size.width * .8f, size.height * .15f), radius = size.maxDimension * .85f,
            ))
        })
        if (style == PlayerStyle.MIDNIGHT_GLASS && android.os.Build.VERSION.SDK_INT >= 31 && AppMotion.elementBlur) {
            val context = LocalContext.current
            val backdrop = remember(track.artwork, context) { ImageRequest.Builder(context).data(track.artwork).size(128).build() }
            AsyncImage(backdrop, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = .16f }.blur(48.dp))
        }
    }) { landscape, menuOpen, setMenuOpen ->
        val insets = styleInsets(landscape)
        BoxWithConstraints(Modifier.fillMaxSize().padding(start = insets.start, end = insets.end, top = insets.top, bottom = insets.bottom)) {
            val fontScale = LocalDensity.current.fontScale
            val wide = maxWidth > maxHeight && maxWidth >= 560.dp
            val compact = maxHeight < 680.dp || fontScale > 1.15f
            val heroHeight = minOf(maxWidth - 40.dp, maxHeight * (if (compact) .37f else .45f)).coerceAtLeast(120.dp)
            val hero: @Composable (Modifier) -> Unit = { heroModifier ->
                when (style) {
                    PlayerStyle.CASSETTE -> CassetteFace(track, motion, { DeckKinematics.fraction(position.positionMs(), duration) }, heroModifier)
                    PlayerStyle.CD_WALKMAN -> WalkmanFace(track, carousel, motion, heroModifier)
                    PlayerStyle.PIPPO_DUO -> DuoFace(motion, heroModifier)
                    else -> GlassFace(track, carousel, heroModifier)
                }
            }
            // Scrolling is an overflow fallback for large accessibility text, never a fixed-size clipped panel.
            if (wide) {
                Row(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                    Box(Modifier.weight(.8f).fillMaxHeight(), contentAlignment = Alignment.Center) { hero(Modifier.fillMaxWidth().aspectRatio(if (style == PlayerStyle.CASSETTE) 1.45f else 1f)) }
                    Column(Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                        DeckHeader(style)
                        DeckControls(style, track, carousel, actions, motion.active && !preview, preview)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 34.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    DeckHeader(style)
                    Spacer(Modifier.height(if (compact) 14.dp else 26.dp))
                    hero(Modifier.widthIn(max = 560.dp).fillMaxWidth().height(heroHeight))
                    Spacer(Modifier.height(if (compact) 14.dp else 28.dp))
                    DeckControls(style, track, carousel, actions, motion.active && !preview, preview)
                }
            }
        }
        StyleMenu(menuOpen, setMenuOpen, tones, actions, atBottom = false, end = insets.end + 18.dp, edge = insets.top + 12.dp, buttonSize = 44.dp, queue = true)
    }
}

@Composable
private fun DeckHeader(style: PlayerStyle) {
    Column(Modifier.fillMaxWidth().padding(end = 42.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("P I P P O", color = DeckInk.copy(alpha = .55f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(style.deckTitle), color = DeckInk, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    }
}

private val PlayerStyle.deckTitle: Int get() = when (this) {
    PlayerStyle.CASSETTE -> R.string.pippo_player_cassette
    PlayerStyle.MIDNIGHT_GLASS -> R.string.pippo_player_midnight
    PlayerStyle.PIPPO_DUO -> R.string.pippo_player_duo
    else -> R.string.pippo_player_walkman
}

/** Only this small state holder ticks; Canvas/layer consumers read the angle during drawing. */
@Stable
private class DeckMotion {
    var degrees by mutableFloatStateOf(0f)
    var active by mutableStateOf(false)
}

@Composable
private fun rememberDeckMotion(preview: Boolean): DeckMotion {
    val motion = remember { DeckMotion() }
    val connection = LocalPlayerConnection.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val showing = rememberStyleShowing()
    val obscured = LocalAppContentObscured.current
    LaunchedEffect(connection, lifecycle, preview, showing, obscured) {
        motion.active = false
        if (preview || connection == null) return@LaunchedEffect
        combine(
            lifecycle.currentStateFlow,
            connection.isEffectivelyPlaying,
            snapshotFlow { showing() && !obscured.value },
            snapshotFlow { AppMotion.enabled && AppMotion.durationScale.scaleFactor > 0f },
        ) { state, playing, visible, animations ->
            val active = state.isAtLeast(Lifecycle.State.STARTED) && visible
            active to (active && playing && animations)
        }.distinctUntilChanged().collectLatest { (active, moving) ->
            motion.active = active
            if (moving) {
                var last = 0L
                while (true) withFrameNanos { now ->
                    if (last != 0L) motion.degrees = DeckKinematics.advance(motion.degrees, now - last)
                    last = now
                }
            }
        }
    }
    DisposableEffect(motion) { onDispose { motion.active = false } }
    return motion
}

@Composable
private fun AlbumPicture(url: String?, modifier: Modifier) {
    val context = LocalContext.current
    val crossfade = if (AppMotion.enabled) 240 else 0
    val request = remember(url, context, crossfade) { ImageRequest.Builder(context).data(url).crossfade(crossfade).build() }
    Box(modifier.background(Color(0xFF3A2D3D)), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_launcher_monochrome), null, tint = DeckInk.copy(alpha = .3f), modifier = Modifier.size(42.dp))
        AsyncImage(request, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun GlassFace(track: StyleTrack, carousel: com.mochyy.mochymusic.ui.player.SongCarousel?, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize(.9f).drawBehind {
            drawCircle(Brush.radialGradient(listOf(DeckPeach.copy(alpha = .20f), Color.Transparent), radius = size.minDimension * .7f), radius = size.minDimension * .7f)
        })
        CarouselPictures(carousel, track.artwork, Modifier.fillMaxHeight(.92f).aspectRatio(1f).clip(RoundedCornerShape(26.dp)).border(1.dp, Color.White.copy(alpha = .18f), RoundedCornerShape(26.dp))) { url, item -> AlbumPicture(url, item) }
    }
}

@Composable
private fun DuoFace(motion: DeckMotion, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(28.dp)).border(1.dp, DeckPeach.copy(alpha = .18f), RoundedCornerShape(28.dp))) {
        AsyncImage(R.drawable.pippo_duo_player, contentDescription = null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize().graphicsLayer {
            // Gentle camera drift, rather than distorting a flattened face or pretending it has rigged limbs.
            val phase = motion.degrees * Math.PI / 180.0
            scaleX = 1.035f; scaleY = 1.035f
            translationX = (sin(phase) * 3.dp.toPx()).toFloat()
            translationY = (cos(phase) * 2.dp.toPx()).toFloat()
        })
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, DeckBlack.copy(alpha = .72f)), startY = 0f)))
        Text(stringResource(R.string.pippo_player_duo_caption), color = DeckInk.copy(alpha = .8f), fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomStart).padding(18.dp))
    }
}

@Composable
private fun CassetteFace(track: StyleTrack, motion: DeckMotion, fraction: () -> Float, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(22.dp)).background(Brush.verticalGradient(listOf(Color(0xFF555052), Color(0xFF1B191B)))).border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(22.dp))) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                drawRoundRect(Color(0xFFE5D3B7), Offset(w * .06f, h * .07f), Size(w * .88f, h * .53f), CornerRadius(w * .025f))
                drawRect(Color(0xFFBE6F49), Offset(w * .06f, h * .34f), Size(w * .88f, h * .055f))
                drawRoundRect(Color(0xFF171519), Offset(w * .16f, h * .4f), Size(w * .68f, h * .34f), CornerRadius(h * .17f))
                val (left, right) = DeckKinematics.tapeRadii(fraction())
                listOf(.32f to left, .68f to right).forEach { (x, radius) ->
                    val center = Offset(w * x, h * .57f); val outer = h * .16f
                    drawCircle(Color(0xFF574038), outer * radius, center)
                    drawCircle(Color(0xFFB4A89A), outer * .40f, center)
                    rotate(motion.degrees, center) {
                        repeat(6) { tooth ->
                            rotate(tooth * 60f, center) { drawLine(Color(0xFF1D191B), center + Offset(0f, outer * .1f), center + Offset(0f, outer * .38f), outer * .12f) }
                        }
                    }
                    drawCircle(Color(0xFF171519), outer * .18f, center)
                }
                drawRoundRect(Color(0xFF302C2F), Offset(w * .22f, h * .79f), Size(w * .56f, h * .15f), CornerRadius(8f))
                for (x in listOf(.045f, .955f)) for (y in listOf(.065f, .92f)) {
                    val center = Offset(w * x, h * y)
                    drawCircle(Color(0xFF9D9690), w * .012f, center)
                    drawLine(Color(0xFF272329), center - Offset(w * .006f, 0f), center + Offset(w * .006f, 0f), 1.5f)
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp).padding(top = 18.dp)) {
                Text(track.title.ifBlank { stringResource(R.string.pippo_player_tape) }, color = Color(0xFF322725), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("PIPPO  •  TYPE I  /  A", color = Color(0xFF5F4D45), fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun WalkmanFace(track: StyleTrack, carousel: com.mochyy.mochymusic.ui.player.SongCarousel?, motion: DeckMotion, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(38.dp)).background(Brush.linearGradient(listOf(Color(0xFF797479).copy(alpha = .65f), Color(0xFF2D2931), Color(0xFF514952)))).border(1.5.dp, Color.White.copy(alpha = .35f), RoundedCornerShape(38.dp)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFF15131B), size.minDimension * .44f)
                drawCircle(Brush.sweepGradient(listOf(Color(0xFFDFDBE8), Color(0xFF9D94AB), Color(0xFFE7D0BC), Color(0xFF8BA3A8), Color(0xFFDFDBE8))), size.minDimension * .425f)
                repeat(7) { ring -> drawCircle(Color.White.copy(alpha = .10f), size.minDimension * (.28f + ring * .02f), style = Stroke(1f)) }
            }
            CarouselPictures(carousel, track.artwork, Modifier.fillMaxSize(.72f).clip(CircleShape).graphicsLayer { rotationZ = motion.degrees }) { url, item -> AlbumPicture(url, item) }
            Box(Modifier.fillMaxSize(.13f).clip(CircleShape).background(Color(0xFF201D25)).border(3.dp, Color(0xFFB8ACB8), CircleShape))
            Box(Modifier.align(Alignment.TopStart).padding(19.dp).size(5.dp).background(DeckPeach, CircleShape))
            Text("PIPPO / DIGITAL AUDIO", color = DeckInk.copy(alpha = .65f), fontSize = 8.sp, letterSpacing = 1.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp))
        }
    }
}

@Composable
private fun DeckControls(style: PlayerStyle, track: StyleTrack, carousel: com.mochyy.mochymusic.ui.player.SongCarousel?, actions: StyleActions, active: Boolean, preview: Boolean) {
    val connection = LocalPlayerConnection.current
    val transport by (connection?.uiState?.transportState ?: remember { flowOf(TransportState()) }).collectAsState(initial = TransportState())
    val likes = remember(connection) { connection?.uiState?.trackInfo?.map { it.liked }?.distinctUntilChanged() ?: flowOf(false) }
    val liked by likes.collectAsState(initial = false)
    val interactive = LocalStyleInteractive.current
    val onTv = isTvLayout()
    val enabled = active && !preview && connection != null && interactive()
    Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha = if (style == PlayerStyle.MIDNIGHT_GLASS) .065f else .035f)).border(1.dp, Color.White.copy(alpha = .09f), RoundedCornerShape(26.dp)).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                StyleSongs(carousel, track, StyleLyrics(null, emptyList())) { song, item ->
                    Column(item.then(if (style == PlayerStyle.CD_WALKMAN) Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFB9C4A8)).padding(12.dp) else Modifier)) {
                        val lcd = style == PlayerStyle.CD_WALKMAN
                        Text(song.track.title.ifBlank { stringResource(R.string.pippo_player_ready) }, color = if (lcd) Color(0xFF263023) else DeckInk, fontFamily = if (lcd) FontFamily.Monospace else FontFamily.Default, fontSize = if (lcd) 18.sp else 23.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(5.dp))
                        Text(song.track.artist, color = if (lcd) Color(0xFF42503D) else DeckInk.copy(alpha = .65f), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (!onTv) DeckButton(if (liked) R.drawable.favorite else R.drawable.favorite_border, R.string.like, enabled, selected = liked, toggle = true) { connection?.toggleLike() }
        }
        Spacer(Modifier.height(8.dp))
        PlayerProgressSlider(Modifier.fillMaxWidth(), active = active && !preview)
        if (!onTv) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compactTransport = maxWidth < 248.dp
                val repeatText = when (transport.repeatMode) { androidx.media3.common.Player.REPEAT_MODE_ONE -> R.string.repeat_mode_one; androidx.media3.common.Player.REPEAT_MODE_ALL -> R.string.repeat_mode_all; else -> R.string.repeat_mode_off }
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        if (!compactTransport) DeckButton(R.drawable.shuffle, R.string.shuffle, enabled, selected = transport.shuffleModeEnabled, toggle = true) { connection?.player?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
                        DeckButton(R.drawable.skip_previous, R.string.previous, enabled && transport.canSkipPrevious) { connection?.seekToPrevious() }
                        val playScale by animateFloatAsState(if (transport.isPlaying) 1f else .92f, tween(220), label = "deckPlay")
                        IconButton(
                            onClick = { if (interactive()) connection?.togglePlayPause() }, enabled = enabled,
                            modifier = Modifier.size(56.dp).graphicsLayer { scaleX = playScale; scaleY = playScale }.clip(if (style == PlayerStyle.CASSETTE) RoundedCornerShape(16.dp) else CircleShape).background(DeckPeach),
                        ) {
                            Icon(painterResource(if (transport.isPlaying) R.drawable.pause else R.drawable.play), stringResource(if (transport.isPlaying) R.string.pause else R.string.play), tint = DeckBlack, modifier = Modifier.size(28.dp))
                        }
                        DeckButton(R.drawable.skip_next, R.string.next, enabled && transport.canSkipNext) { connection?.seekToNext() }

                        if (!compactTransport) DeckButton(if (transport.repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat, repeatText, enabled, selected = transport.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF, toggle = true) { connection?.player?.toggleRepeatMode() }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
                        if (compactTransport) DeckButton(R.drawable.shuffle, R.string.shuffle, enabled, selected = transport.shuffleModeEnabled, toggle = true) { connection?.player?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
                        DeckButton(R.drawable.lyrics, R.string.lyrics, enabled && actions.onLyrics != null) { actions.onLyrics?.invoke() }
                        if (!compactTransport) Spacer(Modifier.width(20.dp))
                        DeckButton(R.drawable.queue_music, R.string.queue, enabled && actions.onQueue != null) { actions.onQueue?.invoke() }
                        if (compactTransport) DeckButton(if (transport.repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat, repeatText, enabled, selected = transport.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF, toggle = true) { connection?.player?.toggleRepeatMode() }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckButton(icon: Int, description: Int, enabled: Boolean, selected: Boolean = false, toggle: Boolean = false, onClick: () -> Unit) {
    val interactive = LocalStyleInteractive.current
    val stateLabel = stringResource(if (selected) R.string.pippo_player_on else R.string.pippo_player_off)
    IconButton(onClick = { if (interactive()) onClick() }, enabled = enabled, modifier = Modifier.size(48.dp).semantics { role = Role.Button; if (toggle) stateDescription = stateLabel }) {
        Icon(painterResource(icon), stringResource(description), tint = if (selected) DeckPeach else DeckInk, modifier = Modifier.size(23.dp))
    }
}
