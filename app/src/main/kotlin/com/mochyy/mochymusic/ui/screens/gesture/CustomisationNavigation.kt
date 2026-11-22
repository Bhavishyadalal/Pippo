package com.mochyy.mochymusic.ui.screens.gesture

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.PureBlackKey
import com.mochyy.mochymusic.ui.component.AppNavigationBar
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.component.BottomNavigationMotion
import com.mochyy.mochymusic.ui.component.BottomNavigationMotionKey
import com.mochyy.mochymusic.ui.component.BottomNavigationStyle
import com.mochyy.mochymusic.ui.component.BottomNavigationStyleKey
import com.mochyy.mochymusic.ui.component.RemoteFocus
import com.mochyy.mochymusic.ui.component.ScrollPlayerEnabledKey
import com.mochyy.mochymusic.ui.component.ScrollPlayerMotion
import com.mochyy.mochymusic.ui.component.ScrollPlayerMotionKey
import com.mochyy.mochymusic.ui.component.applyNavigationMotion
import com.mochyy.mochymusic.ui.component.pressBounce
import com.mochyy.mochymusic.ui.screens.Screens
import com.mochyy.mochymusic.ui.screens.audiolab.LabColors
import com.mochyy.mochymusic.ui.utils.AppMotion
import com.mochyy.mochymusic.utils.rememberEnumPreference
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.delay

@Composable
internal fun BottomNavigationSection() {
    val (style, setStyle) = rememberEnumPreference(BottomNavigationStyleKey, BottomNavigationStyle.GLASS)
    val (openingMotion, setOpeningMotion) = rememberEnumPreference(BottomNavigationMotionKey, BottomNavigationMotion.SLIDE)
    val (scrollEnabled, setScrollEnabled) = rememberPreference(ScrollPlayerEnabledKey, defaultValue = true)
    val (scrollMotion, setScrollMotion) = rememberEnumPreference(ScrollPlayerMotionKey, ScrollPlayerMotion.FLOW)
    val (pureBlack) = rememberPreference(PureBlackKey, defaultValue = false)
    val animationsEnabled = AppMotion.durationScale.scaleFactor > 0f

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NavigationCard {
            Text(stringResource(R.string.nav_appearance_title), color = Color.White, style = CardLabelStyle)
            NavigationPreview(style, pureBlack)
            NavigationChoices(
                BottomNavigationStyle.entries, style, { it.label },
                swatch = { NavigationStyleSwatch(it, pureBlack) },
                onSelect = setStyle,
            )
        }

        NavigationCard {
            val title = stringResource(R.string.scroll_player_title)
            var replay by remember { mutableIntStateOf(0) }
            val canPreview = scrollEnabled && animationsEnabled
            var previewFold by remember(scrollMotion, canPreview) { mutableStateOf<Boolean?>(null) }
            val progress = rememberMotionPreview(scrollMotion, replay, canPreview, previewFold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(title, color = Color.White, style = CardLabelStyle)
                    NavigationCaption(stringResource(R.string.scroll_player_description))
                }
                AppSwitch(
                    checked = scrollEnabled,
                    onCheckedChange = setScrollEnabled,
                    modifier = Modifier.semantics { contentDescription = title },
                )
            }
            NavigationPreview(
                style, pureBlack, progress, scrollMotion = scrollMotion,
                onFoldSwipe = if (canPreview) ({ previewFold = it }) else null,
            )
            if (canPreview) NavigationCaption(stringResource(R.string.scroll_player_preview_swipe))
            NavigationChoices(ScrollPlayerMotion.entries, scrollMotion, { it.label }, enabled = scrollEnabled) {
                setScrollMotion(it)
                replay++
            }
            if (scrollEnabled && !animationsEnabled) {
                NavigationCaption(stringResource(R.string.scroll_player_motion_disabled))
            }
        }

        NavigationCard {
            var replay by remember { mutableIntStateOf(0) }
            val progress = rememberMotionPreview(openingMotion, replay, animationsEnabled)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(stringResource(R.string.nav_motion_title), color = Color.White, style = CardLabelStyle)
                NavigationCaption(stringResource(R.string.nav_motion_description))
            }
            NavigationPreview(style, pureBlack, progress, openingMotion = openingMotion)
            NavigationChoices(BottomNavigationMotion.entries, openingMotion, { it.label }) {
                setOpeningMotion(it)
                replay++
            }
            if (!animationsEnabled) {
                NavigationCaption(stringResource(R.string.nav_motion_preview_disabled))
            }
            MotionPreviewButton(enabled = animationsEnabled) { replay++ }
        }
    }
}

