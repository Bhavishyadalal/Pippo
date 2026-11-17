package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.UiCharacter
import com.mochyy.mochymusic.ui.brand.PippoCharacter
import com.mochyy.mochymusic.ui.brand.PippoPose
import com.mochyy.mochymusic.ui.customization.UiCustomizationState
import com.mochyy.mochymusic.ui.theme.PippoCorners

/** Static tonal surfaces: no blur, background animation or additional image assets. */
@Composable
internal fun CozySettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(PippoCorners.Group)
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().clip(shape)
        .background(Brush.verticalGradient(listOf(colors.surfaceContainerHigh, colors.surfaceContainer)))
        .border(1.dp, colors.onSurface.copy(alpha = .065f), shape)
        .padding(20.dp), content = content)
}

@Composable
internal fun SettingsIconTile(icon: Int, modifier: Modifier = Modifier) {
    val tone = when (icon) {
        R.drawable.equalizer, R.drawable.settings, R.drawable.speed -> MaterialTheme.colorScheme.secondary
        R.drawable.palette, R.drawable.backup, R.drawable.library_add -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Box(modifier.size(40.dp).clip(RoundedCornerShape(14.dp))
        .background(tone.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), null, tint = tone, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun SettingsSectionHeading(title: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 10.dp).weight(1f), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun SettingsValueBadge(value: String) {
    Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = .10f)).padding(horizontal = 10.dp, vertical = 5.dp))
}

@Composable
internal fun SettingsWelcomeCard(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    CozySettingsCard(modifier) {
        val textScale = LocalDensity.current.fontScale * UiCustomizationState.options.textScale
        BoxWithConstraints {
            val showCharacter = maxWidth >= 260.dp && textScale < 1.5f && UiCustomizationState.options.character != UiCharacter.HIDDEN
            Row(Modifier.fillMaxWidth().drawBehind {
                drawCircle(accent.copy(alpha = .045f), radius = 72.dp.toPx(), center = Offset(size.width - 20.dp.toPx(), size.height / 2))
                drawCircle(accent.copy(alpha = .045f), radius = 48.dp.toPx(), center = Offset(size.width - 20.dp.toPx(), size.height / 2))
            }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_cozy_eyebrow), style = MaterialTheme.typography.labelSmall, color = accent)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_cozy_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_cozy_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (showCharacter) {
                    Spacer(Modifier.width(12.dp))
                    PippoCharacter(PippoPose.Listening, Modifier.width(92.dp).height(116.dp))
                }
            }
        }
    }
}

@Composable
internal fun SettingsEmptyState(query: String) {
    CozySettingsCard {
        SettingsIconTile(R.drawable.settings)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.settings_nothing_matches, query), style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.settings_cozy_search_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
