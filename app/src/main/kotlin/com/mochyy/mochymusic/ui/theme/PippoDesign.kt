package com.mochyy.mochymusic.ui.theme

import com.mochyy.mochymusic.ui.customization.UiCustomizationState

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Shared visual vocabulary. Artwork palettes and look-module rendering remain independent. */
object PippoColors {
    val Background = Color(0xFF0E0D12)
    val Surface = Color(0xFF18161D)
    val SurfaceRaised = Color(0xFF231F2A)
    val SurfaceHighest = Color(0xFF302B38)
    val Text = Color(0xFFF4EFF5)
    val SecondaryText get() = if (UiCustomizationState.options.highContrast) Text else Color(0xFFBBB4C3)
    val MutedText get() = if (UiCustomizationState.options.highContrast) Text else Color(0xFF9D95A7)
    val Outline get() = if (UiCustomizationState.options.highContrast) Text.copy(alpha = .6f) else Color(0xFF49414F)
    val Divider = Color(0xFF332D3A)
    val Accent get() = UiCustomizationState.options.accentColor
    val OnAccent get() = UiCustomizationState.options.accentInk
    val AccentContainer = Color(0xFF4A3037)
    val Sage = Color(0xFFBFCEB6)
    val OnSage = Color(0xFF263123)
    val Lavender = Color(0xFFD5C0E8)
    val Error = Color(0xFFFFB4AB)

    fun page(pureBlack: Boolean): Color = if (pureBlack) Color.Black else Background
}

object PippoSpacing {
    val Micro get() = (4 * UiCustomizationState.options.spacingScale).dp
    val Small get() = (8 * UiCustomizationState.options.spacingScale).dp
    val Medium get() = (12 * UiCustomizationState.options.spacingScale).dp
    val Large get() = (16 * UiCustomizationState.options.spacingScale).dp
    val ExtraLarge get() = (20 * UiCustomizationState.options.spacingScale).dp
    val Page get() = (24 * UiCustomizationState.options.spacingScale).dp
    val Section get() = (32 * UiCustomizationState.options.spacingScale).dp
}

object PippoSize {
    val TouchTarget = 48.dp
    val Icon = 24.dp
    val SmallIcon = 20.dp
    val EmptyIcon = 32.dp
    val Field = 52.dp
}

object PippoCorners {
    val Small get() = (12 * UiCustomizationState.options.cornerScale).dp
    val Card get() = (20 * UiCustomizationState.options.cornerScale).dp
    val Group get() = (24 * UiCustomizationState.options.cornerScale).dp
    val Sheet get() = (32 * UiCustomizationState.options.cornerScale).dp
    val Pill = RoundedCornerShape(percent = 50)
}

/** State feedback uses short transitions; existing player/header choreography keeps its own timing. */
object PippoMotion {
    const val Feedback = 140
    const val StateChange = 220
    const val Reveal = 320
    const val Loading = 900
}

val PippoShapes get() = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(PippoCorners.Small),
    medium = RoundedCornerShape(PippoCorners.Card),
    large = RoundedCornerShape(PippoCorners.Group),
    extraLarge = RoundedCornerShape(PippoCorners.Sheet),
)
