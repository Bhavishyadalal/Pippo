package com.mochyy.mochymusic.ui.screens.gesture

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.NavController
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.*
import com.mochyy.mochymusic.ui.component.TopToaster
import com.mochyy.mochymusic.ui.customization.CustomizationProfiles
import com.mochyy.mochymusic.ui.customization.UiCustomization
import com.mochyy.mochymusic.ui.screens.NavigationTab
import com.mochyy.mochymusic.ui.screens.Screens
import com.mochyy.mochymusic.ui.screens.audiolab.LabCard
import com.mochyy.mochymusic.ui.screens.audiolab.LabToggleRow
import com.mochyy.mochymusic.utils.rememberEnumPreference
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

@Composable
private fun BetaCard(title: Int, subtitle: Int? = null, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val state = stringResource(if (expanded) R.string.beta_expanded else R.string.beta_collapsed)
    LabCard(padding = 20.dp) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { stateDescription = state }.clickable(role = Role.Button) { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleLarge)
        }
        if (expanded) {
            if (subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private fun Enum<*>.caption(): String = name.lowercase(Locale.ROOT).replace('_', ' ').replaceFirstChar { it.uppercaseChar() }

@Composable
private fun <T : Enum<T>> BetaChoice(title: Int, selected: T, values: List<T>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(title), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.widthIn(max = 160.dp).heightIn(min = 48.dp)) {
                Text(selected.caption(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded, { expanded = false }) {
                values.forEach { item ->
                    DropdownMenuItem(text = { Text(item.caption()) }, onClick = { expanded = false; onSelect(item) })
                }
            }
        }
    }
}

@Composable
private inline fun <reified T : Enum<T>> PreferenceChoice(title: Int, key: Preferences.Key<String>, default: T) {
    val (value, update) = rememberEnumPreference(key, default)
    BetaChoice(title, value, enumValues<T>().toList(), update)
}

@Composable
private fun PreferenceToggle(title: Int, key: Preferences.Key<Boolean>, default: Boolean) {
    val (value, update) = rememberPreference(key, default)
    LabToggleRow(stringResource(title), subtitle = null, checked = value, onCheckedChange = update)
}

@Composable
internal fun BetaPerformanceSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    BetaCard(R.string.beta_performance, R.string.beta_performance_help) {
        listOf(R.string.beta_light, R.string.beta_balanced, R.string.beta_expressive).forEachIndexed { index, label ->
            OutlinedButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        CustomizationProfiles.performance(context, index)
                        TopToaster.show(context.getString(R.string.beta_applied))
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { TopToaster.error(context.getString(R.string.beta_operation_failed)) }
                    finally { busy = false }
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(label)) }
        }
        PreferenceToggle(R.string.beta_loading_motion, UiLoadingMotionKey, true)
        PreferenceToggle(R.string.beta_press_feedback, UiPressEffectsKey, true)
    }
}

