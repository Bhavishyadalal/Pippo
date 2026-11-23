// ForceProviderSheet.kt

package com.mochyy.mochymusic.ui.player.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.AudioQuality
import com.mochyy.mochymusic.constants.AudioQualityKey
import com.mochyy.mochymusic.module.ModuleFeature
import com.mochyy.mochymusic.module.StreamOption
import com.mochyy.mochymusic.modules.SourceModules
import com.mochyy.mochymusic.playback.ProviderSwitch
import com.mochyy.mochymusic.playback.ProviderSwitchState
import com.mochyy.mochymusic.ui.player.menu.LocalMenuSheetClose
import com.mochyy.mochymusic.ui.player.menu.MenuRowSurface
import com.mochyy.mochymusic.ui.player.menu.MenuSheetSurface
import com.mochyy.mochymusic.utils.rememberPreference
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.component.RemoteFocus
import com.mochyy.mochymusic.ui.component.pressBounce
import com.mochyy.mochymusic.ui.theme.PippoColors
import com.mochyy.mochymusic.ui.theme.PippoCorners
import com.mochyy.mochymusic.ui.theme.PippoMotion
import com.mochyy.mochymusic.ui.utils.silverSweep

private val YouTubeQualities = listOf(AudioQuality.AUTO, AudioQuality.HIGH, AudioQuality.MEDIUM, AudioQuality.LOW)

private fun qualityLabelRes(quality: AudioQuality): Int = when (quality) {
    AudioQuality.HIGH -> R.string.aq_quality_high
    AudioQuality.MEDIUM -> R.string.aq_quality_medium
    AudioQuality.LOW -> R.string.aq_quality_low
    else -> R.string.aq_quality_auto
}

