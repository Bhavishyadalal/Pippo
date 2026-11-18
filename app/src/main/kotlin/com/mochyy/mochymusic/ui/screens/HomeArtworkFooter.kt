package com.mochyy.mochymusic.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.BlurEffectCache
import com.mochyy.mochymusic.ui.utils.AppMotion
import kotlinx.coroutines.flow.collectLatest

internal const val HomeArtworkKey = "pippo_home_artwork"
private val scenes = listOf(
    R.drawable.pippo_home_scene_01, R.drawable.pippo_home_scene_02,
    R.drawable.pippo_home_scene_03, R.drawable.pippo_home_scene_04,
    R.drawable.pippo_home_scene_05, R.drawable.pippo_home_scene_06,
    R.drawable.pippo_home_scene_07, R.drawable.pippo_home_scene_08,
    R.drawable.pippo_home_scene_09, R.drawable.pippo_home_scene_10,
)

internal class HomeArtworkRotation private constructor(
    current: Int,
    wasVisited: Boolean,
    pending: List<Int>,
) {
    constructor() : this(0, false, emptyList())
    private val remaining = ArrayDeque(pending)
    var artwork by mutableIntStateOf(current)
        private set
    var visited by mutableStateOf(wasVisited)
        private set

    init { if (artwork == 0) advance() }

    fun markVisited() { visited = true }

    fun advance() {
        if (remaining.isEmpty()) {
            val nextRound = scenes.shuffled().toMutableList()
            if (nextRound.first() == artwork) {
                nextRound.add(nextRound.removeAt(0))
            }
            remaining.addAll(nextRound)
        }
        artwork = remaining.removeFirst()
        visited = false
    }

    companion object {
        val Saver = listSaver<HomeArtworkRotation, Int>(
            save = { listOf(it.artwork, if (it.visited) 1 else 0) + it.remaining },
            restore = { HomeArtworkRotation(it[0], it[1] == 1, it.drop(2)) },
        )
    }
}

private enum class FooterVisibility { Entered, Between, Above }

@Composable
internal fun FollowHomeArtworkScroll(list: LazyListState, rotation: HomeArtworkRotation, enabled: Boolean) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(list, rotation, enabled, lifecycle) {
        if (!enabled) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow {
                val layout = list.layoutInfo
                val item = layout.visibleItemsInfo.firstOrNull { it.key == HomeArtworkKey }
                val visible = item?.let {
                    (minOf(it.offset + it.size, layout.viewportEndOffset - layout.afterContentPadding) -
                        maxOf(it.offset, layout.viewportStartOffset)).coerceAtLeast(0)
                } ?: 0
                when {
                    item != null && item.size > 0 && visible >= item.size * .34f -> FooterVisibility.Entered
                    item == null && list.lastScrolledBackward -> FooterVisibility.Above
                    else -> FooterVisibility.Between
                }
            }.collect { visibility ->
                when (visibility) {
                    FooterVisibility.Entered -> rotation.markVisited()
                    FooterVisibility.Above -> if (rotation.visited) rotation.advance()
                    FooterVisibility.Between -> Unit
                }
            }
        }
    }
}

@Composable
internal fun HomeArtworkFooter(rotation: HomeArtworkRotation) {
    val artwork = rotation.artwork
    val reveal = remember(artwork) { Animatable(0f) }
    val reducedMotion = AppMotion.durationScale.scaleFactor <= 0f
    val painter = rememberAsyncImagePainter(artwork)
    val imageState by painter.state.collectAsState()
    val loaded = imageState is AsyncImagePainter.State.Success
    LaunchedEffect(artwork, loaded, rotation.visited, reducedMotion) {
        if (reducedMotion) reveal.snapTo(1f)
        else if (loaded && rotation.visited) reveal.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    val stillRevealing by remember(reveal) { derivedStateOf { reveal.value < 1f } }

    Box(Modifier.fillMaxWidth().aspectRatio(1.93f).clipToBounds()) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val progress = if (reducedMotion) 1f else reveal.value
                alpha = .68f + .32f * progress
                translationY = (1f - progress) * 5.dp.toPx()
            },
        )
        if (loaded && rotation.visited && !reducedMotion && stillRevealing) {
            // A second pass draws only the upper strip. The lower scene stays sharp.
            Box(
                Modifier.fillMaxWidth().aspectRatio(5.5f).clipToBounds()
                    .graphicsLayer {
                        val remaining = 1f - reveal.value
                        compositingStrategy = CompositingStrategy.Offscreen
                        alpha = remaining
                        renderEffect = BlurEffectCache.element(8.dp.toPx() * remaining)
                    }
                    .drawWithCache {
                        val mask = Brush.verticalGradient(
                            0f to Color.White,
                            .57f to Color.White,
                            1f to Color.Transparent,
                        )
                        val glow = Brush.radialGradient(
                            listOf(Color(0x55F7AC8B), Color.Transparent),
                            center = center.copy(y = size.height * .83f),
                            radius = size.width * .42f,
                        )
                        val sceneSize = Size(size.width, size.width / 1.93f)
                        onDrawBehind {
                            with(painter) { draw(size = sceneSize) }
                            drawRect(glow)
                            drawRect(mask, blendMode = BlendMode.DstIn)
                        }
                    },
            )
        }
        Box(Modifier.fillMaxSize().drawWithCache {
            val seam = Brush.verticalGradient(
                0f to Color.Black,
                .27f to Color.Transparent,
                1f to Color.Transparent,
            )
            onDrawBehind { drawRect(seam) }
        })
    }
}
