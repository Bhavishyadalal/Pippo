// Type.kt

package com.mochyy.mochymusic.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.Typography
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.AppFontKey
import com.mochyy.mochymusic.extensions.toEnum
import com.mochyy.mochymusic.utils.PreferencesCache

private val Inter = FontFamily(
    Font(R.font.inter_thin, FontWeight.Thin),
    Font(R.font.inter_extralight, FontWeight.ExtraLight),
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

/** a look module's font, standing in for inter across the app while that look is on; null is inter */
object LookFont {
    var family: FontFamily? by mutableStateOf(null)
}

internal enum class AppFont(@StringRes val label: Int, private val resource: Int? = null) {
    DEFAULT(R.string.app_font_default),
    LILITA_ONE(R.string.app_font_lilita_one, R.font.lilita_one),
    CAPRASIMO(R.string.app_font_caprasimo, R.font.caprasimo),
    SOME_TIME_LATER(R.string.app_font_some_time_later, R.font.some_time_later);

    private val bundledFamily = resource?.let { FontFamily(Font(it)) }
    val family: FontFamily get() = bundledFamily ?: LookFont.family ?: Inter
}

private val selectedAppFont = derivedStateOf {
    PreferencesCache.snapshot.value[AppFontKey].toEnum(AppFont.DEFAULT)
}

val InterFontFamily: FontFamily get() = selectedAppFont.value.family

/** Headings are compact; paragraphs and small labels keep normal reading space. */
fun appTypography(font: FontFamily): Typography {
    fun style(size: Int, height: Int, weight: FontWeight, tracking: Float) = TextStyle(
        fontFamily = font,
        fontSize = size.sp,
        lineHeight = height.sp,
        fontWeight = weight,
        letterSpacing = tracking.em,
    )
    return Typography(
        displayLarge = style(57, 64, FontWeight.Bold, -.035f),
        displayMedium = style(45, 52, FontWeight.Bold, -.035f),
        displaySmall = style(36, 44, FontWeight.Bold, -.03f),
        headlineLarge = style(32, 40, FontWeight.Bold, -.025f),
        headlineMedium = style(28, 36, FontWeight.Bold, -.025f),
        headlineSmall = style(24, 32, FontWeight.SemiBold, -.02f),
        titleLarge = style(22, 28, FontWeight.SemiBold, -.02f),
        titleMedium = style(16, 24, FontWeight.SemiBold, -.01f),
        titleSmall = style(14, 20, FontWeight.SemiBold, -.005f),
        bodyLarge = style(16, 24, FontWeight.Normal, 0f),
        bodyMedium = style(14, 20, FontWeight.Normal, 0f),
        bodySmall = style(12, 18, FontWeight.Normal, .005f),
        labelLarge = style(14, 20, FontWeight.Medium, 0f),
        labelMedium = style(12, 18, FontWeight.Medium, .005f),
        labelSmall = style(11, 16, FontWeight.Medium, .01f),
    )
}
