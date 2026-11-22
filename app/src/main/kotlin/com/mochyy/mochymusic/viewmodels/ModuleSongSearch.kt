package com.mochyy.mochymusic.viewmodels

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun searchModuleTestSongs(query: String): List<SongItem> = withContext(Dispatchers.IO) {
    try {
        YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
            .getOrThrow().items.filterIsInstance<SongItem>().take(8)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptyList()
    }
}