@Composable
internal fun NavigationCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(LabColors.Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun NavigationCaption(text: String) {
    Text(text, color = Color.White.copy(alpha = .68f), style = CaptionStyle.copy(fontSize = 12.sp, lineHeight = 16.sp))
}

@Composable
internal fun <T : Enum<T>> NavigationChoices(
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    enabled: Boolean = true,
    swatch: (@Composable (T) -> Unit)? = null,
    onSelect: (T) -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = options.indexOf(selected).coerceAtLeast(0))
    val haptics = LocalHapticFeedback.current
    LazyRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options, key = { it.name }) { option ->
            val active = option == selected
            val interaction = remember { MutableInteractionSource() }
            val tileShape = RoundedCornerShape(15.dp)
            val fill by animateColorAsState(
                if (active) Color.White.copy(alpha = if (enabled) .15f else .08f) else LabColors.Row,
                label = "navChoiceFill",
            )
            val outline by animateColorAsState(
                if (active) Color.White.copy(alpha = if (enabled) .58f else .22f) else Color.Transparent,
                label = "navChoiceOutline",
            )
            Column(
                modifier = Modifier
                    .pressBounce(interaction, pressedScale = .96f)
                    .clip(tileShape)
                    .background(fill)
                    .border(1.dp, outline, tileShape)
                    .selectable(
                        selected = active,
                        enabled = enabled,
                        role = Role.RadioButton,
                        interactionSource = interaction,
                        indication = RemoteFocus,
                        onClick = {
                            if (!active) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(option)
                        },
                    )
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterVertically),
            ) {
                swatch?.invoke(option)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(17.dp).border(1.dp, Color.White.copy(alpha = .32f), CircleShape)
                            .background(if (active) Color.White else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (active) Icon(painterResource(R.drawable.check), null, Modifier.size(11.dp), tint = Color.Black)
                    }
                    Text(
                        stringResource(label(option)),
                        color = Color.White.copy(alpha = if (enabled) 1f else .5f),
                        style = CardLabelStyle.copy(fontSize = 13.sp, lineHeight = 16.sp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationStyleSwatch(style: BottomNavigationStyle, pureBlack: Boolean) {
    val tileWidth = 104.dp
    val shape = when (style) {
        BottomNavigationStyle.GLASS, BottomNavigationStyle.CAPSULE -> CircleShape
        BottomNavigationStyle.DOCKED -> RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)
        BottomNavigationStyle.RIBBON -> RoundedCornerShape(5.dp)
    }
    val barWidth = when (style) {
        BottomNavigationStyle.GLASS -> 84.dp
        BottomNavigationStyle.CAPSULE -> 66.dp
        BottomNavigationStyle.DOCKED -> tileWidth
        BottomNavigationStyle.RIBBON -> 90.dp
    }
    Box(
        Modifier.size(tileWidth, 35.dp).clip(RoundedCornerShape(9.dp))
            .background(if (pureBlack) Color.Black else Color(0xFF211F29)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier.padding(bottom = if (style == BottomNavigationStyle.DOCKED) 0.dp else 5.dp)
                .size(barWidth, 18.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF575260), Color(0xFF33313D))))
                .border(1.dp, Color.White.copy(alpha = .18f), shape),
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(Screens.MainScreens.size) { index ->
                val accent = if (index == 0) .9f else .40f
                Box(Modifier.size(if (index == 0 && style != BottomNavigationStyle.RIBBON) 11.dp else 5.dp, 5.dp)
                    .background(Color.White.copy(alpha = accent), CircleShape))
            }
        }
    }
}

