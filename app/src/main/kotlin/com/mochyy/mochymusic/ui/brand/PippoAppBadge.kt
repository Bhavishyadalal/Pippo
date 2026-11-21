package com.mochyy.mochymusic.ui.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.utils.AppIcons

/** Same circular crop and foreground proportions as the portrait used by the launch flight. */
@Composable
fun PippoAppBadge(modifier: Modifier = Modifier) {
    val icon = AppIcons.rememberCurrent()
    val background = icon.background?.let { colorResource(it) } ?: Color.Transparent
    Box(modifier.clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = icon.foreground,
            contentDescription = "Pippo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(.96f),
        )
    }
}
