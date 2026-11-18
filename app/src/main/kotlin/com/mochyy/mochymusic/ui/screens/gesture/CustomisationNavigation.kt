package com.mochyy.mochymusic.ui.screens.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.PureBlackKey
import com.mochyy.mochymusic.ui.component.AppNavigationBar
import com.mochyy.mochymusic.ui.component.BottomNavigationMotion
import com.mochyy.mochymusic.ui.component.BottomNavigationMotionKey
import com.mochyy.mochymusic.ui.component.BottomNavigationStyle
import com.mochyy.mochymusic.ui.component.BottomNavigationStyleKey
import com.mochyy.mochymusic.ui.component.applyNavigationMotion
import com.mochyy.mochymusic.ui.screens.Screens
import com.mochyy.mochymusic.ui.screens.audiolab.LabColors
import com.mochyy.mochymusic.ui.utils.AppMotion
import com.mochyy.mochymusic.utils.rememberEnumPreference
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BottomNavigationSection() {
    val (style, setStyle) = rememberEnumPreference(BottomNavigationStyleKey, BottomNavigationStyle.GLASS)
    val (motion, setMotion) = rememberEnumPreference(BottomNavigationMotionKey, BottomNavigationMotion.SLIDE)
    val (pureBlack) = rememberPreference(PureBlackKey, defaultValue = false)
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var previewPlaying by remember { mutableStateOf(false) }
    val canAnimate = AppMotion.durationScale.scaleFactor > 0f

    Column(
        Modifier.fillMaxWidth().clip(CardShape).background(LabColors.Card).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(stringResource(R.string.nav_appearance_title), color = Color.White, style = CardLabelStyle)
        Box(
            Modifier.fillMaxWidth().height(86.dp).clip(CardShape)
                .background(if (pureBlack) Color.Black else LabColors.Row),
        ) {
            AppNavigationBar(
                navigationItems = Screens.MainScreens,
                selectedTabRoute = Screens.Home.route,
                onItemClick = { _, _ -> },
                pureBlack = pureBlack,
                style = style,
                preview = true,
                modifier = Modifier.align(Alignment.BottomCenter).clipToBounds()
                    .graphicsLayer { applyNavigationMotion(motion, progress.value, size.height) },
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BottomNavigationStyle.entries.forEach { option ->
                FilterChip(
                    selected = option == style,
                    onClick = { setStyle(option) },
                    label = { Text(stringResource(option.label)) },
                )
            }
        }

        Text(stringResource(R.string.nav_motion_title), color = Color.White, style = CardLabelStyle)
        Text(stringResource(R.string.nav_motion_description), color = Color.White.copy(alpha = .65f))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BottomNavigationMotion.entries.forEach { option ->
                FilterChip(
                    selected = option == motion,
                    onClick = { setMotion(option) },
                    label = { Text(stringResource(option.label)) },
                )
            }
        }
        TextButton(
            enabled = canAnimate && !previewPlaying,
            onClick = {
                previewPlaying = true
                scope.launch {
                    try {
                        progress.animateTo(1f, tween(330))
                        delay(140)
                        progress.animateTo(0f, tween(330))
                    } finally {
                        previewPlaying = false
                    }
                }
            },
        ) { Text(stringResource(R.string.nav_motion_preview)) }
    }
}
