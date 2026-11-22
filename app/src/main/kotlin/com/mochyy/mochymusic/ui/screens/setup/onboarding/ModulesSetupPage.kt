package com.mochyy.mochymusic.ui.screens.setup.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.module.TrackQuery
import com.mochyy.mochymusic.module.monochrome.MonochromeModule
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.screens.settings.modules.ModuleLogo
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
private val SetupDetail = Color(0xFFE1E1E8)
private val SetupAccent = Color(0xFFB3EAD6)
private val SetupOutline = Color(0xFF494954)
private val SetupCard = RoundedCornerShape(22.dp)
private val SongBackdrop = Brush.linearGradient(listOf(Color(0xFF323B61), Color(0xFF23242F)))

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
    val selected = chosenSong ?: suggested?.toSetupSong() ?: OneKiss
    val fetching = fetchResults.values.any { it == ModuleFetchResult.Fetching }
    val canFetch = active && modules.any(viewModel::canFetch)

    fun cancelFetches() {
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
        badge = { PageBadge(painterResource(R.drawable.graphic_eq)) },
        title = stringResource(R.string.onb_modules_title),
        subtitle = stringResource(R.string.onb_modules_subtitle),
        modifier = Modifier.imePadding(),
        footer = {
            OnbButton(stringResource(R.string.onb_continue), onClick = {
                cancelFetches()
                focus.clearFocus()
                onContinue()
            }, enabled = active)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OnbTextField(query, { query = it }, stringResource(R.string.module_test_search), placeholderColor = SetupDetail)
            if (query.isNotBlank()) {
                when {
                    searching -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp).align(Alignment.CenterHorizontally),
                        color = Onb.Subtitle,
                        strokeWidth = 2.dp,
                    )
                    matches.isEmpty() -> Text(stringResource(R.string.onb_modules_no_results), color = SetupDetail, fontSize = 14.sp)
                    else -> matches.forEach { item ->
                        key(item.id) {
                            val song = item.toSetupSong()
                            SetupSongRow(song, canFetch, onClick = { fetch(song) })
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().clip(SetupCard).background(SongBackdrop)
                    .border(1.dp, SetupOutline, SetupCard).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SetupSongRow(selected, enabled = canFetch && !fetching, featured = true, onClick = { fetch() })
                OnbButton(
                    text = stringResource(if (fetching) R.string.cancel else R.string.onb_modules_fetch),
                    onClick = { if (fetching) cancelFetches() else fetch() },
                    enabled = active && (fetching || canFetch),
                )
            }
            if (modules.isNotEmpty()) {
                Text(
                    stringResource(R.string.onb_modules_sources),
                    modifier = Modifier.padding(top = 8.dp, start = 2.dp, bottom = 2.dp),
                    color = Onb.Title,
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
    onCheckedChange: (Boolean) -> Unit,
    leading: @Composable () -> Unit,
) {
    val detailColor = when (result) {
        is ModuleFetchResult.Found -> SetupAccent
        ModuleFetchResult.Timeout, ModuleFetchResult.Unavailable, ModuleFetchResult.Unsupported -> Color(0xFFFFD5B0)
        else -> SetupDetail
    }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp)
            .clip(SetupCard).background(Onb.Card).border(1.dp, SetupOutline, SetupCard)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Onb.Title, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (result is ModuleFetchResult.Found) {
                    Icon(painterResource(R.drawable.check), null, Modifier.size(14.dp), tint = detailColor)
                }
                Text(detail, color = detailColor, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        AppSwitch(checked, onCheckedChange = null, enabled = enabled)
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
