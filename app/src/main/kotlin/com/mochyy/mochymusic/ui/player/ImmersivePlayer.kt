package com.mochyy.mochymusic.ui.player

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.playback.PlayerConnection
import com.mochyy.mochymusic.ui.player.models.TrackInfo
import com.mochyy.mochymusic.ui.utils.AppMotion
import com.mochyy.mochymusic.utils.rememberEnumPreference
import kotlinx.coroutines.delay

private enum class ImmersiveStyle(@StringRes val title: Int, @StringRes val description: Int, @DrawableRes val icon: Int) {
    CLASSIC(R.string.immersive_classic, R.string.immersive_classic_description, R.drawable.music_note),
    ARTWORK_CINEMA(R.string.immersive_cinema, R.string.immersive_cinema_description, R.drawable.image),
    LYRICS_FOCUS(R.string.immersive_lyrics_focus, R.string.immersive_lyrics_description, R.drawable.lyrics),
}

private val ImmersiveStyleKey = stringPreferencesKey("pippo.immersive.style")
private val SceneBase = Color(0xFF141119)
private val SceneInk = Color(0xFFFFF7F7)
private val SceneMuted = Color(0xFFDACDD4)
private val SceneAccent = Color(0xFFF2B5A7)
private val DockColor = Color(0xFF241D26)

@Composable
internal fun ImmersivePlayer(onExit: () -> Unit) {
    val connection = LocalPlayerConnection.current ?: return
    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        ImmersiveSystemBars()
        ImmersiveContent(connection, onExit)
    }
}

@Composable
private fun ImmersiveSystemBars() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(window, view, lifecycle) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val oldBehavior = controller.systemBarsBehavior
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        val oldKeepAwake = view.keepScreenOn
        val observer = LifecycleEventObserver { _, _ ->
            view.keepScreenOn = oldKeepAwake || lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            view.keepScreenOn = oldKeepAwake
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = oldBehavior
        }
    }
}