@Composable
internal fun BetaAppearanceSection() {
    val (accent) = rememberEnumPreference(UiAccentKey, UiAccent.CORAL)
    val (customColor, setCustomColor) = rememberPreference(UiCustomAccentKey, 0xFFF2B5A7.toInt())
    val context = LocalContext.current
    var hex by remember(customColor) { mutableStateOf("%06X".format(customColor and 0xFFFFFF)) }
    BetaCard(R.string.beta_appearance, R.string.beta_appearance_help) {
        PreferenceChoice(R.string.beta_accent, UiAccentKey, UiAccent.CORAL)
        if (accent == UiAccent.CUSTOM) {
            OutlinedTextField(hex, { hex = it.take(7).removePrefix("#") }, label = { Text(stringResource(R.string.beta_hex)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = {
                val parsed = hex.takeIf { it.length == 6 }?.toLongOrNull(16)
                if (parsed == null || parsed !in 0L..0xFFFFFFL) TopToaster.error(context.getString(R.string.beta_invalid_color))
                else setCustomColor(parsed.toInt() or 0xFF000000.toInt())
            }) { Text(stringResource(R.string.beta_apply_color)) }
        }
        PreferenceToggle(R.string.beta_contrast, UiContrastKey, false)
        PreferenceToggle(R.string.beta_black, PureBlackKey, false)
        PreferenceChoice(R.string.beta_font, UiFontKey, UiFont.INTER)
        val (scale, setScale) = rememberPreference(UiTextScaleKey, 1f)
        Text(stringResource(R.string.beta_text_size, (scale * 100).toInt()), style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(.9f, 1f, 1.1f, 1.2f).forEach { next ->
                TextButton(onClick = { setScale(next) }) { Text("${(next * 100).toInt()}%") }
            }
        }
        PreferenceChoice(R.string.beta_corners, UiCornersKey, UiCorners.SOFT)
        PreferenceChoice(R.string.beta_spacing, UiSpacingKey, UiSpacing.COMFORTABLE)
    }
}

@Composable
internal fun BetaNavigationSection() {
    val (order, setOrder) = rememberPreference(UiNavOrderKey, UiCustomization.DefaultNavOrder)
    val items = remember(order) { UiCustomization.ordered(Screens.MainScreens, order) { it.route } }
    BetaCard(R.string.beta_navigation, R.string.beta_navigation_help) {
        PreferenceChoice(R.string.beta_nav_style, UiNavStyleKey, UiNavStyle.CURRENT)
        PreferenceChoice(R.string.beta_nav_labels, UiNavLabelsKey, UiNavLabels.OFF)
        PreferenceChoice(R.string.beta_nav_indicator, UiNavIndicatorKey, UiNavIndicator.PILL)
        PreferenceChoice(R.string.beta_nav_icons, UiNavIconSizeKey, UiNavIconSize.NORMAL)
        PreferenceToggle(R.string.beta_nav_swipe, UiNavSwipeKey, true)
        Text(stringResource(R.string.beta_tab_order), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        items.forEachIndexed { index, screen ->
            val title = stringResource(screen.titleId)
            val upDescription = stringResource(R.string.beta_move_up, title)
            val downDescription = stringResource(R.string.beta_move_down, title)
            fun move(delta: Int) {
                val next = items.toMutableList()
                val target = index + delta
                if (target !in next.indices) return
                val other = next[target]; next[target] = screen; next[index] = other
                setOrder(next.joinToString(",") { it.route })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                TextButton({ move(-1) }, enabled = index > 0, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = upDescription }) { Text("↑") }
                TextButton({ move(1) }, enabled = index < items.lastIndex, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = downDescription }) { Text("↓") }
            }
        }
    }
}

@Composable
internal fun BetaMotionSection() {
    BetaCard(R.string.beta_motion, R.string.beta_motion_help) {
        PreferenceChoice(R.string.beta_page_transition, UiTransitionKey, UiTransition.DEFAULT)
        val (duration, setDuration) = rememberPreference(UiTransitionDurationKey, 200)
        Text(stringResource(R.string.beta_duration, duration), style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(140, 200, 300, 400).forEach { value -> TextButton(onClick = { setDuration(value) }) { Text("$value") } }
        }
        PreferenceToggle(R.string.beta_press_feedback, UiPressEffectsKey, true)
        PreferenceToggle(R.string.beta_haptics, UiHapticsKey, true)
        PreferenceToggle(R.string.beta_loading_motion, UiLoadingMotionKey, true)
        PreferenceToggle(R.string.beta_record_motion, DiscRotatingAnimationKey, true)
        PreferenceToggle(R.string.beta_animated_covers, CanvasThumbnailAnimationKey, true)
    }
}

@Composable
internal fun BetaCharacterSection() {
    BetaCard(R.string.beta_characters, R.string.beta_characters_help) {
        PreferenceChoice(R.string.beta_character_choice, UiCharacterKey, UiCharacter.RANDOM)
        com.mochyy.mochymusic.ui.brand.PippoCharacter(com.mochyy.mochymusic.ui.brand.PippoPose.Thanks, Modifier.fillMaxWidth().height(180.dp))
    }
}

@Composable
internal fun BetaMoreSection(nav: NavController) {
    BetaCard(R.string.beta_more, R.string.beta_more_help) {
        val (start, setStart) = rememberEnumPreference(DefaultOpenTabKey, NavigationTab.HOME)
        BetaChoice(R.string.beta_opening_tab, start, NavigationTab.entries, setStart)
        PreferenceChoice(R.string.beta_library_spacing, UiLibraryRowsKey, UiSpacing.COMFORTABLE)
        PreferenceChoice(R.string.beta_browse_tiles, GridItemsSizeKey, GridItemSize.BIG)
        PreferenceToggle(R.string.beta_mini_outline, MiniPlayerOutlineKey, true)
        PreferenceToggle(R.string.beta_mini_black, PureBlackMiniPlayerKey, false)
        PreferenceToggle(R.string.beta_hide_quality, HideAudioQualityBadgeKey, false)
        PreferenceToggle(R.string.beta_show_descriptions, HideSettingsDescriptionsKey, true)
        listOf(R.string.beta_lyrics_options to "now_playing_settings", R.string.beta_playback_options to "playback_settings").forEach { (label, route) ->
            OutlinedButton(onClick = { nav.navigate(route) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(label)) }
        }
    }
}

@Composable
internal fun BetaHomeSection() {
    val (order, setOrder) = rememberPreference(UiHomeOrderKey, "")
    val (hidden, setHidden) = rememberPreference(UiHomeHiddenKey, "")
    val shelves = remember(order) { UiCustomization.ordered(UiCustomization.HomeShelves, order) { it } }
    val disabled = remember(hidden) { hidden.split(',').toSet() }
    BetaCard(R.string.beta_home, R.string.beta_home_help) {
        shelves.forEachIndexed { index, id ->
            fun move(delta: Int) {
                val next = shelves.toMutableList(); val target = index + delta
                if (target !in next.indices) return
                val other = next[target]; next[target] = id; next[index] = other
                setOrder(next.joinToString(","))
            }
            Column {
                val label = when (id) {
                    "recently_played" -> R.string.beta_shelf_recent
                    "most_played" -> R.string.beta_shelf_most
                    "from_the_community" -> R.string.beta_shelf_community
                    "speed_dial" -> R.string.beta_shelf_speed
                    "history" -> R.string.beta_shelf_history
                    "live_shows" -> R.string.beta_shelf_live
                    "artist_list" -> R.string.beta_shelf_artists
                    "account_playlists" -> R.string.beta_shelf_playlists
                    "dont_forget_these_songs" -> R.string.beta_shelf_forgotten
                    "music_videos" -> R.string.beta_shelf_videos
                    else -> R.string.beta_shelf_hits
                }
                LabToggleRow(stringResource(label), null, id !in disabled, { visible ->
                    setHidden((if (visible) disabled - id else disabled + id).filter { it in UiCustomization.HomeShelves }.joinToString(","))
                })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { move(-1) }, enabled = index > 0) { Text(stringResource(R.string.beta_earlier)) }
                    TextButton(onClick = { move(1) }, enabled = index < shelves.lastIndex) { Text(stringResource(R.string.beta_later)) }
                }
            }
        }
        TextButton(onClick = { setOrder(""); setHidden("") }) { Text(stringResource(R.string.beta_restore_home)) }
    }
}

