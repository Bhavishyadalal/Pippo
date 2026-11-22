package com.mochyy.mochymusic.ui.screens.update

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.BuildConfig
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.PopupSheetState
import com.mochyy.mochymusic.ui.component.TopToaster
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
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AboutHero()
            Text(
                stringResource(R.string.pippo_about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE1D8DF),
                modifier = Modifier.padding(horizontal = 2.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                aboutLinks.forEach { link -> AboutLinkBadge(link) }
            }
            TextButton(onClick = { popup.showPippoLicense() }) {
                Text(stringResource(R.string.pippo_about_license), color = Color(0xFFE9DEE5))
            }
        }
    }
}

@Composable
private fun AboutHero() {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 154.dp).clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF261B25)),
    ) {
        AsyncImage(
            model = R.drawable.pippo_home_scene_07,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(0f to Color.Transparent, .45f to Color(0x2218101A), 1f to Color(0xF2181019)),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text("Pippo", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                stringResource(R.string.pippo_about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFEBD4CD),
            )
        }
    }
}

@Composable
private fun AboutLinkBadge(link: AboutLink) {
    val uriHandler = LocalUriHandler.current
    val openError = stringResource(R.string.pippo_about_link_error)
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 66.dp)
            .clip(shape)
            .background(Color(0xFF17151A))
            .border(1.dp, link.accent.copy(alpha = .23f), shape)
            .clickable(role = Role.Button) {
                runCatching { uriHandler.openUri(link.url) }.onFailure { TopToaster.error(openError) }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(link.accent.copy(alpha = .18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(link.icon), contentDescription = null, tint = link.accent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(link.label), style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(link.account, style = MaterialTheme.typography.bodySmall, color = link.accent, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(painterResource(R.drawable.pippo_open_link), contentDescription = null, tint = link.accent, modifier = Modifier.size(18.dp))
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
