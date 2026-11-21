package com.mochyy.mochymusic.ui.screens.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.db.entities.Song
import com.mochyy.mochymusic.playback.DownloadUtil
import com.mochyy.mochymusic.utils.OfflineMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PlaylistRepair(songs: List<Song>, downloadUtil: DownloadUtil) {
    val downloads by downloadUtil.downloads.collectAsState()
    val losslessFailures by downloadUtil.losslessFailures.collectAsState()
    val missing by produceState(emptyList<Song>(), songs, downloads) {
        val known = downloads
        value = withContext(Dispatchers.IO) {
            val eligible = songs.filterNot { it.song.isLocal }.distinctBy { it.id }
            if (eligible.none { known.containsKey(it.id) || it.song.dateDownload != null }) {
                emptyList()
            } else {
                eligible.filter { downloadUtil.needsPlaylistRepair(known[it.id]) }
            }
        }
    }
    var open by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(false) }
    var retrying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    if (missing.isNotEmpty()) {
        TextButton(
            onClick = {
                error = false
                open = true
            },
            modifier = Modifier.padding(horizontal = 24.dp),
        ) { Text(pluralStringResource(R.plurals.pippo_repair_count, missing.size, missing.size)) }
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(stringResource(R.string.pippo_repair_title)) },
        text = {
            Column {
                Text(stringResource(R.string.pippo_repair_description))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(missing, key = { it.id }) { song ->
                        Column {
                            Text(song.song.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                            val reason = when {
                                song.id in losslessFailures -> R.string.pippo_repair_lossless_unavailable
                                downloads[song.id]?.state == Download.STATE_FAILED -> R.string.pippo_repair_failed
                                else -> R.string.pippo_repair_missing
                            }
                            Text(stringResource(reason), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (error) {
                    Text(stringResource(if (OfflineMode.isOn) R.string.pippo_repair_offline else R.string.pippo_repair_retry_error))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = missing.isNotEmpty() && !retrying, onClick = {
                if (OfflineMode.isOn) error = true
                else {
                    retrying = true
                    scope.launch {
                        try {
                            val submitted = downloadUtil.repairPlaylist(missing)
                            if (submitted == 0 && OfflineMode.isOn) error = true
                            else open = false
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            error = true
                        } finally {
                            retrying = false
                        }
                    }
                }
            }) { Text(stringResource(R.string.pippo_repair_retry)) }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } },
    )
}