@Composable
internal fun BetaProfilesSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (raw) = rememberPreference(UiSavedProfilesKey, "[]")
    val saved = remember(raw) { CustomizationProfiles.profiles(raw) }
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    fun perform(work: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { work(); TopToaster.show(context.getString(R.string.beta_applied)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { TopToaster.error(context.getString(R.string.beta_operation_failed)) }
            finally { busy = false }
        }
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) perform {
            val document = CustomizationProfiles.export(context)
            withContext(Dispatchers.IO) {
                requireNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(document.toByteArray(Charsets.UTF_8)) }
            }
        }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) perform {
            val text = withContext(Dispatchers.IO) {
                requireNotNull(context.contentResolver.openInputStream(uri)).bufferedReader(Charsets.UTF_8).use { reader ->
                    val buffer = CharArray(2048); val result = StringBuilder()
                    while (true) {
                        val count = reader.read(buffer)
                        if (count <= 0) break
                        require(result.length + count <= 65_536) { "Theme file too large" }
                        result.append(buffer, 0, count)
                    }
                    result.toString()
                }
            }
            CustomizationProfiles.importDocument(context, text)
        }
    }
    BetaCard(R.string.beta_profiles, R.string.beta_profiles_help) {
        OutlinedTextField(name, { name = it.take(32) }, label = { Text(stringResource(R.string.beta_preset_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { perform { CustomizationProfiles.save(context, name) } }, enabled = !busy && name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.beta_save_preset)) }
        for (index in 0 until saved.length()) {
            val item = saved.optJSONObject(index) ?: continue
            val label = item.optString("name")
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            Row {
                TextButton(onClick = { perform { CustomizationProfiles.apply(context, item.getJSONObject("settings")) } }, enabled = !busy) { Text(stringResource(R.string.beta_apply)) }
                TextButton(onClick = { perform { CustomizationProfiles.delete(context, label) } }, enabled = !busy) { Text(stringResource(R.string.beta_delete)) }
            }
        }
        OutlinedButton(onClick = { exportPicker.launch("pippo-theme-v1.json") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.beta_export)) }
        OutlinedButton(onClick = { importPicker.launch(arrayOf("application/json", "text/plain")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.beta_import)) }
        TextButton(onClick = { reset = true }, enabled = !busy) { Text(stringResource(R.string.beta_reset)) }
    }
    if (reset) AlertDialog(
        onDismissRequest = { reset = false },
        title = { Text(stringResource(R.string.beta_reset)) },
        text = { Text(stringResource(R.string.beta_reset_help)) },
        confirmButton = { TextButton(onClick = { reset = false; perform { CustomizationProfiles.reset(context) } }) { Text(stringResource(R.string.beta_reset)) } },
        dismissButton = { TextButton(onClick = { reset = false }) { Text(stringResource(R.string.cancel)) } },
    )
}


