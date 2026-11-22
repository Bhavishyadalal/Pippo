package com.mochyy.mochymusic.ui.screens.setup.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.module.TrackQuery
import com.mochyy.mochymusic.module.monochrome.MonochromeModule
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.screens.settings.modules.ModuleLogo
import com.mochyy.mochymusic.ui.utils.AppMotion
import com.mochyy.mochymusic.viewmodels.ModuleFetchResult
import com.mochyy.mochymusic.viewmodels.OnboardingModulesViewModel
import com.mochyy.mochymusic.viewmodels.onboardingSampleTrack
import com.mochyy.mochymusic.viewmodels.searchModuleTestSongs
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private data class SetupSong(val track: TrackQuery, val cover: String? = null)

private val OneKiss = SetupSong(onboardingSampleTrack)
private val SetupDetail = Color(0xFFD0D9DF)
private val SetupSilver = Color(0xFFE0EBF0)
private val SetupAccent = Color(0xFFA4E8DB)
private val SetupWarning = Color(0xFFF3CAA8)
private val SetupOutline = Color(0xFF49555E)
private val SetupCard = RoundedCornerShape(24.dp)
private val SetupRow = RoundedCornerShape(18.dp)
private val SongBackdrop = Brush.linearGradient(listOf(Color(0xFF273742), Color(0xFF11191F), Color(0xFF1C2D31)))
private val SongRim = Brush.linearGradient(listOf(Color(0xFF81939D), Color(0xFF38454D), Color(0xFF556967)))

private fun SongItem.toSetupSong() = SetupSong(
    TrackQuery(title, artists.map { it.name }, album?.name, duration?.times(1000L)),
    thumbnail,
)

