package com.mochyy.mochymusic.ui.screens.update

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.BuildConfig
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.PopupSheetState
import com.mochyy.mochymusic.ui.component.RemoteFocus
import com.mochyy.mochymusic.ui.component.pressBounce
import com.mochyy.mochymusic.ui.component.TopToaster
import com.mochyy.mochymusic.ui.utils.AppMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AboutLink(
    @StringRes val label: Int,
    val account: String,
    val url: String,
    @DrawableRes val icon: Int,
    val accent: Color,
)

private val aboutLinks = listOf(
    AboutLink(
        R.string.pippo_about_instagram, "@bhavishya_dalal_",
        "https://www.instagram.com/bhavishya_dalal_/", R.drawable.pippo_instagram, Color(0xFFFFBBD9),
    ),
    AboutLink(
        R.string.pippo_about_github, "Bhavishyadalal",
        "https://github.com/Bhavishyadalal", R.drawable.github, Color(0xFFC9C3F6),
    ),
    AboutLink(
        R.string.pippo_about_source, "Bhavishyadalal/Pippo",
        "https://github.com/Bhavishyadalal/Pippo", R.drawable.pippo_source_code, Color(0xFFBCDCCA),
    ),
)

fun PopupSheetState.showAboutSheet(onOpenDevSpace: (Int) -> Unit = {}) {
    val popup = this
    show(fullBleed = true, surface = Color.Black, buttonBar = { SheetPrimaryButton(stringResource(R.string.done), popup::dismiss) }) {
        AboutContent(onLicense = { popup.showPippoLicense() })
    }
}

@Composable
private fun AboutContent(onLicense: () -> Unit) {
    val motion = AppMotion.durationScale.scaleFactor > 0f
    val reveal = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(motion) {
        if (motion) reveal.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        else reveal.snapTo(1f)
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AboutHero(Modifier.aboutEntrance(reveal, 0f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                aboutLinks.forEachIndexed { index, link ->
                    AboutLinkBadge(link, Modifier.aboutEntrance(reveal, 0.12f + index * 0.13f))
                }
            }
            TextButton(
                onClick = onLicense,
                modifier = Modifier.aboutEntrance(reveal, 0.5f),
            ) {
                Text(stringResource(R.string.pippo_about_license), color = Color(0xFFD0D0D0))
            }
        }
    }
}

private fun Modifier.aboutEntrance(reveal: Animatable<Float, *>, start: Float) = graphicsLayer {
    val progress = ((reveal.value - start) / (1f - start)).coerceIn(0f, 1f)
    alpha = progress
    translationY = 12.dp.toPx() * (1f - progress)
}

@Composable
private fun AboutHero(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier.fillMaxWidth().heightIn(min = 190.dp).clip(shape)
            .background(Color(0xFF211A1B))
            .border(1.dp, Color.White.copy(alpha = 0.13f), shape),
    ) {
        AsyncImage(
            model = R.drawable.pippo_home_scene_07,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(0f to Color(0x19130B0D), 0.35f to Color(0x40201314), 1f to Color.Black),
            ),
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    Modifier.size(37.dp).clip(RoundedCornerShape(11.dp)).background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_launcher_mark), null, Modifier.size(23.dp), tint = Color.White)
                }
                Text("Pippo", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                    color = Color.White, modifier = Modifier.semantics { heading() })
            }
            Text(
                stringResource(R.string.pippo_about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFF2EAE6),
            )
            Text(
                stringResource(R.string.pippo_about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFCFBDB6),
            )
        }
    }
}

@Composable
private fun AboutLinkBadge(link: AboutLink, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val openError = stringResource(R.string.pippo_about_link_error)
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(19.dp)
    Row(
        modifier.fillMaxWidth()
            .heightIn(min = 66.dp)
            .pressBounce(interaction, pressedScale = 0.974f)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF1C1C1C), Color(0xFF101010))))
            .border(1.dp, Color.White.copy(alpha = .09f), shape)
            .clickable(interactionSource = interaction, indication = RemoteFocus, role = Role.Button) {
                runCatching { uriHandler.openUri(link.url) }.onFailure { TopToaster.error(openError) }
            }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(link.accent.copy(alpha = .14f))
                .border(1.dp, link.accent.copy(alpha = .22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(link.icon), contentDescription = null, tint = link.accent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(link.label), style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(link.account, style = MaterialTheme.typography.bodySmall, color = link.accent)
        }
        Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.pippo_open_link), contentDescription = null, tint = Color(0xFFD6D6D6), modifier = Modifier.size(16.dp))
        }
    }
}

private fun PopupSheetState.showPippoLicense() {
    val popup = this
    show(buttonBar = { SheetPrimaryButton(stringResource(R.string.done), popup::dismiss) }) {
        val context = LocalContext.current
        val license by produceState(initialValue = "", context) {
            value = withContext(Dispatchers.IO) {
                context.assets.open("LICENSE.txt").bufferedReader().use { it.readText() }
            }
        }
        Text(
            text = license,
            modifier = Modifier.fillMaxWidth().height(420.dp)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
