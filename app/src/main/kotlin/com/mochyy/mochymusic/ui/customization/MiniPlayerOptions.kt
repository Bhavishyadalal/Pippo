package com.mochyy.mochymusic.ui.customization

import androidx.datastore.preferences.core.Preferences
import com.mochyy.mochymusic.constants.*

data class MiniPlayerOptions(
    val style: MiniStyle = MiniStyle.CLASSIC,
    val progress: MiniProgress = MiniProgress.NONE,
    val corners: MiniCorners = MiniCorners.SOFT,
    val spacing: UiSpacing = UiSpacing.COMFORTABLE,
    val previous: Boolean = false,
    val next: Boolean = true,
    val favorite: Boolean = false,
    val queue: Boolean = false,
    val elapsed: Boolean = false,
    val quality: Boolean = false,
    val marquee: Boolean = false,
    val status: Boolean = false,
) {
    val customLayout: Boolean get() = this != MiniPlayerOptions()
    val surfaceRadius: Int get() = when (style) {
        MiniStyle.FLOATING_PILL -> 32
        MiniStyle.COMPACT_CARD -> 12
        MiniStyle.CONNECTED_DOCK -> 12
        else -> 20
    }
    companion object {
        private inline fun <reified T : Enum<T>> choice(value: String?, fallback: T): T =
            enumValues<T>().firstOrNull { it.name == value } ?: fallback
        fun from(p: Preferences) = MiniPlayerOptions(
            style = choice(p[MiniStyleKey], MiniStyle.CLASSIC),
            progress = choice(p[MiniProgressKey], MiniProgress.NONE),
            corners = choice(p[MiniCornersKey], MiniCorners.SOFT),
            spacing = choice(p[MiniSpacingKey], UiSpacing.COMFORTABLE),
            previous = p[MiniPreviousKey] ?: false,
            next = p[MiniNextKey] ?: true,
            favorite = p[MiniFavoriteKey] ?: false,
            queue = p[MiniQueueKey] ?: false,
            elapsed = p[MiniElapsedKey] ?: false,
            quality = p[MiniQualityKey] ?: false,
            marquee = p[MiniMarqueeKey] ?: false,
            status = p[MiniStatusKey] ?: false,
        )
    }
}
