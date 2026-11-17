package com.mochyy.mochymusic.ui.customization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.datastore.preferences.core.Preferences
import com.mochyy.mochymusic.constants.*

/** Only preference changes update this snapshot; animation frames never touch preference storage. */
data class UiCustomization(
    val accent: UiAccent = UiAccent.CORAL,
    val customAccent: Int = 0xFFF2B5A7.toInt(),
    val highContrast: Boolean = false,
    val textScale: Float = 1f,
    val font: UiFont = UiFont.INTER,
    val corners: UiCorners = UiCorners.SOFT,
    val spacing: UiSpacing = UiSpacing.COMFORTABLE,
    val navStyle: UiNavStyle = UiNavStyle.CURRENT,
    val navLabels: UiNavLabels = UiNavLabels.OFF,
    val navIndicator: UiNavIndicator = UiNavIndicator.PILL,
    val navOrder: String = DefaultNavOrder,
    val navIconSize: UiNavIconSize = UiNavIconSize.NORMAL,
    val navSwipe: Boolean = true,
    val transition: UiTransition = UiTransition.DEFAULT,
    val transitionDuration: Int = 200,
    val pressEffects: Boolean = true,
    val haptics: Boolean = true,
    val loadingMotion: Boolean = true,
    val libraryRows: UiSpacing = UiSpacing.COMFORTABLE,
    val miniOutline: Boolean = true,
    val miniBlack: Boolean = false,
    val hideQuality: Boolean = false,
    val character: UiCharacter = UiCharacter.RANDOM,
) {
    val accentColor: Color get() = when (accent) {
        UiAccent.CORAL -> Color(0xFFF2B5A7)
        UiAccent.SAGE -> Color(0xFFBFCEB6)
        UiAccent.LAVENDER -> Color(0xFFD5C0E8)
        UiAccent.ICE -> Color(0xFFAEDBE8)
        UiAccent.GOLD -> Color(0xFFE8CF94)
        UiAccent.MONOCHROME -> Color(0xFFE6E1E8)
        UiAccent.CUSTOM -> {
            val raw = Color(customAccent or 0xFF000000.toInt())
            if (raw.luminance() < .2f) androidx.compose.ui.graphics.lerp(raw, Color.White, .6f) else raw
        }
    }
    val accentInk: Color get() = if (accent == UiAccent.CORAL) Color(0xFF35221F)
        else if (accentColor.luminance() > .179f) Color(0xFF171318) else Color.White
    val cornerScale: Float get() = when (corners) { UiCorners.SOFT -> 1f; UiCorners.SQUARE -> .35f; UiCorners.ROUND -> 1.3f }
    val spacingScale: Float get() = when (spacing) { UiSpacing.COMPACT -> .85f; UiSpacing.COMFORTABLE -> 1f; UiSpacing.ROOMY -> 1.15f }

    companion object {
        const val DefaultNavOrder = "home,search_input,library,settings"
        fun from(p: Preferences) = UiCustomization(
            accent = selected(p[UiAccentKey], UiAccent.CORAL),
            customAccent = p[UiCustomAccentKey] ?: 0xFFF2B5A7.toInt(),
            highContrast = p[UiContrastKey] ?: false,
            textScale = (p[UiTextScaleKey]?.takeIf { it.isFinite() } ?: 1f).coerceIn(.85f, 1.25f),
            font = selected(p[UiFontKey], UiFont.INTER),
            corners = selected(p[UiCornersKey], UiCorners.SOFT),
            spacing = selected(p[UiSpacingKey], UiSpacing.COMFORTABLE),
            navStyle = selected(p[UiNavStyleKey], UiNavStyle.CURRENT),
            navLabels = selected(p[UiNavLabelsKey], UiNavLabels.OFF),
            navIndicator = selected(p[UiNavIndicatorKey], UiNavIndicator.PILL),
            navOrder = p[UiNavOrderKey] ?: DefaultNavOrder,
            navIconSize = selected(p[UiNavIconSizeKey], UiNavIconSize.NORMAL),
            navSwipe = p[UiNavSwipeKey] ?: true,
            transition = selected(p[UiTransitionKey], UiTransition.DEFAULT),
            transitionDuration = (p[UiTransitionDurationKey] ?: 200).coerceIn(100, 500),
            pressEffects = p[UiPressEffectsKey] ?: true,
            haptics = p[UiHapticsKey] ?: true,
            loadingMotion = p[UiLoadingMotionKey] ?: true,
            libraryRows = selected(p[UiLibraryRowsKey], UiSpacing.COMFORTABLE),
            miniOutline = p[MiniPlayerOutlineKey] ?: true,
            miniBlack = p[PureBlackMiniPlayerKey] ?: false,
            hideQuality = p[HideAudioQualityBadgeKey] ?: false,
            character = selected(p[UiCharacterKey], UiCharacter.RANDOM),
        )
        val HomeShelves = listOf("recently_played", "most_played", "from_the_community", "speed_dial", "history", "live_shows", "artist_list", "account_playlists", "dont_forget_these_songs", "music_videos", "all_time_hits")
        private inline fun <reified T : Enum<T>> selected(value: String?, fallback: T): T =
            enumValues<T>().firstOrNull { it.name == value } ?: fallback

        /** Unknown, duplicated or missing names cannot remove a navigation destination. */
        fun <T> ordered(items: List<T>, order: String, route: (T) -> String): List<T> {
            val byRoute = items.associateBy(route)
            val chosen = order.take(256).split(',').distinct().mapNotNull { byRoute[it] }
            val chosenRoutes = chosen.mapTo(HashSet(), route)
            return chosen + items.filter { route(it) !in chosenRoutes }
        }
    }
}

object UiCustomizationState {
    var options by mutableStateOf(UiCustomization())
}