@Composable
private fun MotionPreviewButton(enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier.fillMaxWidth()
            .pressBounce(interaction, pressedScale = .97f)
            .clip(RoundedCornerShape(16.dp))
            .background(LabColors.Row)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interaction,
                indication = RemoteFocus,
                onClick = onClick,
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        val ink = Color.White.copy(alpha = if (enabled) 1f else .4f)
        Icon(painterResource(R.drawable.replay), null, Modifier.size(17.dp), tint = ink)
        Text(stringResource(R.string.nav_motion_preview), color = ink, style = CardLabelStyle.copy(fontSize = 13.sp))
    }
}

@Composable
private fun rememberMotionPreview(
    motion: Enum<*>,
    replay: Int,
    enabled: Boolean,
    swipeFold: Boolean? = null,
): State<Float> {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(motion, replay, enabled, swipeFold) {
        val scrollMotion = motion as? ScrollPlayerMotion
        if (enabled && swipeFold != null && scrollMotion != null) {
            progress.animateTo(if (swipeFold) 1f else 0f, scrollMotion.animation(entering = !swipeFold))
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        if (enabled && replay > 0) {
            progress.animateTo(1f, scrollMotion?.animation(entering = false) ?: tween(330))
            delay(180)
            progress.animateTo(0f, scrollMotion?.animation(entering = true) ?: tween(330))
        }
    }
    return progress.asState()
}

@Composable
private fun NavigationPreview(
    style: BottomNavigationStyle,
    pureBlack: Boolean,
    progress: State<Float>? = null,
    scrollMotion: ScrollPlayerMotion? = null,
    openingMotion: BottomNavigationMotion? = null,
    onFoldSwipe: ((Boolean) -> Unit)? = null,
) {
    val currentFoldSwipe = rememberUpdatedState(onFoldSwipe)
    val rowHeight = style.barHeight + 16.dp
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        Modifier.fillMaxWidth()
            .height(if (scrollMotion != null) rowHeight + 62.dp else rowHeight + 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    if (pureBlack) listOf(LabColors.Row, Color.Black)
                    else listOf(Color(0xFF33313F), LabColors.Row),
                ),
            )
            .then(if (onFoldSwipe == null) Modifier else Modifier.pointerInput(Unit) {
                var distance = 0f
                detectVerticalDragGestures(
                    onDragStart = { distance = 0f },
                    onDragEnd = { if (kotlin.math.abs(distance) >= 12.dp.toPx()) currentFoldSwipe.value?.invoke(distance < 0f) },
                    onVerticalDrag = { _, delta -> distance += delta },
                )
            })
            .clearAndSetSemantics {},
    ) {
        if (scrollMotion != null && progress != null) {
            MiniPlayerPreview(
                Modifier.align(Alignment.BottomCenter)
                    .padding(start = 14.dp, end = 14.dp, bottom = rowHeight + 2.dp)
                    .graphicsLayer {
                        if (!scrollMotion.keepsTabsVisible) translationY = rowHeight.toPx() * progress.value
                    },
            )
        }
        AppNavigationBar(
            navigationItems = Screens.MainScreens,
            selectedTabRoute = Screens.Home.route,
            onItemClick = { _, _ -> },
            pureBlack = pureBlack,
            style = style,
            preview = true,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .height(rowHeight)
                .graphicsLayer {
                    val fraction = progress?.value ?: 0f
                    if (scrollMotion != null) {
                        scrollMotion.applyTo(this, fraction, size.height, rtl = isRtl)
                    } else if (openingMotion != null) {
                        applyNavigationMotion(openingMotion, fraction, size.height)
                    }
                },
        )
    }
}

@Composable
private fun MiniPlayerPreview(modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(45.dp)
            .clip(RoundedCornerShape(24.dp)).background(Color(0xFF282630))
            .border(1.dp, Color.White.copy(alpha = .15f), RoundedCornerShape(24.dp))
            .padding(horizontal = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(29.dp).clip(RoundedCornerShape(9.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFDEB0A6), Color(0xFF6F628F)))),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.fillMaxWidth(.49f).height(4.dp).background(Color.White.copy(alpha = .82f), CircleShape))
            Box(Modifier.fillMaxWidth(.3f).height(3.dp).background(Color.White.copy(alpha = .34f), CircleShape))
        }
        Icon(painterResource(R.drawable.play), null, Modifier.size(19.dp), tint = Color.White)
    }
}
