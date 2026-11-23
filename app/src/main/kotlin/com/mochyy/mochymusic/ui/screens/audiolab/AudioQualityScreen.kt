// audio lab's quality page: streaming tiers per provider, download quality, and the orders songs play and download in

package com.mochyy.mochymusic.ui.screens.audiolab

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.mochyy.mochymusic.module.AccountState
import com.mochyy.mochymusic.module.ModuleFeature
import com.mochyy.mochymusic.module.applemusic.AppleMusicModule
import com.mochyy.mochymusic.module.monochrome.MonochromeModule
import com.mochyy.mochymusic.modules.SourceModules
import com.mochyy.mochymusic.viewmodels.ModulesViewModel
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.AudioQuality
import com.mochyy.mochymusic.constants.AudioQualityKey
import com.mochyy.mochymusic.constants.DownloadQuality
import com.mochyy.mochymusic.constants.DownloadQualityKey
import com.mochyy.mochymusic.eq.audio.AudioTap
import com.mochyy.mochymusic.ui.theme.InterFontFamily
import com.mochyy.mochymusic.utils.rememberEnumPreference
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

private class QualityOption(
    val quality: AudioQuality,
    @StringRes val title: Int,
    @StringRes val description: Int,
)

private val YouTubeOptions = listOf(
    QualityOption(AudioQuality.AUTO, R.string.aq_quality_auto, R.string.aq_desc_auto),
    QualityOption(AudioQuality.HIGH, R.string.aq_quality_high, R.string.aq_desc_high),
    QualityOption(AudioQuality.MEDIUM, R.string.aq_quality_medium, R.string.aq_desc_medium),
    QualityOption(AudioQuality.LOW, R.string.aq_quality_low, R.string.aq_desc_low),
)