@Composable
private fun ImmersiveContent(connection: PlayerConnection, onExit: () -> Unit) {
    var style by rememberEnumPreference(ImmersiveStyleKey, ImmersiveStyle.CLASSIC)
    val selectedStyle = style
    val track by connection.uiState.trackInfo.collectAsStateWithLifecycle()
    val playing by connection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val canSkipNext by connection.canSkipNext.collectAsStateWithLifecycle()
    val canSkipPrevious by connection.canSkipPrevious.collectAsStateWithLifecycle()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    var controlsVisible by remember { mutableStateOf(true) }
    var pickerOpen by remember { mutableStateOf(false) }
    var dockPressed by remember { mutableStateOf(false) }
    var interaction by remember { mutableIntStateOf(0) }
    fun revealControls() {
        controlsVisible = true
        interaction++
    }
    val toggleControls: () -> Unit = {
        if (controlsVisible) controlsVisible = false else revealControls()
    }
    val timeout = LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
        5_500L, containsIcons = true, containsText = true, containsControls = true,
    ) ?: 5_500L
    LaunchedEffect(controlsVisible, pickerOpen, dockPressed, playing, resumed, interaction, timeout) {
        if (controlsVisible && playing && resumed && !pickerOpen && !dockPressed) {
            delay(timeout)
            controlsVisible = false
        }
    }
    val currentToggle by rememberUpdatedState(toggleControls)
    val currentSwipe by rememberUpdatedState<(Float) -> Unit> { distance ->
        when {
            distance < 0 && canSkipNext -> {
                connection.seekToNext()
                revealControls()
            }
            distance > 0 && canSkipPrevious -> {
                connection.seekToPrevious()
                revealControls()
            }
            else -> Unit
        }
    }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = SceneAccent,
            onPrimary = SceneBase,
            surface = DockColor,
            onSurface = SceneInk,
            onSurfaceVariant = SceneMuted,
        ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().background(SceneBase)) {
            val dockMaxHeight = maxHeight * .48f
            ImmersiveBackdrop(selectedStyle, track.thumbnailUrl)
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(shape = CircleShape, color = DockColor) {
                        IconButton(onClick = onExit) {
                            Icon(painterResource(R.drawable.fullscreen_exit), stringResource(R.string.immersive_exit), tint = SceneInk)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!controlsVisible) {
                            Surface(shape = CircleShape, color = DockColor) {
                                IconButton(onClick = { revealControls() }) {
                                    Icon(painterResource(R.drawable.expand_less), stringResource(R.string.immersive_show_controls), tint = SceneInk)
                                }
                            }
                        }
                        Box {
                            Surface(shape = CircleShape, color = DockColor) {
                                TextButton(onClick = { revealControls(); pickerOpen = true }) {
                                    Text(stringResource(selectedStyle.title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(painterResource(R.drawable.expand_more), null, Modifier.size(18.dp))
                                }
                            }
                            DropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                                ImmersiveStyle.entries.forEach { choice ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(stringResource(choice.title), style = MaterialTheme.typography.titleSmall)
                                                Text(stringResource(choice.description), style = MaterialTheme.typography.bodySmall)
                                            }
                                        },
                                        leadingIcon = { Icon(painterResource(choice.icon), null) },
                                        trailingIcon = if (choice == selectedStyle) {
                                            { Icon(painterResource(R.drawable.check), stringResource(R.string.immersive_selected), tint = SceneAccent) }
                                        } else null,
                                        onClick = {
                                            pickerOpen = false
                                            style = choice
                                            revealControls()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                val toggleLabel = stringResource(if (controlsVisible) R.string.immersive_hide_controls else R.string.immersive_show_controls)
                Box(
                    Modifier.weight(1f).fillMaxWidth()
                        .semantics {
                            onClick(label = toggleLabel) { currentToggle(); true }
                        }
                        .pointerInput(Unit) { detectTapGestures { currentToggle() } }
                        .pointerInput(connection) {
                            val threshold = 72.dp.toPx()
                            var distance = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { distance = 0f },
                                onDragCancel = { distance = 0f },
                                onDragEnd = { if (kotlin.math.abs(distance) >= threshold) currentSwipe(distance) },
                            ) { change, amount ->
                                change.consume()
                                distance += amount
                            }
                        },
                ) {
                    when {
                        selectedStyle == ImmersiveStyle.ARTWORK_CINEMA ->
                            ImmersiveSongTitle(track, Modifier.align(Alignment.BottomStart).padding(24.dp))
                        resumed && !pickerOpen -> LyricsScreen(
                            onClose = onExit,
                            screenHeight = maxHeight,
                            contentBottomInset = 0.dp,
                            showHeader = false,
                            immersivePresentation = true,
                            modifier = Modifier.align(Alignment.Center).widthIn(max = 720.dp).fillMaxSize(),
                        )
                    }
                }

                val dockFadeMs = if (AppMotion.durationScale.scaleFactor > 0f) 140 else 0
                AnimatedVisibility(
                    visible = controlsVisible,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enter = fadeIn(tween(dockFadeMs)),
                    exit = fadeOut(tween(dockFadeMs)),
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .heightIn(max = dockMaxHeight),
                        shape = RoundedCornerShape(24.dp),
                        color = DockColor,
                        contentColor = SceneInk,
                    ) {
                        Column(
                            Modifier.verticalScroll(rememberScrollState())
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                        dockPressed = true
                                        try {
                                            do {
                                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                            } while (event.changes.any { it.pressed })
                                        } finally {
                                            dockPressed = false
                                            interaction++
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (selectedStyle != ImmersiveStyle.ARTWORK_CINEMA) {
                                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (track.artist.isNotBlank()) {
                                    Text(track.artist, color = SceneMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            PlayerProgressSlider(active = controlsVisible && resumed && !pickerOpen)
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(enabled = canSkipPrevious, onClick = { revealControls(); connection.seekToPrevious() }) {
                                    Icon(painterResource(R.drawable.skip_previous), stringResource(R.string.immersive_previous))
                                }
                                FilledIconButton(
                                    onClick = { revealControls(); connection.togglePlayPause() },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = SceneAccent, contentColor = SceneBase),
                                ) {
                                    Icon(
                                        painterResource(if (playing) R.drawable.pause else R.drawable.play),
                                        stringResource(if (playing) R.string.pause else R.string.play),
                                    )
                                }
                                IconButton(enabled = canSkipNext, onClick = { revealControls(); connection.seekToNext() }) {
                                    Icon(painterResource(R.drawable.skip_next), stringResource(R.string.immersive_next))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImmersiveSongTitle(track: TrackInfo, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(track.title, style = MaterialTheme.typography.headlineMedium, color = SceneInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(track.artist, style = MaterialTheme.typography.titleMedium, color = SceneMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ImmersiveBackdrop(style: ImmersiveStyle, artwork: String?) {
    val context = LocalContext.current
    val lyricsFocus = style == ImmersiveStyle.LYRICS_FOCUS
    val cinema = style == ImmersiveStyle.ARTWORK_CINEMA
    Box(Modifier.fillMaxSize()) {
        if (!lyricsFocus && !artwork.isNullOrBlank()) {
            val request = remember(context, artwork, cinema) {
                ImageRequest.Builder(context).data(artwork).size(if (cinema) 1200 else 720).build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().fillMaxSize(if (cinema) 1f else .66f),
            )
        }
        Box(Modifier.fillMaxSize().drawWithCache {
            val shade = when (style) {
                ImmersiveStyle.CLASSIC -> Brush.verticalGradient(
                    0f to SceneBase.copy(alpha = .44f),
                    .24f to SceneBase.copy(alpha = .79f),
                    .66f to SceneBase,
                )
                ImmersiveStyle.ARTWORK_CINEMA -> Brush.verticalGradient(
                    0f to SceneBase.copy(alpha = .55f),
                    .35f to Color.Transparent,
                    .61f to SceneBase.copy(alpha = .22f),
                    1f to SceneBase,
                )
                ImmersiveStyle.LYRICS_FOCUS -> Brush.verticalGradient(
                    0f to Color(0xFF251B2B),
                    .46f to Color(0xFF1A1420),
                    1f to SceneBase,
                )
            }
            onDrawBehind { drawRect(shade) }
        })
    }
}
