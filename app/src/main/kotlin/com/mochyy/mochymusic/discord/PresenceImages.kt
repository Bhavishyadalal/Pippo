// PresenceImages.kt

package com.mochyy.mochymusic.discord

import com.mochyy.mochymusic.ui.utils.resize

/** A status picture before Discord has it: what to show, not yet where it lives. */
/** How the rotating artwork turns: one full turn every [secondsPerTurn], drawn at [fps]. */
data class SpinStyle(val secondsPerTurn: Int, val fps: Int)

sealed interface PresencePicture {
    data class Web(val url: String) : PresencePicture

    /** the play/pause badge, with its ring [step] of [PresenceImages.StatusSteps] round */
    data class Status(val step: Int, val playing: Boolean) : PresencePicture

    /** the rotating disc made from [sourceUrl], turning as [style] says */
    data class Spinning(val sourceUrl: String, val style: SpinStyle) : PresencePicture

    // a picture on the phone (a local song's cover, a picked one) that goes to a free host before discord can show it
    data class Local(val sourceUrl: String) : PresencePicture

    data object Logo : PresencePicture
}

/** Selects user artwork without loading upstream branding assets. */
object PresenceImages {
    /** the ring moves in 5% steps: finer than that doesn't show on a 40px badge */
    const val StatusSteps = 20

    const val LogoUrl = ""

    fun statusUrl(status: PresencePicture.Status): String {
        return ""
}

    /** What [source] shows for [track]; artwork that's missing falls back to the next best thing. */
    fun pick(source: PresenceImage, track: NowPlaying, nowMs: Long, spin: SpinStyle, custom: String = ""): PresencePicture? {
        // YouTube hands out small thumbnails; Discord shows the big picture at up to 240px
        val artwork = track.artworkUrl?.takeIf { it.isNotBlank() }?.let(::sized)
        val artistArtwork = track.artistArtworkUrl?.takeIf { it.isNotBlank() }?.let(::sized)
        return when (source) {
            PresenceImage.OFF -> null
            PresenceImage.ARTWORK -> still(artwork)
            PresenceImage.ARTIST -> still(artistArtwork ?: artwork)
            PresenceImage.APP_LOGO -> null
            PresenceImage.PLAYBACK_STATUS -> null
            PresenceImage.SPINNING_ARTWORK -> artwork?.let { PresencePicture.Spinning(it, spin) }
            PresenceImage.SPINNING_ARTIST ->
                (artistArtwork ?: artwork)?.let { PresencePicture.Spinning(it, spin) }
            PresenceImage.CUSTOM -> still(custom.takeIf { it.isNotBlank() })
        }
    }

    // a web picture as discord fetches it, anything on the phone as one to upload first
    private fun still(url: String?): PresencePicture? = when {
        url == null -> null
        isWebImage(url) -> PresencePicture.Web(url)
        else -> PresencePicture.Local(url)
    }

    private fun sized(url: String): String = if (isWebImage(url)) url.resize(512, 512) else url

    /** a local file or content:// cover can't be fetched by Discord */
    fun isWebImage(url: String) = url.startsWith("https://") || url.startsWith("http://")
}
