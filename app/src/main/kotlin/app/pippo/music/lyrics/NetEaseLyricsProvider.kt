// NetEaseLyricsProvider.kt

package app.pippo.music.lyrics

import android.content.Context
import com.music.directlyrics.NetEase
import app.pippo.music.constants.EnableNetEaseKey
import app.pippo.music.utils.dataStore
import app.pippo.music.utils.get

object NetEaseLyricsProvider : LyricsProvider {
    override val name = "NetEase"
    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableNetEaseKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = NetEase.getLyrics(title, artist, duration)
}