/** "force change": plays this song from another of the providers that are switched on, at the quality picked */
@Composable
fun ForceProviderSheet(currentProviderId: String?, currentOption: String? = null, onDismiss: () -> Unit) {
    val service = LocalPlayerConnection.current?.service ?: return
    val modules = service.sourceModules
    val order by modules.order.collectAsState()
    val youTubeOn by modules.youTubeEnabled.collectAsState()
    val entries by modules.entries.collectAsState()
    val state by service.providerSwitch.state.collectAsState()
    val attempts by service.providerSwitch.attempts.collectAsState()
    val (youTubeQuality) = rememberPreference(AudioQualityKey, AudioQuality.AUTO.name)
    val audioEntries = entries.filter { ModuleFeature.AudioSource in it.module.adds }
        .associateBy { it.id }
    val providers = order.filter { it == SourceModules.YouTubeId || it in audioEntries }

    LaunchedEffect(Unit) { modules.refresh() }
    DisposableEffect(Unit) { onDispose { service.providerSwitch.acknowledge() } }

    MenuSheetSurface(
        onDismiss = onDismiss,
        fullDetent = 0.74f,
        wrapHeight = true,
        dragFromContent = false,
        showDragHandle = false,
        swipeDismissDistance = 56.dp,
    ) { dragHandle ->
        ProviderSheetHeader(dragHandle)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()),
        ) {
            InfoColumn(modifier = Modifier.widthIn(max = 600.dp)) {
                Text(
                    text = stringResource(R.string.pc_force_switch_hint),
                    color = PippoColors.SecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(16.dp))
                SwitchStatus(state, nameOf = modules::nameOf, onCancel = service.providerSwitch::cancel)
                providers.forEach { id ->
                    val module = modules.module(id)
                    val enabled = if (id == SourceModules.YouTubeId) youTubeOn else audioEntries[id]?.enabled == true
                    val available = enabled && modules.isUsable(id)
                    val choice = module?.let(modules::qualityChoice)
                    val picked = choice?.let { c -> modules.picks(id)[c.id] ?: c.options.firstOrNull()?.id }
                    key(id) {
                        ProviderChoiceRow(
                            providerId = id,
                            name = modules.nameOf(id),
                            isCurrent = id == currentProviderId,
                            enabled = enabled,
                            available = available,
                            attempt = attempts[id],
                            onEnabledChange = { service.setPlaybackProviderEnabled(id, it) },
                            options = when {
                                choice != null -> choice.options
                                id == SourceModules.YouTubeId -> YouTubeQualities.map {
                                    StreamOption(it.name, stringResource(qualityLabelRes(it)), "")
                                }
                                else -> emptyList()
                            },
                            selectedOption = currentOption?.takeIf { id == currentProviderId }
                                ?: if (id == SourceModules.YouTubeId) youTubeQuality else picked,
                            onPick = { option ->
                                val requested = option?.let { (choice?.id ?: SourceModules.QualityChoice) to it }
                                service.providerSwitch.force(id, requested)
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderSheetHeader(dragHandle: Modifier) {
    val close = LocalMenuSheetClose.current
    val title = stringResource(R.string.pc_force_title)
    Column(
        modifier = Modifier.fillMaxWidth().then(dragHandle),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.width(36.dp).height(4.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.48f)),
            )
        }
        Row(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                .padding(start = InfoSidePadding, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f).silverSweep(title),
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = close) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.dismiss),
                    tint = PippoColors.SecondaryText,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SwitchStatus(state: ProviderSwitchState, nameOf: (String) -> String, onCancel: () -> Unit) {
    val shape = RoundedCornerShape(PippoCorners.Card)
    val accent = when (state) {
        is ProviderSwitchState.Failed -> PippoColors.Error
        is ProviderSwitchState.Done -> PippoColors.Sage
        else -> Color(0xFFCFD4DB)
    }
    AnimatedVisibility(
        visible = state !is ProviderSwitchState.Idle,
        enter = fadeIn(tween(PippoMotion.StateChange)) + expandVertically(tween(PippoMotion.StateChange)),
        exit = fadeOut(tween(PippoMotion.Feedback)) + shrinkVertically(tween(PippoMotion.StateChange)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .clip(shape)
                .background(MenuRowSurface)
                .border(1.dp, accent.copy(alpha = 0.26f), shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            when (state) {
                is ProviderSwitchState.Working -> {
                    Text(stringResource(R.string.pc_force_fetching, nameOf(state.providerId)), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.pc_force_try_of, state.attempt, state.of), color = InfoMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    val progress by animateFloatAsState(state.attempt / state.of.toFloat(), label = "switchTries")
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))) {
                        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(accent))
                    }
                    TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                }
                is ProviderSwitchState.Starting -> {
                    Text(stringResource(R.string.pc_force_starting, nameOf(state.providerId)), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                }
                is ProviderSwitchState.Failed -> {
                    Text(stringResource(R.string.pc_force_unavailable), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        state.reason ?: if (state.opening) {
                            stringResource(R.string.pc_force_open_failed, nameOf(state.providerId))
                        } else {
                            stringResource(R.string.pc_force_lookup_failed, nameOf(state.providerId))
                        },
                        color = InfoMuted,
                        fontSize = 12.sp,
                    )
                }
                is ProviderSwitchState.Done -> {
                    Text(stringResource(R.string.pc_force_switched, nameOf(state.providerId)), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                ProviderSwitchState.Idle -> Unit
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderChoiceRow(
    providerId: String,
    name: String,
    isCurrent: Boolean,
    enabled: Boolean,
    available: Boolean,
    attempt: ProviderSwitch.Attempt?,
    onEnabledChange: (Boolean) -> Unit,
    options: List<StreamOption>,
    selectedOption: String?,
    /** null when the row itself was tapped, keeping the quality as it is */
    onPick: (String?) -> Unit,
) {
    val shape = RoundedCornerShape(PippoCorners.Card)
    val rowPress = remember { MutableInteractionSource() }
    val active = enabled && isCurrent
    val statusColor by animateColorAsState(
        if (active) PippoColors.Sage else PippoColors.SecondaryText,
        tween(PippoMotion.StateChange),
        label = "providerStatus",
    )
    val cardColor by animateColorAsState(
        if (active) lerp(MenuRowSurface, PippoColors.Sage, 0.055f) else MenuRowSurface,
        tween(PippoMotion.StateChange),
        label = "providerCard",
    )
    val outlineColor by animateColorAsState(
        if (active) PippoColors.Sage.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.075f),
        tween(PippoMotion.StateChange),
        label = "providerOutline",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardColor)
            .border(1.dp, outlineColor, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
                    .pressBounce(rowPress, pressedScale = 0.98f)
                    .clip(RoundedCornerShape(PippoCorners.Small))
                    .clickable(
                        enabled = available,
                        interactionSource = rowPress,
                        indication = RemoteFocus,
                        role = Role.Button,
                        onClick = { onPick(null) },
                    ),
            ) {
                ProviderLogo(providerId, name, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name, color = Color.White, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val status = when {
                        !enabled && isCurrent -> R.string.pc_provider_off_current
                        !enabled -> R.string.pc_provider_off
                        !available -> R.string.pc_provider_unavailable
                        isCurrent -> R.string.pc_force_playing_here
                        else -> R.string.pc_force_tap_here
                    }
                    Text(stringResource(status), color = statusColor, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            val switchLabel = stringResource(R.string.pc_provider_toggle, name)
            AppSwitch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.semantics { contentDescription = switchLabel },
            )
        }
        attempt?.let { result ->
            val quality = result.quality
            val failure = result.failure
            if (result.opened || quality != null || failure != null) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val streamResult = when {
                        quality != null -> stringResource(
                            if (result.opened) R.string.pippo_provider_opened_quality else R.string.pippo_provider_found_quality,
                            quality,
                        )
                        result.opened -> stringResource(R.string.pippo_provider_opened)
                        else -> null
                    }
                    if (streamResult != null) Text(
                        streamResult, color = InfoMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    if (failure != null) Text(
                        failure.reason ?: stringResource(
                            if (failure.opening) R.string.pc_force_open_failed else R.string.pc_force_lookup_failed,
                            name,
                        ),
                        color = InfoMuted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (available && options.size > 1) {
            Spacer(Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                options.forEach { option ->
                    QualityChip(
                        label = option.label,
                        selected = option.id == selectedOption,
                        active = active,
                        onClick = { onPick(option.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityChip(label: String, selected: Boolean, active: Boolean, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(9.dp)
    val fill by animateColorAsState(
        if (selected) Color.White.copy(alpha = if (active) 0.17f else 0.09f) else Color.Transparent,
        tween(PippoMotion.Feedback),
        label = "qualityFill",
    )
    val outline by animateColorAsState(
        if (selected) Color.White.copy(alpha = if (active) 0.42f else 0.20f) else Color.Transparent,
        tween(PippoMotion.Feedback),
        label = "qualityOutline",
    )
    val textColor by animateColorAsState(
        if (selected) Color.White else PippoColors.SecondaryText,
        tween(PippoMotion.Feedback),
        label = "qualityText",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .pressBounce(press, pressedScale = 0.95f)
            .clip(shape)
            .selectable(selected, interactionSource = press, indication = RemoteFocus, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier
                .background(fill, shape)
                .border(1.dp, outline, shape)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            color = textColor,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
