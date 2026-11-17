// Theme.kt

package com.mochyy.mochymusic.ui.theme

import com.mochyy.mochymusic.ui.customization.UiCustomizationState
import com.mochyy.mochymusic.constants.UiFont
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

val DefaultThemeColor = Color(0xFF8E8E8E)

/**
 * mochy renders dark-only.
 *
 * The light palette was never finished - large parts of the UI hardcode white text and dark
 * glass - so on a light-mode phone the app looked broken rather than light. Pinned here as one
 * switch instead of being decided independently at every call site.
 */
const val ForceDarkTheme = true

@Composable
fun MochyTheme(
    darkTheme: Boolean = ForceDarkTheme,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val ui = UiCustomizationState.options

    // Dynamic (Material You) colour is deliberately not used - the app ships its own palette.
    // The branch is gone rather than dead-but-present: dynamic*ColorScheme is API 31.
    val baseColorScheme = remember(darkTheme, ui.accent, ui.customAccent, ui.highContrast) {

        if (darkTheme) {
            darkColorScheme(
                primary = PippoColors.Accent,
                onPrimary = PippoColors.OnAccent,
                primaryContainer = PippoColors.AccentContainer,
                onPrimaryContainer = PippoColors.Text,
                secondary = PippoColors.Sage,
                onSecondary = PippoColors.OnSage,
                secondaryContainer = Color(0xFF303B2C),
                onSecondaryContainer = PippoColors.Sage,
                tertiary = PippoColors.Lavender,
                onTertiary = Color(0xFF342440),
                tertiaryContainer = Color(0xFF44354F),
                onTertiaryContainer = PippoColors.Lavender,
                background = PippoColors.Background,
                onBackground = PippoColors.Text,
                surface = PippoColors.Background,
                onSurface = PippoColors.Text,
                surfaceVariant = PippoColors.SurfaceRaised,
                onSurfaceVariant = PippoColors.SecondaryText,
                surfaceTint = Color.Transparent,
                surfaceDim = PippoColors.Background,
                surfaceBright = PippoColors.SurfaceHighest,
                surfaceContainerLowest = PippoColors.Background,
                surfaceContainerLow = PippoColors.Surface,
                surfaceContainer = PippoColors.Surface,
                surfaceContainerHigh = PippoColors.SurfaceRaised,
                surfaceContainerHighest = PippoColors.SurfaceHighest,
                outline = PippoColors.Outline,
                outlineVariant = PippoColors.Divider,
                error = PippoColors.Error,
                onError = Color(0xFF3D1614),
                errorContainer = Color(0xFF602925),
                onErrorContainer = PippoColors.Error,
                inverseSurface = PippoColors.Text,
                inverseOnSurface = PippoColors.Surface,
                inversePrimary = PippoColors.AccentContainer,
                scrim = Color.Black,
            )
        } else {
            lightColorScheme(
                primary = Color.Black,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE0E0E0),
                onPrimaryContainer = Color.Black,
                secondary = Color(0xFF333333),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFCCCCCC),
                onSecondaryContainer = Color.Black,
                background = Color.White,
                onBackground = Color.Black,
                surface = Color.White,
                onSurface = Color.Black,
                surfaceVariant = Color(0xFFF0F0F0),
                onSurfaceVariant = Color.Black,
                surfaceTint = Color.White
            )
        }
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    val font = LookFont.family ?: when (ui.font) {
        UiFont.INTER -> InterFontFamily
        UiFont.SYSTEM -> FontFamily.Default
        UiFont.SERIF -> FontFamily.Serif
        UiFont.MONOSPACE -> FontFamily.Monospace
    }
    val typography = remember(font, ui.textScale) { appTypography(font, ui.textScale) }
    val shapes = remember(ui.corners) { PippoShapes }
    val haptics = if (ui.haptics) LocalHapticFeedback.current else SilentUiHaptics
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
    ) {
        CompositionLocalProvider(LocalHapticFeedback provides haptics) {
            ProvideTextStyle(value = typography.bodyLarge, content = content)
        }
    }
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}

private object SilentUiHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}