@Composable
fun AudioQualityScreen(navController: NavController, modules: ModulesViewModel = hiltViewModel()) {
    val (stored, onQuality) = rememberEnumPreference(AudioQualityKey, defaultValue = AudioQuality.AUTO)
    val (downloadQuality, onDownloadQuality) = rememberEnumPreference(
        DownloadQualityKey,
        defaultValue = DownloadQuality.HIGH,
    )
    val losslessDownloads = downloadQuality == DownloadQuality.LOSSLESS
    // the tiers Monochrome used to serve from here belong to its module now
    val quality = if (stored.usesMonochrome) AudioQuality.HIGH else stored
    val entries by modules.entries.collectAsState()
    val order by modules.order.collectAsState()
    val downloadOrder by modules.downloadOrder.collectAsState()
    val youTubeOn by modules.youTubeEnabled.collectAsState()
    val statusSignIn = stringResource(R.string.aq_status_sign_in)
    val statusSignInAgain = stringResource(R.string.aq_status_sign_in_again)
    val statusOn = stringResource(R.string.aq_status_on)
    val statusOffTap = stringResource(R.string.aq_status_off_tap)
    val statusNotInstalled = stringResource(R.string.aq_status_not_installed)
    val moduleStatus = { entry: com.mochyy.mochymusic.modules.ModuleEntry ->
        when {
            entry.account is AccountState.SignedOut -> statusSignIn
            entry.account is AccountState.Expired -> statusSignInAgain
            entry.enabled -> statusOn
            else -> statusOffTap
        }
    }
    val usable = { id: String ->
        if (id == SourceModules.YouTubeId) youTubeOn
        else entries.any { it.id == id && it.enabled && (it.module.signIn == null || it.account is AccountState.SignedIn) }
    }
    // a module that's switched off isn't part of this page at all, only youtube and the modules that are on show up
    val shownModules = entries.filter { it.enabled && it.adds(ModuleFeature.AudioSource) }
    val shownOrder = order.filter { id -> id == SourceModules.YouTubeId || shownModules.any { it.id == id } }
    val shownDownloadOrder = downloadOrder.filter { id -> id == SourceModules.YouTubeId || shownModules.any { it.id == id } }
    val subtitle = when {
        shownModules.any { usable(it.id) } -> stringResource(R.string.aq_automatic_provider)
        youTubeOn -> {
            val option = YouTubeOptions.first { it.quality == quality }
            stringResource(R.string.aq_youtube_tier, stringResource(option.title))
        }
        else -> stringResource(R.string.aq_turn_provider_on)
    }

    AudioLabPage(
        title = LabTitle(stringResource(R.string.aq_title_first), stringResource(R.string.aq_title_second), firstSize = 40.sp, secondSize = 64.sp),
        onBack = { navController.navigateUp() },
        subtitle = subtitle,
    ) {
        LabSheetTitle(stringResource(R.string.aq_section_streaming))
        LabGroup {
            ProviderCaption(
                logo = R.drawable.provider_youtube_music,
                name = "YouTube Music",
                status = when {
                    youTubeOn -> stringResource(R.string.aq_youtube_status_on)
                    order.none(usable) -> stringResource(R.string.aq_youtube_status_off_none)
                    else -> stringResource(R.string.aq_youtube_status_off)
                },
                on = youTubeOn,
            )
            YouTubeOptions.forEach { option ->
                QualityRow(stringResource(option.title), stringResource(option.description), selected = quality == option.quality, enabled = youTubeOn) { onQuality(option.quality) }
            }

            // each module's own qualities, under its name
            shownModules.forEach { entry ->
                val id = entry.module.info.id
                val active = usable(id)
                Spacer(Modifier.height(6.dp))
                ProviderCaption(logo = providerLogo(id), name = entry.module.info.name, status = moduleStatus(entry), on = active)
                val qualityChoice = entry.module.choices.firstOrNull { it.id == "quality" }
                if (qualityChoice != null) {
                    val picked = entry.picks[qualityChoice.id]?.takeIf { p -> qualityChoice.options.any { it.id == p } }
                        ?: qualityChoice.options.firstOrNull()?.id
                    qualityChoice.options.forEach { option ->
                        QualityRow(option.label, option.description, selected = picked == option.id, enabled = active) {
                            modules.setPick(id, qualityChoice.id, option.id)
                        }
                    }
                } else if (id == MonochromeModule.Id) {
                    QualityRow(stringResource(R.string.aq_quality_lossless), stringResource(R.string.aq_desc_monochrome_lossless), selected = true, enabled = active) {}
                }
                LabTextAction(stringResource(R.string.aq_module_settings, entry.module.info.name), R.drawable.navigate_next) {
                    navController.navigate(com.mochyy.mochymusic.ui.screens.settings.modules.moduleRoute(id))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        LabSheetTitle(stringResource(R.string.aq_section_download))
        DownloadQualityGroup(downloadQuality, onDownloadQuality)
        Footnote(stringResource(R.string.download_settings_quality_note))
        LabTextAction(stringResource(R.string.download_settings_title), R.drawable.download) {
            navController.navigate(com.mochyy.mochymusic.ui.screens.settings.storage.DownloadSettingsRoute)
        }

        Spacer(Modifier.height(24.dp))
        LabSheetTitle(stringResource(R.string.aq_section_provider))
        ProviderOrder(
            order = shownOrder,
            name = modules::nameOf,
            logo = ::providerLogo,
            usable = usable,
            status = { id ->
                if (id == SourceModules.YouTubeId) {
                    if (youTubeOn) statusOn else statusOffTap
                } else {
                    entries.firstOrNull { it.module.info.id == id }?.let(moduleStatus) ?: statusNotInstalled
                }
            },
            onTap = { id ->
                // youtube switches right here; a module's switch is on its own page, switching it off here would make it vanish under the finger
                if (id == SourceModules.YouTubeId) {
                    modules.setEnabled(id, !youTubeOn)
                } else {
                    navController.navigate(com.mochyy.mochymusic.ui.screens.settings.modules.moduleRoute(id))
                }
            },
            onReorder = modules::setVisibleOrder,
        )
        Footnote(stringResource(R.string.quality_provider_automatic_footnote))

        Spacer(Modifier.height(24.dp))
        LabSheetTitle(stringResource(R.string.quality_download_order))
        val youTubeForDownloads = stringResource(
            if (losslessDownloads) R.string.quality_download_order_youtube_skipped else R.string.quality_download_order_youtube,
        )
        ProviderOrder(
            order = shownDownloadOrder,
            name = modules::nameOf,
            logo = ::providerLogo,
            usable = { id -> if (id == SourceModules.YouTubeId) !losslessDownloads else usable(id) },
            status = { id ->
                if (id == SourceModules.YouTubeId) {
                    youTubeForDownloads
                } else {
                    entries.firstOrNull { it.module.info.id == id }?.let(moduleStatus) ?: statusNotInstalled
                }
            },
            // youtube is always there for downloads, so only a module's row has somewhere to go
            onTap = { id ->
                if (id != SourceModules.YouTubeId) navController.navigate(com.mochyy.mochymusic.ui.screens.settings.modules.moduleRoute(id))
            },
            onReorder = modules::setVisibleDownloadOrder,
        )
        if (!losslessDownloads) Footnote(stringResource(R.string.quality_download_order_footnote))
    }
}

@Composable
internal fun DownloadQualityGroup(
    quality: DownloadQuality,
    onQuality: (DownloadQuality) -> Unit,
) {
    LabGroup {
        DownloadQuality.entries.forEach { option ->
            QualityRow(
                stringResource(option.downloadTitleRes),
                stringResource(option.downloadDetailRes),
                selected = quality == option,
                enabled = true,
            ) {
                onQuality(option)
            }
        }
    }
}

internal val DownloadQuality.downloadTitleRes: Int
    get() = when (this) {
        DownloadQuality.LOSSLESS -> R.string.aq_quality_lossless
        DownloadQuality.HIGH -> R.string.aq_quality_high
        DownloadQuality.MEDIUM -> R.string.aq_quality_medium
        DownloadQuality.LOW -> R.string.aq_quality_low
    }

internal val DownloadQuality.downloadDetailRes: Int
    get() = when (this) {
        DownloadQuality.LOSSLESS -> R.string.aq_dl_desc_lossless
        DownloadQuality.HIGH -> R.string.download_settings_high_detail
        DownloadQuality.MEDIUM -> R.string.download_settings_medium_detail
        DownloadQuality.LOW -> R.string.download_settings_low_detail
    }

@Composable
private fun Footnote(text: String) {
    Text(
        text,
        fontFamily = InterFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = LabColors.Secondary,
        modifier = Modifier.padding(top = 10.dp, start = 6.dp, end = 6.dp),
    )
}

internal fun providerLogo(id: String): Int? = when (id) {
    AppleMusicModule.Id -> R.drawable.provider_apple_music
    MonochromeModule.Id -> R.drawable.provider_monochrome
    SourceModules.YouTubeId -> R.drawable.provider_youtube_music
    else -> null
}

/** a provider's name over its qualities in the streaming group, dimmed while it's off */
@Composable
private fun ProviderCaption(logo: Int?, name: String, status: String, on: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.graphicsLayer { alpha = if (on) 1f else 0.45f }) { ProviderLogo(logo, size = 24, name = name) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            Text(status, fontFamily = InterFontFamily, fontSize = 11.sp, lineHeight = 14.sp, color = LabColors.Secondary)
        }
    }
}

// a provider with no logo of its own (jiosaavn, a module from a link) shows its first letter instead of a blank tile
@Composable
internal fun ProviderLogo(logo: Int?, size: Int, name: String? = null) {
    Box(
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.26f).dp)).background(Color(0xFFD9D9D9)),
        contentAlignment = Alignment.Center,
    ) {
        if (logo != null) {
            androidx.compose.foundation.Image(
                painterResource(logo), null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.size(size.dp),
            )
        } else if (name != null) {
            Text(name.take(1).uppercase(), fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = (size * 0.44f).sp, color = Color.Black.copy(alpha = 0.7f))
        }
    }
}

@Composable
internal fun QualityRow(title: String, description: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Color(0xFF2A2A2E) else Color(0xFF111113), tween(200), label = "row")
    val alpha by animateFloatAsState(if (enabled) 1f else 0.35f, tween(250), label = "enabled")
    val check by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "check")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.22f) else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            Text(description, fontFamily = InterFontFamily, fontSize = 11.sp, lineHeight = 14.sp, color = LabColors.Secondary)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier.size(22.dp).graphicsLayer { scaleX = check; scaleY = check; this.alpha = check },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.check), null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}
