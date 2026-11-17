package com.mochyy.mochymusic.ui.theme

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
    val SecondaryText = Color(0xFFBBB4C3)
    val MutedText = Color(0xFF9D95A7)
    val Outline = Color(0xFF49414F)
    val Divider = Color(0xFF332D3A)
    val Accent = Color(0xFFF2B5A7)
    val OnAccent = Color(0xFF35221F)
    val AccentContainer = Color(0xFF4A3037)
    val Sage = Color(0xFFBFCEB6)
    val OnSage = Color(0xFF263123)
    val Lavender = Color(0xFFD5C0E8)
    val Error = Color(0xFFFFB4AB)

    fun page(pureBlack: Boolean): Color = if (pureBlack) Color.Black else Background
}

object PippoSpacing {
    val Micro = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 20.dp
    val Page = 24.dp
    val Section = 32.dp
}

object PippoSize {
    val TouchTarget = 48.dp
    val Icon = 24.dp
    val SmallIcon = 20.dp
    val EmptyIcon = 32.dp
    val Field = 52.dp
}

object PippoCorners {
    val Small = 12.dp
    val Card = 20.dp
    val Group = 24.dp
    val Sheet = 32.dp
    val Pill = RoundedCornerShape(percent = 50)
}

/** State feedback uses short transitions; existing player/header choreography keeps its own timing. */
object PippoMotion {
    const val Feedback = 140
    const val StateChange = 220
    const val Reveal = 320
    const val Loading = 900
}

val PippoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(PippoCorners.Small),
    medium = RoundedCornerShape(PippoCorners.Card),
    large = RoundedCornerShape(PippoCorners.Group),
    extraLarge = RoundedCornerShape(PippoCorners.Sheet),
)
