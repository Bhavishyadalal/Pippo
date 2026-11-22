// YouTubeAlbumRadio.kt

package com.mochyy.mochymusic.playback.queues

import androidx.media3.common.MediaItem
import com.music.innertube.YouTube
import com.music.innertube.models.WatchEndpoint
import com.mochyy.mochymusic.extensions.toMediaItem
import com.mochyy.mochymusic.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeAlbumRadio(
    private var playlistId: String,
) : Queue {
    override val preloadItem: MediaMetadata? = null
    override val continuesAsRadio = true

    private val endpoint: WatchEndpoint
        get() = WatchEndpoint(
            playlistId = playlistId
        )

    private var albumSongIds = emptySet<String>()
    private var continuation: String? = null
    private var firstTimeLoaded: Boolean = false

    override suspend fun getInitialStatus(): Queue.Status = withContext(IO) {
        val albumSongs = YouTube.albumSongs(playlistId).getOrThrow()
        albumSongIds = albumSongs.map { it.id }.toSet()
        Queue.Status(
            title = albumSongs.firstOrNull()?.album?.name.orEmpty(),
            items = albumSongs.map { it.toMediaItem() },
            mediaItemIndex = 0
        )
    }

    override fun hasNextPage(): Boolean = !firstTimeLoaded || continuation != null

    override suspend fun nextPage(): List<MediaItem> = withContext(IO) {
        val previous = continuation
        val nextResult = YouTube.next(endpoint, previous).getOrThrow()
        continuation = nextResult.continuation?.takeUnless { it == previous }
        if (!firstTimeLoaded) {
            firstTimeLoaded = true
            nextResult.items.filterNot { it.id in albumSongIds }.map { it.toMediaItem() }
        } else {
            nextResult.items.map { it.toMediaItem() }
        }
    }
}