@Composable
internal fun ModulesSetupPage(
    active: Boolean,
    onContinue: () -> Unit,
    viewModel: OnboardingModulesViewModel = hiltViewModel(),
) {
    val allEntries by viewModel.entries.collectAsStateWithLifecycle()
    val modules = remember(allEntries) { allEntries.filter(viewModel::showInSetup) }
    val fetchResults = remember { mutableStateMapOf<String, ModuleFetchResult>() }
    val jobs = remember { mutableMapOf<String, Job>() }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var matches by remember { mutableStateOf(emptyList<SongItem>()) }
    var searching by remember { mutableStateOf(false) }
    val suggested by viewModel.suggestedSong.collectAsStateWithLifecycle()
    var chosenSong by remember { mutableStateOf<SetupSong?>(null) }
    var trackLookup by remember { mutableStateOf<Job?>(null) }
    var fetchRun by remember { mutableStateOf(0) }
    var showFetchProgress by remember { mutableStateOf(false) }
    val selected = chosenSong ?: suggested?.toSetupSong() ?: OneKiss
    val fetching = fetchResults.values.any { it == ModuleFetchResult.Fetching }
    val canFetch = active && modules.any(viewModel::canFetch)
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val motion = active && lifecycle.isAtLeast(Lifecycle.State.RESUMED) && AppMotion.durationScale.scaleFactor > 0f
    val titleSweep = remember { Animatable(1f) }
    LaunchedEffect(motion, fetchRun) {
        titleSweep.snapTo(1f)
        if (motion) {
            delay(120)
            titleSweep.snapTo(0f)
            titleSweep.animateTo(1f, tween(1_300, easing = LinearEasing))
        }
    }

    fun cancelFetches() {
        showFetchProgress = false
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        trackLookup?.cancel()
        trackLookup = null
        fetchResults.keys.filter { fetchResults[it] == ModuleFetchResult.Fetching }.forEach { fetchResults.remove(it) }
    }

    fun fetch(song: SetupSong? = chosenSong) {
        if (!canFetch) return
        cancelFetches()
        fetchResults.clear()
        fetchRun++
        showFetchProgress = true
        chosenSong = song
        query = ""
        focus.clearFocus()
        val track = scope.async {
            song?.track ?: suggested?.toSetupSong()?.track
                ?: withTimeoutOrNull(2_500L) { viewModel.loadSuggestedSong() }?.toSetupSong()?.track
                ?: onboardingSampleTrack
        }
        trackLookup = track
        modules.filter(viewModel::canFetch).forEach { entry ->
            fetchResults[entry.id] = ModuleFetchResult.Fetching
            jobs[entry.id] = scope.launch {
                fetchResults[entry.id] = viewModel.fetch(entry.id, track.await())
                jobs.remove(entry.id)
            }
        }
    }

    LaunchedEffect(active) {
        if (active) viewModel.loadSuggestedSong() else cancelFetches()
    }
    LaunchedEffect(active, query) {
        val text = query.trim()
        matches = emptyList()
        searching = active && text.isNotEmpty()
        if (!searching) return@LaunchedEffect
        delay(350)
        matches = withTimeoutOrNull(10_000L) { searchModuleTestSongs(text) }.orEmpty()
        searching = false
    }

    OnboardingPage(
        badge = { ModulesEmblem() },
        title = stringResource(R.string.onb_modules_title),
        subtitle = stringResource(R.string.onb_modules_subtitle),
        modifier = Modifier.imePadding(),
        titleModifier = Modifier.semantics { heading() }.silverText(titleSweep, motion),
        footer = {
            OnbButton(stringResource(R.string.onb_continue), onClick = {
                cancelFetches()
                focus.clearFocus()
                onContinue()
            }, enabled = active)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OnbTextField(
                query, { query = it }, stringResource(R.string.module_test_search),
                placeholderColor = SetupDetail,
                leadingIcon = painterResource(R.drawable.search_outline),
            )
            if (query.isNotBlank()) {
                when {
                    searching -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp).align(Alignment.CenterHorizontally),
                        color = Onb.Subtitle,
                        strokeWidth = 2.dp,
                    )
                    matches.isEmpty() -> Text(stringResource(R.string.onb_modules_no_results), color = SetupDetail, fontSize = 14.sp)
                    else -> Column(
                        Modifier.fillMaxWidth().clip(SetupRow).background(Color(0xFF182127))
                            .border(1.dp, SetupOutline, SetupRow).padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        matches.forEach { item ->
                            key(item.id) {
                                val song = item.toSetupSong()
                                SetupSongRow(song, canFetch, onClick = { fetch(song) })
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().clip(SetupCard).background(SongBackdrop)
                    .etchedSignal().border(1.dp, SongRim, SetupCard).padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SetupSongRow(selected, enabled = canFetch && !fetching, featured = true, onClick = { fetch() })
                SetupFetchButton(
                    fetching = fetching,
                    onClick = { if (fetching) cancelFetches() else fetch() },
                    enabled = active && (fetching || canFetch),
                )
                if (showFetchProgress && fetchResults.isNotEmpty()) {
                    val total = fetchResults.size
                    val completed = fetchResults.values.count { it != ModuleFetchResult.Fetching }
                    ModuleFetchProgress(completed, total)
                }
            }
            if (modules.isNotEmpty()) {
                Text(
                    stringResource(R.string.onb_modules_sources),
                    modifier = Modifier.padding(top = 8.dp, start = 2.dp, bottom = 2.dp),
                    color = SetupSilver,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            modules.forEach { entry ->
                key(entry.id) {
                    val result = fetchResults[entry.id]
                    SetupModuleRow(
                        title = entry.module.info.name,
                        detail = moduleFetchDescription(entry.enabled, entry.id == MonochromeModule.Id, result),
                        result = result,
                        checked = entry.enabled,
                        enabled = active,
                        motion = motion,
                        onCheckedChange = { enabled ->
                            if (active) {
                                jobs.remove(entry.id)?.cancel()
                                fetchResults.remove(entry.id)
                                viewModel.setEnabled(entry.id, enabled)
                            }
                        },
                        leading = { ModuleLogo(entry.id, entry.module.info.name, size = 36) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupModuleRow(
    title: String,
    detail: String,
    result: ModuleFetchResult?,
    checked: Boolean,
    enabled: Boolean,
    motion: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leading: @Composable () -> Unit,
) {
    val found = result is ModuleFetchResult.Found
    val detailColor = when (result) {
        is ModuleFetchResult.Found -> SetupAccent
        ModuleFetchResult.Timeout, ModuleFetchResult.Unavailable, ModuleFetchResult.Unsupported -> SetupWarning
        else -> SetupDetail
    }
    val resultIcon = when (result) {
        is ModuleFetchResult.Found -> R.drawable.check
        ModuleFetchResult.Fetching -> R.drawable.sync
        ModuleFetchResult.Timeout, ModuleFetchResult.Unavailable, ModuleFetchResult.Unsupported -> R.drawable.warning
        else -> null
    }
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(result, motion) {
        reveal.snapTo(1f)
        if (motion && result != null && result != ModuleFetchResult.Fetching) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(780, easing = FastOutSlowInEasing))
        }
    }
    val surface = if (found) Color(0xFF192B2C) else Color(0xFF1D252B)
    val rim = if (found) Color(0xFF49766E) else if (checked) SetupOutline else Color(0xFF37434A)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 78.dp)
            .clip(SetupRow).background(surface)
            .then(if (found && motion) Modifier.resultRipple(reveal) else Modifier)
            .border(1.dp, rim, SetupRow)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF10191D)),
            contentAlignment = Alignment.Center,
        ) { leading() }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                title,
                modifier = if (found) Modifier.silverText(reveal, motion) else Modifier,
                color = SetupSilver,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.graphicsLayer {
                    val rest = if (motion) reveal.value else 1f
                    translationY = 5.dp.toPx() * (1f - rest)
                    alpha = 0.74f + 0.26f * rest
                },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (resultIcon != null) {
                    Icon(painterResource(resultIcon), null, Modifier.size(14.dp), tint = detailColor)
                } else {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (checked) SetupAccent else SetupOutline))
                }
                Text(detail, color = detailColor, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        AppSwitch(checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {}, enabled = enabled)
    }
}

@Composable
private fun ModulesEmblem() {
    val rim = RoundedCornerShape(16.dp)
    Box(
        Modifier.size(52.dp).clip(rim).background(SongBackdrop).etchedSignal()
            .border(1.dp, SongRim, rim),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.graphic_eq), null, Modifier.size(24.dp), tint = SetupSilver)
    }
}

@Composable
private fun SetupFetchButton(fetching: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(if (fetching) R.string.cancel else R.string.onb_modules_fetch)
    val shape = RoundedCornerShape(14.dp)
    val fill = when {
        fetching -> Color(0xFF223238)
        enabled -> SetupSilver
        else -> Color(0xFF394950)
    }
    val ink = if (enabled && !fetching) Color(0xFF152329) else SetupDetail
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape).background(fill)
            .border(1.dp, if (fetching) SetupOutline else Color.Transparent, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(if (fetching) R.drawable.close else R.drawable.graphic_eq), null, Modifier.size(17.dp), tint = ink)
        Text(label, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ModuleFetchProgress(completed: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(SetupOutline)) {
            Box(
                Modifier.fillMaxWidth(completed.toFloat() / total.coerceAtLeast(1))
                    .fillMaxHeight().background(SetupAccent),
            )
        }
        Text(
            stringResource(R.string.onb_modules_progress, completed, total),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = SetupDetail,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Modifier.silverText(sweep: Animatable<Float, AnimationVector1D>, motion: Boolean): Modifier {
    val visible by remember(sweep) { derivedStateOf { sweep.value < 1f } }
    return graphicsLayer {
        compositingStrategy = if (motion && visible) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }.drawWithCache {
        val band = size.width * 0.62f
        val silver = Brush.linearGradient(
            0f to Color.Transparent,
            0.26f to Color(0x703C5363),
            0.49f to Color(0xFFB7D4E0),
            0.62f to Color.White,
            0.75f to Color(0xB8DBF6FF),
            1f to Color.Transparent,
            start = Offset.Zero,
            end = Offset(band * 0.8f, size.height * 0.48f),
        )
        onDrawWithContent {
            drawContent()
            if (motion && visible && band > 0f) {
                val travel = size.width + band
                // The text supplies the mask, so the silver never paints over the spaces.
                translate(left = travel * sweep.value - band) {
                    drawRect(silver, size = Size(band, size.height), blendMode = BlendMode.SrcAtop)
                }
            }
        }
    }
}

private fun Modifier.etchedSignal(): Modifier = drawWithCache {
    val wave = Path()
    val w = size.width
    val h = size.height
    val amplitude = h * 0.16f
    val trace = Brush.horizontalGradient(listOf(Color.Transparent, Color(0x38A8CDD8), Color(0x64C4E3E9)))
    repeat(3) { index ->
        val y = h * (0.47f + index * 0.10f)
        wave.moveTo(w * 0.25f, y)
        wave.cubicTo(w * 0.42f, y + amplitude, w * 0.57f, y + amplitude, w * 0.68f, y)
        wave.cubicTo(w * 0.79f, y - amplitude, w * 0.90f, y - amplitude, w, y)
    }
    onDrawBehind { drawPath(wave, trace, style = Stroke(1.dp.toPx())) }
}

private fun Modifier.resultRipple(reveal: Animatable<Float, AnimationVector1D>): Modifier = drawWithCache {
    val crestWidth = size.width * 0.5f
    val crest = Brush.horizontalGradient(listOf(Color.Transparent, Color(0x406BDEC8), Color.Transparent), endX = crestWidth)
    val wave = Path().apply {
        moveTo(0f, size.height * 0.56f)
        cubicTo(crestWidth * 0.26f, size.height * 0.32f, crestWidth * 0.51f, size.height * 0.78f, crestWidth, size.height * 0.43f)
    }
    val stroke = Stroke(18.dp.toPx())
    onDrawBehind {
        val phase = reveal.value
        if (phase < 1f) {
            val left = (size.width + crestWidth) * phase - crestWidth
            translate(left = left) {
                drawPath(wave, crest, style = stroke, alpha = 1f - phase * 0.6f)
            }
        }
    }
}

@Composable
private fun moduleFetchDescription(enabled: Boolean, autoEnables: Boolean, result: ModuleFetchResult?): String = when {
    result == ModuleFetchResult.Fetching -> stringResource(R.string.onb_modules_fetching)
    result is ModuleFetchResult.Found -> stringResource(R.string.onb_modules_found, result.quality)
    result == ModuleFetchResult.NoMatch -> stringResource(R.string.onb_modules_no_match)
    result == ModuleFetchResult.Timeout -> stringResource(R.string.onb_modules_timeout)
    result == ModuleFetchResult.Unsupported -> stringResource(R.string.onb_modules_unsupported)
    result == ModuleFetchResult.Unavailable -> stringResource(R.string.onb_modules_unavailable)
    !enabled && autoEnables -> stringResource(R.string.onb_modules_enable_on_success)
    !enabled -> stringResource(R.string.onb_modules_off)
    else -> stringResource(R.string.onb_modules_ready)
}

@Composable
private fun SetupSongRow(song: SetupSong, enabled: Boolean, featured: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(if (featured) 64.dp else 44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF343F67)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.music_note), null, tint = Color(0xFFDBE4FF), modifier = Modifier.size(21.dp))
            if (song.cover != null) {
                AsyncImage(song.cover, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(song.track.title, color = Onb.Title, fontSize = if (featured) 19.sp else 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(song.track.artists.joinToString(", "), color = SetupDetail, fontSize = 14.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
