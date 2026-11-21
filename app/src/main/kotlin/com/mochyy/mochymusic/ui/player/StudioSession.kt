package com.mochyy.mochymusic.ui.player

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R

internal const val StudioSessionArtHeight = 0.86f
internal val StudioSessionTopClearance = 38.dp
internal val StudioSessionVolumeMinHeight = 640.dp

@Composable
internal fun StudioSessionHeader(modifier: Modifier = Modifier) {
    val connection = LocalPlayerConnection.current ?: return
    val queueTitle by connection.queueTitle.collectAsState()
    val title = queueTitle?.trim()?.takeIf { it.isNotEmpty() }
    Text(
        text = if (title == null) stringResource(R.string.pc_hero_now_playing) else stringResource(R.string.pc_session_queue, title),
        color = Color.White.copy(alpha = 0.87f),
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(horizontal = 32.dp),
    )
}