@Composable
internal fun BetaMiniPlayerSection() {
    BetaCard(R.string.mini_customization, R.string.mini_customization_help) {
        PreferenceChoice(R.string.mini_style, MiniStyleKey, MiniStyle.CLASSIC)
        PreferenceChoice(R.string.mini_progress, MiniProgressKey, MiniProgress.NONE)
        PreferenceChoice(R.string.mini_corners, MiniCornersKey, MiniCorners.SOFT)
        PreferenceChoice(R.string.mini_spacing, MiniSpacingKey, UiSpacing.COMFORTABLE)
        PreferenceToggle(R.string.mini_show_previous, MiniPreviousKey, false)
        PreferenceToggle(R.string.mini_show_next, MiniNextKey, true)
        PreferenceToggle(R.string.mini_show_favorite, MiniFavoriteKey, false)
        PreferenceToggle(R.string.mini_show_queue, MiniQueueKey, false)
        PreferenceToggle(R.string.mini_show_elapsed, MiniElapsedKey, false)
        PreferenceToggle(R.string.mini_show_quality, MiniQualityKey, false)
        PreferenceToggle(R.string.mini_show_status, MiniStatusKey, false)
        PreferenceToggle(R.string.mini_marquee, MiniMarqueeKey, false)
        PreferenceToggle(R.string.beta_mini_outline, MiniPlayerOutlineKey, true)
        PreferenceToggle(R.string.beta_mini_black, PureBlackMiniPlayerKey, false)
    }
}
