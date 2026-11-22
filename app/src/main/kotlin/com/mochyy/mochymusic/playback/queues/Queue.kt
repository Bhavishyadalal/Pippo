// Queue.kt

package com.mochyy.mochymusic.playback.queues

import androidx.media3.common.MediaItem
import com.mochyy.mochymusic.extensions.metadata
import com.mochyy.mochymusic.models.MediaMetadata

interface Queue {
    val preloadItem: MediaMetadata?

    // the song this queue starts on, when that's known before it's read, so a pick can fetch it while the old song still plays
    val firstSongId: String? get() = preloadItem?.id
    val continuesAsRadio: Boolean get() = false

    suspend fun getInitialStatus(): Status

    fun hasNextPage(): Boolean

    suspend fun nextPage(): List<MediaItem>

    data class Status(
        val title: String?,
        val items: List<MediaItem>,
        val mediaItemIndex: Int,
        val position: Long = 0L,
    ) {
        fun filterExplicit(enabled: Boolean = true): Status =
            if (enabled) excluding { it.metadata?.explicit == true } else this

        fun filterVideoSongs(disableVideos: Boolean = false): Status =
            if (disableVideos) excluding { it.metadata?.isVideoSong == true } else this

        private fun excluding(remove: (MediaItem) -> Boolean): Status {
            val retained = items.withIndex().filterNot { remove(it.value) }
            if (retained.size == items.size) return this
            if (retained.isEmpty()) return copy(items = emptyList(), mediaItemIndex = 0, position = 0L)

            // Keep the original occurrence, including duplicates. If the chosen song was
            // filtered out, start at the next remaining song with a fresh position.
            val next = retained.indexOfFirst { it.index >= mediaItemIndex }
            val index = if (next < 0) retained.lastIndex else next
            return copy(
                items = retained.map { it.value },
                mediaItemIndex = index,
                position = if (retained[index].index == mediaItemIndex) position else 0L,
            )
        }
    }
}

fun List<MediaItem>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filterNot {
            it.metadata?.explicit == true
        }
    } else {
        this
    }

fun List<MediaItem>.filterVideoSongs(disableVideos: Boolean = false) =
    if (disableVideos) {
        filterNot { it.metadata?.isVideoSong == true }
    } else {
        this
    }
